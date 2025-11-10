package me.gamerduck.alwaysauth.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.gamerduck.alwaysauth.Platform;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Core session proxy server that intercepts Minecraft authentication requests.
 * <p>
 * This server acts as a middleware between Minecraft servers and Mojang's authentication servers.
 * It forwards authentication requests to Mojang with a configurable timeout, and falls back to
 * a local database when Mojang's servers are unreachable.
 * </p>
 * <p>
 * The server handles two primary endpoints:
 * <ul>
 *     <li>/session/minecraft/hasJoined - Validates player authentication</li>
 *     <li>/session/minecraft/join - Records player join requests</li>
 * </ul>
 * </p>
 * <p>
 * When authentication mode is enabled, requests are routed through /auth with token validation.
 * </p>
 */
public class SessionProxyServer {
    /** Timeout in milliseconds for upstream Mojang server requests */
    private static final int UPSTREAM_TIMEOUT_MS = 3000;

    private final HttpServer server;
    private final AuthDatabase database;
    private final Gson gson;
    private final SessionConfig config;
    private final Platform platform;
    private final String upstreamSessionServer;
    private final Boolean debug;

    /**
     * Gets the authentication database instance.
     *
     * @return the AuthDatabase used for caching player authentication data
     */
    public AuthDatabase getDatabase() {
        return database;
    }

    /**
     * Constructs a new SessionProxyServer.
     * <p>
     * Initializes the HTTP server, database connection, and endpoint handlers.
     * The server uses virtual threads for concurrent request handling.
     * </p>
     *
     * @param port the port number to bind the HTTP server to
     * @param dataFolder the directory for storing local database files
     * @param platform the platform implementation providing logging and configuration
     * @param config the session configuration containing server settings
     * @throws IOException if the server cannot be created or bound to the specified port
     */
    public SessionProxyServer(int port, File dataFolder, Platform platform, SessionConfig config) throws IOException {
        this.platform = platform;
        this.gson = new Gson();
        this.config = config;
        this.debug = platform.isDebug();

        if (config.isRemoteDatabase()) {
            this.database = new AuthDatabase(
                    config.getDatabaseHost(),
                    config.getDatabasePort(),
                    config.getDatabaseName(),
                    config.getDatabaseUsername(),
                    config.getDatabasePassword(),
                    config.getDatabaseType(),
                    platform
            );
        } else {
            this.database = new AuthDatabase(new File(dataFolder, "authcache.db"), platform);
        }

        this.upstreamSessionServer = config.getUpstreamSessionServer();

        this.server = HttpServer.create(new InetSocketAddress(config.getIpAddress(), port), 0);


        if (config.isAuthenticationEnabled()) {
            this.server.createContext("/auth", this::handleAuthPath);
        } else {
            this.server.createContext("/session/minecraft/hasJoined", this::handleHasJoined);
            this.server.createContext("/session/minecraft/join", this::handleJoin);
        }
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        platform.sendLogMessage("Session proxy server created on port " + port);
        platform.sendLogMessage("Authentication mode: " + (config.isAuthenticationEnabled() ? "ENABLED" : "DISABLED"));
    }

    /**
     * Starts the HTTP server to begin accepting authentication requests.
     * <p>
     * The server will begin listening on the configured port and handle incoming requests
     * using virtual threads for concurrent processing.
     * </p>
     */
    public void start() {
        server.start();
        platform.sendLogMessage("Session proxy server started");
    }

    /**
     * Stops the HTTP server and closes all resources.
     * <p>
     * This method gracefully shuts down the HTTP server and closes the database connection.
     * The server will stop accepting new requests immediately.
     * </p>
     */
    public void stop() {
        server.stop(0);
        database.close();
        platform.sendLogMessage("Session proxy server stopped");
    }

    /**
     * Verifies the authentication token for protected endpoints.
     * <p>
     * If authentication is disabled in config, always returns true.
     * Otherwise, validates the provided token against the configured secret key.
     * </p>
     *
     * @param providedToken the token to verify, or null if not provided
     * @return true if the token is valid or authentication is disabled, false otherwise
     */
    private boolean verifyAuthToken(String providedToken) {
        if (!config.isAuthenticationEnabled()) {
            return true;
        }

        if (providedToken == null) {
            return false;
        }

        String currentToken = config.getSecretKey();
        if (providedToken.equals(currentToken)) {
            return true;
        }

        return false;
    }

    /**
     * Handles authenticated requests to the /auth endpoint.
     * <p>
     * Extracts and verifies the authentication token from the query string,
     * then routes the request to the appropriate handler based on the path.
     * Expects format: /auth?token={token}/session/minecraft/{endpoint}
     * </p>
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleAuthPath(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            String token = null;
            String fullPath = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && "token".equals(pair[0])) {
                        String[] pairAndPath = pair[1].split("/session", 2);
                        token = URLDecoder.decode(pairAndPath[0], "UTF-8");
                        fullPath = "/session" + pairAndPath[1];
                        break;
                    }
                }
            }

            if (!verifyAuthToken(token)) {
                platform.sendWarningLogMessage("Invalid or missing auth token");
                sendResponse(exchange, 403, "Forbidden: Invalid authentication token");
                return;
            }

            if (fullPath.startsWith("/session/minecraft/hasJoined")) {
                handleHasJoined(exchange);
            } else if (fullPath.startsWith("/session/minecraft/join")) {
                handleJoin(exchange);
            } else {
                sendResponse(exchange, 404, "Not Found");
            }

        } catch (Exception e) {
            platform.sendSevereLogMessage("Error handling auth path: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal server error");
        }
    }

    /**
     * Handles the /session/minecraft/hasJoined endpoint for player authentication validation.
     * <p>
     * This endpoint is called by Minecraft servers to verify that a player has successfully
     * authenticated with Mojang. The method:
     * <ol>
     *     <li>Forwards the request to Mojang's servers</li>
     *     <li>On success: caches the authentication data and returns the profile</li>
     *     <li>On failure: attempts fallback authentication using cached data</li>
     * </ol>
     * Fallback authentication validates IP addresses and expiry times based on security settings.
     * </p>
     *
     * @param exchange the HTTP exchange containing username, serverId, and optional IP parameters
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleHasJoined(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            QueryParams params = parseQuery(query);

            String username = params.get("username");
            String serverId = params.get("serverId");
            String ip = params.get("ip");

            if (username == null || serverId == null) {
                sendResponse(exchange, 400, "Missing parameters");
                return;
            }

            if (debug) platform.sendLogMessage("Authentication request for user: " + username + " (serverId: " + serverId + ")");

            try {
                String upstreamResponse = forwardToUpstream("/session/minecraft/hasJoined", query);

                if (upstreamResponse != null && !upstreamResponse.isEmpty()) {
                    JsonObject profile = gson.fromJson(upstreamResponse, JsonObject.class);
                    database.cacheAuthentication(username, ip, profile);
                    if (debug) platform.sendLogMessage("Successfully authenticated " + username + " via Upstream");
                }

                sendResponse(exchange, 200, upstreamResponse);
                return;

            } catch (Exception e) {
                platform.sendWarningLogMessage("Upstream authentication failed for " + username + ": " + e.getMessage());

                if (config.isFallbackEnabled()) {
                    String fallbackResponse = database.getFallbackAuth(username, ip, config.getMaxOfflineHours());

                    if (fallbackResponse != null) {
                        platform.sendWarningLogMessage("Using FALLBACK authentication for " + username);
                        sendResponse(exchange, 200, fallbackResponse);
                        return;
                    } else {
                        platform.sendWarningLogMessage("Fallback authentication failed for " + username + " - no cached data or IP mismatch");
                    }
                }

                sendResponse(exchange, 204, "");
            }

        } catch (Exception e) {
            platform.sendSevereLogMessage("Error handling hasJoined: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal server error");
        }
    }

    /**
     * Handles the /session/minecraft/join endpoint for recording player join requests.
     * <p>
     * This endpoint is called when a player attempts to join a server. The request
     * is forwarded directly to Mojang's servers for processing.
     * </p>
     *
     * @param exchange the HTTP exchange containing the join request body
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleJoin(HttpExchange exchange) throws IOException {
        try {
            String body = readRequestBody(exchange);
            String response = forwardToUpstreamPost("/session/minecraft/join", body);

            sendResponse(exchange, response != null ? 204 : 500, response != null ? "" : "Failed");

        } catch (Exception e) {
            platform.sendWarningLogMessage("Error forwarding join request: " + e.getMessage());
            sendResponse(exchange, 500, "Internal server error");
        }
    }

    /**
     * Forwards a GET request to the upstream Mojang session server.
     * <p>
     * Sends the request with a configured timeout. Returns the response body
     * if the status is 200, an empty string if 204, or throws an exception for other codes.
     * </p>
     *
     * @param endpoint the API endpoint path (e.g., "/session/minecraft/hasJoined")
     * @param query the URL query string containing request parameters
     * @return the response body from Mojang's servers, or empty string for 204 responses
     * @throws IOException if the request fails, times out, or returns an error status code
     */
    private String forwardToUpstream(String endpoint, String query) throws IOException {
        URL url = new URL(upstreamSessionServer + endpoint + "?" + query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(UPSTREAM_TIMEOUT_MS);
        conn.setReadTimeout(UPSTREAM_TIMEOUT_MS);

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            return readInputStream(conn.getInputStream());
        } else if (responseCode == 204) {
            return "";
        }

        throw new IOException("Upstream returned status: " + responseCode);
    }

    /**
     * Forwards a POST request to the upstream Mojang session server.
     * <p>
     * Sends the request body as JSON with a configured timeout.
     * Returns an empty string if the status is 204, or throws an exception for other codes.
     * </p>
     *
     * @param endpoint the API endpoint path (e.g., "/session/minecraft/join")
     * @param body the JSON request body to send
     * @return an empty string if successful (204 status)
     * @throws IOException if the request fails, times out, or returns an error status code
     */
    private String forwardToUpstreamPost(String endpoint, String body) throws IOException {
        URL url = new URL(upstreamSessionServer + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(UPSTREAM_TIMEOUT_MS);
        conn.setReadTimeout(UPSTREAM_TIMEOUT_MS);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 204) {
            return "";
        }

        throw new IOException("Upstream returned status: " + responseCode);
    }

    /**
     * Sends an HTTP response to the client.
     * <p>
     * Handles proper encoding and content-length headers. For 204 (No Content) responses,
     * sends -1 as the content length. Closes the exchange after sending the response.
     * </p>
     *
     * @param exchange the HTTP exchange to send the response through
     * @param statusCode the HTTP status code to return
     * @param response the response body content
     * @throws IOException if an I/O error occurs while sending the response
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, statusCode == 204 ? -1 : bytes.length);

        if (statusCode != 204 && bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        exchange.close();
    }

    /**
     * Reads an InputStream and converts it to a String.
     * <p>
     * Uses UTF-8 encoding and reads line by line, concatenating all lines
     * into a single string. Automatically closes the BufferedReader.
     * </p>
     *
     * @param is the InputStream to read from
     * @return the complete content of the stream as a String
     * @throws IOException if an I/O error occurs while reading
     */
    private String readInputStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * Reads the request body from an HTTP exchange.
     *
     * @param exchange the HTTP exchange containing the request body
     * @return the request body as a String
     * @throws IOException if an I/O error occurs while reading the body
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        return readInputStream(exchange.getRequestBody());
    }

    /**
     * Parses a URL query string into key-value pairs.
     * <p>
     * Decodes URL-encoded parameter values using UTF-8 encoding.
     * Silently ignores malformed parameters or encoding errors.
     * </p>
     *
     * @param query the query string to parse (e.g., "username=Player&serverId=abc123")
     * @return a QueryParams object containing the parsed parameters
     */
    private QueryParams parseQuery(String query) {
        QueryParams params = new QueryParams();
        if (query == null) return params;

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                try {
                    params.put(pair[0], URLDecoder.decode(pair[1], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                }
            }
        }
        return params;
    }

    /**
     * Simple container for URL query parameters.
     * <p>
     * Provides basic key-value storage for parsed query string parameters.
     * </p>
     */
    private static class QueryParams {
        private final java.util.HashMap<String, String> params = new java.util.HashMap<>();

        /**
         * Stores a key-value pair.
         *
         * @param key the parameter name
         * @param value the parameter value
         */
        void put(String key, String value) {
            params.put(key, value);
        }

        /**
         * Retrieves the value for a given parameter name.
         *
         * @param key the parameter name
         * @return the parameter value, or null if not found
         */
        String get(String key) {
            return params.get(key);
        }
    }
}
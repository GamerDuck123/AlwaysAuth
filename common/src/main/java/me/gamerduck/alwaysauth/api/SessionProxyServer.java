package me.gamerduck.alwaysauth.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.gamerduck.alwaysauth.Platform;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.Executors;

/**
 * Core session proxy server that forwards authentication to Upstream
 * and falls back to local database when Upstream is unavailable.
 */
public class SessionProxyServer {
    private static final int UPSTREAM_TIMEOUT_MS = 3000;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_VALIDITY_MS = 86400000; // 24 hours

    private final HttpServer server;
    private final AuthDatabase database;
    private final Gson gson;
    private final SessionConfig config;
    private final Platform platform;
    private final String upstreamSessionServer;
    private final Boolean debug;

    public AuthDatabase getDatabase() {
        return database;
    }

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
            // Only needed if authentication is disabled
            this.server.createContext("/session/minecraft/hasJoined", this::handleHasJoined);
            this.server.createContext("/session/minecraft/join", this::handleJoin);
        }
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        platform.sendLogMessage("Session proxy server created on port " + port);
        platform.sendLogMessage("Authentication mode: " + (config.isAuthenticationEnabled() ? "ENABLED" : "DISABLED"));
    }

    public void start() {
        server.start();
        platform.sendLogMessage("Session proxy server started");
    }

    public void stop() {
        server.stop(0);
        database.close();
        platform.sendLogMessage("Session proxy server stopped");
    }

    private boolean verifyAuthToken(String providedToken) {
        if (!config.isAuthenticationEnabled()) {
            return true;
        }

        if (providedToken == null) {
            return false;
        }

        // Check current token
        String currentToken = config.getSecretKey();
        if (providedToken.equals(currentToken)) {
            return true;
        }

        return false;
    }

    private void handleAuthPath(HttpExchange exchange) throws IOException {
        try {
            // Get query string to extract token
            String query = exchange.getRequestURI().getQuery();
            String token = null;
            String fullPath = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && "token".equals(pair[0])) {
                        // Splitting at /session ensures that /sessio could be in a key, although very unlikely
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

    private String readRequestBody(HttpExchange exchange) throws IOException {
        return readInputStream(exchange.getRequestBody());
    }

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

    private static class QueryParams {
        private final java.util.HashMap<String, String> params = new java.util.HashMap<>();

        void put(String key, String value) {
            params.put(key, value);
        }

        String get(String key) {
            return params.get(key);
        }
    }
}
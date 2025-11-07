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
 * Core session proxy server that forwards authentication to Mojang
 * and falls back to local database when Mojang is unavailable.
 */
public class SessionProxyServer {
    private static final int MOJANG_TIMEOUT_MS = 3000;

    private final HttpServer server;
    private final AuthDatabase database;
    private final Gson gson;
    private final SessionConfig config;
    private final Platform platform;
    private final String upstreamSessionServer;

    public AuthDatabase getDatabase() {
        return database;
    }

    public SessionProxyServer(int port, File dataFolder, Platform platform, SessionConfig config) throws IOException {
        this.platform = platform;
        this.gson = new Gson();
        this.config = config;
        if (config.isRemoteDatabase()) this.database = new AuthDatabase(config.getDatabaseHost(), config.getDatabasePort(),
                config.getDatabaseName(), config.getDatabaseUsername(), config.getDatabasePassword(), config.getDatabaseType(), platform);
        else this.database = new AuthDatabase(new File(dataFolder, "authcache.db"), platform);
        this.upstreamSessionServer = config.getUpstreamSessionServer();

        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        this.server.createContext("/session/minecraft/hasJoined", this::handleHasJoined);
        this.server.createContext("/session/minecraft/join", this::handleJoin);
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        platform.sendLogMessage("Session proxy server created on port " + port);
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

            platform.sendLogMessage("Authentication request for user: " + username + " (serverId: " + serverId + ")");

            try {
                String mojangResponse = forwardToMojang("/session/minecraft/hasJoined", query);

                if (mojangResponse != null && !mojangResponse.isEmpty()) {
                    JsonObject profile = gson.fromJson(mojangResponse, JsonObject.class);
                    database.cacheAuthentication(username, ip, profile);
                    platform.sendLogMessage("Successfully authenticated " + username + " via Mojang");
                }

                sendResponse(exchange, 200, mojangResponse);
                return;

            } catch (Exception e) {
                platform.sendWarningLogMessage("Mojang authentication failed for " + username + ": " + e.getMessage());

                // Fall back to local database
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

                sendResponse(exchange, 204, ""); // No content = authentication failed
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
            String response = forwardToMojangPost("/session/minecraft/join", body);

            sendResponse(exchange, response != null ? 204 : 500, response != null ? "" : "Failed");

        } catch (Exception e) {
            platform.sendWarningLogMessage("Error forwarding join request: " + e.getMessage());
            sendResponse(exchange, 500, "Internal server error");
        }
    }

    private String forwardToMojang(String endpoint, String query) throws IOException {
        URL url = new URL(upstreamSessionServer + endpoint + "?" + query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(MOJANG_TIMEOUT_MS);
        conn.setReadTimeout(MOJANG_TIMEOUT_MS);

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            return readInputStream(conn.getInputStream());
        } else if (responseCode == 204) {
            return ""; // No content means auth failed
        }

        throw new IOException("Mojang returned status: " + responseCode);
    }

    private String forwardToMojangPost(String endpoint, String body) throws IOException {
        URL url = new URL(upstreamSessionServer + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(MOJANG_TIMEOUT_MS);
        conn.setReadTimeout(MOJANG_TIMEOUT_MS);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 204) {
            return "";
        }

        throw new IOException("Mojang returned status: " + responseCode);
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
                    // UTF-8 is always supported
                }
            }
        }
        return params;
    }

    private static class QueryParams {
        private final java.util.Map<String, String> params = new java.util.HashMap<>();

        void put(String key, String value) {
            params.put(key, value);
        }

        String get(String key) {
            return params.get(key);
        }
    }
}
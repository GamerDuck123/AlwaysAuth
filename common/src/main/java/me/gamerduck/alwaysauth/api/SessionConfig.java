package me.gamerduck.alwaysauth.api;

import me.gamerduck.alwaysauth.Platform;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;

public class SessionConfig {
    private final LinkedHashMap<String, String> properties;
    private final LinkedHashMap<String, String> comments;
    private final File configFile;
    private final Platform platform;

    private static final boolean DEFAULT_DEBUG = false;

    private static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_PORT = 8765;

    private static final boolean DEFAULT_FALLBACK_ENABLED = true;
    private static final int DEFAULT_MAX_OFFLINE_HOURS = 72; // 3 days
    private static final int DEFAULT_CLEANUP_DAYS = 30;

    private static final String DEFAULT_DB_TYPE = "h2";
    private static final String DEFAULT_DB_HOST = "localhost";
    private static final int DEFAULT_DB_PORT_MYSQL = 3306;
    private static final String DEFAULT_DB_NAME = "minecraft";
    private static final String DEFAULT_DB_USERNAME = "root";
    private static final String DEFAULT_DB_PASSWORD = "";

    private static final String DEFAULT_UPSTREAM_SESSION_SERVER = "https://sessionserver.mojang.com";
    private static final boolean DEFAULT_AUTHENTICATION_ENABLED = true; // PATH-based auth works!
    private static final boolean DEFAULT_ENCRYPT_PROFILE_DATA = false;

    public SessionConfig(File dataFolder, Platform platform) {
        this.configFile = new File(dataFolder, "config.properties");
        this.platform = platform;
        this.properties = new LinkedHashMap<>();
        this.comments = new LinkedHashMap<>();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        loadConfig();
    }

    private void loadConfig() {
        setDefaults();

        if (configFile.exists()) {
            LinkedHashMap<String, String> defaults = new LinkedHashMap<>(properties);
            LinkedHashMap<String, String> defaultComments = new LinkedHashMap<>(comments);

            properties.clear();
            comments.clear();

            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                String lastComment = null;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty()) {
                        continue;
                    }

                    if (line.startsWith("#")) {
                        lastComment = line.substring(1).trim();
                        continue;
                    }

                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex > 0) {
                        String key = line.substring(0, equalsIndex).trim();
                        String value = line.substring(equalsIndex + 1).trim();
                        properties.put(key, value);

                        if (lastComment != null) {
                            comments.put(key, lastComment);
                            lastComment = null;
                        }
                    }
                }

                boolean configChanged = false;

                for (String key : defaults.keySet()) {
                    if (!properties.containsKey(key)) {
                        properties.put(key, defaults.get(key));
                        comments.put(key, defaultComments.get(key));
                        configChanged = true;
                        platform.sendLogMessage("Added new config option: " + key);
                    }
                }

                LinkedHashMap<String, String> filtered = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    if (defaults.containsKey(entry.getKey())) {
                        filtered.put(entry.getKey(), entry.getValue());
                    } else {
                        configChanged = true;
                        platform.sendLogMessage("Removed deprecated config option: " + entry.getKey());
                    }
                }

                for (String key : filtered.keySet()) {
                    String oldComment = comments.get(key);
                    String newComment = defaultComments.get(key);

                    // Comment was added
                    if ((oldComment == null || oldComment.isEmpty()) && newComment != null && !newComment.isEmpty()) {
                        comments.put(key, newComment);
                        configChanged = true;
                    }
                    // Comment was removed
                    else if ((oldComment != null && !oldComment.isEmpty()) && (newComment == null || newComment.isEmpty())) {
                        comments.remove(key);
                        configChanged = true;
                    }
                    // Comment was changed
                    else if (oldComment != null && newComment != null && !oldComment.equals(newComment)) {
                        comments.put(key, newComment);
                        configChanged = true;
                    }
                }

                LinkedHashMap<String, String> reordered = new LinkedHashMap<>();
                for (String key : defaults.keySet()) {
                    if (filtered.containsKey(key)) {
                        reordered.put(key, filtered.get(key));
                    }
                }
                properties.clear();
                properties.putAll(reordered);

                if (configChanged) {
                    platform.sendLogMessage("Configuration updated, saving changes...");
                    saveConfig();
                }

                platform.sendLogMessage("Configuration loaded from " + configFile.getName());
            } catch (IOException e) {
                platform.sendSevereLogMessage("Failed to load config, using defaults: " + e.getMessage());
                properties.clear();
                properties.putAll(defaults);
                comments.clear();
                comments.putAll(defaultComments);
                saveConfig();
            }
        } else {
            saveConfig();
        }
    }

    private void setDefaults() {
        properties.clear();
        comments.clear();
        setProperty("debug", String.valueOf(DEFAULT_DEBUG), "Whether or not there should be debug message\n# This won't work on the standalone jar");

        // Server settings
        setProperty("ip-address", DEFAULT_IP_ADDRESS, "The ip for the session server\n# If set anything other than 127.0.0.1 or 0.0.0.0 (allows public access), it will treat as external server\n# An external server means only port needs to be set (to match that external server) and it will use that to authenticate.\n# Please note as of right now you will not see console logs on the server if you are using an external server");
        setProperty("port", String.valueOf(DEFAULT_PORT), "Port for the session server");

        // Security settings
        setProperty("authentication-enabled", String.valueOf(DEFAULT_AUTHENTICATION_ENABLED), "Enable HMAC-SHA256 signature verification for authorized servers\n# Currently DISABLED by default due to Minecraft URL handling limitations\n# Use firewall rules or localhost restriction for access control instead\n# Database encryption works regardless of this setting");
        setProperty("secret-key", generateSecretKey(), "Secret key for database encryption (auto-generated)\n# KEEP THIS SECRET! Used to encrypt IP addresses and profile data in database\n# If deleted database will need to also be reset!\n# To regenerate, delete this line and restart the server");
        setProperty("encrypt-profile-data", String.valueOf(DEFAULT_ENCRYPT_PROFILE_DATA), "Encrypt player profile data in database (JSON with skins/capes)\n# IP addresses are ALWAYS encrypted\n# Set to true for maximum privacy (slight performance impact)");

        // Fallback settings
        setProperty("fallback-enabled", String.valueOf(DEFAULT_FALLBACK_ENABLED), "Enable session fallback when Mojang servers are down");
        setProperty("max-offline-hours", String.valueOf(DEFAULT_MAX_OFFLINE_HOURS), "Maximum hours a player can stay offline before requiring re-authentication (0 = always require)");
        setProperty("cleanup-days", String.valueOf(DEFAULT_CLEANUP_DAYS), "Days before old session data is cleaned up");
        setProperty("security-level", "basic", "Security level: 'basic' (always verify) or 'medium' (use max-offline-hours)");
        setProperty("upstream-server", "https://sessionserver.mojang.com", "Upstream Session Server URL\n# Default is Mojang's official one but this option is here to work with things like minehut's external servers");

        // Database settings
        setProperty("database.type", DEFAULT_DB_TYPE, "Database type: h2, mysql, or mariadb");
        setProperty("database.host", DEFAULT_DB_HOST, "Database host (not used for H2)");
        setProperty("database.port", String.valueOf(DEFAULT_DB_PORT_MYSQL), "Database port (not used for H2)");
        setProperty("database.name", DEFAULT_DB_NAME, "Database name");
        setProperty("database.username", DEFAULT_DB_USERNAME, "Database username (not used for H2)");
        setProperty("database.password", DEFAULT_DB_PASSWORD, "Database password (not used for H2)");
    }

    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256 bits
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes).replaceAll("\\+", String.valueOf((char) (random.nextInt(26) + 'a')));
    }

    private void setProperty(String key, String value, String comment) {
        properties.put(key, value);
        if (comment != null && !comment.isEmpty()) {
            if (comment.contains("\n")) Arrays.stream(comment.split("\n")).forEachOrdered(line -> comments.put(key, comment));
            else comments.put(key, comment);
        }
    }

    public void saveConfig() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            writer.write("###################################");
            writer.newLine();
            writer.write("#                                 #");
            writer.newLine();
            writer.write("#    Always Auth Configuration    #");
            writer.newLine();
            writer.write("#                                 #");
            writer.newLine();
            writer.write("###################################");
            writer.newLine();
            writer.newLine();

            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Write comment if exists
                if (comments.containsKey(key)) {
                    writer.write("# " + comments.get(key));
                    writer.newLine();
                }

                // Write property
                writer.write(key + "=" + value);
                writer.newLine();
                writer.newLine();
            }

            platform.sendLogMessage("Configuration saved to " + configFile.getName());
        } catch (IOException e) {
            platform.sendSevereLogMessage("Failed to save config: " + e.getMessage());
        }
    }

    // Getters
    public int getPort() {
        return Integer.parseInt(properties.getOrDefault("port", String.valueOf(DEFAULT_PORT)));
    }

    public Boolean getDebug() {
        return Boolean.parseBoolean(properties.getOrDefault("debug", String.valueOf(DEFAULT_DEBUG)));
    }

    public boolean isAuthenticationEnabled() {
        return Boolean.parseBoolean(properties.getOrDefault("authentication-enabled", String.valueOf(DEFAULT_AUTHENTICATION_ENABLED)));
    }

    public String getSecretKey() {
        return properties.getOrDefault("secret-key", generateSecretKey());
    }

    public boolean isEncryptProfileData() {
        return Boolean.parseBoolean(properties.getOrDefault("encrypt-profile-data", String.valueOf(DEFAULT_ENCRYPT_PROFILE_DATA)));
    }

    public boolean isFallbackEnabled() {
        return Boolean.parseBoolean(properties.getOrDefault("fallback-enabled", String.valueOf(DEFAULT_FALLBACK_ENABLED)));
    }

    public int getMaxOfflineHours() {
        String level = properties.getOrDefault("security-level", "basic");
        if ("basic".equalsIgnoreCase(level)) {
            return 0;
        }
        return Integer.parseInt(properties.getOrDefault("max-offline-hours", String.valueOf(DEFAULT_MAX_OFFLINE_HOURS)));
    }

    public String getUpstreamSessionServer() {
        return properties.getOrDefault("upstream-server", DEFAULT_UPSTREAM_SESSION_SERVER);
    }

    public String getIpAddress() {
        return properties.getOrDefault("ip-address", DEFAULT_IP_ADDRESS);
    }

    public int getCleanupDays() {
        return Integer.parseInt(properties.getOrDefault("cleanup-days", String.valueOf(DEFAULT_CLEANUP_DAYS)));
    }

    public String getSecurityLevel() {
        return properties.getOrDefault("security-level", "basic");
    }

    public String getSessionServerUrl() {
        return "http://" + getIpAddress() + ":" + getPort() + "/auth?token=" + getSecretKey();
    }

    public String getDatabaseType() {
        return properties.getOrDefault("database.type", DEFAULT_DB_TYPE);
    }

    public String getDatabaseHost() {
        return properties.getOrDefault("database.host", DEFAULT_DB_HOST);
    }

    public int getDatabasePort() {
        return Integer.parseInt(properties.getOrDefault("database.port", String.valueOf(DEFAULT_DB_PORT_MYSQL)));
    }

    public String getDatabaseName() {
        return properties.getOrDefault("database.name", DEFAULT_DB_NAME);
    }

    public String getDatabaseUsername() {
        return properties.getOrDefault("database.username", DEFAULT_DB_USERNAME);
    }

    public String getDatabasePassword() {
        return properties.getOrDefault("database.password", DEFAULT_DB_PASSWORD);
    }

    // Setters
    public void setUpstreamSessionServer(String server) {
        properties.put("upstream-server", server);
    }

    public void setIpAddress(String ip) {
        properties.put("ip-address", ip);
    }

    public void setPort(int port) {
        properties.put("port", String.valueOf(port));
    }

    public void setAuthenticationEnabled(boolean enabled) {
        properties.put("authentication-enabled", String.valueOf(enabled));
    }

    public void setSecretKey(String key) {
        properties.put("secret-key", key);
    }

    public void setEncryptProfileData(boolean encrypt) {
        properties.put("encrypt-profile-data", String.valueOf(encrypt));
    }

    public void setFallbackEnabled(boolean enabled) {
        properties.put("fallback-enabled", String.valueOf(enabled));
    }

    public void setMaxOfflineHours(int hours) {
        properties.put("max-offline-hours", String.valueOf(hours));
    }

    public void setSecurityLevel(String level) {
        if ("basic".equalsIgnoreCase(level) || "medium".equalsIgnoreCase(level)) {
            properties.put("security-level", level.toLowerCase());
        }
    }

    public void setCleanupDays(int days) {
        properties.put("cleanup-days", String.valueOf(days));
    }

    public void setDatabaseType(String type) {
        if ("sqlite".equalsIgnoreCase(type) || "mysql".equalsIgnoreCase(type) ||
                "mariadb".equalsIgnoreCase(type) || "postgresql".equalsIgnoreCase(type)) {
            properties.put("database.type", type.toLowerCase());
        }
    }

    public void setDatabaseHost(String host) {
        properties.put("database.host", host);
    }

    public void setDatabasePort(int port) {
        properties.put("database.port", String.valueOf(port));
    }

    public void setDatabaseName(String name) {
        properties.put("database.name", name);
    }

    public void setDatabaseUsername(String username) {
        properties.put("database.username", username);
    }

    public void setDatabasePassword(String password) {
        properties.put("database.password", password);
    }

    public boolean isRemoteDatabase() {
        return !getDatabaseType().equalsIgnoreCase("h2");
    }

    @Override
    public String toString() {
        return "SessionConfig{" +
                "ip-address=" + getIpAddress() +
                ", port=" + getPort() +
                ", authenticationEnabled=" + isAuthenticationEnabled() +
                ", fallbackEnabled=" + isFallbackEnabled() +
                ", securityLevel=" + getSecurityLevel() +
                ", upstream-server=" + getUpstreamSessionServer() +
                ", maxOfflineHours=" + getMaxOfflineHours() +
                ", cleanupDays=" + getCleanupDays() +
                ", databaseType=" + getDatabaseType() +
                ", databaseHost=" + getDatabaseHost() +
                ", databasePort=" + getDatabasePort() +
                '}';
    }
}
package me.gamerduck.alwaysauth.api.config;

import me.gamerduck.alwaysauth.Platform;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class SessionConfig {
    private final LinkedHashMap<String, String> properties;
    private final LinkedHashMap<String, String> comments;
    private final File configFile;
    private final Platform platform;
    private final Map<String, String> envOverrides;

    private static final boolean DEFAULT_DEBUG = false;
    private static final boolean DEFAULT_UPDATE_CHECKS = true;

    private static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_PORT = 8765;

    private static final boolean DEFAULT_FALLBACK_ENABLED = true;
    private static final int DEFAULT_MAX_OFFLINE_HOURS = 72;
    private static final int DEFAULT_CLEANUP_DAYS = 30;

    private static final String DEFAULT_DB_TYPE = "h2";
    private static final String DEFAULT_DB_HOST = "localhost";
    private static final int DEFAULT_DB_PORT_MYSQL = 3306;
    private static final String DEFAULT_DB_NAME = "minecraft";
    private static final String DEFAULT_DB_USERNAME = "root";
    private static final String DEFAULT_DB_PASSWORD = "";

    private static final String DEFAULT_UPSTREAM_SESSION_SERVER = "https://sessionserver.mojang.com";
    private static final boolean DEFAULT_AUTHENTICATION_ENABLED = true;

    public SessionConfig(File dataFolder, Platform platform) {
        this(dataFolder, platform, null);
    }

    public SessionConfig(File dataFolder, Platform platform, Map<String, String> envOverrides) {
        this.configFile = new File(dataFolder, "config.properties");
        this.platform = platform;
        this.envOverrides = envOverrides != null ? envOverrides : Collections.emptyMap();
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
                    }
                }

                LinkedHashMap<String, String> filtered = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    if (defaults.containsKey(entry.getKey())) {
                        filtered.put(entry.getKey(), entry.getValue());
                    } else {
                        configChanged = true;
                    }
                }

                for (String key : filtered.keySet()) {
                    String oldComment = comments.get(key);
                    String newComment = defaultComments.get(key);

                    if ((oldComment == null || oldComment.isEmpty()) && newComment != null && !newComment.isEmpty()) {
                        comments.put(key, newComment);
                        configChanged = true;
                    }
                    else if ((oldComment != null && !oldComment.isEmpty()) && (newComment == null || newComment.isEmpty())) {
                        comments.remove(key);
                        configChanged = true;
                    }
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

        applyEnvOverrides();
    }

    private void applyEnvOverrides() {
        if (envOverrides.isEmpty()) return;

        String prefix = "ALWAYS_AUTH_";
        for (Map.Entry<String, String> entry : envOverrides.entrySet()) {
            String envKey = entry.getKey();
            String envValue = entry.getValue();
            if (envValue == null || envValue.isEmpty()) continue;

            String configKey = envKey.substring(prefix.length())
                    .toLowerCase()
                    .replace('_', '.');

            if (!properties.containsKey(configKey)) {
                int lastDot = configKey.lastIndexOf('.');
                if (lastDot > 0) {
                    configKey = configKey.substring(0, lastDot) + "-" + configKey.substring(lastDot + 1);
                }
            }

            if (properties.containsKey(configKey)) {
                String oldValue = properties.get(configKey);
                properties.put(configKey, envValue);
                platform.sendLogMessage("Config override from environment: " + configKey + " = " + envValue + " (was: " + oldValue + ")");
            } else {
                platform.sendWarningLogMessage("Unknown environment variable ignored: " + envKey + " (mapped to unknown key: " + configKey + ")");
            }
        }
    }

    private void setDefaults() {
        properties.clear();
        comments.clear();
        addComment("###################################\n"
                + "#                                 #\n"
                + "#    Always Auth Configuration    #\n"
                + "#                                 #\n"
                + "###################################");
        setProperty("debug", String.valueOf(DEFAULT_DEBUG), "Whether or not there should be debug message\n# This won't work on the standalone jar");
        setProperty("check-updates", String.valueOf(DEFAULT_UPDATE_CHECKS), "Check for updates and notify staff (and console) on join who have the permission alwaysauth.admin");
        setProperty("ip-address", DEFAULT_IP_ADDRESS, "The ip for the session server\n# If set anything other than 127.0.0.1 or 0.0.0.0 (allows public access), it will treat as external server\n# An external server means only port needs to be set (to match that external server) and it will use that to authenticate.\n# Please note as of right now you will not see console logs on the server if you are using an external server");
        setProperty("port", String.valueOf(DEFAULT_PORT), "Port for the session server");

        addComment("\n###########################\n"
                + "#    Security Settings    #\n"
                + "###########################");
        setProperty("authentication-enabled", String.valueOf(DEFAULT_AUTHENTICATION_ENABLED), "Enable HMAC-SHA256 signature verification for authorized servers\n# Currently DISABLED by default due to Minecraft URL handling limitations\n# Use firewall rules or localhost restriction for access control instead\n# Database encryption works regardless of this setting");
        setProperty("secret-key", generateSecretKey(), "Secret key for database encryption (auto-generated)\n# KEEP THIS SECRET! Used to encrypt IP addresses and profile data in database\n# If deleted database will need to also be reset!\n# To regenerate, delete this line and restart the server");

        addComment("\n###########################\n"
                + "#    Fallback Settings    #\n"
                + "###########################");
        setProperty("fallback-enabled", String.valueOf(DEFAULT_FALLBACK_ENABLED), "Enable session fallback when Mojang servers are down");
        setProperty("max-offline-hours", String.valueOf(DEFAULT_MAX_OFFLINE_HOURS), "Maximum hours a player can stay offline before requiring re-authentication (0 makes medium level act like basic)");
        setProperty("cleanup-days", String.valueOf(DEFAULT_CLEANUP_DAYS), "Days before old session data is cleaned up");
        setProperty("security-level", "basic", "Security level: 'basic' (IP check only) or 'medium' (IP Check + max-offline-hours limit)");
        setProperty("upstream-server", "https://sessionserver.mojang.com", "Upstream Session Server URL\n# Default is Mojang's official one but this option is here to work with things like minehut's external servers");

        addComment("\n###########################\n"
                + "#    Database Settings    #\n"
                + "###########################");
        setProperty("database.type", DEFAULT_DB_TYPE, "Database type: h2, mysql, or mariadb");
        setProperty("database.host", DEFAULT_DB_HOST, "Database host (not used for H2)");
        setProperty("database.port", String.valueOf(DEFAULT_DB_PORT_MYSQL), "Database port (not used for H2)");
        setProperty("database.name", DEFAULT_DB_NAME, "Database name");
        setProperty("database.username", DEFAULT_DB_USERNAME, "Database username (not used for H2)");
        setProperty("database.password", DEFAULT_DB_PASSWORD, "Database password (not used for H2)");
    }

    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes).replaceAll("\\+", String.valueOf((char) (random.nextInt(26) + 'a')));
    }

    private void setProperty(String key, String value, String comment) {
        properties.put(key, value);
        if (comment != null && !comment.isEmpty()) {
            comments.put(key, comment);
        }
    }

    private void addComment(String commentText) {
        String commentKey = "__COMMENT__" + ThreadLocalRandom.current().nextInt();
        properties.put(commentKey, "");
        comments.put(commentKey, commentText);
    }

    public void saveConfig() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith("__COMMENT__")) {
                    if (comments.containsKey(key)) {
                        writer.write(comments.get(key));
                        writer.newLine();
                    }
                    writer.newLine();
                } else {
                    if (comments.containsKey(key)) {
                        writer.write("# " + comments.get(key));
                        writer.newLine();
                    }

                    writer.write(key + "=" + value);
                    writer.newLine();
                }
            }

            platform.sendLogMessage("Configuration saved to " + configFile.getName());
        } catch (IOException e) {
            platform.sendSevereLogMessage("Failed to save config: " + e.getMessage());
        }
    }

    public int getPort() {
        return Integer.parseInt(properties.getOrDefault("port", String.valueOf(DEFAULT_PORT)));
    }

    public Boolean getDebug() {
        return Boolean.parseBoolean(properties.getOrDefault("debug", String.valueOf(DEFAULT_DEBUG)));
    }

    public Boolean getUpdates() {
        return Boolean.parseBoolean(properties.getOrDefault("check-updates", String.valueOf(DEFAULT_UPDATE_CHECKS)));
    }

    public boolean isAuthenticationEnabled() {
        return Boolean.parseBoolean(properties.getOrDefault("authentication-enabled", String.valueOf(DEFAULT_AUTHENTICATION_ENABLED)));
    }

    public String getSecretKey() {
        return properties.getOrDefault("secret-key", generateSecretKey());
    }

    public boolean isFallbackEnabled() {
        return Boolean.parseBoolean(properties.getOrDefault("fallback-enabled", String.valueOf(DEFAULT_FALLBACK_ENABLED)));
    }

    public int getMaxOfflineHours() {
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
        return properties.getOrDefault("security-level", "basic").toLowerCase();
    }

    public String getSessionServerUrl() {
        if (getIpAddress().startsWith("http")) return getIpAddress() + "/auth?token=" + getSecretKey();
        else return "http://" + getIpAddress() + ":" + getPort() + "/auth?token=" + getSecretKey();
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

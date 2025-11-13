package me.gamerduck.alwaysauth.api;

import me.gamerduck.alwaysauth.Platform;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Configuration manager for AlwaysAuth session server settings.
 * <p>
 * Handles loading, saving, and accessing configuration properties from a config.properties file.
 * Automatically migrates old configurations by adding new properties, removing deprecated ones,
 * and maintaining property order.
 * </p>
 * <p>
 * Configuration includes:
 * <ul>
 *     <li>Server settings (IP address, port)</li>
 *     <li>Security settings (authentication, encryption, secret keys)</li>
 *     <li>Fallback settings (offline hours, cleanup days, security level)</li>
 *     <li>Database settings (type, host, port, credentials)</li>
 * </ul>
 * </p>
 */
public class SessionConfig {
    private final LinkedHashMap<String, String> properties;
    private final LinkedHashMap<String, String> comments;
    private final File configFile;
    private final Platform platform;

    private static final boolean DEFAULT_DEBUG = false;
    private static final boolean DEFAULT_UPDATE_CHECKS = true;

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

    /**
     * Constructs a new SessionConfig instance.
     * <p>
     * Creates or loads the configuration file from the specified data folder.
     * If the folder doesn't exist, it will be created. Automatically loads
     * configuration from disk or creates a new file with default values.
     * </p>
     *
     * @param dataFolder the directory where config.properties should be stored
     * @param platform the platform implementation for logging
     */
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

    /**
     * Loads configuration from disk and migrates if necessary.
     * <p>
     * If the config file exists, it reads all properties and comments, then:
     * <ul>
     *     <li>Adds any new properties from defaults</li>
     *     <li>Removes deprecated properties</li>
     *     <li>Updates property comments if changed</li>
     *     <li>Reorders properties to match defaults</li>
     * </ul>
     * If the file doesn't exist or an error occurs, creates a new config with default values.
     * </p>
     */
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

    /**
     * Sets all default configuration values and their comments.
     * <p>
     * This method defines the complete configuration schema including:
     * default values, comments for documentation, and the order properties
     * should appear in the config file.
     * </p>
     */
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
        // Server settings
        setProperty("ip-address", DEFAULT_IP_ADDRESS, "The ip for the session server\n# If set anything other than 127.0.0.1 or 0.0.0.0 (allows public access), it will treat as external server\n# An external server means only port needs to be set (to match that external server) and it will use that to authenticate.\n# Please note as of right now you will not see console logs on the server if you are using an external server");
        setProperty("port", String.valueOf(DEFAULT_PORT), "Port for the session server");

        // Security settings
        addComment("\n###########################\n"
                + "#    Security Settings    #\n"
                + "###########################");
        setProperty("authentication-enabled", String.valueOf(DEFAULT_AUTHENTICATION_ENABLED), "Enable HMAC-SHA256 signature verification for authorized servers\n# Currently DISABLED by default due to Minecraft URL handling limitations\n# Use firewall rules or localhost restriction for access control instead\n# Database encryption works regardless of this setting");
        setProperty("secret-key", generateSecretKey(), "Secret key for database encryption (auto-generated)\n# KEEP THIS SECRET! Used to encrypt IP addresses and profile data in database\n# If deleted database will need to also be reset!\n# To regenerate, delete this line and restart the server");

        addComment("\n###########################\n"
                + "#    Fallback Settings    #\n"
                + "###########################");
        // Fallback settings
        setProperty("fallback-enabled", String.valueOf(DEFAULT_FALLBACK_ENABLED), "Enable session fallback when Mojang servers are down");
        setProperty("max-offline-hours", String.valueOf(DEFAULT_MAX_OFFLINE_HOURS), "Maximum hours a player can stay offline before requiring re-authentication (0 = always require)");
        setProperty("cleanup-days", String.valueOf(DEFAULT_CLEANUP_DAYS), "Days before old session data is cleaned up");
        setProperty("security-level", "basic", "Security level: 'basic' (always verify) or 'medium' (use max-offline-hours)");
        setProperty("upstream-server", "https://sessionserver.mojang.com", "Upstream Session Server URL\n# Default is Mojang's official one but this option is here to work with things like minehut's external servers");

        addComment("\n###########################\n"
                + "#    Database Settings    #\n"
                + "###########################");
        // Database settings
        setProperty("database.type", DEFAULT_DB_TYPE, "Database type: h2, mysql, or mariadb");
        setProperty("database.host", DEFAULT_DB_HOST, "Database host (not used for H2)");
        setProperty("database.port", String.valueOf(DEFAULT_DB_PORT_MYSQL), "Database port (not used for H2)");
        setProperty("database.name", DEFAULT_DB_NAME, "Database name");
        setProperty("database.username", DEFAULT_DB_USERNAME, "Database username (not used for H2)");
        setProperty("database.password", DEFAULT_DB_PASSWORD, "Database password (not used for H2)");
    }

    /**
     * Generates a cryptographically secure random secret key.
     * <p>
     * Creates a 256-bit (32 byte) random key using SecureRandom, encodes it as Base64,
     * and replaces '+' characters with random lowercase letters to avoid URL encoding issues.
     * </p>
     *
     * @return a Base64-encoded 256-bit secret key
     */
    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes).replaceAll("\\+", String.valueOf((char) (random.nextInt(26) + 'a')));
    }

    /**
     * Sets a property with an optional comment.
     * <p>
     * Comments can contain multiple lines separated by newline characters.
     * The comment will appear above the property in the saved config file.
     * </p>
     *
     * @param key the property name
     * @param value the property value
     * @param comment the comment to display above the property, or null for no comment
     */
    private void setProperty(String key, String value, String comment) {
        properties.put(key, value);
        if (comment != null && !comment.isEmpty()) {
            comments.put(key, comment);
        }
    }

    /**
     * Adds a standalone comment or section header to the config file.
     * <p>
     * The comment will appear exactly where this method is called in relation to other
     * properties and comments, maintaining the order defined in setDefaults().
     * You handle all formatting yourself - the text you provide will be written
     * as-is to the config file without any modification or prefix.
     * </p>
     * <p>
     * This is useful for adding section headers or visual separators in the config file.
     * For example:
     * <pre>
     * addComment("# ========================================");
     * addComment("#           Database Settings");
     * addComment("# ========================================");
     * setProperty("database.type", "h2", "Database type");
     * </pre>
     * </p>
     *
     * @param commentText the text to write to the config file (you handle formatting)
     */
    private void addComment(String commentText) {
        String commentKey = "__COMMENT__" + ThreadLocalRandom.current().nextInt();
        properties.put(commentKey, "");
        comments.put(commentKey, commentText);
    }

    /**
     * Saves the current configuration to disk.
     * <p>
     * Writes all properties to the config file with a formatted header,
     * including comments above each property. Properties are written in
     * the order they were added to maintain a consistent file structure.
     * </p>
     */
    public void saveConfig() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Check if this is a header (standalone comment)
                if (key.startsWith("__COMMENT__")) {
                    // Only write the comment, not the property
                    if (comments.containsKey(key)) {
                        writer.write(comments.get(key));
                        writer.newLine();
                    }
                    writer.newLine();
                } else {
                    // Regular property - write comment if exists
                    if (comments.containsKey(key)) {
                        writer.write("# " + comments.get(key));
                        writer.newLine();
                    }

                    // Write property
                    writer.write(key + "=" + value);
                    writer.newLine();
                }
            }

            platform.sendLogMessage("Configuration saved to " + configFile.getName());
        } catch (IOException e) {
            platform.sendSevereLogMessage("Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Gets the server port number.
     *
     * @return the port number for the session proxy server
     */
    public int getPort() {
        return Integer.parseInt(properties.getOrDefault("port", String.valueOf(DEFAULT_PORT)));
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug logging is enabled, false otherwise
     */
    public Boolean getDebug() {
        return Boolean.parseBoolean(properties.getOrDefault("debug", String.valueOf(DEFAULT_DEBUG)));
    }

    /**
     * Checks if updates are enabled.
     *
     * @return true if updates are enabled, false otherwise
     */
    public Boolean getUpdates() {
        return Boolean.parseBoolean(properties.getOrDefault("check-updates", String.valueOf(DEFAULT_UPDATE_CHECKS)));
    }

    /**
     * Checks if authentication is enabled for the /auth endpoint.
     *
     * @return true if token authentication is required, false otherwise
     */
    public boolean isAuthenticationEnabled() {
        return Boolean.parseBoolean(properties.getOrDefault("authentication-enabled", String.valueOf(DEFAULT_AUTHENTICATION_ENABLED)));
    }

    /**
     * Gets the secret key used for authentication and encryption.
     *
     * @return the Base64-encoded secret key
     */
    public String getSecretKey() {
        return properties.getOrDefault("secret-key", generateSecretKey());
    }

    /**
     * Checks if fallback authentication is enabled.
     *
     * @return true if fallback to cached authentication is allowed when Mojang is down, false otherwise
     */
    public boolean isFallbackEnabled() {
        return Boolean.parseBoolean(properties.getOrDefault("fallback-enabled", String.valueOf(DEFAULT_FALLBACK_ENABLED)));
    }

    /**
     * Gets the maximum offline hours before requiring re-authentication.
     * <p>
     * Returns 0 if security level is "basic" (no time limit), otherwise returns
     * the configured max-offline-hours value for "medium" security level.
     * </p>
     *
     * @return the maximum hours a player can be offline, or 0 for no limit
     */
    public int getMaxOfflineHours() {
        String level = properties.getOrDefault("security-level", "basic");
        if ("basic".equalsIgnoreCase(level)) {
            return 0;
        }
        return Integer.parseInt(properties.getOrDefault("max-offline-hours", String.valueOf(DEFAULT_MAX_OFFLINE_HOURS)));
    }

    /**
     * Gets the upstream session server URL.
     *
     * @return the URL of the upstream Mojang session server
     */
    public String getUpstreamSessionServer() {
        return properties.getOrDefault("upstream-server", DEFAULT_UPSTREAM_SESSION_SERVER);
    }

    /**
     * Gets the IP address the session server binds to.
     *
     * @return the IP address (e.g., "127.0.0.1" or "0.0.0.0")
     */
    public String getIpAddress() {
        return properties.getOrDefault("ip-address", DEFAULT_IP_ADDRESS);
    }

    /**
     * Gets the number of days before old cache entries are cleaned up.
     *
     * @return the cleanup threshold in days
     */
    public int getCleanupDays() {
        return Integer.parseInt(properties.getOrDefault("cleanup-days", String.valueOf(DEFAULT_CLEANUP_DAYS)));
    }

    /**
     * Gets the security level setting.
     *
     * @return "basic" for always allowing cached auth, or "medium" for time-limited cache
     */
    public String getSecurityLevel() {
        return properties.getOrDefault("security-level", "basic");
    }

    /**
     * Constructs the full session server URL with authentication token.
     * <p>
     * This URL is used by Minecraft servers to connect to the local proxy.
     * Format: http://ip:port/auth?token=secretkey
     * </p>
     *
     * @return the complete session server URL including authentication token
     */
    public String getSessionServerUrl() {
        return "http://" + getIpAddress() + ":" + getPort() + "/auth?token=" + getSecretKey();
    }

    /**
     * Gets the database type.
     *
     * @return the database type ("h2", "mysql", or "mariadb")
     */
    public String getDatabaseType() {
        return properties.getOrDefault("database.type", DEFAULT_DB_TYPE);
    }

    /**
     * Gets the database host address.
     *
     * @return the database host (not used for H2)
     */
    public String getDatabaseHost() {
        return properties.getOrDefault("database.host", DEFAULT_DB_HOST);
    }

    /**
     * Gets the database port number.
     *
     * @return the database port (not used for H2)
     */
    public int getDatabasePort() {
        return Integer.parseInt(properties.getOrDefault("database.port", String.valueOf(DEFAULT_DB_PORT_MYSQL)));
    }

    /**
     * Gets the database name.
     *
     * @return the database/schema name
     */
    public String getDatabaseName() {
        return properties.getOrDefault("database.name", DEFAULT_DB_NAME);
    }

    /**
     * Gets the database username.
     *
     * @return the database username (not used for H2)
     */
    public String getDatabaseUsername() {
        return properties.getOrDefault("database.username", DEFAULT_DB_USERNAME);
    }

    /**
     * Gets the database password.
     *
     * @return the database password (not used for H2)
     */
    public String getDatabasePassword() {
        return properties.getOrDefault("database.password", DEFAULT_DB_PASSWORD);
    }

    /**
     * Sets the upstream session server URL.
     *
     * @param server the upstream Mojang session server URL
     */
    public void setUpstreamSessionServer(String server) {
        properties.put("upstream-server", server);
    }

    /**
     * Sets the IP address for the session server to bind to.
     *
     * @param ip the IP address (e.g., "127.0.0.1" or "0.0.0.0")
     */
    public void setIpAddress(String ip) {
        properties.put("ip-address", ip);
    }

    /**
     * Sets the session server port number.
     *
     * @param port the port number
     */
    public void setPort(int port) {
        properties.put("port", String.valueOf(port));
    }

    /**
     * Enables or disables authentication for the /auth endpoint.
     *
     * @param enabled true to require token authentication, false otherwise
     */
    public void setAuthenticationEnabled(boolean enabled) {
        properties.put("authentication-enabled", String.valueOf(enabled));
    }

    /**
     * Sets the secret key for authentication and encryption.
     *
     * @param key the Base64-encoded secret key
     */
    public void setSecretKey(String key) {
        properties.put("secret-key", key);
    }

    /**
     * Enables or disables profile data encryption.
     *
     * @param encrypt true to encrypt player profile data in the database, false otherwise
     */
    public void setEncryptProfileData(boolean encrypt) {
        properties.put("encrypt-profile-data", String.valueOf(encrypt));
    }

    /**
     * Enables or disables fallback authentication.
     *
     * @param enabled true to allow fallback to cached auth when Mojang is down, false otherwise
     */
    public void setFallbackEnabled(boolean enabled) {
        properties.put("fallback-enabled", String.valueOf(enabled));
    }

    /**
     * Sets the maximum offline hours before requiring re-authentication.
     *
     * @param hours the maximum hours a player can be offline
     */
    public void setMaxOfflineHours(int hours) {
        properties.put("max-offline-hours", String.valueOf(hours));
    }

    /**
     * Sets the security level.
     * <p>
     * Only accepts "basic" or "medium" (case-insensitive). Invalid values are ignored.
     * </p>
     *
     * @param level "basic" for always allowing cached auth, or "medium" for time-limited cache
     */
    public void setSecurityLevel(String level) {
        if ("basic".equalsIgnoreCase(level) || "medium".equalsIgnoreCase(level)) {
            properties.put("security-level", level.toLowerCase());
        }
    }

    /**
     * Sets the number of days before old cache entries are cleaned up.
     *
     * @param days the cleanup threshold in days
     */
    public void setCleanupDays(int days) {
        properties.put("cleanup-days", String.valueOf(days));
    }

    /**
     * Sets the database type.
     * <p>
     * Only accepts "sqlite", "mysql", "mariadb", or "postgresql" (case-insensitive).
     * Invalid values are ignored.
     * </p>
     *
     * @param type the database type
     */
    public void setDatabaseType(String type) {
        if ("sqlite".equalsIgnoreCase(type) || "mysql".equalsIgnoreCase(type) ||
                "mariadb".equalsIgnoreCase(type) || "postgresql".equalsIgnoreCase(type)) {
            properties.put("database.type", type.toLowerCase());
        }
    }

    /**
     * Sets the database host address.
     *
     * @param host the database host
     */
    public void setDatabaseHost(String host) {
        properties.put("database.host", host);
    }

    /**
     * Sets the database port number.
     *
     * @param port the database port
     */
    public void setDatabasePort(int port) {
        properties.put("database.port", String.valueOf(port));
    }

    /**
     * Sets the database name.
     *
     * @param name the database/schema name
     */
    public void setDatabaseName(String name) {
        properties.put("database.name", name);
    }

    /**
     * Sets the database username.
     *
     * @param username the database username
     */
    public void setDatabaseUsername(String username) {
        properties.put("database.username", username);
    }

    /**
     * Sets the database password.
     *
     * @param password the database password
     */
    public void setDatabasePassword(String password) {
        properties.put("database.password", password);
    }

    /**
     * Checks if a remote database is configured.
     *
     * @return true if using MySQL or MariaDB, false if using local H2 database
     */
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
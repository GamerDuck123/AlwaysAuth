package me.gamerduck.alwaysauth.api;

import me.gamerduck.alwaysauth.Platform;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration management for the session fallback system
 */
public class SessionConfig {
    private final Properties properties;
    private final File configFile;
    private final Platform platform;

    // Default values
    private static final String DEFAULT_EXTERNAL_DOMAIN = "http://127.0.0.1";
    private static final int DEFAULT_PORT = 8765;

    private static final boolean DEFAULT_FALLBACK_ENABLED = true;
    private static final int DEFAULT_MAX_OFFLINE_HOURS = 72; // 3 days
    private static final int DEFAULT_CLEANUP_DAYS = 30;

    private static final String DEFAULT_DB_TYPE = "sqlite";
    private static final String DEFAULT_DB_HOST = "localhost";
    private static final int DEFAULT_DB_PORT_MYSQL = 3306;
    private static final int DEFAULT_DB_PORT_POSTGRESQL = 5432;
    private static final String DEFAULT_DB_NAME = "minecraft";
    private static final String DEFAULT_DB_USERNAME = "root";
    private static final String DEFAULT_DB_PASSWORD = "";

    private static final String DEFAULT_UPSTREAM_SESSION_SERVER = "https://sessionserver.mojang.com";

    public SessionConfig(File dataFolder, Platform platform) {
        this.configFile = new File(dataFolder, "config.properties");
        this.platform = platform;
        this.properties = new Properties();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        loadConfig();
    }

    private void loadConfig() {
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                platform.sendLogMessage("Configuration loaded from " + configFile.getName());
            } catch (IOException e) {
                platform.sendSevereLogMessage("Failed to load config, using defaults: " + e.getMessage());
                setDefaults();
            }
        } else {
            setDefaults();
            saveConfig();
        }
    }

    private void setDefaults() {
        properties.setProperty("external-domain", DEFAULT_EXTERNAL_DOMAIN);
        properties.setProperty("port", String.valueOf(DEFAULT_PORT));
        properties.setProperty("fallback-enabled", String.valueOf(DEFAULT_FALLBACK_ENABLED));
        properties.setProperty("max-offline-hours", String.valueOf(DEFAULT_MAX_OFFLINE_HOURS));
        properties.setProperty("cleanup-days", String.valueOf(DEFAULT_CLEANUP_DAYS));
        properties.setProperty("security-level", "basic");
        properties.setProperty("upstream-server", "https://sessionserver.mojang.com");

        properties.setProperty("database.type", DEFAULT_DB_TYPE);
        properties.setProperty("database.host", DEFAULT_DB_HOST);
        properties.setProperty("database.port", String.valueOf(DEFAULT_DB_PORT_MYSQL));
        properties.setProperty("database.name", DEFAULT_DB_NAME);
        properties.setProperty("database.username", DEFAULT_DB_USERNAME);
        properties.setProperty("database.password", DEFAULT_DB_PASSWORD);
    }

    public void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "Session Fallback Configuration");
            platform.sendLogMessage("Configuration saved to " + configFile.getName());
        } catch (IOException e) {
            platform.sendSevereLogMessage("Failed to save config: " + e.getMessage());
        }
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("port", String.valueOf(DEFAULT_PORT)));
    }

    public boolean isFallbackEnabled() {
        return Boolean.parseBoolean(properties.getProperty("fallback-enabled", String.valueOf(DEFAULT_FALLBACK_ENABLED)));
    }

    public int getMaxOfflineHours() {
        String level = properties.getProperty("security-level", "basic");
        if ("basic".equalsIgnoreCase(level)) {
            return 0;
        }
        return Integer.parseInt(properties.getProperty("max-offline-hours", String.valueOf(DEFAULT_MAX_OFFLINE_HOURS)));
    }

    public String getUpstreamSessionServer() {
        return properties.getProperty("upstream-server", DEFAULT_UPSTREAM_SESSION_SERVER);
    }

    public void setUpstreamSessionServer(String server) {
        properties.setProperty("upstream-server", server);
    }

    public String getExternalIp() {
        return properties.getProperty("external-domain", DEFAULT_UPSTREAM_SESSION_SERVER);
    }

    public void setExternalIp(String ip) {
        properties.setProperty("external-domain", ip);
    }

    public int getCleanupDays() {
        return Integer.parseInt(properties.getProperty("cleanup-days", String.valueOf(DEFAULT_CLEANUP_DAYS)));
    }

    public String getSecurityLevel() {
        return properties.getProperty("security-level", "basic");
    }

    public void setPort(int port) {
        properties.setProperty("port", String.valueOf(port));
    }

    public void setFallbackEnabled(boolean enabled) {
        properties.setProperty("fallback-enabled", String.valueOf(enabled));
    }

    public void setMaxOfflineHours(int hours) {
        properties.setProperty("max-offline-hours", String.valueOf(hours));
    }

    public void setSecurityLevel(String level) {
        if ("basic".equalsIgnoreCase(level) || "medium".equalsIgnoreCase(level)) {
            properties.setProperty("security-level", level.toLowerCase());
        }
    }

    public void setCleanupDays(int days) {
        properties.setProperty("cleanup-days", String.valueOf(days));
    }

    public String getSessionServerUrl() {
        return getExternalIp() + ":" + getPort();
    }

    public String getDatabaseType() {
        return properties.getProperty("database.type", DEFAULT_DB_TYPE);
    }

    public String getDatabaseHost() {
        return properties.getProperty("database.host", DEFAULT_DB_HOST);
    }

    public int getDatabasePort() {
        String dbType = getDatabaseType();
        int defaultPort = dbType.equalsIgnoreCase("postgresql") ? DEFAULT_DB_PORT_POSTGRESQL : DEFAULT_DB_PORT_MYSQL;
        return Integer.parseInt(properties.getProperty("database.port", String.valueOf(defaultPort)));
    }

    public String getDatabaseName() {
        return properties.getProperty("database.name", DEFAULT_DB_NAME);
    }

    public String getDatabaseUsername() {
        return properties.getProperty("database.username", DEFAULT_DB_USERNAME);
    }

    public String getDatabasePassword() {
        return properties.getProperty("database.password", DEFAULT_DB_PASSWORD);
    }

    public void setDatabaseType(String type) {
        if ("sqlite".equalsIgnoreCase(type) || "mysql".equalsIgnoreCase(type) ||
                "mariadb".equalsIgnoreCase(type) || "postgresql".equalsIgnoreCase(type)) {
            properties.setProperty("database.type", type.toLowerCase());
        }
    }

    public void setDatabaseHost(String host) {
        properties.setProperty("database.host", host);
    }

    public void setDatabasePort(int port) {
        properties.setProperty("database.port", String.valueOf(port));
    }

    public void setDatabaseName(String name) {
        properties.setProperty("database.name", name);
    }

    public void setDatabaseUsername(String username) {
        properties.setProperty("database.username", username);
    }

    public void setDatabasePassword(String password) {
        properties.setProperty("database.password", password);
    }

    public boolean isRemoteDatabase() {
        return !getDatabaseType().equalsIgnoreCase("sqlite");
    }

    @Override
    public String toString() {
        return "SessionConfig{" +
                "external-ip=" + getExternalIp() +
                ", port=" + getPort() +
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
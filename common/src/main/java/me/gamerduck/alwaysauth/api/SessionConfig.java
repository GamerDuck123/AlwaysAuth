package me.gamerduck.alwaysauth.api;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration management for the session fallback system
 */
public class SessionConfig {
    private final Properties properties;
    private final File configFile;
    private final Logger logger;

    // Default values
    private static final int DEFAULT_PORT = 8765;
    private static final boolean DEFAULT_FALLBACK_ENABLED = true;
    private static final int DEFAULT_MAX_OFFLINE_HOURS = 72; // 3 days
    private static final int DEFAULT_CLEANUP_DAYS = 30;

    public SessionConfig(File dataFolder, Logger logger) {
        this.logger = logger;
        this.configFile = new File(dataFolder, "config.properties");
        this.properties = new Properties();

        // Create data folder if needed
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        loadConfig();
    }

    private void loadConfig() {
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                logger.info("Configuration loaded from " + configFile.getName());
            } catch (IOException e) {
                logger.warning("Failed to load config, using defaults: " + e.getMessage());
                setDefaults();
            }
        } else {
            setDefaults();
            saveConfig();
        }
    }

    private void setDefaults() {
        properties.setProperty("port", String.valueOf(DEFAULT_PORT));
        properties.setProperty("fallback-enabled", String.valueOf(DEFAULT_FALLBACK_ENABLED));
        properties.setProperty("max-offline-hours", String.valueOf(DEFAULT_MAX_OFFLINE_HOURS));
        properties.setProperty("cleanup-days", String.valueOf(DEFAULT_CLEANUP_DAYS));
        properties.setProperty("security-level", "basic");
    }

    public void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "Session Fallback Configuration");
            logger.info("Configuration saved to " + configFile.getName());
        } catch (IOException e) {
            logger.warning("Failed to save config: " + e.getMessage());
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
        return "http://127.0.0.1:" + getPort();
    }

    @Override
    public String toString() {
        return "SessionConfig{" +
                "port=" + getPort() +
                ", fallbackEnabled=" + isFallbackEnabled() +
                ", securityLevel=" + getSecurityLevel() +
                ", maxOfflineHours=" + getMaxOfflineHours() +
                ", cleanupDays=" + getCleanupDays() +
                '}';
    }
}
package me.gamerduck.alwaysauth;

import me.gamerduck.alwaysauth.api.AuthDatabase;
import me.gamerduck.alwaysauth.api.SessionConfig;
import me.gamerduck.alwaysauth.api.SessionProxyServer;
import me.gamerduck.alwaysauth.api.updates.ModrinthUpdateChecker;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Abstract base class for platform-specific AlwaysAuth implementations.
 * <p>
 * This class provides the core functionality for AlwaysAuth across different Minecraft platforms
 * (Paper, Spigot, Fabric, NeoForge, Velocity). Each platform extends this class and implements
 * platform-specific message sending and logging methods.
 * </p>
 * <p>
 * Manages the lifecycle of:
 * <ul>
 *     <li>Session proxy server for intercepting authentication requests</li>
 *     <li>Configuration management</li>
 *     <li>Command handlers for player/admin commands</li>
 * </ul>
 * </p>
 *
 * @param <CS> the platform's command sender type (e.g., CommandSender, Player, etc.)
 */
public abstract class Platform<CS> {
    private final SessionProxyServer proxyServer;
    private SessionConfig config;
    private final Path platformFolder;

    /**
     * Static instance accessible from mixin classes.
     * <p>
     * Required for mixin injection in Fabric/NeoForge where mixins cannot receive constructor parameters.
     * Set during Platform construction. Use with caution as this breaks type safety.
     * </p>
     */
    public static Platform<?> mixinOnly$instance;

    /**
     * Constructs a Platform instance and initializes AlwaysAuth.
     * <p>
     * Performs the following initialization:
     * <ol>
     *     <li>Loads configuration from the platform folder</li>
     *     <li>Creates and starts the session proxy server (if using local mode)</li>
     *     <li>Sets up authentication URL replacement via mixins or reflection</li>
     * </ol>
     * If the configured IP is 127.0.0.1 or 0.0.0.0, starts a local proxy server.
     * Otherwise, assumes an external session server is being used.
     * </p>
     *
     * @param platformFolder the directory for storing configuration and database files
     * @throws RuntimeException if initialization fails
     */
    public Platform(Path platformFolder) {
        this.platformFolder = platformFolder;
        mixinOnly$instance = this;
        try {
            config = new SessionConfig(platformFolder.toFile(), this);
            sendLogMessage("Configuration loaded: " + config);

            if (config.getIpAddress().equalsIgnoreCase("127.0.0.1")
                    || config.getIpAddress().equalsIgnoreCase("0.0.0.0")) {
                proxyServer = new SessionProxyServer(
                        config.getPort(),
                        platformFolder.toFile(),
                        this,
                        config
                );
                proxyServer.start();

                sendLogMessage("AlwaysAuth enabled! Proxy running on port " + config.getPort());
                sendLogMessage("Fallback mode: " + (config.isFallbackEnabled() ? "ENABLED" : "DISABLED"));
                sendLogMessage("Security level: " + config.getSecurityLevel().toUpperCase());
            } else {
                sendLogMessage("AlwaysAuth enabled! Using external domain " + config.getSessionServerUrl());
                proxyServer = null;
            }
        } catch (Exception e) {
            sendSevereLogMessage("Failed to enable AlwaysAuth" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a message to a command sender (player or console).
     * <p>
     * Platform implementations should handle color code formatting appropriately
     * for their platform (e.g., § codes for Bukkit, Component for Paper).
     * </p>
     *
     * @param commandSender the recipient of the message
     * @param msg the message to send (may contain color codes)
     */
    public abstract void sendMessage(CS commandSender, String msg);

    /**
     * Logs an info-level message to the platform's logger.
     *
     * @param msg the message to log
     */
    public abstract void sendLogMessage(String msg);

    /**
     * Logs a severe/error-level message to the platform's logger.
     *
     * @param msg the error message to log
     */
    public abstract void sendSevereLogMessage(String msg);

    /**
     * Logs a warning-level message to the platform's logger.
     *
     * @param msg the warning message to log
     */
    public abstract void sendWarningLogMessage(String msg);

    /**
     * Gets the update message if updates is enabled and there is a new update available.
     *
     * @return the update message
     */
    public String getUpdateMessage() {
        if (config.getUpdates() && ModrinthUpdateChecker.hasNewer()) {
            return "\u00A7cHey there is a new update for AlwaysAuth! " +
                    "\u00A7cPlease update soon for the latest and best features! https://modrinth.com/plugin/alwaysauth/versions";
        } else {
            return null;
        }
    }

    /**
     * Handles platform shutdown and cleanup.
     * <p>
     * Stops the session proxy server and closes database connections.
     * Should be called when the plugin/mod is disabled or the server shuts down.
     * </p>
     */
    public void onDisable() {
        if (proxyServer != null) {
            proxyServer.stop();
        }
        sendLogMessage("AlwaysAuth disabled");
    }

    /**
     * Checks if debug mode is enabled.
     * <p>
     * Returns true if debug is enabled in config OR if using an external session server
     * (proxyServer is null), as external server mode requires verbose logging.
     * </p>
     *
     * @return true if debug logging should be enabled, false otherwise
     */
    public Boolean isDebug() {
        return config.getDebug() || proxyServer == null;
    }

    /**
     * Handles the /alwaysauth status command.
     * <p>
     * Displays current configuration status including:
     * proxy port, fallback mode, security level, max offline time (if medium security),
     * and session server URL.
     * </p>
     *
     * @param player the command sender requesting status
     */
    public void cmdStatus(CS player) {
        sendMessage(player, "§6§lAlwaysAuth Status");
        sendMessage(player,"§7Proxy Port: §f" + config.getPort());
        sendMessage(player,"§7Fallback: " + (config.isFallbackEnabled() ? "§aENABLED" : "§cDISABLED"));
        sendMessage(player,"§7Security: §f" + config.getSecurityLevel().toUpperCase());
        if (config.getSecurityLevel().equals("medium")) {
            sendMessage(player,"§7Max Offline Time: §f" + config.getMaxOfflineHours() + " hours");
        }
        sendMessage(player,"§7Session URL: §f" + config.getSessionServerUrl());
    }

    /**
     * Handles the /alwaysauth stats command.
     * <p>
     * Displays authentication cache statistics including:
     * total cached players and number of players active in the last 24 hours.
     * </p>
     *
     * @param player the command sender requesting statistics
     */
    public void cmdStats(CS player) {
        AuthDatabase.CacheStats stats = proxyServer.getDatabase().getStats();
        sendMessage(player,"§6§lCache Statistics");
        sendMessage(player,"§7Total Players: §f" + stats.totalPlayers);
        sendMessage(player,"§7Active (24h): §f" + stats.recentPlayers);
    }

    /**
     * Handles the /alwaysauth toggle command.
     * <p>
     * Toggles fallback authentication mode on/off and saves the configuration.
     * </p>
     *
     * @param player the command sender requesting the toggle
     */
    public void cmdToggle(CS player) {
        config.setFallbackEnabled(!config.isFallbackEnabled());
        config.saveConfig();
        sendMessage(player,"§6Fallback mode " + (config.isFallbackEnabled() ? "§aenabled" : "§cdisabled"));
    }

    /**
     * Handles the /alwaysauth security &lt;level&gt; command.
     * <p>
     * Sets the security level to "basic" or "medium". Basic allows cached authentication
     * indefinitely, while medium enforces a time limit (max-offline-hours).
     * </p>
     *
     * @param player the command sender setting security level
     * @param level the security level ("basic" or "medium")
     */
    public void cmdSecurity(CS player, String level) {
        if (!level.equals("basic") && !level.equals("medium")) {
            sendMessage(player,"§cInvalid security level. Use 'basic' or 'medium'");
        }
        config.setSecurityLevel(level);
        config.saveConfig();
        sendMessage(player,"§6Security level set to: §f" + level.toUpperCase());
        if (level.equals("medium")) {
            sendMessage(player,"§7Players must have logged in within " + config.getMaxOfflineHours() + " hours");
        }
    }

    /**
     * Handles the /alwaysauth cleanup command.
     * <p>
     * Removes old authentication cache entries that haven't been used in the configured
     * number of days (cleanup-days setting). Reports the number of entries deleted.
     * </p>
     *
     * @param player the command sender requesting cleanup
     */
    public void cmdCleanup(CS player) {
        int cleaned = proxyServer.getDatabase().cleanOldEntries(config.getCleanupDays());
        sendMessage(player,"§6Cleaned §f" + cleaned + "§6 old entries (older than " + config.getCleanupDays() + " days)");
    }

    /**
     * Handles the /alwaysauth reload command.
     * <p>
     * Reloads the configuration from disk. Note that this does not restart the
     * proxy server or re-establish database connections.
     * </p>
     *
     * @param player the command sender requesting reload
     */
    public void cmdReload(CS player) {
        config = new SessionConfig(platformFolder.toFile(), this);
        sendMessage(player,"§6Configuration reloaded");
    }

    /**
     * Handles unknown /alwaysauth subcommands.
     * <p>
     * Displays an error message directing the user to use /alwaysauth for help.
     * </p>
     *
     * @param player the command sender who used an invalid subcommand
     */
    public void cmdDefault(CS player) {
        sendMessage(player,"§cUnknown subcommand. Use /alwaysauth for help");
    }

    /**
     * Handles the /alwaysauth or /alwaysauth help command.
     * <p>
     * Displays a list of all available AlwaysAuth commands and their descriptions.
     * </p>
     *
     * @param player the command sender requesting help
     */
    public void cmdHelp(CS player) {
        sendMessage(player,"§6§lAlways Auth");
        sendMessage(player,"§7Commands:");
        sendMessage(player,"§e/alwaysauth status §7- Show current status");
        sendMessage(player,"§e/alwaysauth stats §7- Show cache statistics");
        sendMessage(player,"§e/alwaysauth toggle §7- Enable/disable fallback");
        sendMessage(player,"§e/alwaysauth security <basic|medium> §7- Set security level");
        sendMessage(player,"§e/alwaysauth cleanup §7- Clean old cache entries");
        sendMessage(player,"§e/alwaysauth reload §7- Reload configuration");
    }

    /**
     * Gets the session proxy server instance.
     * <p>
     * Returns null if using an external session server (when IP is not 127.0.0.1 or 0.0.0.0).
     * </p>
     *
     * @return the SessionProxyServer instance, or null if using external server
     */
    public SessionProxyServer proxyServer() {
        return proxyServer;
    }

    /**
     * Gets the current configuration.
     *
     * @return the SessionConfig instance
     */
    public SessionConfig config() {
        return config;
    }

    /**
     * Gets the platform's data folder path.
     *
     * @return the Path to the platform's configuration and data directory
     */
    public Path platformFolder() {
        return platformFolder;
    }

}

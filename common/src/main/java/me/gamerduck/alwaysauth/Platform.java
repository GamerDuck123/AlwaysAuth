package me.gamerduck.alwaysauth;

import me.gamerduck.alwaysauth.api.AuthDatabase;
import me.gamerduck.alwaysauth.api.SessionConfig;
import me.gamerduck.alwaysauth.api.SessionProxyServer;

import java.nio.file.Path;
import java.util.logging.Logger;

public abstract class Platform<CS> {
    private final SessionProxyServer proxyServer;
    private SessionConfig config;
    private final Path platformFolder;

    public static Platform<?> mixinOnly$instance;

    public Platform(Path platformFolder) {
        this.platformFolder = platformFolder;
        mixinOnly$instance = this;
        try {
            config = new SessionConfig(platformFolder.toFile(), this);
            sendLogMessage("Configuration loaded: " + config);

            if (config.getExternalIp().equalsIgnoreCase("http://127.0.0.1")) {
            proxyServer = new SessionProxyServer(
                    config.getPort(),
                    platformFolder.toFile(),
                    this,
                    config
            );
            proxyServer.start();

//            if (System.getProperty("minecraft.api.session.host").equalsIgnoreCase(config.getSessionServerUrl())) {
//                logger.warning("Could not automatically configure session server.");
//                logger.warning("Please add this to your server JVM arguments:");
//                logger.warning("-Dminecraft.api.session.host=" + config.getSessionServerUrl());
//            }

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

    public abstract void sendMessage(CS commandSender, String msg);
    public abstract void sendLogMessage(String msg);
    public abstract void sendSevereLogMessage(String msg);
    public abstract void sendWarningLogMessage(String msg);

    public void onDisable() {
        if (proxyServer != null) {
            proxyServer.stop();
        }
        sendLogMessage("AlwaysAuth disabled");
    }

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

    public void cmdStats(CS player) {
        AuthDatabase.CacheStats stats = proxyServer.getDatabase().getStats();
        sendMessage(player,"§6§lCache Statistics");
        sendMessage(player,"§7Total Players: §f" + stats.totalPlayers);
        sendMessage(player,"§7Active (24h): §f" + stats.recentPlayers);
    }

    public void cmdToggle(CS player) {
        config.setFallbackEnabled(!config.isFallbackEnabled());
        config.saveConfig();
        sendMessage(player,"§6Fallback mode " + (config.isFallbackEnabled() ? "§aenabled" : "§cdisabled"));
    }

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

    public void cmdCleanup(CS player) {
        int cleaned = proxyServer.getDatabase().cleanOldEntries(config.getCleanupDays());
        sendMessage(player,"§6Cleaned §f" + cleaned + "§6 old entries (older than " + config.getCleanupDays() + " days)");
    }

    public void cmdReload(CS player) {
        config = new SessionConfig(platformFolder.toFile(), this);
        sendMessage(player,"§6Configuration reloaded");
    }

    public void cmdDefault(CS player) {
        sendMessage(player,"§cUnknown subcommand. Use /alwaysauth for help");
    }

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

    public SessionProxyServer proxyServer() {
        return proxyServer;
    }

    public SessionConfig config() {
        return config;
    }

    public Path platformFolder() {
        return platformFolder;
    }

}

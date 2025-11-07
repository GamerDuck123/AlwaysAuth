package me.gamerduck.alwaysauth.spigot;

import me.gamerduck.alwaysauth.Platform;
import me.gamerduck.alwaysauth.reflection.AuthenticationURLReplacer;
import me.gamerduck.alwaysauth.reflection.ServerPropertiesReplacer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class SpigotPlatform extends Platform<CommandSender> {

    private final Logger LOGGER;

    public SpigotPlatform(JavaPlugin bootstrap) {
        super(bootstrap.getDataFolder().toPath());

        this.LOGGER = bootstrap.getLogger();

        AuthenticationURLReplacer.replaceSessionService(this, config().getSessionServerUrl());
        ServerPropertiesReplacer.forcePreventProxyConnections(this);

        bootstrap.getCommand("alwaysauth").setExecutor((commandSender, command, s, args) -> {
            if (args.length == 0) {
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "status" -> cmdStatus(commandSender);
                case "stats" -> cmdStats(commandSender);
                case "toggle" -> cmdToggle(commandSender);
                case "security" -> {
                    if (args.length < 2) {
                        sendMessage(commandSender, "§cUsage: /alwaysauth security <basic|medium>");
                        return true;
                    }
                    String level = args[1].toLowerCase();
                    cmdSecurity(commandSender, level);
                }
                case "cleanup" -> cmdCleanup(commandSender);
                case "reload" -> cmdReload(commandSender);
                default -> cmdDefault(commandSender);
            }

            return true;
        });

    }

    @Override
    public void sendMessage(CommandSender commandSender, String msg) {
        commandSender.sendMessage(msg);
    }

    @Override
    public void sendLogMessage(String msg) {
        LOGGER.info(msg);
    }

    @Override
    public void sendSevereLogMessage(String msg) {
        LOGGER.severe(msg);
    }

    @Override
    public void sendWarningLogMessage(String msg) {
        LOGGER.warning(msg);
    }

}

package me.gamerduck.alwaysauth.spigot;

import me.gamerduck.alwaysauth.Platform;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotPlatform extends Platform<Player, CommandSender> {
    public SpigotPlatform(JavaPlugin bootstrap) {
        super(bootstrap.getDataFolder().toPath(), bootstrap.getLogger());

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

}

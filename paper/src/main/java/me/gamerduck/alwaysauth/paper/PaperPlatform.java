package me.gamerduck.alwaysauth.paper;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.gamerduck.alwaysauth.Platform;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class PaperPlatform extends Platform<CommandSourceStack> {
    public PaperPlatform(JavaPlugin bootstrap) {
        super(bootstrap.getDataFolder().toPath(), bootstrap.getLogger());

        bootstrap.registerCommand("alwaysauth", List.of("aa", "alwaysa"), (commandSourceStack, args) -> {
            if (commandSourceStack.getSender().hasPermission("alwaysauth.admin")) {
                if (args.length == 0) {
                    cmdHelp(commandSourceStack);
                    return;
                }
                switch (args[0].toLowerCase()) {
                    case "status" -> cmdStatus(commandSourceStack);
                    case "stats" -> cmdStats(commandSourceStack);
                    case "toggle" -> cmdToggle(commandSourceStack);
                    case "security" -> {
                        if (args.length < 2) {
                            sendMessage(commandSourceStack, "§cUsage: /alwaysauth security <basic|medium>");
                            return;
                        }
                        String level = args[1].toLowerCase();
                        cmdSecurity(commandSourceStack, level);
                    }
                    case "cleanup" -> cmdCleanup(commandSourceStack);
                    case "reload" -> cmdReload(commandSourceStack);
                    default -> cmdDefault(commandSourceStack);
                }

                return;
            } else {
                sendMessage(commandSourceStack, "§cNo permissions");
            }
        });


    }

    @Override
    public void sendMessage(CommandSourceStack commandSender, String msg) {
        commandSender.getSender().sendMessage(msg);
    }

}

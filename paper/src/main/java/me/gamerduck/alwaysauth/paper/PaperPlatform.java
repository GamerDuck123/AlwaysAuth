package me.gamerduck.alwaysauth.paper;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.gamerduck.alwaysauth.Platform;
import me.gamerduck.alwaysauth.api.updates.ModrinthUpdateChecker;
import me.gamerduck.alwaysauth.reflection.AuthenticationURLReplacer;
import me.gamerduck.alwaysauth.reflection.ServerPropertiesReplacer;
import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Logger;

public class PaperPlatform extends Platform<CommandSourceStack> implements Listener {

    private static final Logger LOGGER = Logger.getLogger("AlwaysAuth");
    private final JavaPlugin plugin;

    public PaperPlatform(JavaPlugin bootstrap) {
        super(bootstrap.getDataFolder().toPath());
        this.plugin = bootstrap;

        String message = getUpdateMessage();
        if (message != null) sendLogMessage(message);

        AuthenticationURLReplacer.replaceSessionService(this, config().getSessionServerUrl());
        ServerPropertiesReplacer.forcePreventProxyConnections(this);

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

    @Override
    public void sendLogMessage(String msg) {
        LOGGER.info(msg.replaceAll("§.", ""));
    }

    @Override
    public void sendSevereLogMessage(String msg) {
        LOGGER.severe(msg.replaceAll("§.", ""));
    }

    @Override
    public void sendWarningLogMessage(String msg) {
        LOGGER.warning(msg.replaceAll("§.", ""));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("alwaysauth.admin")) {
            String message = getUpdateMessage();
            if (message != null) event.getPlayer().sendMessage(message);
        }
    }

}

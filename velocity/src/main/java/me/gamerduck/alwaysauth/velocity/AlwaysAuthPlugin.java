package me.gamerduck.alwaysauth.velocity;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.configuration.PlayerFinishedConfigurationEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import me.gamerduck.alwaysauth.velocity.api.VelocityLibraryResolver;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(id = "alwaysauth", name = "AlwaysAuth", version = "0.0.1",
        url = "https://github.com/GamerDuck123/AlwaysAuth", description = "Keep your server always online even when mojang isn't!", authors = {"GamerDuck123"})
public class AlwaysAuthPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private static VelocityPlatform velocityPlatform;

    @Inject
    public AlwaysAuthPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        CommandManager commandManager = server.getCommandManager();
        commandManager.register(commandManager
                        .metaBuilder("alwaysauth")
                        .aliases("aa", "alwaysa")
                        .plugin(this)
                        .build(),
                new BrigadierCommand(BrigadierCommand.literalArgumentBuilder("alwaysauth")
                        .requires(source -> source.hasPermission("alwaysauth.admin"))
                        .executes(context -> {
                            velocityPlatform.cmdHelp(context.getSource());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.literalArgumentBuilder("status")
                                .executes(context -> {
                                    velocityPlatform.cmdStatus(context.getSource());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(BrigadierCommand.literalArgumentBuilder("stats")
                                .executes(context -> {
                                    velocityPlatform.cmdStats(context.getSource());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(BrigadierCommand.literalArgumentBuilder("toggle")
                                .executes(context -> {
                                    velocityPlatform.cmdToggle(context.getSource());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(BrigadierCommand.literalArgumentBuilder("security")
                                .then(BrigadierCommand.literalArgumentBuilder("basic")
                                        .executes(context -> {
                                            velocityPlatform.cmdSecurity(context.getSource(), "basic");
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(BrigadierCommand.literalArgumentBuilder("medium")
                                        .executes(context -> {
                                            velocityPlatform.cmdSecurity(context.getSource(), "medium");
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(BrigadierCommand.literalArgumentBuilder("cleanup")
                                .executes(context -> {
                                    velocityPlatform.cmdCleanup(context.getSource());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(BrigadierCommand.literalArgumentBuilder("reload")
                                .executes(context -> {
                                    velocityPlatform.cmdReload(context.getSource());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .build()));
    }
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Path libraries  = dataDirectory.resolve("libraries");
        if (Files.notExists(libraries)) {
            try {
                Files.createDirectories(libraries);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        VelocityLibraryResolver resolver = new VelocityLibraryResolver();
        resolver.addDependency("com.h2database:h2:2.3.232");
        resolver.addDependency("com.mysql:mysql-connector-j:8.0.33");
        resolver.addDependency("org.mariadb.jdbc:mariadb-java-client:3.3.2");
        resolver.addRepository("https://repo.papermc.io/repository/maven-public/");
        try {
            resolver.resolveDependencies(libraries, server.getPluginManager(), this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            velocityPlatform = new VelocityPlatform(server, logger, dataDirectory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyShutdownEvent event) {
        velocityPlatform.onDisable();
    }

    @Subscribe
    public void onPlayerJoin(PlayerFinishedConfigurationEvent event) {
        if (event.player().hasPermission("alwaysauth.admin")) {
            velocityPlatform.getUpdateMessage().ifPresent(msg -> event.player().sendMessage(Component.text(msg)));
        }
    }
}
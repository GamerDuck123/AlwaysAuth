package me.gamerduck.alwaysauth.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class AlwaysAuthMod implements ModInitializer {
    private static FabricPlatform fabricPlatform;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("alwaysauth")
                    .requires(source -> source.hasPermission(4))
                    .executes(context -> {
                        fabricPlatform.cmdHelp(context.getSource());
                        return 1;
                    })
                    .then(Commands.literal("status")
                            .executes(context -> {
                                fabricPlatform.cmdStatus(context.getSource());
                                return 1;
                            })
                    )
                    .then(Commands.literal("stats")
                            .executes(context -> {
                                fabricPlatform.cmdStats(context.getSource());
                                return 1;
                            })
                    )
                    .then(Commands.literal("toggle")
                            .executes(context -> {
                                fabricPlatform.cmdToggle(context.getSource());
                                return 1;
                            })
                    )
                    .then(Commands.literal("security")
                            .then(Commands.literal("basic")
                                    .executes(context -> {
                                        fabricPlatform.cmdSecurity(context.getSource(), "basic");
                                        return 1;
                                    })
                            )
                            .then(Commands.literal("medium")
                                    .executes(context -> {
                                        fabricPlatform.cmdSecurity(context.getSource(), "medium");
                                        return 1;
                                    })
                            )
                    )
                    .then(Commands.literal("cleanup")
                            .executes(context -> {
                                fabricPlatform.cmdCleanup(context.getSource());
                                return 1;
                            })
                    )
                    .then(Commands.literal("reload")
                            .executes(context -> {
                                fabricPlatform.cmdReload(context.getSource());
                                return 1;
                            })
                    )
            );
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (server.isDedicatedServer()) {
                try {
                    fabricPlatform = new FabricPlatform(server);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (server.isDedicatedServer()) {
                fabricPlatform.onDisable();
            }
        });
        ServerPlayerEvents.JOIN.register(player -> {
            if (player.getPermissionLevel() >= 4) {
                String message = fabricPlatform.getUpdateMessage();
                if (message != null) player.sendSystemMessage(Component.literal(message));
            }
        });
    }

}

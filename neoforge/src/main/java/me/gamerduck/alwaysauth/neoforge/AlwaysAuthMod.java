package me.gamerduck.alwaysauth.neoforge;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(AlwaysAuthMod.MODID)
public class AlwaysAuthMod {
    public static final String MODID = "alwaysauth";
    private static NeoForgePlatform neoForgePlatform;

    public AlwaysAuthMod() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (event.getServer().isDedicatedServer()) {
            try {
                neoForgePlatform = new NeoForgePlatform(event.getServer());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
        neoForgePlatform.onDisable();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().getPermissionLevel() >= 4) {
            String message = neoForgePlatform.getUpdateMessage();
            if (message != null) ((ServerPlayer) event.getEntity()).sendSystemMessage(Component.literal(message));
        }
    }

    @SubscribeEvent
    public void onCommandRegistrationEvent(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("alwaysauth")
                .requires(source -> source.hasPermission(4))
                .executes(context -> {
                    neoForgePlatform.cmdHelp(context.getSource());
                    return 1;
                })
                .then(Commands.literal("status")
                        .executes(context -> {
                            neoForgePlatform.cmdStatus(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("stats")
                        .executes(context -> {
                            neoForgePlatform.cmdStats(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("toggle")
                        .executes(context -> {
                            neoForgePlatform.cmdToggle(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("security")
                        .then(Commands.literal("basic")
                                .executes(context -> {
                                    neoForgePlatform.cmdSecurity(context.getSource(), "basic");
                                    return 1;
                                })
                        )
                        .then(Commands.literal("medium")
                                .executes(context -> {
                                    neoForgePlatform.cmdSecurity(context.getSource(), "medium");
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("cleanup")
                        .executes(context -> {
                            neoForgePlatform.cmdCleanup(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("reload")
                        .executes(context -> {
                            neoForgePlatform.cmdReload(context.getSource());
                            return 1;
                        })
                )
        );
    }

}
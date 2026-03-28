package me.gamerduck.alwaysauth.neoforgeA1204B1206.mixins;

import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import me.gamerduck.alwaysauth.neoforgeA1204B1206.NeoForgePlatform;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.net.URL;

@Mixin(MinecraftServer.class)
public abstract class DedicatedServerMixin {

    @Unique
    private static NeoForgePlatform platform;

    @Inject(method = "stopServer", at = @At(value = "RETURN"))
    private void injectedShutdown(CallbackInfo ci) {
        MinecraftServer self = (MinecraftServer)(Object) this;
        if (self.isDedicatedServer()) {
            platform.onDisable();
        }
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void injected(CallbackInfo ci) {
        MinecraftServer self = (MinecraftServer)(Object) this;
        if (platform == null) {
            try {
                platform = new NeoForgePlatform();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        replaceAuthUrls(self);

        self.getCommands().getDispatcher().register(Commands.literal("alwaysauth")
                .requires(source -> source.hasPermission(4))
                .executes(context -> {
                    platform.cmdHelp(context.getSource());
                    return 1;
                })
                .then(Commands.literal("status")
                        .executes(context -> {
                            platform.cmdStatus(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("stats")
                        .executes(context -> {
                            platform.cmdStats(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("toggle")
                        .executes(context -> {
                            platform.cmdToggle(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("security")
                        .then(Commands.literal("basic")
                                .executes(context -> {
                                    platform.cmdSecurity(context.getSource(), "basic");
                                    return 1;
                                })
                        )
                        .then(Commands.literal("medium")
                                .executes(context -> {
                                    platform.cmdSecurity(context.getSource(), "medium");
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("cleanup")
                        .executes(context -> {
                            platform.cmdCleanup(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("reload")
                        .executes(context -> {
                            platform.cmdReload(context.getSource());
                            return 1;
                        })
                )
        );
    }

    public void replaceAuthUrls(MinecraftServer server) {
        try {
            Field servicesField = MinecraftServer.class.getDeclaredField("services");
            servicesField.setAccessible(true);
            Object services = servicesField.get(server);

            YggdrasilMinecraftSessionService sessionService = (YggdrasilMinecraftSessionService)
                    services.getClass().getMethod("sessionService").invoke(services);

            String oldBase = "https://sessionserver.mojang.com";
            String newBase = platform.config().getSessionServerUrl();

            for (Field field : YggdrasilMinecraftSessionService.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(sessionService);
                if (value instanceof String str && str.contains("sessionserver.mojang.com")) {
                    field.set(sessionService, str.replace(oldBase, newBase));
                } else if (value instanceof URL url && url.toString().contains("sessionserver.mojang.com")) {
                    field.set(sessionService, new URL(url.toString().replace(oldBase, newBase)));
                }
            }

            platform.sendLogMessage("Successfully replaced authentication URLs with " + newBase);
        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to replace authentication URLs: " + e.getMessage());
        }
    }
}

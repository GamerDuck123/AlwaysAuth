package me.gamerduck.alwaysauth.fabric.mixins;

import me.gamerduck.alwaysauth.fabric.FabricPlatform;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class DedicatedServerMixin {

    @Inject(method = "stopServer", at = @At(value = "RETURN"))
    private void injectedShutdown(CallbackInfo ci) {
        MinecraftServer self = (MinecraftServer)(Object) this;
        if (self.isDedicatedServer()) {
            FabricPlatform.instance.onDisable();
        }
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void injected(CallbackInfo ci) {

        MinecraftServer self = (MinecraftServer)(Object) this;
        self.getCommands().getDispatcher().register(Commands.literal("alwaysauth")
                .requires(source -> FabricPlatform.instance.hasPermission(source, ""))
                .executes(context -> {
                    FabricPlatform.instance.cmdHelp(context.getSource());
                    return 1;
                })
                .then(Commands.literal("status")
                        .executes(context -> {
                            FabricPlatform.instance.cmdStatus(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("stats")
                        .executes(context -> {
                            FabricPlatform.instance.cmdStats(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("toggle")
                        .executes(context -> {
                            FabricPlatform.instance.cmdToggle(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("security")
                        .then(Commands.literal("basic")
                                .executes(context -> {
                                    FabricPlatform.instance.cmdSecurity(context.getSource(), "basic");
                                    return 1;
                                })
                        )
                        .then(Commands.literal("medium")
                                .executes(context -> {
                                    FabricPlatform.instance.cmdSecurity(context.getSource(), "medium");
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("cleanup")
                        .executes(context -> {
                            FabricPlatform.instance.cmdCleanup(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("reload")
                        .executes(context -> {
                            FabricPlatform.instance.cmdReload(context.getSource());
                            return 1;
                        })
                )
        );
    }
}

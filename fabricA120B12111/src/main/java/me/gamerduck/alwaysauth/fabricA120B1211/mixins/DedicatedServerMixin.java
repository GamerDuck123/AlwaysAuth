package me.gamerduck.alwaysauth.fabricA120B1211.mixins;

import me.gamerduck.alwaysauth.fabricA120B1211.FabricPlatform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class DedicatedServerMixin {

    @Inject(method = "shutdown", at = @At(value = "RETURN"))
    private void injectedShutdown(CallbackInfo ci) {
        MinecraftServer self = (MinecraftServer)(Object) this;
        if (self.isDedicated()) {
            FabricPlatform.instance.onDisable();
        }
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void injected(CallbackInfo ci) {

        MinecraftServer self = (MinecraftServer)(Object) this;
        self.getCommandManager().getDispatcher().register(CommandManager.literal("alwaysauth")
                .requires(source -> source.hasPermissionLevel(4))
                .executes(context -> {
                    FabricPlatform.instance.cmdHelp(context.getSource());
                    return 1;
                })
                .then(CommandManager.literal("status")
                        .executes(context -> {
                            FabricPlatform.instance.cmdStatus(context.getSource());
                            return 1;
                        })
                )
                .then(CommandManager.literal("stats")
                        .executes(context -> {
                            FabricPlatform.instance.cmdStats(context.getSource());
                            return 1;
                        })
                )
                .then(CommandManager.literal("toggle")
                        .executes(context -> {
                            FabricPlatform.instance.cmdToggle(context.getSource());
                            return 1;
                        })
                )
                .then(CommandManager.literal("security")
                        .then(CommandManager.literal("basic")
                                .executes(context -> {
                                    FabricPlatform.instance.cmdSecurity(context.getSource(), "basic");
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("medium")
                                .executes(context -> {
                                    FabricPlatform.instance.cmdSecurity(context.getSource(), "medium");
                                    return 1;
                                })
                        )
                )
                .then(CommandManager.literal("cleanup")
                        .executes(context -> {
                            FabricPlatform.instance.cmdCleanup(context.getSource());
                            return 1;
                        })
                )
                .then(CommandManager.literal("reload")
                        .executes(context -> {
                            FabricPlatform.instance.cmdReload(context.getSource());
                            return 1;
                        })
                )
        );
    }
}

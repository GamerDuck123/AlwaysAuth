package me.gamerduck.alwaysauth.fabric.mixins;

import me.gamerduck.alwaysauth.fabric.FabricPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "addPlayer", at = @At(value = "HEAD"))
    private void injectAddPlayer(ServerPlayer serverPlayer, CallbackInfo ci) {
        if (FabricPlatform.instance.hasPermission(serverPlayer.createCommandSourceStack(), "")) {
            FabricPlatform.instance.getUpdateMessage().ifPresent(msg ->
                    //? if >=1.19 {
                    serverPlayer.sendSystemMessage(Component.literal(msg))
                    //?} else
                    //serverPlayer.displayClientMessage(new net.minecraft.network.chat.TextComponent(msg), false)
            );
        }
    }
}

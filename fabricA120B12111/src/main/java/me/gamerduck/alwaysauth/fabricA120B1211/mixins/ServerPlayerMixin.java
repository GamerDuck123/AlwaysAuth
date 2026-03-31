package me.gamerduck.alwaysauth.fabricA120B1211.mixins;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "onSpawn", at = @At(value = "HEAD"))
    private void injected(CallbackInfo ci) {
//        if() {
//            Platform.mixinOnly$instance.getUpdateMessage().ifPresent(msg -> player.sendSystemMessage(Component.literal(msg)));
//        }
    }
}

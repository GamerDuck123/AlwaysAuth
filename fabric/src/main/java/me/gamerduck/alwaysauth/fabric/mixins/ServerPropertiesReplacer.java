package me.gamerduck.alwaysauth.fabric.mixins;

import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DedicatedServerProperties.class)
public abstract class ServerPropertiesReplacer {

    @Shadow
    public boolean preventProxyConnections = true;

//    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/server/dedicated/DedicatedServerProperties;preventProxyConnections:Z", opcode = 181))
//    private void injectPreventProxyConnections(DedicatedServerProperties instance, boolean value) {
//        instance.preventProxyConnections = true;
//    }
//
//    @ModifyConstant(constant = @Constant(stringValue = "Lnet/minecraft/server/dedicated/DedicatedServerProperties;preventProxyConnections:Z"))
//    private boolean injected(boolean value) {
//        return true;
//    }
}

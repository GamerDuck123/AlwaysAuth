package me.gamerduck.alwaysauth.neoforge.mixins;

import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DedicatedServerProperties.class)
public abstract class ServerPropertiesReplacer {

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/server/dedicated/DedicatedServerProperties;preventProxyConnections:Z", opcode = 181))
    private void injected(DedicatedServerProperties instance, boolean value) {
        instance.preventProxyConnections = true;
    }
}

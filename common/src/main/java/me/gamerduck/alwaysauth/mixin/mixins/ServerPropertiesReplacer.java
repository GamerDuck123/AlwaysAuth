package me.gamerduck.alwaysauth.mixin.mixins;

import com.mojang.authlib.Environment;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.Settings;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Proxy;
import java.util.Properties;

@Mixin(DedicatedServerProperties.class)
public abstract class ServerPropertiesReplacer {

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/server/dedicated/DedicatedServerProperties;preventProxyConnections:Z", opcode = Opcodes.PUTFIELD))
    private void injected(DedicatedServerProperties instance, boolean value) {
        instance.preventProxyConnections = true;
    }
}

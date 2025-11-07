package me.gamerduck.alwaysauth.mixin.mixins;

import com.mojang.authlib.Environment;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.net.Proxy;

@Mixin(YggdrasilAuthenticationService.class)
public abstract class AuthenticationURLReplacer extends HttpAuthenticationService {

    protected AuthenticationURLReplacer(Proxy proxy) {
        super(proxy);
    }

    @ModifyArg(method = "createMinecraftSessionService", remap = false, at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService;<init>(Lcom/mojang/authlib/yggdrasil/ServicesKeySet;Ljava/net/Proxy;Lcom/mojang/authlib/Environment;)V", remap = false), index = 2)
    private Environment injected(Environment enviro) {
        return new Environment("http://127.0.0.1:8765", enviro.servicesHost(), enviro.profilesHost(), enviro.name());
    }
}

package me.gamerduck.alwaysauth.fabric.mixins;


//? if >=1.20 {
import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
//?} else {
/*import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
*///?}
import com.mojang.authlib.HttpAuthenticationService;
import me.gamerduck.alwaysauth.fabric.FabricPlatform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.RecordComponent;

@Mixin(YggdrasilAuthenticationService.class)
public abstract class AuthenticationURLReplacer extends HttpAuthenticationService {

    @Unique
    private static FabricPlatform fabricPlatform;

    protected AuthenticationURLReplacer(java.net.Proxy proxy) {
        super(proxy);
    }

    @Inject(method = "<init>*", at = @At(value = "HEAD"))
    private static void injectedInit(CallbackInfo ci) {
        if (fabricPlatform == null) {
            try {
                fabricPlatform = new FabricPlatform();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    //? if >=1.20 {
    @ModifyArg(method = "createMinecraftSessionService", at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService;<init>(Lcom/mojang/authlib/yggdrasil/ServicesKeySet;Ljava/net/Proxy;Lcom/mojang/authlib/Environment;)V"), index = 2)
    private Environment injectedCreateMCSession(Environment enviro) {
        try {
            String newUrl = fabricPlatform.config().getSessionServerUrl();
            RecordComponent[] components = enviro.getClass().getRecordComponents();
            Object[] args = new Object[components.length];
            Class<?>[] types = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
                Object val = components[i].getAccessor().invoke(enviro);
                if (val instanceof String str && str.contains("sessionserver.mojang.com")) {
                    args[i] = newUrl;
                } else {
                    args[i] = val;
                }
            }
            return (Environment) enviro.getClass().getDeclaredConstructor(types).newInstance(args);
        } catch (Exception e) {
            return enviro;
        }
    }
    //?} else if >=1.16{
    
    /*@ModifyArg(method = "createMinecraftSessionService", remap = false, at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService;<init>(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Lcom/mojang/authlib/Environment;)V"))
    private YggdrasilAuthenticationService injectedCreateMCSession(YggdrasilAuthenticationService enviro) {
        try {
            String newUrl = fabricPlatform.config().getSessionServerUrl();
            RecordComponent[] components = enviro.getClass().getRecordComponents();
            Object[] args = new Object[components.length];
            Class<?>[] types = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
                Object val = components[i].getAccessor().invoke(enviro);
                if (val instanceof String str && str.contains("sessionserver.mojang.com")) {
                    args[i] = newUrl;
                } else {
                    args[i] = val;
                }
            }
            return (YggdrasilAuthenticationService) enviro.getClass().getDeclaredConstructor(types).newInstance(args);
        } catch (Exception e) {
            return enviro;
        }
    }

    *///?} else {
    /*@ModifyArg(method = "createMinecraftSessionService", remap = false, at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService;<init>(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;)V"))
    private YggdrasilAuthenticationService injectedCreateMCSession(YggdrasilAuthenticationService enviro) {
        try {
            String newUrl = fabricPlatform.config().getSessionServerUrl();
            RecordComponent[] components = enviro.getClass().getRecordComponents();
            Object[] args = new Object[components.length];
            Class<?>[] types = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
                Object val = components[i].getAccessor().invoke(enviro);
                if (val instanceof String str && str.contains("sessionserver.mojang.com")) {
                    args[i] = newUrl;
                } else {
                    args[i] = val;
                }
            }
            return (YggdrasilAuthenticationService) enviro.getClass().getDeclaredConstructor(types).newInstance(args);
        } catch (Exception e) {
            return enviro;
        }
    }
     
    *///?}
}

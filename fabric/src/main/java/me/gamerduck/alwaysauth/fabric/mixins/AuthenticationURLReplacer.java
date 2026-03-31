package me.gamerduck.alwaysauth.fabric.mixins;

import com.mojang.authlib.Environment;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import me.gamerduck.alwaysauth.Platform;
import me.gamerduck.alwaysauth.fabric.FabricPlatform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.RecordComponent;

/**
 * Mixin that replaces Minecraft's authentication URLs for Fabric/NeoForge platforms.
 * <p>
 * This mixin intercepts the creation of the YggdrasilMinecraftSessionService and modifies
 * the Environment parameter to use AlwaysAuth's custom session server URL instead of
 * Mojang's official session server. This allows authentication requests to be routed
 * through the local proxy server for fallback authentication support.
 * </p>
 * <p>
 * This approach is specific to Fabric/NeoForge which support the SpongePowered Mixin framework.
 * For Paper/Spigot/Velocity platforms, see {@link me.gamerduck.alwaysauth.reflection.AuthenticationURLReplacer}
 * which uses reflection instead.
 * </p>
 * <p>
 * <b>Important:</b> The {@code remap = false} annotation is critical because Authlib classes
 * are not obfuscated and don't need remapping during Mixin processing.
 * </p>
 *
 * @see me.gamerduck.alwaysauth.reflection.AuthenticationURLReplacer Reflection-based alternative for Paper/Spigot/Velocity
 */
@Mixin(YggdrasilAuthenticationService.class)
public abstract class AuthenticationURLReplacer extends HttpAuthenticationService {

    @Unique
    private static FabricPlatform fabricPlatform;

    /**
     * Required constructor for mixin to extend HttpAuthenticationService.
     * <p>
     * This constructor is never actually called - it exists only to satisfy
     * the Java compiler's requirement that the mixin class can extend
     * HttpAuthenticationService.
     * </p>
     *
     * @param proxy the proxy to use for HTTP connections (unused in practice)
     */
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

    /**
     * Injects custom session server URL into the Minecraft session service.
     * <p>
     * This method intercepts the creation of YggdrasilMinecraftSessionService and modifies
     * the Environment parameter being passed to its constructor. By changing the session host
     * in the Environment, all subsequent authentication requests will be directed to
     * AlwaysAuth's proxy server instead of Mojang's servers.
     * </p>
     * <p>
     * The injection point targets the constructor call with these specifics:
     * <ul>
     *     <li>Method: {@code createMinecraftSessionService}</li>
     *     <li>Target: {@code YggdrasilMinecraftSessionService} constructor</li>
     *     <li>Parameter: index 2 (the Environment parameter)</li>
     *     <li>Remap: false (Authlib is not obfuscated)</li>
     * </ul>
     * </p>
     *
     * @param enviro the original Environment parameter containing Mojang's URLs
     * @return a modified Environment with AlwaysAuth's session server URL
     */
    @ModifyArg(method = "createMinecraftSessionService", remap = false, at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService;<init>(Lcom/mojang/authlib/yggdrasil/ServicesKeySet;Ljava/net/Proxy;Lcom/mojang/authlib/Environment;)V"), index = 2)
    private Environment injected(Environment enviro) {
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
}

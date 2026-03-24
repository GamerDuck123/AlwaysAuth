package me.gamerduck.alwaysauth.mixin.mixins;

import me.gamerduck.alwaysauth.Platform;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Proxy;
import java.util.Properties;

/**
 * Mixin that forces the prevent-proxy-connections setting for Fabric/NeoForge platforms.
 * <p>
 * This mixin intercepts the initialization of DedicatedServerProperties and ensures that
 * the preventProxyConnections field is always set to true, regardless of what's configured
 * in server.properties. This is a security measure to maintain proper proxy detection while
 * AlwaysAuth provides authentication fallback functionality.
 * </p>
 * <p>
 * This approach is specific to Fabric/NeoForge which support the SpongePowered Mixin framework.
 * For Paper/Spigot/Velocity platforms, see {@link me.gamerduck.alwaysauth.reflection.ServerPropertiesReplacer}
 * which uses reflection and Unsafe to achieve the same result.
 * </p>
 * <p>
 * <b>Security Note:</b> Forcing this setting to true helps prevent players from bypassing
 * authentication by using proxies/VPNs. AlwaysAuth validates authentication through other
 * mechanisms (IP matching and session validation), so this additional protection layer is important.
 * </p>
 *
 * @see me.gamerduck.alwaysauth.reflection.ServerPropertiesReplacer Reflection-based alternative for Paper/Spigot/Velocity
 */
@Mixin(DedicatedServerProperties.class)
public abstract class ServerPropertiesReplacer {

    /**
     * Redirects the field assignment for preventProxyConnections during initialization.
     * <p>
     * This method intercepts when the DedicatedServerProperties constructor attempts to
     * set the preventProxyConnections field from server.properties. Instead of using
     * the configured value, we force it to always be true for security purposes.
     * </p>
     * <p>
     * The injection point targets:
     * <ul>
     *     <li>Method: {@code <init>} (constructor)</li>
     *     <li>Target: {@code preventProxyConnections} field assignment (PUTFIELD)</li>
     *     <li>Action: Replace the value being written with {@code true}</li>
     * </ul>
     * </p>
     *
     * @param instance the DedicatedServerProperties instance being constructed
     * @param value the original value from server.properties (ignored)
     */
    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/server/dedicated/DedicatedServerProperties;preventProxyConnections:Z", opcode = 181))
    private void injected(DedicatedServerProperties instance, boolean value) {
        instance.preventProxyConnections = true;
    }
}

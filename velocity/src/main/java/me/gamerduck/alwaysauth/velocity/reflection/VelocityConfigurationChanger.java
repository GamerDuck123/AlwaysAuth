package me.gamerduck.alwaysauth.velocity.reflection;

import me.gamerduck.alwaysauth.Platform;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class VelocityConfigurationChanger {

    private static final String VELOCITY_SERVER_CLASS = "com.velocitypowered.proxy.VelocityServer";
    private static final String CONFIG_CLASS = "com.velocitypowered.proxy.config.VelocityConfiguration";
    private static final String PREVENT_CLIENT_PROXY_FIELD = "preventClientProxyConnections";

    private static final boolean PREVENT_CLIENT_PROXY_VALUE = true;

    private static Unsafe getUnsafe() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    public static void forcePreventClientProxyConnections(Platform platform, Object velocityServer) {
        try {

            Class<?> velocityServerClass = Class.forName(VELOCITY_SERVER_CLASS);
            Class<?> configClass = Class.forName(CONFIG_CLASS);

            Field configField = velocityServerClass.getDeclaredField("configuration");
            configField.setAccessible(true);
            Object configuration = configField.get(velocityServer);

            Field preventProxyField = configClass.getDeclaredField(PREVENT_CLIENT_PROXY_FIELD);
            preventProxyField.setAccessible(true);

            Unsafe unsafe = getUnsafe();
            long offset = unsafe.objectFieldOffset(preventProxyField);
            unsafe.putBoolean(configuration, offset, PREVENT_CLIENT_PROXY_VALUE);

            if (platform.isDebug()) platform.sendLogMessage("Successfully set preventClientProxyConnections to " + PREVENT_CLIENT_PROXY_VALUE);

        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to modify Velocity configuration:");
            e.printStackTrace();
        }
    }
}

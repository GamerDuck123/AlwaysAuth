package me.gamerduck.alwaysauth.reflection;


import me.gamerduck.alwaysauth.Platform;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class ServerPropertiesReplacer {


    // Configuration - change these to match your server version if obfuscated
    private static final String CRAFT_SERVER_CLASS = "org.bukkit.craftbukkit.CraftServer";
    private static final String BUKKIT_CLASS = "org.bukkit.Bukkit";
    private static final String DEDICATED_SERVER_CLASS = "net.minecraft.server.dedicated.DedicatedServer";
    private static final String DEDICATED_SERVER_PROPERTIES_CLASS = "net.minecraft.server.dedicated.DedicatedServerProperties";

    // Method names in Bukkit
    private static final String GET_SERVER_METHOD = "getServer";

    // Method names in CraftServer
    private static final String GET_SERVER_METHOD_CRAFT = "getServer";

    // Field names in DedicatedServer
    private static final String SETTINGS_FIELD = "settings";

    // Field names in DedicatedServerProperties
    private static final String PREVENT_PROXY_CONNECTIONS_FIELD = "preventProxyConnections";

    // The value to set
    private static final boolean PREVENT_PROXY_CONNECTIONS_VALUE = true;

    private static Unsafe getUnsafe() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    public static void forcePreventProxyConnections(Platform platform) {
        try {
            // Load classes
            Class<?> bukkitClass = Class.forName(BUKKIT_CLASS);
            Class<?> craftServerClass = Class.forName(CRAFT_SERVER_CLASS);
            Class<?> dedicatedServerClass = Class.forName(DEDICATED_SERVER_CLASS);
            Class<?> dedicatedServerPropertiesClass = Class.forName(DEDICATED_SERVER_PROPERTIES_CLASS);

            // Get Bukkit.getServer()
            Method getServerMethod = bukkitClass.getMethod(GET_SERVER_METHOD);
            Object bukkitServer = getServerMethod.invoke(null);

            // Cast to CraftServer and get MinecraftServer
            Method getMinecraftServerMethod = craftServerClass.getMethod(GET_SERVER_METHOD_CRAFT);
            Object minecraftServer = getMinecraftServerMethod.invoke(bukkitServer);

            // Get the settings field from DedicatedServer
            Field settingsField = dedicatedServerClass.getDeclaredField(SETTINGS_FIELD);
            settingsField.setAccessible(true);
            Object properties = settingsField.get(minecraftServer);

            // Get the preventProxyConnections field
            Field preventProxyField = dedicatedServerPropertiesClass.getDeclaredField(PREVENT_PROXY_CONNECTIONS_FIELD);
            preventProxyField.setAccessible(true);

            // Use Unsafe to modify the final field
            Unsafe unsafe = getUnsafe();
            long fieldOffset = unsafe.objectFieldOffset(preventProxyField);
            unsafe.putBoolean(properties, fieldOffset, PREVENT_PROXY_CONNECTIONS_VALUE);

            platform.sendLogMessage("Successfully set preventProxyConnections to " + PREVENT_PROXY_CONNECTIONS_VALUE);

        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to modify server properties:");
            e.printStackTrace();
        }
    }
}

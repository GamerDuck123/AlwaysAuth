package me.gamerduck.alwaysauth.reflection;


import me.gamerduck.alwaysauth.Platform;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ServerPropertiesReplacer {

    private static final String CRAFT_SERVER_CLASS = "org.bukkit.craftbukkit.CraftServer";
    private static final String BUKKIT_CLASS = "org.bukkit.Bukkit";
    private static final String DEDICATED_SERVER_CLASS = "net.minecraft.server.dedicated.DedicatedServer";
    private static final String DEDICATED_SERVER_PROPERTIES_CLASS = "net.minecraft.server.dedicated.DedicatedServerProperties";
    private static final String DEDICATED_SERVER_SETTINGS_CLASS = "net.minecraft.server.dedicated.DedicatedServerSettings";

    private static final String GET_SERVER_METHOD = "getServer";
    private static final String GET_SERVER_METHOD_CRAFT = "getServer";
    private static final String SETTINGS_FIELD = "settings";
    private static final String GET_PROPERTIES_METHOD_SETTINGS = "getProperties";
    private static final String PREVENT_PROXY_CONNECTIONS_FIELD = "preventProxyConnections";
    private static final boolean PREVENT_PROXY_CONNECTIONS_VALUE = true;

    public static void forcePreventProxyConnections(Platform platform) {
        try {
            Class<?> bukkitClass = Class.forName(BUKKIT_CLASS);
            Class<?> craftServerClass = Class.forName(CRAFT_SERVER_CLASS);
            Class<?> dedicatedServerClass = Class.forName(DEDICATED_SERVER_CLASS);
            Class<?> dedicatedServerSettingsClass = Class.forName(DEDICATED_SERVER_SETTINGS_CLASS);
            Class<?> dedicatedServerPropertiesClass = Class.forName(DEDICATED_SERVER_PROPERTIES_CLASS);

            Method getServerMethod = bukkitClass.getMethod(GET_SERVER_METHOD);
            Object bukkitServer = getServerMethod.invoke(null);

            Method getMinecraftServerMethod = craftServerClass.getMethod(GET_SERVER_METHOD_CRAFT);
            Object minecraftServer = getMinecraftServerMethod.invoke(bukkitServer);

            Field settingsField = dedicatedServerClass.getDeclaredField(SETTINGS_FIELD);
            settingsField.setAccessible(true);
            Object settings = settingsField.get(minecraftServer);

            Method getPropertiesMethod = dedicatedServerSettingsClass.getMethod(GET_PROPERTIES_METHOD_SETTINGS);
            Object properties = getPropertiesMethod.invoke(settings);

            Field preventProxyField = dedicatedServerPropertiesClass.getField(PREVENT_PROXY_CONNECTIONS_FIELD);
            preventProxyField.setAccessible(true);
            preventProxyField.set(properties, PREVENT_PROXY_CONNECTIONS_VALUE);

            if (platform.isDebug()) {
                platform.sendLogMessage("Successfully set preventProxyConnections to " + PREVENT_PROXY_CONNECTIONS_VALUE);
            }

        } catch (ClassNotFoundException e) {
            platform.sendSevereLogMessage("Failed to find required class - check if class names changed: " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            platform.sendSevereLogMessage("Failed to find required field - check if field names changed: " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            platform.sendSevereLogMessage("Failed to find required method - check if method names changed: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            platform.sendSevereLogMessage("Unexpected error modifying server properties: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

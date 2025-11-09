package me.gamerduck.alwaysauth.reflection;
import me.gamerduck.alwaysauth.Platform;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.logging.Logger;

public class AuthenticationURLReplacer {

    // Configuration - change these to match your server version if obfuscated
    private static final String MINECRAFT_SERVER_CLASS = "net.minecraft.server.MinecraftServer";
    private static final String CRAFT_SERVER_CLASS = "org.bukkit.craftbukkit.CraftServer";
    private static final String BUKKIT_CLASS = "org.bukkit.Bukkit";
    private static final String SERVICES_CLASS = "net.minecraft.server.Services";
    private static final String YGGDRASIL_SESSION_SERVICE_CLASS = "com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService";
    private static final String HTTP_AUTH_SERVICE_CLASS = "com.mojang.authlib.HttpAuthenticationService";

    // Method names in Bukkit
    private static final String GET_SERVER_METHOD = "getServer";

    // Method names in CraftServer
    private static final String GET_SERVER_METHOD_CRAFT = "getServer";

    // Field names in MinecraftServer
    private static final String SERVICES_FIELD = "services";

    // Method names in Services (record accessor methods)
    private static final String SESSION_SERVICE_METHOD = "sessionService";

    // Field names in YggdrasilMinecraftSessionService
    private static final String BASE_URL_FIELD = "baseUrl";
    private static final String JOIN_URL_FIELD = "joinUrl";
    private static final String CHECK_URL_FIELD = "checkUrl";

    // Method names in HttpAuthenticationService
    private static final String CONSTANT_URL_METHOD = "constantURL";

    public static void replaceSessionService(Platform platform, String customSessionHost) {
        try {
            // Load classes
            Class<?> bukkitClass = Class.forName(BUKKIT_CLASS);
            Class<?> craftServerClass = Class.forName(CRAFT_SERVER_CLASS);
            Class<?> minecraftServerClass = Class.forName(MINECRAFT_SERVER_CLASS);
            Class<?> servicesClass = Class.forName(SERVICES_CLASS);
            Class<?> yggdrasilServiceClass = Class.forName(YGGDRASIL_SESSION_SERVICE_CLASS);
            Class<?> httpAuthServiceClass = Class.forName(HTTP_AUTH_SERVICE_CLASS);

            // Get Bukkit.getServer()
            Method getServerMethod = bukkitClass.getMethod(GET_SERVER_METHOD);
            Object bukkitServer = getServerMethod.invoke(null);

            // Cast to CraftServer and get MinecraftServer
            Method getMinecraftServerMethod = craftServerClass.getMethod(GET_SERVER_METHOD_CRAFT);
            Object minecraftServer = getMinecraftServerMethod.invoke(bukkitServer);

            // Get the services field from MinecraftServer
            Field servicesField = minecraftServerClass.getDeclaredField(SERVICES_FIELD);
            servicesField.setAccessible(true);
            Object services = servicesField.get(minecraftServer);

            // Get the sessionService from Services
            Object sessionService = servicesClass
                    .getMethod(SESSION_SERVICE_METHOD)
                    .invoke(services);

            // Check if it's a YggdrasilMinecraftSessionService
            if (!yggdrasilServiceClass.isInstance(sessionService)) {
                platform.sendSevereLogMessage("Session service is not " + YGGDRASIL_SESSION_SERVICE_CLASS);
                return;
            }

            // Prepare new URLs
            String newBaseUrl = customSessionHost + "/session/minecraft/";
            Method constantUrlMethod = httpAuthServiceClass.getMethod(CONSTANT_URL_METHOD, String.class);

            // Modify the baseUrl field
            Field baseUrlField = yggdrasilServiceClass.getDeclaredField(BASE_URL_FIELD);
            baseUrlField.setAccessible(true);
            baseUrlField.set(sessionService, newBaseUrl);

            // Modify the joinUrl field
            Field joinUrlField = yggdrasilServiceClass.getDeclaredField(JOIN_URL_FIELD);
            joinUrlField.setAccessible(true);
            URL newJoinUrl = (URL) constantUrlMethod.invoke(null, newBaseUrl + "join");
            joinUrlField.set(sessionService, newJoinUrl);

            // Modify the checkUrl field
            Field checkUrlField = yggdrasilServiceClass.getDeclaredField(CHECK_URL_FIELD);
            checkUrlField.setAccessible(true);
            URL newCheckUrl = (URL) constantUrlMethod.invoke(null, newBaseUrl + "hasJoined");
            checkUrlField.set(sessionService, newCheckUrl);

            if (platform.isDebug()) {
                platform.sendLogMessage("Successfully replaced authentication URLs:");
                platform.sendLogMessage("  Base URL: " + newBaseUrl);
                platform.sendLogMessage("  Join URL: " + newJoinUrl);
                platform.sendLogMessage("  Check URL: " + newCheckUrl);
            }

        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to replace authentication service:");
            e.printStackTrace();
        }
    }
}
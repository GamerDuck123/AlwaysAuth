package me.gamerduck.alwaysauth.reflection;
import me.gamerduck.alwaysauth.Platform;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;

public class AuthenticationURLReplacer {

    private static final String MINECRAFT_SERVER_CLASS = "net.minecraft.server.MinecraftServer";
    private static final String CRAFT_SERVER_CLASS = "org.bukkit.craftbukkit.CraftServer";
    private static final String BUKKIT_CLASS = "org.bukkit.Bukkit";
    private static final String SERVICES_CLASS = "net.minecraft.server.Services";
    private static final String YGGDRASIL_SESSION_SERVICE_CLASS = "com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService";
    private static final String HTTP_AUTH_SERVICE_CLASS = "com.mojang.authlib.HttpAuthenticationService";

    private static final String GET_SERVER_METHOD = "getServer";
    private static final String GET_SERVER_METHOD_CRAFT = "getServer";
    private static final String SERVICES_FIELD = "services";
    private static final String SESSION_SERVICE_METHOD = "sessionService";
    private static final String BASE_URL_FIELD = "baseUrl";
    private static final String JOIN_URL_FIELD = "joinUrl";
    private static final String CHECK_URL_FIELD = "checkUrl";
    private static final String CONSTANT_URL_METHOD = "constantURL";

    public static void replaceSessionService(Platform platform, String customSessionHost) {
        try {
            Class<?> bukkitClass = Class.forName(BUKKIT_CLASS);
            Class<?> craftServerClass = Class.forName(CRAFT_SERVER_CLASS);
            Class<?> minecraftServerClass = Class.forName(MINECRAFT_SERVER_CLASS);
            Class<?> servicesClass = Class.forName(SERVICES_CLASS);
            Class<?> yggdrasilServiceClass = Class.forName(YGGDRASIL_SESSION_SERVICE_CLASS);
            Class<?> httpAuthServiceClass = Class.forName(HTTP_AUTH_SERVICE_CLASS);

            Method getServerMethod = bukkitClass.getMethod(GET_SERVER_METHOD);
            Object bukkitServer = getServerMethod.invoke(null);

            Method getMinecraftServerMethod = craftServerClass.getMethod(GET_SERVER_METHOD_CRAFT);
            Object minecraftServer = getMinecraftServerMethod.invoke(bukkitServer);

            Field servicesField = minecraftServerClass.getDeclaredField(SERVICES_FIELD);
            servicesField.setAccessible(true);
            Object services = servicesField.get(minecraftServer);

            Object sessionService = servicesClass
                    .getMethod(SESSION_SERVICE_METHOD)
                    .invoke(services);

            if (!yggdrasilServiceClass.isInstance(sessionService)) {
                platform.sendSevereLogMessage("Session service is not " + YGGDRASIL_SESSION_SERVICE_CLASS);
                return;
            }

            String newBaseUrl = customSessionHost + "/session/minecraft/";
            Method constantUrlMethod = httpAuthServiceClass.getMethod(CONSTANT_URL_METHOD, String.class);

            Field baseUrlField = yggdrasilServiceClass.getDeclaredField(BASE_URL_FIELD);
            baseUrlField.setAccessible(true);
            baseUrlField.set(sessionService, newBaseUrl);

            Field joinUrlField = yggdrasilServiceClass.getDeclaredField(JOIN_URL_FIELD);
            joinUrlField.setAccessible(true);
            URL newJoinUrl = (URL) constantUrlMethod.invoke(null, newBaseUrl + "join");
            joinUrlField.set(sessionService, newJoinUrl);

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
            platform.sendSevereLogMessage("Unexpected error during authentication URL replacement: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

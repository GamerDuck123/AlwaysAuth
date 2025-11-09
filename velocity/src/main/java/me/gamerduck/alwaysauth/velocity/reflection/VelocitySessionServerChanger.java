package me.gamerduck.alwaysauth.velocity.reflection;

import me.gamerduck.alwaysauth.Platform;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class VelocitySessionServerChanger {

    // Class and field names
    private static final String LOGIN_HANDLER_CLASS = "com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler";
    private static final String MOJANG_URL_FIELD = "MOJANG_HASJOINED_URL";

    // The new session server URL you want to use

    private static Unsafe getUnsafe() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    public static void setCustomSessionServer(Platform platform, String customSessionServer) {
        try {
            // Load the InitialLoginSessionHandler class
            Class<?> loginHandlerClass = Class.forName(LOGIN_HANDLER_CLASS);

            // Get the MOJANG_HASJOINED_URL field
            Field urlField = loginHandlerClass.getDeclaredField(MOJANG_URL_FIELD);
            urlField.setAccessible(true);

            // Use Unsafe to modify the final static field
            Unsafe unsafe = getUnsafe();
            long offset = unsafe.staticFieldOffset(urlField);
            Object base = unsafe.staticFieldBase(urlField);

            String formattedUrl = customSessionServer.concat("/session/minecraft/hasJoined").concat("?username=%s&serverId=%s");
            unsafe.putObject(base, offset, formattedUrl);

            if (platform.isDebug()) platform.sendLogMessage("Successfully changed Mojang session server URL to " + customSessionServer);

        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to modify Mojang session server URL:");
            e.printStackTrace();
        }
    }

}

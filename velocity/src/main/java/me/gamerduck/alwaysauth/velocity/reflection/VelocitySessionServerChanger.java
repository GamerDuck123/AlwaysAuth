package me.gamerduck.alwaysauth.velocity.reflection;

import me.gamerduck.alwaysauth.Platform;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class VelocitySessionServerChanger {

    private static final String LOGIN_HANDLER_CLASS = "com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler";
    private static final String MOJANG_URL_FIELD = "MOJANG_HASJOINED_URL";

    private static Unsafe getUnsafe() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    public static void setCustomSessionServer(Platform platform, String customSessionServer) {
        try {
            Class<?> loginHandlerClass = Class.forName(LOGIN_HANDLER_CLASS);

            Field urlField = loginHandlerClass.getDeclaredField(MOJANG_URL_FIELD);
            urlField.setAccessible(true);

            Unsafe unsafe = getUnsafe();
            long offset = unsafe.staticFieldOffset(urlField);
            Object base = unsafe.staticFieldBase(urlField);

            String formattedUrl = customSessionServer.concat("/session/minecraft/hasJoined").concat("?username=%s&serverId=%s");
            unsafe.putObject(base, offset, formattedUrl);

            if (platform.isDebug()) platform.sendLogMessage("Successfully changed Mojang session server URL to " + unsafe.getObject(base, offset));

        } catch (Exception e) {
            platform.sendSevereLogMessage("Failed to modify Mojang session server URL:");
            e.printStackTrace();
        }
    }

}

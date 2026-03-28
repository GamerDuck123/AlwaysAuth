package me.gamerduck.alwaysauth.neoforge1211;

import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.logging.LogUtils;
import me.gamerduck.alwaysauth.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;

public class NeoForgePlatform extends Platform<CommandSourceStack> {

    public static final Logger LOGGER = LogUtils.getLogger();
    public static NeoForgePlatform instance;

    public NeoForgePlatform() {
        super(Path.of("config/AlwaysAuth"));
        instance = this;
    }

    public void replaceAuthUrls(MinecraftServer server) {
        try {
            Field servicesField = MinecraftServer.class.getDeclaredField("services");
            servicesField.setAccessible(true);
            Object services = servicesField.get(server);

            Object sessionService = services.getClass().getMethod("sessionService").invoke(services);

            Class<?> yggdrasilClass = Class.forName("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService");
            if (!yggdrasilClass.isInstance(sessionService)) {
                sendSevereLogMessage("Unexpected session service type, URL replacement failed: " + sessionService.getClass().getName());
                return;
            }

            String newBaseUrl = config().getSessionServerUrl() + "/session/minecraft/";
            Method constantUrl = HttpAuthenticationService.class.getMethod("constantURL", String.class);

            Field baseUrlField = yggdrasilClass.getDeclaredField("baseUrl");
            baseUrlField.setAccessible(true);
            baseUrlField.set(sessionService, newBaseUrl);

            Field joinUrlField = yggdrasilClass.getDeclaredField("joinUrl");
            joinUrlField.setAccessible(true);
            joinUrlField.set(sessionService, (URL) constantUrl.invoke(null, newBaseUrl + "join"));

            Field checkUrlField = yggdrasilClass.getDeclaredField("checkUrl");
            checkUrlField.setAccessible(true);
            checkUrlField.set(sessionService, (URL) constantUrl.invoke(null, newBaseUrl + "hasJoined"));

            sendLogMessage("Successfully replaced authentication URLs");
            sendLogMessage("  Base URL: " + newBaseUrl);
        } catch (NoSuchFieldException e) {
            sendSevereLogMessage("Failed to find field for URL replacement: " + e.getMessage());
        } catch (Exception e) {
            sendSevereLogMessage("Failed to replace authentication URLs: " + e.getMessage());
        }
    }

    @Override
    public void sendMessage(CommandSourceStack commandSender, String msg) {
        commandSender.sendSystemMessage(Component.literal(msg));
    }

    @Override
    public void sendLogMessage(String msg) {
        LOGGER.info(msg.replaceAll("§.", ""));
    }

    @Override
    public void sendSevereLogMessage(String msg) {
        LOGGER.error(msg.replaceAll("§.", ""));
    }

    @Override
    public void sendWarningLogMessage(String msg) {
        LOGGER.warn(msg.replaceAll("§.", ""));
    }

}
package me.gamerduck.alwaysauth.neoforgeA1204B1206;

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

    public NeoForgePlatform() {
        super(Path.of("config/AlwaysAuth"));
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
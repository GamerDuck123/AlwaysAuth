package me.gamerduck.alwaysauth.neoforge;

import com.mojang.logging.LogUtils;
import me.gamerduck.alwaysauth.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public class NeoForgePlatform extends Platform<CommandSourceStack> {

    public static final Logger LOGGER = LogUtils.getLogger();
    private MinecraftServer minecraftServer;

    public NeoForgePlatform(MinecraftServer minecraftServer) {
        super(minecraftServer.getServerDirectory().resolve("config/AlwaysAuth"));
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
package me.gamerduck.alwaysauth.neoforge;

import com.mojang.logging.LogUtils;
import me.gamerduck.alwaysauth.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.nio.file.Path;

public class NeoForgePlatform extends Platform<CommandSourceStack> {

    public static final Logger LOGGER = LogUtils.getLogger();
    private MinecraftServer minecraftServer;

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
package me.gamerduck.alwaysauth.fabric;

import me.gamerduck.alwaysauth.Platform;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.logging.Logger;

public class FabricPlatform extends Platform<CommandSourceStack> {

    public static final Logger LOGGER = Logger.getLogger("alwaysauth");
    private MinecraftServer minecraftServer;

    public FabricPlatform(MinecraftServer minecraftServer) {
        super(minecraftServer.getServerDirectory().resolve("config/AlwaysAuth"), LOGGER);



    }

    @Override
    public void sendMessage(CommandSourceStack commandSender, String msg) {
        commandSender.sendSystemMessage(Component.literal(msg));
    }

}

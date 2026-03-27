package me.gamerduck.alwaysauth.fabricA120B1211;

import me.gamerduck.alwaysauth.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class FabricPlatform extends Platform<ServerCommandSource> {

    public static final Logger LOGGER = LoggerFactory.getLogger("alwaysauth");
    private MinecraftServer minecraftServer;
    public static FabricPlatform instance;

    public FabricPlatform() {
        super(Path.of("config/AlwaysAuth"));
        instance = this;
    }

    @Override
    public void sendMessage(ServerCommandSource commandSender, String msg) {
        commandSender.sendMessage(Text.literal(msg));
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

package me.gamerduck.alwaysauth.fabric;

import me.gamerduck.alwaysauth.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
//? >=1.21.11 {
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
//?}
//? if <=1.16 {
/*import java.util.logging.Logger;
*///?} else {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?}
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class FabricPlatform extends Platform<CommandSourceStack> {

    //? <=1.16 {
    /*public static final Logger LOGGER = Logger.getLogger("alwaysauth");
    *///?} else
    public static final Logger LOGGER = LoggerFactory.getLogger("alwaysauth");


    private MinecraftServer minecraftServer;
    public static FabricPlatform instance;

    public FabricPlatform() {
        super(Path.of("config/AlwaysAuth"));
        instance = this;
    }

    @Override
    public void sendMessage(CommandSourceStack commandSender, String msg) {
        //? >=1.20 {
        commandSender.sendSystemMessage(Component.literal(msg));
        //?} else if ~1.19 {
        /*commandSender.sendSuccess(Component.literal(msg), false);
        *///?} else
        //commandSender.sendSuccess(new net.minecraft.network.chat.TextComponent(msg), false);
    }

    @Override
    public boolean hasPermission(CommandSourceStack commandSender, String permission) {
        //? >=1.21.11 {
        return commandSender.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ADMINS));
        //?} else
        //return commandSender.hasPermission(4);
    }

    @Override
    public void sendLogMessage(String msg) {
        LOGGER.info(msg.replaceAll("§.", ""));
    }

    @Override
    public void sendSevereLogMessage(String msg) {
        //~if <=1.16 '.error' -> '.info' {
        LOGGER.error(msg.replaceAll("§.", ""));
        //~}
    }

    @Override
    public void sendWarningLogMessage(String msg) {
        //~if <=1.16 '.warn' -> '.info' {
        LOGGER.warn(msg.replaceAll("§.", ""));
        //~}
    }

}

package me.gamerduck.alwaysauth.velocity;

import com.mojang.brigadier.Command;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import me.gamerduck.alwaysauth.Platform;
import me.gamerduck.alwaysauth.velocity.reflection.VelocityConfigurationChanger;
import me.gamerduck.alwaysauth.velocity.reflection.VelocitySessionServerChanger;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.logging.Logger;

public class VelocityPlatform extends Platform<CommandSource> {

    private final ProxyServer server;
//    private final Logger logger;

    public VelocityPlatform(ProxyServer server, Logger logger, Path dataDirectory) {
        super(dataDirectory);
        this.server = server;

        VelocitySessionServerChanger.setCustomSessionServer(this, config().getSessionServerUrl());
        VelocityConfigurationChanger.forcePreventClientProxyConnections(this, server);
    }

    @Override
    public void sendMessage(CommandSource commandSender, String msg) {
        commandSender.sendMessage(Component.text(msg));
    }

    @Override
    public void sendLogMessage(String msg) {
        System.out.println(msg);
    }

    @Override
    public void sendSevereLogMessage(String msg) {
        System.out.println(msg);
    }

    @Override
    public void sendWarningLogMessage(String msg) {
        System.out.println(msg);
    }
}

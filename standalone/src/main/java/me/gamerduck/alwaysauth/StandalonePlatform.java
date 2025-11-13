package me.gamerduck.alwaysauth;

import java.io.PrintStream;
import java.nio.file.Path;

public class StandalonePlatform extends Platform<PrintStream>{

    public StandalonePlatform(Path dataDirectory) {
        super(dataDirectory);
        String message = getUpdateMessage();
        if (message != null) sendLogMessage(message);
    }

    @Override
    public void sendMessage(PrintStream commandSender, String msg) {
        sendLogMessage(msg.replaceAll("§.", ""));
    }

    @Override
    public void sendLogMessage(String msg) {
        System.out.println("[INFO] " + msg.replaceAll("§.", ""));
    }

    @Override
    public void sendSevereLogMessage(String msg) {
        System.out.println("[ERROR] " + msg.replaceAll("§.", ""));
    }

    @Override
    public void sendWarningLogMessage(String msg) {
        System.out.println("[WARNING] " + msg.replaceAll("§.", ""));
    }

    @Override
    public void cmdHelp(PrintStream player) {
        sendMessage(player,"Always Auth");
        sendMessage(player,"Commands:");
        sendMessage(player,"status - Show current status");
        sendMessage(player,"stats - Show cache statistics");
        sendMessage(player,"toggle - Enable/disable fallback");
        sendMessage(player,"security <basic|medium> - Set security level");
        sendMessage(player,"cleanup - Clean old cache entries");
        sendMessage(player,"reload - Reload configuration");
        sendMessage(player,"quit - Quit the program");
    }
}

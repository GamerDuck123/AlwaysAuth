package me.gamerduck.alwaysauth;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

public class StandalonePlatform extends Platform<PrintStream>{

    public StandalonePlatform(String[] args, Path dataDirectory, long startMS, Map<String, String> configOverrides, boolean daemonMode) throws InterruptedException {
        super(dataDirectory, configOverrides);

        sendLogMessage(String.format("Done (%sms)! For help, type \"help\"", System.currentTimeMillis() - startMS));

        if (daemonMode) {
            sendLogMessage("Running in daemon mode (ALWAYS_AUTH_DAEMON=true). Interactive CLI disabled.");
            Thread.currentThread().join();
            return;
        }

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")
                    || input.equalsIgnoreCase("quit")
                    || input.equalsIgnoreCase("stop")) {
                sendLogMessage("Shutting down...");
                break;
            }

            String[] cmdArgs = input.split(" ");
            switch (cmdArgs[0].toLowerCase()) {
                case "status" -> cmdStatus(System.out);
                case "stats" -> cmdStats(System.out);
                case "toggle" -> cmdToggle(System.out);
                case "security" -> {
                    if (cmdArgs.length < 2) {
                        sendLogMessage("Usage: security <basic|medium>");
                        continue;
                    }
                    String level = cmdArgs[1].toLowerCase();
                    cmdSecurity(System.out, level);
                }
                case "cleanup" -> cmdCleanup(System.out);
                case "reload" -> cmdReload(System.out);
                default -> cmdHelp(System.out);
            }
        }

        scanner.close();
    }

    @Override
    public void sendMessage(PrintStream commandSender, String msg) {
        sendLogMessage(msg.replaceAll("§.", ""));
    }

    @Override
    public boolean hasPermission(PrintStream commandSender, String permission) {
        return true;
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

package me.gamerduck.alwaysauth;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Scanner;

public class StandalonePlatform extends Platform<PrintStream>{

    public StandalonePlatform(Path dataDirectory) {
        super(dataDirectory);
    }
    public static void startup(String[] args, Path dataPath, long startMS) throws Exception {
        StandalonePlatform standAlonePlatform = new StandalonePlatform(dataPath);

        standAlonePlatform.sendLogMessage(String.format("Done (%sms)! For help, type \"help\"", System.currentTimeMillis() - startMS));

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")
                    || input.equalsIgnoreCase("quit")
                    || input.equalsIgnoreCase("stop")) {
                standAlonePlatform.sendLogMessage("Shutting down...");
                break;
            }

            String[] cmdArgs = input.split(" ");
            switch (cmdArgs[0].toLowerCase()) {
                case "status" -> standAlonePlatform.cmdStatus(System.out);
                case "stats" -> standAlonePlatform.cmdStats(System.out);
                case "toggle" -> standAlonePlatform.cmdToggle(System.out);
                case "security" -> {
                    if (cmdArgs.length < 2) {
                        standAlonePlatform.sendLogMessage("Usage: security <basic|medium>");
                        continue;
                    }
                    String level = cmdArgs[1].toLowerCase();
                    standAlonePlatform.cmdSecurity(System.out, level);
                }
                case "cleanup" -> standAlonePlatform.cmdCleanup(System.out);
                case "reload" -> standAlonePlatform.cmdReload(System.out);
                default -> standAlonePlatform.cmdHelp(System.out);
            }
        }

        scanner.close();
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

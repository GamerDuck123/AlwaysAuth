package me.gamerduck.alwaysauth;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.Logger;

public class StandalonePlatform extends Platform<PrintStream>{

    public StandalonePlatform(Path dataDirectory) {
        super(dataDirectory);

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")
                    || input.equalsIgnoreCase("quit")
                    || input.equalsIgnoreCase("stop")) {
                System.out.println("Shutting down...");
                break;
            }

            String[] args = input.split(" ");
            switch (args[0].toLowerCase()) {
                case "status" -> cmdStatus(System.out);
                case "stats" -> cmdStats(System.out);
                case "toggle" -> cmdToggle(System.out);
                case "security" -> {
                    if (args.length < 2) {
                        sendMessage(System.out, "Usage: security <basic|medium>");
                        return;
                    }
                    String level = args[1].toLowerCase();
                    cmdSecurity(System.out, level);
                }
                case "cleanup" -> cmdCleanup(System.out);
                case "reload" -> cmdReload(System.out);
                default -> cmdHelp(System.out);
            }
        }

        scanner.close();
        System.exit(0);
    }

    @Override
    public void sendMessage(PrintStream commandSender, String msg) {
        commandSender.println(msg.replaceAll("§.", ""));
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

package me.gamerduck.alwaysauth;

import java.nio.file.Path;
import java.util.Scanner;

public class AlwaysAuthMain {
    private static StandalonePlatform standAlonePlatform;
    public static void main(String[] arg) {
        long startMS = System.currentTimeMillis();
        String dataDirectory = System.getProperty("directory", "./data");

        try {
            standAlonePlatform = new StandalonePlatform(Path.of(dataDirectory));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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

            String[] args = input.split(" ");
            switch (args[0].toLowerCase()) {
                case "status" -> standAlonePlatform.cmdStatus(System.out);
                case "stats" -> standAlonePlatform.cmdStats(System.out);
                case "toggle" -> standAlonePlatform.cmdToggle(System.out);
                case "security" -> {
                    if (args.length < 2) {
                        standAlonePlatform.sendLogMessage("Usage: security <basic|medium>");
                        return;
                    }
                    String level = args[1].toLowerCase();
                    standAlonePlatform.cmdSecurity(System.out, level);
                }
                case "cleanup" -> standAlonePlatform.cmdCleanup(System.out);
                case "reload" -> standAlonePlatform.cmdReload(System.out);
                default -> standAlonePlatform.cmdHelp(System.out);
            }
        }

        scanner.close();
        System.exit(0);

    }
}

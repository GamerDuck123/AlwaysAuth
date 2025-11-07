package me.gamerduck.alwaysauth;

import java.nio.file.Path;

public class AlwaysAuthMain {
    private static StandalonePlatform standAlonePlatform;
    public static void main(String[] args) {
        String dataDirectory = System.getProperty("data.directory", "./data");

        try {
            standAlonePlatform = new StandalonePlatform(Path.of(dataDirectory));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

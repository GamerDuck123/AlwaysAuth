package me.gamerduck.alwaysauth;

import me.gamerduck.alwaysauth.api.StandaloneLibraryResolver;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

public class AlwaysAuthMain {

    public static void main(String[] args) throws Exception {
        long startMS = System.currentTimeMillis();
        String dataDirectory = System.getProperty("directory", "./data");
        Path dataPath = Path.of(dataDirectory);

        String librariesDirectory = System.getProperty("libraries", "./libraries");
        Path libraries = Path.of(librariesDirectory);
        if (Files.notExists(libraries)) {
            try {
                Files.createDirectories(libraries);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        StandaloneLibraryResolver resolver = new StandaloneLibraryResolver();
        resolver.addDependency("com.h2database:h2:2.3.232");
        resolver.addDependency("com.mysql:mysql-connector-j:8.0.33");
        resolver.addDependency("org.mariadb.jdbc:mariadb-java-client:3.3.2");
        resolver.addDependency("com.google.code.gson:gson:2.10.1");
        resolver.addRepository("https://repo.papermc.io/repository/maven-public/");

        resolver.resolveDependencies(libraries, AlwaysAuthMain.class, null);

        // Load and run the actual main class from the new classloader
        ClassLoader loader = resolver.getClassLoader();
        Class<?> mainClass = loader.loadClass("me.gamerduck.alwaysauth.StandalonePlatform");
        Method runMethod = mainClass.getDeclaredMethod("startup", String[].class, Path.class, long.class);
        runMethod.invoke(null, args, dataPath, startMS);

        System.exit(0);
    }
}

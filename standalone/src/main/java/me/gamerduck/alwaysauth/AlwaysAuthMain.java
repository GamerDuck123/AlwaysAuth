package me.gamerduck.alwaysauth;

import me.gamerduck.alwaysauth.api.StandaloneLibraryResolver;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AlwaysAuthMain {

    public static void main(String[] args) throws Exception {
        long startMS = System.currentTimeMillis();

        String dataDirectory = System.getenv("ALWAYS_AUTH_DATA_DIR");
        if (dataDirectory == null || dataDirectory.isEmpty()) {
            dataDirectory = System.getProperty("directory", "./data");
        }
        Path dataPath = Path.of(dataDirectory);

        String librariesDirectory = System.getenv("ALWAYS_AUTH_LIBRARIES_DIR");
        if (librariesDirectory == null || librariesDirectory.isEmpty()) {
            librariesDirectory = System.getProperty("libraries", "./libraries");
        }
        Path libraries = Path.of(librariesDirectory);
        if (Files.notExists(libraries)) {
            try {
                Files.createDirectories(libraries);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        boolean daemonMode = Boolean.parseBoolean(System.getenv("ALWAYS_AUTH_DAEMON"));

        Map<String, String> envOverrides = new HashMap<>();

        Map<String, String> rdsEnvOverrides = new HashMap<>();
        String rdsHost = System.getenv("RDS_HOSTNAME");
        String rdsPort = System.getenv("RDS_PORT");
        String rdsDbName = System.getenv("RDS_DB_NAME");
        String rdsUsername = System.getenv("RDS_USERNAME");
        String rdsPassword = System.getenv("RDS_PASSWORD");

        if (rdsHost != null && !rdsHost.isEmpty()) {
            rdsEnvOverrides.put("ALWAYS_AUTH_DATABASE_HOST", rdsHost);
        }
        if (rdsPort != null && !rdsPort.isEmpty()) {
            rdsEnvOverrides.put("ALWAYS_AUTH_DATABASE_PORT", rdsPort);
        }
        if (rdsDbName != null && !rdsDbName.isEmpty()) {
            rdsEnvOverrides.put("ALWAYS_AUTH_DATABASE_NAME", rdsDbName);
        }
        if (rdsUsername != null && !rdsUsername.isEmpty()) {
            rdsEnvOverrides.put("ALWAYS_AUTH_DATABASE_USERNAME", rdsUsername);
        }
        if (rdsPassword != null && !rdsPassword.isEmpty()) {
            rdsEnvOverrides.put("ALWAYS_AUTH_DATABASE_PASSWORD", rdsPassword);
        }
        if (!rdsEnvOverrides.isEmpty() && !envOverrides.containsKey("ALWAYS_AUTH_DATABASE_TYPE")) {
            rdsEnvOverrides.put("ALWAYS_AUTH_DATABASE_TYPE", "mysql");
        }

        envOverrides.putAll(rdsEnvOverrides);

        String prefix = "ALWAYS_AUTH_";
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix) && !key.equals("ALWAYS_AUTH_DATA_DIR")
                    && !key.equals("ALWAYS_AUTH_LIBRARIES_DIR")
                    && !key.equals("ALWAYS_AUTH_DAEMON")) {
                envOverrides.put(key, entry.getValue());
            }
        }

        StandaloneLibraryResolver resolver = new StandaloneLibraryResolver();
        resolver.addDependency("com.h2database:h2:2.3.232");
        resolver.addDependency("com.mysql:mysql-connector-j:8.0.33");
        resolver.addDependency("org.mariadb.jdbc:mariadb-java-client:3.3.2");
        resolver.addDependency("com.google.code.gson:gson:2.10.1");
        resolver.addRepository("https://repo.papermc.io/repository/maven-public/");

        resolver.resolveDependencies(libraries, AlwaysAuthMain.class, null);

        StandalonePlatform platform = new StandalonePlatform(args, dataPath, startMS, envOverrides, daemonMode);

        System.exit(0);
    }
}

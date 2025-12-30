package me.gamerduck.alwaysauth.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public abstract class LibraryResolver<T> {

    protected final List<String> repositories = new ArrayList<>();
    protected final List<String> dependencies = new ArrayList<>();

    public void addRepository(String url) {
        repositories.add(url.endsWith("/") ? url : url + "/");
    }

    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }

    public void addDependency(String group, String artifact, String version) {
        addDependency(group + ":" + artifact + ":" + version);
    }

    public abstract void resolveDependencies(Path libsFolder, T libraryManager, Object plugin) throws Exception;

    protected File resolveDependency(String coords, Path libsFolder) {
        try {
            String[] parts = coords.split(":");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid Maven coordinates: " + coords);

            String group = parts[0], artifact = parts[1], version = parts[2];
            String path = group.replace('.', '/') + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
            File output = new File(libsFolder.toFile(), artifact + "-" + version + ".jar");

            if (output.exists()) return output;

            for (String repo : repositories) {
                String fullUrl = repo + path;
                if (downloadFile(fullUrl, output)) return output;
            }

            throw new IOException("Failed to download dependency: " + coords);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected boolean downloadFile(String url, File output) {
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
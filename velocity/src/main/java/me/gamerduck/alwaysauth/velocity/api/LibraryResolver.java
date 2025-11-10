package me.gamerduck.alwaysauth.velocity.api;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.JarFile;

public class LibraryResolver {

    private final List<String> repositories = new ArrayList<>();
    private final List<String> dependencies = new ArrayList<>();

    public void addRepository(String url) {
        repositories.add(url.endsWith("/") ? url : url + "/");
    }

    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }

    public void addDependency(String group, String artifact, String version) {
        addDependency(group + ":" + artifact + ":" + version);
    }

    public void resolveDependencies(Path libsFolder, PluginManager manager, Object plugin) throws Exception {
        if (Files.notExists(libsFolder)) Files.createDirectories(libsFolder);

        for (String dep : dependencies) {
            File jar = resolveDependency(dep, libsFolder);
            if (jar != null) manager.addToClasspath(plugin, jar.toPath());
        }

    }

    private File resolveDependency(String coords, Path libsFolder) {
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

    private boolean downloadFile(String url, File output) {
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
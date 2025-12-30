package me.gamerduck.alwaysauth.velocity.api;
import com.velocitypowered.api.plugin.PluginManager;

import java.io.*;
import java.nio.file.*;

public class VelocityLibraryResolver extends me.gamerduck.alwaysauth.api.LibraryResolver<PluginManager> {

    public void resolveDependencies(Path libsFolder, PluginManager manager, Object plugin) throws Exception {
        if (Files.notExists(libsFolder)) Files.createDirectories(libsFolder);

        for (String dep : dependencies) {
            File jar = resolveDependency(dep, libsFolder);
            if (jar != null) manager.addToClasspath(plugin, jar.toPath());
        }

    }

}
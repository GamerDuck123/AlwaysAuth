package me.gamerduck.alwaysauth.api;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

public class StandaloneLibraryResolver extends LibraryResolver<Class<?>> {

    private URLClassLoader classLoader;

    public void resolveDependencies(Path libsFolder, Class<?> manager, Object plugin) throws Exception {
        if (Files.notExists(libsFolder)) Files.createDirectories(libsFolder);

        List<URL> urls = new ArrayList<>();

        CodeSource codeSource = manager.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            urls.add(codeSource.getLocation());
        }

        for (String dep : dependencies) {
            File jar = resolveDependency(dep, libsFolder);
            if (jar != null) {
                urls.add(jar.toURI().toURL());
            }
        }

        classLoader = new URLClassLoader(
            urls.toArray(new URL[0]),
            manager.getClassLoader().getParent()
        );

        Thread.currentThread().setContextClassLoader(classLoader);
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }

}
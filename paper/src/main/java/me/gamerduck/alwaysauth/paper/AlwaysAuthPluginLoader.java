package me.gamerduck.alwaysauth.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

public class AlwaysAuthPluginLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {

        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addDependency(new Dependency(new DefaultArtifact("com.h2database:h2:2.3.232"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("mysql:mysql-connector-java:8.0.33"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.mariadb.jdbc:mariadb-java-client:3.3.2"), null));
        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2").build());

        classpathBuilder.addLibrary(resolver);
    }
}

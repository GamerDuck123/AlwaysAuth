import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    id("io.papermc.hangar-publish-plugin")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set(property("versionType") as String)
        id.set(property("hangarID") as String)
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        changelog.set(rootProject.file("CHANGELOG.md").readText())
        platforms {

            when (project.name) {
                "paper" -> {
                    register(Platforms.PAPER) {
                        jar.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })

                        platformVersions.set(listOf<String>(libs.findVersion("minecraft").get().toString()))
                    }
                }
                "velocity" -> {
                    register(Platforms.VELOCITY) {
                        jar.set(tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").flatMap { it.archiveFile })

                        platformVersions.set(listOf<String>(libs.findVersion("velocity").get().toString()))
                    }

                }
                else -> throw IllegalStateException("Unknown loader $name")
            }
        }
    }
}

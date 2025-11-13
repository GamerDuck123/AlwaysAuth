import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    id("io.papermc.hangar-publish-plugin")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

hangarPublish {
    publications.register("plugin") {
        version.set("${rootProject.version as String}-${project.name}")
        channel.set(rootProject.property("versionType") as String)
        id.set(rootProject.property("hangarID") as String)
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
                        jar.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })

                        platformVersions.set(listOf<String>("3.4"))
                    }

                }
                else -> throw IllegalStateException("Unknown loader $name")
            }
        }
    }
}

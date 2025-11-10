import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    id("io.papermc.hangar-publish-plugin")
}


hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String + "-" + project.name)
        channel.set(property("versionType") as String)
        id.set(property("hangarID") as String)
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        changelog.set(rootProject.file("CHANGELOG.md").readText())
        platforms {

            when (project.name) {
                "paper" -> {
                    register(Platforms.PAPER) {
                        version.set(project.version as String)
                        jar.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })

                        val versions: List<String> = (property("minecraft_version") as String)
                            .split(",")
                            .map { it.trim() }
                        platformVersions.set(versions)
                    }
                }
                "velocity" -> {
                    register(Platforms.VELOCITY) {
                        jar.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })
                        platformVersions.set(listOf(property("velocityVersion") as String))
                    }
                }
                else -> throw IllegalStateException("Unknown loader $name")
            }
        }
    }
}

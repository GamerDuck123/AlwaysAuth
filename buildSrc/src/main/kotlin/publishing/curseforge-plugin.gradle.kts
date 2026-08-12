plugins {
    id("net.darkhax.curseforgegradle")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

tasks.register("publishCurseForge", net.darkhax.curseforgegradle.TaskPublishCurseForge::class) {
    apiToken = System.getenv("CURSEFORGE_TOKEN")

    val projectId = rootProject.property("curseforgeID") as String?

    val platformName = project.parent
        ?.takeIf { it != project.rootProject }
        ?.name
        ?: project.name

    var mainFile = when (platformName) {
        "fabric" -> upload(projectId, when (project.name) {
            "26.1", "26.1.1", "26.1.2", "26.2" -> tasks.named<Jar>("jar")
            else -> tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar")
        })
        "neoforge"-> upload(projectId, tasks.named<Jar>("jar"))
        else -> throw IllegalStateException("Unknown loader ${project.name}")
    };
    mainFile.addModLoader(
        when (platformName) {
            "fabric261", "fabric" -> "Fabric"
            "neoforge" -> "NeoForge"
            else -> throw IllegalStateException("Unknown loader ${project.name}")
        })

    when (platformName) {
        "fabric261" -> when (project.name) {
            "26.1" -> mainFile.addGameVersion("26.1")
            "26.1.1" -> mainFile.addGameVersion("26.1.1")
            "26.1.2" -> mainFile.addGameVersion("26.1.2")
            "26.2" -> mainFile.addGameVersion("26.2")
            else -> throw IllegalStateException("Unknown loader ${project.name} ")
        }
        "fabric" -> when (project.name) {
            "1.14.4" -> {
                mainFile.addGameVersion("1.14.4")
            }
            "1.15" -> {
                mainFile.addGameVersion("1.15")
                mainFile.addGameVersion("1.15.1")
                mainFile.addGameVersion("1.15.2")
            }
            "1.16" -> {
                mainFile.addGameVersion("1.16")
                mainFile.addGameVersion("1.16.1")
                mainFile.addGameVersion("1.16.2")
                mainFile.addGameVersion("1.16.3")
                mainFile.addGameVersion("1.16.4")
                mainFile.addGameVersion("1.16.5")

            }
            "1.17" -> {
                mainFile.addGameVersion("1.17")
                mainFile.addGameVersion("1.17.1")
            }
            "1.18" -> {
                mainFile.addGameVersion("1.18")
                mainFile.addGameVersion("1.18.1")
                mainFile.addGameVersion("1.18.2")
            }
            "1.19" -> {
                mainFile.addGameVersion("1.19")
                mainFile.addGameVersion("1.19.1")
                mainFile.addGameVersion("1.19.2")
                mainFile.addGameVersion("1.19.3")
                mainFile.addGameVersion("1.19.4")
            }
            "1.20" -> {
                mainFile.addGameVersion("1.20")
                mainFile.addGameVersion("1.20.1")
                mainFile.addGameVersion("1.20.2")
                mainFile.addGameVersion("1.20.3")
                mainFile.addGameVersion("1.20.4")
                mainFile.addGameVersion("1.20.5")
                mainFile.addGameVersion("1.20.6")
                mainFile.addGameVersion("1.21")
                mainFile.addGameVersion("1.21.1")
                mainFile.addGameVersion("1.21.2")
                mainFile.addGameVersion("1.21.3")
                mainFile.addGameVersion("1.21.4")
                mainFile.addGameVersion("1.21.5")
                mainFile.addGameVersion("1.21.6")
                mainFile.addGameVersion("1.21.7")
                mainFile.addGameVersion("1.21.8")
                mainFile.addGameVersion("1.21.9")
                mainFile.addGameVersion("1.21.10")
            }
            "1.21.11" -> mainFile.addGameVersion("1.21.11")
            else -> throw IllegalStateException("Unknown loader $project.name")
        }
        "neoforge" -> when (project.name) {
            "1.20.4" -> {
                mainFile.addGameVersion("1.20.4")
                mainFile.addGameVersion("1.20.5")
                mainFile.addGameVersion("1.20.6")
                mainFile.addGameVersion("1.21")
                mainFile.addGameVersion("1.21.1")
                mainFile.addGameVersion("1.21.2")
                mainFile.addGameVersion("1.21.3")
                mainFile.addGameVersion("1.21.4")
                mainFile.addGameVersion("1.21.5")
                mainFile.addGameVersion("1.21.6")
                mainFile.addGameVersion("1.21.7")
                mainFile.addGameVersion("1.21.8")
                mainFile.addGameVersion("1.21.9")
                mainFile.addGameVersion("1.21.10")
            }
            "1.21.11" -> mainFile.addGameVersion("1.21.11")
            "26.1" -> mainFile.addGameVersion("26.1")
            "26.1.1" -> mainFile.addGameVersion("26.1.1")
            "26.1.2" -> mainFile.addGameVersion("26.1.2")
            "26.2" -> mainFile.addGameVersion("26.2")
            else -> throw IllegalStateException("Unknown loader $project.name")
        }
        else -> throw IllegalStateException("Unknown loader $project.name")
    }

    mainFile.releaseType = rootProject.property("versionType") as String
    mainFile.displayName = "${version as String}-${project.name}-${platformName}"
    mainFile.changelog = rootProject.file("CHANGELOG.md").readText()
}
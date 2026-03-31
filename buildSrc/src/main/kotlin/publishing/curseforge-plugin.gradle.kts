plugins {
    id("net.darkhax.curseforgegradle")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

tasks.register("publishCurseForge", net.darkhax.curseforgegradle.TaskPublishCurseForge::class) {
    apiToken = System.getenv("CURSEFORGE_TOKEN")

    val projectId = rootProject.property("curseforgeID") as String?

    var mainFile = when (project.name) {
        "fabric1211", "fabric", "fabricA120B12111" -> upload(projectId, tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar"))
        "fabric261", "neoforge261", "neoforge1211", "neoforgeA1204B12111" -> upload(projectId, tasks.named<Jar>("jar"))
        else -> throw IllegalStateException("Unknown loader $project.name")
    };
    mainFile.addModLoader(
        when (project.name) {
            "fabric1211", "fabric", "fabric261", "fabricA120B12111" -> "Fabric"
            "neoforge261", "neoforge1211", "neoforgeA1204B12111" -> "NeoForge"
            else -> throw IllegalStateException("Unknown loader $project.name")
        })

    when (project.name) {
        "fabric1211", "neoforge1211" -> mainFile.addGameVersion("1.21.11")
        "fabricA120B12111" -> {
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
        "neoforgeA1204B12111" -> {
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
        "fabric261", "fabric", "neoforge261" -> mainFile.addGameVersion("26.1")
        else -> throw IllegalStateException("Unknown loader $project.name")
    }

    mainFile.releaseType = rootProject.property("versionType") as String
    mainFile.displayName = "${rootProject.version as String}-${when (project.name) {
        "fabric1211", "fabric" -> "fabric-1.21.11"
        "fabricA120B12111" -> "fabric-1.20-1.21.10"
        "fabric261" -> "fabric-26.1"
        "neoforge261" -> "neoforge-26.1"
        "neoforge1211" -> "neoforge-1.21.11"
        "neoforgeA1204B12111" -> "neoforge-1.20.4-1.21.10"
        "paper" -> "paper"
        "spigot" -> "spigot"
        "velocity" -> "velocity"
        else -> throw IllegalStateException("Unknown loader $name")
    }}"
    mainFile.changelog = rootProject.file("CHANGELOG.md").readText()
}
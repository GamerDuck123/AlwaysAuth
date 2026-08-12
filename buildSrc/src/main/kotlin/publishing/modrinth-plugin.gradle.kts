plugins {
    id("com.modrinth.minotaur")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

modrinth {
    val platformName = project.parent
        ?.takeIf { it != project.rootProject }
        ?.name
        ?: project.name

    versionNumber.set("${version as String}-${project.name}-${platformName}")
    loaders.addAll(
        when (platformName) {
            "fabric","fabric261" -> listOf("fabric", "babric", "quilt")
            "neoforge" -> listOf("neoforge")
            "paper" -> listOf("paper", "purpur")
            "spigot" -> listOf("spigot")
            "velocity" -> listOf("velocity")
            else -> throw IllegalStateException("Unknown loader $name")
        }
    )
    uploadFile.set(when (platformName) {
        "fabric" -> when (project.name) {
            "26.1", "26.1.1", "26.1.2", "26.2" -> tasks.named<Jar>("jar")
            else -> tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar")
        }
        "paper", "spigot", "fabric261", "velocity", "neoforge" -> tasks.named<Jar>("jar")
        else -> throw IllegalStateException("Unknown loader $name")
    })

    gameVersions.addAll(when (platformName) {
        "fabric261" -> when (project.name) {
            "26.1.2" -> listOf("26.1.2")
            "26.1.1" -> listOf("26.1.1")
            "26.1" -> listOf("26.1")
            "26.2" -> listOf("26.2")
            else -> throw IllegalStateException("Unknown loader $name")
        }
        "fabric" -> when (project.name) {
            "1.14.4" -> listOf("1.14.4")
            "1.15" -> listOf("1.15", "1.15.1", "1.15.2")
            "1.16" -> listOf("1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5")
            "1.17" -> listOf("1.17", "1.17.1")
            "1.18" -> listOf("1.18", "1.18.1", "1.18.2")
            "1.19" -> listOf("1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4")
            "1.20" -> listOf("1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2",
                "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10")
            "1.21.11" -> listOf("1.21.11")
            "26.1.2" -> listOf("26.1.2")
            "26.1.1" -> listOf("26.1.1")
            "26.1" -> listOf("26.1")
            "26.2" -> listOf("26.2")
            else -> throw IllegalStateException("Unknown loader $name")
        }
        "neoforge" -> when (project.name) {
            "1.20.4" -> listOf("1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2",
                "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10")
            "1.21.11" -> listOf("1.21.11")
            "26.1" -> listOf("26.1")
            "26.1.1" -> listOf("26.1.1")
            "26.1.2" -> listOf("26.1.2")
            "26.2" -> listOf("26.2")
            else -> throw IllegalStateException("Unknown loader $name")
        }
        "paper" -> listOf("1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2")
        "spigot" -> listOf("1.8.8", "1.9", "1.9.2", "1.9.4", "1.10", "1.10.2", "1.11", "1.11.1", "1.11.2", "1.12", "1.12.1",
            "1.12.2", "1.13", "1.13.1", "1.13.2",  "1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4", "1.15", "1.15.1", "1.15.2",
            "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5", "1.17", "1.17.1", "1.18", "1.18.1", "1.18.2", "1.19", "1.19.1",
            "1.19.2", "1.19.3", "1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2",
            "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2")
        "velocity" -> listOf("1.8.8", "1.9", "1.9.2", "1.9.4", "1.10", "1.10.2", "1.11", "1.11.1", "1.11.2", "1.12", "1.12.1",
            "1.12.2", "1.13", "1.13.1", "1.13.2",  "1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4", "1.15", "1.15.1", "1.15.2",
            "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5", "1.17", "1.17.1", "1.18", "1.18.1", "1.18.2", "1.19", "1.19.1",
            "1.19.2", "1.19.3", "1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2",
            "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "26.1", "26.1.1", "26.1.2")
        else -> throw IllegalStateException("Unknown loader $name")
    })

    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(rootProject.property("modrinthID") as String)
    versionType.set(rootProject.property("versionType") as String)
    syncBodyFrom.set(rootProject.file("README.md").readText())
    changelog.set(rootProject.file("CHANGELOG.md").readText())
}
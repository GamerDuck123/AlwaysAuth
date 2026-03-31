plugins {
    id("com.modrinth.minotaur")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

modrinth {
    versionNumber.set("${version as String}-${when (project.name) {
        "fabric1211" -> "fabric-1.21.11"
        "fabricA120B12111" -> "fabric-1.20-1.21.10"
        "fabric261" -> "fabric-26.1"
        "neoforge261" -> "neoforge-26.1"
        "neoforge1211" -> "neoforge-1.21.11"
        "neoforgeA1204B12111" -> "neoforge-1.20.4-1.21.10"
        "paper" -> "paper"
        "spigot" -> "spigot"
        "velocity" -> "velocity"
        else -> throw IllegalStateException("Unknown loader $name")
    }}")
    loaders.addAll(
        when (project.name) {
            "fabric1211", "fabricA120B12111", "fabric261" -> listOf("fabric", "babric", "quilt")
            "neoforge261", "neoforge1211", "neoforgeA1204B12111" -> listOf("neoforge")
            "paper" -> listOf("paper", "purpur")
            "spigot" -> listOf("spigot")
            "velocity" -> listOf("velocity")
            else -> throw IllegalStateException("Unknown loader $name")
        }
    )
    uploadFile.set(when (project.name) {
        "fabric1211", "fabricA120B12111" -> tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar")
        "paper", "spigot", "velocity", "neoforge261", "neoforge1211", "neoforgeA1204B12111", "fabric261" -> tasks.named<Jar>("jar")
        else -> throw IllegalStateException("Unknown loader $name")
    })

    gameVersions.addAll(when (project.name) {
        "fabric261", "neoforge261" -> listOf("26.1")
        "fabric1211", "neoforge1211" -> listOf("1.21.11")
        "fabricA120B12111" -> listOf("1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
            "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10")
        "neoforgeA1204B12111" -> listOf("1.20.4", "1.20.5", "1.20.6",
            "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10")
        "paper" -> listOf( "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "26.1")
        "spigot" -> listOf("1.8.8", "1.9", "1.9.2", "1.9.4", "1.10", "1.10.2", "1.11", "1.11.1", "1.11.2", "1.12", "1.12.1",
            "1.12.2", "1.13", "1.13.1", "1.13.2",  "1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4", "1.15", "1.15.1", "1.15.2",
            "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5", "1.17", "1.17.1", "1.18", "1.18.1", "1.18.2", "1.19", "1.19.1",
            "1.19.2", "1.19.3", "1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2",
            "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1")
        "velocity" -> listOf("1.8.8", "1.9", "1.9.2", "1.9.4", "1.10", "1.10.2", "1.11", "1.11.1", "1.11.2", "1.12", "1.12.1",
            "1.12.2", "1.13", "1.13.1", "1.13.2",  "1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4", "1.15", "1.15.1", "1.15.2",
            "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5", "1.17", "1.17.1", "1.18", "1.18.1", "1.18.2", "1.19", "1.19.1",
            "1.19.2", "1.19.3", "1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2",
            "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "26.1")
        else -> throw IllegalStateException("Unknown loader $name")
    })

    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(rootProject.property("modrinthID") as String)
    versionType.set(rootProject.property("versionType") as String)
    syncBodyFrom.set(rootProject.file("README.md").readText())
    changelog.set(rootProject.file("CHANGELOG.md").readText())
}
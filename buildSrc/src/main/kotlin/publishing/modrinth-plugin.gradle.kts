plugins {
    id("com.modrinth.minotaur")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

modrinth {
    versionNumber.set("${version as String}-${name}")
    loaders.addAll(
        when (project.name) {
            "fabric1211", "fabricA120B1211", "fabric261" -> listOf("fabric", "babric", "quilt")
            "neoforge261", "neoforge1211", "neoforgeA1204B12110" -> listOf("neoforge")
            "paper" -> listOf("paper", "purpur")
            "spigot" -> listOf("spigot")
            "velocity" -> listOf("velocity")
            else -> throw IllegalStateException("Unknown loader $name")
        }
    )
    uploadFile.set(when (project.name) {
        "fabric1211" -> tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar")
        "fabricA120B1211" -> tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar")
        "fabric261" -> tasks.named<Jar>("jar")
        "neoforge261", "neoforge1211", "neoforgeA1204B12110" -> tasks.named<Jar>("jar")
        "paper" -> tasks.named<Jar>("jar")
        "spigot" -> tasks.named<Jar>("jar")
        "velocity" -> tasks.named<Jar>("jar")
        else -> throw IllegalStateException("Unknown loader $name")
    })

    gameVersions.addAll(when (project.name) {
        "fabric261", "neoforge261" -> listOf("26.1")
        "fabric1211", "neoforge1211" -> listOf("1.21.11")
        "neoforgeA1204B12110" -> listOf("")
        "fabricA120B1211" -> listOf("1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
            "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10")
        "paper" -> listOf(libs.findVersion("minecraft").get().toString())
        "spigot" -> listOf(libs.findVersion("minecraft").get().toString())
        "velocity" -> listOf(libs.findVersion("minecraft").get().toString())
        else -> throw IllegalStateException("Unknown loader $name")
    })

    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(rootProject.property("modrinthID") as String)
    versionType.set(rootProject.property("versionType") as String)
    syncBodyFrom.set(rootProject.file("README.md").readText())
    changelog.set(rootProject.file("CHANGELOG.md").readText())
}
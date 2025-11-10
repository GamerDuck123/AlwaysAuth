plugins {
    id("com.modrinth.minotaur")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

modrinth {
    versionNumber.set("${version as String}-${name}")
    loaders.addAll(
        when (project.name) {
            "fabric" -> listOf("fabric", "babric", "quilt")
            "neoforge" -> listOf("neoforge")
            "paper" -> listOf("paper", "purpur")
            "spigot" -> listOf("spigot")
            "velocity" -> listOf("velocity")
            else -> throw IllegalStateException("Unknown loader $name")
        }
    )
    uploadFile.set(when (project.name) {
        "fabric" -> tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar")
        "neoforge" -> tasks.named<Jar>("jar")
        "paper" -> tasks.named<Jar>("jar")
        "spigot" -> tasks.named<Jar>("jar")
        "velocity" -> tasks.named<Jar>("jar")
        else -> throw IllegalStateException("Unknown loader $name")
    })

    gameVersions.addAll(libs.findVersion("minecraft").get().toString())
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(rootProject.property("modrinthID") as String)
    versionType.set(rootProject.property("versionType") as String)
    syncBodyFrom.set(rootProject.file("README.md").readText())
    changelog.set(rootProject.file("CHANGELOG.md").readText())
}
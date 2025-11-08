plugins {
    id("com.modrinth.minotaur")
}

modrinth {
    versionNumber.set("${version as String}-${name}")
    loaders.addAll(
        when (name) {
            "fabric" -> listOf("fabric", "babric", "quilt")
            "neoforge" -> listOf("neoforge")
            "paper" -> listOf("paper", "purpur")
            "spigot" -> listOf("spigot")
            "velocity" -> listOf("velocity")
            else -> throw IllegalStateException("Unknown loader $name")
        }
    )
    uploadFile.set(when (name) {
        "fabric" -> tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar")
        "neoforge" -> tasks.named<Jar>("jar")
        "paper" -> tasks.named<Jar>("jar")
        "spigot" -> tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")
        "velocity" -> tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")
        else -> throw IllegalStateException("Unknown loader $name")
    })

    gameVersions.addAll(rootProject.property("minecraft_version") as String)
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(rootProject.property("modrinthID") as String)
    versionType.set(rootProject.property("versionType") as String)
    syncBodyFrom.set(rootProject.file("README.md").readText())
    changelog.set(rootProject.file("CHANGELOG.md").readText())
}
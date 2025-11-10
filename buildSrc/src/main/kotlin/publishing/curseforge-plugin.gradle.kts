plugins {
    id("net.darkhax.curseforgegradle")
}

tasks.register("publishCurseForge", net.darkhax.curseforgegradle.TaskPublishCurseForge::class) {
    apiToken = System.getenv("CURSEFORGE_TOKEN")

    val projectId = findProperty("curseforgeID") as String?

    var mainFile = when (project.name) {
        "fabric" -> upload(projectId, tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar"))
        "neoforge" -> upload(projectId, tasks.named<Jar>("jar"))
        else -> throw IllegalStateException("Unknown loader $project.name")
    };
    mainFile.addModLoader(
        when (project.name) {
            "fabric" -> "Fabric"
            "neoforge" -> "NeoForge"
            else -> throw IllegalStateException("Unknown loader $project.name")
        })
    mainFile.addGameVersion(rootProject.property("minecraft_version") as String)
    mainFile.releaseType = rootProject.property("versionType") as String
    mainFile.displayName = "${project.version as String}-${project.name}"
    mainFile.changelog = rootProject.file("CHANGELOG.md").readText()
}
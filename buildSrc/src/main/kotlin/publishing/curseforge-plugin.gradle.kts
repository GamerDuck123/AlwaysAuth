plugins {
    id("net.darkhax.curseforgegradle")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

tasks.register("publishCurseForge", net.darkhax.curseforgegradle.TaskPublishCurseForge::class) {
    apiToken = System.getenv("CURSEFORGE_TOKEN")

    val projectId = findProperty("curseforgeID") as String?

    var mainFile = when (name) {
        "fabric" -> upload(projectId, tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar"))
        "neoforge" -> upload(projectId, tasks.named<Jar>("jar"))
        else -> throw IllegalStateException("Unknown loader $name")
    };
    mainFile.addModLoader(
        when (name) {
            "fabric" -> "Fabric"
            "neoforge" -> "NeoForge"
            else -> throw IllegalStateException("Unknown loader $name")
        })

    mainFile.addGameVersion(libs.findVersion("minecraft").get().toString())
    mainFile.releaseType = property("versionType") as String
    mainFile.displayName = "${project.version as String}-${project.name}"
    mainFile.changelog = rootProject.file("CHANGELOG.md").readText()
}
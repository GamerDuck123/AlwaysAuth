plugins {
    id("net.darkhax.curseforgegradle")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

tasks.register("publishCurseForge", net.darkhax.curseforgegradle.TaskPublishCurseForge::class) {
    apiToken = System.getenv("CURSEFORGE_TOKEN")

    val projectId = rootProject.property("curseforgeID") as String?

    var mainFile = when (project.name) {
        "fabricA26" -> upload(projectId, tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar"))
        "fabricA120B26" -> upload(projectId, tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar"))
        "neoforge" -> upload(projectId, tasks.named<Jar>("jar"))
        else -> throw IllegalStateException("Unknown loader $project.name")
    };
    mainFile.addModLoader(
        when (project.name) {
            "fabricA26" -> "Fabric"
            "fabricA120B26" -> "Fabric"
            "neoforge" -> "NeoForge"
            else -> throw IllegalStateException("Unknown loader $project.name")
        })

    mainFile.addGameVersion(when (project.name) {
        "fabricA26" -> libs.findVersion("minecraft").get().toString()
        "fabricA120B26" -> libs.findVersion("minecraft").get().toString()
        "neoforge" -> libs.findVersion("minecraft").get().toString()
        else -> throw IllegalStateException("Unknown loader $project.name")
    })

    mainFile.releaseType = rootProject.property("versionType") as String
    mainFile.displayName = "${rootProject.version as String}-${project.name}"
    mainFile.changelog = rootProject.file("CHANGELOG.md").readText()
}
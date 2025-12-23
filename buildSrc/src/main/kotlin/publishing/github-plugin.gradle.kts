plugins {
    id("com.github.breadmoirai.github-release")
}

githubRelease {
    token(System.getenv("GITHUB_TOKEN"))
    owner.set("GamerDuck123")
    repo.set(rootProject.property("githubID") as String)

    tagName.set("v${rootProject.version as String}")
    releaseName.set("${rootProject.name} v${rootProject.version as String}")
    targetCommitish.set("master")

    body.set(rootProject.file("CHANGELOG.md").readText())

    draft.set(false)
    prerelease.set((rootProject.property("versionType") as String) != "release")

    releaseAssets.setFrom(when (project.name) {
        "standalone" -> tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").flatMap { it.archiveFile }
        "fabric" -> tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar").flatMap { it.archiveFile }
        "neoforge" -> tasks.named<Jar>("jar").flatMap { it.archiveFile }
        "paper" -> tasks.named<Jar>("jar").flatMap { it.archiveFile }
        "spigot" -> tasks.named<Jar>("jar").flatMap { it.archiveFile }
        "velocity" -> tasks.named<Jar>("jar").flatMap { it.archiveFile }
        else -> throw IllegalStateException("Unknown module for GitHub publishing: ${project.name}")
    })

    overwrite.set(false)
    allowUploadToExisting.set(true)
    apiEndpoint.set("https://api.github.com")
}
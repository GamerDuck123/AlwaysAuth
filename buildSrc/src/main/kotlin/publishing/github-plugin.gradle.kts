plugins {
    id("com.github.breadmoirai.github-release")
}

githubRelease {

    val platformName = project.parent
        ?.takeIf { it != project.rootProject }
        ?.name
        ?: project.name


    token(System.getenv("GITHUB_TOKEN"))
    owner.set("GamerDuck123")
    repo.set(rootProject.property("githubID") as String)

    tagName.set("v${rootProject.version as String}")
    releaseName.set("${rootProject.name} v${rootProject.version as String}")
    targetCommitish.set("master")

    body.set(rootProject.file("CHANGELOG.md").readText())

    draft.set(false)
    prerelease.set((rootProject.property("versionType") as String) != "release")

    releaseAssets.setFrom(when (platformName) {
        "fabric" -> when (project.name) {
            "26.1", "26.1.1", "26.1.2", "26.2" -> tasks.named<Jar>("jar").flatMap { it.archiveFile }
            else -> tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar").flatMap { it.archiveFile }
        }
        "neoforge", "fabric261", "standalone", "paper", "spigot", "velocity" -> tasks.named<Jar>("jar").flatMap { it.archiveFile }
        else -> throw IllegalStateException("Unknown module for GitHub publishing: ${project.name}")
    })

    overwrite.set(false)
    allowUploadToExisting.set(true)
    apiEndpoint.set("https://api.github.com")
}
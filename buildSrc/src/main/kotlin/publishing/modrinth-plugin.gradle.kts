plugins {
    id("com.modrinth.minotaur")
}

modrinth {
    versionNumber.set("${version as String}-${name}")
    loaders.addAll(
        when (name) {
            "fabric" -> listOf("fabric", "babric", "quilt")
            "neoforge" -> listOf("neoforge")
            "sponge" -> listOf("sponge")
            "paper" -> listOf("paper", "purpur")
            "spigot" -> listOf("spigot")
            "velocity" -> listOf("velocity")
            "bungeecord" -> listOf("bungeecord")
            "waterfall" -> listOf("waterfall")
            else -> throw IllegalStateException("Unknown loader $name")
        }
    )
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(rootProject.property("modrinthID") as String)
    versionType.set(rootProject.property("versionType") as String)
    syncBodyFrom.set(rootProject.file("README.md").readText())
    changelog.set(rootProject.file("CHANGELOG.md").readText())
}
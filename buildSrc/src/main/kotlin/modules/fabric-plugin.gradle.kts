plugins {
    id("root-plugin")

    id("fabric-loom")
    id("modrinth-plugin")
    id("curseforge-plugin")
    id("github-plugin")
}

repositories {
    maven("https://maven.fabricmc.net")
    maven("https://maven.parchmentmc.org/")
}
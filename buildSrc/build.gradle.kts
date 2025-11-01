plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net")
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:${rootProject.property("shadow_jar")}")
    implementation("io.papermc.paperweight:paperweight-userdev:${rootProject.property("paper_weight")}")
    implementation("net.neoforged:moddev-gradle:${rootProject.property("neo_forge")}")
    implementation("fabric-loom:fabric-loom.gradle.plugin:${rootProject.property("fabric_loom")}")
    implementation("com.modrinth.minotaur:Minotaur:${rootProject.property("minotaur")}")
    implementation("net.darkhax.curseforgegradle:net.darkhax.curseforgegradle.gradle.plugin:${rootProject.property("curseforge")}")
    implementation("io.papermc.hangar-publish-plugin:io.papermc.hangar-publish-plugin.gradle.plugin:${rootProject.property("hangar")}")
}
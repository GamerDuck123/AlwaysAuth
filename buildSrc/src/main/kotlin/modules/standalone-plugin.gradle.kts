plugins {
    id("root-plugin")
    id("com.gradleup.shadow")
}

repositories {
    maven("https://libraries.minecraft.net")

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
}
rootProject.name = "AlwaysAuth"

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    create(":fabric") {
        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        versions("1.20", "1.21.11", "26.1")
        vcsVersion = "1.21.11"
    }
}

include("common", "paper", "fabric", "neoforge261", "neoforgeA1204B12111",  "neoforge1211", "spigot", "velocity", "standalone")
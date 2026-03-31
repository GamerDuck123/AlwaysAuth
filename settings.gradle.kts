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
        versions("1.20", "1.21.11")
        vcsVersion = "1.21.11"
    }
    create(":neoforge") {
        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        versions("1.20.4", "1.21.11")
        vcsVersion = "1.21.11"
    }
}

include("common", "paper", "fabric", "fabric261", "neoforge", "neoforge261", "spigot", "velocity", "standalone")
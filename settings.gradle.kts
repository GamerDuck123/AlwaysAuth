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
        versions("26.1", "26.1.1", "26.1.2", "26.2", "1.20", "1.21.11", "1.19", "1.18", "1.17", "1.16", "1.15", "1.14.4")
        vcsVersion = "26.2"
    }
    create(":neoforge") {
        versions("1.20.4", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2")
        vcsVersion = "26.2"
    }
}

include("common", "paper", "fabric", "neoforge", "spigot", "velocity", "standalone")
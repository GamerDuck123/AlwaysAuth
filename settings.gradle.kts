rootProject.name = "AlwaysAuth"
include("common", "paper", "fabric", "neoforge", "spigot", "bungeecord", "velocity")

pluginManagement {
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    }
}
include("standalone")
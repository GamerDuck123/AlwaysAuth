dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
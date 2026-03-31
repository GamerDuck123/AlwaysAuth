plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net")
    maven("https://jitpack.io")
    maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
}

dependencies {
    implementation(libs.bundles.build)
}
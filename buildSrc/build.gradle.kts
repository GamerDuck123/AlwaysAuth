plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net")
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.bundles.build)
}
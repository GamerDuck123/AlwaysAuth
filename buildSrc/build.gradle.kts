plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net")
}

dependencies {
    implementation(libs.bundles.build)
}
plugins {
    id("common-plugin")
}

dependencies {
    compileOnly(libs.mixin)
    annotationProcessor("${libs.mixin.get()}:processor")

    compileOnly(libs.extras)
    annotationProcessor(libs.extras)

    implementation(libs.gson)
    compileOnly(libs.authlib)
}

neoForge {
    neoFormVersion = "${libs.versions.minecraft.get()}-${libs.versions.neoform.get()}"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

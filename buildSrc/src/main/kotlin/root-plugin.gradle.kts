plugins {
    `java-library`
    `maven-publish`
}

base {
    archivesName.set("${rootProject.name}-${name}")
}

repositories {
    maven("https://jitpack.io/")
    maven("https://libraries.minecraft.net")

    mavenCentral()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of("21"))
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }
}
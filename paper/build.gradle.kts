plugins {
    id("paper-plugin")
}

dependencies {
    paperweight.paperDevBundle("${libs.versions.minecraft.get()}-R0.1-SNAPSHOT")
    compileOnly(libs.brigadier)
}
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION


tasks.register<Copy>("copyCommonSources") {
    from("$rootDir/common/src/main/java") {
        exclude("me/gamerduck/${project.property("modid")}/mixin/**")
        into("common/java")

        filter { line: String ->
            line.replace("@version@", project.version.toString())
        }
        filter { line: String ->
            line.replace("@modrinthToken@", project.property("modrinthID") as String)
        }
        filter { line: String ->
            line.replace("@loader@", project.name)
        }
    }

    from("$rootDir/common/src/main/resources/templates") {
        include("paper-plugin.yml")
        into("common/resources")

        filesMatching("paper-plugin.yml") {
            expand(mapOf(
                "name" to rootProject.name,
                "group" to project.group,
                "version" to rootProject.version,
                "mainFile" to "${rootProject.name}Plugin",
                "description" to rootProject.description,
                "apiVersion" to libs.versions.minecraft.get()
            ))
        }
    }
    into("${layout.buildDirectory}/generated/sources")
}

sourceSets {
    main {
        java {
            srcDir("${layout.buildDirectory}/generated/sources/common/java")
        }
        resources {
            srcDir("${layout.buildDirectory}/generated/sources/common/resources")
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("copyCommonSources")
}

tasks {
    processResources {
        dependsOn("copyCommonSources")
    }
}

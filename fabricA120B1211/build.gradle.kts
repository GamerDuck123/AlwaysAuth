
plugins {
    id("fabric-plugin")
}

dependencies {
    minecraft("com.mojang:minecraft:1.20")

    mappings("net.fabricmc:yarn:1.20+build.1:v2")

    modImplementation(libs.fabric.loader)

    implementation(libs.authlib)

    implementation(libs.h2)
    include(libs.h2)
    implementation(libs.gson)
    include(libs.gson)
}

tasks.register<Copy>("copyCommonSources") {
    from("$rootDir/common/src/main/java") {
        exclude("me/gamerduck/${project.property("modid")}/reflection/**")
        exclude("me/gamerduck/${project.property("modid")}/mixin/mixins/**")
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

    from("$rootDir/common/src/main/resources") {
        include("alwaysauth.accesswidener")

        into("common/resources")
    }

    from("$rootDir/common/src/main/resources/assets") {
        include("icon.png")

        into("common/resources/assets/alwaysauth")
    }

    from("$rootDir/common/src/main/resources/templates") {
        include("${project.property("modid")}.mixins.json")
        include("fabric.mod.json")
        into("common/resources")

        filesMatching("${project.property("modid")}.mixins.json") {
            expand(mapOf(
                "group" to project.group,
                "compatibilityLevel" to "JAVA_17"
            ))
        }

        filesMatching("fabric.mod.json") {
            expand(mapOf(
                "name" to rootProject.name,
                "group" to project.group,
                "version" to rootProject.version,
                "modid" to rootProject.property("modid"),
                "mainFile" to "${rootProject.name}Mod",
                "description" to project.description,
                "fabricApiVersion" to libs.versions.api.get(),
                "fabricLoaderVersion" to libs.versions.loader.get(),
                "minecraftVersion" to libs.versions.minecraft.get(),
                "author" to project.property("author"),
                "website" to project.property("website"),
                "sources" to project.property("sources"),
                "issues" to project.property("issues"),
                "accessWidenerEnd" to "accesswidener",
                "fabricLoader" to ">=15",
                "minecraftVersions" to ">=1.20 <=1.21.10",
                "javaVersions" to ">=17",
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

loom {
        accessWidenerPath.set(file("../common/src/main/resources/${project.property("modid")}.accesswidener"))

    mods {
        create(project.property("modid").toString()) {
            sourceSet(sourceSets.main.get())
        }
    }

}

tasks {
    compileJava {

        dependsOn("copyCommonSources")
    }
    build {
        dependsOn(remapJar)
    }
    remapJar {
        destinationDirectory.set(file("${rootProject.layout.projectDirectory}/build/all"))
        archiveFileName.set("${rootProject.name}-fabric-120-12110-${rootProject.version}.jar")
    }
    processResources {
        dependsOn("copyCommonSources")
    }
}
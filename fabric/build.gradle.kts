plugins {
    id("fabric-plugin")
}

val requiredJava = when {
    sc.current.parsed >= "1.21.11" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}


dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")

    if (sc.current.parsed <= "1.21.11") mappings(loom.officialMojangMappings())

    if (sc.current.parsed <= "1.21.11") {
        modImplementation(libs.fabric.loader)

        modImplementation(libs.authlib)

        modImplementation(libs.h2)
        include(libs.h2)
        modImplementation(libs.gson)
        include(libs.gson)
    } else {
        implementation(libs.fabric.loader)

        implementation(libs.authlib)

        implementation(libs.h2)
        include(libs.h2)
        implementation(libs.gson)
        include(libs.gson)
    }
}

tasks.register<Copy>("copyCommonSources") {
    from("$rootDir/common/src/main/java") {
        exclude("me/gamerduck/${project.property("modid")}/reflection/**")
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
        if (sc.current.parsed >= "1.21.11") {
            include("12111alwaysauth.classtweaker")
            filesMatching("12111alwaysauth.classtweaker") {
                relativePath = RelativePath(true, "common/resources/alwaysauth.classtweaker")
            }
        } else if (sc.current.parsed >= "26.1") {
            include("alwaysauth.classtweaker")
        } else {
            include("alwaysauth.accesswidener")
        }

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
            expand(
                mapOf(
                    "group" to "me.gamerduck.alwaysauth.fabric",
                    "compatibilityLevel" to "JAVA_17"
                )
            )
        }

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "name" to rootProject.name,
                    "group" to "me.gamerduck.alwaysauth.fabric",
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
                    "accessWidenerEnd" to if (sc.current.parsed >= "1.21.11") "classtweaker" else "accesswidener",
                    "fabricLoader" to ">=0.15",
                    "minecraftVersions" to ">=${sc.current.version}",
                    "javaVersions" to ">=17",
                )
            )
        }
    }

    into(layout.buildDirectory.dir("generated/sources"))
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/sources/common/java"))
        }
        resources {
            srcDir(layout.buildDirectory.dir("generated/sources/common/resources"))
        }
    }
}

loom {
    accessWidenerPath.set(rootProject.file("common/src/main/resources/${rootProject.property("modid")}.classtweaker"))

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
    jar {
        destinationDirectory.set(file("${rootProject.layout.projectDirectory}/build/all"))
        archiveFileName.set("${rootProject.name}-fabric-261-${rootProject.version}.jar")
    }
    build {
//        destinationDirectory.set(file("${rootProject.layout.projectDirectory}/build/all"))
    }
    processResources {
        dependsOn("copyCommonSources")
    }
}
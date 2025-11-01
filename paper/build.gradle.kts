import io.papermc.hangarpublishplugin.model.Platforms

dependencies {
    paperweight.paperDevBundle("${libs.versions.minecraft.get()}-R0.1-SNAPSHOT")
    compileOnly(libs.brigadier)
}

modrinth {
    uploadFile.set(tasks.jar)
    gameVersions.addAll(libs.versions.minecraft.get())
}


hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set(property("versionType") as String)
        id.set(property("hangarID") as String)
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        changelog.set(rootProject.file("CHANGELOG.md").readText())
        platforms {
            register(Platforms.PAPER) {
                jar.set(tasks.jar.flatMap { it.archiveFile })

                val versions: List<String> = (libs.versions.minecraft.get())
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
        }
    }
}

tasks.register<Copy>("copyCommonSources") {
    from("$rootDir/common/src/main/java") {
        exclude("me/gamerduck/${project.property("modid")}/mixin/**")
        into("common/java")
    }
    from("$rootDir/common/src/main/resources") {
        exclude("META-INF/**")
        exclude("templates/**")
        exclude("${project.property("modid")}.accesswidener")
        exclude("${project.property("modid")}.mixins.json")
        into("common/resources")
    }

//    Fill in default config from commons
//    from("$rootDir/common/src/main/resources/templates") {
//        include("${project.property("modid")}.properties")
//        into("common/resources")
//        includeEmptyDirs = false
//
//        filesMatching("**/${project.property("modid")}.properties") {
//            expand(mapOf(
//                "default_path" to "plugins/${project.name}/storage",
//            ))
//        }
//    }


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
    assemble {
        dependsOn(reobfJar)
    }
    processResources {
        dependsOn("copyCommonSources")
        val props = mapOf(
            "name" to rootProject.name,
            "group" to project.group,
            "version" to project.version,
            "mainFile" to "${rootProject.name}Plugin",
            "description" to project.description,
            "apiVersion" to libs.versions.minecraft.get()
        )

        from("src/main/templates") {
            listOf(
                "paper-plugin.yml",
            ).forEach {
                filesMatching(it) {
                    expand(props)
                }
            }
        }
        into(layout.buildDirectory.dir("src/main/resources"))
    }
}

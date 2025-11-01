import net.neoforged.nfrtgradle.CreateMinecraftArtifacts
neoForge {
    version = libs.versions.neo.get()

    parchment {
        mappingsVersion = libs.versions.parchment.mappings.get()
        minecraftVersion = libs.versions.minecraft.get()
    }

    mods {
        create(project.property("modid").toString()) {
            sourceSet(sourceSets.main.get())
        }
    }
}
modrinth {
    uploadFile.set(tasks.jar)
    gameVersions.addAll(libs.versions.minecraft.get())
}

tasks.register("publishCurseForge", net.darkhax.curseforgegradle.TaskPublishCurseForge::class) {
    apiToken = System.getenv("CURSEFORGE_TOKEN")

    val projectId = findProperty("curseforgeID") as String?

    val mainFile = upload(projectId, tasks.jar)
    mainFile.addModLoader("NeoForge")
    mainFile.addGameVersion(libs.versions.minecraft.get())
    mainFile.releaseType = property("versionType") as String
    mainFile.displayName = "${project.version as String}-${project.name}"
    mainFile.changelog = rootProject.file("CHANGELOG.md").readText()
}

val localRuntime: Configuration by configurations.creating
configurations {
    runtimeClasspath.get().extendsFrom(localRuntime)
}


tasks.register<Copy>("copyCommonSources") {
    from("$rootDir/common/src/main/java") {
        into("common/java")
    }
    from("$rootDir/common/src/main/resources") {
        exclude("${project.property("modid")}.accesswidener")
        exclude("templates/**")
        into("common/resources")
        filesMatching("**/${project.property("modid")}.mixins.json") {
            expand(mapOf(
                "group" to rootProject.group,
            ))
        }
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

tasks.named<CreateMinecraftArtifacts>("createMinecraftArtifacts") {
    dependsOn("copyCommonSources")
}


tasks {
    processResources {
        dependsOn("copyCommonSources")
        val props = mapOf(
            "minecraftVersion" to libs.versions.minecraft.get(),
            "neoVersion" to libs.versions.neo.get(),
            "modid" to rootProject.property("modid"),
            "modName" to rootProject.name,
            "modLicense" to project.property("license"),
            "issueTracker" to project.property("issues"),
            "modVersion" to project.property("version").toString().replace("v", ""),
            "modAuthor" to project.property("author"),
            "modDescription" to project.property("description")
        )

        from("src/main/templates") {
            listOf(
                "META-INF/neoforge.mods.toml",
            ).forEach {
                filesMatching(it) {
                    expand(props)
                }
            }
        }
        into(layout.buildDirectory.dir("src/main/resources"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
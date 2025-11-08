import net.neoforged.nfrtgradle.CreateMinecraftArtifacts
dependencies {
    implementation(libs.h2)
}

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

val localRuntime: Configuration by configurations.creating
configurations {
    runtimeClasspath.get().extendsFrom(localRuntime)
}


tasks.register<Copy>("copyCommonSources") {
    from("$rootDir/common/src/main/java") {
        exclude("me/gamerduck/${project.property("modid")}/reflection/**")
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
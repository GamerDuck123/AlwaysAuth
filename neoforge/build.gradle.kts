plugins {
    id("neoforge-plugin")
}

val requiredJava = when {
    sc.current.parsed >= "1.21.11" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

dependencies {
    jarJar(libs.h2) {
        version {
            strictly("[${libs.versions.h2.get()}]")
        }
    }
    jarJar(libs.mysql) {
        version {
            strictly("[${libs.versions.mysql.get()}]")
        }
    }
    jarJar(libs.mariadb) {
        version {
                strictly("[${libs.versions.mariadb.get()}]")
        }
    }
}


neoForge {

    version = property("deps.neoforge") as String

    if (sc.current.parsed <= "1.21.11") {
        parchment {
            mappingsVersion = property("deps.mappings") as String
            minecraftVersion = "${sc.current.version}"
        }
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
        include("accesstransformer.cfg")

        into("common/resources/META-INF")
    }

    from("$rootDir/common/src/main/resources/assets/lang") {
        include("*")

        into("common/resources/lang")
    }

    from("$rootDir/common/src/main/resources/templates") {
        include("${project.property("modid")}.mixins.json")
        include("neoforge.mods.toml")
        into("common/resources")

        filesMatching("${project.property("modid")}.mixins.json") {
            expand(mapOf(
                "group" to project.group,
                "compatibilityLevel" to "JAVA_17"
            ))
        }

        filesMatching("neoforge.mods.toml") {
            expand(mapOf(
                "minecraftVersion" to property("mod.mc_dep") as String,
                "neoVersion" to property("mod.neoforge_dep") as String,
                "modid" to rootProject.property("modid"),
                "modName" to rootProject.name,
                "modLicense" to project.property("license"),
                "issueTracker" to project.property("issues"),
                "modVersion" to rootProject.property("version").toString(),
                "modAuthor" to project.property("author"),
                "modDescription" to project.property("description")
            ))
            relativePath = RelativePath(true, "common/resources/META-INF/$name")
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


tasks.named("createMinecraftArtifacts") {
    dependsOn("copyCommonSources")
}


tasks {
    compileJava {
        dependsOn("copyCommonSources")
    }
    processResources {
        dependsOn("copyCommonSources")
    }
    jar {
        destinationDirectory.set(file("${rootProject.layout.projectDirectory}/build/all"))
        archiveFileName.set("${rootProject.name}-neoforge-1211-${rootProject.version}.jar")
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
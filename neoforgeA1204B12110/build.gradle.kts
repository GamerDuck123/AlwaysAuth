plugins {
    id("neoforge-plugin")
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

    version = "20.4.251"

    parchment {
        mappingsVersion = "2024.04.14"
        minecraftVersion = "1.20.4"
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
//        include("${project.property("modid")}.mixins.json")
        include("1.20.neoforge.mods.toml")
        into("common/resources")

//        filesMatching("${project.property("modid")}.mixins.json") {
//            expand(mapOf(
//                "group" to project.group,
//                "compatibilityLevel" to "JAVA_17"
//            ))
//        }

        filesMatching("1.20.neoforge.mods.toml") {
            expand(mapOf(
                "minecraftVersion" to "[1.20.4, 1.21.10]",// 1.20.6]",
                "neoVersion" to "[20.4.251, 21.10.64]", //20.6.139]",
                "loaderVersion" to "[1,)",
                "modid" to rootProject.property("modid"),
                "modName" to rootProject.name,
                "modLicense" to project.property("license"),
                "issueTracker" to project.property("issues"),
                "modVersion" to rootProject.property("version").toString(),
                "modAuthor" to project.property("author"),
                "modDescription" to project.property("description")
            ))
            relativePath = RelativePath(true, "common/resources/META-INF/neoforge.mods.toml")
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

tasks.named("createMinecraftArtifacts") {
    dependsOn("copyCommonSources")
}


tasks {
    processResources {
        dependsOn("copyCommonSources")
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
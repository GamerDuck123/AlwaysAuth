plugins {
    id("fabric-plugin") apply false
    id("fabric-A261-plugin") apply false
}

fun isObfuscated() = sc.current.parsed <= "1.21.11"

val pluginId = when {
    isObfuscated() -> "fabric-plugin"
    else -> "fabric-A261-plugin"
}
pluginId?.let { pluginManager.apply(it) }

val requiredJava = when {
    sc.current.parsed >= "1.21.11" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

val loom = extensions.getByType(net.fabricmc.loom.api.LoomGradleExtensionAPI::class.java)

fun chooseImplementation(dep: Any) {
    dependencies.add(
        if (isObfuscated()) "modImplementation" else "implementation",
        dep
    )
}
dependencies {
    "minecraft"("com.mojang:minecraft:${sc.current.version}")
    if (isObfuscated()) "mappings"(loom.officialMojangMappings())
    chooseImplementation("net.fabricmc:fabric-loader:${project.property("mod.fabric_loader_dep")}")
    chooseImplementation("com.mojang:authlib:${project.property("mod.authlib")}")
    chooseImplementation(libs.h2)
    "include"(libs.h2)
    chooseImplementation(libs.gson)
    "include"(libs.gson)
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
            include("1.21.11.alwaysauth.classtweaker")
            filesMatching("1.21.11.alwaysauth.classtweaker") {
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
                    "compatibilityLevel" to requiredJava.name
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
                    "fabricLoaderVersion" to "${project.property("mod.fabric_loader_dep")}",
                    "minecraftVersion" to sc.current.version,
                    "author" to project.property("author"),
                    "website" to project.property("website"),
                    "sources" to project.property("sources"),
                    "issues" to project.property("issues"),
                    "accessWidenerEnd" to "${project.property("mod.accesswidener_type")}",
                    "fabricLoader" to ">=${project.property("mod.fabric_loader_dep")}",
                    "minecraftVersions" to "${project.property("mod.mc_dep")}",
                    "javaVersions" to "${project.property("mod.java_dep")}",
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

extensions.configure(net.fabricmc.loom.api.LoomGradleExtensionAPI::class.java) {
    accessWidenerPath.set(rootProject.file("common/src/main/resources/${rootProject.property("modid")}.${project.property("mod.accesswidener_type")}"))
    mods {
        register(project.property("modid").toString()) {
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
        archiveFileName.set("${rootProject.name}-fabric-${sc.current.version}-${rootProject.version}.jar")
    }
    processResources {
        dependsOn("copyCommonSources")
    }
}
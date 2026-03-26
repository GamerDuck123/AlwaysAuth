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
        exclude("META-INF/**")
        exclude("templates/**")
        exclude("${project.property("modid")}.classtweaker")
        into("common/resources")
        filesMatching("**/${project.property("modid")}.mixins.json") {
            expand(mapOf(
                "group" to rootProject.group.toString() + ".mixin.mixins.p26",
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
    }
    processResources {
        dependsOn("copyCommonSources")
        val props = mapOf(
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
            "issues" to project.property("issues")
        )

        from("src/main/templates") {
            listOf(
                "fabric.mod.json",
            ).forEach {
                filesMatching(it) {
                    expand(props)
                }
            }
        }
        into(layout.buildDirectory.dir("src/main/resources"))
    }
}
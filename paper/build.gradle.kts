plugins {
    id("paper-plugin")
}

dependencies {
    paperweight.paperDevBundle("${libs.versions.minecraft.get()}-R0.1-SNAPSHOT")
    compileOnly(libs.brigadier)
}

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
    from("$rootDir/common/src/main/resources") {
        exclude("META-INF/**")
        exclude("templates/**")
        exclude("${project.property("modid")}.accesswidener")
        exclude("${project.property("modid")}.mixins.json")
        into("common/resources")
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
    assemble {
        dependsOn(reobfJar)
    }
    processResources {
        dependsOn("copyCommonSources")
        val props = mapOf(
            "name" to rootProject.name,
            "group" to project.group,
            "version" to rootProject.version,
            "mainFile" to "${rootProject.name}Plugin",
            "description" to rootProject.description,
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

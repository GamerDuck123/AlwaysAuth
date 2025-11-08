import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.exclude

dependencies {
    compileOnly("org.spigotmc:spigot-api:${libs.versions.minecraft.get()}-R0.1-SNAPSHOT")
    implementation(libs.authlib)
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
    processResources {
        dependsOn("copyCommonSources")
        val props = mapOf(
            "name" to rootProject.name,
            "group" to project.group,
            "version" to project.version,
            "description" to rootProject.description,
            "mainFile" to "${rootProject.name}Plugin",
            "apiVersion" to libs.versions.minecraft.get()
        )

        from("src/main/templates") {
            listOf(
                "plugin.yml",
            ).forEach {
                filesMatching(it) {
                    expand(props)
                }
            }
        }
        into(layout.buildDirectory.dir("src/main/resources"))
    }
    build {
        dependsOn("shadowJar")
    }
//    shadowJar {
//        dependencies {
//            exclude(dependency("com.mojang:brigadier"))
//        }
//
//        relocate("me.lucko.commodore", "me.gamerduck.rules.bukkit.commodore")
//        archiveClassifier.set("")
//    }
}

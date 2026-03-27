plugins {
    id("spigot-plugin")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:${libs.versions.minecraft.get()}-R0.1-SNAPSHOT")
    implementation(libs.authlib)
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

    from("$rootDir/common/src/main/resources/templates") {
        include("plugin.yml")
        into("common/resources")

        filesMatching("plugin.yml") {
            expand(mapOf(
                "name" to rootProject.name,
                "group" to project.group,
                "version" to rootProject.version,
                "description" to rootProject.description,
                "mainFile" to "${rootProject.name}Plugin",
                "apiVersion" to libs.versions.minecraft.get()
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

tasks {
    processResources {
    }
}

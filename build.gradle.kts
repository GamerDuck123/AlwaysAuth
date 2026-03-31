plugins {
    id("root-plugin")
}


defaultTasks("build")

rootProject.group = project.property("group") as String
rootProject.version = project.property("version") as String
rootProject.description = project.property("description") as String

allprojects {
    if (this.name != rootProject.name) {
        project.version = rootProject.version
        project.group = "${rootProject.group}.${this.name}"
    }
}

tasks {
    publish {
//        dependsOn(subprojects.filter { it.name in listOf("paper", "fabric261", "fabric1211", "fabricA120B12111", "neoforge261", "neoforge1211", "neoforgeA1204B12111", "spigot", "velocity") }.map { it.tasks.named("modrinth") })
//        dependsOn(subprojects.filter { it.name in listOf("paper", "velocity") }.map { it.tasks.named("publishPluginPublicationToHangar") })
        dependsOn(subprojects.filter { it.name in listOf("fabric261", "fabric1211", "fabricA120B12111", "neoforge261", "neoforge1211", "neoforgeA1204B12111") }.map { it.tasks.named("publishCurseForge") })
//        dependsOn(subprojects.filter { it.name in listOf("standalone", "paper", "fabric261", "fabric1211", "fabricA120B12111", "neoforge261", "neoforge1211", "neoforgeA1204B12111", "spigot", "velocity") }.map { it.tasks.named("githubRelease") })
    }

    register<Copy>("singlePublish") {
        val platforms = (project.findProperty("platform") as String?)
            ?.split(",")?.map { it.trim() }

        fun List<String>.filterByPlatform() =
            if (platforms != null) intersect(platforms.toSet()).toList() else this

        dependsOn(subprojects.filter { it.name in listOf("paper", "fabric261", "fabric1211", "fabricA120B12111", "neoforge261", "neoforge1211", "neoforgeA1204B12111", "spigot", "velocity").filterByPlatform() }.map { it.tasks.named("modrinth") })
        dependsOn(subprojects.filter { it.name in listOf("paper", "velocity").filterByPlatform() }.map { it.tasks.named("publishPluginPublicationToHangar") })
//        dependsOn(subprojects.filter { it.name in listOf("fabric261", "fabric1211", "fabricA120B12111", "neoforge261", "neoforge1211", "neoforgeA1204B12111").filterByPlatform() }.map { it.tasks.named("publishCurseForge") })
        dependsOn(subprojects.filter { it.name in listOf("standalone", "paper", "fabric261", "fabric1211", "fabricA120B12111", "neoforge261", "neoforge1211", "neoforgeA1204B12111", "spigot", "velocity").filterByPlatform() }.map { it.tasks.named("githubRelease") })
    }

    assemble {
        dependsOn(subprojects.filter { it.name !in listOf("common", "fabric", "neoforge") }.map {
            it.tasks.named("clean")
            it.tasks.named("copyCommonSources")
            it.tasks.named("processResources")
            it.tasks.named("build")
        })
    }
    register<Copy>("copyCommonSources") {
        dependsOn(subprojects.filter { it.name !in listOf("common", "fabric", "neoforge") }.map {
            it.tasks.named("copyCommonSources")
        })
    }
    withType<JavaCompile>().configureEach {
        enabled = false
    }
    named("jar").configure {
        enabled = false
    }
    named("build").configure {
        enabled = false
    }
}
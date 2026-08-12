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

fun Project.belongsTo(vararg names: String): Boolean {
    val targets = names.map { it.lowercase() }.toSet()

    var current: Project? = this

    while (current != null && current != rootProject) {
        if (current.name.lowercase() in targets) {
            return true
        }

        current = current.parent
    }

    return false
}

tasks {

    publish {
        dependsOn(
            subprojects
                .filter { it.belongsTo("paper", "fabric", "neoforge", "spigot", "velocity") }
                .mapNotNull { it.tasks.findByName("modrinth") }
        )

//        dependsOn(
//            subprojects
//                .filter { it.belongsTo("paper", "velocity") }
//                .mapNotNull { it.tasks.findByName("publishPluginPublicationToHangar") }
//        )

        dependsOn(
            subprojects
                .filter { it.belongsTo("fabric", "neoforge") }
                .mapNotNull { it.tasks.findByName("publishCurseForge") }
        )

        dependsOn(
            subprojects
                .filter { it.belongsTo("standalone", "paper", "fabric", "neoforge", "spigot", "velocity") }
                .mapNotNull { it.tasks.findByName("githubRelease") }
        )
    }

    register<Copy>("singlePublish") {
        val platforms = (project.findProperty("platform") as String?)
            ?.split(",")?.map { it.trim() }

        fun List<String>.filterByPlatform() =
            if (platforms != null) intersect(platforms.toSet()).toList() else this

        dependsOn(subprojects.filter { it.name in listOf("paper", "fabric", "neoforge", "spigot", "velocity").filterByPlatform() }.map { it.tasks.named("modrinth") })
        dependsOn(subprojects.filter { it.name in listOf("paper", "velocity").filterByPlatform() }.map { it.tasks.named("publishPluginPublicationToHangar") })
        dependsOn(subprojects.filter { it.name in listOf("fabric", "neoforge").filterByPlatform() }.map { it.tasks.named("publishCurseForge") })
        dependsOn(subprojects.filter { it.name in listOf("standalone", "paper", "fabric", "neoforge", "spigot", "velocity").filterByPlatform() }.map { it.tasks.named("githubRelease") })
    }

    assemble {
        dependsOn(subprojects.filter { it.name !in listOf("common", "fabric", "neoforge", "fabric261") }.map {
            it.tasks.named("clean")
            it.tasks.named("copyCommonSources")
            it.tasks.named("processResources")
            it.tasks.named("build")
        })
    }
    register<Copy>("copyCommonSources") {
        dependsOn(subprojects.filter { it.name !in listOf("common", "fabric", "neoforge", "fabric261") }.map {
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
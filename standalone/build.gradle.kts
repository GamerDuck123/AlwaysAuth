plugins {
    id("standalone-plugin")
}

dependencies {
    compileOnly(libs.authlib)

    compileOnly(libs.h2)
    compileOnly(libs.gson)
    compileOnly(libs.mysql)
    compileOnly(libs.mariadb)
}

tasks.register<Copy>("copyCommonSources") {
    from("$rootDir/common/src/main/java") {
        exclude("me/gamerduck/${project.property("modid")}/mixin/**")
        exclude("me/gamerduck/${project.property("modid")}/reflection/**")
        into("common/java")

        filter { line: String ->
            line.replace("@version@", project.version.toString())
        }
        filter { line: String ->
            line.replace("@modrinthToken@", project.property("modrinthID") as String)
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
    processResources {
        dependsOn("copyCommonSources")
    }
    jar {
        manifest {
            attributes(
                "Main-Class" to "me.gamerduck.alwaysauth.AlwaysAuthMain"
            )
        }
    }
    build {
    }
}

//tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
//    archiveClassifier.set("")
//    mergeServiceFiles()
//}
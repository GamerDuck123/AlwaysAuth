dependencies {
    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)

    implementation(libs.h2)
    implementation(libs.mysql)
    implementation(libs.mariadb)
}

tasks.register<Copy>("copyCommonSources") {
    from("$rootDir/common/src/main/java") {
        exclude("me/gamerduck/${project.property("modid")}/mixin/**")
        exclude("me/gamerduck/${project.property("modid")}/reflection/**")
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
    }
    build {
        dependsOn("shadowJar")
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    relocate("org.h2", "me.gamerduck.alwaysauth.libs.h2")
    mergeServiceFiles()
}
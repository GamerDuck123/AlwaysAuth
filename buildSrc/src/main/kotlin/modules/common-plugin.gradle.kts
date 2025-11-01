plugins {
    id("root-plugin")

    id("idea")
    id("net.neoforged.moddev")
}

repositories {
    maven("https://libraries.minecraft.net")
}
tasks.withType<JavaCompile>().configureEach {
    enabled = false
}
tasks.named("jar").configure {
    enabled = false
}
tasks.named("build").configure {
    enabled = false
}
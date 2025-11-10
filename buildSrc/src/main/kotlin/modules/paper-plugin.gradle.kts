plugins {
    id("root-plugin")

    id("io.papermc.paperweight.userdev")
    id("modrinth-plugin")
    id("hangar-plugin")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://libraries.minecraft.net")
}

rootProject.name = "breed-renderer-cli"

pluginManagement {
    val kotlinVersion: String by settings

    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("multiplatform") version kotlinVersion
    }
}
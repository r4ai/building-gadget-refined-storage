pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.creeperhost.net")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.neoforged.net/releases")
        maven("https://thedarkcolour.github.io/KotlinForForge/")
        maven("https://maven.creeperhost.net")
        maven("https://www.cursemaven.com")
    }
}

rootProject.name = "building-gadget-refined-storage"

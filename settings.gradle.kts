pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "maz_modzzz"
include(":app")

// Optional: if you drop the BlackBox "bcore" module into ./bcore, it is picked up automatically.
if (file("bcore").exists()) {
    include(":bcore")
}

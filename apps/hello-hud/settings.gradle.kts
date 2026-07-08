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
        // Rokid CXR SDK artifacts (needed once you add the cxr-service-bridge dep).
        // Requires developer-portal access; hello-hud does not use it yet.
        maven { url = uri("https://maven.rokid.com/repository/maven-public/") }
    }
}

rootProject.name = "hello-hud"
include(":app")

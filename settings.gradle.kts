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
    }
}

rootProject.name = "NebulaBrowser"
include(
    ":app",
    ":core:domain",
    ":core:database",
    ":core:datastore",
    ":core:security",
    ":browser:core",
    ":engines:api",
    ":engines:chromium"
)

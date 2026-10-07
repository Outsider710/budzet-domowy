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

rootProject.name = "BudzetDomowy"
include(
    ":app",
    ":core:data",
    ":core:ui",
    ":feature:home",
    ":feature:transactions",
    ":feature:categories",
    ":feature:goals",
    ":feature:recurring",
    ":feature:report",
    ":feature:settings",
    ":feature:splash",
)

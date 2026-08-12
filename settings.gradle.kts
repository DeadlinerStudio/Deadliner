pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public") {
            content {
                includeGroupByRegex("org\\.jetbrains\\.kotlinx(\\..*)?")
                includeGroupByRegex("app\\.cash\\.sqldelight(\\..*)?")
                includeGroupByRegex("io\\.ktor(\\..*)?")
                includeGroupByRegex("com\\.squareup\\.okio(\\..*)?")
            }
        }
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-releases/") {
            content {
                includeGroupByRegex("org\\.jetbrains\\.kotlinx(\\..*)?")
                includeGroupByRegex("app\\.cash\\.sqldelight(\\..*)?")
                includeGroupByRegex("io\\.ktor(\\..*)?")
                includeGroupByRegex("com\\.squareup\\.okio(\\..*)?")
            }
        }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.nju.edu.cn/maven/") }
    }
}

rootProject.name = "Deadliner"
include(":app")

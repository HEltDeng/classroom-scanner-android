import org.gradle.api.initialization.resolve.RepositoriesMode

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
        // The official Google Maven CDN timed out on the bundled ML Kit AAR in
        // this environment; keep the official repository and use its mirror first.
        maven(url = "https://maven.aliyun.com/repository/google")
        google()
        mavenCentral()
    }
}

rootProject.name = "classroom-scanner-android"
include(":app")

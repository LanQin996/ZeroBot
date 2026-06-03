pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://nexus.jsdu.cn/releases")
        }
        mavenCentral()
    }
}

rootProject.name = "ZeroBot"

include("zerobot-plugin-api")
include("zerobot-core")
include("zerobot-app")
include("examples:echo-plugin")

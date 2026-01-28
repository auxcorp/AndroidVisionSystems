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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "IndustrialVisionSystems"

include(":app")
include(":core:common")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":core:network")
include(":feature:camera")
include(":feature:inspection")
include(":feature:agents")
include(":feature:analytics")
include(":feature:settings")
include(":vision:processing")
include(":vision:ml")
include(":ai:agents")
include(":ai:llm")

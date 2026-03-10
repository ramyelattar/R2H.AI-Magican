pluginManagement {
    includeBuild("build-logic")
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

rootProject.name = "Magican"

include(
    ":app",
    ":core:design",
    ":core:audio",
    ":core:haptics",
    ":core:sensors",
    ":ai:runtime",
    ":ai:orchestrator",
    ":features:tarot",
    ":features:palm",
    ":features:voiceaura",
    ":features:dreams",
    ":features:horoscope",
    ":features:compatibility",
    ":features:birthchart",
    ":features:library"
)

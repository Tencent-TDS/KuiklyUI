pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.buildFileName = "build.1.7.20.gradle.kts"

val buildFileName = "build.1.7.20.gradle.kts"
include(":core-annotations")
project(":core-annotations").buildFileName = buildFileName
include(":core-ksp")
project(":core-ksp").buildFileName = buildFileName

include(":core")
project(":core").buildFileName = buildFileName
include(":core-render-android")
project(":core-render-android").buildFileName = buildFileName

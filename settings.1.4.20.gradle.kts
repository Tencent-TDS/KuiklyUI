pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.buildFileName = "build.1.4.20.gradle.kts"

include(":core-annotations")
project(":core-annotations").buildFileName = "build.1.4.20.gradle"
include(":core-kapt")
include(":core")
project(":core").buildFileName = "build.1.4.20.gradle.kts"
include(":core-render-android")
project(":core-render-android").buildFileName = "build.1.4.20.gradle"
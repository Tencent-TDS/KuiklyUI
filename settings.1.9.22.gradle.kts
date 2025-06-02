pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

val buildFileName = "build.1.9.22.gradle.kts"
rootProject.buildFileName = buildFileName


include(":core-annotations")
project(":core-annotations").buildFileName = buildFileName

include(":core-ksp")
project(":core-ksp").buildFileName = buildFileName

include(":core")
project(":core").buildFileName = buildFileName
include(":core-render-android")
project(":core-render-android").buildFileName = buildFileName
include(":compose")
project(":compose").buildFileName = buildFileName
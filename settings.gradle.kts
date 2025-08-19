pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

include(":demo")
include(":androidApp")
include(":h5App")
include(":miniApp")

val buildFileName = "build.2.0.21.gradle.kts"


include(":core-annotations")
project(":core-annotations").buildFileName = buildFileName

include(":core-ksp")
project(":core-ksp").buildFileName = buildFileName

include(":core")
project(":core").buildFileName = buildFileName

include(":core-render-android")
project(":core-render-android").buildFileName = buildFileName

include(":core-render-web:base")
include(":core-render-web:h5")
include(":core-render-web:miniapp")


include(":compose")

rootProject.buildFileName = buildFileName
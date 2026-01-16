pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

val buildFileName = "build.2.2.20.gradle.kts"
rootProject.buildFileName = buildFileName


include(":core-annotations")
project(":core-annotations").buildFileName = buildFileName

include(":core-ksp")
project(":core-ksp").buildFileName = buildFileName

include(":core")
project(":core").buildFileName = buildFileName

include(":core-render-android")
project(":core-render-android").buildFileName = buildFileName

include(":core-render-web:base")
project(":core-render-web:base").buildFileName = buildFileName
include(":core-render-web:h5")
project(":core-render-web:h5").buildFileName = buildFileName
include(":core-render-web:miniapp")
project(":core-render-web:miniapp").buildFileName = buildFileName

include(":compose")
project(":compose").buildFileName = buildFileName

// include(":demo")
// include(":androidApp")

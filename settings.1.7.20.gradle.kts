pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
        }
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
        }
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

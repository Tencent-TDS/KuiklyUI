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

rootProject.buildFileName = "build.1.4.20.gradle.kts"

include(":core-annotations")
project(":core-annotations").buildFileName = "build.1.4.20.gradle"
include(":core-kapt")
include(":core")
project(":core").buildFileName = "build.1.4.20.gradle.kts"
include(":core-render-android")
project(":core-render-android").buildFileName = "build.1.4.20.gradle"
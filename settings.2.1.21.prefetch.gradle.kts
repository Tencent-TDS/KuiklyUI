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

rootProject.buildFileName = "build.2.1.21.prefetch.gradle.kts"

include(":core-annotations")
project(":core-annotations").buildFileName = "build.2.1.21.gradle.kts"

include(":core")
project(":core").buildFileName = "build.2.1.21.gradle.kts"

include(":core-render-android")
project(":core-render-android").buildFileName = "build.2.1.21.gradle.kts"

include(":compose")
project(":compose").buildFileName = "build.2.1.21.prefetch.gradle.kts"

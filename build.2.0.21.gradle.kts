plugins {
    kotlin("multiplatform") version "2.0.21" apply false
    kotlin("plugin.compose") version "2.1.21" apply false
    id("com.android.application") version "7.4.2" apply false
    id("com.android.library") version "7.4.2" apply false
    id("org.jetbrains.compose") version "1.7.3" apply false
    id("com.google.devtools.ksp") version "2.1.21-2.0.1" apply false
}

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        //maven("https://mirrors.tencent.com/repository/maven/tencent_public/")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
    }
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("${MavenConfig.GROUP}:core")).using(project(":core"))
            substitute(module("${MavenConfig.GROUP}:core-annotations")).using(project(":core-annotations"))
            substitute(module("${MavenConfig.GROUP}:compose")).using(project(":compose"))
        }
    }
}
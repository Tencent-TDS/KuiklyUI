plugins {
    kotlin("multiplatform") version "2.0.21" apply false
    id("com.android.application") version "7.4.2" apply false
    id("com.android.library") version "7.4.2" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        classpath(BuildPlugin.kotlin)
        classpath(BuildPlugin.android)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("${MavenConfig.GROUP}:core")).using(project(":core"))
            substitute(module("${MavenConfig.GROUP}:core-annotations")).using(project(":core-annotations"))
        }
    }
}


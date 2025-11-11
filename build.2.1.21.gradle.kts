plugins {
    kotlin("multiplatform") version "2.1.21" apply false
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
    dependencies {
        classpath(BuildPlugin.kotlin)
        classpath(BuildPlugin.android)
        classpath(BuildPlugin.kuikly)
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
            substitute(module("${MavenConfig.GROUP}:compose")).using(project(":compose"))
        }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.WARNING)
    }
}


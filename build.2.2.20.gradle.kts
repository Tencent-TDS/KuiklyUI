plugins {
    kotlin("multiplatform") version "2.2.20" apply false
    kotlin("plugin.compose") version "2.2.20" apply false
    id("com.android.application") version "7.4.2" apply false
    id("com.android.library") version "7.4.2" apply false
    id("org.jetbrains.compose") version "1.7.3" apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.4" apply false
}

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()

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
    }
}


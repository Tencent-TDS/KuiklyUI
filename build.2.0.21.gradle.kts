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
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/kuikly-open")
        }
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
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/kuikly-open")
        }
    }
}


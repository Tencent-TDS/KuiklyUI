plugins {
    id("com.google.devtools.ksp") version(Version.getKSPVersion())
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
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
}

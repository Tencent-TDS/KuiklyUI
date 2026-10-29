// 根构建脚本：Kotlin 2.3.10 分支
// 与 build.2.1.21.gradle.kts 的差异：
//   1) Kotlin / Compose 编译器插件版本 2.1.21 -> 2.3.10
//   2) KSP 插件 2.1.21-2.0.1 -> 2.3.4（KSP2 起版本号与 Kotlin 解耦）
//   3) AGP 版本由 KUIKLY_AGP_VERSION 控制，本分支需传 8.6.0（Kotlin 2.3 要求 8.2.2~8.13.0）
//   4) 移除 android.disableAutomaticComponentCreation，改由 publish/compatible/2.3.10.yaml 在发布时替换 gradle.properties
plugins {
    kotlin("multiplatform") version "2.3.10" apply false
    kotlin("plugin.compose") version "2.3.10" apply false
    id("com.android.application") version "8.6.0" apply false
    id("com.android.library") version "8.6.0" apply false
    id("org.jetbrains.compose") version "1.7.3" apply false
    id("com.google.devtools.ksp") version "2.3.4" apply false
}

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
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
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
        }
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


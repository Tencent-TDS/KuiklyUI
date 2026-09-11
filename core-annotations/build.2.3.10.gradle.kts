// core-annotations/build.2.1.21.gradle.kts 的 Kotlin 2.3 兼容版本
// 相对原脚本只改：kotlinOptions -> compilerOptions；compileSdk 抬到 35（AGP 8.x）

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("maven-publish")
    signing
}

group = MavenConfig.GROUP
version = Version.getCoreVersion()

publishing {
    repositories {
        val username = MavenConfig.getUsername(project)
        val password = MavenConfig.getPassword(project)
        if (username.isNotEmpty() && password.isNotEmpty()) {
            maven {
                credentials {
                    setUsername(username)
                    setPassword(password)
                }
                url = uri(MavenConfig.getRepoUrl(version as String))
            }
        } else {
            mavenLocal()
        }
    }

    afterEvaluate {
        publications.withType<MavenPublication>().configureEach {
            pom.configureMavenCentralMetadata()
            signPublicationIfKeyPresent(project)
        }
        publications.named<MavenPublication>("jvm") {
            artifact(emptyJavadocJar)
        }
    }
}

kotlin {
    jvm {
        compilerOptions {
            moduleName.set("${project.group}.${project.name}")
        }
    }

    androidTarget {
        compilerOptions {
            moduleName.set("${project.group}.${project.name}")
        }
    }

    js(IR) {
        browser()
    }

    iosArm64()
    iosX64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    sourceSets {
        val commonMain by getting
    }
}

android {
    compileSdk = 35
    namespace = "com.tencent.kuikly.core.annotations"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 32
    }
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

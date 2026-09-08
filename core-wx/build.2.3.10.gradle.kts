// core-wx/build.2.1.21.gradle.kts 的 Kotlin 2.3 兼容版本
// 改动同 core：
//   1) kotlinOptions -> compilerOptions
//   2) js(IR) moduleName -> outputModuleName.set
//   3) webpackTask outputFileName -> mainOutputFileName.set
//   4) compileSdk 30 -> 35（AGP 8.x 要求 34+）

plugins {
    kotlin("multiplatform")
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

        publications.withType<MavenPublication>().configureEach {
            pom.configureMavenCentralMetadata()
            signPublicationIfKeyPresent(project)
        }
    }
}

kotlin {

    androidTarget {
        compilerOptions {
            moduleName.set("${project.group}.${project.name}")
        }
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    iosSimulatorArm64()
    iosX64()
    iosArm64()

    macosX64()
    macosArm64()

    js(IR) {
        outputModuleName.set("KuiklyCore-core-wx")
        browser {
            webpackTask {
                mainOutputFileName.set("KuiklyCore-core-wx.js")
            }
            commonWebpackConfig {
                output?.library = null
            }
        }
        binaries.executable()
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }

        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
            }
        }
    }
}

android {
    compileSdk = 35
    namespace = "com.tencent.kuikly.core.wx"
    defaultConfig {
        minSdk = 21
        targetSdk = 35
    }
}

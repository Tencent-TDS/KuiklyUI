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

// core-wx: Kuikly WeChat MiniProgram bindings (views + modules).
//
// Target matrix aligned with :core — publishes android/iOS/macOS/js/ohosArm64.
// The wrappers themselves are pure Kotlin (no js() macros / no wx.* access —
// all `wx.*` calls live in core-render-web), so it is safe to compile on every
// target. On non-MiniProgram runtime, WX views degrade to a plain view and
// `registerWXModules()` is a no-op.
kotlin {
    androidTarget {
        compilerOptions {
            moduleName.set("${project.group}.${project.name}")
        }
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    ohosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()

    macosX64()
    macosArm64()

    js {
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
    }
}

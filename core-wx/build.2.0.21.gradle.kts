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
// Target matrix aligned with :core — publishes android/iOS/macOS/js(IR).
// Rationale: business apps may share cross-platform pages in commonMain and
// conditionally use WXButton / registerWXModules() only on MiniProgram runtime.
// The wrappers themselves are pure Kotlin (no js() macros / no wx.* access —
// all `wx.*` calls live in core-render-web), so it is safe to compile on every
// target. On non-MiniProgram runtime, WX views degrade to a plain view and
// `registerWXModules()` is a no-op.
//
// Apps that don't need WX capabilities simply do NOT depend on :core-wx
// — core itself has zero references to :core-wx, so no code pulled in.
kotlin {

    androidTarget {
        compilations.all {
            kotlinOptions {
                moduleName = "${project.group}.${project.name}"
            }
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
        moduleName = "KuiklyCore-core-wx"
        browser {
            webpackTask {
                outputFileName = "${moduleName}.js"
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
    compileSdk = 30
    namespace = "com.tencent.kuikly.core.wx"
    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }
}

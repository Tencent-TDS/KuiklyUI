plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("maven-publish")
    signing
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs += "-Xjvm-default=all"
            }
        }
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js(IR) {
        browser()
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                // 设置部分优化标志
                freeCompilerArgs += listOf(
                    "-Xinline-classes",
                    "-opt-in=kotlin.ExperimentalStdlibApi",
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in=kotlin.experimental.ExperimentalNativeApi",
                    "-opt-in=kotlin.contracts.ExperimentalContracts",
//                    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:nonSkippingGroupOptimization=true",
                    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
                    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true",
                    "-Xcontext-receivers"
                )
            }
        }
    }

    ohosArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        commonMain.dependencies {
            implementation(project(":core"))
            api("com.tencent.kuikly-open.compose.runtime:runtime:1.7.3-kuikly1")
            api("com.tencent.kuikly-open.compose.runtime:runtime-saveable:1.7.3-kuikly1")
            api("com.tencent.kuikly-open.compose.annotation-internal:annotation:1.7.3-kuikly1")
            api("com.tencent.kuikly-open.compose.collection-internal:collection:1.7.3-kuikly1")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-KBA-001")
            api("org.jetbrains.kotlinx:atomicfu:0.23.2-KBA-001")
        }

        // Android 特有源集中添加 ProfileInstaller 依赖
        val androidMain by getting {
            dependencies {
                compileOnly(project(":core-render-android"))
                implementation("androidx.profileinstaller:profileinstaller:1.3.1")
                // 保留现有依赖...
            }
        }
    }
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

android {
    namespace = "com.tencent.kuikly.compose"
    compileSdk = 30
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
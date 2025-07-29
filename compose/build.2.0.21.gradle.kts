plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
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
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "compose"
            isStatic = true
        }
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
                    "-Xcontext-receivers"
                )
            }
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        commonMain.dependencies {
            //put your multiplatform dependencies here
            api(project(":core"))
            api(compose.runtime)
            api(compose.runtimeSaveable)
//            api(compose.foundation)
//            api(compose.animation)
//            api(compose.ui)

            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            api("androidx.annotation:annotation:1.9.1")
            api("org.jetbrains.kotlinx:atomicfu:0.25.0")
            api("org.jetbrains.compose.collection-internal:collection:1.7.3")
            implementation("com.tencent.kuiklyx-open:coroutines:1.1.0-2.0.21")
        }

        commonTest.dependencies {
//            implementation(libs.kotlin.test)
        }

        // Android 特有源集中添加 ProfileInstaller 依赖
        val androidMain by getting {
            dependencies {
                implementation("androidx.profileinstaller:profileinstaller:1.3.1")
                // 保留现有依赖...
            }
        }
    }
}

// 配置Maven发布
publishing {
//    publications.withType<MavenPublication> {
//        artifactId = "compose"
//    }

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

group = MavenConfig.GROUP
version = Version.getCoreVersion()

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
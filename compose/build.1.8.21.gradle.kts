import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
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
//                    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
                    "-Xcontext-receivers"
                )
            }
        }
    }

    android {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    ios()
    iosSimulatorArm64()
    iosX64()

    // sourceSets
    val commonMain by sourceSets.getting {
        dependencies {
            //put your multiplatform dependencies here
            api(project(":core"))
            api(compose.runtime)
            api(compose.runtimeSaveable)
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
            api("org.jetbrains.kotlinx:atomicfu:0.21.0")
            api("org.jetbrains.compose.collection-internal:collection:1.6.0")
            implementation("com.tencent.kuiklyx-open:coroutines:1.0.1")
        }
    }

//    val iosMain by sourceSets.getting {
//        dependsOn(commonMain)
//    }
//    val iosTest by sourceSets.getting {
//        dependsOn(commonTest)
//    }
//
//    targets.withType<KotlinNativeTarget> {
//        val mainSourceSets = this.compilations.getByName("main").defaultSourceSet
//        val testSourceSets = this.compilations.getByName("test").defaultSourceSet
//        when {
//            konanTarget.family.isAppleFamily -> {
//                mainSourceSets.dependsOn(iosMain)
//                testSourceSets.dependsOn(iosTest)
//            }
//
//        }
//    }
//
//    cocoapods {
//        summary = "Some description for the Shared Module"
//        homepage = "Link to the Shared Module homepage"
//        version = "1.0"
//        ios.deploymentTarget = "16.0"
//        framework {
//            baseName = "composeDSL"
//            isStatic = true
//        }
//    }
}

android {
    compileSdk = 30
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }
}

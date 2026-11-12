import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            moduleName.set("${project.group}.${project.name}")
        }
    }

    ohosArm64 {
        val main by compilations.getting
        val ohos by main.cinterops.creating {
            definitionFile.set(file("src/ohosArm64Main/ohosInterop/cinterop/ohos.def"))
            includeDirs(file("src/ohosArm64Main/ohosInterop/include"))
            includeDirs(file("../core-render-ohos/src/main/cpp/libohos_render"))
            compilerOpts("-Wno-error=int-conversion", "-Wno-int-conversion")
            compilerOpts("-Wno-error=deprecated-declarations", "-Wno-deprecated-declarations")

            // Add HarmonyOS SDK include paths (Windows only)
            if (System.getProperty("os.name")?.contains("windows", ignoreCase = true) == true) {
                val ohosSdkHome = System.getenv("OHOS_SDK_HOME")
                if (!ohosSdkHome.isNullOrEmpty()) {
                    includeDirs(
                        "$ohosSdkHome/native/sysroot/usr/include",
                        "$ohosSdkHome/native/sysroot/usr/include/aarch64-linux-ohos"
                    )
                }
            }
        }
    }

    iosSimulatorArm64()
    iosX64()
    iosArm64()

    js {
        outputModuleName.set("KuiklyCore-core")
        browser {
            webpackTask {
                mainOutputFileName.set("KuiklyCore-core.js")
            }

            commonWebpackConfig {
                output?.library = null
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting
        val appleMain by creating {
            dependsOn(commonMain)
        }
        val ohosArm64Main by getting {
            dependsOn(commonMain)
        }
    }

    targets.withType<KotlinNativeTarget> {
        val appleMain = this@kotlin.sourceSets.getByName("appleMain")
        if (konanTarget.family.isAppleFamily) {
            val main by compilations.getting
            main.defaultSourceSet.dependsOn(appleMain)
            val kuikly by main.cinterops.creating {
                definitionFile.set(project.file("src/appleMain/iosInterop/cinterop/ios.def"))
            }
        }
    }
}

android {
    compileSdk = 35
    namespace = "com.tencent.kuikly.core"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

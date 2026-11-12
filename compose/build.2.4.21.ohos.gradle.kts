import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("maven-publish")
    signing
}

group = MavenConfig.GROUP
version = Version.getCoreVersion()

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
            "-opt-in=kotlin.experimental.ExperimentalNativeApi",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true",
        )
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            moduleName.set("${project.group}.${project.name}")
            freeCompilerArgs.add("-Xjvm-default=all")
        }
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js {
        browser()
    }

    ohosArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        commonMain.dependencies {
            implementation(project(":core"))
            api("com.tencent.kuikly-open.compose.runtime:runtime:1.7.3-kuikly3")
            api("com.tencent.kuikly-open.compose.runtime:runtime-saveable:1.7.3-kuikly3")
            api("com.tencent.kuikly-open.compose.annotation-internal:annotation:1.7.3-kuikly3")
            api("com.tencent.kuikly-open.compose.collection-internal:collection:1.7.3-kuikly3")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-KBA-001")
            api("org.jetbrains.kotlinx:atomicfu:0.23.2-KBA-001")
        }

        // Normal 2.1.21 artifacts stay on Compose runtime 1.7.3 and disable prefetch.
        val runtimeLegacyMain by creating {
            dependsOn(commonMain.get())
        }

        // Keep the default Native/Apple hierarchy in the runtimeLegacy branch so
        // ios targets compile the actuals from nativeMain and appleMain.
        val nativeMain by creating {
            dependsOn(runtimeLegacyMain)
        }
        val appleMain by creating {
            dependsOn(nativeMain)
        }

        val androidMain by getting {
            dependsOn(runtimeLegacyMain)
            dependencies {
                compileOnly(project(":core-render-android"))
                implementation("androidx.profileinstaller:profileinstaller:1.3.1")
            }
        }

        val jsMain by getting {
            dependsOn(runtimeLegacyMain)
        }

        // ohos is a Native target: wire it through nativeMain so it inherits both
        // the common native actuals and the runtimeLegacy actuals.
        val ohosArm64Main by getting {
            dependsOn(nativeMain)
        }

        listOf(
            "iosX64Main", "iosArm64Main", "iosSimulatorArm64Main",
            "macosX64Main", "macosArm64Main",
        ).forEach { ssName ->
            findByName(ssName)?.dependsOn(appleMain)
        }
    }
}

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
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

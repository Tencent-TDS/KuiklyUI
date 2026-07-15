plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("maven-publish")
    signing
}

group = MavenConfig.GROUP
version = Version.getCoreVersion()

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
                freeCompilerArgs += "-Xjvm-default=all"
                moduleName = "${project.group}.${project.name}"
            }
        }
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    js(IR) {
        browser()
    }

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
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            api("androidx.annotation:annotation:1.9.1")
            api("org.jetbrains.kotlinx:atomicfu:0.25.0")
            api("org.jetbrains.compose.collection-internal:collection:1.9.3")
            implementation(project(":core-annotations"))
        }

        commonTest.dependencies {
//            implementation(libs.kotlin.test)
        }

        // runtime19Main: real PausableComposition + CompositionObserver wiring (Kotlin 2.1.21+)
        val runtime19Main by creating {
            dependsOn(commonMain.get())
        }

        // Wire Android and JS targets explicitly
        val androidMain by getting {
            dependsOn(runtime19Main)
            dependencies {
                compileOnly(project(":core-render-android"))
                implementation("androidx.profileinstaller:profileinstaller:1.3.1")
                // 保留现有依赖...
            }
        }

        val jsMain by getting {
            dependsOn(runtime19Main)
        }

        // Wire each native/Apple target source set individually.
        // nativeMain / appleMain are intermediate sets that may not be addressable directly
        // via "by getting" at this point; we use findByName for safety.
        listOf(
            "iosX64Main", "iosArm64Main", "iosSimulatorArm64Main",
            "macosX64Main", "macosArm64Main",
        ).forEach { ssName ->
            findByName(ssName)?.dependsOn(runtime19Main)
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

android {
    namespace = "com.tencent.kuikly.compose"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
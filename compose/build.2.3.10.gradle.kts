import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xjvm-default=all")
            moduleName.set("${project.group}.${project.name}")
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

    // Kotlin 2.3: compilations.all 内 compilerOptions 存在隐式 receiver 歧义，
    // 改为 kotlin 块级 compilerOptions（对所有 target/compilation 生效，语义等价）
    compilerOptions {
        // 设置部分优化标志
        freeCompilerArgs.addAll(
            listOf(
                "-Xinline-classes",
                "-opt-in=kotlin.ExperimentalStdlibApi",
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in=kotlin.experimental.ExperimentalNativeApi",
                "-opt-in=kotlin.contracts.ExperimentalContracts",
//                    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:nonSkippingGroupOptimization=true",
                "-P", "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
                "-Xcontext-receivers"
            )
        )
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
            api("org.jetbrains.compose.collection-internal:collection:1.7.3")
            implementation(project(":core-annotations"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        val runtimeLegacyTest by creating {
            dependsOn(commonTest.get())
        }
        val androidUnitTest by getting {
            dependsOn(runtimeLegacyTest)
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
                // 保留现有依赖...
            }
        }

        val jsMain by getting {
            dependsOn(runtimeLegacyMain)
        }

        // Wire Apple targets through appleMain so nativeMain actuals are included.
        listOf(
            "iosX64Main", "iosArm64Main", "iosSimulatorArm64Main",
            "macosX64Main", "macosArm64Main",
        ).forEach { ssName ->
            findByName(ssName)?.dependsOn(appleMain)
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
    id("maven-publish")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
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
                    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
                    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true",
                    "-Xcontext-receivers"
                )

                // 安装包优化
                val CLANG_OPT_FLAGS = "-Os -mllvm -enable-machine-outliner=always -ffunction-sections"
                val CLANG_FLAGS = "clangOptFlags.ios_arm64=$CLANG_OPT_FLAGS;clangDebugFlags.ios_arm64=$CLANG_OPT_FLAGS;clangOptFlags.ohos_arm64=$CLANG_OPT_FLAGS;clangDebugFlags.ohos_arm64=$CLANG_OPT_FLAGS"
                freeCompilerArgs += "-Xoverride-konan-properties=$CLANG_FLAGS"
            }
        }
    }

    ohosArm64 {
        binaries.sharedLib("shared"){
            freeCompilerArgs += "-Xadd-light-debug=enable"
            // 安装包优化
            linkerOpts += "--pack-dyn-relocs=relr"
            linkerOpts += "--gc-sections"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(project(":core-annotations"))
                implementation(project(":compose"))
                // Chat Demo 相关依赖
                implementation("com.tencent.kuiklybase:markdown:0.3.0-ohos")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

dependencies {
    add("kspOhosArm64", project(":core-ksp"))
}


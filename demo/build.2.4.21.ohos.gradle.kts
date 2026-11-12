plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
    id("maven-publish")
    id("com.tencent.kuiklybase.knoi.plugin")
}

knoi {
    tsGenDir = projectDir.absolutePath + "/../ohosApp/entry/src/main/ets/ts-api/"
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
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

    ohosArm64 {
        compilerOptions {
            freeCompilerArgs.add("-Xadd-light-debug=enable")
        }
        binaries.sharedLib("shared") {
            linkerOpts += listOf("--build-id=sha1")
            if (buildType == org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.RELEASE) {
                // 保留 Konan 默认 -O3（不用 -Os），仅叠加 sections；linker 侧加 relr/gc/hash-style
                val clangOptFlags = "-O3 -ffunction-sections -fdata-sections"
                val clangFlags =
                    "clangOptFlags.ohos_arm64=$clangOptFlags;clangDebugFlags.ohos_arm64=$clangOptFlags"
                freeCompilerArgs += listOf("-Xoverride-konan-properties=$clangFlags")
                linkerOpts += listOf("--pack-dyn-relocs=relr", "--gc-sections", "--hash-style=gnu")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(project(":core-annotations"))
                implementation(project(":compose"))
                implementation(project(":core-wx"))
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

ksp {
    arg("catchException", "false")
}

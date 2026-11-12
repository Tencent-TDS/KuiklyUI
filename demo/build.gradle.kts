import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.compose")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
    id("com.tencent.kuikly-open.kuikly")
}

group = Publishing.kuiklyGroup
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

/**
 * LLVM PGO / Machine Outliner 开关，由环境变量 LLVM_PGO_TYPE 控制：
 * - LLVMPGO: 插装编译，运行时生成 .profraw
 * - MachineOutliner: 使用 demo/llvmProfile/real_ios.profdata 做 PGO + outlining
 * - 未设置 / 其他: 普通编译
 *
 * 详见 docs/DevGuide/machine-outliner-pgo-guide.md
 */
val llvmPgoType = System.getenv("LLVM_PGO_TYPE").orEmpty()
val enableLlvmPgoInstrument = llvmPgoType == "LLVMPGO"
val enableMachineOutliner = llvmPgoType == "MachineOutliner"
val iosProfdataFile = project.file("llvmProfile/real_ios.profdata")

fun buildAppleClangOverrideProperties(): String? {
    val optFlags = when {
        enableLlvmPgoInstrument -> {
            // 插装必须跑 LLVM passes；不能沿用 Debug 默认的 -disable-llvm-passes。
            // 关闭 value profiling（LLVM16 正确开关是 -disable-vp；
            // -enable-value-profiling=false 是空操作会被忽略）。否则 __llvm_prf_data
            // 里会残留 NumValueSites>0，而运行时 dump 不写 VP 段，导致 profraw 自相矛盾、
            // llvm-profdata merge 报 "truncated profile data"。
            "-Os -ffunction-sections " +
                "-fprofile-instrument=llvm " +
                "-fprofile-instrument-path=/fake/default_ios.profraw " +
                "-mllvm -disable-vp"
        }
        enableMachineOutliner -> {
            // 开源 KN/LLVM16 仅稳定支持 enable-machine-outliner；
            // 参考文档中的 *-threshold 等为内部 LLVM 扩展，此处不默认启用。
            val outliner =
                "-Os -ffunction-sections -mllvm -enable-machine-outliner=always"
            if (iosProfdataFile.exists()) {
                logger.lifecycle("[LLVM_PGO] 使用 profdata: ${iosProfdataFile.absolutePath}")
                "$outliner -fprofile-instrument-use-path=${iosProfdataFile.absolutePath}"
            } else {
                logger.warn(
                    "[LLVM_PGO] MachineOutliner 已开启，但未找到 ${iosProfdataFile.path}；" +
                        "仍启用 outlining（无 PGO use）。完整 PGO 请见 docs/DevGuide/machine-outliner-pgo-guide.md"
                )
                outliner
            }
        }
        else -> return null
    }
    // Debug 构建走 clangDebugFlags，Release 走 clangOptFlags。
    // PGO 插装时还需覆盖 clangFlags，去掉 -disable-llvm-passes，否则插装 pass 不会执行。
    val appleSuffixes = listOf("ios_arm64", "ios_x64", "ios_simulator_arm64")
    val props = mutableListOf<String>()
    for (suffix in appleSuffixes) {
        props += "clangOptFlags.$suffix=$optFlags"
        props += "clangDebugFlags.$suffix=$optFlags"
        if (enableLlvmPgoInstrument || enableMachineOutliner) {
            props += "clangFlags.$suffix=-cc1 -emit-obj -x ir"
        }
    }
    return props.joinToString(";")
}

val appleClangOverrideProperties = buildAppleClangOverrideProperties()
if (appleClangOverrideProperties != null) {
    logger.lifecycle("[LLVM_PGO] LLVM_PGO_TYPE=$llvmPgoType，已启用 Kotlin/Native clang 优化参数覆盖")
}

kotlin {

    // target
    androidTarget() {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    js(IR) {
        moduleName = Output.name
        browser {
            webpackTask {
                outputFileName = "${moduleName}.js" // 最后输出的名字
            }

            commonWebpackConfig {
                output?.library = null // 不导出全局对象，只导出必要的入口函数
                devtool = "source-map" // 不使用默认的 eval 执行方式构建出 source-map，而是构建单独的 sourceMap 文件
            }
        }
        binaries.executable() //将kotlin.js与kotlin代码打包成一份可直接运行的js文件
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    // sourceSet
    val commonMain by sourceSets.getting {
        dependencies {
            implementation(project(":core"))
            implementation(project(":compose"))
            implementation(project(":core-annotations"))
//            compileOnly(project(":core-annotations"))
            // :core-wx is OPTIONAL. Depend on it here only because the demo
            // showcases WeChat MiniProgram components / APIs. Apps that do not
            // need WX capabilities simply omit this dependency and pay zero
            // cost (no classes leaked into android/iOS artifacts).
            // Declared in commonMain so cross-platform pages can conditionally
            // use `WXButton {}` / `registerWXModules()` behind an
            // `is_miniprogram` runtime check.
            implementation(project(":core-wx"))
            // Chat Demo 相关依赖
            implementation("com.tencent.kuiklybase:markdown:0.4.0")
            implementation("io.ktor:ktor-client-core:2.3.10")
        }
    }

    val jsMain by sourceSets.getting {
        dependsOn(commonMain)
//        kotlin.srcDir(
//            "build/generated/ksp/js/jsMain/kotlin"
//        )
    }

    val androidMain by sourceSets.getting {
        dependsOn(commonMain)
        dependencies {
            implementation("io.ktor:ktor-client-okhttp:2.3.10")
        }
//        kotlin.srcDirs(
//            "build/generated/ksp/android/androidDebug/kotlin",
//            "build/generated/ksp/android/androidRelease/kotlin",
//        )
    }

    sourceSets.iosMain {
        dependsOn(commonMain)
        dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.10")
        }
    }

    sourceSets.appleMain {
        dependsOn(commonMain)
        dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.10")
        }
    }

    targets.withType<KotlinNativeTarget> {
        val mainSourceSets = this.compilations.getByName("main").defaultSourceSet
        when {

            konanTarget.family.isAppleFamily -> {
                mainSourceSets.dependsOn(sourceSets.getByName("appleMain"))
            }

            konanTarget.family == Family.ANDROID -> {
                binaries {
                    val outputName = "nativevue"
                    sharedLib(outputName, listOf(RELEASE)) {
                        linkerOpts += linkerOpts + getLinkerArgs()
                        freeCompilerArgs = freeCompilerArgs + getCommonCompilerArgs()
                    }
                    sharedLib(outputName, listOf(DEBUG)) {
                        freeCompilerArgs = freeCompilerArgs + getCommonCompilerArgs()
                    }
                }
            }
        }
    }

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        osx.deploymentTarget = "10.13"
//        podfile = project.file("../iosApp/Podfile")
        framework {
            isStatic = true
            baseName = "shared"
            if (appleClangOverrideProperties != null) {
                freeCompilerArgs += "-Xoverride-konan-properties=$appleClangOverrideProperties"
            }
        }
        license = "MIT"
        extraSpecAttributes["resources"] = "['src/commonMain/assets/**']"
    }
}

// cocoapods framework 二进制创建之后，再给所有 Apple Native binary 挂上 PGO 参数（含非 pod 产物）。
if (appleClangOverrideProperties != null) {
    kotlin.targets.withType<KotlinNativeTarget>().configureEach {
        if (!konanTarget.family.isAppleFamily) return@configureEach
        compilations.configureEach {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xoverride-konan-properties=$appleClangOverrideProperties")
            }
        }
        binaries.configureEach {
            if ("-Xoverride-konan-properties=$appleClangOverrideProperties" !in freeCompilerArgs) {
                freeCompilerArgs += "-Xoverride-konan-properties=$appleClangOverrideProperties"
            }
        }
    }
}

fun getPageNameList(): String {
    return project.properties["pageNameList"] as? String ?: ""
}

ksp {
    arg("pageName", getPageName())
    arg("pageNameList", getPageNameList())
    arg(Output.KEY_PACK_LOCAL_JS_BUNDLE, packLocalJsBundle())
}

dependencies {
    compileOnly(project(":core-ksp")) {
        add("kspIosArm64", this)
        add("kspIosX64", this)
        add("kspIosSimulatorArm64", this)
        add("kspMacosArm64", this)
        add("kspMacosX64", this)
        add("kspAndroid", this)
        add("kspJs", this)
    }
}

android {
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    namespace = "com.tencent.kuikly.demo"
    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }

//    buildTypes {
//        release {
//            ndk {
//                abiFilters.add("arm64-v8a")
//            }
//        }
//    }

    sourceSets {
        named("main") {
            jniLibs.srcDirs("src/androidMain/libs/")
            assets.srcDirs("src/commonMain/assets")
        }
    }

}

fun getPageName(): String {
    return project.properties["pageName"] as? String ?: ""
}

fun packLocalJsBundle(): String {
    return (project.properties[Output.KEY_PACK_LOCAL_JS_BUNDLE] as? String) ?: ""
}

fun getCommonCompilerArgs(): List<String> {
    return listOf(
        "-Xallocator=std"
    )
}

fun getLinkerArgs(): List<String> {
    return listOf(
        "-Wl,--gc-sections,-s"
    )
}

// Compose 编译器稳定性报告（按需开启，用于验证类的稳定性推断，会增加编译耗时）
// composeCompiler {
//     reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
//     metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
// }

// Kuikly 插件配置
kuikly {
    // JS 产物配置
    js {
        // 构建产物名，与 KMM 插件 webpackTask#outputFileName 一致
        outputName("nativevue2")
        // 可选：分包构建时的页面列表，如果为空则构建全部页面
        // addSplitPage("route","home")
    }
}

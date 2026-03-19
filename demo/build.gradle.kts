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
            // Chat Demo 相关依赖
            implementation("com.tencent.kuiklybase:markdown:0.4.0")
            implementation("io.ktor:ktor-client-core:2.3.10")
        }
    }

    val jsMain by sourceSets.getting {
        dependsOn(commonMain)
        dependencies {
            implementation(project(":core-render-web:miniapp"))
        }
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
        }
        license = "MIT"
        extraSpecAttributes["resources"] = "['src/commonMain/assets/**']"
    }
}

ksp {
    arg("pageName", getPageName())
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

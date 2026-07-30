plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("com.google.devtools.ksp")
}

group = "com.kuikly.table"
version = "1.0.0"

val KEY_PAGE_NAME = "pageName"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // 产出 shared.framework，供 iOS 宿主通过 CocoaPods（pod 'shared'）接入
    cocoapods {
        summary = "Kuikly Table Component"
        homepage = "https://github.com/Tencent-TDS/KuiklyUI"
        version = "1.0.0"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
            isStatic = true
            freeCompilerArgs = freeCompilerArgs + listOf("-Xallocator=std")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.tencent.kuikly-open:core:2.15.0-2.0.21")
                implementation("com.tencent.kuikly-open:core-annotations:2.15.0-2.0.21")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("com.tencent.kuikly-open:core-render-android:2.15.0-2.0.21")
                // 基础 DSL（View/Text/ScrollerView）与基类（ComposeView/Attr/Pager）的 Android actual，
                // 由 core-android 提供；core-render-android 仅含渲染引擎，不含 DSL，必须显式声明。
                api("com.tencent.kuikly-open:core-android:2.15.0-2.0.21")
            }
        }
    }
}

// @Page 注解处理（KSP）。
// moduleId 用于生成类名命名空间；单模块组件工程 isMainModule=true、enableMultiModule=false。
// pageName 为空时构建全部 @Page 页面（CI 也可用 -PpageName=xxx 仅构建指定页）。
ksp {
    arg(KEY_PAGE_NAME, getPageName())
    arg("moduleId", "shared")
    arg("isMainModule", "true")
    arg("enableMultiModule", "false")
}

dependencies {
    compileOnly("com.tencent.kuikly-open:core-ksp:2.15.0-2.0.21") {
        add("kspAndroid", this)
        add("kspIosArm64", this)
        add("kspIosX64", this)
        add("kspIosSimulatorArm64", this)
    }
}

android {
    namespace = "com.kuikly.table"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

fun getPageName(): String {
    return (project.properties[KEY_PAGE_NAME] as? String) ?: ""
}

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.compose")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
}


group = Publishing.kuiklyGroup
version = "1.0.0"

repositories {
    mavenLocal()
}

kotlin {

    // target
    androidTarget() {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

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
            compileOnly(project(":core-annotations"))
            // Chat Demo 相关依赖
            // implementation("com.tencent.kuiklybase:markdown:0.1.0")
            implementation("io.ktor:ktor-client-core:2.3.10")
        }
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

    targets.withType<KotlinNativeTarget> {
        val mainSourceSets = this.compilations.getByName("main").defaultSourceSet
        when {

            konanTarget.family.isAppleFamily -> {
                mainSourceSets.dependsOn(sourceSets.getByName("iosMain"))
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
}

dependencies {
    compileOnly(project(":core-ksp")) {
        add("kspIosArm64", this)
        add("kspIosX64", this)
        add("kspIosSimulatorArm64", this)
        add("kspAndroid", this)
    }
}

android {
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
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
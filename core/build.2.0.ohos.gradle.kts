import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
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
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    ohosArm64 {
        val main by compilations.getting
        val ohos by main.cinterops.creating {
            defFile = file("src/ohosArm64Main/ohosInterop/cinterop/ohos.def")
            includeDirs(file("src/ohosArm64Main/ohosInterop/include"))
        }
    }

    iosSimulatorArm64()
    iosX64()
    iosArm64()

    js(IR) {
        moduleName = "KuiklyCore-core"
        browser {
            webpackTask {
                outputFileName = "${moduleName}.js" // 最后输出的名字
            }

            commonWebpackConfig {
                output?.library = null // 不导出全局对象，只导出必要的入口函数
            }
        }
        binaries.executable() //将kotlin.js与kotlin代码打包成一份可直接运行的js文件
    }

    // sourceSets
    val commonMain by sourceSets.getting

    val ohosArm64Main by sourceSets.getting {
        dependsOn(commonMain)
    }

    val iosMain by sourceSets.creating {
        dependsOn(commonMain)
    }

    targets.withType<KotlinNativeTarget> {
        when {
            konanTarget.family.isAppleFamily -> {
                val main by compilations.getting
                main.defaultSourceSet.dependsOn(iosMain)
                val kuikly by main.cinterops.creating {
                    defFile(project.file("src/iosMain/iosInterop/cinterop/ios.def"))
                }
            }
        }
    }

//    cocoapods {
//        summary = "Some description for the Shared Module"
//        homepage = "Link to the Shared Module homepage"
//        ios.deploymentTarget = "14.1"
//        if (!buildForAndroidCompat) {
//            framework {
//                isStatic = true
//                baseName = "kuiklyCore"
//            }
//        }
//    }
}

android {
    compileSdk = 30
    namespace = "com.tencent.kuikly.core"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

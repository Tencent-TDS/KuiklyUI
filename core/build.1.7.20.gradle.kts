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

    android {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    ios()
    iosSimulatorArm64()
    iosX64()

    // sourceSets
    sourceSets {
        val commonMain by getting
        val appleMain by sourceSets.creating {
            dependsOn(commonMain)
        }
    }

    targets.withType<KotlinNativeTarget> {
        val appleMain by sourceSets.getting
        when {
            konanTarget.family.isAppleFamily -> {
                val main by compilations.getting
                main.defaultSourceSet.dependsOn(appleMain)
                val kuikly by main.cinterops.creating {
                    defFile(project.file("src/appleMain/iosInterop/cinterop/ios.def"))
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
}
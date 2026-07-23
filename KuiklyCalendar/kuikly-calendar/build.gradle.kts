plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `maven-publish`
}

group = Publishing.kuiklyGroup
version = "1.0.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }

        val commonMain by getting {
            dependencies {
                api("com.tencent.kuikly-open:core:${Version.getKuiklyVersion()}")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
        }

        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace = "com.tencent.kuikly.calendar"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }
}

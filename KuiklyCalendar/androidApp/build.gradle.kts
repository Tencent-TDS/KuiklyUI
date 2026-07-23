plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 34
    namespace = "com.tencent.kuikly.calendar.android"

    defaultConfig {
        applicationId = "com.tencent.kuikly.calendar.demo"
        minSdk = 24
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packagingOptions {
        doNotStrip("**/*.so")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("com.tencent.kuikly-open:core-render-android:${Version.getKuiklyVersion()}")
    implementation(Dependencies.material)
    implementation(Dependencies.androidxAppcompat)
    implementation(Dependencies.androidXCoreKtx)
}

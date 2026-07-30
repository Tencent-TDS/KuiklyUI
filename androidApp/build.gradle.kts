plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.kuikly.table.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.kuikly.table.android"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":shared"))

    // Kuikly Android 渲染端（与 shared 模块锁定同一版本：com.tencent.kuikly-open:2.15.0-2.0.21）
    implementation("com.tencent.kuikly-open:core-render-android:2.15.0-2.0.21")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
}

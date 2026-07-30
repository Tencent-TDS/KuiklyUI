plugins {
    id("com.android.application")
    kotlin("android")
}

android {
<<<<<<< HEAD
    namespace = "com.kuikly.table.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.kuikly.table.android"
        minSdk = 21
        targetSdk = 34
=======
    compileSdk = 34
    namespace = "com.tencent.kuikly.android.demo"
    defaultConfig {
        applicationId = "com.tencent.kuikly.android.demo"
        minSdk = 24
        targetSdk = 32
>>>>>>> fac3e1bf76900eca384d895d842c13066d9bcd67
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
<<<<<<< HEAD
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
=======
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    sourceSets.getByName("main") {
        jniLibs {
            srcDir("libs")
        }
    }

    packagingOptions {
        doNotStrip("**/*.so")
>>>>>>> fac3e1bf76900eca384d895d842c13066d9bcd67
    }
}

dependencies {
<<<<<<< HEAD
    implementation(project(":shared"))

    // Kuikly Android 渲染端（与 shared 模块锁定同一版本：com.tencent.kuikly-open:2.15.0-2.0.21）
    implementation("com.tencent.kuikly-open:core-render-android:2.15.0-2.0.21")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
}
=======
    implementation(fileTree("libs") {

        include("*.jar")
    })
    implementation(project(":core"))
    implementation(project(":demo"))
    implementation(project(":core-render-android"))
    implementation("com.squareup.okhttp3:okhttp:3.12.0")
    implementation(Dependencies.material)
    implementation(Dependencies.androidxAppcompat)

    implementation(Dependencies.androidXCoreKtx)

    implementation("com.github.bumptech.glide:glide:4.12.0") // Glide主库，确保这里的版本是最新的
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0") // Glide注解处理
    implementation("com.tencent.tav:libpag:4.1.49-noffavc")
    implementation("com.google.android.exoplayer:exoplayer:2.16.1")
    implementation("com.github.penfeizhou.android.animation:apng:2.25.0")

}
>>>>>>> fac3e1bf76900eca384d895d842c13066d9bcd67

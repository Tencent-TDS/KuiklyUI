plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 34
    defaultConfig {
        applicationId = "com.tencent.kuikly.android.demo"
        minSdk = 21
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
    sourceSets.getByName("main") {
        jniLibs {
            srcDir("libs")
        }
    }

    packagingOptions {
        doNotStrip("**/*.so")
    }
}

dependencies {
    implementation(fileTree("libs") {

        include("*.jar")
    })
    implementation(project(":core"))
    implementation(project(":demo"))
    implementation(project(":core-render-android"))
    implementation("com.squareup.okhttp3:okhttp:3.12.0")
    implementation(Dependencies.material)
    implementation(Dependencies.androidxAppcompat)

    implementation(Dependencies.hippy)
    implementation(Dependencies.androidXCoreKtx)

    implementation("com.github.bumptech.glide:glide:4.12.0") // Glide主库，确保这里的版本是最新的
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0") // Glide注解处理
    implementation("com.tencent.tav:libpag:4.1.49-noffavc")
    implementation("com.google.android.exoplayer:exoplayer:2.16.1")
    implementation("com.github.penfeizhou.android.animation:apng:2.25.0")

}
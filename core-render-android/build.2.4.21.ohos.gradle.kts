import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    signing
}

group = MavenConfig.GROUP
version = Version.getCoreVersion()

afterEvaluate {
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
        }

        publications {
            create<MavenPublication>("maven") {
                groupId = MavenConfig.GROUP
                artifactId = MavenConfig.RENDER_ANDROID_ARTIFACT_ID
                version = Version.getRenderVersion()
                from(components["release"])
                pom.configureMavenCentralMetadata()
                signPublicationIfKeyPresent(project)
            }
        }
    }
}

android {
    compileSdk = 35
    namespace = "com.tencent.kuikly.core.render.android"
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        moduleName.set("${project.group}.${project.name}")
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    compileOnly(project(":core"))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
}

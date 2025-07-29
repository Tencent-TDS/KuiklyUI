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
            if (!username.isEmpty() && !password.isEmpty()) {
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
            create<MavenPublication>("release") {
                groupId = MavenConfig.GROUP
                artifactId = "core"
                version = Version.getCoreVersion()
                artifact(tasks.getByName("bundleReleaseAar"))
//                artifact("$buildDir/outputs/aar/core-compat-release.aar")
            }
//            tasks.named("publishReleasePublicationToMavenRepository").configure {
//                dependsOn("bundleReleaseAar")
//            }
//            signPublicationIfKeyPresent(project)
//            pom.configureMavenCentralMetadata()
        }
    }


}


android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro")
//        }
//    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_7
        targetCompatibility = JavaVersion.VERSION_1_7
    }
//    kotlinOptions {
//        freeCompilerArgs = freeCompilerArgs + listOf(
//            "-Xmulti-platform"
//        )
//    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += listOf("-Xmulti-platform")
    }

    val fileDir = rootProject.rootDir.absolutePath + "/core/src"
    sourceSets.getByName("main").java.srcDirs(
        "$fileDir/commonMain/kotlin",
        "$fileDir/androidMain/kotlin"
    )
    sourceSets.getByName("main").manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Version.getKotlinVersion()}")
}
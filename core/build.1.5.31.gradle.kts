import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform")
    // kotlin("native.cocoapods")
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

//    ios()
//    iosSimulatorArm64()

    // sourceSets
    val commonMain by sourceSets.getting {
        dependencies {
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31")
        }
    }

    val androidMain by sourceSets.getting {
        dependsOn(commonMain)
        dependencies {
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31")
        }
    }


//    val iosMain by sourceSets.getting {
//        dependsOn(commonMain)
//    }

//    targets.withType<KotlinNativeTarget> {
//        val mainSourceSets = this.compilations.getByName("main").defaultSourceSet
//        when {
//            konanTarget.family.isAppleFamily -> {
//                mainSourceSets.dependsOn(iosMain)
//            }
//        }
//    }

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
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }
}
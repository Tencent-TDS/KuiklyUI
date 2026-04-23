plugins {
    kotlin("multiplatform")
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

// core-wx: Kuikly WeChat MiniProgram bindings.
// Only js(IR) target -> no android / iOS zombie classes.
kotlin {
    js(IR) {
        moduleName = "KuiklyCore-core-wx"
        browser {
            webpackTask {
                outputFileName = "${moduleName}.js"
            }
            commonWebpackConfig {
                output?.library = null
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":core"))
            }
        }
    }
}

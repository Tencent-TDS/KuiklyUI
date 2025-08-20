plugins {
    // Import KMM plugin
    kotlin("multiplatform")
    // Import Android library plugin, provides maven publishing configuration
    // id("com.android.library")
    // Import maven publishing plugin
    id("maven-publish")
}

// maven 产物 groupId，com.tencent.kuikly
group = MavenConfig.GROUP_WEB
// maven 产物版本，这里统一使用 render 的版本号
version = Version.getCoreVersion()


// 配置 maven 发布
publishing {
    repositories {
        // 仓库配置，未配置用户名和密码的情况下发布到本地
        val username = MavenConfig.getUsername(project)
        val password = MavenConfig.getPassword(project)
        if (username.isNotEmpty() && password.isNotEmpty()) {
            // 流水线配置了用户名密码才会走到这个逻辑
            maven {
                credentials {
                    setUsername(username)
                    setPassword(password)
                }
                url = uri(MavenConfig.getRepoUrl(version as String))
            }
        } else {
            // 否则本地逻辑发布到本地
            mavenLocal()
        }
    }
}


kotlin {
    js(IR) {
        moduleName = "KuiklyCore-render-web-h5"
        // Output build products that support browser execution
        browser {
            webpackTask {
                outputFileName = "${moduleName}.js" // Final output name
            }

            commonWebpackConfig {
                output?.library = null // Don't export global objects, only export necessary entry functions
            }
        }
        // Output executable JS rather than library
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                // Import js standard library
                api(project(":core-render-web:base"))
            }
        }
    }
}


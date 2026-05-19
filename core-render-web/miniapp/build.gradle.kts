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
        moduleName = "KuiklyCore-render-web-miniapp"
        // Output build products that support browser execution
        browser {
            webpackTask {
                outputFileName = "${moduleName}.js" // Final output name
                // 禁用 webpack 的代码压缩和混淆
                mode = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.DEVELOPMENT
            }

            commonWebpackConfig {
                output?.library = null // Don't export global objects, only export necessary entry functions
                // 禁用 webpack 优化
                devtool = null
            }
        }
        // Output executable JS rather than library
        binaries.executable()
        
        // 添加编译选项：禁用成员名称混淆
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-Xir-minimized-member-names=false",  // 禁用成员名称混淆
                    "-Xir-property-lazy-initialization=false"  // 禁用惰性属性初始化优化
                )
            }
        }
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

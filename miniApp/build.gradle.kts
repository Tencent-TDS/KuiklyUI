import java.nio.file.Paths

plugins {
    // Import KMM plugin
    kotlin("multiplatform")
}

kotlin {
    // Build JS output for webApp
    js(IR) {
        // Build output supports browser
        browser {
            webpackTask {
                // Final output executable JS filename
                outputFileName = "miniprogramApp.js"
            }

            commonWebpackConfig {
                // Do not export global objects, only export necessary entry methods
                output?.library = null
                devtool = null
                // devtool = org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool.INLINE_CHEAP_SOURCE_MAP
            }
        }
        // Package render code and webApp code together and execute directly
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                // Import web render
                implementation(project(":core-render-web:base"))
                implementation(project(":core-render-web:miniapp"))
            }
        }
    }
}

// Business project path name
val businessPathName = "demo"

/**
 * Copy locally built unified JS result to miniprogramApp's dist/business directory
 */
fun copyLocalJSBundle(buildSubPath: String) {
    // Output target path
    val destDir = Paths.get(project.buildDir.absolutePath, "../",
        "dist", "business").toFile()
    if (!destDir.exists()) {
        // Create directory if it doesn't exist
        destDir.mkdirs()
    } else {
        // Remove original files if directory exists
        destDir.deleteRecursively()
    }

    val sourceDir = Paths.get(
        project.rootDir.absolutePath,
        businessPathName,
        "build/kotlin-webpack/js", buildSubPath
    ).toFile()

    println("Copying from: ${sourceDir.absolutePath}")
    println("Copying to: ${destDir.absolutePath}")

    // Copy files
    project.copy {
        // Copy js files from business build result
        from(sourceDir) {
            include("nativevue2.js")
        }
        into(destDir)
    }
}

project.afterEvaluate {
    // 创建前置任务：生成 webpack 配置
    tasks.register("generateWebpackConfig") {
        group = "kuikly"
        description = "Generate webpack configuration before compilation"
        
        doLast {
            val configDir = File(projectDir.absolutePath, "webpack.config.d")
            if (!configDir.exists()) {
                configDir.mkdirs()
            }
            
            val configFile = File(configDir, "config.js")
            configFile.writeText("""
                config.target = 'node';
            """.trimIndent())
            
            println("Generated webpack config at: ${configFile.absolutePath}")
        }
    }

    // 让 JS 编译任务依赖于配置生成任务
    tasks.named("compileKotlinJs") {
        dependsOn("generateWebpackConfig")
    }
    
    tasks.named("jsBrowserDevelopmentWebpack") {
        dependsOn("generateWebpackConfig")
    }
    
    tasks.named("jsBrowserProductionWebpack") {
        dependsOn("generateWebpackConfig")
    }

    // kotlin 1.9 from 改为 $buildDir/dist/js/distributions
    tasks.register<Copy>("syncRenderProductionToDist") {
        from("$buildDir/kotlin-webpack/js/productionExecutable")
        into("$projectDir/dist/lib")
        include("**/*.js", "**/*.d.ts")
    }

    // kotlin 1.9 from 改为 $buildDir/dist/js/developmentExecutable
    tasks.register<Copy>("syncRenderDevelopmentToDist") {
        from("$buildDir/kotlin-webpack/js/developmentExecutable")
        into("$projectDir/dist/lib")
        include("**/*.js", "**/*.d.ts")
    }

    tasks.register<Copy>("copyAssets") {
        val assetsDir = Paths.get(
            project.rootDir.absolutePath,
            businessPathName,
            "src/commonMain/assets"
        ).toFile()
        from(assetsDir)
        into("$projectDir/dist/assets")
        include("**/**")
    }


    tasks.named("jsBrowserProductionWebpack") {
        finalizedBy("syncRenderProductionToDist")
    }

    tasks.named("jsBrowserDevelopmentWebpack") {
        finalizedBy("syncRenderDevelopmentToDist")
    }

    tasks.register("jsMiniAppProductionWebpack") {
        group = "kuikly"
        // First execute jsBrowserProductionWebpack build task
        dependsOn("jsBrowserProductionWebpack")
        // Then copy corresponding nativevue2.zip from business build result and copy nativevue2.js
        // to miniprogramApp's release directory
        doLast {
            copyLocalJSBundle("productionExecutable")
        }
    }

    tasks.register("jsMiniAppDevelopmentWebpack") {
        group = "kuikly"
        // First execute jsBrowserDevelopmentWebpack build task
        dependsOn("jsBrowserDevelopmentWebpack")
        // Then copy corresponding nativevue2.zip from business build result and copy nativevue2.js
        // to miniprogramApp's release directory
        doLast {
            copyLocalJSBundle("developmentExecutable")
        }
    }
}

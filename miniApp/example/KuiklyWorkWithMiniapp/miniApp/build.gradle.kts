import java.nio.file.Paths

plugins {
    // Import KMM plugin
    kotlin("multiplatform")
}

kotlin {
    // Build JS output for miniApp
    js(IR) {
        // Build output supports browser
        browser {
            webpackTask {
                // Final output executable JS filename
                outputFileName = "miniApp.js"
                webpackConfigApplier {
                    val tempConfigFile = File(project.buildDir, "../webpack.config.d/config.js")
                    tempConfigFile.parentFile.mkdirs()
                    // use node target
                    tempConfigFile.writeText(
                        """
                        config.target = 'node';
                    """.trimIndent()
                    )
                    file(tempConfigFile.absolutePath)
                }
            }

            commonWebpackConfig {
                // Do not export global objects, only export necessary entry methods
                output?.library = null
                // use next line to enable source map
                // devtool = org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool.INLINE_CHEAP_SOURCE_MAP
                devtool = null
            }
        }
        // Package render code and miniApp code together and execute directly
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                // Import web render
                implementation("com.tencent.kuikly-open.core-render-web:base:${Version.getKuiklyVersion()}")
                implementation("com.tencent.kuikly-open.core-render-web:miniapp:${Version.getKuiklyVersion()}")
            }
        }
    }
}

// Business project path name
val businessPathName = "shared"

/**
 * Copy locally built unified JS result to miniApp's dist/business directory
 */
fun copyLocalJSBundle(buildSubPath: String) {
    // Output target path
    val destDir = Paths.get(
        project.buildDir.absolutePath, "../",
        "dist", "business"
    ).toFile()
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
        "build/dist/js", buildSubPath
    ).toFile()

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
    // kotlin 1.9 from 改为 $buildDir/dist/js/distributions
    tasks.register<Copy>("syncRenderProductionToDist") {
        from("$buildDir/kotlin-webpack/js/distributions")
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
        // to miniApp's release directory
        copyLocalJSBundle("distributions")
    }

    tasks.register("jsMiniAppDevelopmentWebpack") {
        group = "kuikly"
        // First execute jsBrowserDevelopmentWebpack build task
        dependsOn("jsBrowserDevelopmentWebpack")
        // Then copy corresponding nativevue2.zip from business build result and copy nativevue2.js
        // to miniApp's release directory
        copyLocalJSBundle("developmentExecutable")
    }
}
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
                webpackConfigApplier {
                    val tempConfigFile = File(projectDir.absolutePath, "/webpack.config.d/config.js")
                    tempConfigFile.parentFile.mkdirs()
                    // use node target
                    tempConfigFile.writeText("""
                        config.target = 'node';
                    """.trimIndent())
                    file(tempConfigFile.absolutePath)
                }
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
        copyLocalJSBundle("productionExecutable")
    }

    tasks.register("jsMiniAppDevelopmentWebpack") {
        group = "kuikly"
        // First execute jsBrowserDevelopmentWebpack build task
        dependsOn("jsBrowserDevelopmentWebpack")
        // Then copy corresponding nativevue2.zip from business build result and copy nativevue2.js
        // to miniprogramApp's release directory
        copyLocalJSBundle("developmentExecutable")
    }
}

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
        }
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
        }
    }
}

// Kotlin 2.3.10 分支：用于三端 App 的构建与运行验证（./k2310.sh 默认 settings）
// 与 settings.2.3.10.gradle.kts（发布用，不含 App 宿主）的区别：
// 本文件额外包含 :demo / :androidApp / :h5App / :miniApp 宿主工程
val buildFileName = "build.2.3.10.gradle.kts"
rootProject.buildFileName = buildFileName

include(":core-annotations")
project(":core-annotations").buildFileName = buildFileName

include(":core-ksp")
project(":core-ksp").buildFileName = buildFileName

include(":core")
project(":core").buildFileName = buildFileName

include(":core-wx")
project(":core-wx").buildFileName = buildFileName

include(":core-render-android")
project(":core-render-android").buildFileName = buildFileName

include(":core-render-web:base")
project(":core-render-web:base").buildFileName = buildFileName
include(":core-render-web:h5")
project(":core-render-web:h5").buildFileName = buildFileName
include(":core-render-web:miniapp")
project(":core-render-web:miniapp").buildFileName = buildFileName

include(":compose")
project(":compose").buildFileName = buildFileName

include(":demo")
project(":demo").buildFileName = buildFileName

include(":androidApp")
project(":androidApp").buildFileName = "build.gradle.kts"

include(":h5App")
project(":h5App").buildFileName = buildFileName

include(":miniApp")
project(":miniApp").buildFileName = buildFileName

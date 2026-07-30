pluginManagement {
    repositories {
        // 国内镜像优先，加速插件解析
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // 统一声明 KSP 插件版本，避免子模块声明不同版本导致冲突
        id("com.google.devtools.ksp") version "2.0.21-1.0.28"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内镜像优先（Tencent 是 Kuikly 官方开源仓库，必须保留才能解析 com.tencent.kuikly-open）
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "KuiklyTableUI"

include(":shared")
include(":androidApp")
// iOS 宿主是独立的 Xcode 工程（iosApp/），不纳入 Gradle 构建。
// 在 macOS 上用 Xcode 打开 iosApp/iosApp.xcodeproj 即可，不需要在此 include。

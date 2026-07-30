// 检测 CI 环境：GitHub Actions 会设置 GITHUB_ACTIONS / CI 环境变量。
// 注意：settings.gradle.kts 顶层的 val 在 pluginManagement 块（早期求值）里不可见，
// 因此这里直接内联 System.getenv(...) 判断，避免 Unresolved reference。
// 海外 CI runner 访问国内镜像（阿里云/华为云）不稳定（502 / 超时），
// 故 CI 下只用全球源 + Tencent 官方源（Kuikly 仅在 Tencent Maven 发布）；
// 本地（中国）仍优先用国内镜像加速，不影响本地构建速度。

pluginManagement {
    repositories {
        // 仅非 CI 环境使用国内镜像加速插件解析
        if (System.getenv("GITHUB_ACTIONS") == null && System.getenv("CI") == null) {
            maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        }
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
        // Tencent 官方仓库：Kuikly 只在 Tencent Maven 发布，CI 也必须保留
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
        // 仅非 CI 环境使用国内镜像加速：
        // Maven Central 镜像（aliyun/public）排在 Google 镜像之前，
        // 保证 kotlinx-coroutines 等中央仓库构件优先从中央镜像解析，
        // 避免先打 Google 镜像（无 coroutines）返回 502 导致解析中断。
        if (System.getenv("GITHUB_ACTIONS") == null && System.getenv("CI") == null) {
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "KuiklyTableUI"

include(":shared")
include(":androidApp")
// iOS 宿主是独立的 Xcode 工程（iosApp/），不纳入 Gradle 构建。
// 在 macOS 上用 Xcode 打开 iosApp/iosApp.xcodeproj 即可，不需要在此 include。

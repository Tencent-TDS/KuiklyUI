# Android 宿主应用 (androidApp)

## 概述

Android 宿主应用，用于在 Android 设备/模拟器上运行和验证表格组件。

## 文件结构

```
androidApp/
├── build.gradle.kts                    # Android 应用构建配置
└── src/main/
    ├── AndroidManifest.xml             # 应用清单（含网络权限、Activity 注册）
    ├── java/com/kuikly/table/android/
    │   ├── KRApplication.kt            # Application 入口（初始化日志系统 + @Page 路由注册）
    │   ├── KuiklyRenderActivity.kt     # Kuikly 容器 Activity
    │   └── adapter/
    │       ├── KRImageAdapter.kt       # 图片加载适配器
    │       ├── KRLogAdapter.kt         # 日志适配器（Logcat + 文件日志）
    │       ├── KRRouterAdapter.kt      # 路由适配器
    │       ├── KRThreadAdapter.kt      # 线程适配器
    │       └── KRUncaughtExceptionHandlerAdapter.kt  # 异常捕获（含崩溃日志保存）
    └── res/
        ├── layout/activity_hr.xml      # Activity 布局
        ├── values/styles.xml           # 主题样式
        └── xml/network_security_config.xml  # 网络安全配置
```

## 关键类说明

### KRApplication
- 继承 `Application`
- 在 `onCreate()` 中按顺序：
  1. 初始化日志系统（`TableLog.init`）
  2. 注册 `@Page` 路由表（`KuiklyCoreEntry.triggerRegisterPages()`）
- 在 `onTerminate()` 中刷新日志缓冲区

### KuiklyRenderActivity
- 继承 `AppCompatActivity`
- 实现 `KuiklyRenderViewBaseDelegatorDelegate`
- 启动时默认打开 `tableDemo` 演示页
- 通过 `start(context, pageName, pageData)` 静态方法跳转到其他页面

### 日志系统集成
- `KRLogAdapter`：同时输出到 Android Logcat 和文件日志
- `KRUncaughtExceptionHandlerAdapter`：未捕获异常保存到独立崩溃日志文件
- 崩溃日志包含：时间戳、应用版本、设备信息、完整堆栈

## 运行方式

1. Android Studio 打开仓库根目录
2. 选择 `androidApp` 运行配置
3. 启动模拟器或连接真机（minSdk 21+）
4. 自动打开 `tableDemo` 演示页

## 构建

```bash
./gradlew :androidApp:assembleDebug
```

## 依赖

- Kuikly Android 渲染端: `com.tencent.kuikly-open:core-render-android:2.15.0-2.0.21`
- AndroidX: appcompat, recyclerview, core-ktx
- Glide: 图片加载

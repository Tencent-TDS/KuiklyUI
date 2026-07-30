# iOS 宿主工程

本目录是一个标准的 Xcode 工程，用于在 **macOS + Xcode + CocoaPods** 下验证 iOS 端运行。
Windows 无法构建 iOS，因此默认未纳入根 `settings.gradle.kts`（iOS 是独立 Xcode 工程，不是 Gradle 模块）。

## 原理

`shared` 模块通过 Kotlin/Native + CocoaPods 产出 `shared.framework`；本工程用
`pod 'shared', :path => '../shared'` 以本地 pod 方式引入，再由 `OpenKuiklyIOSRender`（~> 2.7.0）
完成页面渲染。组件源码位于 `shared/src/commonMain`，Android 与 iOS 共用同一份实现，**无需拷贝代码**。

## 本地验证步骤（需要 Mac）

1. 安装 Xcode 15+ 与 CocoaPods：`sudo gem install cocoapods`。
2. 在工程根目录执行：
   ```bash
   ./gradlew :shared:podInstall     # 生成 shared.podspec 并执行 pod install、构建 framework
   cd iosApp
   pod install
   ```
3. 打开工作区：`open iosApp.xcworkspace`（注意不是 `.xcodeproj`）。
4. 选模拟器（iOS 14.1+），⌘R 运行。启动后加载 `@Page("tableDemo")` 演示页，
   验证表格渲染、横纵滚动、点击事件、主题与固定列。

## 没有 Mac 怎么办（你的情况）

iOS 端编译验证已交由根目录 `.github/workflows/ci.yml` 的 `ios` Job 完成：
push 代码后，GitHub 的 macOS runner 会自动 `pod install` + 用 Xcode 编译 `iosApp` 宿主。
你只需要在仓库 **Actions** 页面确认该 Job 变绿即可，无需本地 Mac。

## 日志系统

iOS 端使用与 Android 端相同的日志接口，日志文件保存在 App 的 Documents 目录下：

```
Documents/kuikly_table_logs/
├── kuikly_table.log          # 主日志
├── kuikly_table_crash.log    # 崩溃日志
└── kuikly_table.log.1~5      # 轮转备份
```

崩溃时自动生成独立崩溃报告 `crash_YYYYMMDD_HHMMSS.log`，包含设备信息、应用版本和完整堆栈。

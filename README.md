# KuiklyTableUI

基于 [Kuikly](https://kuikly.tds.qq.com) 跨平台框架开发的表格组件，组件源码位于 `shared/src/commonMain`，Android / iOS 共用同一份代码。

## 功能特性

- 多行多列数据展示
- 横纵双向滚动，支持大数据量（固定列 + 嵌套 ScrollerView，数据量大时建议配合固定列使用）
- 斑马纹、行/列/外边框、内边距、对齐等样式配置
- 固定列（冻结列）支持，左右面板滚动联动
- 自定义表格主题（内置 DEFAULT / DARK / COMPACT / BLUE 四套预设，亦可用 DSL 自定义）
- 单元格自定义渲染（在单元格内嵌入任意 Kuikly 组件）
- 简洁的 DSL 语法（`Table { ... }`）
- 事件系统：单元格点击 / 行点击 / 表头点击 / 滚动
- 跨平台支持（Android / iOS，共用同一份 `commonMain` 代码）

## 每个文件的功能

### 工程配置
| 文件 | 功能 |
|------|------|
| `settings.gradle.kts` | Gradle 根配置，注册 `:shared` / `:androidApp` 模块 |
| `build.gradle.kts` | 根工程，统一插件版本 |
| `gradle.properties` | Gradle 全局属性（JVM 参数等） |
| `gradlew` / `gradlew.bat` | Gradle Wrapper 脚本（macOS/Linux 与 Windows） |
| `gradle/wrapper/` | Wrapper 可执行 jar 与分发配置（Gradle 8.4） |
| `.github/workflows/ci.yml` | GitHub Actions 双平台 CI：Android（Ubuntu）+ iOS（macOS） |
| `shared/shared.podspec` | CocoaPods 配置，供 iOS 以本地 pod 集成 `shared` |
| `iosApp/Podfile` | iOS 依赖（OpenKuiklyIOSRender ~> 2.15.0 + SDWebImage） |

### shared（KMP 共享模块，组件核心）

**commonMain（跨平台核心）**
| 文件 | 功能 |
|------|------|
| `KuiklyTableView.kt` | 表格主组件（ComposeView），负责整体渲染与滚动布局 |
| `TableAttr.kt` | 表格属性定义（列/行/主题/边框/滚动等） |
| `TableEvent.kt` | 事件定义（单元格点击 / 行点击 / 表头点击 / 滚动） |
| `model/ColumnDef.kt` | 列定义（key/title/width/customRenderer） |
| `model/CellData.kt` | 单元格数据 |
| `model/TableData.kt` | 表格数据容器 |
| `model/TableTheme.kt` | 主题模型，内置 DEFAULT / DARK / COMPACT / BLUE 预设 |
| `model/TextAlign.kt` | 对齐枚举 |
| `dsl/TableDsl.kt` | DSL 入口 `Table { }`，提供 column/row/theme 等链式 API |
| `log/TableLogger.kt` | 日志模块 `expect` 声明（跨平台接口） |
| `demo/TableDemoPage.kt` | 演示入口列表页，`@Page("tableDemo")` |
| `demo/DemoTables.kt` | 9 个表格示例的 DSL 配置（复用） |
| `demo/DemoPages.kt` | 独立演示页 `tableDemo1`~`tableDemo9`（`@Page` 注册） |

**androidMain / iosMain（平台实现）**
| 文件 | 功能 |
|------|------|
| `androidMain/.../log/TableLogger.android.kt` | 日志模块 `actual`（Android，JVM `synchronized` 加锁） |
| `iosMain/.../log/TableLogger.ios.kt` | 日志模块 `actual`（iOS，Foundation C interop） |

**commonTest（单元测试）**
| 文件 | 功能 |
|------|------|
| `TableAttrTest.kt` / `TableEventTest.kt` | 属性 / 事件测试 |
| `dsl/TableDslTest.kt` | DSL 测试 |
| `model/CellDataTest.kt`、`ColumnDefTest.kt`、`TableDataTest.kt`、`TableThemeTest.kt`、`TextAlignTest.kt` | 模型测试 |

### androidApp（Android 宿主，可直接运行）
| 文件 | 功能 |
|------|------|
| `build.gradle.kts` | Android 应用配置 |
| `AndroidManifest.xml` | 清单（注册宿主 Activity、网络配置） |
| `KRApplication.kt` | Application，调用 `KuiklyCoreEntry.triggerRegisterPages()` 注册页面 |
| `KuiklyRenderActivity.kt` | Kuikly 渲染宿主 Activity |
| `adapter/KRImageAdapter.kt` | 图片适配器 |
| `adapter/KRLogAdapter.kt` | 日志适配器 |
| `adapter/KRRouterAdapter.kt` | 路由适配器（object） |
| `adapter/KRThreadAdapter.kt` | 线程适配器 |
| `adapter/KRUncaughtExceptionHandlerAdapter.kt` | 未捕获异常适配器 |
| `res/layout/activity_hr.xml` | 宿主布局 |
| `res/values/styles.xml` | 样式 |
| `res/xml/network_security_config.xml` | 网络安全配置 |

### iosApp（iOS 宿主，macOS 验证）
| 文件 | 功能 |
|------|------|
| `iosApp.xcodeproj/project.pbxproj` | Xcode 工程配置 |
| `iosApp.xcworkspace/` | 工作区（含 CocoaPods） |
| `Info.plist` | 应用配置 |
| `iOSApp.swift` | App 入口 |
| `ContentView.swift` | 内容视图（承载 Kuikly 页面） |
| `KuiklyRenderViewPage.swift` | Kuikly 渲染页 |
| `iosApp-Bridging-Header.h` | ObjC / Swift 桥接头 |
| `KuiklyExpand/KuiklyRenderViewController.h/.m` | Kuikly 渲染视图控制器 |
| `KuiklyExpand/Handler/KRRouterHandler.h/.m` | 路由处理器 |
| `KuiklyExpand/Handler/KuiklyRenderComponentExpandHandler.h/.m` | 组件扩展处理器 |
| `KuiklyExpand/Modules/HRBridgeModule.h/.m` | 桥接模块 |
| `FDFullscreenPopGesture/` | 全屏手势返回（第三方） |
| `Assets.xcassets/` | 图标 / 色彩资源 |

## 验证情况

| 验证项 | 环境 | 结果 |
|--------|------|------|
| Android 实机运行 | Android Studio 模拟器 API 37.1 / Android 17（x86_64） | ✅ 通过 |
| 单元测试 | `./gradlew :shared:testDebugUnitTest`（8 个测试文件） | ✅ 通过 |
| iOS 编译验证 | GitHub Actions macOS runner（Kotlin/Native 框架 + Xcode 模拟器编译） | ✅ 通过 |

- **Android**：点击 `tableDemo` 列表进入 9 个独立演示页，确认基础表格、四套主题、斑马纹、横纵双向滚动、固定列、自定义渲染、事件回调均正常渲染。
- **iOS**：因仅有 Windows 电脑无 Mac，**运行时未做验证**；仅通过 CI 在 macOS 上完成**编译验证**（`:shared` Kotlin/Native 框架编译 + Xcode 模拟器架构编译 `iosApp` 宿主，均 BUILD SUCCESSFUL）。

## 验证截图

以下为 Android Studio 模拟器（API 37.1 / Android 17）实测截图：

| Demo 列表 | 基础表格 | 蓝色主题 |
|:---------:|:--------:|:--------:|
| ![Demo 列表](images/demo-list.png) | ![基础表格](images/demo-basic.png) | ![蓝色主题](images/demo-blue.png) |

| 深色主题 | 大数据量滚动 |
|:--------:|:------------:|
| ![深色主题](images/demo-dark.png) | ![大数据量滚动](images/demo-bigdata.png) |

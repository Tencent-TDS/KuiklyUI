# KuiklyTableUI

基于 [Kuikly](https://kuikly.tds.qq.com) 跨平台框架开发的表格组件，支持多行多列数据展示、横纵双向滚动和自定义样式配置。组件源码位于 `shared/src/commonMain`，可在 Android / iOS 共用。

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

## 效果预览

运行 `androidApp` 后进入 `tableDemo` 列表页，点击对应条目即可查看 9 个独立演示页。以下为 Android Studio 模拟器（API 37.1 / Android 17）实测截图：

| Demo 列表 | 基础表格 | 蓝色主题 |
|:---------:|:--------:|:--------:|
| ![Demo 列表](images/demo-list.png) | ![基础表格](images/demo-basic.png) | ![蓝色主题](images/demo-blue.png) |

| 深色主题 | 大数据量滚动 |
|:--------:|:------------:|
| ![深色主题](images/demo-dark.png) | ![大数据量滚动](images/demo-bigdata.png) |

## 仓库结构

```
KuiklyTableUI/
├── settings.gradle.kts          # 根工程：注册 :shared / :androidApp（:iosApp 默认关闭）
├── build.gradle.kts             # 根工程：统一插件版本
├── shared/                      # KMP 共享模块（组件核心代码）
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/com/kuikly/table/
│       ├── KuiklyTableView.kt   # 表格主组件（ComposeView）
│       ├── TableAttr.kt         # 属性
│       ├── TableEvent.kt        # 事件
│       ├── model/               # ColumnDef / CellData / TableData / TableTheme / TextAlign
│       ├── dsl/TableDsl.kt      # DSL 入口 Table {}
│       └── demo/
│           ├── TableDemoPage.kt # 演示入口列表页，@Page("tableDemo")
│           ├── DemoTables.kt    # 9 个表格示例的 DSL 配置（复用）
│           └── DemoPages.kt     # 独立演示页 tableDemo1~9（@Page 注册）
│   └── src/commonTest/...       # 单元测试（8 个文件）
├── androidApp/                  # Android 宿主（可直接运行）
│   ├── build.gradle.kts
│   └── src/main/...             # KuiklyRenderActivity + 5 个必需适配器 + 布局
└── iosApp/                      # iOS 宿主（macOS 验证用，详见「运行与验证」一节）
```

## 快速开始

### 方式一：直接运行演示（推荐验证）

1. 安装 Android Studio（Giraffe / Hedgehog 及以上，内置 KMP 支持），确保可访问 Kuikly 官方开源 Maven 仓库（`https://mirrors.tencent.com/nexus/repository/maven-tencent/`）。
2. 用 Android Studio 打开本仓库根目录。
3. 选择 `androidApp` 运行配置，启动模拟器或连接真机（minSdk 21+）。
4. 应用启动后默认打开 `tableDemo` 演示入口页（列表展示 9 个示例）。点击任意一项，路由跳转到对应的独立演示页 `tableDemo1`~`tableDemo9`（每个页面顶部带"返回"按钮回到列表）。

### 方式二：在你的 Kuikly 工程中集成组件

将 `shared/src/commonMain/kotlin/com/kuikly/table/` 整个目录拷贝到你的宿主工程
`shared/src/commonMain/kotlin/com/kuikly/table/`，即可在任意 Page 的 `body()` 中使用：

```kotlin
import com.kuikly.table.dsl.Table

Table {
    column("name", "姓名", 120f)
    column("age", "年龄", 80f)
    column("city", "城市", 100f)

    row("张三", "28", "北京")
    row("李四", "32", "上海")
    row("王五", "25", "广州")
}
```

> 说明：本仓库当前以**源码形式**集成（组件未发布到 Maven）。如需发布为 Maven 依赖，
> 可在 `shared/build.gradle.kts` 中追加 `maven-publish` 配置后发布，再将下方坐标替换为你自己的仓库地址：
> ```kotlin
> implementation("com.kuikly.table:table:1.0.0")
> ```

## DSL 用法

### 自定义主题

```kotlin
Table {
    column("product", "产品", 150f)
    column("price", "价格", 100f)

    row("Kuikly Pro", "99.00")
    row("Kuikly Plus", "199.00")

    theme {
        headerBackgroundColor = 0xFF1976D2
        headerTextColor = 0xFFFFFFFF
        headerFontSize = 15f
        stripedRows = true
        rowAlternateColor = 0xFFE3F2FD
        borderColor = 0xFFBBDEFB
    }
}
```

### 大数据量滚动

```kotlin
Table {
    for (i in 1..10) {
        column("col$i", "列$i", 100f)
    }
    for (r in 1..100) {
        row(*(1..10).map { "R${r}C${it}" }.toTypedArray())
    }
    maxHeight(400f)
    scrollEnabled(true)
}
```

### 固定列

```kotlin
Table {
    column("id", "ID", 60f)
    column("name", "姓名", 120f)
    column("email", "邮箱", 200f)
    column("phone", "电话", 150f)

    row("001", "张三", "zhangsan@qq.com", "13800138001")
    row("002", "李四", "lisi@qq.com", "13800138002")

    fixedColumns(1)
}
```

### 事件处理

```kotlin
Table {
    column("action", "操作", 80f)
    column("detail", "详情", 200f)

    row("查看", "任务详情")
    row("编辑", "修改配置")

    onCellClick { rowIndex, colIndex, value ->
        println("Cell clicked: [$rowIndex, $colIndex] = $value")
    }
    onRowClick { rowIndex -> println("Row clicked: $rowIndex") }
}
```

### 单元格自定义渲染

```kotlin
val statusColumn = ColumnDef(
    key = "status",
    title = "状态",
    width = 100f,
    customRenderer = { cellData, _, _ ->
        View {
            attr {
                backgroundColor(if (cellData.value == "ok") 0xFF4CAF50 else 0xFFF44336)
                borderRadius(4f)
                padding(4f)
            }
            Text { attr { text(cellData.value); color(0xFFFFFFFF); fontSize(12f) } }
        }
    }
)

Table {
    columns(statusColumn)
    row("ok")
    row("error")
}
```

## API 文档

### DSL 方法

| 方法 | 说明 | 参数 |
|------|------|------|
| `column(key, title, width)` | 定义一列 | key: 列标识, title: 列标题, width: 列宽 |
| `columns(vararg cols)` | 批量定义列 | ColumnDef 列表 |
| `row(vararg values)` | 添加一行 | 各列对应的值 |
| `rows(data)` | 批量设置数据 | List<List<String>> |
| `data(tableData)` | 从 TableData 填充 | TableData 对象 |
| `theme(theme)` | 设置主题 | TableTheme 对象 |
| `theme { ... }` | DSL 配置主题 | ThemeBuilder |
| `headerVisible(bool)` | 是否显示表头 | Boolean |
| `fixedColumns(count)` | 固定列数 | Int |
| `maxHeight(height)` | 最大高度 | Float |
| `scrollEnabled(bool)` | 是否可滚动 | Boolean |
| `onCellClick { }` | 单元格点击 | (Int, Int, String) -> Unit |
| `onRowClick { }` | 行点击 | (Int) -> Unit |
| `onHeaderClick { }` | 表头点击 | (Int, String) -> Unit |
| `onScroll { }` | 滚动回调 | (Float, Float) -> Unit |

### TableTheme 属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| headerBackgroundColor | Long | 0xFFF5F5F5 | 表头背景色 |
| headerTextColor | Long | 0xFF333333 | 表头文字颜色 |
| headerFontSize | Float | 14f | 表头字号 |
| headerFontBold | Boolean | true | 表头字重是否加粗 |
| headerHeight | Float | 44f | 表头高度 |
| rowHeight | Float | 40f | 行高 |
| rowBackgroundColor | Long | 0xFFFFFFFF | 行背景色 |
| rowAlternateColor | Long | 0xFFFAFAFA | 交替行背景色 |
| cellTextColor | Long | 0xFF666666 | 单元格文字颜色 |
| cellFontSize | Float | 13f | 单元格字号 |
| cellPaddingHorizontal | Float | 8f | 单元格水平内边距 |
| cellPaddingVertical | Float | 4f | 单元格垂直内边距 |
| borderColor | Long | 0xFFE0E0E0 | 边框颜色 |
| borderWidth | Float | 0.5f | 边框宽度 |
| showRowBorder | Boolean | true | 显示行边框 |
| showColumnBorder | Boolean | true | 显示列边框 |
| showOuterBorder | Boolean | true | 显示外边框 |
| stripedRows | Boolean | true | 斑马纹 |

## 运行与验证

### Android（你可以在 Windows 上直接验证）
1. 用 Android Studio 打开本仓库根目录。
2. 选择 `androidApp` 运行配置，启动模拟器或连接真机（minSdk 21+）。
3. 应用启动后默认进入 `tableDemo` 列表页，点击任一项进入对应的独立演示页（tableDemo1~tableDemo9），分别验证：基础表格、对齐、斑马纹、横纵滚动、固定列、自定义渲染、事件回调、深色/紧凑主题；返回用各页顶部的"返回"按钮。
4. 页面路由表由 `KRApplication.onCreate()` 中的 `KuiklyCoreEntry.triggerRegisterPages()` 注册（编译期 KSP 自动生成，无需手写）。

### iOS（需要 macOS；你只有 Windows，走 CI 验证）
- 本地验证（macOS）：`cd iosApp && pod install && open iosApp.xcworkspace`，模拟器运行即可。
- 由于你只有一台 Windows 电脑，**iOS 端编译验证交由 GitHub Actions 的 macOS runner 完成**（见下方"持续集成"）。每次 push / PR 都会自动在 Mac 上构建 `shared` 的 Kotlin/Native 框架并编译 Xcode 宿主，等价于"在 Mac 上跑通"。

### 单元测试
`shared` 模块含 8 个测试文件，覆盖模型与 DSL。在 Android Studio 的 Gradle 面板运行 `:shared:testDebugUnitTest`，或命令行 `./gradlew :shared:testDebugUnitTest`。

## 持续集成（双平台验证）

本仓库包含 `.github/workflows/ci.yml`，分两个 Job：

- **android**（Ubuntu）：编译 `androidApp` Debug 包 + 运行 `shared` 单元测试。
- **ios**（macOS）：`pod install` 构建 `shared` 的 Kotlin/Native 框架，并用 Xcode 在模拟器架构下编译 `iosApp` 宿主。

只要 GitHub Actions 两个 Job 都变绿，即代表 Android 与 iOS 两端均能编译通过。你只需 push 代码，然后到仓库的 **Actions** 页面查看结果即可，无需本地 Mac。

## 开发环境

- Kotlin 2.0.21（KSP 2.0.21-1.0.28）
- Kuikly 框架（`com.tencent.kuikly-open:2.15.0-2.0.21`，官方开源坐标，仓库 `https://mirrors.tencent.com/nexus/repository/maven-tencent/`）
- Gradle 8.4（已内置 wrapper，无需手动安装）
- JDK 17（Gradle 8.4 + AGP 8.1 要求）
- Android Studio（Android）；Xcode 15+ + CocoaPods（iOS，仅 macOS 本地验证需要，CI 已自动覆盖）

## 接入注意事项（已对齐官方，部分项按官方文档推断）

- 已对齐到官方开源坐标 `com.tencent.kuikly-open:2.15.0-2.0.21`，相关 artifact 均已在腾讯 Maven 镜像核实存在。
- `@Page` 注解包名：`com.tencent.kuikly.core.annotations.Page`（已确认）。
- 页面注册：`KRApplication.onCreate()` 调用 `KuiklyCoreEntry.triggerRegisterPages()`，基于 `core-ksp`（KSP）在编译期生成的 `object KuiklyCoreEntry`（`com.tencent.kuikly.core.android`）推断实现，符合官方文档示例。
- `@Page` 的 KSP 参数（`moduleId` / `isMainModule` / `enableMultiModule`）取自官方 `kuiklyMultiModuleDemo`。本组件为单模块，`enableMultiModule=false`；若构建报 KSP 参数相关错误，可参照官方多模块文档调整。
- 未引入 `com.tencent.kuikly-open.kuikly` Gradle 插件（官方 multiModuleDemo 同样未引入；`@Page` 由 core-ksp 经 KSP 处理，无需该插件）。

## 许可证

Apache License 2.0（见 [LICENSE](LICENSE)）

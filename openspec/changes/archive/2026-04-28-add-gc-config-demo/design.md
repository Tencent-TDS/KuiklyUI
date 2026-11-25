# Design: Demo 工程 GC 配置与测试页面

## 适用 DSL 模式

本变更仅涉及 demo 模块，使用**自研 DSL**（Pager + body()）模式。

## 架构概览

本变更包含三个核心部分：

```
1. GCManager（expect/actual）— GC 配置与 suspend/resume 封装
2. GCTestPage — 复杂列表测试页面
3. 初始化集成 — 在 Kotlin/Native 初始化时调用 configureGC
```

### 平台适配策略

由于 `kotlin.native.runtime.GC` 仅在 Kotlin/Native 平台可用，采用 `expect`/`actual` 模式：

| Source Set | 实现 | 说明 |
|-----------|------|------|
| commonMain | `expect` 声明 | 定义 `configureGC()`、`suspendGC()`、`resumeGC()` |
| appleMain | `actual` 实现 | 调用 `kotlin.native.runtime.GC` 的真实 API |
| ohosArm64Main | `actual` 实现 | 调用 `kotlin.native.runtime.GC` 的真实 API |
| androidMain | `actual` 空实现 | JVM GC 由 JVM 管理，无需操作 |
| jsMain | `actual` 空实现 | JS 无 GC 控制 API |

## 类设计

### 1. GCManager（commonMain — expect 声明）

**文件**: `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`

```kotlin
package com.tencent.kuikly.demo.pages.base

/**
 * 配置 Kotlin/Native GC 参数。
 * 应在 Kotlin 初始化时调用一次。
 */
expect fun configureGC()

/**
 * 暂停 GC。内部会控制调用频率，避免过于频繁的 suspend/resume 切换。
 * 连续调用多次 suspendGC 只会在首次生效。
 */
expect fun suspendGC()

/**
 * 恢复 GC。与 suspendGC 配对使用。
 */
expect fun resumeGC()
```

### 2. GCManager（appleMain / ohosArm64Main — actual 实现）

**文件**: `demo/src/appleMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`
**文件**: `demo/src/ohosArm64Main/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`

```kotlin
package com.tencent.kuikly.demo.pages.base

import kotlin.native.runtime.GC
import kotlin.time.Duration.Companion.seconds

// GC suspend 状态追踪
private var isGCSuspended = false
// 上次 suspend 的时间戳（毫秒），用于频率控制
private var lastSuspendTimeMs: Long = 0L
// 最小 suspend 间隔（毫秒），防止过于频繁的 suspend/resume 切换
private const val MIN_SUSPEND_INTERVAL_MS = 500L

actual fun configureGC() {
    GC.regularGCInterval = 10.seconds
    GC.targetHeapBytes = 256L * 1024 * 1024
    GC.minHeapBytes = 256L * 1024 * 1024
    GC.targetHeapUtilization = 0.65
    // 如果 Kotlin 版本不支持以下字段，编译时注释掉
    GC.heapGrowthUseAllocatedBytes = true
}

actual fun suspendGC() {
    if (isGCSuspended) return
    val now = currentTimeMillis()
    if (now - lastSuspendTimeMs < MIN_SUSPEND_INTERVAL_MS) return
    lastSuspendTimeMs = now
    isGCSuspended = true
    GC.suspend()
}

actual fun resumeGC() {
    if (!isGCSuspended) return
    isGCSuspended = false
    GC.resume()
}

// 获取当前时间戳的辅助函数
private fun currentTimeMillis(): Long {
    return kotlin.system.getTimeMillis()
}
```

### 3. GCManager（androidMain / jsMain — actual 空实现）

**文件**: `demo/src/androidMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`
**文件**: `demo/src/jsMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`

```kotlin
package com.tencent.kuikly.demo.pages.base

actual fun configureGC() {
    // JVM/JS 平台无需配置 Kotlin/Native GC
}

actual fun suspendGC() {
    // JVM/JS 平台无需操作
}

actual fun resumeGC() {
    // JVM/JS 平台无需操作
}
```

### 4. GC 初始化调用（@EagerInitialization）

使用 Kotlin/Native 的 `@EagerInitialization` 注解标注顶层属性，使其在程序加载时自动执行初始化，无需手动调用。这比在 `BasePager.created()` 中手动调用更优雅，保证在任何 Kotlin 代码执行前 GC 就已配置好。

**方案**：在 appleMain / ohosArm64Main 的 GCManager.kt 中，添加一个 `@EagerInitialization` 标注的顶层属性：

```kotlin
// 在 appleMain/ohosArm64Main 的 GCManager.kt 中添加
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@EagerInitialization
private val gcInitializer = configureGC()
```

`@EagerInitialization` 会让 Kotlin/Native 运行时在库加载时立即执行该属性的初始化表达式，从而自动调用 `configureGC()`。

**优势**：
- 无需修改 `BasePager.kt`，零侵入
- 保证在任何页面创建之前 GC 参数就已生效
- 不需要 `expect`/`actual` 来处理初始化时机，因为 `@EagerInitialization` 本身就是 Kotlin/Native 专属注解，只在 Native 平台的 actual 文件中使用
- androidMain / jsMain 的空实现无需任何初始化逻辑

### 5. GCTestPage（测试页面）

**文件**: `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/GCTestPage.kt`

#### 页面结构

```
┌─────────────────────────────────────┐
│ NavBar: "GC Test Page"              │
├─────────────────────────────────────┤
│ 控制面板（固定在顶部）                │
│ ┌─────────────────────────────────┐ │
│ │ [GC Suspend: ON/OFF]            │ │
│ │ [内存等级: 1/2/3/4/5]           │ │
│ │ [滚到顶部] [滚到底部]           │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ List（flex: 1）                     │
│ ┌─────────────────────────────────┐ │
│ │ vforLazy item 0                 │ │
│ │ ┌───────┬──────────────────┐    │ │
│ │ │ Image │ Title             │    │ │
│ │ │       │ Subtitle          │    │ │
│ │ │       │ Description       │    │ │
│ │ └───────┴──────────────────┘    │ │
│ │ vforLazy item 1                 │ │
│ │ ...                             │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

#### 核心状态

```kotlin
@Page("GCTestPage")
internal class GCTestPage : BasePager() {
    // 数据列表
    private val dataList by observableList<GCTestItem>()
    // 是否启用 GC suspend/resume
    private var gcSuspendEnabled by observable(true)
    // 内存消耗等级（1-5，每级 10MB）
    private var memoryLevel by observable(1)
    // List 引用，用于滚动控制
    private lateinit var listRef: ViewRef<ListView<*, *>>
}
```

#### 内存模拟策略

每个 item 创建时，根据 `memoryLevel` 分配 `ByteArray`：

```kotlin
// 每级 10MB，分配 ByteArray 但不做 CPU 密集操作
private fun simulateMemoryConsumption(level: Int): List<ByteArray> {
    val arrays = mutableListOf<ByteArray>()
    val totalBytes = level * 10 * 1024 * 1024 // level * 10MB
    // 分成多个 1MB 的块，避免单次分配过大
    val chunkSize = 1 * 1024 * 1024
    val chunkCount = totalBytes / chunkSize
    repeat(chunkCount) {
        arrays.add(ByteArray(chunkSize))
    }
    return arrays
}
```

> **注意**：内存模拟的 `ByteArray` 存储在 item 的数据模型中，随 item 生命周期管理。当 item 被 vforLazy 回收时，对应的 `ByteArray` 也会被 GC 回收。

#### 滚动事件处理

```kotlin
List {
    ref { ctx.listRef = it }
    attr { flex(1f) }
    event {
        dragBegin {
            if (ctx.gcSuspendEnabled) {
                suspendGC()
            }
        }
        scrollEnd {
            if (ctx.gcSuspendEnabled) {
                resumeGC()
            }
        }
    }
    vforLazy({ ctx.dataList }) { item, index, count ->
        // item 布局...
    }
}
```

#### 控制按钮

1. **GC Suspend 开关**：切换 `gcSuspendEnabled` 状态，按钮显示当前状态（ON/OFF）
2. **内存等级按钮**：循环切换 1-5 级，按钮显示当前等级和对应内存大小
3. **滚到顶部**：调用 `listRef.view?.setContentOffset(0f, 0f, animated = true)`
4. **滚到底部**：调用 `listRef.view?.setContentOffset(0f, Float.MAX_VALUE, animated = true)`（或计算 contentSize）

## NativeBridge 交互

本变更不涉及 NativeBridge 交互。GC 操作完全在 Kotlin/Native 层面完成。

## 文件变更清单

### demo 模块

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `demo/src/commonMain/.../pages/base/GCManager.kt` | expect 声明 |
| 新增 | `demo/src/appleMain/.../pages/base/GCManager.kt` | actual 实现（iOS/macOS） |
| 新增 | `demo/src/ohosArm64Main/.../pages/base/GCManager.kt` | actual 实现（HarmonyOS） |
| 新增 | `demo/src/androidMain/.../pages/base/GCManager.kt` | actual 空实现 |
| 新增 | `demo/src/jsMain/.../pages/base/GCManager.kt` | actual 空实现 |
| 新增 | `demo/src/commonMain/.../pages/demo/GCTestPage.kt` | GC 测试页面 |

### 不修改的模块

- core — 不修改
- compose — 不修改
- core-render-* — 不修改

## 数据流图

```mermaid
flowchart TD
    A[App 启动 / 库加载] --> B[@EagerInitialization 触发]
    B --> C[configureGC]
    C --> D[设置 GC 参数]
    
    E[用户打开 GCTestPage] --> F[创建 10000 个 item]
    F --> G[每个 item 根据 memoryLevel 分配 ByteArray]
    
    H[用户开始拖拽 List] --> I{gcSuspendEnabled?}
    I -->|Yes| J[suspendGC]
    J --> K[频率控制检查]
    K -->|通过| L[GC.suspend]
    K -->|未通过| M[跳过]
    
    N[滚动结束] --> O{gcSuspendEnabled?}
    O -->|Yes| P[resumeGC]
    P --> Q[GC.resume]
    
    R[用户点击 GC 开关] --> S[切换 gcSuspendEnabled]
    T[用户点击内存等级] --> U[切换 memoryLevel 1-5]
    V[用户点击滚到顶部] --> W[setContentOffset 0,0]
    X[用户点击滚到底部] --> Y[setContentOffset 0,MAX]
```

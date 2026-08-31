# Tasks: Demo 工程 GC 配置与测试页面

## 任务列表

### Task 1: 创建 GCManager expect/actual 文件 ✅

**目的**: 封装 Kotlin/Native GC 配置和 suspend/resume 操作，通过 expect/actual 实现跨平台兼容。

#### 1.1 创建 commonMain expect 声明

**文件**: `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`

- 声明 `expect fun configureGC()`
- 声明 `expect fun suspendGC()`
- 声明 `expect fun resumeGC()`

#### 1.2 创建 appleMain actual 实现

**文件**: `demo/src/appleMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`

- `configureGC()`: 设置 GC.regularGCInterval、targetHeapBytes、minHeapBytes、targetHeapUtilization、heapGrowthUseAllocatedBytes
- `suspendGC()`: 带 500ms 最小间隔频率控制 + isGCSuspended 状态追踪
- `resumeGC()`: 仅在已暂停时调用 GC.resume()

#### 1.3 创建 ohosArm64Main actual 实现

**文件**: `demo/src/ohosArm64Main/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`

- 与 appleMain 实现相同

#### 1.4 创建 androidMain actual 空实现

**文件**: `demo/src/androidMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`

#### 1.5 创建 jsMain actual 空实现

**文件**: `demo/src/jsMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`

**验收标准**: 所有平台编译通过，频率控制逻辑正确

---

### Task 2: 使用 @EagerInitialization 集成 configureGC 初始化 ✅

**文件**: `demo/src/appleMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`（及 ohosArm64Main 对应文件）

- 在 appleMain 和 ohosArm64Main 的 GCManager.kt 中，添加 `@EagerInitialization` 标注的顶层属性：
  ```kotlin
  @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
  @EagerInitialization
  private val gcInitializer = configureGC()
  ```
- `@EagerInitialization` 使 Kotlin/Native 在库加载时自动执行 `configureGC()`
- 无需修改 `BasePager.kt`，零侵入
- androidMain / jsMain 无需任何初始化逻辑（空实现即可）

**验收标准**: GC 参数在 Kotlin/Native 库加载时自动配置，无需手动调用，不修改任何现有文件

---

### Task 3: 创建 GCTestPage 测试页面 ✅

**文件**: `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/GCTestPage.kt`

#### 3.1 创建数据模型 GCTestItem

- title、subtitle、description、imageUrl 字段
- memoryChunks: MutableList<ByteArray> 用于模拟内存消耗

#### 3.2 创建页面类

- `@Page("GCTestPage")` 注解，继承 BasePager
- 状态: dataList、gcSuspendEnabled(默认true)、memoryLevel(1-5)、listRef

#### 3.3 实现内存模拟

- `simulateMemoryConsumption(level)`: 每级 10MB，用 1MB ByteArray 块
- 仅分配内存不做 CPU 密集操作

#### 3.4 实现控制面板

- GC Suspend 开关按钮（ON/OFF，绿色/灰色）
- 内存等级按钮（循环 1→5→1，显示对应 MB 数）
- 滚到顶部按钮: setContentOffset(0f, 0f, animated=true)
- 滚到底部按钮: setContentOffset(0f, MAX_VALUE, animated=true)

#### 3.5 实现 List + vforLazy

- dragBegin 事件: gcSuspendEnabled 时调用 suspendGC()
- scrollEnd 事件: gcSuspendEnabled 时调用 resumeGC()
- 每个 item: 左侧 Image(80x80) + 右侧 Title/Subtitle/Description

#### 3.6 实现数据初始化

- created() 中生成 10000 个 GCTestItem
- 使用 picsum.photos 占位图
- 根据 memoryLevel 分配内存

#### 3.7 实现内存等级切换

- 切换时清空并重新生成 dataList

**验收标准**: 页面可正常运行，List 流畅滚动，所有按钮交互正常，内存模拟不导致 CPU 卡顿

---

### Task 4: 平台测试验证 ✅

#### 4.1 iOS 平台测试 ✅
- 验证 GC 配置生效、suspend/resume 正常、滚动流畅
- iOS 模拟器编译通过 (BUILD SUCCEEDED)

#### 4.2 Android 平台测试
- 验证空实现不影响功能、页面交互正常

#### 4.3 HarmonyOS 平台测试（如有环境）
- 验证 GC 配置和 suspend/resume 正常

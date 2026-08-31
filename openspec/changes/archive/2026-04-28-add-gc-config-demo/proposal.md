# Proposal: Demo 工程 GC 配置与测试页面

## 概述

为 KuiklyUI Demo 工程增加 Kotlin/Native GC（垃圾回收）配置能力，包括 `configureGC` 初始化、`GC.suspend()`/`GC.resume()` 的封装，以及一个用于测试 GC 行为对滚动性能影响的复杂列表页面。

## 动机

Kotlin/Native 的 GC 在高频滚动场景下可能导致卡顿，尤其是在内存压力较大时。通过合理配置 GC 参数（如 `targetHeapBytes`、`targetHeapUtilization` 等）以及在滚动期间暂停 GC，可以显著改善滚动流畅度。

本变更旨在：
1. 提供一套 GC 配置的最佳实践，方便业务参考
2. 封装 `GC.suspend()`/`GC.resume()` 并控制调用频率，避免滥用
3. 提供一个可视化的测试页面，用于验证不同 GC 配置和内存压力下的滚动性能表现

## 需求

### 核心功能

1. **configureGC 配置**：在 demo 工程中增加 GC 参数配置，包括：
   - `GC.regularGCInterval = 10.seconds`
   - `GC.targetHeapBytes = 256 * 1024 * 1024`（256MB）
   - `GC.minHeapBytes = 256 * 1024 * 1024`（256MB）
   - `GC.targetHeapUtilization = 0.65`
   - `GC.heapGrowthUseAllocatedBytes = true`
   - 如果编译时发现所使用的 Kotlin 版本不支持特定字段，需注释掉

2. **Kotlin 初始化时执行 configureGC**：使用 `@EagerInitialization` 注解在 Kotlin/Native 库加载时自动调用上述配置

3. **GC.suspend()/GC.resume() 封装**：
   - 提供 `suspendGC()` 和 `resumeGC()` 封装函数
   - 对 `suspend` 的调用需要控制频率（防抖/节流），避免过于频繁的 suspend/resume 切换

4. **复杂测试页面**：
   - 主体为一个 List，每个 item 包含图片和文本等元素
   - 每个 item 创建时模拟消耗大量内存（确保不过于消耗 CPU）
   - List 在 `dragBegin` 时调用 `suspendGC`，在 `scrollEnd` 时调用 `resumeGC`
   - UI 上增加按钮控制是否启用 suspend/resume GC 逻辑
   - UI 上增加按钮控制模拟内存消耗的程度（5 个等级，每级增加 10MB）
   - 页面上增加两个按钮，用于滚到顶部和滚到底部

### 非功能需求

- GC 配置和封装代码放在 demo 模块的 `nativeMain` 或 `appleMain` source set 中（因为 `kotlin.native.runtime.GC` 仅在 Kotlin/Native 平台可用）
- 测试页面放在 `commonMain`，通过 `expect`/`actual` 调用平台特定的 GC 操作
- 内存模拟不应过度消耗 CPU，使用 `ByteArray` 分配即可

## Non-goals

- 不修改 KuiklyUI 框架核心代码（core/compose 模块）
- 不提供生产级别的 GC 管理方案，仅作为 demo 演示和测试
- 不涉及 Android/JVM 的 GC 配置（JVM GC 由 JVM 自身管理）
- 不涉及 Web/JS 平台的 GC 配置

## 影响范围

- **受影响平台**：iOS、HarmonyOS（Kotlin/Native 平台）；Android/Web 仅编译通过（GC 操作为空实现）
- **受影响模块**：demo
- **新增文件**：
  - `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/GCTestPage.kt`（测试页面）
  - `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`（expect 声明）
  - `demo/src/appleMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`（actual 实现 - iOS/macOS）
  - `demo/src/androidMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`（actual 空实现 - Android）
  - `demo/src/jsMain/kotlin/com/tencent/kuikly/demo/pages/base/GCManager.kt`（actual 空实现 - JS）
- **不修改现有文件**：所有变更均为新增文件，使用 `@EagerInitialization` 实现零侵入初始化

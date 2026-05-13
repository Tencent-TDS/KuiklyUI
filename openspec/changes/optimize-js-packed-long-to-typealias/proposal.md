## Why

Compose fork 里 Offset、Size、IntOffset 等 14+ value class 用 `Long` 做 packedValue 字段，JVM/Native 上零分配零开销，但 Kotlin/JS 把 Long 实现为 `{low: Int, high: Int}` 小对象 + 软件模拟 64 位运算。实测（profile）结论：手势、Lazy 滚动、布局 place 等高频路径上 `new Long(...)`、`Long.shr`、`Long.and`、`Long.equals` 密集出现，导致 JS 端 young GC 压力明显且 CPU 占用高。

问题已锁定在 Long 自身，而非 value class 包装。Kotlin 2.1.21 / 2.2.x 都没有多字段 value class，升版本解决不了；JSC 这类引擎对 BigInt 支持弱也不能走 BigInt。唯一可行路径是在 common 层把 packedValue 的 **底层类型本身平台差异化** ——JVM/Native 继续 Long，JS 切 Double（用 Float32Array 视图做 Float↔Double 位转换）。

## What Changes

- 新增 `compose/src/commonMain/.../ui/util/PackedValue.kt`，定义 `expect class PackedFloats` 与 `expect class PackedInts` 两个类型占位。
- 各平台 actual 使用 `actual typealias` 指向原生类型：
  - `androidMain` / `appleMain` / `nativeMain` / `ohosArm64Main`：`typealias PackedFloats = Long`、`typealias PackedInts = Long`
  - `jsMain`：`typealias PackedFloats = Double`、`typealias PackedInts = Int`（或 Double，下一期决定）
- 改造 `InlineClassHelper.kt`：`packFloats` / `unpackFloat1/2` / `packInts` / `unpackInt1/2` 的返回/参数类型从 `Long` 改为新类型；common 层改为 `expect fun`，各平台提供 actual；新增 `expect fun PackedFloats.bitEquals(other)` / `PackedInts.bitEquals(other)` 替换直接的 `==` 哨兵比较（JS 下 NaN 语义要求）。
- 修改以下 14 个 value class 的 `packedValue` 字段类型（Long → PackedFloats / PackedInts），以及内部对 `packedValue == other.packedValue` 这类哨兵比较的写法：
  - Float 打包（8 个）：Offset、Size、CornerRadius、DpOffset、DpSize、Velocity、ScaleFactor、TransformOrigin
  - Int 打包（3 个）：IntOffset、IntSize、TextRange
  - 特殊打包（3 个）：TextUnit（独立 `PackedTextUnit`）、Constraints（**本次不动**，bit 布局复杂收益低）、HitTestResult.DistanceAndInLayer（文件内 private，评估后决定）
- **BREAKING (internal only)**：以上 value class 的 `internal val packedValue` 字段类型从 Long 变为 PackedFloats/PackedInts。由于字段可见性是 `internal` 且消费者都在 compose 模块内部，对外 API 无影响；但 compose 模块内直接引用 `packedValue` 的代码需要跟着改（主要是 `InlineClassHelper` 调用点和 Unspecified 哨兵比较处）。

## Non-goals

- **不改 Constraints**：其 64 位打包编码 4 个布局维度，bit 布局复杂，改造风险高、收益相对低，留到后续评估。
- **不改 core-render-\***：Web 渲染层用独立 Float 参数跨 bridge，与 compose packed 类型零耦合，不需要任何改动。
- **不升级 Kotlin 版本**：方案在 Kotlin 2.1.21 上 POC 已验证通过，不需要升 2.2。
- **不做跨 Compose 上游 rebase 的改造范围扩展**：本次只动 KuiklyUI fork 内的 compose 源码，不尝试向上游 JetBrains Compose 反哺。
- **不引入 BigInt 等现代 JS API**：JSC 兼容性差，不走这条路。

## Capabilities

### New Capabilities

- `compose-packed-value-types`: 定义跨平台的二元打包值类型（PackedFloats、PackedInts、PackedTextUnit）及其 pack/unpack/bitEquals API，描述 JVM/Native/JS 三路 actual 的实现语义与性能约束（JS 端必须零 Long 参与）。

### Modified Capabilities

（无：compose fork 的 Offset/Size/IntOffset 等 value class 目前未建立独立 spec；本次只改它们的内部 packedValue 字段类型，对外公共 API 签名保持不变，因此不视为现有 capability 的 requirement 变更。）

## Impact

### Affected platforms

- **JS（h5App、miniApp）**：直接受益对象。GC 压力下降、Long 软件模拟消除、手势/Lazy/布局热路径 CPU 降低（预期 10-30%，实测为准）。
- **Android / iOS / HarmonyOS / macOS**：actual 走 `typealias = Long`，生成代码与现状**完全等价**，零性能回归风险。

### Affected modules

- `compose/`：本次改动集中在此。
  - `commonMain/ui/util/InlineClassHelper.kt`、新增 `PackedValue.kt`
  - `commonMain/ui/geometry/{Offset,Size,CornerRadius}.kt`
  - `commonMain/ui/unit/{IntOffset,IntSize,Dp,TextUnit,Velocity,TextRange}.kt`
  - `commonMain/ui/layout/ScaleFactor.kt`
  - `commonMain/ui/graphics/TransformOrigin.kt`
  - 各 target source set（androidMain / appleMain / nativeMain / ohosArm64Main / jsMain）新增 actual 实现文件
- `core/`、`core-render-*`、`demo/`：**不涉及**。

### API 兼容性

- 对外公共 API（`Offset(x, y)` 构造、`.x`、`.y`、`+`、`-`、`*`、`copy()`、`Unspecified` 常量等）签名完全不变。
- 内部 `packedValue` 字段类型变化，但字段是 `internal`，外部无感知。
- 二进制兼容性：由于 `value class` 的 JVM erase 依赖 backing field 类型，**改 packedValue 类型会破坏 Compose 模块的二进制兼容**。这是 compose 内部实现细节，kuikly-compose 不是对外提供 ABI 的 library（只作为 kuikly 一部分打包），接受此 breaking 即可。

### 文档

- 本次涉及的都是 Compose fork 内部实现，**不需要更新对外用户文档**（docs/）。
- `.ai/` 知识库的 compose-dsl 相关文档**也无需更新**（消费方看不到 packedValue 类型）。仅本 change 的 spec 作为内部参考。

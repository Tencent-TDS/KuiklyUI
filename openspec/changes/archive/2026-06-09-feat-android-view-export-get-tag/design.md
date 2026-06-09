## Context

### 现状：View tag 的管理方式

在 `core-render-android` 中，每个 Kuikly 跨端 View 都有一个唯一的 `tag`（Int 类型），用于标识该 View。tag 的管理在 `KuiklyRenderLayerHandler` 中：

- `renderViewRegistry: SparseArray<RenderViewHandler>` — 以 tag 为 key 存储 `RenderViewHandler`
- `createRenderViewHandler(tag, viewName)` — 创建 View 时，将 tag 作为 key 存入 registry
- `getRenderViewHandler(tag)` — 通过 tag 查找对应的 `RenderViewHandler`
- `setProp(tag, propKey, propValue)` — 通过 tag 找到 View 并设置属性

`RenderViewHandler` 是一个 data class，包含：
```kotlin
data class RenderViewHandler(
    val viewName: String,
    val viewExport: IKuiklyRenderViewExport
)
```

**问题**：`IKuiklyRenderViewExport` 实现类无法直接获取自己的 tag，因为 tag 只存在于 `KuiklyRenderLayerHandler` 的 `renderViewRegistry` 的 key 中，没有传递给 `IKuiklyRenderViewExport` 本身。

### 为什么需要暴露 tag

业务场景：
- 自定义原生 View 需要在内部逻辑中引用自己的 Kuikly 侧标识，用于与 Kuikly 侧 Module / 其他跨端机制做关联
- 某些原生 SDK 要求传入一个唯一标识，业务希望复用 Kuikly 已有的 View 标识而非自建映射
- 避免业务侧自建"原生 View → Kuikly tag"的映射表，减少出错和内存泄漏风险

## Goals / Non-Goals

### Goals

- 在 `IKuiklyRenderViewExport` 接口中新增 `kuiklyRenderTag: Int` 属性，使得实现类可以获取自身的 Kuikly tag
- 在 View 创建时（`createRenderViewHandler`），自动将 tag 设置到 `IKuiklyRenderViewExport` 上
- 现有实现类无需修改即可继续编译运行（向后兼容）
- 改动局限于 `core-render-android` 模块

### Non-Goals

- **不改 iOS / 鸿蒙渲染层**：本次仅解决 Android 侧需求
- **不引入新的跨平台协议**：tag 是 Android 渲染层内部概念
- **不修改 `KuiklyRenderContext`**：不需要通过 context 查询 tag

### 适用 DSL

本次改动**同时影响自研 DSL（core/）和 Compose DSL（compose/）**，因为 `IKuiklyRenderViewExport` 是两种 DSL 共用的 Android 渲染层接口。但改动仅涉及 `core-render-android` 模块，对 DSL 层无感知。

### NativeBridge 影响

**无**。tag 的设置发生在 Android 原生渲染层内部（`KuiklyRenderLayerHandler`），不涉及跨 NativeBridge 通信。

## Decisions

### D1：在 `IKuiklyRenderViewExport` 中以接口属性 + 默认实现的方式新增 `kuiklyRenderTag`

**选择**：在 `IKuiklyRenderViewExport` 接口中新增：

```kotlin
/**
 * 获取该 View 对应的 Kuikly 跨端渲染层 tag
 *
 * tag 是 Kuikly 渲染层用于标识该 View 的内部引用 ID。
 * 在 View 创建后由 [KuiklyRenderLayerHandler] 自动设置，实现类可直接读取。
 * 默认值为 -1，表示 tag 尚未设置（理论上不会发生在正常流程中）。
 */
val kuiklyRenderTag: Int
    get() = view().getTag(KUIKLY_RENDER_TAG_KEY) as? Int ?: -1
```

并设置方法：

```kotlin
/**
 * 设置该 View 对应的 Kuikly 跨端渲染层 tag
 * 由渲染层在创建 View 时自动调用，实现类一般无需手动调用
 */
fun setKuiklyRenderTag(tag: Int) {
    view().setTag(KUIKLY_RENDER_TAG_KEY, tag)
}
```

**替代方案与 pros/cons**：

| 方案 | 结果 |
|---|---|
| (A) 在接口中新增 `var kuiklyRenderTag: Int` 抽象属性 | 会破坏现有实现类的二进制兼容性，所有实现类都需要实现该属性 |
| (B) **采用的方案**：接口属性 + 默认实现（通过 View.setTag/getTag） | 现有实现类无需修改，向后兼容；使用 Android View 原生的 setTag/getTag 机制，无额外存储开销 |
| (C) 在 `RenderViewHandler` 中存储 tag，提供查询方法 | 需要 `IKuiklyRenderViewExport` 反向查找 `RenderViewHandler`，引入额外复杂度 |
| (D) 通过 `KuiklyRenderContext` 提供 `getTag(view: View): Int` 方法 | 需要遍历 registry 反向查找，O(n) 复杂度，且不直观 |

**Rationale**：方案 B 利用了 Android `View.setTag(key, value)` 机制，这是 Android 原生的扩展属性机制。使用 `setTag/getTag` 有以下优势：
- 零额外存储：tag 直接存在 Android View 对象的 tag 稀疏数组中
- 向后兼容：默认实现意味着现有实现类不受影响
- 简单直观：实现类直接读取 `kuiklyRenderTag` 即可

### D2：在 `KuiklyRenderLayerHandler.createRenderViewHandler()` 中设置 tag

**选择**：在 `createRenderViewHandler(tag, viewName)` 方法中，创建 `RenderViewHandler` 之后立即调用：

```kotlin
renderViewHandler.viewExport.setKuiklyRenderTag(tag)
```

**Rationale**：在 View 创建并注册到 registry 的同时设置 tag，确保 `kuiklyRenderTag` 在 `setProp` 等后续调用中可用。

### D3：使用独立的 key 存储 tag

**选择**：在 `IKuiklyRenderViewExport.kt` 中定义：

```kotlin
private const val KUIKLY_RENDER_TAG_KEY = "kuikly_render_tag_${BuildConfig.APPLICATION_ID ?: "kuikly"}"
```

或使用一个稳定的 int key（通过 `View.generateViewId()` 不适用，因为需要常量）。最终采用 **int key** 方式：

```kotlin
private const val KUIKLY_RENDER_TAG_KEY = 0x1e1c0001  // 预留的 kuikly tag key
```

**Rationale**：使用 int key 比 String key 更高效。在 Android 中，`setTag(int key, Object value)` 使用 SparseArray 存储，key 的高位必须是 `0x1e1c0000`（Android R 起有 `View.setTag(int, Object)` 的限制，但实际上只要不冲突即可）。更简单的做法是使用一个稳定的 int 值，如 `0x2024C001`（kuikly 专属）。

**注意**：经过进一步分析，最安全的方式是使用一个 `AtomicInteger` 生成唯一的 key，或者使用一个公开的常量。考虑到简单性，直接使用一个约定好的 int 常量即可，因为业务一般不会使用 `setTag(int, Object)` 方式。

## File Changes

### `core-render-android/src/main/java/com/tencent/kuikly/core/render/android/export/IKuiklyRenderViewExport.kt`

**修改内容**：
1. 新增 `KUIKLY_RENDER_TAG_KEY` 常量（private）
2. 新增 `kuiklyRenderTag: Int` 属性（带默认实现）
3. 新增 `setKuiklyRenderTag(tag: Int)` 方法（带默认实现）

### `core-render-android/src/main/java/com/tencent/kuikly/core/render/android/layer/KuiklyRenderLayerHandler.kt`

**修改内容**：
1. 在 `createRenderViewHandler(tag, viewName)` 方法中，创建 `RenderViewHandler` 后调用 `renderViewHandler.viewExport.setKuiklyRenderTag(tag)`

## Testing

- 编写单元测试：创建自定义 `IKuiklyRenderViewExport` 实现类，验证 `kuiklyRenderTag` 在 `setProp` 回调中可正确读取
- 手动测试：在 Demo 中创建自定义 View，在 `setProp` 中打印 `kuiklyRenderTag`，确认其值与 Kuikly 侧一致

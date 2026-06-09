## Why

业务在自定义原生 View（`IKuiklyRenderViewExport` 实现类）内部，希望获取该 View 对应的 Kuikly 跨端渲染层 `tag`（即 Kuikly 侧用于标识该 View 的内部引用 ID），以便做某些需要引用标识的逻辑。

当前 `IKuiklyRenderViewExport` 接口未暴露任何 API 可获取 Kuikly 跨端渲染层为该 View 分配的 `tag`。`IKuiklyRenderViewExport` 的设计语义是"原生提供 View，交给 Kuikly 管理"，Kuikly 侧的 tag 属于跨端渲染层内部标识，目前未向原生导出侧暴露。

用户明确说明：**不是用 `getViewWithTag`**（即不是从 Kuikly 侧按 tag 查 View），而是希望 **View 自身能拿到自己的 tag**。

## What Changes

- 在 `IKuiklyRenderViewExport` 接口中新增 `kuiklyRenderTag: Int` 属性（默认值为 `-1`，表示未设置），以及 `setKuiklyRenderTag(tag: Int)` 方法（可选实现）
- 在 `KuiklyRenderLayerHandler.createRenderViewHandler()` 中，创建 View 后将其 tag 设置到 `IKuiklyRenderViewExport` 上
- 实现类可在 `setProp()` 或其他时机通过 `this.kuiklyRenderTag` 获取自身的 Kuikly tag

## Non-goals

- **不改 iOS / 鸿蒙侧**：当前需求来自 Android 业务侧，iOS / 鸿蒙是否需要类似能力待后续补充
- **不改核心 DSL（core/compose）**：本次改动仅涉及 `core-render-android` 渲染层
- **不修改 `KuiklyRenderContext` 或跨平台 bridge**：tag 是 Android 渲染层内部概念，不需要跨平台暴露
- **不提供 `nativeRef` 对象**：Kuikly 在 Android 侧没有独立的 `nativeRef` 对象，tag（Int）即是唯一标识

## Capabilities

### New Capabilities

- `android-view-export-tag`: `IKuiklyRenderViewExport` 实现类可通过 `kuiklyRenderTag` 属性获取自身的 Kuikly 渲染层 tag（Int），用于与 Kuikly 侧 Module / 其他跨端机制做关联。

### Modified Capabilities

（无：仅扩展 `IKuiklyRenderViewExport` 接口，新增可选属性，不影响现有实现类。）

## Impact

### Affected platforms

- **Android**：直接受益对象。新增 `kuiklyRenderTag` 属性，现有实现类不受影响（有默认实现）。

### Affected modules

- `core-render-android`：本次改动集中在此。
  - `export/IKuiklyRenderViewExport.kt`：新增 `kuiklyRenderTag` 属性
  - `layer/KuiklyRenderLayerHandler.kt`：在 `createRenderViewHandler()` 中设置 tag

### API 兼容性

- `kuiklyRenderTag` 以接口属性 + 默认实现的方式提供，`IKuiklyRenderViewExport` 的现有实现类**无需修改**即可继续编译运行
- 需要获取 tag 的实现类只需读取 `kuiklyRenderTag` 属性即可
- 该属性默认值为 `-1`，实现类可在确认 tag 已设置后再使用

### 文档

- `docs/DevGuide/expand-native-ui.md` 中补充关于 `kuiklyRenderTag` 的说明（可选，后续更新）
- `.ai/` 知识库暂不需要更新（属于新增 API，非坑点）

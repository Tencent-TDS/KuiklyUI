## Context

Kuikly Compose 的 `TextField` 映射到 Android 原生 `EditText`（`KRTextFieldView`）。当输入框位于**独立 Window** 中（例如由 `ModalView(inWindow = true)` 承载的 Kuikly `Dialog`，或 demo 中 `MyModalView` 创建的 `Dialog`）时，Android 横屏 IME 全屏编辑模式可能导致首次 `InputMethodManager.showSoftInput(SHOW_IMPLICIT)` 被忽略。设置原生属性 `imeNoFullscreen` 会调整 `IME_FLAG_NO_FULLSCREEN` 并触发 `InputMethodManager.restartInput`，从而重建输入连接，使软键盘能够正常弹出。

本变更仅涉及 **Compose DSL** 的 demo 与文档说明；**自研 DSL** 侧已在 Input 组件文档中提供 `imeNoFullscreen` 能力说明。

## Goals / Non-goals

**Goals**

- 提供最小复现页 `@Page("ComposeLandscapeKeyboardReproDemo")`，供 QA 与回归。
- 支持 adb 可选强制横屏，无需依赖人工旋转设备。
- 在用户常查阅的位置写明规避方式：Compose TextField 差异化、`modifier` 索引、Input 组件 `imeNoFullscreen` 文档。

**Non-goals**

- 修改 `compose/`、`core/`、`core-render-android/` 等框架实现代码。
- 在本变更内订正 `docs/API/components/text-area.md` 与 `imeNoFullscreen` 表述是否一致（可作为后续独立变更）。

## Decisions

### Decision: 在 demo + 文档层解决，不修改框架默认值

**理由**：若在所有 Compose 文本输入上默认开启 `imeNoFullscreen`，会改变横屏 IME 的全局体验，属于**破坏性**行为。业务仅在「独立 Window 浮层 + 横屏」等必要场景对 `TextField` 设置 `Modifier.setProp("imeNoFullscreen", true)`。

### Decision: `KuiklyRenderActivity` 可选 `forceLandscape`

**理由**：便于 CI 与 adb 稳定复现横屏问题；未传该 extra 的宿主行为不变。

### Decision: 文档交叉链接采用文件级路径，不用易碎锚点

**理由**：中文标题在 VuePress 等站点生成的 slug 不稳定；`modifier.md` 对 `core-components.md` 使用无 `#` 的文件级引用。

## File changes (by module)

| Module | Path |
|--------|------|
| demo | `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ComposeLandscapeKeyboardReproDemo.kt` |
| androidApp | `androidApp/src/main/java/com/tencent/kuikly/android/demo/KuiklyRenderActivity.kt` |
| docs | `docs/Compose/core-components.md`, `docs/Compose/modifier.md`, `docs/API/components/input.md` |

## NativeBridge

不适用 — 无跨端 Bridge 协议变更。

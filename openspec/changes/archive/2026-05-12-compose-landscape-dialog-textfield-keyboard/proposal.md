## Why

在 Android 横屏下，Compose DSL 的 `TextField` 若位于独立 `Window` 的 `Dialog` 浮层中，通过 `FocusRequester.requestFocus()` 等方式程序化请求焦点时，可能出现焦点回调已触发但**软键盘仍不弹出**的现象。根因与 Android IME 横屏全屏编辑（fullscreen extract）及独立 Window 下首次 `showSoftInput(SHOW_IMPLICIT)` 被忽略有关；规避方式是在原生输入控件上设置 `imeNoFullscreen`，Compose 侧通过 `Modifier.setProp("imeNoFullscreen", true)` 透传。

主仓库需提供**最小复现 Demo**、**便于 adb 稳定横屏启动**的方式，以及**官网文档**，避免业务重复踩坑与排查。

## What Changes

- **demo/**：新增 `ComposeLandscapeKeyboardReproDemo` — 按钮打开 `Dialog`，内含 `TextField`，`LaunchedEffect` 中调用 `FocusRequester.requestFocus()`，并在 `TextField` 上使用 `Modifier.setProp("imeNoFullscreen", true)`。
- **androidApp/**：`KuiklyRenderActivity` 支持可选 Intent 布尔参数 `forceLandscape`，便于 adb 横屏复现（`am start ... --ez forceLandscape true`）。
- **docs/**：
  - `docs/Compose/core-components.md` — 新增 TextField 差异化说明（#8），并修正 `ModalNavigationDrawer` 小节标题重复编号（改为 #9）。
  - `docs/Compose/modifier.md` — 在 `focusRequester()` 条目中增加指向 TextField 差异化说明的交叉引用。
  - `docs/API/components/input.md` — 扩充 `imeNoFullscreen`：独立 Window 浮层 + 横屏场景下的使用建议。

## Capabilities

### New Capabilities

- `compose-landscape-dialog-keyboard-docs`：针对 Android 横屏 + `Dialog` + Compose `TextField` 软键盘问题的**文档化规避路径**与**复现入口**说明。

### Modified Capabilities

- （无 — 未改动 `core/`、`compose/` 等框架模块行为或对外 API 签名。）

## Impact

- **模块**：仅 `demo/`、`androidApp/`、`docs/`。
- **平台**：Android（行为说明与复现辅助）；其余平台无行为变更。
- **API 变更**：无（`forceLandscape` 为调试/复现用可选参数；文档仅描述既有 `setProp` / 原生属性）。
- **破坏性变更**：无。

## Non-goals

- 修改 `CoreTextField` 或 `KRTextFieldView` 的默认行为（不在框架层默认开启 `imeNoFullscreen`）。
- 新增 `TextAreaAttr.imeNoFullscreen` 等 KMP 层 API（除非另立变更单独提案）。
- 在 `ComposeAllSample` 中注册复现页（本变更外；通过 `pageName` 或路由打开）。
- 修改 iOS / HarmonyOS / Web / 小程序 渲染层或 IME 行为。

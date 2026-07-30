## ADDED Requirements

### Requirement: Compose 横屏 Dialog TextField 软键盘复现演示

仓库 SHALL 提供 Compose DSL 演示页：打开含 `TextField` 的 `Dialog`，程序化请求焦点，并应用文档所述规避方式，使 Android 横屏下软键盘能够正常出现。

#### Scenario: Android — 打开浮层后键盘可弹出

- **GIVEN** 应用在 Android 真机或模拟器上处于横屏，或通过 `forceLandscape=true` 启动后处于横屏
- **WHEN** 用户进入 `ComposeLandscapeKeyboardReproDemo`，点击按钮打开对话框浮层，且 `TextField` 获得程序化焦点
- **THEN** 软键盘 SHALL 可见（或 SHALL 在不依赖用户二次点击的前提下可被唤起；不因缺少 `imeNoFullscreen` 而单独导致首次无法弹出）

#### Scenario: iOS — demo 与文档变更无回归

- **GIVEN** 变更集仅增加 Kotlin common 的 demo 与文档
- **WHEN** 使用更新后的 `demo` 源码构建 iOS App
- **THEN** 既有 Compose 页面 SHALL 能编译运行，且不引入对 Android-only Intent extra 的运行时依赖

#### Scenario: HarmonyOS — demo 与文档变更无回归

- **GIVEN** 变更集未修改 `core-render-ohos` 或鸿蒙专有源码
- **WHEN** 构建鸿蒙 demo
- **THEN** 构建 SHALL 不因本变更而要求新增 native 符号

#### Scenario: Web — demo 与文档变更无回归

- **GIVEN** 变更集未修改 `core-render-web` 或 JS 侧键盘相关 API
- **WHEN** 构建 H5 / 小程序 demo
- **THEN** Web 侧键盘行为 SHALL 不因本变更而改变

### Requirement: KuiklyRenderActivity 可选 adb 横屏 Intent 参数

在 Android 上，`KuiklyRenderActivity` SHALL 读取可选布尔 Intent extra `forceLandscape`；当为 `true` 时，SHALL 请求横屏方向，以稳定复现仅横屏出现的问题。

#### Scenario: Android — 传入横屏 extra 时生效

- **GIVEN** 使用 `--ez forceLandscape true` 启动 `KuiklyRenderActivity`
- **WHEN** `onCreate` 执行
- **THEN** `requestedOrientation` SHALL 设为 `SCREEN_ORIENTATION_USER_LANDSCAPE`（或等价的 user-landscape 常量）

#### Scenario: Android — 默认启动行为不变

- **GIVEN** 启动 `KuiklyRenderActivity` 时未传入 `forceLandscape`
- **WHEN** `onCreate` 执行
- **THEN** 活动 SHALL 不因本变更而单独强制改写屏幕方向策略

### Requirement: 官网文档说明规避方式与交叉引用

文档 SHALL 说明：Android 横屏且输入框位于独立 Window 浮层时，Compose `TextField` 的首次软键盘展示可能被阻断；SHALL 写明使用 `Modifier.setProp("imeNoFullscreen", true)`，并 SHALL 为自研 DSL 读者关联 Input 的 `imeNoFullscreen` 说明。

#### Scenario: Compose 文档 — TextField 差异化已收录

- **WHEN** 读者打开 `docs/Compose/core-components.md`
- **THEN** TextField 差异化章节 SHALL 包含一条说明：Android 横屏 + 独立 Window 浮层下的键盘行为及 `setProp` 规避方式

#### Scenario: Compose 文档 — focusRequester 索引指向 TextField 差异化

- **WHEN** 读者打开 `docs/Compose/modifier.md` 的焦点相关小节
- **THEN** `focusRequester()` 条目 SHALL 引用 TextField 差异化文档中已知的 Android 浮层场景说明

#### Scenario: API 文档 — imeNoFullscreen 说明浮层与横屏

- **WHEN** 读者打开 `docs/API/components/input.md` 中的 `imeNoFullscreen` 小节
- **THEN** 该小节 SHALL 说明在浮层 + 横屏场景下宜设置 `imeNoFullscreen(true)`，并 SHALL 提及 Compose 使用 `Modifier.setProp("imeNoFullscreen", true)`

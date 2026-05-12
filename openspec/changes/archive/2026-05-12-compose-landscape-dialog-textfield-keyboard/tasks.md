## 1. Demo（`demo/`）

- [x] 1.1 新增 `ComposeLandscapeKeyboardReproDemo.kt`：含 `Dialog`、`TextField`、`FocusRequester`、`LaunchedEffect`，以及 `Modifier.setProp("imeNoFullscreen", true)`。

## 2. Android 宿主（`androidApp/`）

- [x] 2.1 在 `KuiklyRenderActivity.onCreate` 中读取布尔 extra `forceLandscape`；为 `true` 时将 `requestedOrientation` 设为 `SCREEN_ORIENTATION_USER_LANDSCAPE`。

## 3. 文档（`docs/`）

- [x] 3.1 `docs/Compose/core-components.md` — 补充 TextField 差异化条目 #8（Android 横屏 + 独立 Window 浮层）；将重复的 `ModalNavigationDrawer` 小节标题由 `#7` 改为 `#9`。
- [x] 3.2 `docs/Compose/modifier.md` — 在 `focusRequester()` 条目中增加指向 TextField 差异化说明的引用。
- [x] 3.3 `docs/API/components/input.md` — 扩充 `imeNoFullscreen`：独立 Window 浮层 + 横屏场景说明，并注明 Compose 使用 `Modifier.setProp("imeNoFullscreen", true)`。

## 4. 验证

- [x] 4.1 Android：`./gradlew :androidApp:assembleDebug` 编译通过。
- [x] 4.2 Android：`adb install -r` 后 `am start` 指定 `pageName=ComposeLandscapeKeyboardReproDemo` 且 `forceLandscape=true`；打开浮层后确认软键盘可弹出。

## 5. 明确排除范围

- [x] 5.1 不在 `ComposeAllSample` 中增加列表入口（通过 `pageName` / 深链打开复现页）。

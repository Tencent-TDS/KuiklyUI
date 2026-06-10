## 1. Core event contract

- [x] 1.1 在 `core/.../Pager.kt` 中新增 page-level IME 事件常量与 payload key 约定
- [x] 1.2 确认新增 IME 事件沿用现有 pager event 分发链路，不影响既有 `root/window/configuration` 事件处理

## 2. Compose IME state and API

- [x] 2.1 在 `compose/.../platform/LocalConfiguration.kt` 中新增 IME 状态字段与 `onImeInsetsChanged()` 更新入口
- [x] 2.2 在 `compose/.../ComposeContainer.kt` 中接收新的 IME pager event 并更新页面级 IME 状态
- [x] 2.3 在 `compose/.../foundation/layout/WindowInsets.kt` 中新增 `WindowInsets.ime`
- [x] 2.4 在 `compose/.../foundation/layout/WindowInsetsPadding.kt` 中新增 `Modifier.imePadding()` 并复用现有 inset 消费语义
- [x] 2.5 在 `compose/.../material3/Scaffold.kt` 中将默认 `contentWindowInsets` 调整为系统栏与 IME 的组合策略

## 3. Renderer bridge implementation

- [x] 3.1 在 `core-render-android/` 中复用 `KRKeyboardModule`，补齐页面级键盘 listener 与 IME pager event 发送
- [x] 3.2 在 `core-render-ios/` 的页面宿主层补齐键盘通知 observer，并发送 IME pager event
- [x] 3.3 在 `core-render-ohos/` 中把现有窗口级 `keyboardHeightChange` 链路接到 IME pager event 发送
- [x] 3.4 验证三端 renderer 的 listener 生命周期与释放逻辑，避免重复通知或泄漏

## 4. Demo coverage

- [x] 4.1 新增或更新 Compose demo，覆盖底部输入栏使用 `Modifier.imePadding()` 的场景
- [x] 4.2 新增或更新 Compose demo，覆盖 `Scaffold` 默认内容 inset 规避键盘的普通表单场景
- [x] 4.3 验证旧有 `keyboardHeightChange` demo/业务写法未被新能力破坏

## 5. Platform verification

- [ ] 5.1 在 Android 上验证 `WindowInsets.ime`、`imePadding()`、`Scaffold` 默认避让与旧 callback 兼容性
- [ ] 5.2 在 iOS 上验证 `WindowInsets.ime`、`imePadding()`、`Scaffold` 默认避让与旧 callback 兼容性
- [ ] 5.3 在 HarmonyOS 上验证 `WindowInsets.ime`、`imePadding()`、`Scaffold` 默认避让与旧 callback 兼容性

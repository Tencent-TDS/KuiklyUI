## 1. OpenSpec 范围收口

- [x] 1.1 确认 `compose-ime-return-to-phase1` 与 `compose-ime-insets` 的 phase1 边界完全一致
- [x] 1.2 明确与 `compose-ime-animation-polish`、`compose-ime-linear-animation-mode`、`native-ime-animation-progress`、`ios-ime-sync-send-event`、`compose-ime-sync-curve-animation` 的 superseded / withdrawn 处理策略

## 2. 协议与 Compose 状态回滚

- [x] 2.1 在 `core/.../Pager.kt` 中移除 `IME_SOURCE`、`IME_ANIMATED_HEIGHT` 及相关 source 常量
- [x] 2.2 在 `compose/.../ComposeContainer.kt` 中回退到只解析 phase1 payload：`height / duration / curve`
- [x] 2.3 在 `compose/.../platform/LocalConfiguration.kt` 中移除 target / projected / source 相关状态与辅助方法
- [x] 2.4 在 `compose/.../foundation/layout/WindowInsets.kt` 中移除内部 IME 补动画与 `native-progress` 优先消费
- [x] 2.5 确认 `Modifier.imePadding()` 与 `Scaffold` 默认内容避让保持 phase1 行为不变

## 3. 平台侧回滚

- [x] 3.1 在 Android 宿主层移除 `imeInsetsDidChanged` 的 `source` 透传与 IME sync 特例
- [x] 3.2 在 iOS renderer 中移除 `native-progress`、DisplayLink、proxyView、`animatedHeight/source` 发送与 IME sync 特例依赖
- [x] 3.3 在 HarmonyOS renderer 中移除 `source` 透传与 IME sync 特例
- [x] 3.4 在 `iosApp/` 与 `ohosApp/` demo host 中回退 `imeInsetsDidChanged` 的同步发送声明
- [x] 3.5 确认三端 page-level keyboard height bridge 仍然保留且生命周期正确

## 4. Demo 与文档回滚

- [x] 4.1 回退 `KeyboardHeightDemo` 到 phase1 验证文案与展示字段
- [x] 4.2 回退 `ScaffoldDemo` 到 phase1 验证文案与展示字段
- [x] 4.3 移除 demo 中 `sync curve`、`native-progress`、`phase3 提前映射` 等说明

## 5. 验证与回归

- [x] 5.1 在 Android 上验证 `WindowInsets.ime`、`imePadding()`、`Scaffold` 默认避让与旧 callback 兼容性
- [x] 5.2 在 iOS 上验证 `WindowInsets.ime`、`imePadding()`、`Scaffold` 默认避让与旧 callback 兼容性
- [x] 5.3 在 HarmonyOS 上验证 `WindowInsets.ime`、`imePadding()`、`Scaffold` 默认避让与旧 callback 兼容性
- [x] 5.4 确认 Compose 路径中不再依赖 `source`、`animatedHeight`、`sync-send`、`native-progress` 才能成立 phase1 行为

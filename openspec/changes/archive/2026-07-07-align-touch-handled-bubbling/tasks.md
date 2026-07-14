## 1. core-render-ios

- [x] 1.1 收敛 `KRView.m` 中 `touchesBegan` / `touchesEnded` / `touchesMoved` / `touchesCancelled` 的判断为仅在未 `handled` 时才调用 `super`
- [x] 1.2 同步 macOS 编译分支中的 `touchesBeganWithEvent` / `touchesEndedWithEvent` / `touchesMovedWithEvent` / `touchesCancelledWithEvent`，保持与 iOS 相同的 handled-first 语义
- [x] 1.3 复核 `stop-propagation` 映射与复用重置逻辑，确保属性入口保留但不再参与 raw touch 默认上抛判定

## 2. core-render-ohos

- [x] 2.1 调整 `KRView.cpp` 普通 raw touch 分支，使 `TryFireOnTouch*Event` 返回 `handled=true` 后立即阻止继续向父层传播
- [x] 2.2 调整 `super_touch_type_ == SELF / PARENT / NONE` 三条分支，确保 super touch 协作路径也遵守 handled-first 语义
- [x] 2.3 复核 `stop_propagation_` 的保留代码与复位路径，避免旧判断残留继续影响 raw touch 默认消费逻辑

## 3. demo 与验证

- [x] 3.1 更新触摸验证 demo，覆盖“子节点未处理则继续上抛、子节点已处理则父节点不再收到”的对照场景
- [x] 3.2 在 iOS / macOS 上验证父子节点 `touchDown` / `touchMove` / `touchUp` / `touchCancel` 的日志顺序与消费结果
- [x] 3.3 在 HarmonyOS 上验证普通 touch 与 `superTouch` 嵌套场景下的 handled-first 行为
- [x] 3.4 在 Android 上执行未受影响场景的回归烟测，确认本次 renderer 调整未引入跨端行为说明偏差

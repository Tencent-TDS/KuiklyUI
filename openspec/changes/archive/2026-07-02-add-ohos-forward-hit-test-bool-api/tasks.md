## 1. core 公共 API

- [x] 1.1 在 `IStyleAttr` / `Attr` 中新增 HarmonyOS 专属 `ohosViewHitTestMode(mode: String)` 公共方法，并通过 `setProp("hit-test-ohos", mode)` 下发
- [x] 1.2 新增 `default` / `block` / `transparent` / `none` 四个公开字符串常量，避免业务手写 native 字符串
- [x] 1.3 清理上一版 `forwardTransparentOhos(Boolean)` 方案遗留，确保公共 API 口径唯一

## 2. core-render-ohos 命中行为对齐

- [x] 2.1 保持 `KRView` 现有 `hit-test-ohos` 行为不变，并确保 `ohosViewHitTestMode(...)` 能直接走到该链路
- [x] 2.2 在 `KRForwardArkTSView::SetProp` 中补齐 `hit-test-ohos` 公开模式映射，命中后在 C++ wrapper 层消费且不再透传 ArkTS
- [x] 2.3 为 `KRForwardArkTSView` 增加 reset 回退逻辑，移除属性时恢复默认 `TRANSPARENT`

## 3. 文档与示例清理

- [x] 3.1 删除上一版 Boolean API 的文档描述，统一更新为 `ohosViewHitTestMode + 常量` 方案
- [x] 3.2 补充 HarmonyOS only 文档，说明四个公开常量值及普通 view / `forwardview v1` 的默认行为差异

## 4. 平台验证

- [x] 4.1 在 HarmonyOS 普通 `KRView` 链路验证四个公开常量值的命中行为
- [x] 4.2 在 HarmonyOS `forwardview v1` 链路验证四个公开常量值，以及未设置/重置后的默认 `TRANSPARENT`
- [x] 4.3 验证 Android、iOS、Web、miniApp 使用 `ohosViewHitTestMode(...)` 时保持现有行为且不会报错

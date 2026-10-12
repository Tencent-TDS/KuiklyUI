## Why

HarmonyOS 原生 `KRView` 已经支持 `hit-test-ohos` 属性，但自研 DSL 侧没有正式公开 API 和常量封装，业务只能手写 `setProp("hit-test-ohos", "...")`。同时 `forwardview v1` 没有消费这条属性链路，导致普通 view 与转发 ArkTS view 的命中能力不一致，因此需要把这项能力正式开放，并补齐 `forwardview v1` 支持。

## What Changes

- 在 `core` 公共 `Attr` / `IStyleAttr` 中开放 HarmonyOS 专属 API：`ohosViewHitTestMode(mode: String)`
- 该 API 底层统一通过 `setProp("hit-test-ohos", mode)` 下发，复用现有 native prop key
- 为 `default`、`block`、`transparent`、`none` 四个可选值提供公开字符串常量，避免业务手写字符串
- 让 `core-render-ohos` 的 `KRForwardArkTSView` 补齐对 `hit-test-ohos` 的消费能力，并与 `KRView` 使用同一套取值
- 保持现有默认行为兼容：普通 `KRView` 继续保持当前默认行为，`forwardview v1` 未设置该属性时仍保持 `TRANSPARENT`
- 移除上一版 `forwardTransparentOhos(Boolean)` 方案及其相关设计、文档和实现痕迹
- 更新文档，明确该 API 为 HarmonyOS only，且 `forwardview v1` 与普通 view 的默认值差异

## Non-goals

- 不修改 `forwardview v2` 的事件或属性透传语义
- 不统一 Android、iOS、Web、小程序与 HarmonyOS 的 hit-test 行为
- 不改变 `KRView` 已有 `hit-test-ohos` 底层取值语义
- 不要求 ArkTS 业务组件直接理解或处理该属性

## Capabilities

### New Capabilities
- `ohos-hit-test-api`: 为 HarmonyOS 自研 DSL 公开 `ohosViewHitTestMode` API 与常量值，并定义 `KRView` / `forwardview v1` 的支持与默认行为

### Modified Capabilities
- None

## Impact

- **Affected platforms**: HarmonyOS（实现生效），Android / iOS / Web / miniApp（忽略该属性，无行为变化）
- **Affected modules**: `core/`、`core-render-ohos/`、`docs/`，必要时补充 `demo/` 示例或验证页
- **Affected APIs**: `IStyleAttr` / `Attr` 新增 `ohosViewHitTestMode(mode: String)` 与公开字符串常量值
- **Systems**: HarmonyOS native render 的 `KRView` / `KRForwardArkTSView` 命中策略与属性分发链路

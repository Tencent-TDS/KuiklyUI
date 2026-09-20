## Why

iOS 侧动态字体加载存在核心缺陷：`fontFamily` 必须与字体文件内嵌的 PostScript Name 精确一致，而 Android/鸿蒙天然支持别名映射。业务反馈 iOS 字体适配体验不一致，且 `KuiklyFontProtocol` 原有 `hr_loadCustomFont:` 方法仅返回注册成功/失败，框架无法获取真实 PostScript Name，导致字体加载链路不完整。

## What Changes

- **`KuiklyFontProtocol` 新增协议方法**：`hr_fontWithFontFamily:fontSize:fontWeight:contextParams:`，允许业务方根据 `fontFamily` 直接返回 `UIFont`，框架不再二次猜测
- **`KRFontModule` 新增转发方法**：同名类方法，向下游 `gFontHandler` 转发
- **`KRConvertUtil +UIFont:` 优先级调整**：在静态注册检查后、文件注册前插入适配器调用
- **`KuiklyRenderLayerHandler` contextParam 补齐**：普通 LayerHandler 的 `p_createShadowHandlerWithTag:` 中注入 `contextParam`，修复非 TurboDisplay 链路动态字体不可用的问题
- 更新 Demo 示例：`KRFontHanlder.m` 实现新协议方法并展示 PostScript Name 自动提取能力

## Capabilities

### New Capabilities
- `font-mapping-adapter`: iOS 字体映射适配器协议及实现，定义 `fontFamily → UIFont` 的映射契约
- `font-postscript-extractor`: 字体文件 PostScript Name 自动提取能力（基于 `CGFontCopyPostScriptName`）

### Modified Capabilities
- 无（新增协议方法，不破坏现有 spec）

## Non-goals

- 不涉及字体文件下载/网络加载的异步方案
- 不涉及 Android/鸿蒙侧的字体映射改造（仅 iOS 侧补齐）
- 不改变 TurboDisplay 链路的字体加载行为（已正确注入 contextParam）

## Impact

| 维度 | 影响 |
|------|------|
| **平台** | iOS 专属改动 |
| **模块** | `core-render-ios/Extension/Modules/KRFontModule.h/m`、`core-render-ios/Extension/Category/KRConvertUtil.m`、`core-render-ios/Handler/KuiklyRenderLayerHandler.mm`、`iosApp/.../KRFontHanlder.m` |
| **API 变更** | `KuiklyFontProtocol` 新增 `@optional` 方法，向后兼容 |
| **兼容性** | 已有 `hr_fontWithFontFamily:` 回退路径（`componentExpandHandler`）作为兜底，无 breaking change |

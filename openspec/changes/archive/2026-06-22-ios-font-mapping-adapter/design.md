## Context

当前 iOS 字体加载链路：

```
业务设置 fontFamily="CustomSans"（别名）
  → [UIFont fontWithName:@"CustomSans" size:16] → nil（真实名是 "CustomSans-Regular"）
  → hr_loadCustomFont: 注册字体文件 ✅
  → [UIFont fontWithName:@"CustomSans" size:16] → nil（仍用别名取，取不到）
  → 返回系统默认字体 ❌
```

核心问题是 `hr_loadCustomFont:` 返回 `BOOL`，框架不知道真实 PostScript Name。同时普通 LayerHandler（非 TurboDisplay）的 shadow 创建时缺 `contextParam`，动态字体资源路径拼不出来。

本设计在 `KuiklyFontProtocol` 中新增一个「fontFamily → UIFont」的映射方法，让业务方自主完成从 fontFamily 到 UIFont 的转换，框架不再做注册后的二次猜测。

影响范围：iOS render 层，涉及 4 个文件（KRFontModule.h/m、KRConvertUtil.m、KuiklyRenderLayerHandler.mm），以及 Demo 的 KRFontHanlder.m。

## 涉及 DSL

本变更为 iOS Native 层改动，不涉及 Kotlin DSL 层（自研 DSL 和 Compose DSL 均不感知）。

## 文件变更分组

| 模块 | 文件 | 变更类型 |
|------|------|----------|
| `core-render-ios/Extension/Modules` | `KRFontModule.h` | 协议扩展 + 类方法新增 |
| `core-render-ios/Extension/Modules` | `KRFontModule.m` | 转发方法实现 |
| `core-render-ios/Extension/Category` | `KRConvertUtil.m` | 调用优先级调整 |
| `core-render-ios/Handler` | `KuiklyRenderLayerHandler.mm` | contextParam 注入补齐 |
| `iosApp/...` | `KRFontHanlder.m` | Demo 实现更新 |

## Decisions

### Decision 1：新增协议方法 vs 改造现有方法

| 方案 | 评价 |
|------|------|
| 改造 `hr_loadCustomFont:` 返回 NSString * | ❌ 破坏已有协议，所有业务方需同步修改 |
| 新增 `hr_fontWithFontFamily:` 方法 | ✅ `@optional`，向后兼容 |

**结论**：新增 `@optional` 方法。

### Decision 2：优先级顺序

```
静态注册 → 新适配器(直接返回UIFont) → hr_loadCustomFont(文件注册) → componentExpandHandler → 系统默认
```

新适配器排在「文件注册」之前，因为业务方可以直接在新方法中完成注册 + 返回，更高效。只有新方法返回 nil 时才走旧的文件注册 + 二次查找路径。

### Decision 3：PostScript Name 提取时机

用 `CGFontCopyPostScriptName` 在注册前从字体二进制提取真实名。这一步在业务方 `hr_fontWithFontFamily:` 实现内部完成，框架层的 Demo 示例 `KRFontHanlder` 展示如何做。

### Decision 4：contextParam 注入方式

在 `p_createShadowHandlerWithTag:` 中直接调用 `[shadow hrv_setPropWithKey:@"contextParam" propValue:_contextParam]`，不走有主线程断言的 `setContextParamToShadow:`，因为 shadow 创建在 context 线程执行。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 业务方新方法未实现，走旧路径 | `@optional`，未实现时表现与现状一致 |
| `CGFontCopyPostScriptName` 返回 nil（罕见畸形字体文件）| fallback 到 `[UIFont fontWithName:fontFamily size:fontSize]` 尝试 |
| 新方法被滥用（每次调用都重新注册字体）| Demo 示例展示缓存判断逻辑 |

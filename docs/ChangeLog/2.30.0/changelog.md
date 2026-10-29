# 2.30.0 变更说明

## Part 1 总体说明

2.30.0 在网络能力、iOS 组件与体验、鸿蒙能力与性能以及渲染稳定性四个方面做了增强：新增流式网络请求、补齐 iOS 导航栏与工具栏等原生组件、丰富鸿蒙文本选择与返回顶部能力，并修复了富文本、嵌套滚动与 Compose 性能问题。具体如下：

- **网络模块流式请求**：Network 模块新增 `httpStreamRequest` 流式 HTTP 文本接口，响应体按网络分片逐块回调，无需等待完整响应，适用于大模型流式输出与实时数据推送，并支持通过返回句柄主动关闭连接。
- **iOS 组件与体验**：新增 UINavigationBar、UIToolbar 原生组件与 Scroller/List 的 iOS 26 滚动边缘效果属性；修复字体解析回退时字重与斜体丢失，并新增字体别名适配器供业务注册自定义字体。
- **鸿蒙能力与性能**：文本选择支持全部类型，新增自定义状态栏返回顶部；utf 转码改用 simdutf 优化性能，并新增 KRJSON 的 LLDB 调试格式化器。
- **渲染修复与性能**：修复富文本 span 背景色在 Android、iOS 与鸿蒙的渲染，以及 Android 与 iOS 的多级嵌套滚动穿透问题；优化 Compose 动画与布局中的装箱开销。

历史提交可见 [2.29.2...2.30.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.29.2...2.30.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### iOS

- feat(scroller): add iOS 26 scroll edge effect attrs on Scroller/List · [#1823](https://github.com/Tencent-TDS/KuiklyUI/pull/1823)
- fix(ios): keep fontWeight and italic on font resolution fallback and add font family alias adapter · [#1796](https://github.com/Tencent-TDS/KuiklyUI/pull/1796)
- feat(ios): add UINavigationBar and UIToolbar Kuikly components · [#1822](https://github.com/Tencent-TDS/KuiklyUI/pull/1822)

### 鸿蒙

- perf(ohos): optimize utf transcoding with simdutf · [#1806](https://github.com/Tencent-TDS/KuiklyUI/pull/1806)
- feat(ohos): add an LLDB formatter for KRJSON words · [#1818](https://github.com/Tencent-TDS/KuiklyUI/pull/1818)
- feat(ohos): support all text selection types · [#1815](https://github.com/Tencent-TDS/KuiklyUI/pull/1815)
- feat(ohos): implement custom status-bar back-to-top · [#1769](https://github.com/Tencent-TDS/KuiklyUI/pull/1769)

### Compose

- perf(compose): avoid Long allocations on animation and layout · [#1808](https://github.com/Tencent-TDS/KuiklyUI/pull/1808)

### 多端

- feat: Add HTTP/SSE streaming request support in Network module · [#1639](https://github.com/Tencent-TDS/KuiklyUI/pull/1639)
- fix: resolve multi-level nested scroll penetration on Android and iOS · [#1810](https://github.com/Tencent-TDS/KuiklyUI/pull/1810)
- fix: render rich text span backgroundColor on Android, iOS and OHOS · [#1774](https://github.com/Tencent-TDS/KuiklyUI/pull/1774)

### 其他

- docs: add 2.29.0 version description to changelog · [#1814](https://github.com/Tencent-TDS/KuiklyUI/pull/1814)

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

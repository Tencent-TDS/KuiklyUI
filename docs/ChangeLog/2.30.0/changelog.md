# 2.30.0 变更说明

## Part 1 总体说明

2.30.0 在网络能力、iOS 原生组件、鸿蒙交互与渲染修复四方面做了增强：新增 HTTP/SSE 流式请求与 iOS 系统导航栏/工具栏组件，补齐鸿蒙文本选择与状态栏回顶交互，并修复嵌套滚动穿透、富文本背景色与字体回退等问题。具体如下：

- **网络流式请求**：Network 模块新增 HTTP/SSE 流式请求支持，Android、iOS、鸿蒙三端都能读取流式响应，适配流式输出的实时场景。
- **iOS 原生组件与视觉细节**：新增 UINavigationBar、UIToolbar 原生组件；Scroller / List 支持 iOS 26 滚动边缘效果属性；字体解析回退时保留 fontWeight 与 italic，并新增字体族别名适配器。
- **鸿蒙文本与交互增强**：富文本支持全部文本选择类型，Scroller 支持点击状态栏回顶，UTF 转码性能得到优化，并新增 KRJSON 的 LLDB 调试格式化器。
- **滚动与渲染修复**：修复 Android、iOS 的多级嵌套滚动穿透，修复 Android、iOS、鸿蒙的富文本 span 背景色渲染；Compose 动画与布局减少 Long 装箱以降低分配开销。

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

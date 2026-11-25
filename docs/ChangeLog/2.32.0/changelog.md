# 2.32.0 变更说明

## Part 1 总体说明

2.32.0 在鸿蒙、输入与触摸、Compose 绘制、iOS 手势与滚动四个方面做了增强：鸿蒙补齐 kotlin 2.4.21 构建并修复多项渲染问题，键盘映射与多点触控更完整，Compose 绘图细节更可控，iOS 点击与滚动更贴合原生。具体如下：

- **鸿蒙工程与渲染**：新增 kotlin 2.4.21 构建支持；修复快照描述符共享所有权、HTTP 空响应保护、富文本绘制时排版错乱，以及粘贴按 UTF-8 边界截断导致的渲染异常。
- **输入与触摸交互**：扩展 Input/TextArea 与 Compose 的 keyboardType 映射，让 Android、iOS、鸿蒙输入体验保持一致；多点触控保留按压状态，复杂手势下按压态不再丢失。
- **Compose 绘制能力**：PathEffect 虚线下沉到 canvas 的 lineDash 命令，绘制更高效；修饰符新增 turboDisplayAutoUpdateEnable 属性支持。
- **iOS 手势与滚动**：tap 手势改用 TouchUpInside 语义，点击判定更准确；支持与宿主的联合滚动。

历史提交可见 [2.31.0...2.32.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.31.0...2.32.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### iOS

- fix(ios): use KRTapGestureRecognizer with TouchUpInside semantics · [#1893](https://github.com/Tencent-TDS/KuiklyUI/pull/1893)
- 处理与宿主的联合滚动 · [#1892](https://github.com/Tencent-TDS/KuiklyUI/pull/1892)

### 鸿蒙

- fix(ohos): manage snapshot drawable descriptors with shared ownership · [#1878](https://github.com/Tencent-TDS/KuiklyUI/pull/1878)
- fix(ohos): guard empty HTTP response in KRNetworkModule · [#1876](https://github.com/Tencent-TDS/KuiklyUI/pull/1876)
- fix(ohos): avoid draw-time typography relayout corruption · [#1835](https://github.com/Tencent-TDS/KuiklyUI/pull/1835)
- fix(ohos): align paste truncation to UTF-8 boundary to avoid renderer… · [#1857](https://github.com/Tencent-TDS/KuiklyUI/pull/1857)
- feat(ohos): add support for  kotlin 2.4.21 · [#1898](https://github.com/Tencent-TDS/KuiklyUI/pull/1898)

### Compose

- feat(compose): modifier support attr turboDisplayAutoUpdateEnable · [#1888](https://github.com/Tencent-TDS/KuiklyUI/pull/1888)
- feat(compose): lower PathEffect dashes to the canvas lineDash command · [#1783](https://github.com/Tencent-TDS/KuiklyUI/pull/1783)

### H5

- fix(miniApp): protect for minApp for no Event error · [#1901](https://github.com/Tencent-TDS/KuiklyUI/pull/1901)

### 多端

- feat(input): 扩展 Input/TextArea 与 Compose 的 keyboardType 映射 · [#1885](https://github.com/Tencent-TDS/KuiklyUI/pull/1885)
- fix(touch): preserve pressed state for multi-touch events · [#1895](https://github.com/Tencent-TDS/KuiklyUI/pull/1895)

### 其他

- docs(ohos): document the V2 custom native view registration approach · [#1877](https://github.com/Tencent-TDS/KuiklyUI/pull/1877)
- Bugfix/mini app global this · [#1880](https://github.com/Tencent-TDS/KuiklyUI/pull/1880)
- chore: update adaptive fold Demo · [#1889](https://github.com/Tencent-TDS/KuiklyUI/pull/1889)
- docs: add Compose DSL coroutine and thread switching guide · [#1891](https://github.com/Tencent-TDS/KuiklyUI/pull/1891)

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

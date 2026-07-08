# Kuikly Compose 文本虚线能力 · 摘要（1 页）

> 完整分析见 `kuikly-compose-dashed-underline.md`。本页只给结论与源码证据。

## 一、现状：Kuikly Compose 当前文本虚线到什么程度

**文本是什么**：Kuikly 的 `Text` 底层是各端原生文本控件（Android `TextView` / iOS `NSAttributedString` / 鸿蒙 ArkUI `Text`），文字由系统文本引擎渲染，Kuikly 只负责推送文字参数。Text 背后**没有 Compose 画布**。

**虚线到什么程度**：四种写法实测——

| 写法 | 结果 |
|------|------|
| `TextDecoration.Underline` | ✅ 实线下划线（无虚线位） |
| `AnnotatedString` + `SpanStyle(Underline)` | ✅ 局部实线下划线 |
| `Modifier.drawBehind { 画线段 }` | ❌ 不显示（被 `CanvasView` 闸门跳过） |
| `Modifier.textPostProcessor("dashed")` | ✅ 三端虚线，已验证 |

唯一能画出虚线的路径是 `textPostProcessor("dashed")`，它绕过 Compose 层、让各端原生文本引擎画虚线：Android `DashedUnderlineSpan`、iOS `NSUnderlinePatternDash`、鸿蒙 `kDashedUnderline` span（基线手画）。

## 二、目标：官方 Kotlin Compose 是怎样的

官方 `Text` 是**自绘组件**（背后有 Skia 画布），`drawBehind` 能执行、`drawLine` 有 `pathEffect` 参数。声明式 `TextDecoration` 同样只有 3 位（无虚线），但靠 `drawBehind { drawLine(pathEffect = dashPathEffect) }` 能画出虚线。

**差异根因**：不在 API 层（两边 `TextDecoration` 都只有 3 位），在渲染架构——官方自绘，Kuikly 原生渲染。

| 维度 | 官方 | Kuikly |
|------|------|--------|
| `Text` 底层 | 自绘 Canvas | 原生 RichText |
| `Text` 上 `drawBehind` | ✅ 生效 | ❌ 被跳过 |
| `drawLine` 有 `pathEffect` | ✅ 有 | ❌ 无（被注释） |

## 三、方案：如何对齐，是否值得做

**两条路线**：

- **路线 B（自绘对齐）**：让 `Text` 支持 `drawBehind` + 开放 `pathEffect`，机制层完全对齐官方。❌ **当前不可行**，两道硬伤：
  1. `DrawBackgroundModifier.draw()` 仅在 `view is CanvasView` 时执行 `onDraw()`，`Text` 底层是 `RichTextView`，自绘回调永不触发。
  2. `PathEffect` 类与 `drawLine` 的 `pathEffect` 参数在 Kuikly Compose 移植中全套被注释，编译不过。

- **路线 C（原生桥接，已落地）**：`textPostProcessor("dashed")` 让各端原生文本层画虚线。✅ 三端已验证通过。视觉结果与官方无区别，只是实现机制不同（原生文本层 vs Canvas 自绘）。

**结论**：路线 C 已实现"视觉结果对齐官方"，当前无需额外投入。路线 B 触碰 Kuikly 渲染架构核心假设（Text 全权交给原生 RichText），工作量大、风险高，作为远期技术观察，暂不跟进。

## 源码证据速查

| 事实 | 位置 |
|------|------|
| `drawBehind` 的 CanvasView 闸门 | `compose/.../ui/draw/DrawModifier.kt` `DrawBackgroundModifier.draw()` |
| `Text` 节点无 Draw 能力 | `compose/.../text/modifiers/TextStringRichNode.kt`（仅 Layout+Semantics） |
| `pathEffect` 被注释 | `compose/.../graphics/drawscope/DrawScope.kt`（import 与参数均 `//`） |
| 底层 `setLineDash` 已就绪 | `core/.../views/CanvasView.kt:135`、`core-render-android/.../KRCanvasView.kt:166` |
| Android 虚线 span | `androidApp/.../adapter/KRTextPostProcessorAdapter.kt` |
| 鸿蒙虚线 span | `core-render-ohos/.../KRRichTextView.cpp` `kDashedUnderline` |

# Kuikly Compose 文本虚线能力分析

> 目标：搞清楚 Kuikly Compose 当前文本虚线能做到什么程度、Kotlin Compose（官方）是什么水平、要不要完全对齐。
> 关联 demo：Kuikly 侧 `demo/.../compose/DashedUnderlineDemo.kt`；Kotlin Compose 对照 `DashedLineVerify/MainActivity.kt`。

## 术语澄清（先看这个，避免后面混）

| 叫法 | 是什么 | 包名 |
|------|--------|------|
| **Kotlin Compose** | Google 官方 UI 工具包（即 Jetpack Compose），只跑 Android | `androidx.compose.*` |
| **Kuikly Compose** | 腾讯 Kuikly 的 Compose DSL，写法模仿 Kotlin Compose，底层走 Kuikly 自有渲染引擎（跨端） | `com.tencent.kuikly.compose.*` |

> 本文档只对比这两套：**Kotlin Compose** 指 Google 官方（即 Jetpack Compose）；**Kuikly Compose** 指腾讯 Kuikly 的 Compose DSL 层。两者除 API 写法相似外，底层渲染完全不同。

---

## 一、Kuikly Compose 当前文本虚线到什么程度（初步观察）

从四层链路看，目前声明式 API、样式模型、native 透传层**仍没有**虚线相关入口（`TextDecoration` 无 Dashed 位、`drawBehind` 在 Text 上被跳过）；但三端都可通过 `Modifier.textPostProcessor("dashed")` 借原生绘制虚线（OHOS 详见 1.4）。注意：这里"Kuikly 声明式无虚线"不等于"底层平台无虚线"——Android/iOS/鸿蒙（ArkUI `TextDecorationStyle.Dashed`）原生均支持虚线，只是 Kuikly 的声明式封装未暴露该能力；我们通过 `textPostProcessor` 通道把虚线请求转交各端原生 / framework 渲染。

### 1.1 四层链路（为什么画不出）

| 层 | 现状 | 证据 |
|----|------|------|
| **API 层** | `TextDecoration` 只有 `Underline`(0x1) / `LineThrough`(0x2)，没有 Dashed | `compose/.../text/style/TextDecoration.kt` |
| **模型层** | 内部用位掩码 `mask` 存，没有给"虚线"留任何一位 | 同上 |
| **透传层** | 翻译成字符串 `"underline" / "line-through" / "none"`，无 `dashed` 分支 | `compose/.../foundation/text/KuiklyTextExtension.kt` 的 `applyTextDecoration` |
| **渲染层** | `Text` 底层节点 `TextStringRichNode` 只实现 `LayoutModifierNode + SemanticsModifierNode`，不实现 Draw；`drawBehind` 作用在 `Text` 上被 `DrawModifier` 跳过（只有 `CanvasView` 才画） | `DrawModifier.kt`、`TextStringRichNode.kt` |

> **位掩码**：用一个整数的二进制位当多个开关。Kuikly 用 `0x1`=下划线、`0x2`=删除线，当前未给虚线留位。
> **`drawBehind` 被跳过**：源码里 `if (view is CanvasView) { 画 } else { 打 error log，跳过 }`。Kuikly 的 `Text` 底层是各平台原生文本控件（Android=TextView、iOS=NSAttributedString 富文本、OHOS=ArkUI Text），不是 Kuikly 的 `CanvasView`，所以 `drawBehind` 在 Text 上不生效（待确认是否为设计意图）。

### 1.2 Kuikly 四场景对照（实测）

| 场景 | 写法 | 结果 |
|------|------|------|
| 场景1 | `TextDecoration.Underline` | ✅ 实线下划线 |
| 场景2 | `AnnotatedString` + `SpanStyle(Underline)` | ✅ 局部实线下划线 |
| 场景3 | `Text` + `Modifier.drawBehind { 手动循环画短线段 }` | ❌ 不显示（`drawBehind` 被跳过） |
| **场景4** | `Modifier.textPostProcessor("dashed")` | ✅ 三端虚线下划线（Android/iOS 走原生适配器；OHOS 走方案 C 的 kDashedUnderline span，基线手画真·文本虚线） |

### 1.3 已知可用路径 —— `textPostProcessor` 借道原生

```kotlin
// 用法（Compose DSL）
Text("要加虚线的文本", Modifier.textPostProcessor("dashed"))
```

- Android：在 `KRTextPostProcessorAdapter` 注册 `"dashed"` 分支，用 `DashedUnderlineSpan : ReplacementSpan` 接管文字绘制，先画文字、再在 baseline 下方用 `canvas.drawLine` 一段段画短线。
- iOS：走适配器，用原生富文本虚线样式——整段 `NSAttributedString` 加 `NSUnderlineStyleSingle | NSUnderlinePatternDash`（同色）；已通过真机验证（iPhone 模拟器场景4 虚线正常显示）。
- 鸿蒙（OHOS）：ArkUI 原生 `Text` 组件的 `decoration` 属性**本身支持虚线**（`TextDecorationStyle.Dashed` 修饰 `Underline` 即可）。但方案 C 落地前，Kuikly 的 OHOS `textPostProcessor` 桥只回传文本/图片 span，未把虚线线型（`TextDecorationStyle`）透传/表达出来，所以当时 `dashed` 分支无法借原生之手画虚线、只能走 Compose 自绘兜底（见 1.4）。**方案 C 已落地**（分支 `feat/ohos-dashed-underline-span`）：`core-render-ohos` 新增 `kDashedUnderline` span 类型（`KRTextPostProcessSpan::Type` + 虚线参数 `dash/gap/color/thickness`）；`KRRichTextShadow` 在 Phase 2 展开该 span（文字照常显示、打内部标记），Phase 3 记录其字符区间，`KRRichTextView::OnForegroundDraw` 在 Phase 5 用 `OH_Drawing` 在基线处手画一段段短线。**这是真·文本虚线**（跟字形、换行逐行各一条），区别于早期被移除的 Compose 纯布局近似。
- 属于"绕过文本装饰、借原生"，**不走 `TextDecoration`**，是独立通道。

> 注：demo 早期在 OHOS 上用 Compose 纯布局（Row + 小色块）做过"视觉近似"兜底，但那只是文字下方放一条虚线装饰，**不是真正的文本虚线**（不跟字形 / 基线、换行不会每行各一条）。该近似已移除，OHOS 统一走 `textPostProcessor("dashed")`；方案 C 落地后，OHOS 由 framework 在基线处手画真·文本虚线（见 1.3 鸿蒙条）。

### 1.4 OHOS 演进：从"文本降级"到"基线手画"（为什么一开始画不出、后来能画了）

**阶段1 —— 一开始为什么是"纯文本降级"**

方案 C 落地前，OHOS 的 `textPostProcessor` 桥接（`ohosApp/entry/src/main/cpp/napi_init.cpp`）在 `"dashed"` 分支里调用的是：

```cpp
// napi_init.cpp 旧代码（方案C前）
KRTextProcessedResultAppendTextSpan(builder, text)
```

这个公开 API 当时只能回传**纯文本 span 或图片 span**，没有任何字段可以表达"虚线下划线样式"（dash 长度、间隔、颜色、线宽）。于是 framework 收到的是"把这段文字原样显示"，虚线信息在桥接层就丢失了，屏幕上只显示普通文本、没有虚线。

直接证据（HiLog `A01236/KuiklyDashed`）：
```
dashed processor hit on OHOS, but current API cannot express dashed underline; degrade to plain text.
text=这是一条真正贴合文字宽度的虚线下划线
```
这条日志证明代码路径已走通（`dashed` 分支确实被命中），但执行逻辑就是"打印警告 → 退化为纯文本"。**根因是 API 能力缺口：没有能承载虚线样式的 span 类型，不是渲染引擎不会画。**

**阶段2 —— 后来怎么实现"基线手画"**

既然现有 API 不够，就**新增一个带虚线参数的 span 类型**，让 framework 自己手画（方案 C，分支 `feat/ohos-dashed-underline-span`）：

- `KRTextPostProcessor.h`：`Type` 枚举加 `kDashedUnderline`；struct 加 `dash_width/gap_width/underline_color/thickness` 字段
- `Kuikly.h` / `Kuikly.cpp`：新增公开 C API `KRTextProcessedResultAppendDashedUnderlineSpan(builder, text, dash, gap, color, thickness)`
- `napi_init.cpp`：`"dashed"` 分支从 `AppendTextSpan`（降级纯文本）改为 `AppendDashedUnderlineSpan(builder, text, 6.0f, 4.0f, 0xff000000, 1.0f)`
- `KRRichTextShadow`：Phase2 展开该 span（文字照常显示 + 打内部标记）；Phase3 记录字符区间 `[start,end)` + 绘制参数到 `dashed_underline_records_`
- `KRRichTextView::OnForegroundDraw`：Phase5 遍历 records，用 `OH_Drawing_TypographyGetRectsForRange` 取跨行矩形，在每个矩形内按 dash+gap 手画短线段

> 关键：**这不是 Compose 自绘（drawBehind 在 Kuikly Text 上仍不生效），而是 framework C++ 渲染层在文本排版完成、真正画到屏幕前，额外叠加的虚线绘制**——和 Android `DashedUnderlineSpan` 在 baseline 下手画的思路一致。它是真·文本虚线（跟字形、换行逐行各一条），区别于早期被移除的 Row+小色块视觉近似，也区别于阶段1 的纯文本降级。

**演进对照**：

```
阶段1（方案C前）：napi_init "dashed" → AppendTextSpan(纯文本) → 无虚线 ❌（API 无虚线字段）
阶段2（方案C落地）：napi_init "dashed" → AppendDashedUnderlineSpan(dash=6,gap=4,...)
                 → Shadow Phase2/3 记录区间+参数
                 → View OnForegroundDraw GetRectsForRange + Path 手画 → 基线处真虚线 ✅
```

---

## 二、目标：Kotlin Compose（官方）是怎样的，支持虚线吗

**观察：Kotlin Compose 的声明式文本装饰同样没有虚线（`TextDecoration` 也只有 Underline/LineThrough）。但 Kotlin Compose 的 `Text` 是真正的画布组件，能用 `drawBehind` + `PathEffect.dashPathEffect` 一键画出虚线。即：声明式未发现虚线开关，但自绘能力可以画出虚线。**

> 注意：这一点和 Kuikly 的**差异主要在"渲染层"**——API 层（未发现虚线开关）、模型层（位掩码当前未给虚线留位）两边目前看起来一致。

### 2.1 Kotlin Compose 五场景（来自 `DashedLineVerify`）

| 场景 | 写法 | 结果 |
|------|------|------|
| 场景1 | `Text` + `drawBehind { drawLine(pathEffect = dashPathEffect) }` | ✅ 整行虚线 |
| 场景2 | `Text` + `onTextLayout` 拿包围盒 + `drawBehind` 只画局部 | ✅ 局部虚线 |
| 场景3 | `textDecoration = TextDecoration.Underline` | ✅ 实线下划线（对照基线） |
| 场景4 | 多行折行文本 + `onTextLayout.getLineBottom` 逐行画 | ✅ 每行都画 |
| 场景5 | 不同 dash/gap/strokeWidth 参数 | ✅ 疏密粗细可调 |

Kotlin Compose 核心 API（Kuikly Compose 没有的）：

```kotlin
drawLine(
    color = Color.Red,
    start = Offset(0f, size.height),
    end = Offset(size.width, size.height),
    strokeWidth = 1.dp.toPx(),
    pathEffect = PathEffect.dashPathEffect(   // ← 内置"虚线画笔效果"
        intervals = floatArrayOf(8f, 4f),     //   按 [8px 实, 4px 空] 节奏画
        phase = 0f
    )
)
```

### 2.2 两边差异根因

| 维度 | Kotlin Compose | Kuikly Compose |
|------|---------------------|----------------|
| `Text` 底层 | 自有 Skia/Canvas 渲染管线 | 原生 RichText（Android=TextView，iOS=NSAttributedString） |
| `Text` 上 `drawBehind` | ✅ 生效 | ❌ 被跳过（非 CanvasView） |
| `drawLine` 有 `pathEffect` | ✅ 有 | ❌ 无 |
| 声明式文本虚线 | ❌ 没有 | ❌ 没有 |
| 能画虚线的路径 | `drawBehind` + `PathEffect`（纯声明式） | 三端均 `textPostProcessor` → 原生 / framework 绘制：Android=`DashedUnderlineSpan`、iOS=`NSAttributedString` 虚线、OHOS=`kDashedUnderline` span 基线手画（见 1.4） |

---

## 三、方案：如何完全对齐，是否值得做

### 3.1 "对齐官方"的三层解读与路线对照

**核心澄清**：Kotlin Compose（官方）能画虚线的**机制**是 `drawBehind` + `PathEffect`，而非 `TextDecoration`。所以"对齐官方"这个概念模型是**路线 B**（开放自绘）。但路线 B 受 Kuikly 渲染架构限制，目前物理上不可行。下面是两条路线的准确分类：

| 路线 | 本质 | 是否对齐官方 | 可行性 |
|------|------|-------------|--------|
| **路线 B（自绘对齐）** | 让 Kuikly `Text` 支持 `drawBehind`（取消 CanvasView 闘门）+ `drawLine` 开放 `pathEffect`，采用与官方 Compose **相同的虚线绘制机制** | ✅ **是**，机制层完全对齐 | ❌ **当前不可行**，两道硬伤（见下） |
| **路线 C（原生桥接——已落地）** | `textPostProcessor("dashed")` 桥接各端原生虚线能力（Android `DashedUnderlineSpan`、iOS `NSAttributedString` 虚线、OHOS `kDashedUnderline` span 基线手画） | ⚠️ **结果对齐**（最终画出了虚线），但机制不同（不是 Canvas 自绘，而是让原生文本层画） | ✅ **三端已实际验证通过** |

---

**路线 B（自绘对齐 —— 概念正确，但物理上受限）**
- 目标：让 Kuikly 的 `Text` 底层可被 `DrawModifier` 识别（不再要求必须是 `CanvasView`）；`drawLine` 开放 `pathEffect` 参数（桥接到原生 `DashPathEffect`）。
- **两道硬伤**（往期源码验证结论）：
  1. **CanvasView 闸门**（`compose/.../ui/draw/DrawModifier.kt`）：`DrawBackgroundModifier.draw()` 只在 `view is CanvasView` 时执行 `onDraw()`，否则跳过。`Text` 底层是原生 `RichTextView`（非 CanvasView），故 `drawBehind` 的自绘回调永不触发。
  2. **无 PathEffect**：`DrawScope.drawLine` 的 `pathEffect` 参数与 `PathEffect` 类在 Kuikly 移植中全套被注释掉，`drawLine(..., pathEffect=...)` 编译不过。
- 工作量：**很大**，涉及 Kuikly 渲染架构假设（Compose 文本当前全权交给原生 RichText），且三端原生画布机制差异大。

**路线 C（原生桥接 —— 已落地，结果对齐）**
- 机制：`textPostProcessor("dashed")` 把虚线请求转交各端原生渲染层：Android 走 `DashedUnderlineSpan`、iOS 走 `NSAttributedString` 虚线、OHOS 走 `kDashedUnderline` span 基线手画。
- 工作量：三端都是"桥接"，无需动声明式 API 或渲染架构，风险最低。
- **这是当前推荐的"对齐官方视觉结果"的路径**。它没有对齐官方的绘制机制（Canvas 自绘），但在用户看到的效果上等价。

### 3.2 代价 vs 收益

| 项 | 说明 |
|----|------|
| 业务真实诉求 | 文本虚线下划线是个别场景（如链接、价格强调），非高频刚需 |
| 现有解法 | `textPostProcessor` 三端已落地（Android `DashedUnderlineSpan`、iOS `NSAttributedString` 虚线、OHOS `kDashedUnderline` span 基线手画），场景4 已验证 |
| "结果对齐"的代价 | 路线 C 的开发和回归成本已付出，当前无需额外投入。路线 B（自绘对齐）受架构限制不可行 |
| 风险 | 路线 B 触碰 Kuikly 渲染核心假设，可能引出版面/性能回归，已判定当前暂不跟进 |

### 3.3 当前结论

> 三端已通过 `textPostProcessor("dashed")` 实现虚线下划线（Android=`DashedUnderlineSpan`、iOS=`NSAttributedString` 虚线、OHOS=`kDashedUnderline` span 基线手画）。该通道实现了**视觉结果对齐官方**——用户看到的文字虚线下划线和官方 Compose 没有区别。与官方的差异只在实现机制上：官方靠 `drawBehind` + `PathEffect`（Canvas 自绘），我们靠各端原生文本层绘制。这条差异是 Kuikly 渲染架构设计决定的（Text 底层走原生 RichText 而非 CanvasView），不算"缺口"，而是不同的架构取舍。
>
> 路线 B（自绘对齐）作为远期技术观察，暂不跟进。

---

## 附录：关键代码位置

| 内容 | 路径 |
|------|------|
| `TextDecoration` 定义（仅 Underline/LineThrough） | `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/text/style/TextDecoration.kt` |
| 透传层（无 dashed 分支） | `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/KuiklyTextExtension.kt` |
| `Text` 节点（无 Draw 能力） | `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/modifiers/TextStringRichNode.kt` |
| `drawBehind` 跳过逻辑 | `compose/.../DrawModifier.kt`（仅 `CanvasView` 生效） |
| Android 虚线适配器 | `androidApp/.../adapter/KRTextPostProcessorAdapter.kt`（`"dashed"` 分支 + `DashedUnderlineSpan`） |
| Kuikly demo | `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/DashedUnderlineDemo.kt` |
| Kotlin Compose 对照 demo | `DashedLineVerify/app/src/main/java/com/example/dashedlineverify/MainActivity.kt` |
| `textPostProcessor` 官方说明 | `docs/DevGuide/text-post-processor-guide.md` |

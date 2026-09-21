## Context

自研 `Text` 与 `RichText` 共用 `TextAttr`，对齐仅有左、中、右，经 `textAlign` 字符串下发。Compose 已声明 `TextAlign.Justify`，但 `applyTextAlign()` 将 `Justify`、`Start`、`End` 一律按左对齐处理。

各平台渲染现状：

- iOS 的 `KRConvertUtil` 已将 `"justify"` 映射为 `NSTextAlignmentJustified`。
- Web 与小程序将 `textAlign` 原样写入 CSS。
- Android 的 `getTextAlign()` 只识别居中与右对齐；`Layout.Alignment` 没有两端对齐，须使用 `StaticLayout.Builder.setJustificationMode`。
- HarmonyOS 的 `ConvertToTextAlign` 与 `ConvertToArkUITextAlign` 只有左、中、右。

Android `minSdk` 为 21。`JUSTIFICATION_MODE_INTER_CHARACTER` 自 API 35 起可用。启用系统两端对齐后，`Layout.getOffsetForHorizontal`、`getPrimaryHorizontal`、`getSecondaryHorizontal` 仍按未拉伸宽度计算；`getLineMax`、`getLineWidth` 返回的也是未拉伸宽度。现有点击、选区、占位图坐标都依赖这些接口。

本变更同时覆盖自研 DSL 与 Compose DSL。Compose 只调整映射，最终仍使用 core 的 `Text` 与 `RichText`。跨语言通信不新增方法，继续通过 `textAlign` 属性下发 `"justify"`。

## Goals / Non-Goals

**Goals:**

- `Text` 与 `RichText` 可声明两端对齐：软换行且撑满容器的行拉伸至两端，段落末行、硬换行所在行、未撑满的单行不拉伸。
- Compose 将 `Justify` 映射为两端对齐；在从左到右书写方向下，`Start` 为左对齐，`End` 为右对齐。
- 两端对齐生效后，片段点击、图片占位的可点击区域、文本选区与视觉位置一致。
- 渲染按平台依次实现并验收：iOS、HarmonyOS、Web、小程序、Android。
- 独立示例覆盖英文、中文、中英混排，单行与多行，软换行与硬换行，富文本，以及点击、图片占位和选区。
- Android 上左、中、右对齐不增加额外开销；验收必须回归既有非两端对齐场景。
- 官网与 Compose 文档同步更新接口、语义与平台支持说明。文档是交付必要条件。

**Non-Goals:**

- 不为 `Input`、`TextArea`、Canvas 增加两端对齐。
- 不在 core 增加 `START`、`END`，不实现从右到左书写方向。
- 不在 Android 上自研排版；API 低于 35 不启用系统两端对齐。
- 不对末行或短行做拉伸。
- 不以截图像素对比作为主要验收标准。

## Decisions

### D1：core 只增加 `JUSTIFY`，Compose 的 `Start` 与 `End` 在映射层处理

**决策**：`TextAlign` 增加 `JUSTIFY("justify")`，`textAlignJustify()` 写入现有 `textAlign` 属性。Compose `applyTextAlign()` 将 `Justify` 映射为 `"justify"`，`Start` 映射为 `"left"`，`End` 映射为 `"right"`。

**未采纳**：在 core 同步增加 `START`、`END`。当前各平台文本方向固定为从左到右，二者只是左、右对齐的别名；若日后支持从右到左，仍须在映射层转换。

### D2：Android 仅在 API 35 及以上按字符拉开，更低版本按左对齐

**决策**：`Build.VERSION.SDK_INT >= 35` 时调用 `setJustificationMode(JUSTIFICATION_MODE_INTER_CHARACTER)`；否则不设置该模式，`"justify"` 与左对齐走同一路径。不使用按单词拉开：中文没有词间空格，视觉上等同未生效。

**未采纳**：所有版本使用按单词拉开（中文无效）；自研字间排版（超出本次范围）。

### D3：Android 在两端对齐行上集中换算坐标，不得直接使用未计入拉伸的系统查询

**决策**：新增坐标换算辅助（实现名建议 `JustifiedLayout`）。对已拉伸的行对外提供：

- 由坐标求字符偏移、由偏移求横坐标
- 行的可视左右边界（撑满时为 `0` 至 `layout.width`）
- 是否为已拉伸行：已启用按字符拉开、不是段落末行、可见字符数大于 1

现有调用改为经该辅助访问：

| 调用点 | 原接口 | 用途 |
|---|---|---|
| `KRRichTextView.findSpanIndex` | `getOffsetForHorizontal`、`getLineLeft`、`getLineRight` | 片段点击与长按 |
| `KRRichTextViewDrawer.getOffsetForPosition` | `getOffsetForHorizontal` | 选区起点 |
| `getSelectionRect`、`getPositionForOffset` | `getPrimaryHorizontal` | 选区高亮与手柄 |
| `getPlaceholderRect` | `getPrimaryHorizontal` | 图片与占位框 |
| `getSelectionPath` | 内部同样依赖上述接口 | 选区路径 |

`getLineWidth`、`getLineMax` 只作为未拉伸宽度的输入，行内多余空白为 `layout.width - getLineWidth(line)`。

系统 `getSelectionPath` 无法替换内部实现。两端对齐行上须按换算后的横坐标自行构造路径，或改用矩形高亮。不得只修正手柄位置、却仍使用未换算的路径。

**非两端对齐路径**：绝大多数文本是左、中、右对齐。辅助类型须在入口判断「对齐为两端对齐、API 不低于 35、且已启用系统拉开模式」后，立即使用原有 `Layout` 接口，与修改前同一路径。禁止：

- 在左、中、右对齐或 API 低于 35 时按行计算多余空白、遍历替换型占位
- 每次点击或选区分配间隔数组或包装对象
- 对所有 `StaticLayout.Builder` 无条件调用 `setJustificationMode`
- 在测量或绘制路径上为非两端对齐增加日志或同步校准

`setJustificationMode` 仅在 `textAlign == "justify"` 且 API 不低于 35 时调用。

**未采纳**：假定 API 35 已修正系统查询（与已知行为不符）；在各调用点分别估算拉伸（易遗漏行右边界或占位框）；先换算再判断是否两端对齐（会增加主要场景开销）。

### D4：图片占位的间隔规则须先实测再写入公式

**决策**：在编写换算公式之前，于 API 35 设备上对三行文本测量系统如何把多余空白分配到 `ReplacementSpan`：纯中文、中文加空格、中文加图片占位。每个替换型占位计为 1 个间隔单元，不按其 UTF-16 长度计。末行不补偿。

**未采纳**：先按均匀字间写死公式。图片两侧会再次偏移，独立示例中的点击与选区会失败。

### D5：按平台实现，Android 放在最后

**决策**：先完成接口、映射与示例。尚未实现的平台将 `"justify"` 按左对齐处理。随后：

1. iOS 与 macOS：映射已存在，用独立示例确定点击、选区、图片占位的期望行为，作为后续平台对照。
2. HarmonyOS：接入两端对齐枚举，点击与选区使用同一套排版结果；未复现查询偏差时，不预先套用 Android 的坐标换算。
3. Web，然后小程序。
4. Android：先行测量、实现坐标换算、完成验收，并回归 API 低于 35 的设备。该项工作量最大，放在最后，以免阻塞其他平台。

某一平台验收通过后，才可在文档中标记该平台已支持。HarmonyOS 与 Web 默认不复用 Android 的换算实现。

### D6：两处示例，独立页承担完整场景

**决策**：

- 现有 `TextExampleTextAlign` 增加满宽多行两端对齐对照，短句无法验证效果。
- 新增自研页 `TextJustifyDemo`，页顶可在两端对齐与左对齐之间切换。Compose `TextDemo` 只补充静态的中、英、混排与软硬换行。

独立页章节：

| 章节 | 内容 | 验收 |
|---|---|---|
| 语言与行结构 | 英、中、混排，各含短单行与软换行多行 | 短单行不拉伸；中文软换行与左对齐对照 |
| 换行 | 软换行、硬换行、二者混合、末行极短与接近满宽 | 硬换行所在行不拉伸 |
| 富文本静态 | 多样式、图片占位在行首/行中/行尾、连续两图、表情符号 | 人工确认 |
| 点击 | 同一行左、中、右可点片段，行尾空白回退到组件点击，点图片中心，硬换行后右侧片段 | 主要标准；结果写入可观察状态并带测试标记 |
| 选区 | 从拉伸产生的空隙起选、跨越换行符、跨越片段或图片 | 主要标准；平台无选区则标明不适用 |
| 边界情况 | 首行缩进、字距、截断、单字或单词或空串、标点收尾 | 人工确认；换算须避免除以零 |

点击与富文本静态使用相同结构但分成两块，避免同一控件既展示又响应。结果不得只写入日志。

### D7：以行为验收，不以像素对比为主

各平台验收包括：中文软换行与左对齐对照，点击章节，以及该平台可用的选区。其余章节截图留存。自动化在该平台验收通过后编写，不为所有平台设置同一通过条件。

### D8：文档与接口一并交付，支持说明随平台更新

**决策**：`docs/API/components/text.md` 须增加 `textAlignJustify`，体例与现有左、中、右对齐一致。须写明：软换行满行拉伸；末行、硬换行行、短单行不拉伸；Android 仅 API 35 及以上生效；未验收平台与更低 API 回退为左对齐。`rich-text.md` 须写明继承该属性。`docs/Compose/core-components.md` 须写明 `TextAlign.Justify` 已映射到 core。各平台验收通过后更新支持说明，不得只完成代码。

**未采纳**：等所有平台验收后再写文档。业务会先看到接口而没有说明。

## Risks / Trade-offs

- Android 系统查询与绘制不一致 → 集中换算坐标，覆盖行右边界被误判为未命中的情况。
- 图片占位的间隔与均匀字间不一致 → 先按 D4 实测再写公式。
- API 低于 35 无中文两端对齐 → 在文档中写明；旧设备回归点击与选区不得被破坏。
- iOS 上图片占位与两端对齐仍可能偏移 → iOS 同样验收点击与选区，不默认无问题。
- HarmonyOS 绘制后将对齐重置为左对齐 → 首次排版必须带上两端对齐，并用中文段落确认后续帧。
- 小程序对中文两端对齐支持较弱 → 观感差异写入该平台说明，行为验收仍须完成。
- 换算时除以零 → 单字、单词、空串不视为已拉伸行。
- 将换算套用到所有对齐从而拖慢列表 → 入口直接返回原接口；Android 须回归既有左、中、右示例的点击与选区。

## Migration Plan

- 仅新增接口，默认仍为左对齐，业务无需迁移。
- 对外按平台声明支持；未验收平台保持左对齐。
- 回滚：停止下发 `JUSTIFY`，或 Android 不启用系统拉开模式，查询路径与现网一致。

## Open Questions

- API 35 按字符拉开时，替换型占位计几个间隔（D4 实测后关闭）。
- HarmonyOS 与 Web 是否具备与 iOS、Android 同级的选区接口；若无，该平台选区标明不适用。

## File changes（按模块）

### core

- `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/TextView.kt`：`TextAlign.JUSTIFY`、`textAlignJustify()`

### compose

- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/KuiklyTextExtension.kt`：`applyTextAlign`

### core-render-android

- `KRRichTextView.kt`：仅在两端对齐且 API 不低于 35 时设置拉开模式；片段命中与占位框在已拉伸行上走坐标换算
- `text/KRRichTextViewDrawer.kt`：选区坐标同样区分已拉伸行与其他对齐
- 新增坐标换算辅助

### core-render-ios

- `KRConvertUtil.m`：映射已存在，无改动则保留记录
- 若验收未通过：按实测修改 `KRRichTextView`、`KRLabel`、`KRTextSelectionHelper`

### core-render-ohos

- `KRConvertUtil.cpp`：转换函数增加两端对齐
- `KRRichTextShadow.cpp`、`KRParagraph.cpp`：使用现有排版对齐接口；点击与选区以实测为准

### core-render-web

- H5 `KRRichTextView.kt`：确认将 `"justify"` 赋给样式
- 小程序富文本处理与样式声明：确认输出 `text-align: justify`

### demo

- `TextExamplePage.kt`：对齐示例增加满宽多行两端对齐
- 新增 `TextJustifyDemo`
- 目录页增加入口
- Compose `TextDemo.kt`：静态两端对齐

### docs

- `docs/API/components/text.md`：方法说明与平台支持
- `docs/API/components/rich-text.md`：继承说明
- `docs/Compose/core-components.md`：`Justify` 已接通，并指向支持说明

## Context

自研 `Text` 与 `RichText` 共用 `TextAttr`，对齐仅有左、中、右，经 `textAlign` 字符串下发。Compose 已声明 `TextAlign.Justify`，但 `applyTextAlign()` 将 `Justify`、`Start`、`End` 一律按左对齐处理。

各平台渲染现状：

- iOS 的 `KRConvertUtil` 已将 `"justify"` 映射为 `NSTextAlignmentJustified`。
- H5 与小程序将 `textAlign` 原样写入 CSS。
- Android 的 `getTextAlign()` 只识别居中与右对齐；`Layout.Alignment` 没有两端对齐。API 23 及以上由 `JustifiedLayout` 在 `StaticLayout` 换行结果上按字素分配水平位置，不使用 `setJustificationMode`。
- HarmonyOS 的 `ConvertToTextAlign` 与 `ConvertToArkUITextAlign` 已把 `"justify"` 映射为 `TEXT_ALIGN_JUSTIFY`。纯文本走系统两端对齐。含 `ImageSpan` 的行与系统文本一致，见 Risks。

Android `minSdk` 为 21。`StaticLayout.Builder` 自 API 23 起可用。系统 `setJustificationMode` 的水平查询与绘制不一致，因此不采用。两端对齐时点击、选区、占位图使用 `JustifiedLayout` 覆写后的水平坐标。API 低于 23 走旧 `StaticLayout` 构造，`"justify"` 按左对齐。

本变更同时覆盖自研 DSL 与 Compose DSL。Compose 只调整映射，最终仍使用 core 的 `Text` 与 `RichText`。跨语言通信不新增方法，继续通过 `textAlign` 属性下发 `"justify"`。

## Goals / Non-Goals

**Goals:**

- `Text` 与 `RichText` 可声明两端对齐：软换行且撑满容器的行拉伸至两端，段落末行、硬换行所在行、未撑满的单行不拉伸。
- Compose 将 `Justify` 映射为两端对齐；在从左到右书写方向下，`Start` 为左对齐，`End` 为右对齐。
- 两端对齐生效后，片段点击、图片占位的可点击区域、文本选区与视觉位置一致。
- 各平台自行实现两端对齐，不共用 Android 的坐标换算。
- 独立示例覆盖英文、中文、中英混排，单行与多行，软换行与硬换行，富文本，以及点击、图片占位和选区。
- Android 上左、中、右对齐不增加额外开销；验收必须回归既有非两端对齐场景。
- 官网与 Compose 文档同步更新接口、语义与平台支持说明。文档是交付必要条件。

**Non-Goals:**

- 不为 `Input`、`TextArea`、Canvas 增加两端对齐。
- 不在 core 增加 `START`、`END`，不实现从右到左书写方向。
- 不在 Android 上使用系统 `setJustificationMode`。API 低于 23 不启用两端对齐。
- 不对末行或短行做拉伸。
- 不以截图像素对比作为主要验收标准。

## Decisions

### D1：core 只增加 `JUSTIFY`，Compose 的 `Start` 与 `End` 在映射层处理

**决策**：`TextAlign` 增加 `JUSTIFY("justify")`，`textAlignJustify()` 写入现有 `textAlign` 属性。Compose `applyTextAlign()` 将 `Justify` 映射为 `"justify"`，`Start` 映射为 `"left"`，`End` 映射为 `"right"`。

**未采纳**：在 core 同步增加 `START`、`END`。当前各平台文本方向固定为从左到右，二者只是左、右对齐的别名；若日后支持从右到左，仍须在映射层转换。

### D2：Android 在 API 23 及以上按字素拉伸，更低版本按左对齐显示

**决策**：`Build.VERSION.SDK_INT >= 23` 且 `textAlign == "justify"` 时创建 `JustifiedLayout`。换行、行高和行边界仍来自 `StaticLayout`；水平位置按字素簇分配行内空白。末行、硬换行行，以及字素少于 2 的行不拉伸。API 低于 23 使用旧 `StaticLayout` 构造，`"justify"` 与左对齐同一路径。不调用 `setJustificationMode`。

**未采纳**：API 35 的 `JUSTIFICATION_MODE_INTER_CHARACTER`。系统水平查询仍按未拉伸宽度计算，点击与选区会对不齐。按单词拉伸对无空格中文无效。

### D3：Android 两端对齐的水平坐标由 JustifiedLayout 提供

**决策**：`JustifiedLayout` 覆写 `getOffsetForHorizontal`、`getPrimaryHorizontal`、`getLineLeft`、`getLineRight` 等查询，返回拉伸后的位置。片段点击、选区、占位框继续调用 `Layout` 接口；两端对齐时这些调用落到 `JustifiedLayout`，左、中、右对齐时仍是 `StaticLayout`。

`ReplacementSpan` 视为一个原子字素，不按其 UTF-16 长度拆开。字素少于 2 时不分配空白，避免除以零。

**非两端对齐路径**：不创建 `JustifiedLayout`，不按行计算多余空白，不遍历字素。API 24/25 且设置了 `lineBreakMargin` 时，不调用有缺陷的 `StaticLayout.Builder.setIndents`，先截断文本再排版。

**未采纳**：在各调用点分别估算拉伸；对所有 `StaticLayout.Builder` 调用 `setJustificationMode`。

### D4：图片占位按原子字素参与拉伸

**决策**：每个 `ReplacementSpan` 计为 1 个字素，与普通字素一样参与行内空白分配。占位框使用 `JustifiedLayout` 给出的横坐标。末行与硬换行行不补偿。

**未采纳**：先在 API 35 上测量系统拉伸如何分配 `ReplacementSpan` 两侧空白。实现不走系统拉伸。

### D5：各平台自行排版，不共用 Android 的坐标换算

**决策**：

1. iOS 与 macOS：占位符与文本共用 `NSParagraphStyle` 和 span 下标，点击落在拉伸后的字形上。不给占位符写基线偏移。macOS 与 iOS 相同。
2. HarmonyOS：使用系统两端对齐。点击与选区使用同一套排版结果，不套用 `JustifiedLayout`。
3. H5：`text-align: justify`。点击由浏览器按字形命中。无文本选区。
4. 小程序：`text-align: justify`。软换行撑满的行，图片与点击按拉伸后的位置计算。无文本选区。
5. Android：API 23 及以上为 `JustifiedLayout`，API 22 及以下按左对齐显示。

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
| 选区 | 从拉伸产生的空隙起选、跨越换行符、跨越片段或图片 | 主要标准；H5 与小程序无文本选区 |
| 边界情况 | 首行缩进、字距、截断、单字或单词或空串、标点收尾 | 人工确认；字素少于 2 时不拉伸 |

点击与富文本静态使用相同结构但分成两块，避免同一控件既展示又响应。结果不得只写入日志。

### D7：以行为验收，不以像素对比为主

各平台验收包括：中文软换行与左对齐对照，点击章节，以及该平台可用的选区。其余章节截图留存。自动化在该平台验收通过后编写，不为所有平台设置同一通过条件。

### D8：文档与接口一并交付，支持说明随平台更新

**决策**：`docs/API/components/text.md` 增加 `textAlignJustify`，体例与左、中、右对齐一致。介绍与 CSS `text-align: justify` 相同：文本左右两侧对齐到容器边缘，最后一行除外。平台说明只写是否支持，不写点击或选区。鸿蒙行指向下方平台限制：富文本内嵌图片受文本组件限制，拉伸不均匀，使用时注意验证与规避。`rich-text.md` 只写继承 Text 的全部属性，不单列该方法。`docs/Compose/core-components.md` 的组件列表不单列 `TextAlign.Justify`。

**未采纳**：等所有平台都做完再写文档。业务会先看到接口而没有说明。

## Risks / Trade-offs

- 系统拉伸的查询与绘制不一致 → 不使用 `setJustificationMode`，由 `JustifiedLayout` 同时负责拉伸与水平查询。
- 图片占位须作为原子字素参与拉伸 → 每个 `ReplacementSpan` 计为 1 个字素。
- API 低于 23 无两端对齐 → 在文档中写明；这些版本走旧 `StaticLayout`，与左对齐相同。
- iOS 图片占位缺段落样式时整行不拉伸，缺 span 下标时点击落到前一个文本 → 占位符写入与文本一致的段落样式和 span 下标，且不附加基线偏移。
- HarmonyOS 绘制后将对齐重置为左对齐 → 首次排版必须带上两端对齐，并用中文段落确认后续帧。
- HarmonyOS 含 `ImageSpan` 的两端对齐与系统 `Text` + `ImageSpan` 一致。纯文本里中文按字拉伸，英文只在空格处拉伸。占位框宽度保持声明尺寸，图片画在框的左边缘。图片与后一个字之间的空白不在点击和选区范围内，因此不能把图片移到这段空白的正中。行末是文字时，整行也可能到不了容器右边缘。Kuikly 不单独补偿图片横坐标。
- HarmonyOS 排版下标把每个占位算作 1，`text_content_` 不含占位。`getSelection` 必须先把排版下标换回纯文本下标，返回高亮覆盖的文字，占位不进入字符串。
- HarmonyOS 选区可见时暂时关闭容器矩形裁剪，否则手柄圆点会被裁掉。选区结束后按 `overflow` 与圆角恢复。有 `clipPath` 时不改 `NODE_CLIP`。
- H5 与小程序没有文本选区 → 支持说明写明这一点。
- 字素少于 2 时分配空白会除以零 → 这些行不拉伸。
- 两端对齐排版套用到左、中、右会拖慢列表 → 这些对齐不创建 `JustifiedLayout`。

## Migration Plan

- 仅新增接口，默认仍为左对齐，业务无需迁移。
- 对外按平台写明差异。Android API 22 及以下按左对齐显示。
- 回滚：停止下发 `JUSTIFY`，或 Android 不创建 `JustifiedLayout`，查询路径与现网一致。

## File changes（按模块）

### core

- `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/TextView.kt`：`TextAlign.JUSTIFY`、`textAlignJustify()`

### compose

- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/KuiklyTextExtension.kt`：`applyTextAlign`

### core-render-android

- `KRRichTextView.kt`：API 23 及以上且两端对齐时创建 `JustifiedLayout`；其余对齐使用 `StaticLayout`
- `text/JustifiedLayout.kt`：字素级水平位置，并覆写点击、选区、占位所用的查询

### core-render-ios

- `KRConvertUtil.m`：`"justify"` 映射为 `NSTextAlignmentJustified`，无改动
- `KRRichTextView.m`：占位符共用段落样式与 span 下标；借字号只复制字体字段；不写基线偏移

### core-render-ohos

- `KRConvertUtil.cpp`：`"justify"` 映射为 `TEXT_ALIGN_JUSTIFY` 与 `ARKUI_TEXT_ALIGNMENT_JUSTIFY`
- `KRRichTextShadow.cpp`：`TextIndexForTypographyOffset` 把排版下标换回纯文本下标。图片横坐标与系统占位框左边缘一致，不因两端对齐再平移
- `KRRichTextView.cpp`：`GetSelectedContent` 用换算后的下标切片
- `KRView`：选区手柄抬高层级，且不计入 `GetChildCount`
- `IKRRenderViewExport`：原点相同的子节点按指针区分，避免 RichText 与 ImageSpan 在选区查找里互相覆盖
- `KRBasePropsHandler`：选区可见时暂停矩形裁剪，结束后按 overflow 与圆角恢复

### core-render-web

- H5 `KRRichTextView.kt`：将 `"justify"` 赋给样式
- 小程序富文本处理与样式声明：输出 `text-align: justify`

### demo

- `TextExamplePage.kt`：对齐示例增加满宽多行两端对齐
- 新增 `TextJustifyDemo`
- 目录页增加入口
- Compose `TextDemo.kt`：静态两端对齐

### docs

- `docs/API/components/text.md`：方法说明与平台支持
- `docs/API/components/rich-text.md`：继承 Text 全部属性，不单列两端对齐
- `docs/Compose/core-components.md`：组件列表不单列 `TextAlign.Justify`

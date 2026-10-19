## Why

自研 `Text`、`RichText` 目前只支持左对齐、居中、右对齐。Compose 虽已声明 `TextAlign.Justify`，实际仍按左对齐下发，两端对齐不可用。阅读、文章类页面需要段落两端对齐；对齐生效后，点击、图片占位和文本选区必须落在用户看到的字符上，不能只改变观感。

## What Changes

- **core**：`TextAlign` 增加 `JUSTIFY`，`TextAttr` 增加 `textAlignJustify()`。`RichText` 继承该接口，不另设对齐枚举。
- **compose**：将 `TextAlign.Justify` 映射为 `"justify"`；在当前从左到右的书写方向下，将 `Start` 映射为左对齐、`End` 映射为右对齐。不新增 Compose 公开枚举。
- **demo**：在现有文本对齐示例中增加两端对齐对照（须使用满宽多行段落）。新增独立自研示例页，覆盖英文、中文、中英混排，单行与多行，软换行与硬换行，以及富文本、点击、图片占位和选区。Compose 文本示例仅补充静态两端对齐。
- **各平台渲染**：各平台自行实现两端对齐。绘制、点击与选区使用同一套拉伸后的坐标。Android API 22 及以下按左对齐显示。
- **Android**：API 23 及以上使用 `JustifiedLayout`。换行仍由 `StaticLayout` 完成，行内空白按字素分配。点击、选区与图片占位使用该 Layout 的水平坐标。不使用 `setJustificationMode`。左对齐、居中、右对齐不创建 `JustifiedLayout`。
- **iOS**：沿用 `NSTextAlignmentJustified`。图片占位与相邻文本共用段落样式，点击落在拉伸后的字形上。占位符不写基线偏移。macOS 与 iOS 相同。
- **文档**：官网文档补充 `textAlignJustify`，写明末行与短行不拉伸，并列出各平台差异。`RichText` 继承 Text 的全部属性。Compose 组件列表不单列 `TextAlign.Justify`。

无破坏性变更：仅新增枚举与方法，默认对齐行为不变。

## Capabilities

### New Capabilities

- `text-justify-align`：`Text` / `RichText` 两端对齐的接口、各平台渲染与命中语义、官网文档、示例与分平台验收。涉及模块：`core`、`compose`、`core-render-android`、`core-render-ios`、`core-render-ohos`、`core-render-web`、`demo`、`docs`。

### Modified Capabilities

无。`openspec/specs/` 中没有既有的文本对齐规格。

## Impact

- **平台**：iOS、macOS（与 iOS 相同）、Android（API 23 及以上拉伸，API 22 及以下按左对齐显示）、鸿蒙、H5、小程序。H5 与小程序无文本选区。鸿蒙含图片的行不补偿空白。
- **模块**：
  - `core`：对齐枚举与 `textAlignJustify()`
  - `compose`：`TextAlign` 到 core 属性的映射
  - `core-render-android`：`JustifiedLayout` 负责两端对齐的水平位置；非两端对齐不创建该 Layout
  - `core-render-ios`：占位符共用段落样式与 span 下标；不写基线偏移
  - `core-render-ohos`：`"justify"` 映射为系统两端对齐。含 `ImageSpan` 的行不补偿图片位置。`getSelection` 返回选中的文字，占位不进入字符串。首次排版即两端对齐，后续帧保持拉伸
  - `core-render-web`：H5 与小程序使用 CSS `text-align: justify`
  - `demo`：文本对齐示例、独立验收页、Compose 文本示例
  - `docs`：`docs/API/components/text.md`、`rich-text.md`、`docs/Compose/core-components.md`
- **不改动**：`Input`、`TextArea`、Canvas 的对齐；`core-annotations`、`core-ksp`。
- **跨语言通信**：继续使用现有 `textAlign` 属性，不新增原生方法。

## Non-goals

- 不为 `Input`、`TextArea`、Canvas 增加两端对齐。
- 不在 core 增加 `START`、`END` 枚举，不实现从右到左书写方向。
- 不在 Android 上使用系统 `setJustificationMode`。API 低于 23 不启用两端对齐。
- 不对段落末行、硬换行所在行、未撑满容器的单行做拉伸。
- 不以截图像素对比作为主要验收标准；以点击、图片占位、选区是否与视觉一致为准，静态效果与左对齐对照人工确认。
- 不对左对齐、居中、右对齐预计算拉伸量，也不走两端对齐分支。
- 不要求所有平台同时完成后再编写自动化；各平台验收通过后再补充该平台脚本。

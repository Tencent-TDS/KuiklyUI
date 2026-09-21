## Why

自研 `Text`、`RichText` 目前只支持左对齐、居中、右对齐。Compose 虽已声明 `TextAlign.Justify`，实际仍按左对齐下发，两端对齐不可用。阅读、文章类页面需要段落两端对齐；对齐生效后，点击、图片占位和文本选区必须落在用户看到的字符上，不能只改变观感。

## What Changes

- **core**：`TextAlign` 增加 `JUSTIFY`，`TextAttr` 增加 `textAlignJustify()`。`RichText` 继承该接口，不另设对齐枚举。
- **compose**：将 `TextAlign.Justify` 映射为 `"justify"`；在当前从左到右的书写方向下，将 `Start` 映射为左对齐、`End` 映射为右对齐。不新增 Compose 公开枚举。
- **demo**：在现有文本对齐示例中增加两端对齐对照（须使用满宽多行段落）。新增独立自研示例页，覆盖英文、中文、中英混排，单行与多行，软换行与硬换行，以及富文本、点击、图片占位和选区。Compose 文本示例仅补充静态两端对齐。
- **各平台渲染**：按平台依次实现并验收，顺序为 iOS、HarmonyOS、Web、小程序、Android。尚未验收通过的平台，以及 Android API 低于 35 的设备，须将 `"justify"` 按左对齐处理。不得出现绘制已拉伸、点击或选区仍按未拉伸坐标计算的情况。
- **Android**：仅在 API 35 及以上使用系统按字符拉开的两端对齐。点击、选区、图片占位的坐标换算必须计入拉伸后的位置。左对齐、居中、右对齐是主要使用场景，不得因支持两端对齐而增加这些路径的测量或内存开销，也不得改变其原有行为。Android 验收必须包含既有非两端对齐场景，不能只验证新示例页。
- **文档**：须同步更新官网与 Compose 文档：补充 `textAlignJustify`、说明末行与短行不拉伸、列出各平台支持情况（含 Android 仅 API 35 及以上生效）。`RichText` 与 Compose 文档须写明继承关系及 `Justify` 已接通。文档是交付必要条件。

无破坏性变更：仅新增枚举与方法，默认对齐行为不变。

## Capabilities

### New Capabilities

- `text-justify-align`：`Text` / `RichText` 两端对齐的接口、各平台渲染与命中语义、官网文档、示例与分平台验收。涉及模块：`core`、`compose`、`core-render-android`、`core-render-ios`、`core-render-ohos`、`core-render-web`、`demo`、`docs`。

### Modified Capabilities

无。`openspec/specs/` 中没有既有的文本对齐规格。

## Impact

- **平台**：iOS、macOS（与 iOS 共用 `core-render-ios`）、Android（仅 API 35 及以上生效）、HarmonyOS、Web、小程序。
- **模块**：
  - `core`：对齐枚举与 `textAlignJustify()`
  - `compose`：`TextAlign` 到 core 属性的映射
  - `core-render-android`：系统两端对齐，以及拉伸后的坐标换算；非两端对齐路径保持原实现
  - `core-render-ios`：沿用已有 `"justify"` 映射，验收点击、选区与图片占位
  - `core-render-ohos`：补齐两端对齐枚举并接入排版
  - `core-render-web`：H5 与小程序使用 CSS `text-align: justify`
  - `demo`：文本对齐示例、独立验收页、Compose 文本示例
  - `docs`：`docs/API/components/text.md`、`rich-text.md`、`docs/Compose/core-components.md`
- **不改动**：`Input`、`TextArea`、Canvas 的对齐；`core-annotations`、`core-ksp`。
- **跨语言通信**：继续使用现有 `textAlign` 属性，不新增原生方法。

## Non-goals

- 不为 `Input`、`TextArea`、Canvas 增加两端对齐。
- 不在 core 增加 `START`、`END` 枚举，不实现从右到左书写方向。
- 不在 Android 上自研排版引擎；API 低于 35 不启用系统两端对齐。
- 不对段落末行、硬换行所在行、未撑满容器的单行做拉伸。
- 不以截图像素对比作为主要验收标准；以点击、图片占位、选区是否与视觉一致为准，静态效果与左对齐对照人工确认。
- 不对左对齐、居中、右对齐预计算拉伸量，也不走两端对齐分支。
- 不要求所有平台同时完成后再编写自动化；各平台验收通过后再补充该平台脚本。

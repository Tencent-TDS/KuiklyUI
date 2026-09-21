## 1. core

- [ ] 1.1 `TextView.kt` 的 `TextAlign` 增加 `JUSTIFY("justify")`
- [ ] 1.2 `TextAttr` 增加 `textAlignJustify()`，写入 `TextConst.TEXT_ALIGN`
- [ ] 1.3 编译验证：`./gradlew :core:compileDebugKotlinAndroid`

## 2. compose

- [ ] 2.1 `KuiklyTextExtension.applyTextAlign`：将 `Justify` 映射为 `"justify"`，将 `Start` 映射为 `"left"`，将 `End` 映射为 `"right"`，删除将其按左对齐处理的分支
- [ ] 2.2 编译验证：`./gradlew :compose:compileDebugKotlinAndroid`

## 3. demo

- [ ] 3.1 `TextExampleTextAlign` 增加满宽多行 `textAlignJustify()` 对照，加高容器，不得使用短句
- [ ] 3.2 新建自研 `@Page("TextJustifyDemo")`：页顶可在两端对齐与左对齐之间切换，交互结果写入可观察状态并带测试标记
- [ ] 3.3 语言与行结构：英文、中文、中英混排，各含短单行与软换行多行
- [ ] 3.4 换行：软换行、硬换行、二者混合、末行极短与接近满宽
- [ ] 3.5 富文本静态：多样式片段；图片占位在行首、行中、行尾；连续两图；表情符号（不注册点击）
- [ ] 3.6 点击：同一行左、中、右可点片段；行尾空白回退到 `RichText` 点击；点图片中心；硬换行后右侧片段；结果写入可观察状态
- [ ] 3.7 选区：从拉伸空隙起选、跨越换行符、跨越片段或图片；无选区的平台可隐藏交互并保留说明
- [ ] 3.8 边界情况：首行缩进、字距、截断、单字或单词或空串、标点收尾
- [ ] 3.9 `ExampleIndexPage` 增加 `TextJustifyDemo` 入口
- [ ] 3.10 Compose `TextDemo` 增加静态两端对齐：中文、英文、混排，以及软换行与硬换行

## 4. renderer iOS / macOS

- [ ] 4.1 确认 `KRConvertUtil` 将 `"justify"` 映射为 `NSTextAlignmentJustified` 并已作用于 `KRRichTextView` 段落样式；若无需修改则留下依据
- [ ] 4.2 在 iOS 模拟器打开独立示例：中文软换行与左对齐对照，确认末行与硬换行行不拉伸
- [ ] 4.3 验收点击：右侧片段、行尾空白、图片中心、硬换行后右侧片段
- [ ] 4.4 验收 iOS 与 macOS 选区：从空隙起选、跨越换行符、高亮与 `getSelection` 一致
- [ ] 4.5 若 4.3 或 4.4 未通过，仅修改 iOS 命中或选区实现后重新验收
- [ ] 4.6 在 `text.md` 支持说明中更新 iOS 与 macOS；未通过验收不得标记为已支持

## 5. renderer HarmonyOS

- [ ] 5.1 `ConvertToTextAlign` 与 `ConvertToArkUITextAlign` 增加 `"justify"` 到系统两端对齐枚举
- [ ] 5.2 确认首次排版带上两端对齐；对齐被重置后后续帧仍保持拉伸
- [ ] 5.3 验收中文软换行对照与点击；选区能验证则验证，否则在支持说明中标明不适用
- [ ] 5.4 未复现查询坐标偏差时，不预先套用 Android 的坐标换算
- [ ] 5.5 在 `text.md` 支持说明中更新 HarmonyOS

## 6. renderer Web / 小程序

- [ ] 6.1 确认 H5 `KRRichTextView` 将 `"justify"` 赋给 `style.textAlign`
- [ ] 6.2 验收 H5 的对照与点击；选区能验证则验证，否则标明不适用；更新 `text.md`
- [ ] 6.3 确认小程序将 `"justify"` 输出为 `text-align: justify`
- [ ] 6.4 单独验收小程序；若中文观感弱于 H5，写入 `text.md`，不得因 H5 已通过而自动标记完成

## 7. renderer Android（最后实施）

- [ ] 7.1 前序平台验收通过或明确跳过之后，在 API 35 设备上测量纯中文、中文加空格、中文加图片占位的空白分配，写入 design 未决问题的结论
- [ ] 7.2 新增坐标换算辅助：入口判断为两端对齐、API 不低于 35 且已启用系统拉伸后才换算；左、中、右立即使用原有 `Layout` 接口，不得计算多余空白或分配间隔数组
- [ ] 7.3 实现由偏移求横坐标、由坐标求偏移、行可视边界、是否已拉伸行；仅已拉伸行计算多余空白；极短行不得除以零
- [ ] 7.4 `createStaticLayoutBuilder` 仅在 `textAlign == "justify"` 且 API 不低于 35 时设置拉开模式
- [ ] 7.5 `findSpanIndex`：已拉伸行走换算；左、中、右保持使用 `getOffsetForHorizontal`、`getLineLeft`、`getLineRight`
- [ ] 7.6 选区绘制：已拉伸行走换算；其他对齐保持 `getPrimaryHorizontal` 与 `getSelectionPath`
- [ ] 7.7 占位框同样区分路径；已拉伸行的选区路径使用换算坐标或改用矩形
- [ ] 7.8 在 API 35 上验收中文软换行对照、点击与选区
- [ ] 7.9 在 API 低于 35 上确认两端对齐的视觉与点击等同左对齐
- [ ] 7.10 回归非两端对齐场景：`TextExamplePage` 的左、中、右，富文本点击，既有选区页；行为须与修改前一致
- [ ] 7.11 在 `text.md` 支持说明中更新 Android（API 35 及以上生效，更低版本回退为左对齐）

## 8. docs

- [ ] 8.1 `docs/API/components/text.md` 在右对齐之后增加 `textAlignJustify`：说明、示例、效果，体例与左、中、右对齐一致
- [ ] 8.2 同文件写明：软换行满行拉伸；末行、硬换行行、短单行不拉伸
- [ ] 8.3 同文件增加平台支持说明：iOS 与 macOS、HarmonyOS、Web、小程序、Android API 35 及以上及更低版本；未验收平台标为未支持或回退为左对齐
- [ ] 8.4 `docs/API/components/rich-text.md` 写明继承 `textAlignJustify`
- [ ] 8.5 `docs/Compose/core-components.md` 写明 `TextAlign.Justify` 已映射到 core，并指向 `text.md` 的支持说明
- [ ] 8.6 各平台验收通过后更新 8.3；支持说明未更新不得将该平台标为完成

## 9. 自动化

- [ ] 9.1 iOS 验收通过后补充脚本：打开独立示例，点击右侧片段、图片与行尾，断言可观察状态
- [ ] 9.2 HarmonyOS 验收通过后补充点击脚本（若该平台自动化可用）
- [ ] 9.3 Android API 35 验收通过后补充对应脚本；更低 API 仅回归左对齐行为

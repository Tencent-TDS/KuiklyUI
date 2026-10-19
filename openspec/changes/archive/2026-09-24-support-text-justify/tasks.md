## 1. core

- [x] 1.1 `TextAlign` 增加 `JUSTIFY("justify")`
- [x] 1.2 `textAlignJustify()` 写入 `TextConst.TEXT_ALIGN`
- [x] 1.3 `./gradlew :core:compileDebugKotlinAndroid`

## 2. compose

- [x] 2.1 `applyTextAlign`：`Justify` 为 `"justify"`，`Start` 为 `"left"`，`End` 为 `"right"`
- [x] 2.2 `./gradlew :compose:compileDebugKotlinAndroid`

## 3. demo

- [x] 3.1 `TextExampleTextAlign` 增加满宽多行两端对齐
- [x] 3.2 `@Page("TextJustifyDemo")`：可在两端对齐与左对齐之间切换，结果写入可观察状态
- [x] 3.3 英文、中文、中英混排，各含短单行与软换行多行
- [x] 3.4 软换行、硬换行、二者混合、末行极短与接近满宽
- [x] 3.5 多样式片段；图片占位在行首、行中、行尾；连续两图；表情符号
- [x] 3.6 同一行左、中、右可点片段；行尾空白；图片中心；硬换行后右侧片段
- [x] 3.7 从拉伸空隙起选、跨越换行符、跨越片段或图片
- [x] 3.8 首行缩进、字距、截断、单字或单词或空串、标点收尾
- [x] 3.9 `ExampleIndexPage` 增加 `TextJustifyDemo` 入口
- [x] 3.10 Compose `TextDemo` 增加静态两端对齐：中文、英文、混排，软换行与硬换行

## 4. iOS / macOS

- [x] 4.1 `"justify"` 映射为 `NSTextAlignmentJustified`
- [x] 4.2 中文软换行拉伸，末行与硬换行行不拉伸
- [x] 4.3 右侧片段、行尾空白、图片中心、硬换行后右侧片段按拉伸后的字形响应
- [x] 4.4 选区、绘制与点击共用同一 TextKit 排版。macOS 与 iOS 相同
- [x] 4.5 图片占位写入段落样式与 span 下标，不写基线偏移

## 5. HarmonyOS

- [x] 5.1 `"justify"` 映射为系统两端对齐
- [x] 5.2 首次排版即为两端对齐，后续帧保持拉伸
- [x] 5.3 中文软换行与点击按绘制位置。`getSelection` 返回选中文字，占位不进入字符串
- [x] 5.4 不套用 Android 的坐标换算
- [x] 5.5 富文本内嵌图片不补偿位置，拉伸不均匀
- [x] 5.6 `getSelection` 把排版下标换回纯文本下标。选区可见时暂停矩形裁剪，结束后恢复

## 6. H5 / 小程序

- [x] 6.1 H5 将 `"justify"` 写入 `text-align`
- [x] 6.2 H5 软换行对照与点击。无文本选区
- [x] 6.3 小程序输出 `text-align: justify`
- [x] 6.4 小程序单独完成软换行对照与点击。无文本选区

## 7. Android

- [x] 7.1 API 23 及以上使用 `JustifiedLayout`：换行仍用 `StaticLayout`，行内空白按字素分配。末行与硬换行行不拉伸
- [x] 7.2 左、中、右对齐不创建 `JustifiedLayout`
- [x] 7.3 点击、选区与占位使用拉伸后的坐标。字素少于 2 的行不拉伸
- [x] 7.4 不调用 `setJustificationMode`
- [x] 7.5 API 22 及以下按左对齐显示
- [x] 7.6 API 24/25 且设置了 `lineBreakMargin` 时不调用 `setIndents`
- [x] 7.7 富文本 Builder 在 API 23 及以上走 `StaticLayout.Builder`

## 8. docs

- [x] 8.1 `text.md` 增加 `textAlignJustify`：介绍、示例、效果，体例与左、中、右对齐一致
- [x] 8.2 介绍与 CSS `justify` 一致：左右两侧对齐到容器边缘，最后一行除外
- [x] 8.3 平台说明：iOS 与 macOS 同一行；鸿蒙、H5、小程序、Android 各自成行。Android 的 API 分界写在说明里
- [x] 8.4 鸿蒙行指向下方平台限制：富文本内嵌图片受文本组件限制，拉伸不均匀，使用时注意验证与规避
- [x] 8.5 `rich-text.md` 与 Compose 组件列表不单列两端对齐

## 9. 自动化

- [x] 9.1 iOS：`scripts/mobile-test/text-justify-ios.ts` 点击右侧片段、图片与行尾，并断言状态
- [x] 9.2 HarmonyOS：`scripts/mobile-test/text-justify-ohos.ts` 打开 `TextJustifyDemo` 并截图
- [x] 9.3 Android：`scripts/mobile-test/text-justify-android.ts` 在 API 23 及以上断言两端对齐点击

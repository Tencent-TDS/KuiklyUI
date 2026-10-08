## ADDED Requirements

### Requirement: Text 与 RichText 必须能声明两端对齐
`core` 的 `TextAlign` MUST 包含 `JUSTIFY`，值为 `"justify"`。`TextAttr` MUST 提供 `textAlignJustify()`，将 `textAlign` 属性设为 `"justify"`。`RichTextAttr` 继承该接口，MUST NOT 再定义独立的对齐枚举。Compose `applyTextAlign()` MUST 将 `TextAlign.Justify` 映射为 `"justify"`，将 `TextAlign.Start` 映射为 `"left"`，将 `TextAlign.End` 映射为 `"right"`。`Input`、`TextArea`、Canvas MUST NOT 因本能力改变对齐语义。

#### Scenario: Android 自研 DSL 下发两端对齐
- **GIVEN** Android 上的自研 DSL `Text` 或 `RichText`
- **WHEN** 业务调用 `textAlignJustify()`
- **THEN** 跨语言通信 MUST 下发 `textAlign = "justify"`

#### Scenario: iOS 自研 DSL 下发两端对齐
- **GIVEN** iOS 上的自研 DSL `Text` 或 `RichText`
- **WHEN** 业务调用 `textAlignJustify()`
- **THEN** 跨语言通信 MUST 下发 `textAlign = "justify"`

#### Scenario: macOS 自研 DSL 下发两端对齐
- **GIVEN** macOS 上的自研 DSL `Text` 或 `RichText`
- **WHEN** 业务调用 `textAlignJustify()`
- **THEN** 跨语言通信 MUST 下发 `textAlign = "justify"`

#### Scenario: HarmonyOS 自研 DSL 下发两端对齐
- **GIVEN** HarmonyOS 上的自研 DSL `Text` 或 `RichText`
- **WHEN** 业务调用 `textAlignJustify()`
- **THEN** 跨语言通信 MUST 下发 `textAlign = "justify"`

#### Scenario: H5 自研 DSL 下发两端对齐
- **GIVEN** H5 上的自研 DSL `Text` 或 `RichText`
- **WHEN** 业务调用 `textAlignJustify()`
- **THEN** 跨语言通信 MUST 下发 `textAlign = "justify"`

#### Scenario: 小程序自研 DSL 下发两端对齐
- **GIVEN** 小程序上的自研 DSL `Text` 或 `RichText`
- **WHEN** 业务调用 `textAlignJustify()`
- **THEN** 跨语言通信 MUST 下发 `textAlign = "justify"`

#### Scenario: Compose 的 Justify、Start、End 映射
- **GIVEN** 任意平台上的 Compose `Text` 或 `BasicText`
- **WHEN** `textAlign` 为 `Justify`、`Start` 或 `End`
- **THEN** core MUST 分别收到 `"justify"`、`"left"`、`"right"`
- **AND** MUST NOT 将 `Justify` 按左对齐下发

### Requirement: 两端对齐按段落语义拉伸，短行与末行不拉伸
软换行且撑满容器的行 MUST 两端对齐。段落末行（硬换行之前的行，或全文最后一行）MUST NOT 被拉伸。宽度小于容器的单行 MUST 保持左对齐。Android API 不低于 23 时 MUST 使用 `JustifiedLayout` 按字素拉伸。Android API 低于 23 MUST 按左对齐显示，且 MUST NOT 出现绘制已拉伸、点击或选区仍按未拉伸坐标计算的情况。

#### Scenario: iOS 中文软换行拉伸且末行不拉伸
- **GIVEN** iOS 上，多行中文 `Text` 使用 `textAlignJustify()`
- **WHEN** 文本软换行超过一行
- **THEN** 中间行 MUST 撑满容器宽度
- **AND** 最后一行 MUST 保持左对齐

#### Scenario: macOS 英文软换行拉伸且末行不拉伸
- **GIVEN** macOS 上，多行英文 `Text` 使用 `textAlignJustify()`
- **WHEN** 文本按词软换行超过一行
- **THEN** 中间行 MUST 撑满容器宽度
- **AND** 最后一行 MUST 保持左对齐

#### Scenario: Android API 23 及以上中文按字素拉伸
- **GIVEN** Android API 不低于 23，多行无空格中文 `Text` 使用 `textAlignJustify()`
- **WHEN** 文本软换行超过一行
- **THEN** 渲染 MUST 使用 `JustifiedLayout`
- **AND** 中间行 MUST 撑满，末行 MUST 不拉伸
- **AND** MUST NOT 调用 `setJustificationMode`

#### Scenario: Android API 低于 23 按左对齐显示
- **GIVEN** Android API 低于 23，同样的 `textAlignJustify()` 中文多行
- **WHEN** 页面渲染
- **THEN** 视觉 MUST 与 `textAlignLeft()` 一致
- **AND** 渲染 MUST NOT 创建 `JustifiedLayout`

#### Scenario: HarmonyOS 硬换行所在行不拉伸
- **GIVEN** HarmonyOS 上，`Text` 含换行符且使用 `textAlignJustify()`
- **WHEN** 两段均发生软换行
- **THEN** 每段末行 MUST NOT 被拉伸
- **AND** 各段中间的软换行行 MUST 撑满

#### Scenario: H5 短单行不拉伸
- **GIVEN** H5 上，单行文本宽度小于容器
- **WHEN** 使用 `textAlignJustify()`
- **THEN** 文本 MUST 靠左，MUST NOT 被拉伸

#### Scenario: 小程序中英混排软换行
- **GIVEN** 小程序上，中英混排多行且使用 `textAlignJustify()`
- **WHEN** 文本软换行
- **THEN** 中间行 MUST 撑满
- **AND** 末行 MUST 保持左对齐

### Requirement: 片段点击必须命中视觉位置
`RichText` 的 `Span.click` 与 `ImageSpan.click` MUST 按拉伸后的字形位置响应。点在行尾空白且未落在任何片段上时 MUST 交给 `RichText.click`，MUST NOT 响应该行最后一个片段。Android 两端对齐的点击 MUST 使用 `JustifiedLayout` 覆写后的水平查询。

#### Scenario: iOS 点击行右侧片段
- **GIVEN** iOS 上，中文软换行富文本在同一视觉行有左、中、右可点片段
- **WHEN** 用户点击右侧片段的视觉中心
- **THEN** MUST 触发该右侧片段的点击
- **AND** MUST NOT 触发左侧或中间片段

#### Scenario: macOS 点击行尾空白
- **GIVEN** macOS 上，同上的富文本
- **WHEN** 用户点击该行文本右侧空白
- **THEN** MUST 触发已注册的 `RichText.click`
- **AND** MUST NOT 触发最后一个片段的点击

#### Scenario: Android API 23 及以上点击行右侧片段
- **GIVEN** Android API 不低于 23，同上的富文本
- **WHEN** 用户点击右侧片段的视觉中心
- **THEN** MUST 触发该右侧片段的点击
- **AND** 命中 MUST 使用 `JustifiedLayout` 的水平坐标

#### Scenario: Android API 低于 23 点击与左对齐一致
- **GIVEN** Android API 低于 23，使用 `textAlignJustify()` 的可点富文本
- **WHEN** 用户点击某一片段
- **THEN** 命中结果 MUST 与同一内容使用 `textAlignLeft()` 时一致

#### Scenario: HarmonyOS 点击行右侧片段
- **GIVEN** HarmonyOS 上，同上的富文本
- **WHEN** 用户点击右侧片段的视觉中心
- **THEN** MUST 触发该右侧片段的点击

#### Scenario: H5 点击行右侧片段
- **GIVEN** H5 上，同上的富文本
- **WHEN** 用户点击右侧片段的视觉中心
- **THEN** MUST 触发该右侧片段的点击

#### Scenario: 小程序点击行右侧片段
- **GIVEN** 小程序上，同上的富文本
- **WHEN** 用户点击右侧片段的视觉中心
- **THEN** MUST 触发该右侧片段的点击

### Requirement: 图片占位的绘制位置与可点击区域必须一致
已拉伸行中的 `ImageSpan` 与 `PlaceholderSpan` MUST 画在拉伸后的横坐标上，可点击区域 MUST 覆盖该绘制框。Android 上每个 `ReplacementSpan` MUST 作为一个字素参与拉伸，占位框 MUST 使用 `JustifiedLayout` 的横坐标。HarmonyOS 除外：富文本内嵌图片的两端对齐受鸿蒙文本组件限制，拉伸不均匀。Kuikly MUST NOT 单独补偿图片位置。图片与后一个字之间的空白不在点击和选区范围内。

#### Scenario: iOS 点击行中图片占位
- **GIVEN** iOS 上，中文软换行富文本行中有图片占位
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击
- **AND** MUST NOT 触发左右相邻文字片段

#### Scenario: macOS 点击行中图片占位
- **GIVEN** macOS 上，同上
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击

#### Scenario: Android API 23 及以上点击行中图片占位
- **GIVEN** Android API 不低于 23，同上
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击
- **AND** 占位框 MUST 来自 `JustifiedLayout`

#### Scenario: Android API 低于 23 图片占位与左对齐一致
- **GIVEN** Android API 低于 23
- **WHEN** 用户点击图片占位
- **THEN** 命中 MUST 与左对齐时一致

#### Scenario: HarmonyOS 内嵌图片受文本组件限制
- **GIVEN** HarmonyOS 上含 `ImageSpan` 的富文本使用 `textAlignJustify()`
- **WHEN** 软换行撑满的行包含图片
- **THEN** 拉伸 MUST 不均匀，与鸿蒙文本组件一致
- **AND** MUST NOT 单独补偿图片位置
- **AND** 图片与后一个字之间的空白 MUST NOT 进入点击和选区范围

#### Scenario: H5 点击行中图片占位
- **GIVEN** H5 上，同上
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击

#### Scenario: 小程序点击行中图片占位
- **GIVEN** 小程序上，同上
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击

### Requirement: 选区必须按拉伸后的坐标换算
在该平台提供文本选区时，建立选区、拖选与选区高亮 MUST 使用拉伸后的横坐标。`getSelection` 返回的字符串 MUST 等于视觉选中的字符，跨越硬换行时保留换行符。Android 两端对齐的选区 MUST 使用 `JustifiedLayout` 覆写后的水平查询。H5 与小程序无文本选区，本要求不适用于这两处。

#### Scenario: iOS 从拉伸空隙建立选区
- **GIVEN** iOS 上，可选中的中文软换行富文本
- **WHEN** 用户在某一中间行因拉伸产生的空隙处按下并建立选区
- **THEN** 选中文本 MUST 是该视觉位置对应的字
- **AND** 高亮 MUST 覆盖这些字，MUST NOT 停留在未拉伸位置

#### Scenario: macOS 跨越硬换行的选区
- **GIVEN** macOS 上，含换行符的可选中富文本
- **WHEN** 选区跨越硬换行
- **THEN** `getSelection` MUST 包含换行符及后一段开头字符

#### Scenario: Android API 23 及以上选区手柄位置
- **GIVEN** Android API 不低于 23，该平台选区可用
- **WHEN** 选中一行中间的若干字
- **THEN** 手柄与高亮 MUST 对齐拉伸后的字盒
- **AND** MUST 使用 `JustifiedLayout` 的水平坐标

#### Scenario: Android API 低于 23 选区与左对齐一致
- **GIVEN** Android API 低于 23，选区可用
- **WHEN** 对 `textAlignJustify()` 文本建立选区
- **THEN** 起点与高亮 MUST 与左对齐时一致

#### Scenario: HarmonyOS 含占位的选区文本
- **GIVEN** HarmonyOS 上可选中的 `RichText`，文字前后或中间有 `ImageSpan` 或色块占位
- **WHEN** 选区高亮覆盖其中若干文字
- **THEN** `getSelection` MUST 等于高亮覆盖的文字
- **AND** 占位 MUST NOT 作为字符出现在返回字符串中
- **AND** 图片与后一个字之间的空白 MUST NOT 进入选区范围

#### Scenario: H5 与小程序不在支持说明里写选区
- **GIVEN** 业务查阅 `text.md` 的平台说明
- **WHEN** 阅读 H5 与小程序
- **THEN** MUST NOT 写文本选区或点击命中

### Requirement: Android 两端对齐由 JustifiedLayout 完成
Android API 不低于 23 且 `textAlign` 为 `"justify"` 时，实现 MUST 使用 `JustifiedLayout`。换行 MUST 仍由 `StaticLayout` 完成。水平空白 MUST 按字素分配。每个 `ReplacementSpan` MUST 计为 1 个字素。字素少于 2 的行、末行与硬换行行 MUST NOT 被拉伸，MUST NOT 除以零。实现 MUST NOT 调用 `setJustificationMode`。

#### Scenario: Android 极短行不拉伸
- **GIVEN** Android API 不低于 23，一行只有一个可见字或一个单词
- **WHEN** 该行被声明为两端对齐
- **THEN** 该行 MUST 不拉伸
- **AND** 点击与选区 MUST 与未拉伸时相同

### Requirement: Android 非两端对齐路径不得因本能力变慢或行为改变
Android 上左、中、右对齐是主要使用场景。对齐不是 `"justify"` 或 API 低于 23 时，实现 MUST 使用 `StaticLayout`，MUST NOT 创建 `JustifiedLayout`，MUST NOT 按行计算多余空白，MUST NOT 遍历字素。左、中、右的片段点击、图片占位与选区 MUST 与本变更之前一致。

#### Scenario: Android 左对齐不创建 JustifiedLayout
- **GIVEN** Android 任意 API，`textAlignLeft()` 的多行富文本含可点片段
- **WHEN** 用户点击某一片段
- **THEN** 命中 MUST 与修改前一致
- **AND** 测量 MUST NOT 创建 `JustifiedLayout`

#### Scenario: Android 居中与右对齐不创建 JustifiedLayout
- **GIVEN** Android 上使用 `textAlignCenter()` 或 `textAlignRight()` 的文本
- **WHEN** 发生测量、绘制、片段点击或选区查询
- **THEN** MUST NOT 创建 `JustifiedLayout`
- **AND** MUST NOT 调用 `setJustificationMode`

#### Scenario: Android API 低于 23 的两端对齐请求走左对齐路径
- **GIVEN** Android API 低于 23，业务调用了 `textAlignJustify()`
- **WHEN** 发生点击或选区
- **THEN** 路径 MUST 与左对齐相同
- **AND** MUST NOT 创建 `JustifiedLayout`

### Requirement: 示例必须覆盖语言、换行、富文本与可观察交互
现有文本对齐示例 MUST 增加满宽多行两端对齐对照，MUST NOT 只用短句。独立自研页 MUST 提供两端对齐与左对齐切换，并包含：英文、中文、中英混排；短单行与软换行多行；软换行、硬换行、二者混合、长短末行；多样式富文本；图片占位在行首、行中、行尾；可点的左、中、右片段与行尾空白回退；选区（平台支持时）。交互结果 MUST 写入可断言的状态并带测试标记。Compose 文本示例 MUST 至少包含静态两端对齐的中文、英文、混排以及软换行与硬换行。

#### Scenario: Android 独立页可对照与断言
- **GIVEN** Android 示例应用已安装
- **WHEN** 打开独立两端对齐页并切换对齐方式
- **THEN** 各章节 MUST 可见
- **AND** 点击 MUST 更新带测试标记的状态

#### Scenario: iOS 独立页可对照与断言
- **GIVEN** iOS 示例应用已安装
- **WHEN** 打开独立两端对齐页
- **THEN** 各章节 MUST 可见
- **AND** 点击与选区 MUST 可用于该平台验收

#### Scenario: macOS 独立页可对照与断言
- **GIVEN** macOS 示例应用已安装
- **WHEN** 打开独立两端对齐页
- **THEN** 各章节 MUST 可见
- **AND** 选区 MUST 可验证

#### Scenario: HarmonyOS 独立页可对照与断言
- **GIVEN** HarmonyOS 示例应用已安装
- **WHEN** 打开独立两端对齐页
- **THEN** 各章节 MUST 可见
- **AND** 点击 MUST 更新状态

#### Scenario: H5 独立页可对照
- **GIVEN** H5 示例已打开
- **WHEN** 打开独立两端对齐页
- **THEN** 静态章节与点击 MUST 可用

#### Scenario: 小程序独立页可对照
- **GIVEN** 小程序示例已打开
- **WHEN** 打开独立两端对齐页
- **THEN** 静态章节与点击 MUST 可用

### Requirement: 官网与 Compose 文档写明接口与平台差异
`docs/API/components/text.md` MUST 增加 `textAlignJustify`，体例与左、中、右对齐一致。介绍 MUST 与 CSS `text-align: justify` 一致：文本左右两侧对齐到容器边缘，最后一行除外。MUST NOT 在介绍里枚举软换行、硬换行和短单行。Android API 23 及以上拉伸，API 22 及以下按左对齐显示，写在平台说明中。`docs/API/components/rich-text.md` MUST 保持继承 Text 全部属性的写法，MUST NOT 再单列 `textAlignJustify`。`docs/Compose/core-components.md` MUST NOT 在组件列表里单列 `TextAlign.Justify`。支持说明 MUST 分平台书写：iOS 与 macOS 同一行，鸿蒙、H5、小程序、Android 各自成行。平台说明只写拉伸是否支持，MUST NOT 写点击或选区，MUST NOT 写鸿蒙中文按字、英文按空格的拉伸方式。鸿蒙行 MUST 以「详见下方说明」指向平台限制：富文本内嵌图片的两端对齐受鸿蒙文本组件限制，拉伸不均匀，使用时注意验证与规避。

#### Scenario: Android 文档写明版本差异
- **GIVEN** 业务查阅 `text.md` 的两端对齐
- **WHEN** 阅读平台说明
- **THEN** MUST 能看到 `textAlignJustify` 的用法
- **AND** MUST 写明 Android API 23 及以上拉伸，API 22 及以下按左对齐显示

#### Scenario: 各平台分行说明
- **GIVEN** 业务查阅支持说明
- **WHEN** 对照 iOS、macOS、鸿蒙、H5、小程序、Android
- **THEN** 鸿蒙、H5、小程序、Android MUST 各自成行
- **AND** iOS 与 macOS MUST 写在同一行
- **AND** 说明 MUST NOT 写点击或选区

#### Scenario: Compose 文档不单列 Justify
- **GIVEN** 业务查阅 `docs/Compose/core-components.md`
- **WHEN** 查看 `Text` 的组件说明
- **THEN** MUST NOT 单列 `TextAlign.Justify`

### Requirement: 各平台自行排版
HarmonyOS MUST 使用系统两端对齐，MUST NOT 套用 Android 的 `JustifiedLayout`。H5 与小程序 MUST 分别实现点击与图片位置，MUST NOT 互相代替。iOS 的选区、绘制与点击 MUST 共用同一 TextKit 排版。macOS MUST 与 iOS 使用同一实现。Android API 不低于 23 MUST 用 `JustifiedLayout` 完成拉伸、点击与选区；API 低于 23 MUST 按左对齐显示。

#### Scenario: HarmonyOS 不套用 Android 坐标
- **GIVEN** HarmonyOS 上的两端对齐文本
- **WHEN** 计算点击与选区
- **THEN** MUST 使用该平台自己的排版结果
- **AND** MUST NOT 套用 `JustifiedLayout`

#### Scenario: H5 与小程序分别计算点击
- **GIVEN** 同一段两端对齐富文本
- **WHEN** 分别在 H5 与小程序上点击图片或文字
- **THEN** 小程序 MUST 按自己拉伸后的位置响应
- **AND** MUST NOT 只因为 H5 已拉伸就省略小程序的位置计算

#### Scenario: Android 低版本按左对齐显示
- **GIVEN** Android API 低于 23
- **WHEN** 使用 `textAlignJustify()`
- **THEN** 绘制与点击 MUST 与左对齐相同
- **AND** MUST NOT 创建 `JustifiedLayout`

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

#### Scenario: Web 自研 DSL 下发两端对齐
- **GIVEN** Web 上的自研 DSL `Text` 或 `RichText`
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

### Requirement: 已验收平台必须按段落语义拉伸，短行与末行不拉伸
在该平台渲染已验收通过的前提下，软换行且撑满容器的行 MUST 两端对齐。段落末行（硬换行之前的行，或全文最后一行）MUST NOT 被拉伸。宽度小于容器的单行 MUST 呈现为左对齐。Android 仅当 API 不低于 35 且使用按字符拉开时视为已支持；Android API 低于 35 与尚未验收的平台 MUST 将 `"justify"` 按左对齐绘制，且 MUST NOT 出现绘制已拉伸、命中仍按未拉伸坐标计算的情况。

#### Scenario: iOS 中文软换行拉伸且末行不拉伸
- **GIVEN** iOS 已验收通过，多行中文 `Text` 使用 `textAlignJustify()`
- **WHEN** 文本软换行超过一行
- **THEN** 中间行 MUST 撑满容器宽度
- **AND** 最后一行 MUST 保持左对齐

#### Scenario: macOS 英文软换行拉伸且末行不拉伸
- **GIVEN** macOS 已验收通过，多行英文 `Text` 使用 `textAlignJustify()`
- **WHEN** 文本按词软换行超过一行
- **THEN** 中间行 MUST 撑满容器宽度
- **AND** 最后一行 MUST 保持左对齐

#### Scenario: Android API 35 及以上中文按字符拉开
- **GIVEN** Android API 不低于 35，多行无空格中文 `Text` 使用 `textAlignJustify()`
- **WHEN** 文本软换行超过一行
- **THEN** 渲染 MUST 使用 `JUSTIFICATION_MODE_INTER_CHARACTER`
- **AND** 中间行 MUST 撑满，末行 MUST 不拉伸

#### Scenario: Android API 低于 35 回退为左对齐
- **GIVEN** Android API 低于 35，同样的 `textAlignJustify()` 中文多行
- **WHEN** 页面渲染
- **THEN** 视觉 MUST 与 `textAlignLeft()` 一致
- **AND** 渲染 MUST NOT 设置系统拉开模式

#### Scenario: HarmonyOS 硬换行所在行不拉伸
- **GIVEN** HarmonyOS 已验收通过，`Text` 含换行符且使用 `textAlignJustify()`
- **WHEN** 两段均发生软换行
- **THEN** 每段末行 MUST NOT 被拉伸
- **AND** 各段中间的软换行行 MUST 撑满

#### Scenario: Web 短单行不拉伸
- **GIVEN** Web 已验收通过，单行文本宽度小于容器
- **WHEN** 使用 `textAlignJustify()`
- **THEN** 文本 MUST 靠左，MUST NOT 被拉伸

#### Scenario: 小程序中英混排软换行
- **GIVEN** 小程序已验收通过，中英混排多行且使用 `textAlignJustify()`
- **WHEN** 文本软换行
- **THEN** 中间行 MUST 尝试撑满
- **AND** 若该平台中文两端对齐弱于 H5，MUST 在支持说明中写明观感差异，不得在无说明的情况下标记实现完成

### Requirement: 片段点击必须命中视觉位置
已验收平台上，`RichText` 的 `Span.click` 与 `ImageSpan.click` MUST 按拉伸后的字形位置命中。点在行尾空白且未落在任何片段上时 MUST 回退到 `RichText.click`，MUST NOT 命中该行最后一个片段。Android 已验收平台 MUST NOT 对已拉伸行直接使用 `Layout.getOffsetForHorizontal` 或未换算的 `getLineLeft`、`getLineRight`。

#### Scenario: iOS 点击行右侧片段
- **GIVEN** iOS 已验收通过，中文软换行富文本在同一视觉行有左、中、右可点片段
- **WHEN** 用户点击右侧片段的视觉中心
- **THEN** MUST 触发该右侧片段的点击
- **AND** MUST NOT 触发左侧或中间片段

#### Scenario: macOS 点击行尾空白回退
- **GIVEN** macOS 已验收通过，同上的富文本
- **WHEN** 用户点击该行文本右侧空白
- **THEN** MUST 触发已注册的 `RichText.click`
- **AND** MUST NOT 触发最后一个片段的点击

#### Scenario: Android API 35 及以上点击行右侧片段
- **GIVEN** Android API 不低于 35 且已验收通过，同上的富文本
- **WHEN** 用户点击右侧片段的视觉中心
- **THEN** MUST 触发该右侧片段的点击
- **AND** 命中 MUST 经过坐标换算，MUST NOT 直接调用 `getOffsetForHorizontal`

#### Scenario: Android API 低于 35 点击与左对齐一致
- **GIVEN** Android API 低于 35，使用 `textAlignJustify()` 的可点富文本
- **WHEN** 用户点击某一片段
- **THEN** 命中结果 MUST 与同一内容使用 `textAlignLeft()` 时一致

#### Scenario: HarmonyOS 点击行右侧片段
- **GIVEN** HarmonyOS 已验收通过，同上的富文本
- **WHEN** 用户点击右侧片段的视觉中心
- **THEN** MUST 触发该右侧片段的点击

#### Scenario: Web 点击行右侧片段
- **GIVEN** Web 已验收通过，同上的富文本
- **WHEN** 用户点击右侧片段的视觉中心
- **THEN** MUST 触发该右侧片段的点击

#### Scenario: 小程序点击行右侧片段
- **GIVEN** 小程序已验收通过，同上的富文本
- **WHEN** 用户点击右侧片段的视觉中心
- **THEN** MUST 触发该右侧片段的点击

### Requirement: 图片占位的绘制位置与可点击区域必须一致
已验收平台上，位于已拉伸行中的 `ImageSpan` 与 `PlaceholderSpan` MUST 画在拉伸后的横坐标上，可点击区域 MUST 覆盖该绘制框。Android 已验收平台 MUST 用换算后的横坐标计算占位框，MUST NOT 直接使用 `Layout.getPrimaryHorizontal`。

#### Scenario: iOS 点击行中图片占位
- **GIVEN** iOS 已验收通过，中文软换行富文本行中有图片占位
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击
- **AND** MUST NOT 触发左右相邻文字片段

#### Scenario: macOS 点击行中图片占位
- **GIVEN** macOS 已验收通过，同上
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击

#### Scenario: Android API 35 及以上点击行中图片占位
- **GIVEN** Android API 不低于 35 且已验收通过，同上
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击
- **AND** 占位框 MUST 来自坐标换算

#### Scenario: Android API 低于 35 图片占位与左对齐一致
- **GIVEN** Android API 低于 35
- **WHEN** 用户点击图片占位
- **THEN** 命中 MUST 与左对齐时一致

#### Scenario: HarmonyOS 点击行中图片占位
- **GIVEN** HarmonyOS 已验收通过，同上
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击

#### Scenario: Web 点击行中图片占位
- **GIVEN** Web 已验收通过，同上
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击

#### Scenario: 小程序点击行中图片占位
- **GIVEN** 小程序已验收通过，同上
- **WHEN** 用户点击该图视觉中心
- **THEN** MUST 触发该图片占位的点击

### Requirement: 选区必须按拉伸后的坐标换算
在该平台提供文本选区能力时，建立选区、拖选与选区高亮 MUST 使用拉伸后的横坐标。`getSelection` 返回的字符串 MUST 等于视觉选中的字符，跨越硬换行时保留换行符。Android 已验收平台 MUST NOT 对已拉伸行直接使用 `getPrimaryHorizontal` 或 `getOffsetForHorizontal` 绘制手柄或计算起点；选区路径 MUST 使用换算坐标或改用矩形。平台无选区能力时，本要求对该平台标明不适用，MUST NOT 记为通过。

#### Scenario: iOS 从拉伸空隙建立选区
- **GIVEN** iOS 已验收通过，可选中的中文软换行富文本
- **WHEN** 用户在某一中间行因拉伸产生的空隙处按下并建立选区
- **THEN** 选中文本 MUST 是该视觉位置对应的字
- **AND** 高亮 MUST 覆盖这些字，MUST NOT 停留在未拉伸位置

#### Scenario: macOS 跨越硬换行的选区
- **GIVEN** macOS 已验收通过，含换行符的可选中富文本
- **WHEN** 选区跨越硬换行
- **THEN** `getSelection` MUST 包含换行符及后一段开头字符

#### Scenario: Android API 35 及以上选区手柄位置
- **GIVEN** Android API 不低于 35 且已验收通过，该平台选区可用
- **WHEN** 选中一行中间的若干字
- **THEN** 手柄与高亮 MUST 对齐拉伸后的字盒
- **AND** MUST 经过坐标换算，MUST NOT 直接使用 `getPrimaryHorizontal`

#### Scenario: Android API 低于 35 选区与左对齐一致
- **GIVEN** Android API 低于 35，选区可用
- **WHEN** 对 `textAlignJustify()` 文本建立选区
- **THEN** 起点与高亮 MUST 与左对齐时一致

#### Scenario: HarmonyOS 选区或标明不适用
- **GIVEN** HarmonyOS 已验收通过
- **WHEN** 该平台具备与独立示例选区章节相同的接口
- **THEN** 选区 MUST 按拉伸后的坐标工作
- **AND** 若不具备，支持说明 MUST 将该平台选区标为不适用

#### Scenario: Web 选区或标明不适用
- **GIVEN** Web 已验收通过
- **WHEN** 该平台具备选区接口
- **THEN** 选区 MUST 按拉伸后的坐标工作
- **AND** 若不具备，MUST 标为不适用

#### Scenario: 小程序选区或标明不适用
- **GIVEN** 小程序已验收通过
- **WHEN** 该平台具备选区接口
- **THEN** 选区 MUST 按拉伸后的坐标工作
- **AND** 若不具备，MUST 标为不适用

### Requirement: Android 坐标换算必须在写入公式前完成三类文本的实测
Android 实现 MUST 把已拉伸行的坐标换算集中在单一辅助类型中。在确定替换型占位的间隔规则之前，MUST 在 API 35 设备上对纯中文、中文加空格、中文加图片占位三行测量空白分配。仅含一个可见字、一个单词或空串的行 MUST 视为未拉伸，MUST NOT 除以零。

#### Scenario: Android API 35 及以上须先实测再合入公式
- **GIVEN** Android API 不低于 35 的实现开始
- **WHEN** 尚未记录三类文本的空白与间隔实测
- **THEN** MUST NOT 合入换算公式
- **AND** MUST NOT 在各调用点分别估算拉伸量

#### Scenario: Android 极短行不进行换算
- **GIVEN** Android API 不低于 35，一行只有一个可见字或一个单词
- **WHEN** 该行被声明为两端对齐
- **THEN** 换算 MUST 将其视为未拉伸行
- **AND** 点击与选区 MUST 与左对齐相同

### Requirement: Android 非两端对齐路径不得因本能力变慢或行为改变
Android 上左、中、右对齐是主要使用场景。坐标换算 MUST 在对齐不是 `"justify"`、或 API 低于 35、或系统拉开模式未启用时立即使用修改前的 `Layout` 接口，MUST NOT 按行计算多余空白，MUST NOT 遍历替换型占位，MUST NOT 为这类请求分配间隔缓冲。`setJustificationMode` MUST 仅在 `textAlign == "justify"` 且 API 不低于 35 时调用。左、中、右的片段点击、图片占位与选区 MUST 与本变更之前一致。Android 验收 MUST 回归既有非两端对齐页面（至少包括 `TextExamplePage` 的左、中、右与富文本点击，以及已有的选区页），MUST NOT 只验收独立两端对齐示例。

#### Scenario: Android 左对齐点击不计算多余空白
- **GIVEN** Android 任意 API，`textAlignLeft()` 的多行富文本含可点片段
- **WHEN** 用户点击某一片段
- **THEN** 命中 MUST 与修改前一致
- **AND** 代码路径 MUST 直接使用 `getOffsetForHorizontal`、`getLineLeft`、`getLineRight`，MUST NOT 计算行内多余空白

#### Scenario: Android 居中与右对齐不启用拉开模式
- **GIVEN** Android 上使用 `textAlignCenter()` 或 `textAlignRight()` 的文本
- **WHEN** 发生测量、绘制、片段点击或选区查询
- **THEN** MUST NOT 调用 `setJustificationMode`
- **AND** MUST NOT 为这些查询分配两端对齐所用的间隔数组

#### Scenario: Android API 低于 35 的两端对齐请求走左对齐路径
- **GIVEN** Android API 低于 35，业务调用了 `textAlignJustify()`
- **WHEN** 发生点击或选区
- **THEN** 路径 MUST 与左对齐相同
- **AND** MUST NOT 计算多余空白或遍历替换型占位

#### Scenario: Android 验收必须回归非两端对齐场景
- **GIVEN** 坐标换算已合入
- **WHEN** 在 Android 上打开 `TextExamplePage` 的左、中、右与富文本点击，以及既有选区示例（若存在）
- **THEN** 视觉、点击、选区 MUST 与修改前一致
- **AND** 不得只凭独立两端对齐示例宣布 Android 完成

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

#### Scenario: Web 独立页可对照
- **GIVEN** H5 示例已打开
- **WHEN** 打开独立两端对齐页
- **THEN** 静态章节与点击 MUST 可用

#### Scenario: 小程序独立页可对照
- **GIVEN** 小程序示例已打开
- **WHEN** 打开独立两端对齐页
- **THEN** 静态章节与点击 MUST 可用

### Requirement: 官网与 Compose 文档必须同步接口与平台差异
本变更 MUST 更新官网文档，MUST NOT 将文档视为可选项。`docs/API/components/text.md` MUST 增加 `textAlignJustify` 说明，体例与左、中、右对齐一致，并 MUST 写明：软换行满行拉伸；末行、硬换行行、短单行不拉伸；Android 仅 API 不低于 35 生效，更低版本与尚未验收的平台回退为左对齐。`docs/API/components/rich-text.md` MUST 写明 `RichText` 继承该属性。`docs/Compose/core-components.md` MUST 写明 `TextAlign.Justify` 已映射到 core，不再按左对齐处理。支持说明 MUST 随各平台验收更新，尚未验收的平台 MUST 标为未支持或回退，MUST NOT 写成全部平台已支持。

#### Scenario: Android 文档必须写明版本限制与回退
- **GIVEN** 本变更的文档已提交
- **WHEN** 业务查阅 `text.md` 的两端对齐
- **THEN** MUST 能看到 `textAlignJustify` 的用法示例
- **AND** MUST 写明 Android 仅 API 不低于 35 时拉伸，更低版本为左对齐

#### Scenario: iOS 文档不得暗示全部平台一致
- **GIVEN** iOS 已验收通过而 Android 尚未验收
- **WHEN** 业务查阅支持说明
- **THEN** iOS MUST 标为已支持
- **AND** Android MUST NOT 被写成已支持

#### Scenario: macOS 与 iOS 一并说明
- **GIVEN** iOS 与 macOS 共用 `core-render-ios`
- **WHEN** 文档描述 Apple 平台
- **THEN** macOS MUST 与 iOS 一并出现在支持说明中

#### Scenario: HarmonyOS 验收后更新支持说明
- **GIVEN** HarmonyOS 已验收通过
- **WHEN** 更新文档
- **THEN** 支持说明 MUST 将 HarmonyOS 标为已支持
- **AND** MUST NOT 在渲染已合入后仍写待验收

#### Scenario: Web 文档写明 CSS 语义
- **GIVEN** Web 已验收通过
- **WHEN** 查阅 `text.md`
- **THEN** MUST 能确认 H5 使用 `text-align: justify`
- **AND** 末行与短行不拉伸的语义 MUST 与其他已支持平台一致

#### Scenario: 小程序文档单独标明差异
- **GIVEN** 小程序中文两端对齐弱于 H5
- **WHEN** 查阅支持说明
- **THEN** 小程序 MUST 单独成行
- **AND** 观感差异 MUST 写明，MUST NOT 并入 Web 已支持

#### Scenario: Compose 文档必须写明 Justify 已接通
- **GIVEN** `applyTextAlign` 已映射 `Justify`
- **WHEN** 业务查阅 `docs/Compose/core-components.md`
- **THEN** MUST 写明 `TextAlign.Justify` 会下发两端对齐
- **AND** MUST NOT 再暗示 `Justify` 无效或等同左对齐

### Requirement: 渲染必须按平台实现，验收通过后才标记支持
实现顺序 MUST 为 iOS（含 macOS）、HarmonyOS、Web、小程序、Android。某一平台的验收 MUST 包含：中文软换行与左对齐对照、点击章节、该平台可用的选区。尚未验收的平台 MUST 保持左对齐，MUST NOT 写入已支持两端对齐。自动化脚本 MUST 按平台追加，MUST NOT 为全部平台设置同一通过条件。Android 的坐标换算 MUST 在前序平台验收之后再实施。

#### Scenario: 前序平台未通过不得标记后续平台完成
- **GIVEN** iOS 独立示例的验收尚未通过
- **WHEN** 评估支持说明
- **THEN** iOS 与 macOS MUST 仍为待验收
- **AND** HarmonyOS、Web、小程序、Android MUST NOT 被标为已支持

#### Scenario: HarmonyOS 验收不套用 Android 换算
- **GIVEN** HarmonyOS 已接入系统两端对齐
- **WHEN** 点击与选区实测通过
- **THEN** 才可标记 HarmonyOS 已支持
- **AND** MUST NOT 在未复现查询偏差时预先套用 Android 的坐标换算

#### Scenario: Web 与小程序分别验收
- **GIVEN** H5 已验收通过
- **WHEN** 小程序尚未完成点击验收
- **THEN** 小程序 MUST 仍为待验收
- **AND** MUST NOT 因样式已透传而自动标记完成

#### Scenario: Android 最后验收且须覆盖高低版本
- **GIVEN** iOS、HarmonyOS、Web、小程序已按序验收或明确跳过
- **WHEN** 宣布 Android 支持两端对齐
- **THEN** MUST 在 API 不低于 35 的设备上通过点击、选区与中文软换行对照
- **AND** MUST 在 API 低于 35 上确认视觉与点击等同左对齐
- **AND** MUST NOT 在前序平台未验收时先合入 Android 换算并宣称全部平台完成

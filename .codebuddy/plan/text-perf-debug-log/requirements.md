# 需求文档

## 引言

本功能旨在为 `JankPageDemo.kt` 中的 LazyColumn/LazyRow 滑动性能问题添加调试日志，用于排查 Text 文本组件是否存在同一组件多次重组、多次布局的情况。

Profile 分析显示文本的布局测量是性能瓶颈的主要来源。通过添加具有固定前缀的日志，并能区分同一 Text 组件的不同实例，可以帮助定位问题。

## 需求

### 需求 1：在 Demo 页面 Text 组件添加重组日志

**用户故事：** 作为一名开发者，我希望在 Demo 页面的 Text 组件被重组时能打印日志，以便分析滑动过程中是否存在不必要的重组。

#### 验收标准

1. WHEN 用户滑动 LazyColumn/LazyRow 时 THEN 系统 SHALL 在每个 Text 组件重组时打印日志
2. IF 日志被打印 THEN 日志 SHALL 包含固定前缀 `[TextPerfDebug]` 以便过滤
3. WHEN 日志被打印 THEN 日志 SHALL 包含组件的唯一标识符（通过 carIndex 和 itemIndex 组合生成）以区分不同 Text 实例
4. WHEN 日志被打印 THEN 日志 SHALL 包含重组类型标识 `[Recompose]`
5. WHEN 日志被打印 THEN 日志 SHALL 包含文本内容摘要（如 "Title_0_1"）以便识别具体是哪个 Text

### 需求 2：在 Demo 页面 Text 组件添加布局测量日志

**用户故事：** 作为一名开发者，我希望在 Demo 页面的 Text 组件进行布局测量时能打印日志，以便分析滑动过程中是否存在多次布局测量的情况。

#### 验收标准

1. WHEN 用户滑动 LazyColumn/LazyRow 时 THEN 系统 SHALL 在每个 Text 组件进行布局测量时打印日志
2. IF 日志被打印 THEN 日志 SHALL 包含固定前缀 `[TextPerfDebug]` 以便过滤
3. WHEN 日志被打印 THEN 日志 SHALL 包含组件的唯一标识符（通过 carIndex 和 itemIndex 组合生成）以区分不同 Text 实例
4. WHEN 日志被打印 THEN 日志 SHALL 包含布局类型标识 `[Layout]`
5. WHEN 日志被打印 THEN 日志 SHALL 包含约束条件（maxWidth, maxHeight）和测量结果尺寸信息

### 需求 3：日志格式规范

**用户故事：** 作为一名开发者，我希望日志格式统一规范，以便快速过滤和分析日志。

#### 验收标准

1. IF 日志是重组日志 THEN 日志格式 SHALL 为：`[TextPerfDebug][Recompose] id={uniqueId} text={textContent}`
2. IF 日志是布局日志 THEN 日志格式 SHALL 为：`[TextPerfDebug][Layout] id={uniqueId} constraints=(w:{maxW}, h:{maxH}) size=(w:{width}, h:{height})`
3. WHEN 生成 uniqueId THEN 系统 SHALL 使用 `{carIndex}_{itemIndex}_{textType}` 的格式，其中 textType 可以是 title/tag/views/rule 等
4. IF 需要过滤日志 THEN 用户 SHALL 能通过搜索 `[TextPerfDebug]` 前缀来获取所有相关日志
5. IF 需要过滤特定组件的日志 THEN 用户 SHALL 能通过搜索具体的 uniqueId（如 `0_1_title`）来获取该组件的所有日志

### 需求 4：利用 onTextLayout 回调记录布局信息

**用户故事：** 作为一名开发者，我希望通过 Text 组件的 onTextLayout 回调来获取布局测量信息，以便在不修改框架代码的情况下进行调试。

#### 验收标准

1. WHEN Text 组件完成布局 THEN onTextLayout 回调 SHALL 被调用并打印日志
2. IF onTextLayout 被调用 THEN 日志 SHALL 包含文本的测量尺寸信息
3. WHEN 同一 Text 组件被多次布局 THEN 系统 SHALL 打印多条日志，以便开发者发现多次布局的问题

### 需求 5：使用 SideEffect 记录重组次数

**用户故事：** 作为一名开发者，我希望通过 Compose 的 SideEffect 来记录重组发生，以便准确统计重组次数。

#### 验收标准

1. WHEN Text 所在的 Composable 函数被重组 THEN SideEffect SHALL 被执行并打印日志
2. IF SideEffect 被执行 THEN 日志 SHALL 包含当前组件的标识信息
3. WHEN 组件重用时 THEN 系统 SHALL 能区分是新建还是重组

## 边界情况考虑

1. **性能影响**：日志打印本身可能影响性能，因此日志内容应尽量简洁
2. **日志量控制**：由于 Demo 页面有 200 * 16 = 3200 个列表项，每个项有多个 Text，日志量可能很大
3. **唯一标识生成**：需要确保标识符能够唯一区分每个 Text 组件实例

## 技术限制

1. 需要在 Demo 代码中添加日志，而不是修改框架层代码
2. 使用 Kotlin 的 `println` 函数进行日志输出
3. 需要兼容跨平台场景（commonMain）

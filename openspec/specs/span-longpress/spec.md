# span-longpress Specification

## Purpose
为 Kuikly self-DSL `RichText` 提供 span 级声明式长按能力，并在 Android、iOS、HarmonyOS 上统一 span 命中、事件锁定与参数透传语义，确保首版交付范围明确且与组件级 `RichText.longPress` 兼容。

## Requirements
### Requirement: self-DSL RichText span 必须支持声明式长按回调
Kuikly self-DSL 的 `RichText` span SHALL 提供声明式长按注册能力，使业务可以为单个文字 span 绑定长按回调，并获得 `LongPressParams` 作为事件参数。

#### Scenario: Android 上为文字 span 注册长按回调
- **GIVEN** Android 平台上的 Kuikly self-DSL 页面包含一个 `RichText`
- **AND** 该 `RichText` 内部包含一个文字 `Span`
- **WHEN** 业务为该 `Span` 注册 `longPress` 回调
- **THEN** `core` MUST 为该 `Span` 记录长按处理器
- **AND** Android 渲染侧 MUST 允许该 `Span` 在长按命中时回调到业务层

#### Scenario: iOS 上为文字 span 注册长按回调
- **GIVEN** iOS 平台上的 Kuikly self-DSL 页面包含一个 `RichText`
- **AND** 该 `RichText` 内部包含一个文字 `Span`
- **WHEN** 业务为该 `Span` 注册 `longPress` 回调
- **THEN** `core` MUST 为该 `Span` 记录长按处理器
- **AND** iOS 渲染侧 MUST 允许该 `Span` 在长按命中时回调到业务层

#### Scenario: HarmonyOS 上为文字 span 注册长按回调
- **GIVEN** HarmonyOS 平台上的 Kuikly self-DSL 页面包含一个 `RichText`
- **AND** 该 `RichText` 内部包含一个文字 `Span`
- **WHEN** 业务为该 `Span` 注册 `longPress` 回调
- **THEN** `core` MUST 为该 `Span` 记录长按处理器
- **AND** HarmonyOS 渲染侧 MUST 允许该 `Span` 在长按命中时回调到业务层

### Requirement: span longPress 必须在 `start` 阶段锁定首个命中的 span
Kuikly self-DSL `RichText` 在收到长按手势时 MUST 在 `start` 阶段执行一次 span 命中判断；若命中某个 span，则 MUST 将该目标记录为本轮手势的 `activeSpanIndex`，后续 `move`、`end`、`cancel` 均派发给同一目标，直到手势结束。

#### Scenario: Android 在长按开始时锁定目标 span
- **GIVEN** Android 平台上的一个 `RichText` 同时包含多个可长按 span
- **WHEN** 用户在某个 span 上触发 `longPress state=start`
- **THEN** Android 渲染侧 MUST 回传该首个命中的 span index
- **AND** `core` MUST 将其记录为本轮手势的 `activeSpanIndex`
- **AND** 后续 `move`、`end`、`cancel` MUST 派发给同一 span 而不是重新命中

#### Scenario: iOS 在手指轻微移动时保持同一长按目标
- **GIVEN** iOS 平台上的一个 `RichText` 已在 `start` 阶段命中某个 span
- **WHEN** 用户在同一轮长按手势中发生轻微移动并触发 `move`
- **THEN** `core` MUST 继续将事件派发给最初锁定的 span
- **AND** iOS 渲染侧 MUST NOT 因移动重新切换目标 span

#### Scenario: HarmonyOS 在手势结束后清理锁定目标
- **GIVEN** HarmonyOS 平台上的一个 `RichText` 已锁定某个 `activeSpanIndex`
- **WHEN** 当前长按手势收到 `end` 或 `cancel`
- **THEN** `core` MUST 在事件派发完成后清空该 `activeSpanIndex`
- **AND** 下一轮长按 MUST 重新从 `start` 阶段计算命中目标

### Requirement: span longPress 必须优先于 `RichText.longPress`
Kuikly self-DSL `RichText` 在 `start` 阶段命中 span 时 MUST 将整轮长按手势优先派发给该 span；只有未命中任何 span 时，才可以回退到组件级 `RichText.longPress`。

#### Scenario: Android 命中 span 时不重复触发组件级长按
- **GIVEN** Android 平台上的一个 `RichText` 同时注册了 `RichText.longPress` 和某个 `Span.longPress`
- **WHEN** 用户在该 span 上触发长按并命中该 span
- **THEN** `core` MUST 派发该轮长按给对应 `Span.longPress`
- **AND** `core` MUST NOT 在同一轮手势中重复触发 `RichText.longPress`

#### Scenario: iOS 未命中 span 时回退到组件级长按
- **GIVEN** iOS 平台上的一个 `RichText` 注册了组件级 `longPress`
- **AND** 当前触点没有命中任何可长按 span
- **WHEN** 用户触发长按
- **THEN** `core` MUST 触发 `RichText.longPress`
- **AND** `core` MUST NOT 伪造 span index 或派发到任何 span

#### Scenario: HarmonyOS 上可长按 span 与不可长按文本共存
- **GIVEN** HarmonyOS 平台上的一个 `RichText` 同时包含注册了 `Span.longPress` 的文本和未注册长按的普通文本
- **WHEN** 用户在未注册长按的普通文本区域触发长按
- **THEN** `core` MUST 只在命中可长按 span 时才派发 span 级事件
- **AND** 其余情况 MUST 按组件级 `RichText.longPress` 语义处理

### Requirement: 长按事件参数必须保留原始命中信息
Kuikly `LongPressParams` SHALL 保留 renderer 回传的原始参数，以便 `RichText` span 长按分发逻辑可以从中解析 span index 等命中信息，同时不改变现有长按字段语义。

#### Scenario: Android 长按参数保留 span index 原始数据
- **GIVEN** Android 渲染侧在 span 长按命中时生成了包含 span index 的原始事件 payload
- **WHEN** `core` 将该 payload 解码为 `LongPressParams`
- **THEN** `LongPressParams` MUST 保留该原始 payload
- **AND** `RichText` 的 span 长按拦截逻辑 MUST 能从中读取 span index

#### Scenario: iOS 长按参数在非富文本场景下保持兼容
- **GIVEN** iOS 平台上的一个非富文本视图触发普通长按事件
- **WHEN** `core` 解码该长按事件为 `LongPressParams`
- **THEN** 现有 `x`、`y`、`pageX`、`pageY`、`state`、`isCancel` 等字段 MUST 保持既有语义
- **AND** 新增的原始参数能力 MUST NOT 破坏现有调用方

#### Scenario: HarmonyOS 长按参数可被富文本和普通组件共享
- **GIVEN** HarmonyOS 平台同时存在富文本长按与普通视图长按
- **WHEN** 两类组件分别收到 `LongPressParams`
- **THEN** 富文本组件 MUST 能从原始参数中读取 span 命中信息
- **AND** 普通组件 MUST 可以忽略该原始参数并保持既有行为

### Requirement: Android、iOS、HarmonyOS 必须回传一致的 span 命中结果
首版支持平台中的 Android、iOS、HarmonyOS renderer MUST 在 span 长按命中时向 `core` 回传一致的 span index 语义，使同一份 self-DSL 业务代码在三端获得一致的事件目标。

#### Scenario: Android 回传与点击链路一致的 span index
- **GIVEN** Android 平台上的一个 `RichText` 文字 span 已具备点击命中能力
- **WHEN** 用户对同一 span 触发长按
- **THEN** Android 渲染侧 MUST 使用与 span 点击一致的命中基础信息来确定 span index
- **AND** 回传的 span index MUST 指向业务注册长按的同一个 span

#### Scenario: iOS 回传与富文本属性索引一致的 span index
- **GIVEN** iOS 平台上的一个 `RichText` 文字 span 在富文本属性中带有可用于命中的 index 信息
- **WHEN** 用户对该 span 触发长按
- **THEN** iOS 渲染侧 MUST 回传该 index 对应的 span 命中结果
- **AND** `core` MUST 将事件派发给与该 index 匹配的 span

#### Scenario: HarmonyOS 回传与富文本索引映射一致的 span index
- **GIVEN** HarmonyOS 平台上的一个 `RichText` 已具备可用于点击或渲染映射的 span 标识信息
- **WHEN** 用户对其中某个支持长按的 span 触发长按
- **THEN** HarmonyOS 渲染侧 MUST 回传与点击链路一致的 span index
- **AND** `core` MUST 将该长按事件派发给与该 index 匹配的 span

### Requirement: 首版支持平台范围必须明确包含 HarmonyOS 且不包含 Web
`span-longpress` capability 的首版支持平台 MUST 明确包含 Android、iOS、HarmonyOS；Web、miniApp、macOS 若尚未实现，则 MUST 在文档和实现计划中被明确标记为未覆盖范围。

#### Scenario: 规格声明首版支持平台范围
- **GIVEN** 开发者查阅 `span-longpress` capability 的设计与任务文档
- **WHEN** 其确认首版交付范围
- **THEN** 文档 MUST 明确标注 Android、iOS、HarmonyOS 为首版支持平台
- **AND** 文档 MUST 明确标注 Web、miniApp、macOS 不属于本次首版交付承诺

## ADDED Requirements

### Requirement: Touch 上下文事件

Profiler SHALL 在 `profiler_frames.jsonl` 中写入 touch 上下文事件，记录 `touchBegin` / `touchEnd`（不记录 move 事件）。

> 当前平台 PointerEventType 不包含 Cancel 类型，因此不记录 touchCancel。

Touch 上下文事件 SHALL 写为独立 JSONL 行，格式为：
```json
{"type":"touch_context","eventType":"touchBegin|touchEnd","timestampMs":<ms>,"pointerCount":<N>}
```

Hook 点 SHALL 为 `RootNodeOwner.onPointerInput()`，检测 `PointerEventType.Press/Release`，调用 `RecompositionProfiler.recordTouchContext()`。

仅在 `RecompositionProfiler.isEnabled` 为 true 时记录，未启用时零开销。

#### Scenario: touch 事件写入日志
- **WHEN** 用户在屏幕上按下手指，且 Profiler 已启用
- **THEN** profiler SHALL 写入 `{"type":"touch_context","eventType":"touchBegin","timestampMs":1776678700583,"pointerCount":1}` 到 frames.jsonl

#### Scenario: Profiler 未启用时零开销
- **WHEN** Profiler 未启用且用户触发 touch 事件
- **THEN** SHALL 不产生任何日志写入，不影响性能

#### Scenario: move 事件不记录
- **WHEN** 用户在屏幕上滑动手指（PointerEventType.Move）
- **THEN** profiler SHALL 不写入 touch_context 事件

### Requirement: Scroll 上下文事件

Profiler SHALL 在 `profiler_frames.jsonl` 中写入 scroll 上下文事件，记录 `firstVisibleItemIndex` 变化。

Scroll 上下文事件 SHALL 写为独立 JSONL 行，格式为：
```json
{"type":"scroll_context","listId":"<id>","firstVisibleItemFrom":<N>,"firstVisibleItemTo":<M>,"visibleItemCount":<K>,"timestampMs":<ms>}
```

Hook 点 SHALL 为 `LazyListState.applyMeasureResult()`，在 `scrollPosition.updateFromMeasureResult(result)` 前后比较 `firstVisibleItemIndex`，同步回调 `RecompositionProfiler.recordScrollContext()`。

同样 SHALL 支持 `LazyGridState` 和 `PagerState` 的 scroll 上下文事件。

仅在 `RecompositionProfiler.isEnabled` 为 true 时记录，未启用时零开销。

#### Scenario: 列表滚动时记录上下文
- **WHEN** LazyColumn 滚动导致 `firstVisibleItemIndex` 从 3 变为 5，且 Profiler 已启用
- **THEN** profiler SHALL 写入 `{"type":"scroll_context","listId":"feedsList","firstVisibleItemFrom":3,"firstVisibleItemTo":5,"visibleItemCount":7,"timestampMs":1776678700650}`

#### Scenario: index 未变化时不记录
- **WHEN** LazyColumn 布局更新但 `firstVisibleItemIndex` 未变化
- **THEN** profiler SHALL 不写入 scroll_context 事件

### Requirement: 上下文事件向后兼容

上下文事件行 SHALL 使用与帧事件行不同的 `type` 字段值（`touch_context` / `scroll_context`），旧版读取端按 `type` 字段过滤时 SHALL 自动忽略未知类型。

#### Scenario: 旧版 skill 读取新版日志
- **WHEN** 不支持上下文事件的旧版 skill 读取包含 `touch_context` 和 `scroll_context` 行的 frames.jsonl
- **THEN** 旧版 skill SHALL 忽略这些行，不报错

### Requirement: FileOutputStrategy 新增 appendContextEvent

`FileOutputStrategy` SHALL 新增 `appendContextEvent()` 方法，将上下文事件写为独立 JSONL 行到 `pendingFrames` 缓冲区，随帧数据一起 append 到 `profiler_frames.jsonl`。

#### Scenario: 上下文事件写入缓冲区
- **WHEN** `RecompositionProfiler.recordTouchContext()` 被调用
- **THEN** FileOutputStrategy SHALL 将事件 JSON 追加到 `pendingFrames`，在下一次 flush 时写入文件

### Requirement: RecompositionProfiler 新增 recordTouchContext / recordScrollContext API

`RecompositionProfiler` SHALL 新增公开 API：
- `recordTouchContext(eventType: String, pointerCount: Int)`
- `recordScrollContext(listId: String, from: Int, to: Int, visibleItemCount: Int)`

这两个 API SHALL 检查 `isEnabled` 门控，未启用时直接返回。

#### Scenario: 调用 recordScrollContext
- **WHEN** `LazyListState.applyMeasureResult()` 检测到 `firstVisibleItemIndex` 变化且 Profiler 已启用
- **THEN** SHALL 调用 `RecompositionProfiler.recordScrollContext(listId, oldIndex, newIndex, visibleCount)`

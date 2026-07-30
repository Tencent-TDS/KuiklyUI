## Why

`vforLazy` 在聊天流这类“向顶部补数据”的场景下，顶部占位和窗口校正会与用户手势同时发生。当前实现里，当顶部占位不足时，框架会主动发起一次动画校正；在连续低速拖拽到顶部时，这会表现为列表被突然“接管”，出现明显的向顶端加速滚动，破坏手势跟手感。

这个问题已经在 Android Demo 的 `VforLazyExamplePage` 中稳定复现，而且它不是单纯的 Demo 代码问题，而是 `core/` 中 `LazyLoopDirectivesView` 的滚动校正策略与聊天前插场景不匹配。既然已经确认 root cause，需要补一条 OpenSpec 记录，把“拖拽中不应被动画校正打断”的行为约束固定下来。

## What Changes

- 为 `vforLazy` 新增一条面向聊天前插场景的滚动稳定性 capability，明确区分“主动拖拽中”和“非主动拖拽”两类顶部校正策略。
- 规定当列表仍处于用户主动拖拽阶段时，顶部占位不足只能记录待校正状态并延迟到 `scrollEnd` 处理，不能立刻触发动画 `setContentOffset(...)` 接管滚动。
- 规定当列表已结束主动拖拽（如惯性滚动、程序滚动、补偿性滚动）时，框架仍可保留原有动画校正策略，以避免顶部出现长时间空白。
- 记录 Demo 侧聊天前插验证场景：顶部触发区、前插锁、前插后保持首个可见消息锚点不变，用于验证 core 行为能否支撑无限上划加载。

## Non-goals

- 不重写 `vforLazy` 的窗口管理算法，也不改变 `maxLoadItem`、占位估算或窗口切换的整体策略。
- 不为所有列表统一引入新的渲染层事件协议；本次只约束现有 `ScrollParams.isDragging` 已可表达的行为。
- 不扩展为完整的“聊天列表组件”能力；Demo 中的前插锁和触顶触发逻辑只是验证场景，不作为新的通用公共 API。
- 不承诺一次性解决所有“跳变”来源；本次聚焦“连续低速拖拽时突然加速滚到顶”的问题，以及与其直接相关的顶部前插稳定性。

## Capabilities

### New Capabilities

- `vforlazy-scroll-correction-stability`: 约束 `vforLazy` 在顶部占位不足时的滚动校正行为，要求主动拖拽中延迟校正、非主动拖拽允许动画校正，并保证聊天前插场景中的可见锚点连续性。

### Modified Capabilities

- （无：`openspec/specs/` 中当前没有覆盖 `vforLazy` 顶部校正/前插稳定性的既有 capability，本次新增独立 spec。）

## Impact

### Affected platforms

- **Android**：问题已在 `RecyclerView` 路径稳定复现，是本次修复和验收的直接目标。
- **iOS / HarmonyOS / Web / miniApp / macOS**：`LazyLoopDirectivesView` 位于 `core/` 共享层，行为约束同样适用；各平台是否呈现相同体感，取决于各自 render 层对拖拽态和 offset 校正的实现细节。

### Affected modules

- `core/`：新增 `vforLazy` 顶部校正的行为约束，核心影响点在 `LazyLoopDirectivesView`。
- `demo/`：保留一个可复现、可验证的聊天前插页面，确保 `VforLazyExamplePage` 能覆盖顶部触发、前插保持锚点、连续上划等路径。
- `core-render-android/`：不新增 capability，但 Android 渲染层的拖拽态和滚动结束事件是验收本次行为的重要前提。

### Affected behavior and APIs

- 不新增对外公共 API。
- `ScrollParams.isDragging` 的既有含义被明确用于 `vforLazy` 顶部校正分支判定。
- `vforLazy` 在聊天前插验证场景下的行为从“可能在拖拽中被动画接管”变为“拖拽中延后校正，滚动结束后再补偿”。

### Verification impact

- 需要继续使用 Demo 聊天流场景验证“触顶加载前插”“前插后保持当前位置”“连续低速拖拽不再突然加速到顶”三条关键路径。

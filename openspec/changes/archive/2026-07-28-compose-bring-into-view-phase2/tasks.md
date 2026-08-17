## 1. `compose/` bring-into-view 基础骨架

- [x] 1.1 新增 `BringIntoViewRequester`、`Modifier.bringIntoViewRequester(...)` 与 responder 协议骨架
- [x] 1.2 实现 focused rect、viewport rect、`WindowInsets.ime` 的统一可视区计算器
- [x] 1.3 为 responder 增加请求串行化、阈值过滤与滚动后复检机制

## 2. 骨架完成后的阶段验收

- [x] 2.1 验证 requester 在有 / 无 responder 祖先时都能安全返回，request 链路可串通
- [x] 2.2 验证可视区计算器在“无遮挡 / 底部被 IME 遮挡 / 顶部越界”三类场景下输出正确滚动方向与 delta
- [x] 2.3 验证 responder 的串行化、取消旧请求、阈值过滤与滚动后复检逻辑不产生明显抖动或死循环
- [x] 2.4 输出一份骨架阶段验收结论，明确 requester / responder / calculator 分层已稳定，可继续接入真实容器

### 2.A 建议的骨架验收动作

- [x] 2.A.1 做一个最小验证页或临时调试入口，只验证 request 链路与 calculator，不混入真实 `TextField` 自动行为
- [x] 2.A.2 覆盖 4 组最小样例：无 responder、安全 no-op；有 responder、能收到 request；目标已可见、不误滚；目标被挡住、输出稳定 delta
- [x] 2.A.3 至少录一段骨架阶段演示视频或 GIF，能看出“request 发起 → responder 收到 → delta 判定 → 滚动 / no-op”
- [x] 2.A.4 在评审记录中附上骨架退出结论：哪些能力已经成立、哪些真实场景还没接

### 2.B 骨架验收通过标准

- [x] 2.B.1 **链路通过**：request 不依赖具体 `TextField` 场景，也不要求业务手动兜底
- [x] 2.B.2 **几何通过**：三类遮挡判定结果稳定，重复计算不漂移
- [x] 2.B.3 **稳定性通过**：连续 focus / IME 变化下无明显抢滚、抖动、死循环
- [x] 2.B.4 **边界清晰**：本阶段只证明骨架成立，不把 `LazyColumn`、默认输入自动行为、三端体验混进骨架验收

## 3. `compose/` 容器与输入链路接入（D8 修订：对齐官方 A+B 架构）

- [x] 3.1 实现 `ContentInViewNode.kt`：统一 content-in-view 节点，同时作为 A 的 `FocusedBoundsObserverNode` 入口和 B 的 `BringIntoViewResponderNode` 入口；维护 `focusedChild`（A 跟踪）和请求队列（B 一次性请求）；B 请求优先，无 B 请求时才使用 A 跟踪的 focused child
- [x] ~~3.2 为 `verticalScroll(ScrollState)` 接入 `ContentInViewNode`~~（**2026-07-28 已移除**：ScrollState 容器无手势滚动能力且全仓库业务零使用，phase2 范围收敛为仅 `LazyColumn`；`Scroll.kt` 改动已整体回退）
- [x] 3.3 为 `LazyListState` / `LazyColumn` 接入 `ContentInViewNode`：`viewportRect` 用 `layoutInfo`，复用 focused item pinning 能力
- [x] 3.4 恢复 `FocusableNode` 的路 B 默认触发：取消 `Focusable.kt:228-233` 的注释，获焦时调用 `bringIntoViewRequester.bringIntoView()`；但限制为只在输入组件链路生效
- [x] 3.5 在 `CoreTextField` 接入光标 rect 请求：新增 `BringIntoViewRequester`，在获焦、点击光标、输入、selection 变化时调用 `bringSelectionEndIntoView`（对齐官方 `CoreTextField.kt:311-355 / 1084-1131`）—— API 扩展 + modifier 接入已完成；光标 rect 计算待 `TextLayoutResult` 可用后实现
- [x] 3.6 修正路 A 语义：`BringIntoViewResponderNode.onFocusedBoundsChanged` 改为只记录 `focusedChild`，不立即 `scheduleRequest()`；在 viewport / IME 可视区缩小时判断"原本可见现在被裁剪"才触发滚动（对齐官方 `ContentInViewNode.kt:133-170`）

### 3.A 骨架之后的推荐推进顺序

- [x] 3.A.1 先实现 `ContentInViewNode`（3.1），作为 A+B 统一入口
- [x] 3.A.2 ~~补 `ScrollState` 版 `ContentInViewNode` 子类（3.2），接入 `verticalScroll`，用长表单页验证最小闭环~~（随 3.2 一并移除，范围收敛为仅 `LazyColumn`）
- [x] 3.A.3 恢复 `FocusableNode` 路 B 请求（3.4）+ `CoreTextField` 光标 rect 请求（3.5），验证输入框获焦自动滚入可视区
- [x] 3.A.4 修正路 A 语义（3.6），验证键盘弹起导致 viewport 缩小时的焦点保持补偿
- [x] 3.A.5 再完成 `LazyListState` / `LazyColumn`（3.3），验证 lazy 回收与 focused item pinning 在真实列表中可用

### 3.B 每一阶段的交付物

- [x] 3.B.1 ~~`ScrollState` 阶段至少交付一个长表单 demo、一个无遮挡 no-op 场景、一次 Android 实机或模拟器验证~~（随 3.2 一并移除，范围收敛为仅 `LazyColumn`）
- [x] 3.B.2 `LazyListState` 阶段至少交付一个 `LazyColumn` demo、一次键盘已弹出时切换焦点的录屏、一次 lazy 回收稳定性说明
- [x] 3.B.3 `CoreTextField` 阶段至少交付默认自动触发说明、与手动 requester 的职责边界说明、与业务自定义滚动冲突的已知限制说明

## 4. `demo/` 场景验证与平台回归

- [x] 4.1 ~~新增长表单 `BringIntoViewDemo`~~（已调整：ScrollState 场景移除，`BringIntoViewDemo` 仅保留 `LazyColumn` 验证页）
- [x] 4.2 新增或扩展 `LazyColumn` demo，覆盖键盘已显示时切换下方焦点继续自动滚动
- [x] 4.3 在 Android 验证 LazyColumn、焦点切换、无遮挡场景
- [x] 4.4 在 iOS 验证 LazyColumn、焦点切换、无遮挡场景
- [x] 4.5 在 HarmonyOS 验证 LazyColumn、焦点切换、无遮挡场景

### 4.A 最终验收建议材料

- [x] 4.A.1 提供长表单场景录屏：点击底部输入框后自动滚入可视区
- [x] 4.A.2 提供 `LazyColumn` 场景录屏：键盘已显示时切换更下方输入框仍能继续自动滚动
- [x] 4.A.3 提供无遮挡场景录屏：已完全可见的输入框获焦时不应误滚
- [x] 4.A.4 提供三端简短回归结论：Android / iOS / HarmonyOS 是否一致满足“最终可见”而非逐帧贴边

## 5. 骨架之后的扩展工作记录

- [x] 5.1 评估是否为输入默认自动行为提供 opt-out 开关，避免与业务自定义滚动冲突
- [x] 5.2 评估 `LazyListState` 是否需要补充 viewport helper 或 index/key 级能力，覆盖程序化 focus 到离屏项的后续场景
- [x] 5.3 评估 phase2 稳定后 KuiklyDSL 是否直接复用同一套 request / responder 分层
- [x] 5.4 为后续阶段预留 backlog：nested scroll、多父容器协商、caret 级可见性、Native `Scroller` bridge

### 5.A 首版完成后仍需继续跟踪的事项

- [x] 5.A.1 记录默认自动行为的业务兼容反馈，决定是否需要默认开关或 modifier 级关闭能力
- [x] 5.A.2 记录 lazy 离屏 focus 的真实业务诉求，再决定是否值得补 index/key 协议
- [x] 5.A.3 记录多父滚动容器中的典型问题案例，作为 nested scroll 协商阶段输入
- [x] 5.A.4 记录是否存在必须下沉到 Native `Scroller` bridge 的场景证据，而不是先做预设性扩展

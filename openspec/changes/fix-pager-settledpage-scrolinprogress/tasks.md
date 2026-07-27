## 1. compose — PagerState 根因 A 修复

- [x] 1.1 将 `compose/.../foundation/pager/PagerState.kt` 的 `isSnapAnimating`（约 L422）从 `internal var isSnapAnimating = false` 改为 `internal var isSnapAnimating by mutableStateOf(false)`，确认 import 已含 `androidx.compose.runtime.mutableStateOf`（文件已大量使用，应已存在）。
- [x] 1.2 修改 `PagerState.kt` 的 `settledPage`（约 L1168-1174）：判定从 `if (isScrollInProgress)` 改为 `if (isScrollInProgress || isSnapAnimating)`，使 snap 窗口返回缓存 `settledPageState`。
- [x] 1.3 在 `PagerState.kt` 的 `clearSnapTrackingAfterAlignment()`（约 L1019）函数体开头补充 `settledPageState = currentPage`，确保 snap 结束缓存同步到最终落地页。

## 2. compose — KuiklyScrollableState 根因 B 修复

- [x] 2.1 修改 `compose/.../gestures/KuiklyScrollableState.kt` 的 `kuiklyOnScroll`（约 L74-81）：将无条件的 `isScrollingState.value = true` 改为 `if (kuiklyInfo.isDragging || isScrollingState.value) { isScrollingState.value = true }`，避免非手势 `setContentOffset` 误置滚动状态。
- [x] 2.2 确认 `kuiklyInfo.isDragging`（`KuiklyScrollInfo.kt:99`，`mutableStateOf(false)`）在 `scroll` / `dragEnd` 回调中已由 native 同步（`SubcomposeLayout.kt:321,346`），无需新增 bridge 字段。

## 3. compose — 编译验证

- [x] 3.1 运行 `./gradlew :compose:compileDebugKotlinAndroid` 确认 `compose/` 模块编译通过，无未解析引用 / 类型错误。
- [x] 3.2 检查编译警告：确认 `isSnapAnimating` 改为 `mutableStateOf` 后未引入新的「在非快照上下文读取状态」类警告（如有则评估是否需要 `Snapshot.withMutableSnapshot` 包裹写入点）。

## 4. Android 平台验证

> 注：本开发机为 Linux 宿主，无 Android 模拟器（`/opt/android-sdk` 未安装 emulator/AVD）。下列验证依赖真机/模拟器运行编译出的 APK，将在后续「真机测试」stage（smartrun）执行。本期完成代码实现 + APK 编译归档，运行验证移交真机 stage。

- [ ] 4.1 用 `demo/` 既有 `HorizontalPagerDemo3`（其 UI 已展示 `settledPage`「已停止页面」与 `isScrollInProgress`「是否正在滚动」），从 page 2 滑到 page 3 松手，确认 `settledPage` 序列为 `2 → 3`（无 `2 → 0 → 3` 中间跳变）。
- [ ] 4.2 确认 Android `isScrollInProgress` 序列在滑动手势期间为 true、松手 settle 后为 false（无过渡期异常抖动）。
- [ ] 4.3 调用 `pagerState.animateScrollToPage(3)` 程序化动画，确认动画期间 `isScrollInProgress` 为 true、结束后为 false（回归未破坏）。

## 5. iOS 平台验证

> 注：本开发机为 Linux 宿主，无 Xcode/iOS 模拟器，无法本地编译 iOS。iOS 验证需在 Mac 环境执行；本期不作为本地验收项。

- [ ] 5.1 在 iOS 端用同一 demo 页验证：进入页面静止时 `isScrollInProgress` 即为 false（修复前初始即为 true）。
- [ ] 5.2 从 page 2 滑到 page 3 松手，确认 iOS `settledPage` 序列为 `2 → 3`（无 `0 → 2 → 0` 跳变）。
- [ ] 5.3 触发边界 bounce 回弹后静止，确认 `isScrollInProgress` 归 false（不卡 true）。

## 6. 共享 scrollable 容器回归（KuiklyScrollableState 改动影响面）

> 注：同 §4，依赖真机/模拟器运行验证，移交真机 stage。

- [ ] 6.1 LazyList：`LazyColumn` 快速滚动后静止，确认 `isScrollInProgress` 归 false（不卡 true）。
- [ ] 6.2 PullToRefresh：下拉刷新拖拽过程 `isDragging` 判定正常、`isScrollInProgress` 拖拽中 true 松手后 false，确认决策 2.1 未破坏 PullToRefresh 对 `scrollView?.isDragging` 的依赖。

## 7. 收尾

- [x] 7.1 移除调试用临时日志（若有），确认 `PagerDebugConfig.Snap` 保持原默认值（不因调试改动）。本期未新增调试日志，`PagerDebugConfig` 未改动。
- [ ] 7.2 运行 `openspec status --change "fix-pager-settledpage-scrolinprogress"` 确认 tasks 全部完成（运行验证类任务移交真机 stage 后由其闭环）。

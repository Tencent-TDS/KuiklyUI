## 1. core 滚动校正分支改造

- [x] 1.1 将 dragging 状态沿 `onContentOffsetDidChanged -> createItemByOffset -> createItemByPosition -> correctScrollOffsetInLayout` 链路透传到顶部校正逻辑
- [x] 1.2 调整顶部占位不足分支：`isDragging = true` 时只记录待校正状态并延后到 `scrollEnd` 处理，`isDragging = false` 时保留现有动画校正
- [x] 1.3 在延后 layout 的 wait state 中保存 dragging 上下文，确保 after-layout 回调不会误走非拖拽路径

## 2. demo 聊天前插回归场景维护

- [x] 2.1 保持 `VforLazyExamplePage` 的顶部触发区、前插锁和首个可见消息锚点保持逻辑可用
- [x] 2.2 调整 Demo 参数与回调组合，确保页面能稳定复现“触顶前插”和“连续低速拖拽到顶”两条路径
- [x] 2.3 清理调试残留，保留最小必要的验证辅助逻辑，避免 Demo 引入与本 change 无关的新增代码

## 3. renderer 与平台验证

- [x] 3.1 在 Android 路径验证：连续低速拖拽逼近顶部时不再突然加速滚到顶
- [x] 3.2 在 Android 路径验证：触顶前插后当前首个可见消息锚点保持连续，且离开触发区后可再次触发前插
- [x] 3.3 检查 iOS / HarmonyOS 至少一条实现路径上的 dragging 状态与 `scrollEnd` 时序，确认共享层约束没有明显冲突

# KuiklyUI 窗口分层快速参考指南

## 核心发现（一句话总结）

**KuiklyUI 的 Dialog/Popup 使用"插槽堆栈"系统管理 z-order，而非 Android 的 WindowManager，后添加的 Dialog 自动在最顶层。**

---

## 快速Q&A

### Q1: Dialog 和 Popup 用什么窗口类型？
**A:** 
- ❌ 不使用 `WindowManager.TYPE_APPLICATION`
- ✅ 使用 `ModalView` 组件（KNode<ModalView>）
- ✅ 所有 Dialog 都在同一个 Activity 的 ViewGroup 中

### Q2: 如何控制 Dialog 的 z-order？
**A:**
- Dialog 通过 `SlotProvider.addSlot()` 注册
- `_slots` 列表中的顺序决定 z-order
- **后添加 = 后渲染 = 最顶层** ✓

### Q3: 是否需要特殊权限（如 SYSTEM_ALERT_WINDOW）？
**A:** 
- ✓ **不需要**
- Dialog 都在 Activity 的 ViewGroup 中
- 没有跨应用窗口

### Q4: 如何保证 Overlay Popup 总在最顶层？
**A:**
- 建议方案：扩展 `SlotProvider` 支持优先级
- 或在 `ComposeContainer` 中专项处理 overlay
- 最简单：确保 overlay Dialog 最后添加

### Q5: 三个平台上的实现一致吗？
**A:**
- **都用相同的插槽系统**
- Android: ViewGroup children 顺序
- iOS: CALayer/UIView addSubview 顺序（推断）
- HarmonyOS: ArkUI Stack zIndex（推断）

---

## 关键代码速查

### 找 Dialog 创建
📍 `/compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/window/Dialog.kt:203-212`
```kotlin
fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) = DialogLayout(...)
```

### 找插槽系统
📍 `/compose/src/commonMain/kotlin/com/tencent/kuikly/compose/container/SlotProvider.kt`
```kotlin
class SlotProvider {
    private val _slots = mutableStateListOf<...>()
    fun addSlot(content): Int  // 核心方法
}
```

### 找插槽渲染
📍 `/compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ComposeContainer.kt:288-292`
```kotlin
LocalSlotProvider.current.slots.forEach { slotContent ->
    key(slotContent.first) {
        slotContent.second?.invoke()  // 按顺序渲染
    }
}
```

### 找 ModalView
📍 `/core/src/commonMain/kotlin/com/tencent/kuikly/core/views/ModalView.kt:42-150`
```kotlin
class ModalView : ViewContainer<...>() {
    var inWindow: Boolean = false  // 关键参数
}
```

### 找渲染管道
📍 `/compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/scene/BaseComposeScene.kt:187-234`
```kotlin
override fun render(canvas: Canvas?, nanoTime: Long) {
    // 1. 重组 2. 布局 3. 绘制（按顺序）
}
```

---

## Dialog 插槽生命周期

```
组件首次渲染
    ↓
DisposableEffect(Unit) {  // ← 触发一次
    slotId = slotProvider.addSlot { ... }  ← Dialog 添加到插槽
    
    onDispose {
        slotProvider.removeSlot(slotId)  ← 组件卸载时移除
    }
}
    ↓
ComposeContainer.ProvideContainerCompositionLocals()
    ├→ content()  // 渲染主内容
    └→ slots.forEach { ... }  // 渲染所有 Dialog（按顺序）
    ↓
BaseComposeScene.render()
    ├→ frameClock.sendFrame()  // Dialog 完成注册
    ├→ doLayout()  // 计算大小
    └→ draw(canvas)  // 按 slots 顺序绘制
    ↓
最终画布显示 Dialog
```

---

## 平台实现链路

```
┌─ Android ─────────────────────────────────────────┐
│ SlotProvider._slots.forEach()                    │
│   → Dialog() → ModalView                         │
│     → KNode → KRDivView (Android native)         │
│       → ViewGroup.addView(child, index)          │
│         → 后添加的 view 后绘制                   │
└──────────────────────────────────────────────────┘

┌─ iOS (推断) ────────────────────────────────────┐
│ SlotProvider._slots.forEach()                   │
│   → Dialog() → ModalView                        │
│     → Native Bridge → UIView.addSubview()       │
│       → CALayer.zPosition 或顺序决定层级        │
└──────────────────────────────────────────────────┘

┌─ HarmonyOS (推断) ────────────────────────────┐
│ SlotProvider._slots.forEach()                 │
│   → Dialog() → ModalView                      │
│     → Native Bridge → Stack(zIndex=...)       │
│       → zIndex 较大的组件在上层               │
└──────────────────────────────────────────────┘
```

---

## 实现 Overlay Popup 的最佳实践

### ✓ 推荐方法 1：确保最后添加

```kotlin
// Profiler overlay Dialog 应该是最后创建的
if (isProfilerEnabled) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(),
        content = { ProfilerOverlay() }
    )  // ← 在主内容之后调用
}
```

### ✓ 推荐方法 2：扩展 SlotProvider（更好）

```kotlin
// 修改 SlotProvider
class SlotProvider {
    private val _overlaySlots = mutableStateListOf<...>()
    
    fun addOverlaySlot(content: @Composable () -> Unit): Int {
        // overlay slots 总是在最后
        _overlaySlots.add(...)
    }
    
    val allSlots get() = _slots + _overlaySlots
}

// 使用时
ProfilerDialog(content = { ... })  // 使用 addOverlaySlot
```

### ✓ 推荐方法 3：优先级系统（最灵活）

```kotlin
// 修改 SlotProvider
fun addSlot(
    content: @Composable () -> Unit,
    priority: Int = 0
): Int {
    _slots.add(triple(id, content, priority))
}

// 渲染时
slots.sortedBy { it.third }  // 按优先级排序
    .forEach { ... }

// 使用时
slotProvider.addSlot(
    ProfilerOverlay(),
    priority = 1000  // 最高优先级
)
```

---

## 常见问题排查

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Dialog 显示但被其他 Dialog 盖住 | Dialog 添加顺序不对 | 确保 overlay Dialog 最后添加 |
| Dialog 在后台看不到 | inWindow = false | 设置 inWindow = true |
| Dialog 点击没反应 | 被上层 Dialog 拦截 | 检查上层 Dialog 的事件处理 |
| Dialog 在不同平台显示不一致 | 平台实现差异 | 使用统一的 Dialog API，框架处理平台差异 |
| 无法指定 Dialog 优先级 | 当前系统只支持添加顺序 | 等待优先级系统实现，或自己扩展 |

---

## 性能考虑

- ✓ Dialog 插槽系统很轻量
- ✓ 每个 Dialog 独立的 DisposableEffect 生命周期
- ✓ 卸载时自动从插槽移除
- ⚠️ 大量 Dialog 同时打开时，最后的 Dialog 渲染成本最高（需绘制所有下层）

---

## 验证 Dialog Z-Order 的方法

### 1. 添加日志追踪

```kotlin
// 在 SlotProvider.addSlot 中
fun addSlot(content: @Composable () -> Unit): Int {
    val id = nextSlotId++
    _slots.add(id to content)
    Log.d("SlotProvider", "Added slot $id, total: ${_slots.size}")
    return id
}

// 在 ComposeContainer 中
LocalSlotProvider.current.slots.forEach { slotContent ->
    Log.d("Render", "Rendering slot ${slotContent.first}")
    slotContent.second?.invoke()
}
```

### 2. 可视化 Dialog 堆栈

```kotlin
@Composable
fun DebugSlotStack() {
    val slots = LocalSlotProvider.current.slots
    Column {
        slots.forEachIndexed { index, (id, _) ->
            Text("Slot $index (ID: $id) - zIndex: $index")
        }
    }
}
```

### 3. 截图对比

- 显示 Dialog 1 → 截图
- 再显示 Dialog 2 → 截图
- 检查 Dialog 2 是否在上层

---

## 相关源代码行数总结

| 功能 | 文件 | 起始行 | 类/函数 |
|------|------|--------|--------|
| Dialog API | Dialog.kt | 203 | fun Dialog() |
| DialogProperties | Dialog.kt | 76 | fun DialogProperties() |
| DialogLayout | Dialog.kt | 294 | fun DialogLayout() |
| Dialog 插槽注册 | Dialog.kt | 313 | slotProvider.addSlot() |
| SlotProvider 类 | SlotProvider.kt | 24 | class SlotProvider |
| 插槽渲染 | ComposeContainer.kt | 288 | slots.forEach() |
| BaseComposeScene 渲染 | BaseComposeScene.kt | 187 | fun render() |
| ModalView | ModalView.kt | 42 | class ModalView |
| 双层级机制 | ModalView.kt | 68 | fun willInit() |
| KuiklyComposeScene | KuiklyComposeScene.kt | 35 | fun KuiklyComposeScene() |
| RootNodeOwner | RootNodeOwner.kt | 67 | class RootNodeOwner |

---

## 总结：为什么是插槽系统而非 WindowManager？

1. **跨平台一致性** ✓
   - iOS/HarmonyOS 没有 WindowManager
   - 统一使用插槽系统

2. **权限隔离** ✓
   - 不需要 SYSTEM_ALERT_WINDOW 权限
   - Dialog 在 Activity 内部

3. **性能** ✓
   - 不需要额外的窗口创建
   - 直接操作视图树

4. **易用性** ✓
   - API 简洁（只需 Dialog { } ）
   - 框架自动处理层级

---

**这份快速指南总结了 KuiklyUI Dialog/Popup 的核心机制。**
**如需深入，查看完整研究报告。**

# KuiklyUI Compose Dialog 和 Popup 窗口分层研究报告

## 研究时间
2026-03-25

## 研究概览
本研究分析了 KuiklyUI 中 Compose Dialog 和 Popup 的窗口创建和分层机制，特别关注 Android、iOS 和 HarmonyOS 三个平台的实现差异。

---

## 1. Compose Dialog 窗口创建机制

### 1.1 Dialog 的架构

**文件**: `/compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/window/Dialog.kt`

Dialog 采用**插槽（Slot）系统**而非传统的 WindowManager 添加新窗口：

```kotlin
@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) = DialogLayout(
    modifier = Modifier.semantics { dialog() },
    onDismissRequest = onDismissRequest,
    properties = properties.asKuiklyDialogProperties(),
    content = content
)
```

### 1.2 核心分层组件：SlotProvider

**文件**: `/compose/src/commonMain/kotlin/com/tencent/kuikly/compose/container/SlotProvider.kt`

```kotlin
class SlotProvider {
    private var nextSlotId = 0
    private val _slots = mutableStateListOf<Pair<Int, (@Composable () -> Unit)?>>()
    val slots: List<Pair<Int, (@Composable () -> Unit)?>> get() = _slots

    fun addSlot(content: @Composable () -> Unit): Int {
        val id = nextSlotId++
        _slots.add(id to content)  // 添加到末尾 = 最后渲染 = 最顶层
        return id
    }

    fun removeSlot(slotId: Int) {
        _slots.removeAll { it.first == slotId }
    }
}
```

**关键特性**：
- Dialog 通过 `addSlot()` 添加到 `_slots` 列表末尾
- 插槽按添加顺序渲染，后添加的在顶层
- 这是一个**渐进式堆栈**，而非 WindowManager 的 z-index

### 1.3 Dialog 中的插槽使用

在 `DialogLayout` composable 中：

```kotlin
@Composable
private fun DialogLayout(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    properties: KuiklyDialogProperties,
    content: @Composable () -> Unit
) {
    val currentContent by rememberUpdatedState(content)
    val slotProvider = LocalSlotProvider.current  // 获取当前插槽提供者

    DisposableEffect(Unit) {
        // 插槽内容
        slotId = slotProvider.addSlot {  // ← Dialog 添加到插槽
            ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
                factory = {
                    KNode(ModalView().also {
                        it.inWindow = currentProperties.inWindow  // ← 关键参数
                    }) {
                        getViewEvent().willDismiss {
                            backPressedDispatcher.onBackPressedDispatcher.dispatchOnBackEvent()
                        }
                    }
                },
                // ...
            )
        }

        onDispose {
            slotProvider.removeSlot(slotId)
        }
    }
}
```

### 1.4 ModalView 的双层级机制

**文件**: `/core/src/commonMain/kotlin/com/tencent/kuikly/core/views/ModalView.kt`

```kotlin
class ModalView : ViewContainer<ContainerAttr, ModalEvent>() {
    /* 层级是否顶层，和屏幕等大 */
    var inWindow: Boolean = false
        set(value) {
            if (PagerManager.getCurrentPager().pageData.nativeBuild >= MIN_BUILD_VERSION) {
                field = value
            }
        }

    override fun willInit() {
        super.willInit()
        if (inWindow) {
            // 情况1: inWindow = true
            // Dialog 使用 TYPE_MODAL_VIEW，直接渲染在最高层
            attr {
                absolutePosition(top = 0f, left = 0f)
                width(if (pagerData.activityWidth > 0f) pagerData.activityWidth else pagerData.deviceWidth)
                height(if (pagerData.activityHeight > 0f) pagerData.activityHeight else pagerData.deviceHeight)
            }
            event {
                click { }  // 避免手势穿透
            }
        } else {
            // 情况2: inWindow = false
            // Dialog 在 ModalContentView 中，通过 insertDomSubView 控制层级
            contentView = ModalContentView()
            contentView?.also {
                currentWindow().addChild(it) {
                    attr {
                        absolutePosition(top = 0f, left = 0f, bottom = 0f, right = 0f)
                    }
                }
            }
        }
    }

    override fun didInit() {
        super.didInit()
        if (!inWindow) {
            contentView?.also {
                val index = currentWindow().domChildren().indexOf(it)
                currentWindow().insertDomSubView(it, index)  // ← 插入 DOM 子视图
            }
        }
    }
}
```

---

## 2. 插槽系统的分层渲染

### 2.1 ComposeContainer 中的槽位渲染

**文件**: `/compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ComposeContainer.kt`

```kotlin
@Composable
internal fun ProvideContainerCompositionLocals(content: @Composable () -> Unit) {
    val slotProvider = remember { SlotProvider() }
    CompositionLocalProvider(
        // ... 其他 CompositionLocal
        LocalSlotProvider provides slotProvider,
        // ...
    ) {
        content()  // 渲染主内容
        
        // ← 关键：Dialog 插槽在主内容之后渲染
        LocalSlotProvider.current.slots.forEach { slotContent ->
            key(slotContent.first) {
                slotContent.second?.invoke()  // 按顺序渲染所有 Dialog
            }
        }
    }
}
```

**渲染顺序**：
1. 主页面内容 (content)
2. 按添加顺序的所有 Dialog （从第一个到最后一个）
3. **最后添加的 Dialog 最顶层** ✓

### 2.2 BaseComposeScene 的渲染流程

**文件**: `/compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/scene/BaseComposeScene.kt`

```kotlin
override fun render(
    canvas: Canvas?,
    nanoTime: Long,
) {
    return postponeInvalidation {
        val profilerEnabled = RecompositionProfiler.isEnabled
        val tracker = if (profilerEnabled) RecompositionProfiler.tracker else null
        val frameSampled = tracker?.onFrameStart() ?: false

        recomposer.performScheduledTasks()          // 1. 准备任务
        frameClock.sendFrame(nanoTime)              // 2. 重组（Dialog 插槽注册）
        doLayout()                                   // 3. 布局
        recomposer.performScheduledEffects()        // 4. 副作用
        
        if (frameSampled) {
            tracker?.onFrameEnd(0)
        }

        inputHandler.updatePointerPosition()        // 5. 指针位置
        snapshotInvalidationTracker.onDraw()
        draw(KuiklyCanvas())                        // 6. 绘制（按层级顺序）
    }
}
```

---

## 3. 平台特定的窗口实现

### 3.1 Android 平台 (Native Render)

**关键文件**: 
- `/core-render-android/src/main/java/com/tencent/kuikly/core/render/android/expand/KuiklyRenderViewBaseDelegator.kt`
- `/core-render-android/src/main/java/com/tencent/kuikly/core/render/android/expand/module/KRKeyboardModule.kt`

**Android 的实现特点**：

1. **不使用 WindowManager.TYPE_APPLICATION**：
   - KuiklyUI 没有使用 Android 原生的 `WindowManager.LayoutParams.TYPE_APPLICATION` 或其他类型标志
   - 所有 Dialog/Popup 都在同一个 Activity 的 ViewGroup 中渲染

2. **基于 ViewGroup 层级**：
   - Dialog 添加为 `ModalView` (KNode<ModalView>)
   - 通过 `addView()` 添加到 ViewGroup
   - 层级通过 `ViewGroup.addView(child, index)` 的 `index` 参数控制
   - **后添加的 view 在 ViewGroup 的 children 列表后面 = 最后绘制 = 最顶层**

```kotlin
// 从 ModalView.didInit()
currentWindow().insertDomSubView(it, index)  // 在 DOM 树中重新排序
```

3. **KeyboardModule 中的窗口软键盘处理**：
```kotlin
// WindowManager 仅用于键盘处理，不用于 Dialog 窗口
val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
val softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
```

### 3.2 iOS 平台

**推断**：
- KuiklyUI 是跨平台框架，iOS 部分使用 Swift/Objective-C
- 所有 UI 组件（Dialog、Popup）最终渲染到单一 UIViewController 的 view
- 通过 CALayer 的 `zPosition` 或 `addSubview()` 的顺序控制层级

### 3.3 HarmonyOS 平台

**关键文件**: `/ohosApp/entry/src/main/ets/`

**推断**：
- HarmonyOS 使用 ArkTS/ETS 编写
- Dialog 通过 ArkUI 的 `@CustomDialog` 装饰器或 `AlertDialog` API
- 层级通过 Stack 组件或 z-index 属性控制

---

## 4. Compose Dialog 和 Popup 的 Z-Order 机制

### 4.1 当前 Z-Order 实现

**机制**：插槽堆栈（Slot Stack）

| 渲染级别 | 内容 | Z-Order |
|---------|------|---------|
| 0（最底） | 页面主内容 | 最低 |
| 1 | 第一个 Dialog（添加的第一个）| 低 |
| 2 | 第二个 Dialog（添加的第二个）| 中 |
| N（最顶） | 最后一个 Dialog（最后添加）| **最高** ✓ |

**证据**：
```kotlin
// ComposeContainer.kt 第288-292行
LocalSlotProvider.current.slots.forEach { slotContent ->
    key(slotContent.first) {
        slotContent.second?.invoke()  // 循序遍历，后面的后渲染
    }
}

// SlotProvider.kt 第29-33行
fun addSlot(content: @Composable () -> Unit): Int {
    val id = nextSlotId++
    _slots.add(id to content)  // 添加到末尾
    return id
}
```

### 4.2 当前实现的限制

**问题**：
- ✗ 只支持相对 Dialog 之间的 z-order
- ✗ 无法保证 Overlay Popup 总是在所有 Dialog 之上
- ✗ 无法指定绝对 z-index 值
- ✗ 没有跨度量级的优先级系统

---

## 5. 如何保证 Overlay Popup 总是最顶层

### 5.1 推荐方案 A：Dialog Properties 扩展（最简单）

修改 `DialogProperties` 支持优先级参数：

```kotlin
@Immutable
class KuiklyDialogProperties(
    // ... 现有参数
    val zIndexPriority: Int = 0,  // 新增：0=普通，1000=profiler overlay
    // ...
) : DialogProperties

// Profiler overlay Dialog 使用
Dialog(
    onDismissRequest = {},
    properties = DialogProperties().copy(
        zIndexPriority = 1000  // 最高优先级
    ),
    content = { ProfilerOverlay() }
)
```

在 `ComposeContainer.ProvideContainerCompositionLocals()` 中：
```kotlin
// 按优先级排序后渲染
LocalSlotProvider.current.slots
    .sortedBy { 
        (it.second as? Dialog)?.properties?.zIndexPriority ?: 0 
    }
    .forEach { slotContent ->
        key(slotContent.first) {
            slotContent.second?.invoke()
        }
    }
```

### 5.2 推荐方案 B：专用 OverlaySlot（更灵活）

在 SlotProvider 中添加专用的 overlay 槽位：

```kotlin
class SlotProvider {
    private val _slots = mutableStateListOf<Pair<Int, (@Composable () -> Unit)?>>()
    private val _overlaySlots = mutableStateListOf<Pair<Int, (@Composable () -> Unit)?>>()

    fun addOverlaySlot(content: @Composable () -> Unit): Int {
        val id = nextSlotId++
        _overlaySlots.add(id to content)  // 始终在最顶层
        return id
    }

    val allSlots: List<Pair<Int, (@Composable () -> Unit)?>>
        get() = _slots + _overlaySlots  // overlay 始终在后面
}
```

在渲染中：
```kotlin
// ComposeContainer.ProvideContainerCompositionLocals()
LocalSlotProvider.current.allSlots.forEach { slotContent ->
    key(slotContent.first) {
        slotContent.second?.invoke()
    }
}
```

### 5.3 推荐方案 C：基于 Window Type（最接近原生）

在 ModalView 中扩展窗口类型：

```kotlin
enum class ModalType {
    NORMAL,           // 普通 Dialog
    SYSTEM_ALERT,     // 系统提醒
    PROFILER_OVERLAY  // Profiler overlay (最顶层)
}

class ModalView : ViewContainer<ContainerAttr, ModalEvent>() {
    var modalType: ModalType = ModalType.NORMAL

    override fun willInit() {
        when (modalType) {
            ModalType.PROFILER_OVERLAY -> {
                // 使用专用的最高层标记
                attr { 
                    zIndex(Int.MAX_VALUE)  // 在 CSS 中使用最高 z-index
                }
            }
            else -> { /* 现有逻辑 */ }
        }
    }
}
```

---

## 6. 每个平台的具体实现路径

### 6.1 Android 平台

```
ComposeContainer.ProvideContainerCompositionLocals()
    └─> LocalSlotProvider.current.slots.forEach()
        └─> Dialog(...)
            └─> DialogLayout(...)
                └─> slotProvider.addSlot()
                    └─> ReusableComposeNode { KNode(ModalView) }
                        └─> createRenderView()  [Native Render]
                            └─> KRDivView.addView() [最终添加到 Android ViewGroup]
                                └─> 按 index 添加，后添加的在上层
```

**关键控制点**：
- `ModalView.willInit()` 中的 `currentWindow().insertDomSubView(it, index)`
- Native Render 中 ViewGroup 的 `addView(child, index)`

### 6.2 iOS 平台

```
ComposeContainer.ProvideContainerCompositionLocals()
    └─> Dialog(...)
        └─> ModalView
            └─> createRenderView() [iOS Native Bridge]
                └─> UIView.addSubview() 或 CALayer 操作
                    └─ zPosition 或顺序决定层级
```

### 6.3 HarmonyOS 平台

```
ComposeContainer.ProvideContainerCompositionLocals()
    └─> Dialog(...)
        └─> ModalView
            └─> createRenderView() [HarmonyOS Native Bridge]
                └─> Stack(zIndex = ...) 或 AlertDialog
                    └─ zIndex 参数决定层级
```

---

## 7. 关键代码位置总结

| 机制 | 文件 | 行号 | 作用 |
|------|------|------|------|
| Dialog 创建 | Dialog.kt | 203-212 | Dialog composable 入口 |
| 插槽管理 | SlotProvider.kt | 24-38 | 管理 Dialog 堆栈 |
| 插槽渲染 | ComposeContainer.kt | 288-292 | 按顺序渲染 Dialog |
| ModalView | ModalView.kt | 42-150 | Dialog 的核心视图 |
| insertDomSubView | ViewContainer.kt | ~200 | DOM 层级控制 |
| BaseComposeScene | BaseComposeScene.kt | 187-234 | 渲染管道 |
| KuiklyComposeScene | KuiklyComposeScene.kt | 35-147 | 场景实现 |

---

## 8. 建议和结论

### 8.1 无需特殊权限

✓ **好消息**：KuiklyUI 的 Dialog/Popup 不需要任何特殊的 Android 权限（如 `SYSTEM_ALERT_WINDOW`）
- 所有 Dialog 都在 Activity 的 ViewGroup 中
- 通过插槽系统管理 z-order
- Overlay Popup 可以通过扩展插槽系统实现最顶层渲染

### 8.2 实现建议

**推荐**：方案 A（Dialog Properties 扩展）
- 最小化改动
- 与现有架构兼容
- 支持灵活的优先级系统
- 易于扩展

### 8.3 验证点

在实现 Overlay Popup 时验证以下内容：

1. ✓ Dialog 在插槽列表中的顺序
2. ✓ ProvideContainerCompositionLocals 的渲染顺序
3. ✓ ModalView 的 z-index 属性
4. ✓ 在三个平台（Android/iOS/HarmonyOS）上的一致性
5. ✓ 与触摸事件处理的兼容性（确保 Overlay 捕获点击）

---

## 附录：相关类和接口

### DialogProperties
```kotlin
@Immutable
interface DialogProperties {
    val dismissOnBackPress: Boolean get() = true
    val dismissOnClickOutside: Boolean get() = true
    val usePlatformDefaultWidth: Boolean get() = true
    val inWindow: Boolean get() = true
}
```

### ModalView 的 inWindow 参数含义
- `true`: Dialog 使用全屏 ModalView，type = TYPE_MODAL_VIEW
- `false`: Dialog 使用 ModalContentView，通过 DOM 层级控制

### ComposeScene 的角色
- 单一的 Compose 渲染入口
- 管理重组、布局、绘制的完整流程
- 与 KuiklyUI 的 Native Render 桥接

---

**报告完成**

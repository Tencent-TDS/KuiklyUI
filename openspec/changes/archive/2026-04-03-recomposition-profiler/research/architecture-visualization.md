# KuiklyUI 窗口分层架构可视化

## 架构图 1：整体渲染流程

```
┌────────────────────────────────────────────────────────────────┐
│                    Compose 业务逻辑                              │
│  (页面内容 + Dialog 创建 + Popup 创建)                          │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│              ComposeContainer.setContent()                      │
│  设置 Compose 顶级内容，建立插槽系统                           │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│        ProvideContainerCompositionLocals()                       │
│                                                                  │
│  ┌─────────────────────────────────────────────────────┐      │
│  │ CompositionLocalProvider:                            │      │
│  │   • LocalSlotProvider provides slotProvider          │      │
│  │   • LocalOnBackPressedDispatcherOwner                │      │
│  │   • ... (其他 locals)                               │      │
│  └──────────────────┬──────────────────────────────────┘      │
│                     │                                          │
│                     ▼                                          │
│  ┌─────────────────────────────────────────────────────┐      │
│  │ 1️⃣ 渲染主内容：content()                             │      │
│  │    (页面主体)                                        │      │
│  └─────────────────────────────────────────────────────┘      │
│                                                                  │
│  ┌─────────────────────────────────────────────────────┐      │
│  │ 2️⃣ 渲染所有 Dialog/Popup 插槽：                      │      │
│  │    LocalSlotProvider.current.slots.forEach {        │      │
│  │      slotContent.second?.invoke()  // 按顺序渲染    │      │
│  │    }                                                 │      │
│  │                                                      │      │
│  │    ├─ Dialog 1（首先添加）← z-index 低             │      │
│  │    ├─ Dialog 2（其次添加）← z-index 中             │      │
│  │    └─ Dialog N（最后添加）← z-index 最高 ✓        │      │
│  └─────────────────────────────────────────────────────┘      │
└────────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│           BaseComposeScene.render()                             │
│                                                                  │
│  recomposer.performScheduledTasks()                            │
│            ▼                                                    │
│  frameClock.sendFrame()       ← Dialog 插槽在此注册            │
│            ▼                                                    │
│  doLayout()                   ← 计算大小和位置                │
│            ▼                                                    │
│  recomposer.performScheduledEffects()                          │
│            ▼                                                    │
│  draw(canvas)                 ← 按层级顺序绘制                │
└────────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │   最终画布     │
                    │  Render Image  │
                    └───────────────┘
```

## 架构图 2：插槽堆栈与 Z-Order

```
时间流 ────────────────────────────────────────────────────────────>

用户操作：   显示Dialog1   显示Dialog2   显示Dialog3   关闭Dialog2
   │           │            │            │            │
   ▼           ▼            ▼            ▼            ▼

SlotProvider._slots 状态变化：

初始状态：
┌─────────────────────────┐
│ [] (空)                 │
└─────────────────────────┘

插槽1添加后（显示Dialog1）：
┌─────────────────────────┐
│ [(id1, Dialog1)]        │  ← Dialog1 在顶层 z-index=1
└─────────────────────────┘

插槽2添加后（显示Dialog2）：
┌─────────────────────────┐
│ [(id1, Dialog1)]        │  ← Dialog1 现在在下层 z-index=1
│ [(id2, Dialog2)]        │  ← Dialog2 在顶层 z-index=2  ✓
└─────────────────────────┘

插槽3添加后（显示Dialog3）：
┌─────────────────────────┐
│ [(id1, Dialog1)]        │  ← z-index=1
│ [(id2, Dialog2)]        │  ← z-index=2
│ [(id3, Dialog3)]        │  ← Dialog3 在顶层 z-index=3  ✓
└─────────────────────────┘

移除插槽2后（关闭Dialog2）：
┌─────────────────────────┐
│ [(id1, Dialog1)]        │  ← z-index=1
│ [(id3, Dialog3)]        │  ← Dialog3 仍在顶层 z-index=2  ✓
└─────────────────────────┘

关键特性：后添加 = 后渲染 = 最顶层 ✓
```

## 架构图 3：Dialog 创建流程

```
Dialog(
    onDismissRequest = {},
    properties = DialogProperties(),
    content = { /* 内容 */ }
)
    │
    ▼
DialogLayout(
    modifier = Modifier.semantics { dialog() },
    onDismissRequest = onDismissRequest,
    properties = properties.asKuiklyDialogProperties(),
    content = content
)
    │
    ▼
DisposableEffect(Unit) {
    slotId = slotProvider.addSlot {      ← 在组件挂载时添加到插槽
        ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
            factory = {
                KNode(ModalView().also {
                    it.inWindow = currentProperties.inWindow
                }) {
                    getViewEvent().willDismiss {
                        backPressedDispatcher.onBackPressedDispatcher.dispatchOnBackEvent()
                    }
                }
            },
            update = { /* 更新 */ },
            content = {
                DialogContent(
                    properties = currentProperties,
                    onDismissRequest = { onDismissRequest() },
                    content = currentContent
                )
            }
        )
    }

    onDispose {
        slotProvider.removeSlot(slotId)   ← 组件卸载时从插槽移除
    }
}
```

## 架构图 4：ModalView 的双层级机制

```
ModalView 创建时：

┌──────────────────────────────────────────────────────────┐
│ ModalView.willInit()                                     │
│                                                          │
│ if (inWindow) {                                          │
│   ┌────────────────────────────────────────────────┐    │
│   │ 情况1：inWindow = true                          │    │
│   │                                                │    │
│   │ attr {                                         │    │
│   │   absolutePosition(top=0, left=0)             │    │
│   │   width(screenWidth)                          │    │
│   │   height(screenHeight)                        │    │
│   │ }                                              │    │
│   │                                                │    │
│   │ Native Render:                                 │    │
│   │   viewName() = ViewConst.TYPE_MODAL_VIEW      │    │
│   │            ↓                                   │    │
│   │   渲染为: KRModalView (Android native)        │    │
│   │            ↓                                   │    │
│   │   按 zIndex 排序在 ViewGroup 中               │    │
│   │   (后添加的 view 在上层)                      │    │
│   └────────────────────────────────────────────────┘    │
│                                                          │
│ } else {                                                │
│   ┌────────────────────────────────────────────────┐    │
│   │ 情况2：inWindow = false                         │    │
│   │                                                │    │
│   │ contentView = ModalContentView()              │    │
│   │ currentWindow().addChild(contentView)         │    │
│   │            ↓                                   │    │
│   │ didInit() 中：                                │    │
│   │   currentWindow().insertDomSubView(          │    │
│   │     contentView, index                        │    │
│   │   )                                            │    │
│   │            ↓                                   │    │
│   │ DOM 树中的位置决定了渲染顺序                 │    │
│   └────────────────────────────────────────────────┘    │
│ }                                                       │
└──────────────────────────────────────────────────────────┘
```

## 架构图 5：平台特定实现路径

### Android 平台

```
SlotProvider._slots.forEach()
    │
    ▼
Dialog() Composable
    │
    ▼
DialogLayout { slotProvider.addSlot() }
    │
    ▼
KNode(ModalView)
    │
    ▼
ModalView.createRenderView()
    │
    ▼ [Native Render 桥接]
    │
KRDivView (Android native ViewGroup)
    │
    ▼
KRRenderView.addView(child, index)
    │
    ▼
ViewGroup.onDraw()
    │
    ▼
后添加的 view 后绘制 ← 这就是 z-order!
```

### iOS 平台 (推断)

```
SlotProvider._slots.forEach()
    │
    ▼
Dialog() Composable
    │
    ▼
ModalView
    │
    ▼ [iOS Native Bridge]
    │
UIViewController.view.addSubview(childView)
    │
    ▼
CALayer.zPosition 或
按 addSubview 顺序排序
    │
    ▼
后添加的 subview 在上层
```

### HarmonyOS 平台 (推断)

```
SlotProvider._slots.forEach()
    │
    ▼
Dialog() Composable
    │
    ▼
ModalView
    │
    ▼ [HarmonyOS Native Bridge]
    │
ArkUI Stack { zIndex = ... }
或 AlertDialog
    │
    ▼
zIndex 较大的组件在上层
```

## 架构图 6：当前限制与解决方案对比

```
┌─────────────────────────────────────────────────────────┐
│              当前实现的限制                              │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  问题1: Dialog 之间的 z-order 由添加顺序决定           │
│         → 无法指定绝对优先级                           │
│                                                         │
│  问题2: 无法保证 Profiler Overlay 总在最顶层          │
│         → 业务 Dialog 后添加就会盖住 Overlay           │
│                                                         │
│  问题3: SlotProvider 是全局的，没有优先级系统         │
│         → 同一组件树中所有 Dialog 共享 slots           │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│            解决方案 A：优先级排序（推荐）              │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  修改 SlotProvider：                                   │
│  ┌─────────────────────────────────────────────────┐  │
│  │ fun addSlot(                                    │  │
│  │   content: @Composable () -> Unit,             │  │
│  │   priority: Int = 0                            │  │
│  │ ): Int {                                        │  │
│  │   _slots.add(id to (content to priority))      │  │
│  │   return id                                     │  │
│  │ }                                                │  │
│  └─────────────────────────────────────────────────┘  │
│                                                         │
│  渲染时按优先级排序：                                  │
│  ┌─────────────────────────────────────────────────┐  │
│  │ slots.sortedBy { it.second }                    │  │
│  │   .forEach { (id, content, priority) ->        │  │
│  │     content()                                   │  │
│  │   }                                              │  │
│  └─────────────────────────────────────────────────┘  │
│                                                         │
│  优点：                                                 │
│  ✓ 最小改动                                            │
│  ✓ 支持灵活的优先级系统                                │
│  ✓ 无需修改 Dialog API                                │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│            解决方案 B：OverlaySlot（更灵活）            │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │ class SlotProvider {                            │  │
│  │   private val _slots = mutableListOf()          │  │
│  │   private val _overlaySlots = mutableListOf()  │  │
│  │                                                 │  │
│  │   fun addOverlaySlot(content): Int {           │  │
│  │     _overlaySlots.add(id to content)           │  │
│  │     return id                                   │  │
│  │   }                                              │  │
│  │                                                 │  │
│  │   val allSlots: List<...>                      │  │
│  │     get() = _slots + _overlaySlots             │  │
│  │ }                                                │  │
│  └─────────────────────────────────────────────────┘  │
│                                                         │
│  渲染时：                                               │
│  ┌─────────────────────────────────────────────────┐  │
│  │ provider.allSlots.forEach { slot ->             │  │
│  │   slot.content()                                │  │
│  │ }                                                │  │
│  │ // _overlaySlots 中的内容始终在最后渲染       │  │
│  └─────────────────────────────────────────────────┘  │
│                                                         │
│  优点：                                                 │
│  ✓ 清晰的语义区分                                      │
│  ✓ OverlaySlot 始终在最顶层                           │
│  ✓ 易于维护和扩展                                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## 架构图 7：关键代码控制点

```
┌─────────────────────────────────────────────┐
│      Dialog 显示时序图                      │
└─────────────────────────────────────────────┘

用户代码：
  Dialog(onDismissRequest = {}, content = { ... })
      │
      ▼─ 调用 DialogLayout()

DialogLayout 中的关键点1：
  DisposableEffect(Unit) {                    ← 组件首次挂载
    slotId = slotProvider.addSlot { ... }    ← 添加到插槽
    
    onDispose {
      slotProvider.removeSlot(slotId)        ← 组件卸载时移除
    }
  }
      │
      ▼

关键点2：
  ComposeContainer.ProvideContainerCompositionLocals()
      │
      ▼
  LocalSlotProvider.current.slots.forEach {   ← 渲染所有插槽
    slotContent.second?.invoke()
  }
      │
      ▼

关键点3：
  BaseComposeScene.render()
      │
      ▼
  frameClock.sendFrame(nanoTime)              ← 触发重组
      │
      ▼
  draw(canvas)                                 ← 按插槽顺序绘制
      │
      ▼

最终结果：
  Dialog 在画布上显示（z-order由插槽顺序决定）
```

---

## 总结表格

| 层级 | 组件 | 文件 | 职责 |
|-----|------|------|------|
| 1 | Dialog() | Dialog.kt | Composable API |
| 2 | DialogLayout() | Dialog.kt | 插槽管理 |
| 3 | SlotProvider | SlotProvider.kt | 维护插槽堆栈 |
| 4 | ComposeContainer | ComposeContainer.kt | 渲染插槽 |
| 5 | BaseComposeScene | BaseComposeScene.kt | 渲染管道 |
| 6 | ModalView | ModalView.kt | 视图模型 |
| 7 | Native Render | KRDivView 等 | 平台特定实现 |


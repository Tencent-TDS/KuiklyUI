# 获取组件的宽高与坐标

本节介绍如何获取 Kuikly 组件渲染后的宽高，以及如何计算组件在页面中的坐标位置。

:::tip 优先使用布局
大部分场景下，你不需要手动获取组件的宽高或坐标。FlexBox 布局（如 `allCenter()`、`alignItems`、`absolutePosition` 等）能自动处理对齐和定位，无需知道组件的具体尺寸。

如果你的需求是让背景自适应内容大小、让元素居中、或根据内容自动撑开容器，请优先参考 [FlexBox 布局](flexbox-basic.md)。只有在确实需要**动态读取**渲染结果时，才使用本节介绍的 API。
:::

## 获取组件渲染后的宽高

### layoutFrameDidChange 事件

在组件的 `event {}` 闭包中监听 `layoutFrameDidChange`，当组件布局完成后会收到一个 `Frame` 回调，包含组件的位置和尺寸信息：

```kotlin
@Page("size_demo")
internal class SizeDemoPage : BasePager() {

    private var boxWidth by observable(0f)
    private var boxHeight by observable(0f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }
            View {
                attr {
                    flex(1f)
                    backgroundColor(Color.BLUE)
                }
                event {
                    layoutFrameDidChange { frame ->
                        ctx.boxWidth = frame.width
                        ctx.boxHeight = frame.height
                    }
                }
            }
            Text {
                attr {
                    text("宽: ${ctx.boxWidth}, 高: ${ctx.boxHeight}")
                    fontSize(14f)
                    color(Color.BLACK)
                    marginTop(10f)
                }
            }
        }
    }
}
```

`Frame` 包含以下字段：

| 字段 | 描述 | 类型 |
|:-----|:-----|:-----|
| x | 组件在父容器中的 x 坐标 | Float |
| y | 组件在父容器中的 y 坐标 | Float |
| width | 组件宽度 | Float |
| height | 组件高度 | Float |

:::warning 注意
`layoutFrameDidChange` 回调中的 `frame.x` 和 `frame.y` 是**相对于父容器**的坐标，不是相对于页面根视图的。如果需要获取组件在页面中的绝对坐标，请参考下方 [获取组件在页面中的坐标](#获取组件在页面中的坐标) 章节。
:::

### 直接读取 view.frame

除了通过事件监听，也可以通过 [ViewRef](view-ref.md) 持有组件引用，在布局完成后直接读取 `frame` 属性：

```kotlin
@Page("frame_read_demo")
internal class FrameReadDemoPage : BasePager() {

    lateinit var targetRef: ViewRef<View<*, *>>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                ref {
                    ctx.targetRef = it
                }
                attr {
                    size(200f, 100f)
                    backgroundColor(Color.GREEN)
                }
            }
            View {
                attr {
                    marginTop(20f)
                }
                event {
                    click {
                        val frame = ctx.targetRef.view?.frame
                        // frame?.width -> 200.0, frame?.height -> 100.0
                    }
                }
            }
        }
    }
}
```

:::warning 时机很重要
`view.frame` 需要在布局完成后才有正确的值。如果在 `body()` 首次构建时直接读取，可能拿到的是零值。可以通过以下方式确保在正确时机读取：
- 监听 `layoutFrameDidChange` 事件
- 在 `addTaskWhenPagerUpdateLayoutFinish {}` 中读取
- 在用户交互事件（如 `click`）的回调中读取
:::

### 页面布局完成时机

如果需要在页面级别的布局完成后执行某些操作，可以使用 [Pager](../API/components/pager.md) 提供的时机方法：

```kotlin
override fun created() {
    super.created()
    addTaskWhenPagerUpdateLayoutFinish {
        // 此时所有组件的布局已完成，可以安全读取任何组件的 frame
    }
}
```

### 监听每次布局（高级）

如果需要在**每次**页面布局时都收到通知（而不是一次性任务），可以通过 `IPagerLayoutEventObserver` 接口实现。这是一个高级 API，框架内部的 `ScrollerView`、`TabsView` 等组件使用了它。

实现 `IPagerLayoutEventObserver` 接口，然后通过 `getPager().addPagerLayoutEventObserver(this)` 注册：

```kotlin
@Page("layout_observer_demo")
internal class LayoutObserverDemoPage : BasePager() {

    override fun body(): ViewBuilder {
        return {
            // ...
        }
    }
}

internal class MyComposeView : ComposeView<MyComposeViewAttr, ComposeEvent>(),
    IPagerLayoutEventObserver {

    override fun didMoveToParentView() {
        super.didMoveToParentView()
        getPager().addPagerLayoutEventObserver(this)
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        getPager().removePagerLayoutEventObserver(this) // 必须移除，否则内存泄漏
    }

    override fun onPagerWillCalculateLayoutFinish() {
        // 布局计算即将开始前调用
    }

    override fun onPagerCalculateLayoutFinish() {
        // 布局计算完成后调用（此时 frame 已更新，但尚未同步到渲染层）
    }

    override fun onPagerDidLayout() {
        // 布局完成并同步到渲染层后调用
    }
}
```

三个回调在每次布局循环中的调用顺序为：

1. `onPagerWillCalculateLayoutFinish` — 布局计算前
2. `onPagerCalculateLayoutFinish` — 布局计算后
3. `onPagerDidLayout` — 布局结果同步到渲染层后

:::danger 使用注意
1. **死循环风险**：`IPagerLayoutEventObserver` 的回调在**每次布局**时都会触发。如果在回调中执行了会导致布局脏标记（markDirty）的操作——例如修改组件的尺寸、边距、增删子节点等——将触发新的布局循环，形成**无限循环**。框架内部有最大循环次数保护（单次 `layoutIfNeed` 最多 3 轮），超出后会延迟到下一帧继续布局，不会导致应用卡死，但会造成严重的性能问题和布局抖动。**回调中只能读取布局结果，绝对不能写入会触发重新布局的属性。**

2. **必须移除监听**：在组件移除时（`didRemoveFromParentView` 或 `willRemoveFromParentView`）必须调用 `removePagerLayoutEventObserver` 移除监听器，否则会造成**内存泄漏**，并且已销毁的组件仍会收到回调，可能引发异常。
:::

:::tip 优先选择更简单的 API
大多数场景下，`layoutFrameDidChange` 事件或 `addTaskWhenPagerUpdateLayoutFinish` 就足够了。只有在需要对**每次页面级布局**做出响应的自定义组件中，才需要使用 `IPagerLayoutEventObserver`。
:::

## 获取组件在页面中的坐标

`frame.x` / `frame.y` 只是组件相对于直接父容器的坐标。如果组件被嵌套在多层容器中，想知道它在**页面根视图中的绝对坐标**，需要使用 `convertFrame` 方法。

### convertFrame 方法

`convertFrame` 可以将组件的坐标从当前坐标系转换到任意目标节点的坐标系：

```kotlin
fun convertFrame(frame: Frame, toView: ViewContainer<*, *>?): Frame
```

- `frame`：要转换的坐标（通常传入组件自身的 `frame`）
- `toView`：目标坐标系对应的节点。**传 `null` 表示转换到页面根视图（Pager）坐标系**
- 返回值：转换后的 `Frame`，其中 `x`、`y` 为组件在目标坐标系中的位置

:::warning 限制
`convertFrame` 通过逐级累加父容器的偏移量来计算坐标，**不考虑 `transform`（旋转、缩放等）的影响**。如果父容器链上有 `transform` 变换，计算结果可能不准确。
:::

### 示例：获取组件在页面中的绝对坐标

```kotlin
@Page("position_demo")
internal class PositionDemoPage : BasePager() {

    private var absoluteX by observable(0f)
    private var absoluteY by observable(0f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            View {
                attr {
                    paddingTop(100f)
                    paddingLeft(50f)
                }
                View {
                    attr {
                        paddingTop(30f)
                        paddingLeft(20f)
                    }
                    View {
                        attr {
                            size(80f, 80f)
                            backgroundColor(Color.RED)
                        }
                        event {
                            layoutFrameDidChange { frame ->
                                // frame.x = 0, frame.y = 0（相对于直接父容器）
                                // 转换到页面根视图坐标系
                                val frameInPage = convertFrame(frame, null)
                                ctx.absoluteX = frameInPage.x // 50 + 20 = 70
                                ctx.absoluteY = frameInPage.y // 100 + 30 = 130
                            }
                        }
                    }
                }
            }
            Text {
                attr {
                    text("绝对坐标: (${ctx.absoluteX}, ${ctx.absoluteY})")
                    fontSize(14f)
                    color(Color.BLACK)
                }
            }
        }
    }
}
```

### 示例：转换到指定容器的坐标系

`convertFrame` 也支持转换到任意指定容器的坐标系，而不仅仅是 Pager 根节点：

```kotlin
@Page("convert_demo")
internal class ConvertDemoPage : BasePager() {

    lateinit var containerRef: ViewRef<View<*, *>>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                ref {
                    ctx.containerRef = it
                }
                attr {
                    flex(1f)
                    paddingTop(50f)
                    paddingLeft(30f)
                }
                View {
                    attr {
                        paddingTop(20f)
                        paddingLeft(10f)
                    }
                    View {
                        attr {
                            size(60f, 60f)
                            backgroundColor(Color.BLUE)
                        }
                        event {
                            layoutFrameDidChange { frame ->
                                // 转换到 containerRef 指向的容器的坐标系
                                val target = ctx.containerRef.view as? ViewContainer<*, *>
                                val frameInContainer = convertFrame(frame, target)
                                // frameInContainer.x = 10, frameInContainer.y = 20
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### 触摸事件中的页面坐标

如果只是需要在触摸或拖拽时获取触摸点在页面中的位置，不需要手动做坐标转换。[触摸事件](event.md)的参数已经自带了 `pageX`/`pageY`（相对于页面根视图的坐标）：

```kotlin
View {
    attr {
        size(200f, 200f)
        backgroundColor(Color.GREEN)
    }
    event {
        pan { params ->
            val localX = params.x      // 相对于当前组件
            val localY = params.y
            val pageX = params.pageX   // 相对于页面根视图
            val pageY = params.pageY
        }
        click { params ->
            val pageX = params.pageX   // 相对于页面根视图
            val pageY = params.pageY
        }
    }
}
```

## 获取页面/设备尺寸

如果需要获取页面根视图或设备屏幕的尺寸，可以通过 [PageData](page-data.md) 读取：

```kotlin
val pageWidth = pagerData.pageViewWidth    // 页面根视图宽度
val pageHeight = pagerData.pageViewHeight  // 页面根视图高度
val screenWidth = pagerData.deviceWidth    // 设备屏幕宽度
val screenHeight = pagerData.deviceHeight  // 设备屏幕高度
```

## 小结

| 场景 | 推荐方案 |
|:-----|:---------|
| 获取组件渲染后的宽高 | `layoutFrameDidChange` 事件，或 `view.frame` |
| 获取组件在页面中的绝对坐标 | `convertFrame(frame, null)` |
| 获取组件在指定容器中的坐标 | `convertFrame(frame, targetView)` |
| 获取触摸点在页面中的坐标 | 事件参数 `pageX` / `pageY` |
| 获取页面/屏幕尺寸 | `pagerData.pageViewWidth` / `deviceWidth` 等 |
| 自定义组件需要响应每次页面布局 | `IPagerLayoutEventObserver`（高级，注意避免死循环） |
| 文本居中、背景自适应等布局需求 | 使用 FlexBox 布局，无需手动获取尺寸 |

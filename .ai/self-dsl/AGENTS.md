# 自研 DSL 代码模式

> 以下场景读取本文件：修改 `core/` 模块源码、使用 `Pager`/`body()`/`vfor`/`vif`/`observable`/`ObservableList`/`Color` 开发页面或组件、查询自研 DSL 组件 API、处理自研 DSL 页面的生命周期或响应式状态问题。

## 1. 页面基本结构

```kotlin
@Page("MyPage")
internal class MyPage : Pager() {

    // 响应式状态
    var title: String by observable("Hello")
    var items: ObservableList<String> by observableList()

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }
            // 子组件在这里声明
        }
    }

    // 生命周期钩子（按调用顺序）
    override fun created() { super.created() }
    override fun viewWillLoad() { super.viewWillLoad() }
    override fun viewDidLoad() { super.viewDidLoad() }
    override fun viewDidLayout() { super.viewDidLayout() }
    override fun pageDidAppear() { super.pageDidAppear() }
    override fun pageDidDisappear() { super.pageDidDisappear() }
    override fun pageWillDestroy() { super.pageWillDestroy() }
}
```

**关键点**：
- `@Page("pageName")` — KSP 自动生成注册代码
- 继承 `Pager()`，类通常声明为 `internal`
- `body()` 返回 `ViewBuilder`（即 `ViewContainer<*, *>.() -> Unit`）
- 状态用 `by observable()` / `by observableList()` 委托
- DSL 层级: `ViewName { attr { ... } event { ... } }`

**自定义可复用组件**：继承 `ComposeView<Attr, Event>`，通过扩展函数暴露 DSL 接口：

```kotlin
// 组件定义
internal class MyButton : ComposeView<MyButtonAttr, ComposeEvent>() {
    override fun createAttr() = MyButtonAttr()
    override fun createEvent() = ComposeEvent()
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr { backgroundColor(ctx.attr.bgColor); borderRadius(8f) }
                Text { attr { text(ctx.attr.text); fontSize(ctx.attr.fontSize) } }
                event { click { ctx.event.onClick?.invoke() } }
            }
        }
    }
}
internal class MyButtonAttr : ComposeAttr() {
    var text: String = ""
    var fontSize: Float = 16f
    var bgColor: Color = Color.BLUE
}

// 暴露 DSL 接口
internal fun ViewContainer<*, *>.MyButton(init: MyButton.() -> Unit) {
    addChild(MyButton(), init)
}

// 使用
MyButton {
    attr { text = "点击"; bgColor = Color.RED }
    event { onClick = { /* 处理 */ } }
}
```

## 2. 组件 API 文档

> 完整 API 见 `docs/API/components/`，下表为快速索引。

| 组件 | 分类 | 文档 |
|------|------|------|
| `View` | 基础容器 | [view.md](../../docs/API/components/view.md) |
| `Text` | 文本 | [text.md](../../docs/API/components/text.md) |
| `Image` | 图片 | [image.md](../../docs/API/components/image.md) |
| `Button` | 按钮 | [button.md](../../docs/API/components/button.md) |
| `Input` | 单行输入 | [input.md](../../docs/API/components/input.md) |
| `TextArea` | 多行输入 | [text-area.md](../../docs/API/components/text-area.md) |
| `RichText` | 富文本 | [rich-text.md](../../docs/API/components/rich-text.md) |
| `List` | 列表 | [list.md](../../docs/API/components/list.md) |
| `Scroller` | 滚动容器 | [scroller.md](../../docs/API/components/scroller.md) |
| `WaterfallList` | 瀑布流 | [waterfall-list.md](../../docs/API/components/waterfall-list.md) |
| `PageList` | 分页列表 | [page-list.md](../../docs/API/components/page-list.md) |
| `Refresh` | 下拉刷新 | [refresh.md](../../docs/API/components/refresh.md) |
| `FooterRefresh` | 上拉加载 | [footer-refresh.md](../../docs/API/components/footer-refresh.md) |
| `Modal` | 模态窗口 | [modal.md](../../docs/API/components/modal.md) |
| `AlertDialog` | 提示对话框 | [alert-dialog.md](../../docs/API/components/alert-dialog.md) |
| `ActionSheet` | 操作表 | [action-sheet.md](../../docs/API/components/action-sheet.md) |
| `Switch` | 开关 | [switch.md](../../docs/API/components/switch.md) |
| `Slider` | 滑块 | [slider.md](../../docs/API/components/slider.md) |
| `CheckBox` | 复选框 | [checkbox.md](../../docs/API/components/checkbox.md) |
| `DatePicker` | 日期选择 | [date-picker.md](../../docs/API/components/date-picker.md) |
| `ScrollPicker` | 滚动选择 | [scroll-picker.md](../../docs/API/components/scroll-picker.md) |
| `Tabs` | 标签栏 | [tabs.md](../../docs/API/components/tabs.md) |
| `SliderPage` | 轮播图 | [slider-page.md](../../docs/API/components/slider-page.md) |
| `Video` | 视频播放 | [video.md](../../docs/API/components/video.md) |
| `APNG` | APNG 动画 | [apng.md](../../docs/API/components/apng.md) |
| `PAG` | PAG 动画 | [pag.md](../../docs/API/components/pag.md) |
| `Canvas` | 自绘画布 | [canvas.md](../../docs/API/components/canvas.md) |
| `Blur` | 高斯模糊 | [blur.md](../../docs/API/components/blur.md) |
| `Mask` | 遮罩 | [mask.md](../../docs/API/components/mask.md) |
| `Hover` | 列表悬停置顶 | [hover.md](../../docs/API/components/hover.md) |
| `ActivityIndicator` | 加载指示器 | [activity-indicator.md](../../docs/API/components/activity-indicator.md) |
| LiquidGlass（iOS 26+） | 液态玻璃 | [ios26-liquid-glass.md](../../docs/API/components/ios26-liquid-glass.md) |

**通用属性与事件**：[basic-attr-event.md](../../docs/API/components/basic-attr-event.md)
**布局（Flexbox）**：[docs/DevGuide/flexbox-basic.md](../../docs/DevGuide/flexbox-basic.md)

## 3. Module API 文档

> 完整 API 见 `docs/API/modules/`。

| Module | 文档 |
|--------|------|
| NetworkModule | [network.md](../../docs/API/modules/network.md) |
| RouterModule | [router.md](../../docs/API/modules/router.md) |
| NotifyModule | [notify.md](../../docs/API/modules/notify.md) |
| MemoryCacheModule | [memory-cache.md](../../docs/API/modules/memory-cache.md) |
| CalendarModule | [calendar.md](../../docs/API/modules/calendar.md) |
| SharedPreferencesModule | [sp.md](../../docs/API/modules/sp.md) |
| SnapshotModule | [snapshot.md](../../docs/API/modules/snapshot.md) |
| CodecModule | [codec.md](../../docs/API/modules/codec.md) |
| PerformanceModule | [performance.md](../../docs/API/modules/performance.md) |

## 4. 响应式状态

```kotlin
// 单值
var count: Int by observable(0)
var name: String by observable("")

// 列表
var items: ObservableList<String> by observableList()

// 状态变更自动触发 UI 更新
fun increment() {
    count++  // 自动触发依赖 count 的 UI 节点刷新
}
```

**原理**: `ReactiveObserver` 在 `body()` 执行时自动收集属性依赖，属性变更时通知 UI 更新（类似 Vue 响应式）。

## 5. Module 使用

```kotlin
// 获取内置模块
val networkModule = acquireModule<NetworkModule>(NetworkModule.MODULE_NAME)
val notifyModule = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)

// 注册扩展模块
override fun createExternalModules(): Map<String, Module>? {
    return mapOf("MyModule" to MyModule())
}
```

## 6. 自定义组件（扩展函数模式）

```kotlin
// 定义可复用组件
fun ViewContainer<*, *>.MyCard(
    title: String,
    init: ViewContainer<*, *>.() -> Unit = {}
) {
    View {
        attr {
            backgroundColor(Color.WHITE)
            borderRadius(12f)
            padding(16f)
        }
        Text {
            attr {
                text(title)
                fontSize(18f)
                fontWeightBold()
            }
        }
        init()  // 插入自定义子内容
    }
}

// 使用
override fun body(): ViewBuilder {
    return {
        MyCard("标题") {
            Text { attr { text("内容") } }
        }
    }
}
```

## 7. 颜色 API（重要⚠️）

```kotlin
// ✅ 正确
color(Color(0xFF333333))
color(Color.parseString16ToLong("#333333"))
color(Color.WHITE)
color(Color.BLACK)

// ❌ 错误 —— Android 特有 API，KMP 中不可用
color(Color.parseColor("#333333"))
```

## 8. Demo 示例参考

> 完整示例在 `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/` 下。

| 场景 | 参考文件 |
|------|---------|
| 基础页面结构 / 响应式绑定 | `demo/HelloWorldPage.kt` |
| AlertDialog 弹窗 | `demo/AlertDIalogDemoPage.kt` |
| 动画取消 / 生命周期管理 | `demo/AnimationCancelDemo.kt` |
| Module 使用（CalendarModule） | `demo/CalendarModuleExamplePage.kt` |
| 复杂状态管理 / 动画 | `demo/SlotMachinePage.kt` |
| 边框效果 | `BorderTestPage.kt` |
| APNG 动画 | `APNGExamplePage.kt` |
| 原生调用（NativeBridge） | `NativeCallPage.kt` |
| Canvas 自绘（天气） | `WeatherCanvasPage.kt` |
| ClipPath 裁剪 | `ClipPathTestPage.kt` |
| 返回键拦截 | `BackPressHandlerPager.kt` |
| vfor 懒加载动态添加 | `vforLazyAddPage.kt` |
| 遮罩效果 | `MaskDemoPage.kt` |
| 文本后处理 | `TextPostProcessPager.kt` |
| Input + Span | `InputSpanPager.kt` |
| Diff 更新机制 | `DiffUpdateTestPage.kt` |
| 绝对布局 | `AbsLayoutFixPage.kt` |

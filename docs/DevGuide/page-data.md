# 页面数据PagerData

每个``Kuikly``页面, 都带有由``Native``侧传递过来的数据, 这份数据在``Kuikly``中被称为``PagerData``。

## 如何获取PagerData

在``Kuikly``中，你可以在``Pager``子类或者组合组件``ComposeView``子类内获取``PagerData``

* 在``Pager``子类中获取``PagerData``

```kotlin{11}
@Page("1")
internal class HelloWorldPage : Pager() {

    ...
    override fun body(): ViewBuilder {
        ...
    }

    override fun created() {
        super.created()
        val pgData = pagerData // 通过pagerData变量获取页面参数
    }
}
```

* 在组合组件子类中获取``PagerData``

```kotlin
class TestComposeView : ComposeView<ComposeAttr, ComposeEvent>() {

    ...
    override fun created() {
        super.created()
        val pgData = pagerData // 在组合组件中获取PagerData
    }
}
```

---

``PagerData``里面包含了两类参数


1. **基础参数**: 每个页面默认都带有的参数
2. **业务扩展参数**: 不同的页面，可根据业务诉求，在打开``Kuikly``页面的时候，由``Native``侧将业务数据传递给``Kuikly``页面，然后存放在``PagerData``类中, 供业务侧使用

::: tip 注意
``PagerData``数据需在Pager创建生命周期(onCreate)或之后调用，在此pager创建之前的时机访问（如全局变量初始化时获取``PagerData``数据），会触发框架保护异常：PagerNotFoundException。
:::

## PagerData基础参数

``PagerData``中的基础参数是由``Kuikly``框架传递的，默认每个页面都含有这些基础参数。

| 参数              | 描述                                                   | 类型 |
|-----------------|------------------------------------------------------|--|
| pageViewWidth   | 页面根View宽度                                            | Float |
| pageViewHeight  | 页面根View高度                                            | Float |
| statusBarHeight | 状态栏高度                                                | Float |
| deviceWidth     | 屏幕宽度                                                 | Float |
| deviceHeight    | 屏幕高度                                                 | Float |
| appVersion      | app版本号                                               | String |
| isIOS           | 是否为iOS平台                                             | Boolean |
| isMacOS         | 是否为MacOS平台                                           | Boolean |
| isIphoneX       | 是否为iphoneX机型                                         | Boolean |
| params          | 存放业务扩展的数据                                            | JSONObject |
| safeAreaInsets  | 安全区域: 被系统界面（如状态栏、导航栏、工具栏或底部 Home 指示器、刘海屏底部边距）遮挡的视图区域 | EdgeInsets |
| hingeStatus     | 铰链开合状态（仅折叠设备）：0=未知 1=合上 2=半开 3=全开 | HingeStatus |
| reservedRegions | 系统避让区域列表（仅 iOS 折叠设备）：occlusion=遮挡区（摄像头/侧边栏），division=折缝区 | List<ReservedRegion> |


:::tip 提示
iOS 端可通过实现 `viewControllerHostWindow` 方法，指定当前页面获取 safeAreaInsets 的参考窗口。这在多 Window 场景（如悬浮窗、分屏模式）确保获取到正确的安全区域。[参考示例](../QuickStart/iOS.md#实现kuikly承载容器)
:::

:::warning 版本要求
`hingeStatus` 与 `reservedRegions` 需要 **KuiklyUI 2.29.0** 及以上版本，且仅在 iOS 27.1+ 系统的 iPhone Duo 折叠设备上返回有效数据；其他平台/设备上 `hingeStatus` 为 `UNKNOWN(0)`，`reservedRegions` 为空列表，可放心直接使用。
:::

## ReservedRegion（避让区域）

系统为折叠屏设备提供的区域避让数据，坐标相对页面根视图，单位为点。

:::warning 版本要求
`ReservedRegion` 需要 **KuiklyUI 2.29.0** 及以上版本，且仅 iOS 27.1+ 的折叠设备（如 iPhone Duo）会返回数据；其他平台 / 设备上 `pageData.reservedRegions` 为空列表，可放心直接使用。
:::

| 字段        | 描述                                       | 类型    |
|-----------|------------------------------------------|-------|
| kind      | 区域类型：`OCCLUSION`（被遮挡，需让开）/ `DIVISION`（折缝，内容需分居两侧） | ReservedRegionKind |
| active    | 当前是否生效（如设备平铺时折缝为未生效）                  | Boolean |
| x, y      | 区域左上角坐标（相对页面根视图）                        | Float |
| width, height | 区域尺寸（系统给的最终避让范围，已含 margins）      | Float |
| marginTop/Left/Bottom/Right | frame 中为交互内容预留的边距（inset 掉才是「真正被挡住」的那块） | Float |

使用示例：

```kotlin
// 避让所有生效的遮挡区域
pageData.reservedRegions.forEach { region ->
    if (region.active && region.kind == ReservedRegionKind.OCCLUSION) {
        val bottom = region.y + region.height
        // 按需让开该区域
    }
}

// 判断设备是否处于半折叠（内容分居折缝两侧）
val halfFolded = pageData.hingeStatus == HingeStatus.PARTIALLY_OPEN
```

:::tip 提示
区域变化（折叠/展开/旋转）会随 `rootViewSizeDidChanged` 事件自动更新到上述字段，无需手动刷新。
:::

## PagerData业务扩展参数

``PagerData``的业务扩展参数，是``Native``侧在打开``Kuikly``页面时，传递给``Kuikly``页面的参数，这些扩展参数会被统一存放在``PagerData.params``字段中。

<br/>

例如如果``Native``在打开``Kuikly``页面的时候，传递了``test:1``数据，那在``Kuikly``页面中可以这样获取

```kotlin{10}
internal class HelloWorldPage : Pager() {

    ...
    override fun body(): ViewBuilder {
        ...
    }

    override fun created() {
        super.created()
        val test = pagerData.params.optInt("test") // 获取业务参数
    }
}
```

更好的方式是，新建``PagerData``的扩展类，然后将业务参数通过扩展的形式封装在``PagerData``扩展类中

```kotlin
// PagerDataExt.kt

internal val PageData.test: Int
    get() = params.optInt("test")
```

## 下一步

在学习了``Kuikly``的页面数据``PagerData``概念后, 下一步，我们接着学习[Pager生命周期](pager-lifecycle.md), 了解``Pager``的生命周期




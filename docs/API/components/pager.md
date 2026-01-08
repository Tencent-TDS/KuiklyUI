# Pager(页面入口)

`Pager` 是 KuiklyUI 框架的核心页面入口类，类似于 Android 的 `Activity` 和 iOS 的 `ViewController`。它作为页面的根容器，负责管理页面的生命周期、模块系统、布局计算和事件处理。

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/router_page/RouterPage.kt)

## 类概述

```kotlin
abstract class Pager : ComposeView<ComposeAttr, ComposeEvent>(), IPager
```

## 属性

### pageData
页面数据对象，包含页面的各种配置信息和参数

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| pageData | 页面数据对象 | PageData |

### pageName
页面名称，用于页面标识和路由

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| pageName | 页面名称 | String |

### lifecycleScope
页面生命周期作用域，用于Kuikly内建协程API

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| lifecycleScope | 生命周期作用域 | LifecycleScope |

### isAppeared
页面是否已显示的状态标识

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| isAppeared | 页面是否已显示 | Boolean |

### didCreateBody
页面 body 是否已创建的标识

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| didCreateBody | 页面 body 是否已创建 | Boolean |

## 方法

### getModule()
获取指定名称的模块（可能为空）

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| name | 模块名称 | String |

### acquireModule()
获取指定名称的模块（必须存在，否则抛出异常）

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| name | 模块名称 | String |

### addNextTickTask()
添加下一帧执行的任务

```kotlin
addNextTickTask {
    // 下一帧执行的任务
}
```

### addTaskWhenPagerUpdateLayoutFinish()
添加页面布局完成后执行的任务

```kotlin
addTaskWhenPagerUpdateLayoutFinish {
    // 页面布局完成后执行的任务
}
```

### addTaskWhenPagerDidCalculateLayout()
添加页面布局计算完成后执行的任务

```kotlin
addTaskWhenPagerDidCalculateLayout {
    // 页面布局计算完成后执行的任务
}
```

### setMemoryCache
设置内存缓存

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| key | 缓存键 | String |
| value | 缓存值 | Any |

### getValueForKey
获取内存缓存

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| key | 缓存键 | String |

## 可重载方法

开发者可以通过重载以下方法来自定义页面行为和响应生命周期事件。

### 生命周期方法

详细的生命周期流程请参考 [Pager生命周期](../../DevGuide/pager-lifecycle.md)。

#### body()
**必须实现**。返回页面的 UI 构建器，定义页面的视图结构。

```kotlin
override fun body(): ViewBuilder {
    return {
        attr {
            backgroundColor(Color.WHITE)
        }
        Text {
            attr {
                text("Hello Kuikly")
            }
        }
    }
}
```

#### created()
body 创建前调用，可用于初始化页面状态和数据。

```kotlin
override fun created() {
    super.created()
    // 初始化数据
    loadData()
}
```

#### willInit()
页面初始化前调用，早于 `created()`，可用于最早期的配置。

```kotlin
override fun willInit() {
    super.willInit()
    // 最早期的初始化配置
}
```

#### pageDidAppear()
页面可见时回调（类似 Android 的 `onResume` 或 iOS 的 `viewDidAppear`）。

```kotlin
override fun pageDidAppear() {
    super.pageDidAppear()
    // 页面可见时的逻辑，如开始动画、恢复播放等
}
```

#### pageDidDisappear()
页面不可见时回调（类似 Android 的 `onPause` 或 iOS 的 `viewDidDisappear`）。

```kotlin
override fun pageDidDisappear() {
    super.pageDidDisappear()
    // 页面不可见时的逻辑，如暂停动画、暂停播放等
}
```

#### pageWillDestroy()
页面将要销毁时回调，可用于清理资源。

```kotlin
override fun pageWillDestroy() {
    super.pageWillDestroy()
    // 清理资源、取消订阅等
}
```

#### onFirstFramePaint()
Native 首帧上屏后回调，可用于性能监控或首屏渲染完成后的操作。

```kotlin
override fun onFirstFramePaint() {
    super.onFirstFramePaint()
    // 首帧渲染完成后的逻辑
}
```

### 配置方法

#### createExternalModules()
创建并注册外部扩展模块，返回模块名到模块实例的映射。

```kotlin
override fun createExternalModules(): Map<String, Module>? {
    return mapOf(
        CustomModule.MODULE_NAME to CustomModule()
    )
}
```

#### debugUIInspector()
UI 视图调试开关，返回 `true` 时开启 UI 调试模式。

:::warning 注意
- 与 [debugName](./basic-attr-event.md#debugname方法) 属性互斥，二者只能选其一开启
- 该功能仅建议在开发阶段启用，**请勿在生产环境中使用**
- 启用后会关闭组件层级优化，可能影响性能
:::

| 返回值 | 描述 |
| -- | -- |
| Boolean | 是否开启 UI 调试 |

```kotlin
private var debugUIInspector: Boolean? = null
override fun debugUIInspector(): Boolean {
    if (debugUIInspector == null) {
        // 仅调试版本开启
        debugUIInspector = pageData.params.optBoolean(DEBUG_KEY)
    }
    return debugUIInspector!!
}
```

#### isNightMode()
返回当前是否为夜间模式。

| 返回值 | 描述 |
| -- | -- |
| Boolean | 是否为夜间模式 |

```kotlin
private var nightModel: Boolean? by observable(null)
override fun isNightMode(): Boolean {
    if (nightModel == null) {
        nightModel = pageData.params.optBoolean(IS_NIGHT_MODE_KEY)
    }
    return nightModel!!
}
```

#### isAccessibilityRunning()
返回当前是否有无障碍功能正在运行。

| 返回值 | 描述 |
| -- | -- |
| Boolean | 是否有无障碍功能运行 |

```kotlin
private var accessibilityRunning: Boolean? = null
override fun isAccessibilityRunning(): Boolean {
    if (accessibilityRunning == null) {
        accessibilityRunning = pageData.params.optBoolean(ACCESSIBILITY_KEY)
    }
    return accessibilityRunning!!
}
```

#### isDebugLogEnable()
页面的Debug日志开关，返回 `true` 时开启页面的Debug日志。

  :::warning 注意
- 该功能仅建议在开发阶段/排查页面问题过程启用，**请勿在生产环境中常驻使用**
- 启用后会记录页面五次layout的耗时和dump节点树，有助于排查页面是否正确建立
  :::

| 返回值 | 描述 |
| -- | -- |
| Boolean | 是否开启页面的Debug日志 |

### 事件回调方法

#### themeDidChanged()
主题改变时回调，可用于响应系统主题切换。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| data | 主题变化数据 | JSONObject |

```kotlin
override fun themeDidChanged(data: JSONObject) {
    super.themeDidChanged(data)
    // 响应主题变化，更新UI
}
```

#### onReceivePagerEvent()
接收原生侧发送的页面事件，可用于处理自定义的页面级事件。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| pagerEvent | 事件名称 | String |
| eventData | 事件数据 | JSONObject |

```kotlin
override fun onReceivePagerEvent(pagerEvent: String, eventData: JSONObject) {
    super.onReceivePagerEvent(pagerEvent, eventData)
    // 处理自定义事件
    if (pagerEvent == "customEvent") {
        // 处理自定义事件逻辑
    }
}
```

## 静态属性和方法

### VERIFY_THREAD
开启线程验证，检查UI操作是否在Kuikly线程中执行

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| VERIFY_THREAD | 线程验证开关 | Boolean |

### VERIFY_REACTIVE_OBSERVER
开启响应式观察者验证，检查响应式属性的访问是否在正确的上下文中

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| VERIFY_REACTIVE_OBSERVER | 响应式观察者验证开关 | Boolean |

### verifyFailed()
自定义验证失败时的处理逻辑，默认情况下，验证失败会抛出异常

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| handler | 验证失败处理函数 | (RuntimeException) -> Unit |

### 示例

```kotlin
override fun willInit() {
    super.willInit()
    if (pageData.params.optBoolean("debug")) {
        Pager.VERIFY_THREAD = true // 开启线程校验
        Pager.VERIFY_REACTIVE_OBSERVER = true // 开启observable校验
        // 自定义验证失败处理
        Pager.verifyFailed { exception ->
            println("验证失败: ${exception.message}")
        }
    }
}
```

## 相关文档

- [入门指南-页面入口Pager](../../DevGuide/pager.md)
- [协程和多线程编程指引-关于线程安全](../../DevGuide/thread-and-coroutines.md#关于线程安全)

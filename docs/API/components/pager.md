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

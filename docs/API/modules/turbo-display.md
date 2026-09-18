# TurboDisplayModule

首屏直出渲染（TurboDisplay）模块，用于在业务需要主动干预首屏缓存内容的场景下操作缓存。

<Badge text="iOS 2.16.0 及以上支持" type="warn"/>

:::tip 关于启用方式
TurboDisplay 的启用开关在 Native 侧配置，不涉及 Kotlin 业务代码；本模块提供的是运行期的缓存管理能力。完整的机制说明、配置项与状态恢复方案见「TurboDisplay 首屏加速机制」章节。
:::

## setCurrentUIAsFirstScreenForNextLaunch方法

将当前 UI 采集为下次页面启动的首屏（该首屏可交互），调用后会关闭自动采集。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| extraCacheContent <Badge text="非必需" type="warn"/> | 额外缓存内容，JSON 字符串，用于页面状态恢复 | String |

`extraCacheContent` 格式规范：

```json
{
  "<viewTag>": {
    "viewName": "<组件名称>",
    "<propKey1>": <propValue1>
  }
}
```

其中 `viewName` 为必填项，供端侧校验；`propKey` 为需要恢复的属性，如列表的 `contentOffsetY`。

**示例**

```kotlin
acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
    .setCurrentUIAsFirstScreenForNextLaunch(extraCacheContent)
```

## closeTurboDisplayMode方法

关闭 TurboDisplay 首屏直出渲染模式。

## isTurboDisplay方法

同步查询本次打开是否为 TurboDisplay 模式（即首屏是否由缓存上屏）。

**返回值**

| 类型 | 描述 |
|:----|:-------|
| Boolean | true 表示本次首屏来自缓存 |

**示例**

```kotlin
val isFromCache = acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
    .isTurboDisplay()
```

## clearCurrentPageCache方法

强制清除当前页面的 TurboDisplay 缓存，用于测试时重置缓存状态。

## clearAllCache方法

强制清除全部 TurboDisplay 缓存文件，用于测试时重置缓存状态。

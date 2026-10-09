# Module概述

``Kuikly``作为一个跨端的UI框架, 他本身不具备调用平台API的能力, 但是``Kuikly``提供了一套``Module``机制，你可以通过``Module``机制将平台的API暴露给``Kuikly``侧调用。
``Kuikly``内置了一些通用的``Module``, 如果这些``Module``不满足你业务诉求时, 你可以通过[扩展原生API](../../DevGuide/expand-native-api.md)来自定义``Module``, 将更多的宿主平台API暴露给``Kuikly``侧使用。
下面是``Kuikly``内置的``Module``

* [MemoryCacheModule](memory-cache.md)
* [SharedPreferencesModule](sp.md)
* [RouterModule](router.md)
* [NetworkModule](network.md)
* [NotifyModule](notify.md)
* [SnapshotModule](snapshot.md)
* [CodecModule](codec.md)
* [CalendarModule](calendar.md)
* [PerformanceModule](performance.md)
* [TurboDisplayModule](turbo-display.md) <Badge text="仅 iOS" type="warn"/>
* [VsyncModule](vsync.md) <Badge text="Android" type="warn"/> <Badge text="iOS" type="warn"/> <Badge text="鸿蒙" type="warn"/>
* [BackPressModule](back-press.md) <Badge text="Android" type="warn"/> <Badge text="iOS" type="warn"/> <Badge text="鸿蒙" type="warn"/>
* [FontModule](font.md) <Badge text="Android" type="warn"/> <Badge text="iOS" type="warn"/>
* [FileModule](file.md)
* [LogModule](log.md)

## Module 基类能力

自定义 Module 继承 `Module` 后，可复用基类提供的通信能力：

| 方法 | 说明 |
| -- | -- |
| `syncToNativeMethod(methodName, data: JSONObject?, callbackFn: CallbackFn?)` / `syncToNativeMethod(methodName, args: Array<Any>, callbackFn: AnyCallbackFn?)` | 同步调用端侧方法（JSON 参数版 / 原子参数版） |
| `asyncToNativeMethod(methodName, data: JSONObject?, callbackFn: CallbackFn?)` / `asyncToNativeMethod(methodName, args: Array<Any>, callbackFn: AnyCallbackFn?)` | 异步调用端侧方法（JSON 参数版 / 原子参数版） |
| `toNative(keepCallbackAlive, methodName, param, callback, syncCall)` | 通用端侧调用通道，回参自动转 JSONObject |
| `toTDFNative(keepCallbackAlive, methodName, params, successCallback, errorCallback, syncCall)` | TDF 双回调通道，自动解包 `result` 字段 |
| `removeCallback(callbackRef)` | 销毁全局回调引用 |

模块名需通过 `moduleName()` 返回，自定义 Module 在 `Pager.createExternalModules()` 中注册，详见[扩展原生 API](../../DevGuide/expand-native-api.md)。
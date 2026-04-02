# 原生通信原理（深度参考）

> 通信调用没有响应、callback 没有触发、异步时序问题时读取本文件。

## 1. 通信方法对比与选择

| 方法 | 同步/异步 | 参数类型 | 回调类型 |
|------|----------|---------|---------|
| `syncToNativeMethod(method, JSONObject?, CallbackFn?)` | 同步 | JSON | `(JSONObject?) -> Unit` |
| `asyncToNativeMethod(method, JSONObject?, CallbackFn?)` | 异步 | JSON | `(JSONObject?) -> Unit` |
| `syncToNativeMethod(method, Array<Any>, AnyCallbackFn?)` | 同步 | 二进制 | `(Any?) -> Unit` |
| `asyncToNativeMethod(method, Array<Any>, AnyCallbackFn?)` | 异步 | 二进制 | `(Any?) -> Unit` |

**选择指南**：
- 常规数据交换 → `syncToNativeMethod`（JSON）
- 耗时操作（网络、IO、密集计算）→ `asyncToNativeMethod`
- 传递二进制数据（图片、音频等）→ `syncToNativeMethod`（Array<Any>，支持 ByteArray）

---

## 2. 各平台实现模板

```kotlin
// Android: core-render-android/.../KRMyModule.kt
class KRMyModule : KuiklyRenderBaseModule() {
    override fun callSync(method: String, params: JSONObject?, callback: ((String?) -> Unit)?): String? {
        return when (method) {
            "doSomething" -> { callback?.invoke("result"); "result" }
            else -> null
        }
    }
    override fun callAsync(method: String, params: JSONObject?, callback: ((String?) -> Unit)?) {
        when (method) {
            "doAsync" -> thread { mainHandler.post { callback?.invoke("done") } }
        }
    }
}
// 注册：export.moduleExport("MyModule") { KRMyModule() }
```

```objc
// iOS: core-render-ios/.../KRMyModule.m
@implementation KRMyModule
TDF_EXPORT_MODULE(MyModule)
TDF_EXPORT_METHOD(doSomething) { callback(@[@{@"success": @YES}]); }
TDF_EXPORT_METHOD(doAsync) {
    dispatch_async(dispatch_get_global_queue(0, 0), ^{
        dispatch_async(dispatch_get_main_queue(), ^{ callback(@[@{@"result": @"done"}]); });
    });
}
@end
```

```typescript
// HarmonyOS: core-render-ohos/.../KRMyModule.ets
export class KRMyModule extends KRBaseModule {
    callSync(method: string, params: object | null, callback: ((result: string | null) => void) | null): string | null {
        if (method === "doSomething") { callback?.("result"); return "result"; }
        return null;
    }
}
// 注册：ModulesRegisterEntry.registerModuleCreator("MyModule", () => new KRMyModule())
```

---

## 3. BridgeManager 方法 ID 参考

> 调试底层通信、排查 bridge 异常时查阅。

### Kotlin → Native

| ID | 常量 | 用途 |
|----|------|------|
| 1 | CREATE_RENDER_VIEW | 创建原生视图 |
| 2 | REMOVE_RENDER_VIEW | 移除原生视图 |
| 3 | INSERT_SUB_RENDER_VIEW | 插入子视图 |
| 4 | SET_VIEW_PROP | 设置视图属性 |
| 5 | SET_RENDER_VIEW_FRAME | 设置视图布局 |
| 7 | CALL_VIEW_METHOD | 调用视图方法 |
| 8 | CALL_MODULE_METHOD | 调用模块方法 |
| 15 | FIRE_FATAL_EXCEPTION | 致命异常上报 |
| 16 | SYNC_FLUSH_UI | 同步刷新 UI |
| 17 | CALL_TDF_MODULE_METHOD | 调用 TDF 模块方法 |

### Native → Kotlin

| ID | 常量 | 用途 |
|----|------|------|
| 1 | CREATE_INSTANCE | 创建页面实例 |
| 2 | UPDATE_INSTANCE | 更新实例 |
| 3 | DESTROY_INSTANCE | 销毁实例 |
| 4 | FIRE_CALLBACK | 触发回调 |
| 5 | FIRE_VIEW_EVENT | 触发视图事件 |
| 6 | LAYOUT_VIEW | 布局视图 |

# 原生扩展（Module / 原生 View）

> 以下场景读取本文件：扩展原生能力、新建自定义 Module、桥接原生 View、处理 Kotlin 与 Native 通信。
>
> **开发建议**：优先参考官网文档和 demo 示例直接编写，遇到通信不成功、异步时序异常等问题时再查 [第 4 节（通信细节）](#4-通信方法详解) 和 [第 5 节（BridgeManager）](#5-bridgemanager-方法-id-参考)。

---

## 1. 官网文档索引

| 场景 | 文档 |
|------|------|
| 自研 DSL：扩展原生 Module API | [docs/DevGuide/expand-native-api.md](../../docs/DevGuide/expand-native-api.md) |
| 自研 DSL：扩展原生 View | [docs/DevGuide/expand-native-ui.md](../../docs/DevGuide/expand-native-ui.md) |
| Compose：扩展原生 Module API | [docs/Compose/extend-native-api.md](../../docs/Compose/extend-native-api.md) |
| Compose：桥接原生 View | [docs/Compose/extend-native-ui.md](../../docs/Compose/extend-native-ui.md) |
| Compose：扩展自研 DSL UI 组件 | [docs/Compose/extend-kuikly-dsl-ui.md](../../docs/Compose/extend-kuikly-dsl-ui.md) |

---

## 2. Demo 示例参考

| 场景 | 参考文件 |
|------|---------|
| 日历 Module 使用 | `demo/pages/demo/CalendarModuleExamplePage.kt` |
| 自定义 View（KMP 层定义） | `demo/pages/demo/MyDemoCustomView.kt` |
| 自定义 View（使用示例页面） | `demo/pages/demo/DeclarativeDemo/CustomViewExamplePage.kt` |

> 示例路径前缀：`demo/src/commonMain/kotlin/com/tencent/kuikly/demo/`

---

## 3. 快速参考：Module 开发三步

**Step 1：KMP 层定义**（`core/src/commonMain/`）

```kotlin
class MyModule : Module() {
    override fun moduleName(): String = MODULE_NAME

    fun doSomething(data: String, callback: CallbackFn?) {
        syncToNativeMethod("doSomething", JSONObject().apply { put("data", data) }, callback)
    }

    fun doAsync(data: String, callback: CallbackFn?) {
        asyncToNativeMethod("doAsync", JSONObject().apply { put("data", data) }, callback)
    }

    companion object { const val MODULE_NAME = "MyModule" }
}
```

**Step 2：注册到页面**

```kotlin
override fun createExternalModules(): Map<String, Module>? {
    return mapOf(MyModule.MODULE_NAME to MyModule())
}
```

**Step 3：使用**

```kotlin
val myModule = acquireModule<MyModule>(MyModule.MODULE_NAME)
myModule.doSomething("hello") { result -> /* result: JSONObject? */ }
```

---

## 4. 通信问题排查 / 底层原理

遇到以下情况时读取：[native-bridge-internals.md](native-bridge-internals.md)
- 通信调用没有响应、callback 没有触发
- 异步时序问题（callback 时机不对）
- 需要了解 syncCall vs asyncCall 选择依据
- 需要查阅 BridgeManager 方法 ID 调试底层通信

---

## 6. 跨平台实现检查清单

新增 Module 或 View 时确保：

- [ ] KMP 层定义（`core/src/commonMain/`）
- [ ] Android 实现 + 注册（`core-render-android/`）
- [ ] iOS 实现 + 注册（`core-render-ios/`，ObjC，`KR` 前缀）
- [ ] HarmonyOS 实现 + 注册（`core-render-ohos/`，ArkTS）
- [ ] Web 实现 + 注册（`core-render-web/`，视情况）
- [ ] 方法签名 / 参数格式在所有平台一致

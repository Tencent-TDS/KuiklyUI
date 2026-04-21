# 微信小程序 API 接入

:::tip 阅读对象
本文面向使用 ``Kuikly`` 开发微信小程序页面的业务开发者。阅读之前，建议先完成 [微信小程序工程接入](../QuickStart/Miniapp.md) 与 [微信小程序平台开发方式](miniapp-dev.md)，并已了解 [微信小程序内置组件接入](miniapp-wx-components.md)。
:::

## 1. 背景：为什么还需要对微信小程序 API 单独封装？

``Kuikly`` 已经在 commonMain 提供了一整套**跨端 Module**（``RouterModule`` / ``NetworkModule`` / ``SharedPreferencesModule`` / ``CalendarModule`` / ``NotifyModule`` 等），这些 Module 由各平台（Android/iOS/HarmonyOS/H5/微信小程序）各自实现，能满足"**写一次，到处跑**"。

但在**微信小程序**平台上，仍然有一些能力是**跨端 Module 无法覆盖、或直接使用会损失原生能力**的：

1. **小程序专属能力**：``wx.login`` 的 ``code``、``wx.getUserProfile``、``wx.scanCode``、``wx.showShareMenu``、``wx.requestPayment``、蓝牙 / NFC 等，完全是微信平台的私有 API，其它平台没有等价实现。
2. **更原生的体验**：``wx.showToast / wx.showModal / wx.showActionSheet`` 是小程序的**原生弹窗**，样式、动画、无障碍、性能都优于 Kuikly 跨端组件用 Dom 拼出来的弹层；``wx.setStorageSync`` 也比 Kuikly 跨端的 ``SharedPreferencesModule`` 更原生（后者小程序上其实也是 localStorage 薄包装，只支持字符串 KV）。
3. **系统 / 启动信息**：``wx.getWindowInfo / getLaunchOptionsSync`` 是小程序感知宿主环境、场景值、携带参数的唯一手段，没有跨端等价物。

如果让业务自己在 ``core-render-web/miniapp`` 里手写桥接，会出现：

- 业务破坏了**跨端架构**（要在 commonMain 以外的地方 `js("wx.xxx")`）；
- 每家业务各写一套 ``KRxxxModule``，**重复造轮子**；
- 参数、回调格式不统一，文档散落。

因此 Kuikly 在组件封装（[参见 WX 组件接入文档](miniapp-wx-components.md)）之外，再提供一层**微信小程序 API 封装**：在 commonMain 提供强类型 ``WXxxxModule`` DSL，业务侧只关心 Kotlin，不碰 JS / wxml。

> 一句话：**WX 组件封装**让 UI 跑得够原生，**WX API 封装**让业务调 `wx.xxx` 调得够规范。

---

## 2. 方案总览：三层结构

Kuikly 提供**三种**使用微信小程序 API 的姿势，按业务需求选择：

```text
┌──────────────────────────────────────────────────────────────────┐
│ ① 强类型 Module（优先使用）                                       │
│    WXApiModule / WXStorageModule / WXUIModule / WXSystemModule   │
│    WXClipboardModule / WXLocationModule / WXScanModule           │
│    WXMediaModule    / WXShareModule                               │
│    —— 类型安全、有参数提示、覆盖 80% 常用场景                      │
└──────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────┐
│ ② 兜底通用桥（WXRawApiModule）                                    │
│    wxRaw.call("scanCode", params) { res -> ... }                 │
│    —— 无需新建 Module，一行代码调任意 wx.xxx                       │
└──────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────┐
│ ③ 业务自行封装（可选，用于长期沉淀）                               │
│    MyOrderModule / MyIMModule ... 业务语义的强类型 DSL            │
│    —— 仿照 WXxxxModule，四步完成（见 §4）                          │
└──────────────────────────────────────────────────────────────────┘
```

- **建议优先**使用 ①；
- 如果 Kuikly 还没封装，直接用 ② 兜底；
- 当业务反复在 ② 上调用某 API，建议把它抽成 ③ 专属 Module，沉淀到业务工程。

> 所有 ``WXxxxModule`` 仅在**微信小程序平台**生效；非小程序平台调用会通过 ``onFail`` 回包一条 ``errMsg = "not supported: not in mini-app"``。业务若需跨端运行，请通过 ``pageData.params.optString("is_wx_mp") == "1"`` 分流。

---

## 3. Kuikly 已经封装了哪些 API？

所有强类型 Module 位于 [`core/src/commonMain/kotlin/com/tencent/kuikly/core/module/wx/`](https://github.com/Tencent-TDS/KuiklyUI)，对应的小程序渲染层实现在 [`core-render-web/miniapp/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/runtime/miniapp/expand/module/wx/`](https://github.com/Tencent-TDS/KuiklyUI)。

### 3.1 模块清单

| Module              | 覆盖 wx API                                                                                                               | 典型场景               |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------- | ---------------------- |
| ``WXApiModule``     | ``wx.login`` / ``checkSession`` / ``getUserProfile`` / ``getUserInfo`` / ``getAccountInfoSync``                           | 登录、获取用户信息     |
| ``WXStorageModule`` | ``setStorage`` / ``getStorage`` / ``removeStorage`` / ``clearStorage``（含 Sync） / ``getStorageInfoSync``                | 本地持久化             |
| ``WXUIModule``      | ``showToast`` / ``hideToast`` / ``showLoading`` / ``hideLoading`` / ``showModal`` / ``showActionSheet``                   | 原生交互弹窗           |
| ``WXSystemModule``  | ``getWindowInfo`` / ``getDeviceInfo`` / ``getAppBaseInfo`` / ``getSystemInfo(Sync)`` / ``getLaunchOptionsSync`` / ``getEnterOptionsSync`` | 系统信息 / 启动参数    |
| ``WXClipboardModule`` | ``setClipboardData`` / ``getClipboardData``                                                                            | 剪贴板                 |
| ``WXLocationModule``  | ``getLocation`` / ``chooseLocation`` / ``openLocation``                                                                | 定位                   |
| ``WXScanModule``      | ``scanCode``                                                                                                           | 扫码                   |
| ``WXMediaModule``     | ``chooseImage`` / ``chooseMedia`` / ``previewImage`` / ``saveImageToPhotosAlbum``                                      | 图片 / 多媒体           |
| ``WXShareModule``     | ``showShareMenu`` / ``hideShareMenu`` / ``updateShareMenu``                                                            | 分享菜单               |
| ``WXRawApiModule``    | **任意** ``wx.xxx`` / ``wx.xxxSync``                                                                                   | 兜底桥                 |

### 3.2 前置：把 Module 注册到业务 Pager

Kuikly 的 Module 采用"业务侧注册 + 渲染侧 ``moduleExport`` 实现"的两端模型。**渲染层**已在 [`KuiklyRenderViewDelegator.registerModule()`](https://github.com/Tencent-TDS/KuiklyUI) 中统一 ``moduleExport`` 了全部 WX Module，你不用再处理。但**业务侧**每个 ``Pager`` 需要在 ``createExternalModules()`` 里注册 ``Module`` 实例，``acquireModule<T>(name)`` 才能取到；否则会报错：

```text
acquireModule 失败：KRWXxxxModule 未注册，请在重写Pager.createExternalModules方法时机中添加注册(调用Pager.registerModule方法注册)
```

为了省心，demo 工程的 [`BasePager`](https://github.com/Tencent-TDS/KuiklyUI) 已经一次性注册了全部 WX Module：

```kotlin
override fun createExternalModules(): Map<String, Module>? {
    val externalModules = hashMapOf<String, Module>()
    // ...业务自己的 Module
    // ---- 微信小程序 API 封装 Modules ----
    externalModules[WXApiModule.MODULE_NAME]       = WXApiModule()
    externalModules[WXStorageModule.MODULE_NAME]   = WXStorageModule()
    externalModules[WXUIModule.MODULE_NAME]        = WXUIModule()
    externalModules[WXSystemModule.MODULE_NAME]    = WXSystemModule()
    externalModules[WXClipboardModule.MODULE_NAME] = WXClipboardModule()
    externalModules[WXLocationModule.MODULE_NAME]  = WXLocationModule()
    externalModules[WXScanModule.MODULE_NAME]      = WXScanModule()
    externalModules[WXMediaModule.MODULE_NAME]     = WXMediaModule()
    externalModules[WXShareModule.MODULE_NAME]     = WXShareModule()
    externalModules[WXRawApiModule.MODULE_NAME]    = WXRawApiModule()
    return externalModules
}
```

**接入方做法**：让自己的业务 ``BasePager`` 参照以上写法把需要用到的 WX Module 加进 ``createExternalModules`` 返回的 map 即可。只会用到个别 Module 时也可以按需只注册对应那几个。

> ⚠️ ``Pager.acquireModule`` 本身是跨平台接口，但这些 WX Module **只在微信小程序平台有真正的实现**；在其它平台上，调用会走 fail 兜底（回调参数里会带 ``errMsg``），业务侧可按需判断 ``pageData.params.optString("is_wx_mp") == "1"`` 再决定是否调用。

### 3.3 如何使用：强类型 Module

所有 Module 都通过 ``acquireModule<T>(T.MODULE_NAME)`` 拿到单例实例，然后直接调用方法。统一的回调约定：

- ``onSuccess: (JSONObject?) -> Unit`` —— 调用成功，参数是 wx API 回调的 ``res``；
- ``onFail: (JSONObject?) -> Unit`` —— 调用失败，参数是 wx API 回调的 ``err``（含 ``errMsg``）。

**示例 1：登录 + 缓存**

```kotlin
import com.tencent.kuikly.core.module.wx.WXApiModule
import com.tencent.kuikly.core.module.wx.WXStorageModule
import com.tencent.kuikly.core.module.wx.WXUIModule

// 登录
acquireModule<WXApiModule>(WXApiModule.MODULE_NAME).login(
    onSuccess = { res ->
        val code = res?.optString("code")
        // 用 code 去业务后端换 openid + session_key
        // ...
        // 存本地
        acquireModule<WXStorageModule>(WXStorageModule.MODULE_NAME)
            .setStorageSync("login_code", code ?: "")
    },
    onFail = { err ->
        acquireModule<WXUIModule>(WXUIModule.MODULE_NAME)
            .showToast(title = "登录失败", icon = "error")
    }
)
```

**示例 2：原生 Toast / Modal**

```kotlin
val ui = acquireModule<WXUIModule>(WXUIModule.MODULE_NAME)

ui.showToast(title = "操作成功", icon = "success", duration = 1500)

ui.showModal(
    title = "提示",
    content = "确认删除这条数据吗？",
    confirmText = "删除",
    cancelText = "取消",
    onSuccess = { res ->
        if (res?.optBoolean("confirm") == true) {
            // 用户点了确定
        }
    }
)
```

**示例 3：扫码 / 定位**

```kotlin
acquireModule<WXScanModule>(WXScanModule.MODULE_NAME).scanCode(
    onSuccess = { res -> KLog.i(TAG, "scan=${res?.optString("result")}") },
    onFail = { err -> KLog.e(TAG, "scan fail: $err") }
)

acquireModule<WXLocationModule>(WXLocationModule.MODULE_NAME).getLocation(
    type = "gcj02",
    isHighAccuracy = true,
    onSuccess = { res ->
        val lat = res?.optString("latitude")
        val lng = res?.optString("longitude")
    }
)
```

### 3.4 如何使用：兜底桥 ``WXRawApiModule``

当 Kuikly 尚未封装某个 wx API 时，**不要急着新增 Module**，先用兜底桥 ``WXRawApiModule`` 顶住。

```kotlin
import com.tencent.kuikly.core.module.wx.WXRawApiModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

val raw = acquireModule<WXRawApiModule>(WXRawApiModule.MODULE_NAME)

// 异步调用：raw.call(apiName, args, onSuccess, onFail)
raw.call(
    apiName = "vibrateShort",
    args = JSONObject().apply { put("type", "heavy") },
    onSuccess = { /* 震动成功 */ },
    onFail = { err -> KLog.e(TAG, "vibrate fail: $err") }
)

// 同步调用：raw.callSync(apiName, args) -> JSONObject?
val battery = raw.callSync("getBatteryInfoSync")
val level = battery?.optString("level")
```

**约束**：

1. 参数对象里**不要**手动写 ``success / fail / complete``——兜底桥会自动注入回调；
2. 参数仅支持 JSON 可序列化类型（string / number / boolean / array / JSONObject）；
3. 没有编译期类型校验，请自查 [微信官方文档](https://developers.weixin.qq.com/miniprogram/dev/api/) 填写 ``apiName`` 和 ``args``。

### 3.5 完整 Demo 在哪里？

仓库已提供可运行的示例：

- **强类型 Module Demo**：[`demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/wx/WXApiExamplePage.kt`](https://github.com/Tencent-TDS/KuiklyUI)
   - 按 Module 分 section，覆盖 9 个 WX*Module 的典型方法；
- **兜底桥 Demo**：[`demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/wx/WXRawApiExamplePage.kt`](https://github.com/Tencent-TDS/KuiklyUI)
   - 展示 ``call`` / ``callSync`` 的典型用法；
- **入口**：``ExampleIndexPage`` 的 **WX API Demo** 与 **WX Raw API Demo**，**仅在小程序平台显示**。

在 **微信开发者工具** 打开 ``miniApp/dist``，即可在入口列表中看到上述两个 Demo。

---

## 4. 如果 Kuikly 没有封装我需要的 API，怎么办？

### 4.1 优先方案：直接用兜底桥

先评估是否能用 ``WXRawApiModule.call(...)`` / ``.callSync(...)`` 满足业务。大多数一次性 / 低频 API 不需要自己封装。

### 4.2 沉淀成业务 Module（四步）

只有当**业务反复**在兜底桥上调用某个（或某组）API 时，才建议抽成业务自有的 ``MyXxxModule``。

标准做法与 WX*Module 完全对齐：

```text
┌─────────────────────────────────────────────────────────────────────┐
│ ① 业务 DSL 层（core/commonMain 或业务工程 commonMain）                │
│    class MyXxxModule : Module() {                                    │
│        fun doSomething(onSuccess: CallbackFn?, onFail: CallbackFn?)  │
│        override fun moduleName() = "KRMyXxxModule"                   │
│    }                                                                 │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │ toNative(method, {args:...}, cb)
┌─────────────────────────────────▼───────────────────────────────────┐
│ ② 渲染层（core-render-web/miniapp/jsMain 或业务工程 jsMain）           │
│    class KRMyXxxModule : KuiklyRenderBaseModule() {                  │
│        override fun call(method, params, callback): Any? {           │
│            val args = params.toJSONObjectSafely().optJSONObject(...) │
│            // 调 wx.xxx 或本地逻辑                                   │
│        }                                                             │
│    }                                                                 │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │ moduleExport(name) { KRMyXxxModule() }
┌─────────────────────────────────▼───────────────────────────────────┐
│ ③ Delegator 注册                                                     │
│    KuiklyRenderViewDelegator.registerModule() 增加一行               │
└──────────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────────┐
│ ④（可选）业务 Demo + 小程序 pages/xxx 目录 + app.json pages 列表      │
└──────────────────────────────────────────────────────────────────────┘
```

其中 ②③ 可以直接复用 Kuikly 提供的 ``KRWXApiBridge`` 工具，大多数异步 API 只需要 1~2 行：

```kotlin
class KRMyXxxModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        val args = params.toJSONObjectSafely().optJSONObject("args")
        return when (method) {
            // 直接透传到 wx.login / wx.scanCode 等微信 API
            "xxx", "yyy" -> {
                KRWXApiBridge.invokeAsync(method, args, callback)
                null
            }
            else -> super.call(method, params, callback)
        }
    }
    companion object { const val MODULE_NAME = "KRMyXxxModule" }
}
```

> ``KRWXApiBridge.invokeAsync`` / ``.invokeSync`` 会自动处理：
> - 把 ``rawArgs`` 注入 ``success / fail``；
> - 非小程序平台降级（``onFail`` 回包 ``errMsg = "not supported"``）；
> - 异常捕获与 JSON 序列化。

### 4.3 参考源码

- **参考实现**：``WXApiModule`` + ``KRWXApiModule`` 是最标准的示例。
   - DSL：[`core/src/commonMain/kotlin/com/tencent/kuikly/core/module/wx/WXApiModule.kt`](https://github.com/Tencent-TDS/KuiklyUI)
   - 渲染：[`core-render-web/miniapp/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/runtime/miniapp/expand/module/wx/KRWXApiModule.kt`](https://github.com/Tencent-TDS/KuiklyUI)
   - 注册：[`core-render-web/miniapp/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/runtime/miniapp/expand/KuiklyRenderViewDelegator.kt`](https://github.com/Tencent-TDS/KuiklyUI) 中的 ``registerModule``
- **同步方法样例**：``WXSystemModule.getWindowInfoSync`` / ``KRWXSystemModule`` —— 演示直接返回值的调用；
- **存储类样例**：``WXStorageModule`` / ``KRWXStorageModule`` —— 演示同步 / 异步混合；
- **兜底桥内部机制**：[`core-render-web/miniapp/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/runtime/miniapp/expand/module/wx/KRWXApiBridge.kt`](https://github.com/Tencent-TDS/KuiklyUI) 是所有 Module 的共享工具，直接读源码就能完全理解原理。

### 4.4 常见坑

| 现象 | 原因 | 解决 |
| ---- | ---- | ---- |
| 调用直接崩溃 / 报错 "wx.xxx is not a function" | ``apiName`` 拼错 / 当前基础库版本不支持 | 对照微信开发者工具基础库版本 / 查官方文档 |
| 永远走 fail 分支，``errMsg = "not supported"`` | 不是小程序平台（H5、原生）调到了 WX*Module | 用 ``pageData.params.optString("is_wx_mp") == "1"`` 分流 |
| 参数传进去不生效 | 对象参数没有**完整**给定字段（wx 有默认值要求） | 查微信官方文档，补齐必填字段 |
| success / fail 被你自己覆盖 | 在 ``args`` 中手写了 ``success / fail`` | 不要写；兜底桥会自动注入 |
| 二进制 / 复杂数据调不通 | 兜底桥只支持 JSON 可序列化数据 | 新封装一个 ``KRMyXxxModule``，自行处理 ArrayBuffer |

---

## 5. 使用 AI 辅助生成新 API 封装

封装一个新的 wx API Module，绝大部分情况就是**按模板填空**，非常适合交给大模型。推荐 prompt：

### 5.1 Prompt 举例（生成业务专属 Module）

在 Kuikly 的 `core/src/commonMain/kotlin/com/tencent/kuikly/core/module/wx/` 目录，
已经封装了 `WXApiModule / WXStorageModule / WXUIModule / WXSystemModule / WXScanModule` 等一批微信小程序 API Module。
它们的实现模式：业务层 `WXxxxModule : Module()` + 渲染层 `KRWXxxxModule : KuiklyRenderBaseModule()`，
渲染层统一通过 `KRWXApiBridge.invokeAsync(apiName, args, callback)` 透传 `wx.xxx` 调用。

现在请你参照上述结构，新增对微信小程序**蓝牙系列 API**（wx.openBluetoothAdapter / wx.startBluetoothDevicesDiscovery /
wx.onBluetoothDeviceFound / wx.createBLEConnection 等）的封装，要求：

1. 业务 DSL 类名 `WXBluetoothModule`，放到同目录；
2. 渲染层类名 `KRWXBluetoothModule`，放到 `core-render-web/miniapp/.../expand/module/wx/` 下；
3. `MODULE_NAME` 使用常量 `"KRWXBluetoothModule"`，并在 `WXModuleConst` 中补一个 `BLUETOOTH` 常量；
4. 在 `KuiklyRenderViewDelegator.registerModule()` 中加一行注册；
5. 对 `onXxx` 事件（例如 `onBluetoothDeviceFound`）需要保持回调 keepAlive，请在业务 DSL 层用 `toNative(keepCallbackAlive = true, ...)` 调用；
6. 参考 `WXApiModule.kt` 的注释风格，中文注释说明入参、回调字段；
7. 最后在 `WXApiExamplePage.kt` 添加一个 section 演示蓝牙扫描的调用。

### 5.2 Prompt 举例（无需新建 Module，教 AI 直接用兜底桥）

我需要在 Kuikly 的 commonMain 调用微信小程序 `wx.makePhoneCall`，它不是很常用，不需要新增 Module。
请直接使用 Kuikly 已提供的 `WXRawApiModule.call(...)`，写一段示例代码，支持电话号码校验失败走 onFail。

通过这种"**方案优先级 + 四步模板 + 兜底桥 + 参考源码**"的组合，业务无论"增量扩展"还是"快速接入"都有明确的路径。

---

## 6. 小结

- **默认优先用 Kuikly 已封装的** ``WXxxxModule``（9 个常用 Module）；
- **没封装的** 直接用 ``WXRawApiModule`` 兜底桥，一行代码调任意 ``wx.xxx``；
- **反复用到的** 再沉淀为业务专属 ``MyXxxModule``，配合 ``KRWXApiBridge`` 工具，四步落地；
- **仅在微信小程序平台生效**，跨端业务请用 ``pageData.params.optString("is_wx_mp")`` 分流；
- **有 AI 加持**：按 §5 的 prompt 模板，可以把"封装新 API"变成一句话的活儿。

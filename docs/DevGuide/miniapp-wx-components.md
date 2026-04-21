# 微信小程序内置组件接入

:::tip 阅读对象
本文面向使用 ``Kuikly`` 开发微信小程序页面的业务开发者。阅读之前，建议先完成 [微信小程序工程接入](../QuickStart/Miniapp.md) 与 [微信小程序平台开发方式](miniapp-dev.md)。
:::

## 1. 背景：为什么还需要封装微信小程序内置组件？

``Kuikly`` 已经提供了一套跨端组件（``View``/``Text``/``Image``/``Input``/``Scroller`` 等），它们在 Android/iOS/HarmonyOS/H5/微信小程序等多个平台上渲染出视觉与行为基本一致的 UI。但是在**微信小程序**这个平台上，存在一部分**只能由微信小程序原生组件提供**的能力，Kuikly 跨端组件无法覆盖：

- **开放能力**：如 ``button`` 的 ``open-type="getPhoneNumber" / "getUserInfo" / "contact" / "openSetting"`` 等，只能通过微信小程序原生 ``<button>`` 触发，并返回加密信息给开发者。
- **宿主能力**：如 ``<camera>``、``<map>``、``<video>``、``<web-view>``、``<canvas type="2d">``、硬件拾音等，全部需要调用小程序提供的原生渲染 / 系统 API。
- **表单原语**：``<input>``、``<textarea>``、``<picker>`` 等在小程序下有独立的输入法、联想、软键盘调度与无障碍逻辑，跨端 ``Input`` 在小程序上**并不等价**这些原生行为（例如 ``confirm-hold``、``adjust-keyboard-to`` 等）。

因此 Kuikly 选择**“跨端组件覆盖 80% 通用能力 + 微信小程序内置组件封装覆盖剩余 20% 原生能力”**的方案。这样做的收益：

1. **保留小程序原生能力**：能完整使用开放能力 / 宿主能力，合规、安全。
2. **业务侧 API 统一**：封装后以 Kuikly 风格的 DSL（``WXButton { attr {...} event {...} }``）暴露，业务层无需感知 wxml / JS 胶水代码。
3. **跨端可降级**：通过 ``viewName()`` 的平台分流，同一段业务代码在**非小程序平台**可以使用跨端组件降级渲染。
4. **按需组合**：可以在同一个 Kuikly 页面内混用跨端组件与 WX 原生组件（比如 ``View { WXButton { ... } }``）。

> 一句话：Kuikly 跨端组件解决的是“**写一次，到处跑**”，WX 组件封装解决的是“**在小程序上，跑得够原生**”。

---

## 2. Kuikly 已经封装了哪些微信小程序内置组件？

当前内置于 ``Kuikly`` 的 ``wx`` 组件位于：

- **业务 DSL 层**：[`core/src/commonMain/kotlin/com/tencent/kuikly/core/views/wx/`](https://github.com/Tencent-TDS/KuiklyUI)
- **小程序渲染层**：[`core-render-web/miniapp/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/runtime/miniapp/expand/components/wx/`](https://github.com/Tencent-TDS/KuiklyUI)
- **模板层**：[`miniApp/dist/base.wxml`](https://github.com/Tencent-TDS/KuiklyUI)

### 2.1 组件清单

| DSL 入口       | 对应小程序组件 | 核心能力                                                          | DSL 源文件            |
| -------------- | -------------- | ----------------------------------------------------------------- | --------------------- |
| ``WXButton``   | ``<button>``   | 标准按钮 + ``open-type`` 开放能力（获取手机号/用户信息/联系客服/打开设置/反馈等） | ``WXButtonView.kt``   |
| ``WXInput``    | ``<input>``    | 原生输入框：``type``（text/number/digit/idcard/safe-password 等）、``confirmType``、``password``、``focus``、``maxlength`` | ``WXInputView.kt``    |
| ``WXTextArea`` | ``<textarea>`` | 多行输入：自适应高度、光标控制、键盘回车行为                       | ``WXTextAreaView.kt`` |
| ``WXPicker``   | ``<picker>``   | 原生选择器：``selector / multiSelector / time / date / region``    | ``WXPickerView.kt``   |
| ``WXVideo``    | ``<video>``    | 视频播放、``object-fit``、控件、进度回调、错误回调                 | ``WXVideoView.kt``    |
| ``WXCamera``   | ``<camera>``   | 相机预览、前后摄像头切换、闪光灯、分辨率                           | ``WXCameraView.kt``   |
| ``WXMap``      | ``<map>``      | 地图、标记点、缩放与视图控制、地理事件                             | ``WXMapView.kt``      |
| ``WXWebView``  | ``<web-view>`` | 嵌入外部 H5 页面（需在小程序后台配置业务域名）                    | ``WXWebView.kt``      |

> 每一个 ``WXxxx`` 组件背后，都按"**DSL 层 → 渲染层 → DOM 元素 → wxml 模板**"四层分工实现。我们在 ``base.wxml`` 中已预置了对应模板，业务侧只需直接使用 DSL 即可。

### 2.2 如何使用

和使用其他 Kuikly 组件一致，``WXxxx`` 采用标准的 ``attr { } / event { }`` DSL：

```kotlin
import com.tencent.kuikly.core.views.wx.WXButton
import com.tencent.kuikly.core.views.wx.WXButtonOpenType

WXButton {
    attr {
        width(200f)
        height(44f)
        type("primary")
        size("default")
        openType(WXButtonOpenType.GET_PHONE_NUMBER)
        titleAttr {
            text("获取手机号")
            fontSize(16f)
            color(0xFFFFFFFF)
        }
    }
    event {
        click {
            // 常规点击
        }
        onGetPhoneNumber { detail ->
            // open-type=getPhoneNumber 的回调
            val data = (detail as JSONObject).optString("data")
        }
    }
}
```

再比如 ``WXInput`` + ``WXPicker``：

```kotlin
WXInput {
    attr {
        width(300f)
        height(40f)
        type(WXInputType.NUMBER)
        confirmType(WXInputConfirmType.DONE)
        placeholder("请输入手机号")
    }
    event {
        onInput { s -> ctx.phone = s }
        onConfirm { ctx.submit() }
    }
}

WXPicker {
    attr {
        mode(WXPickerMode.DATE)
        value("2026-04-21")
    }
    event {
        onChange { detail -> ctx.selectedDate = detail.optString("value") }
    }
}
```

### 2.3 只在小程序平台显示 WX 组件

大多数 ``WXxxx`` 组件只能在微信小程序平台使用，请在非小程序平台通过 ``pageData.params`` 分流：

```kotlin
val isMiniProgram = pageData.params.optString("is_wx_mp") == "1"
if (isMiniProgram) {
    WXButton { /* 仅小程序 */ }
} else {
    Button { /* 降级为跨端 Button */ }
}
```

### 2.4 完整 Demo 在哪里？

仓库里已提供了一整套可运行的示例：

- **Demo 页面**：[`demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/wx/WXExamplePage.kt`](https://github.com/Tencent-TDS/KuiklyUI)
   - 覆盖了 ``WXButton`` 各种 ``type/size/plain/disabled/loading``
   - ``open-type`` 开放能力示例（``getPhoneNumber / getUserInfo / contact / openSetting / feedback``）
   - ``WXInput / WXTextArea / WXPicker / WXVideo / WXCamera / WXMap`` 的基础与进阶用法
   - ``WXWebView`` 内嵌 H5 的入口示例（详见下方 ``WXWebViewDemoPage.kt``）
- **入口**：``ExampleIndexPage`` 中的 ``WX Demo`` 入口，**仅在小程序平台显示**。
- **WebView 专用子页**：``demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/wx/WXWebViewDemoPage.kt``

在 **微信开发者工具** 中打开 ``miniApp/dist``，运行 Demo 之后即可在"**WX Demo**"入口查看所有示例。

---

## 3. 如果 Kuikly 没有封装我需要的组件，怎么办？

在 ``Kuikly`` 下扩展一个新的微信小程序内置组件，标准做法是"**四层对齐**"：

```
┌──────────────────────────────────────────────────────────────────┐
│  ① 业务 DSL 层（core/commonMain）                                 │
│    WXXxxView / WXXxxAttr / WXXxxEvent + ViewContainer.WXXxx { }  │
└─────────────────────────┬────────────────────────────────────────┘
                          │ viewName() = "KRWXXxxView"
┌─────────────────────────▼────────────────────────────────────────┐
│  ② 渲染层（core-render-web/miniapp/jsMain）                      │
│    KRWXXxxView : IKuiklyRenderViewExport                          │
│      - setProp / 事件映射                                         │
└─────────────────────────┬────────────────────────────────────────┘
                          │ ele = MiniWXXxxViewElement()
┌─────────────────────────▼────────────────────────────────────────┐
│  ③ DOM 元素层（core-render-web/miniapp/jsMain）                  │
│    MiniWXXxxViewElement : MiniElement                             │
│      - NODE_NAME = "xxx"（小程序原生标签名）                      │
│      - componentsAlias = { _num, 属性短名映射 }                   │
└─────────────────────────┬────────────────────────────────────────┘
                          │ _num = "NN"
┌─────────────────────────▼────────────────────────────────────────┐
│  ④ 模板层（miniApp/dist/base.wxml）                              │
│    <template name="tmpl_0_NN">                                    │
│      <xxx ... bindtap="eh" binderror="eh" ... />                  │
│    </template>                                                    │
└──────────────────────────────────────────────────────────────────┘
```

### 3.1 实现清单（5 步）

1. **① 新增 DOM 元素**：在 ``core-render-web/miniapp/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/runtime/miniapp/dom/wx/`` 下新增 ``MiniWXXxxViewElement.kt``：
    - 指定 ``NODE_NAME = "xxx"``（小程序原生标签名）
    - 声明 ``componentsAlias``：每个 Kuikly 侧属性到 wxml 模板中 ``i.xxx`` 变量的简写映射
    - 为每个属性提供 ``setter``（触发 ``setAttribute``）
2. **② 新增渲染层 View**：在 ``core-render-web/miniapp/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/runtime/miniapp/expand/components/wx/`` 下新增 ``KRWXXxxView.kt``：
    - 实现 ``IKuiklyRenderViewExport``
    - 在 ``setProp(key, value)`` 里把 Kuikly 属性写到 ``canvasElement``
    - 在 ``init { ele.addEventListener(...) }`` 里把小程序事件映射为 ``KuiklyRenderCallback``
    - 暴露 ``VIEW_NAME = "KRWXXxxView"``
3. **③ 在 Render Delegator 中注册**（通常在 ``KuiklyRenderViewDelegator`` / ``KuiklyWebRenderViewDelegator`` 中）：

   ```kotlin
   Transform.addComponentsAlias(
       MiniWXXxxViewElement.NODE_NAME,
       MiniWXXxxViewElement.componentsAlias
   )
   kuiklyRenderExport.renderViewExport(KRWXXxxView.VIEW_NAME) { KRWXXxxView() }
   ```
4. **④ 补全 wxml 模板**：在 ``miniApp/dist/base.wxml`` 中新增 ``<template name="tmpl_0_NN">``：
    - 模板编号 ``NN`` 必须全局唯一，与 ``componentsAlias._num`` 对应
    - 属性用 ``{{i.xxx}}`` / ``{{xs.b(i.xxx, 默认值)}}`` 读取
    - 所有事件统一 ``bind... = "eh"``，Kuikly Render 会自动路由到你的 ``KRWXXxxView``
5. **⑤ 新增业务 DSL**：在 ``core/src/commonMain/kotlin/com/tencent/kuikly/core/views/wx/`` 下新增 ``WXXxxView.kt``：
    - ``WXXxxView : DeclarativeBaseView<WXXxxAttr, WXXxxEvent>()``
    - ``viewName() = "KRWXXxxView"``
    - 在 ``WXXxxAttr`` 里用 ``PROP_XXX with value`` 声明属性方法
    - 在 ``WXXxxEvent`` 里用 ``register(CALLBACK_XXX) { ... }`` 声明事件方法
    - 暴露 ``ViewContainer<*, *>.WXXxx(init: WXXxxView.() -> Unit)`` DSL 扩展

### 3.2 参考 Demo / 源码

- **完整参考实现**：``WXButton`` 是最完整、最"标准"的示例。推荐按以下顺序阅读：
    - ``core/src/commonMain/kotlin/com/tencent/kuikly/core/views/wx/WXButtonView.kt``（DSL）
    - ``core-render-web/miniapp/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/runtime/miniapp/expand/components/wx/KRWXButtonView.kt``（渲染）
    - ``core-render-web/miniapp/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/runtime/miniapp/dom/wx/MiniWXButtonViewElement.kt``（DOM）
    - ``miniApp/dist/base.wxml`` 中 ``tmpl_0_76``（模板）
- **复杂交互参考**：``WXMapView.kt`` 展示了**标记点**等结构化数据属性的传递；``WXVideoView.kt`` 展示了**播放进度 / 错误**多回调事件映射；``WXWebView.kt`` 展示了**跨页面 / 宿主通信**。
- **使用范例**：``demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/wx/WXExamplePage.kt`` 里每个 section 都对应一个组件。
- **通用扩展指南（底层原理）**：[扩展原生 UI - 微信小程序侧](./expand-native-ui.md#微信小程序侧) 提供了跨平台扩展背景与 ``MiniElement`` 机制的完整说明。
- **已有参考 Markdown**：仓库中的 [`miniApp/example/KuiklyWorkWithMiniapp/Kuikly自定义组件集成微信小程序内置组件指南.md`](https://github.com/Tencent-TDS/KuiklyUI) 对每一层代码都有逐行讲解，可作为**手把手教程**。

### 3.3 常见坑

| 现象 | 原因 | 解决 |
| ---- | ---- | ---- |
| 组件渲染为空白 | ``base.wxml`` 中忘记补对应 ``tmpl_0_NN`` 模板 | 对照 ``componentsAlias._num`` 新增模板 |
| ``_num`` 冲突 / 渲染串了 | 编号重复 | 全局唯一，建议递增取最大+1 |
| 事件不触发 | wxml 中没有 ``bindxxx="eh"``，或 ``addEventListener`` 的事件名与小程序事件名大小写不一致 | 小程序事件名**全小写**；绑定必须走 ``eh`` |
| 在非小程序平台崩溃 / 白屏 | 直接使用了 ``WXxxx`` 而未做平台判断 | 通过 ``pageData.params.optString("is_wx_mp") == "1"`` 分流 |
| 微信开发者工具日志 ``load failed due to not in domain list`` | ``<web-view>``/``<map>`` 等调用了未配置的业务域名 | 在小程序后台配置 ``业务域名`` |

---

## 4. 使用 AI 辅助生成新组件

封装一个新的 WX 组件，基本是"**按模板填空**"——特别适合交给大模型（Cursor / Claude / GPT / Gemini 等）完成。我们建议的 prompt 举例如下：

### 4.1 Prompt 举例

在 miniApp/example/KuiklyWorkWithMiniapp/shared/src/commonMain/kotlin/com/example/kuiklyworkwithminiapp 目录实现了微信小程序组件button的封装，业务使用类在CustomButton.kt里，其它实现在其它文件里。
现在需要对微信小程序组件audio进行类似的封装，要求：

- 代码放到某工程某目录里，最后放到wx名字的子目录里；
- 代码结构类似button组件的封装；
- 组件的封装类使用WX前缀，比如WXAudioView.kt、KRWXAudioView、MiniWXAudioViewElement.kt等；
- 补全base.wxml文件；
- 同时开发一个使用该封装组件的demo页面，放在某目录下；

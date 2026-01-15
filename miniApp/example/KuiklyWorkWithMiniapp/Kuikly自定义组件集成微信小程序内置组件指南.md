# Kuikly 自定义组件集成微信小程序内置组件指南

## 概述

本文档说明如何在 Kuikly 项目中通过自定义组件的方式集成和使用微信小程序的内置组件（如 `button`）。

以 `button` 组件为例，展示了完整的集成流程，包括如何：
- 定义小程序原生元素
- 创建 Web 渲染层的视图导出类
- 实现跨平台的自定义组件
- 注册模板和组件映射
- 处理小程序特有的事件回调

ps：在其他跨平台宿主也应该提供自定义组件的实现,本文档暂不讨论

---

## 核心概念

Kuikly 使用多层架构来支持微信小程序内置组件：

1. **DOM 层（miniApp/jsMain）**：定义小程序原生元素的 DOM 包装类
2. **模板层（base.wxml）**：微信小程序 WXML 模板，DOM包装类最终生成描述json，通过模板渲染为微信小程序组件
3. **渲染层（miniApp/jsMain）**：创建 Web 渲染视图类，处理属性和事件，需要注册到kuikly的组件映射
4. **业务层（shared/commonMain）**：提供跨平台的 Compose 风格 API

```
┌─────────────────────────────────────────────┐
│  业务层 (CustomButton.kt)                    │
│  跨平台 Compose API                          │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│  渲染层 (KRCustomButtonView.kt)             │
│  Web 渲染视图导出 + 事件处理                 │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│  DOM 层 (MiniButtonViewElement.kt)          │
│  小程序原生元素包装                          │
└─────────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│  模板层 (base.wxml)                          │
│  微信小程序 WXML 模板                        │
└─────────────────────────────────────────────┘
```

---

## 实现步骤

### 步骤 1：定义小程序原生元素 (DOM 层)

在 `miniApp/src/jsMain/kotlin/dom/` 目录下创建元素类，继承自 `MiniElement`。

**文件：`MiniButtonViewElement.kt`**

```kotlin
package dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElementUtil

class MiniButtonViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    
    // 定义小程序 button 的属性
    var openType: String = ""
        set(value) {
            setAttribute("openType", value)
        }

    companion object {
        // 小程序原生标签名
        const val NODE_NAME = "button"
        
        // 属性映射配置：定义 Kuikly 内部属性名到小程序模板的映射
        val componentsAlias = js("""
            {
                _num: '76',              // 模板编号（唯一标识）
                class: 'cl',             // class 映射
                animation: 'p0',         // animation 映射
                openType: 'openType',    // open-type 映射
                formType: 'formType',
                type: 'type',
                size: 'size',
                name: 'name',
                lang: 'lang',
                sessionFrom: 'sessionFrom',
            }
        """)
    }
}
```

**关键点：**
- `NODE_NAME`：必须与微信小程序的原生标签名一致（如 `button`、`web-view`）
- `componentsAlias`：
  - `_num`：模板编号，需要与 `base.wxml` 中的模板名对应（如 `tmpl_0_76`）
  - 其他字段：定义属性在模板中的简写映射，优化数据传输大小

---

### 步骤 2：创建 Web 渲染视图导出类 (渲染层)

在 `miniApp/src/jsMain/kotlin/components/` 目录下创建渲染视图类。

**文件：`KRCustomButtonView.kt`**

```kotlin
package components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import dom.MiniButtonViewElement
import org.w3c.dom.Element

class KRCustomButtonView : IKuiklyRenderViewExport {
    private val customButtonElement = MiniButtonViewElement()
    
    override val ele: Element
        get() = customButtonElement.unsafeCast<Element>()

    private var getPhoneNumberCallback: KuiklyRenderCallback? = null

    // 处理属性设置
    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            OPEN_TYPE -> {
                customButtonElement.openType = propValue as String
                true
            }
            GET_PHONE_NUMBER_CALLBACK -> {
                getPhoneNumberCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    // 监听小程序事件
    init {
        customButtonElement.addEventListener("getphonenumber", {
            getPhoneNumberCallback?.invoke(mapOf("data" to JSON.stringify(it.detail)))
        })
    }
    
    companion object {
        const val OPEN_TYPE = "openType"
        const val GET_PHONE_NUMBER_CALLBACK = "getPhoneNumberCallback"
        const val VIEW_NAME = "KRCustomButtonView"
    }
}
```

**关键点：**
- 实现 `IKuiklyRenderViewExport` 接口
- `setProp`：处理从业务层传递的属性
- `addEventListener`：监听小程序原生事件（如 `getphonenumber`）
- `KuiklyRenderCallback`：将小程序事件回调到业务层

---

### 步骤 3：注册自定义组件

在 `KuiklyWebRenderViewDelegator.kt` 中注册组件和模板映射。

**文件：`KuiklyWebRenderViewDelegator.kt`**

```kotlin
import components.KRCustomButtonView
import dom.MiniButtonViewElement

class KuiklyWebRenderViewDelegator : KuiklyRenderViewDelegatorDelegate {
    
    override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalRenderView(kuiklyRenderExport)

        // 1. 注册模板别名映射
        Transform.addComponentsAlias(
            MiniButtonViewElement.NODE_NAME,
            MiniButtonViewElement.componentsAlias
        )

        // 2. 注册自定义视图
        kuiklyRenderExport.renderViewExport(KRCustomButtonView.VIEW_NAME, {
            KRCustomButtonView()
        })
    }
}
```

**关键点：**
- `Transform.addComponentsAlias`：注册组件的属性映射配置
- `renderViewExport`：注册自定义视图的工厂方法

---

### 步骤 4：添加 WXML 模板 (模板层)

在 `miniApp/dist/base.wxml` 中添加对应的模板定义。

**文件：`base.wxml`**

```xml
<template name="tmpl_0_76">
  <button 
    class="{{i.cl}}" 
    size="{{xs.b(i.size,'default')}}" 
    type="{{i.type}}" 
    plain="{{xs.b(i.plain,false)}}" 
    disabled="{{i.disabled}}" 
    loading="{{xs.b(i.loading,false)}}" 
    form-type="{{i.formType}}" 
    open-type="{{i.openType}}" 
    name="{{i.name}}" 
    lang="{{xs.b(i.lang,en)}}" 
    session-from="{{i.sessionFrom}}"  
    app-parameter="{{i.appParameter}}" 
    show-message-card="{{xs.b(i.showMessageCard,false)}}" 
    business-id="{{i.businessId}}" 
    bindtouchstart="eh" 
    bindtouchmove="eh" 
    bindtouchend="eh" 
    bindtouchcancel="eh" 
    bindlongpress="eh" 
    bindgetuserinfo="eh" 
    bindcontact="eh" 
    bindgetphonenumber="eh" 
    bindchooseavatar="eh" 
    binderror="eh" 
    bindopensetting="eh" 
    bindlaunchapp="eh" 
    style="{{i.st}}" 
    bindtap="eh"  
    id="{{i.uid||i.sid}}" 
    data-sid="{{i.sid}}">
    <block wx:for="{{i.cn}}" wx:key="sid">
      <template is="{{xs.a(c, item.nn, l)}}" data="{{i:item,c:c+1}}" />
    </block>
  </button>
</template>
```

**关键点：**
- 模板名称 `tmpl_0_76` 对应 `componentsAlias` 中的 `_num: '76'`
- 属性命名：`i.openType` 对应 `componentsAlias` 中的 `openType: 'openType'`
- 事件绑定：所有事件统一绑定到 `eh` 事件处理器
- 嵌套子元素：通过 `i.cn` 渲染子元素

---

### 步骤 5：实现跨平台业务组件 (业务层)

在 `shared/src/commonMain/kotlin/` 中创建 Compose 风格的 API。

**文件：`CustomButton.kt`**

```kotlin
package com.example.kuiklyworkwithminiapp.components

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAttr

// 自定义视图类
internal class CustomButtonView: ComposeView<CustomButtonAttr, CustomButtonEvent>() {

    override fun createEvent(): CustomButtonEvent {
        return CustomButtonEvent()
    }

    override fun createAttr(): CustomButtonAttr {
        return CustomButtonAttr().apply {
            overflow(true)
        }
    }

    // 根据运行平台返回不同的视图名称
    override fun viewName(): String {
        val pageData = getPager().pageData
        if (pageData.params.optString("is_miniprogram") == "1") {
            return "KRCustomButtonView"  // 小程序平台使用自定义组件
        }
        return ViewConst.TYPE_VIEW       // 其他平台使用普通 View
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                justifyContentCenter()
                alignItemsCenter()
            }
            // 渲染按钮文本
            ctx.attr.titleAttrInit?.also { textAttr ->
                Text {
                    attr(textAttr)
                }
            }
        }
    }
}

// 属性类
internal class CustomButtonAttr : ComposeAttr() {
    internal var titleAttrInit: (TextAttr.()->Unit)? = null
    
    fun titleAttr(init: TextAttr.()->Unit) {
        titleAttrInit = init
    }
    
    fun openType(type: String): CustomButtonAttr {
        "openType" with type  // 设置 open-type 属性
        return this
    }
}

// 事件类
internal class CustomButtonEvent : ComposeEvent() {
    // 参数按实际填写类型
    fun onGetPhoneNumber(handler: (phoneNumberData: JSONObjet) -> Unit) {
        this.register(GET_PHONE_NUMBER_CALLBACK) {
            handler(it as JSONObjet)
        }
    }
    
    companion object {
        const val GET_PHONE_NUMBER_CALLBACK = "getPhoneNumberCallback"
    }
}

// DSL 扩展函数
internal fun ViewContainer<*, *>.CustomButton(init: CustomButtonView.() -> Unit) {
    addChild(CustomButtonView(), init)
}
```

**关键点：**
- `viewName()`：根据平台返回不同的视图名称，实现跨平台适配
- `"openType" with type`：通过 `with` 操作符设置自定义属性
- `register()`：注册事件回调，与渲染层的事件监听对应

---

### 步骤 6：业务使用示例

**文件：`CustomButtonPage.kt`**

```kotlin
package com.example.kuiklyworkwithminiapp

import com.example.kuiklyworkwithminiapp.components.CustomButton
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.log.KLog

@Page("customButton", supportInLocal = true)
internal class CustomButtonPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            View {
                attr {
                    width(200f)
                    height(200f)
                    margin(60f)
                }
                
                // 获取手机号按钮
                CustomButton {
                    attr {
                        width(150f)
                        height(40f)
                        borderRadius(10f)
                        backgroundColor(Color.YELLOW)
                        titleAttr {
                            text("getPhoneNumber")
                        }
                        openType("getPhoneNumber")  // 设置 open-type
                    }
                    event {
                        onGetPhoneNumber {  // 监听获取手机号事件
                            val callbackData = it as JSONObject
                            val data = callbackData.optString("data")
                            val dataObj = JSONObject(data)
                            KLog.d("onGetPhoneNumber", dataObj.toString())
                        }
                    }
                }

                // 打开设置按钮
                CustomButton {
                    attr {
                        titleAttr {
                            text("openSetting")
                        }
                        openType("openSetting")
                    }
                }
            }
        }
    }
}
```

## 支持的小程序内置组件属性

以 `button` 组件为例，支持的主要属性：

| 属性 | 类型 | 说明 |
|-----|------|------|
| `size` | String | 按钮大小 (default/mini) |
| `type` | String | 按钮样式 (primary/default/warn) |
| `plain` | Boolean | 是否镂空 |
| `disabled` | Boolean | 是否禁用 |
| `loading` | Boolean | 是否显示加载状态 |
| `form-type` | String | 表单类型 (submit/reset) |
| `open-type` | String | 开放能力类型 |
| `lang` | String | 语言 (en/zh_CN/zh_TW) |
| `session-from` | String | 会话来源 |
| `app-parameter` | String | 打开 APP 时传递的参数 |
| `show-message-card` | Boolean | 是否显示会话内消息卡片 |
| `business-id` | Number | 客服消息业务 ID |

### open-type 支持的开放能力

| 值 | 说明 |
|---|-----|
| `contact` | 打开客服会话 |
| `share` | 触发转发 |
| `getPhoneNumber` | 获取用户手机号 |
| `getUserInfo` | 获取用户信息 |
| `launchApp` | 打开 APP |
| `openSetting` | 打开授权设置页 |
| `feedback` | 打开意见反馈页面 |
| `chooseAvatar` | 获取用户头像 |

---

## 最佳实践

### 1. 属性命名规范

- **DOM 层**：使用驼峰命名（`openType`）
- **模板层**：使用小程序规范（`open-type`）
- **映射配置**：保持一致性

### 2. 事件回调数据格式

统一使用 Map 格式传递数据：

```kotlin
customButtonElement.addEventListener("getphonenumber", {
    getPhoneNumberCallback?.invoke(mapOf("data" to JSON.stringify(it.detail)))
})
```

### 3. 跨平台降级策略

在非小程序平台提供降级实现：(按实际情况处理，也可以在其他平台也实现自定义按钮)

```kotlin
override fun viewName(): String {
    if (isMiniProgram) {
        return "KRCustomButtonView"  // 小程序原生能力
    }
    return ViewConst.TYPE_VIEW       // 降级为普通视图
}
```

### 4. 属性默认值处理

在模板中使用 `xs.b()` 函数提供默认值：

```xml
<button 
  size="{{xs.b(i.size,'default')}}" 
  plain="{{xs.b(i.plain,false)}}" 
/>
```

### 5. 模块化组织

建议的目录结构：

```
miniApp/src/jsMain/kotlin/
├── dom/                           # DOM 元素定义
│   ├── MiniButtonViewElement.kt
│   └── MiniWebViewElement.kt
├── components/                    # 渲染视图实现
│   ├── KRCustomButtonView.kt
│   └── KRWebView.kt
└── KuiklyWebRenderViewDelegator.kt  # 统一注册

shared/src/commonMain/kotlin/
└── components/                    # 业务层组件
    └── CustomButton.kt
```

---

## 扩展其他内置组件

参考 `button` 的实现，可以快速集成其他小程序内置组件（如 `input`、`video`、`map` 等）：

### 快速集成清单

1. **创建 DOM 元素类**
   - 继承 `MiniElement`
   - 定义 `NODE_NAME` 和 `componentsAlias`
   - 添加属性的 setter

2. **创建渲染视图类**
   - 实现 `IKuiklyRenderViewExport`
   - 重写 `setProp` 处理属性
   - 使用 `addEventListener` 监听事件

3. **注册组件**
   - 在 `KuiklyWebRenderViewDelegator` 中调用 `Transform.addComponentsAlias`
   - 调用 `renderViewExport` 注册视图工厂

4. **添加 WXML 模板**
   - 在 `base.wxml` 中添加对应模板
   - 模板名称与 `_num` 对应

5. **实现业务层 API**
   - 创建 `ComposeView`、`ComposeAttr`、`ComposeEvent`
   - 提供 DSL 扩展函数

---

## 常见问题

### Q1: 为什么需要 `componentsAlias`？

**A:** `componentsAlias` 用于优化数据传输大小。通过将长属性名映射为短名称（如 `animation` -> `p0`），可以减少小程序渲染数据的体积。

### Q2: 模板编号 `_num` 如何选择？

**A:** `_num` 需要全局唯一，建议查看 `base.wxml` 中已有的模板编号，选择一个未使用的数字。

### Q3: 如何调试自定义组件？

**A:** 
1. 在微信开发者工具中打开小程序
2. 查看 Console 中的日志输出
3. 检查 WXML 渲染结果是否正确
4. 使用 `KLog.d()` 输出关键信息

### Q4: 事件回调没有触发怎么办？

**A:** 检查以下几点：
1. WXML 模板中是否正确绑定了事件（如 `bindgetphonenumber="eh"`）
2. `addEventListener` 的事件名称是否正确（小程序事件名是小写）
3. 业务层是否正确注册了事件回调

### Q5: 如何支持小程序组件的插槽（slot）？

**A:** 通过 `i.cn`（children nodes）渲染子元素：

```xml
<button>
  <block wx:for="{{i.cn}}" wx:key="sid">
    <template is="{{xs.a(c, item.nn, l)}}" data="{{i:item,c:c+1}}" />
  </block>
</button>
```

## 总结

通过本指南，你可以：

1. ✅ 理解 Kuikly 自定义组件的分层架构
2. ✅ 掌握集成微信小程序内置组件的完整流程
3. ✅ 学会处理小程序特有的事件回调机制
4. ✅ 实现跨平台的业务组件 API
5. ✅ 快速扩展支持其他小程序内置组件

**核心思想**：将小程序原生能力封装为 Kuikly 的 Compose 风格 API，实现业务代码的跨平台复用，同时保留小程序平台的原生能力。

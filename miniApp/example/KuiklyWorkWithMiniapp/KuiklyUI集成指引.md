# KuiklyUI 微信小程序集成指引

本文档指导开发者如何将 KuiklyUI 编译产物集成到已有的微信小程序项目中，实现原生页面与 KuiklyUI 页面共存。

## 📋 目录

- [前置准备](#前置准备)
- [集成步骤](#集成步骤)
- [创建 KuiklyUI 页面](#创建-kuiklyui-页面)
- [页面跳转](#页面跳转)
  - [从原生页面跳转到 KuiklyUI 页面](#从原生页面跳转到-kuiklyui-页面)
  - [从 KuiklyUI 页面跳转到原生页面](#从-kuiklyui-页面跳转到原生页面)
  - [KuiklyUI 页面之间跳转](#kuiklyui-页面之间跳转)
  - [路由跳转实现原理](#路由跳转实现原理)
  - [完整跳转示例](#完整跳转示例)
- [注意事项](#注意事项)
- [常见问题](#常见问题)

---

## 前置准备

### 1. 编译 KuiklyUI 产物

首先需要编译 KuiklyUI 项目，生成微信小程序所需的文件。

```bash
# 开发环境编译
./gradlew jsMiniAppDevelopmentWebpack

# 生产环境编译（推荐）
./gradlew jsMiniAppProductionWebpack
```

编译完成后，产物位于 `miniApp/dist/` 目录。

### 2. 需要的核心文件

从 `miniApp/dist/` 目录中，你需要以下文件：

#### 必需文件（7个）

| 文件路径 | 大小 | 说明 |
|---------|------|------|
| `lib/miniApp.js` | ~2MB | KuiklyUI 核心渲染引擎 |
| `business/nativevue2.js` | ~1.9MB | 业务逻辑代码 |
| `base.wxml` | ~27KB | 基础模板文件 |
| `comp.js` / `comp.json` / `comp.wxml` | <1KB | 核心组件 |
| `custom-wrapper.js` / `.json` / `.wxml` | <1KB | 自定义包装器组件 |
| `utils.wxs` | ~2KB | 工具函数 |

#### 可选文件

| 文件路径 | 说明 |
|---------|------|
| `assets/*` | 静态资源（如果业务代码中使用） |
| `app.wxss` | 全局样式（可根据需要选择性复制） |

---

## 集成步骤

### 步骤 1：复制核心文件

将上述必需文件复制到你的微信小程序项目**根目录**，保持目录结构不变：

```
your-miniapp-project/
├── lib/
│   └── miniApp.js          ← 复制
├── business/
│   └── nativevue2.js       ← 复制
├── base.wxml               ← 复制
├── comp.js                 ← 复制
├── comp.json               ← 复制
├── comp.wxml               ← 复制
├── custom-wrapper.js       ← 复制
├── custom-wrapper.json     ← 复制
├── custom-wrapper.wxml     ← 复制
├── utils.wxs               ← 复制
├── pages/                  ← 你的原有页面
├── app.js                  ← 保持不变
├── app.json                ← 需要修改
└── project.config.json     ← 保持不变
```

**重要提示**：
- ✅ **不需要修改** `app.js`
- ✅ **不需要修改** `project.config.json`
- ⚠️ **只需要修改** `app.json` 添加页面路由

---

## 创建 KuiklyUI 页面

### 步骤 2：创建页面目录

在 `pages/` 下创建你的 KuiklyUI 页面目录，例如 `pages/router/`：

```
pages/
├── router/              ← 新建目录
│   ├── index.js
│   ├── index.json
│   ├── index.wxml
│   └── index.wxss       (可选)
└── ... (其他原有页面)
```

### 步骤 3：编写页面文件

#### 📄 `pages/router/index.js`

```javascript
var business = require('../../business/nativevue2.js')
var render = require('../../lib/miniApp.js')

global.com = business.com;
global.callKotlinMethod = business.callKotlinMethod;

render.renderView({
    pageName: "router"  // 页面名称，与业务代码中定义的路由名对应
})
```

**关键点说明**：
- `business` 和 `render` 的路径是相对于当前页面的
- `global.com` 和 `global.callKotlinMethod` 必须设置
- `pageName` 需要与你在业务代码（Kotlin）中定义的路由名称一致

#### 📄 `pages/router/index.json`

```json
{
  "navigationBarTitleText": "页面标题",
  "disableScroll": true,
  "usingComponents": {
    "custom-wrapper": "../../custom-wrapper",
    "comp": "../../comp"
  },
  "navigationStyle": "custom"
}
```

**配置说明**：
- `disableScroll: true` - **必需**，禁用原生滚动，由 KuiklyUI 接管滚动
- `usingComponents` - **必需**，注册 KuiklyUI 核心组件
- `navigationStyle: "custom"` - **可选**，如果需要自定义导航栏

#### 📄 `pages/router/index.wxml`

```xml
<import src="../../base.wxml"/>
<template is="kuikly_tmpl" data="{{root:root}}" />
```

**说明**：
- 第一行引入 KuiklyUI 基础模板
- 第二行使用模板并传入 `root` 数据

#### 📄 `pages/router/index.wxss` (可选)

```css
/* 根据需要添加页面级样式 */
```

### 步骤 4：注册页面路由

在 `app.json` 中添加新创建的 KuiklyUI 页面：

```json
{
  "pages": [
    "pages/index/index",          // 原有页面
    "pages/list/list",            // 原有页面
    "pages/router/index",         // ← 新增 KuiklyUI 页面
    "pages/settings/index"        // ← 新增 KuiklyUI 页面（如果有）
  ],
  "window": {
    // ... 保持原有配置
  }
}
```

**注意**：
- 页面顺序会影响小程序启动时的首页（第一个为首页）
- 可以将 KuiklyUI 页面放在任意位置

---

## 页面跳转

### 从原生页面跳转到 KuiklyUI 页面

使用微信小程序标准导航 API：

```javascript
// 在原生页面中
wx.navigateTo({
  url: '/pages/router/index'
})

// 或使用 uni-app API
uni.navigateTo({
  url: '/pages/router/index'
})
```

### 从 KuiklyUI 页面跳转到原生页面

在 Kotlin 业务代码中使用路由模块跳转，通过设置 `isMiniNativePage` 参数实现跳转到原生小程序页面：

**当前默认没有支持，需要重写KRRouterModule**

具体可以参考**KuiklyWorkWithMiniapp\miniApp\src\jsMain\kotlin\module\KRRouterModule.kt**

这里需要把KRRouterModule重写，并且重新注册，让Kuikly支持**isMiniNativePage**参数，做针对性处理

```kotlin
// 方式1：跳转到原生页面，不带参数
com.tencent.demo.callBridgeModule(
    "KRRouterModule",
    "openPage",
    JSONObject().apply {
        put("pageName", "list/list")  // 页面路径（相对于 pages 目录）
        put("pageData", JSONObject().apply {
            put("isMiniNativePage", "1")  // 标记为原生页面
        })
    }.toString()
)

// 方式2：跳转到原生页面，带参数
com.tencent.demo.callBridgeModule(
    "KRRouterModule",
    "openPage",
    JSONObject().apply {
        put("pageName", "detail/detail")
        put("pageData", JSONObject().apply {
            put("isMiniNativePage", "1")
            put("id", "123")              // 传递给原生页面的参数
            put("type", "product")        // 可以传递多个参数
        })
    }.toString()
)
```

**参数说明**：
- `pageName`：原生页面路径，相对于 `pages/` 目录，例如 `"list/list"` 对应 `/pages/list/list`
- `isMiniNativePage`：必须设置为 `"1"`，表示这是原生小程序页面
- 其他参数：会作为 URL 参数传递给原生页面，例如 `id=123&type=product`

**最终跳转 URL**：`/pages/list/list?id=123&type=product`

在原生页面中获取参数：

```javascript
// pages/detail/detail.js
Page({
  onLoad(options) {
    console.log(options.id)     // "123"
    console.log(options.type)   // "product"
  }
})
```

### KuiklyUI 页面之间跳转

使用 KuiklyUI 内部路由机制（不设置 `isMiniNativePage`）：

```kotlin
// 方式1：简单跳转
com.tencent.demo.callBridgeModule(
    "KRRouterModule",
    "openPage",
    JSONObject().apply {
        put("pageName", "settings")  // KuiklyUI 页面名称
        put("pageData", JSONObject())
    }.toString()
)

// 方式2：带参数跳转
com.tencent.demo.callBridgeModule(
    "KRRouterModule",
    "openPage",
    JSONObject().apply {
        put("pageName", "settings")
        put("pageData", JSONObject().apply {
            put("userId", "456")
            put("from", "index")
        })
    }.toString()
)
```

### 路由跳转实现原理

KuiklyUI 的路由模块 `KRRouterModule` 通过判断 `isMiniNativePage` 参数来决定跳转方式：

1. **原生页面跳转**（`isMiniNativePage = "1"`）：
   - 使用微信小程序原生 API `wx.navigateTo`
   - 目标 URL：`/pages/{pageName}?{params}`
   - 适用于跳转到非 KuiklyUI 页面

2. **KuiklyUI 页面跳转**（不设置 `isMiniNativePage`）：
   - 使用 `window.open` 或内部路由机制
   - 在 KuiklyUI 渲染引擎内部处理
   - 适用于 KuiklyUI 页面之间的导航

### 完整跳转示例

假设你有以下页面结构：

```
pages/
├── index/           # 原生首页
├── list/            # 原生列表页
├── detail/          # 原生详情页
├── router/          # KuiklyUI 路由页
└── settings/        # KuiklyUI 设置页
```

#### 示例1：首页跳转到 KuiklyUI 路由页

```javascript
// pages/index/index.js
Page({
  goToKuiklyPage() {
    wx.navigateTo({
      url: '/pages/router/index'
    })
  }
})
```

#### 示例2：KuiklyUI 路由页跳转到原生列表页

```kotlin
// 在 KuiklyUI 业务代码中
Button(
    text = "查看列表",
    onClick = {
        com.tencent.demo.callBridgeModule(
            "KRRouterModule",
            "openPage",
            JSONObject().apply {
                put("pageName", "list/list")
                put("pageData", JSONObject().apply {
                    put("isMiniNativePage", "1")
                })
            }.toString()
        )
    }
)
```

#### 示例3：KuiklyUI 路由页跳转到原生详情页（带参数）

```kotlin
// 在 KuiklyUI 业务代码中
fun navigateToDetail(itemId: String) {
    com.tencent.demo.callBridgeModule(
        "KRRouterModule",
        "openPage",
        JSONObject().apply {
            put("pageName", "detail/detail")
            put("pageData", JSONObject().apply {
                put("isMiniNativePage", "1")
                put("id", itemId)
                put("from", "kuikly")
            })
        }.toString()
    )
}
```

```javascript
// pages/detail/detail.js
Page({
  onLoad(options) {
    console.log('商品ID:', options.id)      // 获取传递的参数
    console.log('来源:', options.from)      // "kuikly"
    // 加载详情数据...
  }
})
```

#### 示例4：KuiklyUI 页面之间跳转

```kotlin
// 从路由页跳转到设置页
com.tencent.demo.callBridgeModule(
    "KRRouterModule",
    "openPage",
    JSONObject().apply {
        put("pageName", "settings")
        put("pageData", JSONObject().apply {
            put("userId", currentUserId)
        })
    }.toString()
)
```

---

## 注意事项

### 1. 文件大小

- `lib/miniApp.js` (~2MB) + `business/nativevue2.js` (~1.9MB) ≈ **3.9MB**
- 微信小程序主包限制为 2MB，**必须使用分包策略**
- 建议将 KuiklyUI 相关文件放入分包

**分包配置示例**：

```json
{
  "pages": [
    "pages/index/index"
  ],
  "subPackages": [
    {
      "root": "kuikly-pages",
      "pages": [
        "router/index",
        "settings/index"
      ]
    }
  ]
}
```

分包后文件结构：

```
your-miniapp-project/
├── lib/                    ← 放入分包目录
├── business/               ← 放入分包目录
├── base.wxml               ← 保持在根目录
├── comp.*                  ← 保持在根目录
├── kuikly-pages/           ← 分包目录
│   ├── lib/
│   ├── business/
│   ├── router/
│   └── settings/
└── pages/                  ← 主包页面
```

### 2. 配置要求

#### project.config.json 关键配置

确保以下配置项正确：

```json
{
  "setting": {
    "es6": true,                    // 必需：支持 ES6
    "minified": false,              // 开发时建议关闭
    "disableSWC": true,             // 必需：禁用 SWC 编译器
    "uploadWithSourceMap": true,    // 推荐：便于调试
    "bigPackageSizeSupport": true   // 推荐：支持大包
  }
}
```

### 3. 全局变量

每个 KuiklyUI 页面的 `index.js` 中必须设置：

```javascript
global.com = business.com;
global.callKotlinMethod = business.callKotlinMethod;
```

这些全局变量用于 KuiklyUI 与业务代码的通信。

### 4. 页面配置

KuiklyUI 页面的 `index.json` 中的关键配置：

```json
{
  "disableScroll": true,          // 必需：禁用原生滚动
  "usingComponents": {            // 必需：注册组件
    "custom-wrapper": "../../custom-wrapper",
    "comp": "../../comp"
  }
}
```

### 5. 样式隔离

- KuiklyUI 有自己的样式系统
- 原生页面样式不会影响 KuiklyUI 页面
- 如需自定义样式，可在页面的 `.wxss` 文件中添加

### 6. 路由参数

#### 跳转到 KuiklyUI 页面

如果需要从原生页面传递参数到 KuiklyUI 页面：

```javascript
// 原生页面跳转时带参数
wx.navigateTo({
  url: '/pages/router/index?id=123&type=detail'
})
```

在 KuiklyUI 业务代码中可以通过路由参数获取。

#### 跳转到原生页面

使用路由模块跳转时，通过 `pageData` 传递参数：

```kotlin
com.tencent.demo.callBridgeModule(
    "KRRouterModule",
    "openPage",
    JSONObject().apply {
        put("pageName", "detail/detail")
        put("pageData", JSONObject().apply {
            put("isMiniNativePage", "1")  // 标记为原生页面
            put("id", "123")              // 业务参数
            put("type", "detail")         // 业务参数
        })
    }.toString()
)
```

**重要提示**：
- `isMiniNativePage` 参数会被自动移除，不会传递给目标页面
- 其他所有参数都会作为 URL 参数传递
- 原生页面通过 `onLoad(options)` 接收参数

### 7. 路由模块配置

KuiklyUI 使用 `KRRouterModule` 处理页面跳转，该模块已内置在 `lib/miniApp.js` 中。

**模块功能**：
- ✅ 支持跳转到原生小程序页面（设置 `isMiniNativePage = "1"`）
- ✅ 支持跳转到 KuiklyUI 页面（不设置 `isMiniNativePage`）
- ✅ 支持参数传递
- ✅ 自动处理 URL 拼接

**调用方式**：
```kotlin
com.tencent.demo.callBridgeModule(
    "KRRouterModule",    // 模块名称
    "openPage",          // 方法名称：openPage 或 closePage
    params.toString()    // JSON 字符串参数
)
```

---

## 常见问题

### Q1: 页面显示空白

**可能原因**：
1. `base.wxml` 文件路径不正确
2. 组件未正确注册
3. `pageName` 与业务代码不匹配

**解决方案**：
```javascript
// 检查文件路径是否正确
var render = require('../../lib/miniApp.js')  // 确保路径正确

// 检查 pageName 是否与 Kotlin 代码中定义的一致
render.renderView({
    pageName: "router"  // 必须与业务代码匹配
})
```

### Q2: 提示 "Cannot read property 'com' of undefined"

**原因**：未设置全局变量

**解决方案**：
```javascript
// 在 render.renderView() 之前添加
global.com = business.com;
global.callKotlinMethod = business.callKotlinMethod;
```

### Q3: 小程序包超过 2MB 限制

**解决方案**：使用分包（见上文"文件大小"章节）

### Q4: 页面滚动异常

**原因**：未禁用原生滚动

**解决方案**：
```json
// pages/xxx/index.json
{
  "disableScroll": true  // 必须设置为 true
}
```

### Q5: 样式显示不正常

**可能原因**：
1. 未引入 `app.wxss`（如果需要）
2. 样式冲突

**解决方案**：
- 检查是否需要复制 `miniApp/dist/app.wxss` 到项目根目录
- 使用更具体的样式选择器避免冲突

### Q6: 从 KuiklyUI 跳转原生页面失败

**可能原因**：
1. 未设置 `isMiniNativePage` 参数
2. `pageName` 路径不正确
3. 目标页面未在 `app.json` 中注册

**解决方案**：
```kotlin
// 确保设置了 isMiniNativePage = "1"
com.tencent.demo.callBridgeModule(
    "KRRouterModule",
    "openPage",
    JSONObject().apply {
        put("pageName", "list/list")  // 确保路径正确
        put("pageData", JSONObject().apply {
            put("isMiniNativePage", "1")  // 必须设置
        })
    }.toString()
)

// 检查 app.json 中是否已注册目标页面
// "pages": ["pages/list/list", ...]
```

### Q7: 跳转后参数获取不到

**原因**：参数传递方式不正确

**解决方案**：
```kotlin
// 正确：参数放在 pageData 中（除了 isMiniNativePage）
put("pageData", JSONObject().apply {
    put("isMiniNativePage", "1")
    put("id", "123")        // ✅ 正确
    put("type", "detail")   // ✅ 正确
})

// 错误：参数放在顶层
put("id", "123")  // ❌ 错误，不会被传递
```

## 完整示例

### 示例项目结构

```
my-miniapp/
├── lib/
│   └── miniApp.js
├── business/
│   └── nativevue2.js
├── base.wxml
├── comp.js / comp.json / comp.wxml
├── custom-wrapper.js / .json / .wxml
├── utils.wxs
├── pages/
│   ├── index/              # 原生首页
│   │   ├── index.js
│   │   ├── index.json
│   │   ├── index.wxml
│   │   └── index.wxss
│   ├── list/               # 原生列表页
│   ├── router/             # KuiklyUI 路由页
│   │   ├── index.js
│   │   ├── index.json
│   │   └── index.wxml
│   └── settings/           # KuiklyUI 设置页
│       ├── index.js
│       ├── index.json
│       └── index.wxml
├── app.js
├── app.json
└── project.config.json
```

### app.json 配置示例

```json
{
  "pages": [
    "pages/index/index",
    "pages/list/list",
    "pages/router/index",
    "pages/settings/index"
  ],
  "window": {
    "navigationBarTextStyle": "black",
    "navigationBarTitleText": "我的应用",
    "navigationBarBackgroundColor": "#F8F8F8",
    "backgroundColor": "#F8F8F8"
  },
  "usingComponents": {}
}
```

---

## 更新维护

### 更新 KuiklyUI 版本

1. 重新编译：`./gradlew jsMiniAppProductionWebpack`
2. 替换 `lib/miniApp.js` 文件

### 更新业务代码

1. 修改 `shared/` 目录中的 Kotlin 代码
2. 重新编译：`./gradlew jsMiniAppProductionWebpack`
3. 替换 `business/nativevue2.js` 文件

### 更新静态资源

1. 修改 `shared/src/commonMain/assets/` 中的资源
2. 编译后会自动复制到 `miniApp/dist/assets/`
3. 将更新后的 `assets/` 目录同步到项目

---

## 技术支持

如有问题，请参考：
- KuiklyUI 官方文档
- 本项目的示例代码：`uni-app/dist-with-kuikly/`

---

**最后更新**：2025-12-12

# 微信小程序自定义字体使用指引

在微信小程序环境下，使用自定义字体时，需要在字体加载完成后触发一次文本的重新测量。原因是：小程序自定义字体是异步加载的，Kuikly 初始化时字体可能还没有加载完成，从而测量出的尺寸是默认字体的尺寸，会导致自定义字体的文本显示不全等问题。

## 接入步骤

### 1. 加载字体文件

微信小程序加载自定义字体有两种方式：**本地加载**和**远程加载**。

---

#### 方式一：本地加载（推荐）

由于微信小程序的 `wx.loadFontFace` 不支持直接使用代码包内的相对路径，需要将字体文件转为 **base64 Data URI** 格式嵌入到 JS 文件中。

**步骤 1：将字体文件转为 base64 JS 模块**

使用以下命令将 `.ttf` 文件转为 base64 编码的 JS 模块：

```bash
# 将字体文件转为 base64 data URI 并导出为 JS 模块
echo "module.exports = 'data:font/truetype;charset=utf-8;base64,$(base64 -i your-font.ttf)';" > your-font.js
```

将生成的 JS 文件放到 `miniApp/dist/assets/fonts/` 目录下。

**步骤 2：在 app.js 中加载字体**

```javascript
// miniApp/dist/app.js

var render = require('./lib/miniprogramApp.js')

// Load custom fonts
global.loadCustomFonts = function() {
    try {
        // require base64 encoded font data URI
        var fontDataUri = require('./assets/fonts/MyFont.js')

        wx.loadFontFace({
            global: true,
            family: 'MyFont',
            source: 'url("' + fontDataUri + '")',
            scopes: ['webview', 'native'],
            success: function(res) {
                // Notify Kuikly to re-measure text with correct font metrics
                render.fontLoaded()
            },
            fail: function(err) {
                console.warn('loadFontFace failed:', err.errMsg || JSON.stringify(err))
            }
        })
    } catch(e) {
        console.warn('loadCustomFonts failed:', e.message || e)
    }
}

global.loadCustomFonts()
render.initApp()
```

> **注意**：本地加载方式会增加小程序包体积（base64 编码后体积约为原文件的 1.37 倍），适合字体文件较小的场景。

---

#### 方式二：远程加载

将字体文件部署到 CDN 或 HTTPS 服务器上，通过网络 URL 加载。

```javascript
// miniApp/dist/app.js

var render = require('./lib/miniprogramApp.js')

// Load custom fonts from remote URL
global.loadCustomFonts = function() {
    var fontUrl = 'https://your-cdn.com/fonts/MyFont.ttf'

    wx.loadFontFace({
        global: true,
        family: 'MyFont',
        source: 'url("' + fontUrl + '")',
        scopes: ['webview', 'native'],
        success: function(res) {
            // Notify Kuikly to re-measure text with correct font metrics
            render.fontLoaded()
        },
        fail: function(err) {
            console.warn('loadFontFace failed:', err.errMsg || JSON.stringify(err))
        }
    })
}

global.loadCustomFonts()
render.initApp()
```

> **注意**：
> - 远程 URL 必须是 HTTPS 协议
> - 需要在微信小程序后台配置 `downloadFile` 合法域名
> - 首次加载需要网络请求，可能存在短暂延迟

---

### 2. 关键调用说明

| 方法 | 说明 |
|------|------|
| `render.fontLoaded()` | 通知所有活跃的 Kuikly 页面字体已加载完成，触发文本重新测量 |

`render.fontLoaded()` 的内部逻辑：
1. 重置文本测量上下文
2. 清除文本测量缓存
3. 向所有活跃页面发送 `onFontLoaded` 事件，触发重新布局

### 3. 在 Kotlin 业务代码中使用自定义字体

在 Kuikly DSL 中，通过 `fontFamily` 属性指定自定义字体名称（需与 `wx.loadFontFace` 中的 `family` 一致）：

```kotlin
Text {
    attr {
        text("Hello Custom Font")
        fontSize(20f)
        fontFamily("MyFont")
    }
}
```

## 两种方式对比

| 对比项 | 本地加载（base64） | 远程加载（CDN） |
|--------|-------------------|----------------|
| 包体积 | 增大（约 1.37x 字体大小） | 不影响 |
| 首次加载速度 | 快（无网络请求） | 取决于网络 |
| 离线可用 | ✅ | ❌（首次需联网） |
| 适用场景 | 字体文件较小（< 100KB） | 字体文件较大 |
| 域名配置 | 不需要 | 需配置合法域名 |

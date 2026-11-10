# 如何引入第三方 JS-SDK

目前 Kuikly H5 和 小程序都是使用 Kotlin 开发，那如何引入外部第三方 JS SDK 呢

## H5 引入 JS-SDK 方法：

H5App 宿主工程中有提供 Html 文件，在 Html 的script 中引入 js 路径即可，参考 libpag.min.js 的引入
https://github.com/Tencent-TDS/KuiklyUI/blob/main/h5App/src/jsMain/resources/index.html

```html
// h5App/src/jsMain/resources/index.html
<script type="text/javascript" src="http://127.0.0.1:8083/nativevue2.js"></script>
<script type="text/javascript"
        src="https://cdn.jsdelivr.net/npm/libpag@latest/lib/libpag.min.js"></script>
<script type="text/javascript" src="h5App.js"></script>
</body>
```

## 微信小程序 引入 JS-SDK 方法：

miniApp 小程序宿主工程的 app.js 可以引入第三方 js sdk，但是小程序需要给全局对象 global 暴露接口
https://github.com/Tencent-TDS/KuiklyUI/blob/main/miniApp/dist/app.js

```javascript
// miniApp/dist/app.js

var business = require('./business/nativevue2')
var render = require('./lib/miniprogramApp.js')

// 兼容部分旧版小程序基础库缺失 globalThis 的情况
if (typeof globalThis !== 'undefined') {
    globalThis.com = business.com;
    globalThis.callKotlinMethod = business.callKotlinMethod;
}

global.com = business.com;
global.callKotlinMethod = business.callKotlinMethod;

global.getAssetJson = function(path) {
    var json = require('./assets/' + path.replace('.json','.js'))
    return json
}

render.initApp()
```

> 为什么同时给 `globalThis` 和 `global` 赋值：Kotlin 渲染层产物内部部分路径（例如 `MiniGlobal.globalThis.getAssetJson`）通过 `globalThis` 访问宿主注入的对象，而小程序引擎自身的包命名空间解析走的是 `global`；此外部分旧版基础库里不存在 `globalThis`，所以需要 `typeof` 守卫兜底。

小程序业务调用JS SDK接口示例：
https://github.com/Tencent-TDS/KuiklyUI/blob/main/miniApp/src/jsMain/kotlin/module/KRBridgeModule.kt

通过 MiniGlobal.globalThis 调用暴露的 JS SDK 接口

```javascript
// miniApp/src/jsMain/kotlin/module/KRBridgeModule.kt

"readAssetFile" -> {
val data = MiniGlobal.globalThis.getAssetJson(js("JSON.parse")(params).assetPath)
callback?.invoke((mapOf(
"result" to JSON.stringify(data)
)))
}
```

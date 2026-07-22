# Kuikly Web Render H5 宿主 App

## 项目介绍

本项目为 Kuikly Web Render H5 宿主 App 项目，用于运行 kuiklyCore 示例项目的 H5 版本，小程序版本请参考 miniApp 目录下的文档

## 接入文档

可以参考官网[KuiklyWeb工程接入](https://kuikly.tds.qq.com/QuickStart/Web.html)

## 快速开始

首先构建 demo 项目，得到业务逻辑的 JS 构建产物，并且运行 demo 的开发服务器以提供业务JS的服务
```shell
// 运行 demo serve 服务器，没有安装 npm包则先 npm install 安装一下依赖
npm run serve
// 构建 demo 项目 Debug 版产物(无混淆压缩)
// H5需要用远程ksp源，KuiklyUI仓库加上 -Pkuikly.useLocalKsp=false 参数
./gradlew :demo:packLocalJsBundleDebug -Pkuikly.useLocalKsp=false
// 或 Release 版产物(有混淆和压缩)
./gradlew :demo:packLocalJsBundleRelease -Pkuikly.useLocalKsp=false
```

然后构建 h5App 项目（持续构建可以加上 -t 参数）
```shell
// 运行 h5App 服务器 Debug 版, 部分demo需要配合运行./gradlew :h5App:copyAssetsToWebpackDevServer复制静态资源到对应开发目录
./gradlew :h5App:jsBrowserRun 或者 ./gradlew :h5App:jsBrowserDevelopmentRun'
// 或 Release 版
./gradlew :h5App:jsBrowserProductionRun
```
此时会自动拉起浏览器打开我们的测试页面 http://localhost:8080/ ，此时我们可以看到效果了，默认打开的是路由页，可以打开我们所开发的其他页面。
如果要访问不同的页面，可以通过 url 参数指定页面名称，如：http://localhost:8080/?page_name=router
也可以在路由页面的输入框中输入要跳转到的页面名称，并点击跳转，需要注意这里的页面名称使我们在开发时通过 @page 注解所注册的页面名称
>如果发现通过assets方式引入的图片无法看到，可以执行 ./gradlew :h5App:copyAssetsToWebpackDevServer 将 demo 中的 src/commonMain/assets 图片资源文件拷贝到 webpack 开发服务器的dist下
>这样就能在开发环境下看到 assets 图片了

如果只想构建得到构建产物，并不想运行开发服务器，可以执行

```shell
// 构建 h5App 生产环境产物
./gradlew :h5App:jsBrowserProductionWebpack
// 构建 h5App 开发环境产物
./gradlew :h5App:jsBrowserDevelopmentWebpack
```

对于 kotlin2.0 +版本 （当前默认）
开发环境构建产物在 h5App/build/kotlin-webpack/js/developmentExecutable 中
生产环境构建产物在 h5App/build/kotlin-webpack/js/productionExecutable 中

对于 kotlin1.9 版本
开发环境构建产物在 h5App/build/dist/js/developmentExecutable 中
生产环境构建产物在 h5App/build/dist/js/productionExecutable 中

>如果修改了 demo 项目的代码，需要重新执行 demo 项目的构建脚本 ./gradlew :demo:packLocalJsBundleDebug 或 ./gradlew :demo:packLocalJsBundleRelease
>如果发现项目首次 Sync 时不成功，可以尝试 Build/Clean Project 后再次执行

## 生产环境构建

业务开发完成后，需要构建生产环境的产物。如果要配置流水线，也需要执行此 gradle 脚本生成对应的产物

>如果业务规模较大，构建失败，可能是构建内存不足，可以先执行 export NODE_OPTIONS=--max_old_space_size=16384 提升 nodejs 的运行内存

- 统一构建

```shell
# 构建业务 h5App 和 JSBundle
# 首先构建业务 Bundle
# H5需要用远程ksp源，KuiklyUI仓库加上 -Pkuikly.useLocalKsp=false 参数
./gradlew :demo:packLocalJSBundleRelease -Pkuikly.useLocalKsp=false
# 然后构建宿主 APP
./gradlew :h5App:publishLocalJSBundle
```
>统一构建的产物为 nativevue2.js，同 Module 下的 Page 都集成到一个 JS 文件了
>业务构建产物在 h5App/dist/js/productionExecutable/page 下
>业务的 assets 资源在 h5App/build/dist/js/productionExecutable/assets 下
>h5App 构建产物在 h5App/build/dist/js/productionExecutable 下

- 分页构建
```shell
# 构建业务 h5App 和 JSBundle
# 首先构建业务 Bundle
./gradlew :demo:packSplitJSBundleRelease
# 然后构建 h5App
./gradlew :h5App:publishSplitJSBundle
```
>注意，需要分页构建的页面需要在 demo 项目的 build.gradle.kts 第 155 行添加需要分页构建的页面名称，或是在构建时的脚本参数中增加设置 -PsplitPageList=pageName1,pageName2,pageName3
```shell
addSplitPages(listOf("实际的页面名称"))
```
>分页构建的产物为分页面的 JS，demo 下每个 Page 生成一个对应的 JS 文件
>业务构建产物在 h5App/build/dist/js/productionExecutable/page 下
>h5App 构建产物在 h5App/build/dist/js/productionExecutable 下

## 构建产物说明

h5App是项目的宿主APP，依赖 webRender，构建得到 h5App.js，demo 则是具体业务，构建得到统一的 nativevue2.js 或者是 split 的分页 js 文件。
生产环境部署时 index.html 中会引入具体页面的 nativevue2.js 或 ${pageName}.js，以及 h5App.js，部署生产环境的 html 中业务和 h5App.js 的引用需要根据业务实际情况调整。

```html
<!-- index.html -->

<!-- 如果是构建统一的业务JSBundle，则使用此方式 -->
<script type="text/javascript" src="nativevue2.js"></script>
<!-- 分页的业务JSBundle，js都部署在page内，以路由页为例，需要替换成具体的 Page 名称 -->
<script type="text/javascript" src="page/router.js"></script>
<!-- 宿主 APP 和 webRender 的 JS文件 -->
<script type="text/javascript" src="h5App.js"></script>
```

另外因为 kuikly 支持 assets 方式引用项目 demo 目录下的 assets 中的图片，因此项目构建完成后，如果你有使用 assets 方式引用的图片，那么需要将 h5App/build/dist/js/productionExecutable/assets 目录整个拷贝
到你的 web 服务器根目录，这样项目才可以通过相对路径访问到图片，例如你的网站部署在 https://kuikly.qq.com/, 那么你的 assets 图片就要通过 https://kuikly.qq.com/assets/xxx/xxx.png 来访问了****

## 项目说明

项目入口在 Main.kt 的 main 方法中，其中 KuiklyRenderViewDelegator 用于注册外部自定义 View 和 Module 及 PropHandler，
宿主侧可以在此实现自定义的View，Module并注册到KuiklyRenderViewDelegator中。

项目构建完成之后会生成 h5App.js，我们在 resources/index.html 中对其进行引入。并且在 h5App.js 之前进行 demo 项目 js 的引入。在 main 方法中处理 URL 参数、路由参数及宿主的相关参数。
然后通过 KuiklyWebRenderViewDelegator.init 方法完成 KuiklyRenderView 的初始化，并在初始化完成后创建 kuikly view

## 开发说明

- 特殊样式设置

由于 Web 的某些特性比如滚动条是否隐藏必须通过 CSS 来设置，因此无法通过 DOM 编程实现，所以目前是通过在宿主 APP 的 html 文件内提前定义好 CSS 名称，然后在 项目内引用来实现的

- 新增 Module

如果业务需要新增自定义模块，请将模块放置在module目录中，模块需要继承 KuiklyRenderBaseModule 类，模块方法则需要重写 call 方法来自定义处理，模块定义好之后，要在 KuiklyRenderViewDelegator 的 registerExternalModule 中注册模块

- 新增 View

如果业务需要新增自定义View，请将模块放置在components目录中，View 需要实现 IKuiklyRenderViewExport 接口，并且传入实际的 DOM 元素的类型，View 定义好之后，要在 KuiklyRenderViewDelegator 的 registerExternalRenderView 中注册View

- 来源处理

如果业务逻辑中需要对来自 web 平台的进行特殊逻辑处理，可以在业务代码中通过 pageData.params.optString(is_web") == "1" 来进行处理，比如打开页面的 https 链接等等

- assets 资源处理
web 已支持项目中 assets 目录内图片资源的引用，但需要注意，assets 资源的引用有 ImageUri.pageAssets 和 ImageUri.commonAssets 两种方式，其中 commonAssets 方式引用的是 demo/src/commonMain/assets/common 目录内的图片，
pageAssets 方式引用的是 demo/src/commonMain/assets/{pageName}/内的图片，注意这里{pageName}一定是业务Page中@Page注解内的真实pageName，包括大小写，分隔符等。在部署时，需要将 h5App/build/dist/js/productionExecutable/assets 目录
整个拷贝到 web 项目根目录下，这样业务内通过 ImageUrl.pageAssets 和 ImageUri.commonAssets 所拿到的 assets 资源相对路径就能访问到对应的图片资源了

- 浏览器默认行为开关（`KuiklyProcessor` 全局配置）

WebRender 在 `KuiklyProcessor`（`com.tencent.kuikly.core.render.web.processor.KuiklyProcessor`）上暴露了几个全局布尔开关，
用于控制底层是否阻止一些浏览器默认交互。业务通常在 `Main.kt` 的入口处、`KuiklyRenderView` 初始化之前设置：

| 开关 | 类型 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `preventDefaultDragAndSelect` | `Boolean` | `true` | 兼容旧版的组合开关，赋值时会**同时**修改 `preventDefaultSelect` 与 `preventDefaultDrag` |
| `preventDefaultSelect` | `Boolean` | `true` | 是否阻止文本选中（`selectstart`）。关掉后 H5 页面上的文字可以被选中/复制 |
| `preventDefaultDrag` | `Boolean` | `true` | 是否阻止原生 HTML5 图片拖拽（`dragstart`）。**强烈建议保持 `true`**，否则原生拖拽会吞掉 `mousemove`/`mouseup`，导致 List 拖动状态卡住 |
| `preventDefaultContextMenu` | `Boolean` | `true`（默认阻止） | 是否阻止浏览器右键菜单 / 移动端长按系统菜单（`contextmenu` 事件）。PC / 移动端默认都阻止，业务需要右键 / 长按菜单时显式设为 `false` |
| `autoUpdateRootViewSizeOnResize` | `Boolean` | `false`（默认关闭） | 是否自动把浏览器 / 容器 resize 转发给 Kuikly，触发响应式布局。默认关闭，PC / 移动端都需要业务显式打开。详见下方"响应式布局"小节 |

`preventDefaultContextMenu` 语义如下：

- `true`（默认）：**PC / 移动端都阻止**。
  - PC 端鼠标右键不会弹出浏览器默认右键菜单；
  - 移动端长按不会弹出浏览器"复制 / 保存图片"等系统菜单，避免打断业务的 `longPress` / `pan` 手势。
  - 适合将右键 / 长按作为应用内部手势的页面（也是历史行为兼容的默认选项）。
- `false`：**PC / 移动端都放行**。
  - PC 端右键会弹出浏览器默认右键菜单（可用于复制、检查元素等）；
  - 移动端长按会弹出浏览器默认长按菜单（如"保存图片"）。
  - 适合希望保留浏览器原生交互的页面。

使用示例（放在 `h5App/src/jsMain/kotlin/Main.kt` 的 `main()` 顶部）：

```kotlin
// 业务需要允许右键 / 长按浏览器菜单（例如允许 PC 用户右键复制、允许移动端长按保存图片）：
KuiklyProcessor.preventDefaultContextMenu = false

// 默认行为（PC / 移动端都屏蔽浏览器默认菜单）无需设置，或显式保持：
// KuiklyProcessor.preventDefaultContextMenu = true
```

> 说明：该开关仅控制 WebRender 内部（`LongPressHandler` / `PanHandler`）在 `contextmenu` 事件上的 `preventDefault()` 调用，不会影响业务自行注册的 `contextmenu` 监听。业务侧仍可自由监听 `contextmenu` 事件实现自定义右键菜单。iOS Safari 的"长按预览/图片保存"由 `-webkit-touch-callout` 控制，若需要彻底禁用移动端长按菜单，可在业务 CSS 中额外设置 `-webkit-touch-callout: none;`。

- 响应式布局（运行时更新 rootViewSize）

传给 `KuiklyView.onAttach(container, pageName, pageData, size)` 的 `size` 只作用于**首次初始化**。当宿主容器随后被拉伸/压缩（桌面浏览器 window resize、侧边栏折叠、分栏拖动等）时，Kuikly 需要收到"根视图尺寸变化"的通知才能触发响应式重排。

WebRender 提供了两种方式来完成这件事，二者可同时存在：

**方式 A：命令式 API（业务全权控制）**

`KuiklyView` / `KuiklyRenderViewDelegator` 都暴露了 `updateRootViewSize(width, height)` 方法。业务在任何时机（自己监听 `resize`、`ResizeObserver`、CSS media query、外部脚本调整宽度等）拿到新的容器尺寸后，直接调用即可：

```kotlin
// 业务自己监听 resize
window.addEventListener("resize", {
    kuiklyView.updateRootViewSize(
        window.innerWidth,
        window.innerHeight
    )
})

// 或者监听指定容器（更贴合"部分区域嵌入 Kuikly"的桌面站场景）
val ro = js("new ResizeObserver(function(entries){ /* ... */ })")
ro.observe(container)
// 回调里调用 kuiklyView.updateRootViewSize(newW, newH)
```

内部实现为向 Kuikly Pager 派发 `rootViewSizeDidChanged` 事件，会同步更新 `PageData.pageViewWidth / pageViewHeight`（同时也会更新 `deviceWidth / deviceHeight`），Kuikly 业务代码里基于 `pageData.pageViewWidth` 的百分比 / flex 布局会自动重算。

**方式 B：自动模式（WebRender 内置转发器）**

通过 `KuiklyProcessor.autoUpdateRootViewSizeOnResize` 开关，让 WebRender 自动帮你把 `resize` 派发给 Kuikly：

| 值 | 语义 |
| --- | --- |
| `false`（默认） | **关闭**：WebRender 不做任何自动转发，PC / 移动端行为一致。业务如需响应式，请使用方式 A |
| `true` | **开启**：PC / 移动端都自动转发容器 / 窗口 resize |

> 默认关闭的原因：自动响应 resize 对页面布局的影响面较广（例如移动端软键盘弹起触发 `window.resize` 会导致重排，桌面端某些嵌入式布局也不希望 Kuikly 页面随容器缩放）。业务确认需要响应式布局时再显式打开即可。

自动模式的实现细节：

- 优先使用 `ResizeObserver` 观察真实的根 DOM 容器（`rootContainer`），能同时响应 window resize、侧栏折叠、外部脚本改宽度等所有触发容器几何变化的场景；旧浏览器不支持 `ResizeObserver` 时降级为 `window.resize`。
- 100 ms 节流（与 `H5WindowResizeModule` 保持一致），仅在新旧尺寸实际变化时才向 Kuikly 派发事件，避免同尺寸的重复重排。
- 生命周期跟随 `KuiklyView`：`onAttach` 后启动，`onDetach` 时自动反注册，无需业务手工清理。

使用示例：

```kotlin
// 桌面响应式站点：需要显式打开
KuiklyProcessor.autoUpdateRootViewSizeOnResize = true

// 默认关闭（推荐）：完全由业务用方式 A 掌控 resize 时机
// KuiklyProcessor.autoUpdateRootViewSizeOnResize = false
```

> 注意：
> 1. 响应式最终能不能生效，还取决于 Kuikly 业务页面是否使用了相对/百分比/flex 布局。若业务写死了绝对 `width` / `height`，光转发尺寸事件不会让页面变宽变窄。
> 2. 首次 `onAttach` 传入的 `size` 仍然是初始尺寸，自动转发器会以此为 baseline，只在**尺寸真的变化**后才第一次触发事件，不会引起首屏抖动。
> 3. 开启自动模式后，移动端也会跟随 `resize` 重排。iOS/Android 上软键盘弹起会缩小 `window.innerHeight`，可能引起不必要的布局跳变；如果只想响应"横竖屏切换"而不想响应软键盘，建议关掉自动模式、走方式 A 自行判断后再调用 `updateRootViewSize`。

## 多模块工程下 UMD 全局命名空间被覆盖问题

### 现象

在 `enableMultiModule = true` 的多模块工程（例如业务 shared 模块产出 `nativevue2.js`，`h5App` 模块产出 `h5App.js`，由 `JSMultiEntryBuilder` 分别打包）下，
从 Kuikly 2.19.0 起，页面加载后偶发以下错误：

```text
Cannot read properties of undefined (reading 'registerCallNative')
```

即 `window.com.tencent.kuikly.core.nvi` 分支在 `h5App.js` 加载后变成 `undefined`，桥接注册失败。

### 根因

- `nativevue2.js`（shared 模块）依赖 `core`，其 UMD exports 顶层 `com` 分支下含有 `com.tencent.kuikly.core.nvi.*`，不含 `render.web.*`。
- `h5App.js`（h5App 模块）依赖 `core-render-web:h5`。2.19+ 起 `core-render-web:base`/`h5` 新增了若干 `@file:JsExport` 顶层文件
  （`KuiklyView` / `IKuiklyView` / `KuiklyRenderViewDelegator` / `JSHelper` 等），使得 `h5App.js` 的 UMD exports 顶层也出现 `com` 键，
  但只含 `com.tencent.kuikly.core.render.web.*`，缺失 `nvi` 分支。
- kotlin-webpack 生成的 UMD 尾部对 `window` 侧是**逐 key 整体赋值**，不做深合并：

  ```js
  var a = factory();
  for (var i in a) (typeof exports === 'object' ? exports : root)[i] = a[i];
  ```

  当 `h5App.js` 后加载时，会把它自己的 `com` 整体覆盖到 `window.com`，从而抹掉 `nativevue2.js` 之前挂上的 `com.tencent.kuikly.core.nvi`。

单模块工程下所有代码打在同一个产物里，`com.tencent.kuikly.core` 分支合并挂载一次，因此不会出现这种覆盖问题；这也是"单模块项目升级 2.19+ 没有踩坑、多模块项目却报错"的原因。

### 处理方法

`webpack.config.d/` 下提供两个针对该问题的配置片段，**二选一**启用即可：

- **方案 X（默认启用，见 `webpack.config.d/output.js`）**：把 `h5App.js` 的输出方式从 UMD 改成 IIFE
  （`config.output.libraryTarget = undefined` + `config.output.iife = true`），
  让 `h5App.js` 不再向 `window` 暴露 UMD exports，从根源上避免整体覆盖 `window.com`。
  适用于 `h5App.js` 本身只作为可执行入口、不需要对外提供符号的场景（当前默认场景）。

- **方案 Y（备选，见 `webpack.config.d/kuikly-umd-deep-merge.js`）**：保留 UMD 输出，
  在 emit 之前重写 UMD 尾部，把"逐 key 覆盖挂全局"改成"逐 key 深合并挂全局"——已存在的对象分支做递归合并、
  已经存在的非对象值优先保留旧值。这样 `nativevue2.js` 与 `h5App.js` 各自挂到 `window.com` 的分支就能共存。
  适用于集成方仍要求 `h5App.js` 通过 UMD 对外暴露 `KuiklyView` 等符号、或由于历史原因无法关闭 UMD wrapper 的场景。

> ⚠️ 请勿同时启用两个方案。启用方案 Y 时，需要把 `output.js` 里 `libraryTarget`/`iife` 相关行注释掉，
> 否则 UMD 尾部会被提前抹掉，方案 Y 的字符串替换将匹配不到而失效。

如仅使用官方默认的 h5App 工程结构，保持 `output.js` 现状（方案 X）即可，`kuikly-umd-deep-merge.js` 仅在需要保留 UMD 输出时启用。

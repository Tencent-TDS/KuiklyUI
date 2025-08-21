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
./gradlew :demo:packLocalJsBundleDebug
// 或 Release 版产物(有混淆和压缩)
./gradlew :demo:packLocalJsBundleRelease
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
./gradlew :demo:packLocalJSBundleRelease
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

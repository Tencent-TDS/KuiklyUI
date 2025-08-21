# H5平台开发方式

``Kuikly``在H5平台上，可以编译成**js**产物运行在浏览器上。

## 快速开始

```shell
# 运行 shared 项目 dev server 服务器，没有安装 npm 包则先 npm install 安装一下依赖
npm run serve
#  构建 shared 项目 Debug 版
./gradlew :shared:packLocalJsBundleDebug
```

然后构建 h5App 项目
```shell
#  运行 h5App 服务器 Debug 版
./gradlew :h5App:jsBrowserRun -t
kotlin 2.0 以上运行: ./gradlew :h5App:jsBrowserDevelopmentRun -t
如果window平台因为编译iOS模块失败，可以参考"快速开始-环境搭建"指引配置
# 拷贝 assets 资源到 dev server
./gradlew :h5App:copyAssetsToWebpackDevServer
```

就可以在 http://localhost:8080/ 看到效果了，如果要访问不同的页面，可以通过 url 参数指定页面名称，如：http://localhost:8080/?page_name=router

如果只想构建得到 h5App 的构建产物，并不想运行开发服务器，可以执行

```shell
#  构建 h5App webpack 打包 Release 版
./gradlew :h5App:jsBrowserProductionWebpack
#  构建 h5App webpack 打包 Debug 版
./gradlew :h5App:jsBrowserDevelopmentWebpack
```
开发环境构建产物在 h5App/build/dist/js/developmentExecutable 中
生产环境构建产物在 h5App/build/dist/js/productionExecutable 中

>如果修改了 shared 项目的代码，需要重新执行 shared 项目的构建脚本
>首次 Sync 时如果不成功，可以 Build/Clean Project 试下

## 项目说明

项目入口在 ``Main.kt`` 的 main 方法中，其中 ``KuiklyRenderViewDelegator`` 用于注册外部自定义 ``View`` 和 ``Module`` 及 ``PropHandler``，
宿主侧可以在此实现自定义的``View``，``Module``并注册到``KuiklyRenderViewDelegator``中。

项目构建完成之后会生成 h5App.js，我们在 resources/index.html 中对其进行引入。并且在 h5App.js 之前进行 demo 项目 js 的引入。在 main 方法中处理 URL 参数、路由参数及宿主的相关参数。
然后通过 ``KuiklyRenderView.init`` 方法完成 ``KuiklyRenderView`` 的初始化，并在初始化完成后创建 kuikly view

## 项目发布

当我们在开发环境验证完成之后，需要构建生产环境的产物来进行项目发布

```shell
# 构建业务 h5App 和 JSBundle
# 首先构建业务 Bundle
./gradlew :shared:packLocalJSBundleRelease
# 然后构建宿主 APP
./gradlew :h5App:publishLocalJSBundle
```
>业务构建的产物为 nativevue2.js，在 @Page 注解中同一 Module 下的 Page 都会集成到一个 JS 文件<br>
>业务构建产物在 h5App/build/dist/js/productionExecutable/page 下<br>
>业务的 assets 资源在 h5App/build/dist/js/productionExecutable/assets 下<br>
>h5App 构建产物在 h5App/build/dist/js/productionExecutable 下

>如果业务规模较大导致构建失败，可能是构建内存不足，可以先执行 export NODE_OPTIONS=--max_old_space_size=16384 提升 nodejs 的运行内存
>更多关于构建相关的说明，请参考github中h5App示例项目的Readme文档

### 构建产物说明

h5App是项目的宿主APP，依赖 webRender，构建得到 h5App.js，shared 则是具体业务逻辑，构建得到统一的 nativevue2.js 文件。
生产环境部署时 index.html 中会引入业务逻辑的 nativevue2.js ，以及 h5App.js，部署生产环境的 html 中业务和 h5App.js 的引用需要根据业务实际情况调整。
```html
<!-- index.html -->

<!-- 引用业务逻辑 JS Bundle -->
<script type="text/javascript" src="nativevue2.js"></script>
<!-- 宿主 APP 和 webRender 的 JS文件 -->
<script type="text/javascript" src="h5App.js"></script>
```
另外因为 kuikly 支持 assets 方式引用项目 demo 目录下的 assets 中的图片，因此项目构建完成后，如果你有使用 assets 方式引用的图片，那么需要将 h5App/build/dist/js/productionExecutable/assets 目录整个拷贝
到你的服务器根目录，这样项目才可以通过相对路径访问到图片，例如你的网站部署在 https://kuikly.qq.com/, 那么你的 assets 图片就要通过 https://kuikly.qq.com/assets/xxx/xxx.png 来访问了****

> 我们模版的index.html中是有 pag 文件的 JS 库的引用的，在您实际部署时，如果没有 pag 文件的需求，则可以移除该 JS 引用

### 部署说明

最终我们可以将 h5App/build/dist/js/productionExecutable 中的全部内容作为 H5 的部署产物进行部署，这里实际的部署方式则依赖您的 H5 项目的实际部署方式了。您可以将产物内置到 APP 中进行加载运行，也可以作为通用的 H5 服务进行提供。
这取决于你们的业务需要

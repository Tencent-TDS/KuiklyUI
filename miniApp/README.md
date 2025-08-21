# Kuikly Web 宿主 App

## 项目介绍

本项目为 Kuikly MiniProgram 宿主 App 项目，用于运行 kuiklyCore 示例项目的小程序版本

## 快速开始

```shell
# 运行 shared 项目 dev server 服务器，没有安装 npm 包则先 npm install 安装一下依赖
npm run serve
#  构建 shared 项目 Debug 版
./gradlew :demo:packLocalJsBundleDebug
```

然后构建 miniApp 项目
```shell
#  运行 miniApp 服务器 Debug 版
./gradlew :miniApp:jsMiniAppDevelopmentWebpack
```

# 构建 release 版本
```shell
# 首先构建业务 Bundle
./gradlew :demo:packLocalJSBundleRelease

# 然后构建 miniApp
./gradlew :miniApp:jsMiniAppProductionWebpack
```


使用小程序开发者工具打开miniprogramApp下的dist目录，根据你的实际页面，修改app.json里面的pages数组和在pages里新建对应的页面
```javascript
// 例如demo里存在router的Page, 就需要在app.json的pages数组里添加 "pages/router/index", 同时在pages的目录里新建router目录补充和pages/index目录一样的内容

// pages/index/index.js内容
var render = require('../../lib/miniprogramApp.js')
render.renderView({
    // 这里的pageName是最高优先级，如果没配置，会去拿小程序启动参数里的page_name，如果都没有会报错
    // 建议小程序的第一个页面必须配置pageName
    // pageName: "router",
    statusBarHeight: 0 // 如果要全屏，需要把状态栏高度设置为0
})
```

## 本地静态资源

demo里面的src/commonMain/assets下的文件，需要复制到dist/assets目录
```shell
// 复制业务的assets文件到小程序目录
./gradlew :miniApp:copyAssets
```

## 页面配置

小程序的壳工程中，每个页面里都会调用render.renderView, 支持传递两个参数
1. pageName页面名称, 这里如果配置了，会忽略小程序启动的时候传递的page_name参数
2. statusBarHeight状态栏高度,默认会使用系统的状态了高度, 设置为0可以全屏

在小程序开发者工具，可以配置启动参数，指定启动的页面和其他配置

例如配置 page_name=SafeAreaExamplePage&testParam=123

## 项目说明

项目入口在 Main.kt 的 main 方法中，其中 KuiklyRenderViewDelegator 用于注册外部自定义 View 和 Module 及 PropHandler，
宿主侧可以在此实现自定义的View，Module并注册到KuiklyRenderViewDelegator中。


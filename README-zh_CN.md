<p align="center">
    <img alt="Kuikly Logo"  src="img/kuikly_logo.svg" />
</p>

[English](./README.md) | 简体中文 | [官网](https://framework.tds.qq.com/)

## 项目介绍

`Kuikly` 是基于Kotlin Multiplatform的UI与逻辑全面跨端综合解决方案，由腾讯大前端领域Oteam（公司级）推出，旨在提供一套`一码多端、极致易用、动态灵活的全平台高性能开发框架`。目前已支持平台：
- [X] Android
- [X] iOS
- [X] 鸿蒙
- [ ] Web（Q2开源）
- [ ] 小程序（Q2开源）

`Kuikly` 推出后受到业务广泛认可，已应用于 QQ、QQ 音乐、QQ 浏览器、腾讯新闻、搜狗输入法、应用宝、全民K歌、酷狗音乐、酷我音乐、自选股、ima.copilot、微视等多款产品。
## 特点

- 跨平台：基于 Kotlin 跨平台实现多平台一致运行，一码五端
- 原生性能：运行平台原生编译产物(.aar/.framework/.so)
- 原生开发体验：原生 UI 渲染、原生开发工具链、Kotlin 原生开发语言
- 轻量：SDK 增量小（AOT模式下，Android：约 300 KB，iOS：约 1.2 MB）
- 动态化：支持编译成动态化产物
- 多开发范式：声明式&响应式开发范式，支持自研 DSL 和 Compose DSL

## 项目结构

```shell
.
├── core                    # 跨平台模块，实现各个平台响应式 UI、布局算法、Bridge 通信等核心能力
  ├── src
    ├──	commonMain            #	跨平台共享代码、定义跨平台接口 
    ├── androidMain           # Android 平台实现代码 （aar）
    ├── jvmMain               # 泛 JVM 平台代码（不涉及 Android API）（jar）
    ├── iosMain               # iOS 平台实现代码（framework）
    ├── ohosArm64Main         # Ohos 平台实现代码（so）
├── core-render-android    # android 平台的渲染器模块
├── core-render-ios        # iOS 平台的渲染器模块
├── core-render-ohos       # Ohos 平台的渲染模块
├── core-annotations       # 注解模块，定义业务注解 @Page
├── core-ksp               # 注解处理模块，生成 Core 入口文件 
├── buildSrc               # 编译脚本，用于编译、打包、分包产物相关脚本
├── demo                   # DSL 示例代码 
├── androidApp             # Android 宿主壳工程
├── iosApp                 # iOS 宿主壳工程
├── ohosApp                # Ohos 宿主壳工程
├── compose                # 跨平台模块，实现Compose UI、布局，桥接Kuikly等核心能力
    ├── src
        ├── commonMain      # 跨平台共享代码，包含 Compose UI 组件、布局和事件处理
        ├── androidMain     # Android 平台特定实现
        └── nativeMain      # iOS 和鸿蒙平台特定实现
```
> 注: Compose 目录包含基于 Jetpack Compose 1.7.3 版本的跨平台源代码。我们对原始 Compose 代码进行了必要的修改和适配,以支持 Kuikly 框架的渲染需求。为了便于后续升级,我们注释掉了一些不必要的功能。为了确保功能稳定支持并避免与官方代码冲突,我们将包名从 `androidx.compose` 改为 `com.tencent.kuikly.compose`。原始 Compose 代码来自 [JetBrains Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform-core)。

## 系统要求
- iOS 12.0版本及以上
- Android 5.0版本及以上
- HarmonyOS Next 5.0.0(12) 版本及以上
- Kotlin版本 1.3.10 版本及以上

## 快速上手

- [快速体验](https://kuikly.tds.qq.com/QuickStart/hello-world.html)
- [接入指引](https://kuikly.tds.qq.com/QuickStart/overview.html)
- [组件特性](https://kuikly.tds.qq.com/API/components/override.html)

## 源码构建

### 编译环境
参照[环境搭建](https://kuikly.tds.qq.com/QuickStart/env-setup.html)进行配置
- [Android Studio](https://developer.android.com/studio)
  
  如果你的 Android Studio 版本大于等于 (2024.2.1)，请将 Gradle JDK 版本切换为 JDK17 
  (该版本默认 Gradle JDK 为 21，与项目使用的配置不兼容）

  切换方式: Android Studio -> Settings -> Build,Execution,Deployment -> Build Tools -> Gradle -> Gradle JDK
- [XCode](https://developer.apple.com/xcode/)和[cocoapods](https://cocoapods.org/)
- [DevEco Studio 5.1.0 或者更新版本](https://developer.huawei.com/consumer/cn/deveco-studio/)(API Version >= 18) (你可以通过【 DevECo Studio -> Help -> About HarmonyOS SDK 】查看API Version)
- JDK17

### 运行Android APP
在构建 Android App 之前，请确保完成了环境准备

1. 使用 `Android Studio` 打开 `KuiklyUI` 项目根目录，完成 sync
2. Configuration 选择 androidApp，Run 'androidApp'


### 运行iOS APP
在构建 iOS App 之前，请确保完成了环境准备

1. `cd` 到 `iosApp`
2. 执行 `pod install --repo-update`
3. 使用 `Android Studio` 打开 `KuiklyUI` 项目根目录，完成 `sync`
4. Configuration 选择 iOSApp，Run 'iOSApp'

或者使用 XCode 打开 KuiklyUI/iosApp 目录，`Run`

> 注意：源码iosApp工程在编译时会执行KMP脚本，如果遇到脚本读写文件权限报错，需要在`Xcode -> Build Setting`中将`User Script Sandboxing`设置为`No`

### 运行Ohos APP
在构建 Ohos App 之前，请确保完成了环境准备
1. 在`KuiklyUI`根目录执行鸿蒙跨端产物编译脚本 `./2.0_ohos_test_publish.sh`
2. 使用 DevEco Studio 打开 `KuiklyUI/ohosApp` 项目目录，完成 `sync`
3. 连接真机或启动鸿蒙模拟器，并执行签名操作 `File -> Project Structure -> Signing Configs`
4. 使用DevEco Studio, Run `entry`, 运行Ohos App

注: kuikly鸿蒙跨端产物仅支持Mac编译，Windows可以使用编译好的跨端产物运行Ohos APP

### Kotlin多版本支持

KuiklyUI目录下有各个`Kotlin`版本的gradle配置项

命名规则为 `x.x.xx.gradle.kts`，其中默认使用的是`Kotlin: 2.0.21`

同时，也提供各个版本的测试发布脚本，你可以`x.x.xx_test_publish.sh`构建`kuikly`的本地产物。

> `Kotlin: 1.3.10/1.4.20` 需要切换 `jdk11`

上述任一平台构建成功后，即可通过修改Core、Render、Demo，体验`Kuikly`开发。

### Demo快速体验
<div style="display: inline-block; text-align: left;">
  <img src="img/kuikly_demo_android_qr.png" width="200">
</div>

安卓手机扫码快速体验。iPhone和鸿蒙手机请按上述步骤源码编译Demo APP体验。

## Roadmap
[Roadmap(2025)](https://kuikly.tds.qq.com/Blog/roadmap2025.html)

## 贡献指南
欢迎各位开发者为 `Kuikly` 提出问题或发起 PR，建议你在为 `Kuikly` 贡献代码先阅读 [贡献指引](CONTRIBUTING.md)。

## 行为准则
请注意，本项目的所有参与者都应遵守我们的[行为准则](CODE_OF_CONDUCT.md)。参与即表示您同意遵守其条款。

## 常见问题
[Kuikly QA汇总](https://kuikly.tds.qq.com/QA/kuikly-qa.html)

## 贡献者
- 特别感谢首批贡献者tom（邱良雄）kam（林锦涛）watson（金盎），不仅在大前端领域主导 `Kuikly` 跨端方案孵化探索，而且率先在QQ业务落地。
- 感谢以下核心贡献者对Kuikly持续建设维护与发展优化：
  <br>tom kam watson rocky jonas ruifan pel layen bird zealot zhenhua vinney xuanxi arnon alexa allens eason

## 应用案例
### 接入应用
在腾讯，`Kuikly` 已有20+应用深度使用，页面数1000+，日活用户5亿+，满足了这些业务众多场景下的各类复杂需求。
<div style="display: inline-block; text-align: left;">
  <img src="img/applications.png" width="85%">
</div>
同时开源后，更多腾讯外部业务已在积极接入中，使用案例在征得业务同意后陆续补充

### 场景案例
业务典型应用场景，请参考：[应用场景案例](https://kuikly.tds.qq.com/Introduction/application_cases.html)

## 欢迎关注交流
欢迎扫码下方二维码关注最新动态或咨询交流。
<p align="left">
    <div style="display: inline-block; text-align: center; margin-right: 20px;">
        <div>腾讯端服务微信公众号</div>
        <img alt="TDS" src="img/tds_qrcode.jpg" width="200" />
    </div>
    <div style="display: inline-block; text-align: center; margin-right: 20px;">
        <div>TDS Framework 微信公众号</div>
        <img alt="TDS Framework" src="img/tds_framework_qrcode.jpg" width="200" />
    </div>
    <div style="display: inline-block; text-align: center;">
        <div>在线咨询</div>
        <img alt="在线咨询" src="img/consult_qrcode.png" width="200" />
    </div>
</p>

## 其他说明
The copyright notice pertaining to the Tencent code in this repo was previously in the name of “THL A29 Limited.”  That entity has now been de-registered.  You should treat all previously distributed copies of the code as if the copyright notice was in the name of “Tencent.”


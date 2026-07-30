# Kuikly跨平台框架

## 简介
Kuikly 是基于Kotlin Multiplatform的UI与逻辑全面跨端综合解决方案，由腾讯大前端领域Oteam（公司级）推出，旨在提供一套一码多端、极致易用、动态灵活的全平台高性能开发框架。

**[主页](https://framework.tds.qq.com/)**

**[github](https://github.com/Tencent-TDS/KuiklyUI)**

## 快速开始
### 环境准备
- [DevEco Studio 5.1.0 or newer](https://developer.huawei.com/consumer/cn/download/deveco-studio)
- Device or Emulator with HarmonyOS Next 5.0.0(12) or newer

### 集成Kuikly
1. 你可以通过ohpm将kuikly添加到你的项目中：

  `ohpm install @kuikly-open/render`

2. 或者你可以手动添加到你的项目中：

在应用程序模块的 `oh-package.json5` 中添加kuikly依赖
```json5
  "dependencies": {

    "@kuikly-open/render": 'latest'

  }
```
并执行
`ohpm install`

更多接入信息请参考: [Kuikly鸿蒙接入](https://t.kuikly.tds.qq.com/%E5%BF%AB%E9%80%9F%E5%BC%80%E5%A7%8B/harmony.html)
# 概览

## 适合谁阅读
- 已有 Jetpack Compose / Android 经验，想要一套 **跨 Android / iOS / Web / 鸿蒙 / 小程序** 的统一 UI 方案的同学
- iOS / Web / Harmony 等客户端或前端工程师，希望复用统一 DSL 与业务逻辑
- 正在使用 Kuikly 自研 DSL，希望评估/引入 Compose 方案的团队

## Kuikly Compose 是什么
一句话：**在 Kuikly Core 跨端引擎上，支持标准的 Jetpack Compose DSL**，与自研 DSL 并行，业务可按场景选择。

## Kuikly Compose的核心特点
1. **更多平台支持**：通过复用 Kuikly 的通用渲染层，Kuikly Compose 能够在支持标准 Compose DSL 语法的同时，无缝覆盖主流平台，包括 Android、iOS、**鸿蒙**、H5，以及国内常见的**微信小程序**平台，极大提升了应用的可达性。

2. **动态化能力（beta）**：在 Kuikly 跨端框架层基础上扩展对 Compose DSL 的支持，使 Kuikly Compose 天然具备了 Kuikly 现有的动态化能力，包括热更新、动态下发等特性。

3. **原生体验**：不同于官方 Compose 的自渲染方式，Kuikly Compose 保留了 Kuikly 的原生渲染优势，确保在各个平台上实现高性能、原生级的 UI 体验。

4. **AI友好**：与官方 Compose API 基本一致，可直接使用 Cursor、GitHub Copilot 等 AI 工具生成代码，可靠性高，无需额外适配。

## 与官方 Compose 的区别

| 特性 | Kuikly Compose                            | Compose Multiplatform |
|------|-------------------------------------------|-------------|
| 跨平台支持 | Android/iOS/鸿蒙/H5/微信<br/>小程序/Desktop（支持中） | Android/iOS/Desktop/H5 |
| 动态化 | 支持                                        | 不支持 |
| 渲染方式 | 纯原生渲染                                     | Skia 渲染 |
| 性能 | 原生性能                                      | 依赖 Skia 性能 |
| 包体积 | 较小                                        | 较大 |

## 快速开始
要开始使用 Kuikly Compose DSL，请参考[快速开始](./getting-started.md)文档。

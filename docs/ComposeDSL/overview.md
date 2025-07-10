# Kuikly Compose (Beta版) 概述

## 背景
基于 Kuikly 的核心架构与通用渲染层，我们进一步拓展了对标准 Compose DSL 的支持，目前 Beta 版本已发布。Kuikly对Compose DSL的支持，可以进一步降低客户端开发上手成本，其核心特点是：

1. **更多平台支持**：通过复用 Kuikly 的通用渲染层，Kuikly Compose 能够在支持标准 Compose DSL 语法的同时，无缝覆盖主流平台，包括 Android、iOS、**鸿蒙**、Web，以及国内常见的**小程序**平台，极大提升了应用的可达性。

2. **动态化能力**：在 Kuikly 跨端框架层基础上扩展对 Compose DSL 的支持，使 Kuikly Compose 天然具备了 Kuikly 现有的动态化能力，包括热更新、动态下发等特性。

3. **原生体验**：不同于官方 Compose 的自渲染方式，Kuikly Compose 保留了 Kuikly 的原生渲染优势，确保在各个平台上实现高性能、原生级的 UI 体验。

## 与官方 Compose 的区别

| 特性 | Kuikly Compose          | Compose Multiplatform |
|------|-------------------------|-------------|
| 跨平台支持 | Android/iOS/鸿蒙/Web<br/>/小程序/Desktop（规划中） | Android/iOS/Desktop/Web |
| 动态化 | 支持                      | 不支持 |
| 渲染方式 | 纯原生渲染                   | Skia 渲染 |
| 性能 | 原生性能                    | 依赖 Skia 性能 |
| 包体积 | 较小                      | 较大 |


## 快速开始

要开始使用 Kuikly Compose DSL，请参考[快速开始](./quickStart.md)文档。
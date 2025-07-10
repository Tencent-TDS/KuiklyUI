# Kuikly Compose 概述

## 背景

Kuikly 作为一套成熟的跨平台 UI 框架，在提供原生渲染体验和动态化能力的同时，我们选择扩展支持 Compose DSL，为开发者提供更灵活的 UI 开发方案。这一决策主要基于以下考虑：

1. **降低开发成本**：Compose 作为 Android 官方推荐的 UI 开发方案，已经积累了大量的开发者。通过支持标准的 Compose DSL，开发者可以复用现有的 Compose 开发经验，同时获得 Kuikly 提供的跨平台和动态化能力。

2. **扩展平台支持**：在保持 Compose DSL 语法一致性的同时，Kuikly 的渲染层为开发者提供了更广泛的平台支持：
   - Android/iOS 原生平台
   - 鸿蒙系统
   - Web 平台
   - 小程序平台

3. **原生渲染体验**：与 Compose Multiplatform 不同，Kuikly 采用纯原生渲染方案，确保在各个平台上都能提供最佳的原生 UI 体验，包括流畅的动画效果和精确的触摸响应。

4. **动态化能力**：Kuikly 强大的动态化能力为 Compose DSL 带来了更多可能性：
   - 支持热更新
   - 支持动态下发
   - 支持运行时修改 
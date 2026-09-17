# 2.26.0 版本说明

> 本版导航：**版本说明** · [变更汇总](./changelog.md) · [破坏性与迁移](./breaking.md)

2.26.0 带来 Compose 原生属性动画与 listView 初始滚动位置 API，并修复了 H5 与多端滚动的一批问题。

## Kuikly DSL

### 能力增加

- **listView 初始滚动位置（`initContentOffset`）**：ListView / WaterfallList 支持设置初始内容偏移（#1616）。
- **鸿蒙无障碍支持（鸿蒙）**：新增无障碍能力，`accessibilityRole` 兼容 API 18（#1626）。

### 性能优化

- **Android TurboDisplay 预准备**：Android 端增加 TurboDisplay 预准备（#1679）。

### Bug 修复

- 修复鸿蒙端 `NetworkResponse.statusCode` 一直为 null 的问题（#1623）。
- 修复 sliderpager 滚到最后一项的偏移溢出（#1681）与 `defaultPage` 不生效。
- H5：修复 toImage 结果不清晰、ImageSpan 封面图位置、measure 缓存命中后的多行样式、序列化空值保护等问题。
- Android：修复 PAG ReplaceImageLayer 图片源问题（#1688）。
- iOS：修复缓存树 tag 链断裂导致的崩溃（#1685）、SPM 集成 release 构建的编译错误（#1622）。

## Compose DSL

### 能力增加

- **原生属性动画（opt-in）**：新增 Compose 原生属性动画，需显式开启；包含动画中断与鸿蒙回退的稳定性处理（#1633）。

### Bug 修复

- 修复 Android 后台展开后 lazy 内容未重新对齐（#1642）、pager 滚动会话状态不稳定（#1629）。

# 2.24.0 版本说明

> 本版导航：**版本说明** · [变更汇总](./changelog.md) · [破坏性与迁移](./breaking.md)

本版以稳定性修复为主。

## Kuikly DSL

### Bug 修复

- 修复布局尺寸上报精度问题：尺寸上报前四舍五入到两位小数，避免浮点误差累积（#1588）。
- 修复 iOS TurboDisplay 序列化错误（#1581），以及下拉刷新因重复设置 contentInset 导致的抖动（#1582）。

## Compose DSL

### Bug 修复

- 修复 Compose 1.9 的编译错误（#1580）。

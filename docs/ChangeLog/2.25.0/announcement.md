# 2.25.0 版本说明

> 本版导航：**版本说明** · [变更汇总](./changelog.md) · [破坏性与迁移](./breaking.md)

2.25.0 以鸿蒙与 H5 端修复为主，并同步了 Compose 输入体验的改进。

## Kuikly DSL

### 能力增加

- **H5 输入事件（H5）**：新增 `textInputStateChange` 与 `selectionChange` 事件，业务可以感知输入框状态与选区变化。

### 性能优化

- **鸿蒙包体积**：对 KN 与 libkuikly 应用无副作用的体积优化（#1600）。

### Bug 修复

- 鸿蒙：KN bridge 导出在 `KRRenderCValue.h` 中自包含（#1607）、interop 头文件统一为单一来源（#1596）、同步主任务等待超时从 5s 调整为 10s（#1594）。
- H5：修复 Hover 组件、监听移除、自定义 view 公共属性与 `KuiklyProcessor` 接口支持等问题。

## Compose DSL

### 能力增加

- **TextField emoji 输入焦点处理**：从 ComposeOnKuikly upstream 同步 emoji 输入时的键盘焦点处理（#1593）。

### Bug 修复

- 修复官方 Transition 共享 Snapshot 下的越界问题（#1590）、即时滚动时组合中间页面的问题（#1589）。

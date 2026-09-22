# 2.15.1 变更说明

## Part 1 总体说明

2.15.1 是一个面向稳定性的补丁版本：鸿蒙端页面实例销毁后的调用不再引发崩溃，同时工程的构建依赖与接入说明也做了兼容性调整。具体如下：

- **鸿蒙实例生命周期**：修复调度器悬垂指针与实例销毁后继续调用导致的异常，页面退出与反复进出场景下的崩溃明显减少。
- **工程依赖与接入**：恢复对 Kotlin 1.3 工程的兼容，调整腾讯 Maven 源地址，并补充 AndroidManifest 的配置说明。

历史提交可见 [2.15.0...2.15.1](https://github.com/Tencent-TDS/KuiklyUI/compare/2.15.0...2.15.1)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### 鸿蒙

- fix: KTUIScheduler dangling pointer casting issue · [#997](https://github.com/Tencent-TDS/KuiklyUI/pull/997)
- fix: exception issue when called after the instance being destroyed · [#524](https://github.com/Tencent-TDS/KuiklyUI/pull/524)

### 跨端

- chore: kotlin 1.3 compatibility · [#1004](https://github.com/Tencent-TDS/KuiklyUI/pull/1004)

### 其他

- chore: modify tencent maven source · [#996](https://github.com/Tencent-TDS/KuiklyUI/pull/996)
- docs: android guide add AndroidManifest.xml config intro · [#981](https://github.com/Tencent-TDS/KuiklyUI/pull/981)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

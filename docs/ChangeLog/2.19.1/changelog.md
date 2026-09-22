# 2.19.1 变更说明

## Part 1 总体说明

2.19.1 针对绘制与组合两处性能瓶颈做优化：Canvas 的绘制命令不再逐条跨端下发，Compose DSL 也能把一段已组合的内容整体搬运而不重建。具体如下：

- **Canvas 绘制吞吐**（Android、iOS、鸿蒙、H5）：一帧内的多条绘制命令可以合并成一次下发，图表、笔迹这类绘制密集的场景明显更流畅；Web 端 Canvas 的绘制缺陷也一并修复。
- **组合内容的移动与复用**（Android、iOS、鸿蒙）：Compose DSL 支持 `movableContentOf`，条件分支切换与列表项重排时可以带着状态搬运内容，避免重建。

历史提交可见 [2.19.0...2.19.1](https://github.com/Tencent-TDS/KuiklyUI/compare/2.19.0...2.19.1)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Compose

- feat(compose): support movableContentOf for Compose DSL · [#1368](https://github.com/Tencent-TDS/KuiklyUI/pull/1368)

### 多端

- feat(perf): improve canvas performanance by batching commands · [#1268](https://github.com/Tencent-TDS/KuiklyUI/pull/1268)
- Bugfix/web canvas · [#1383](https://github.com/Tencent-TDS/KuiklyUI/pull/1383)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

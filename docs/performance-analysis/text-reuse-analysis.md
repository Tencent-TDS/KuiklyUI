# Text 组件复用性能分析

> 分析日期：2026-01-23  
> 分支：feature/test-lazy-reuse-issue  
> 测试 Demo：[JankPageDemo.kt](../demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/jank/JankPageDemo.kt)

---

## 背景

滑动列表存在卡顿问题，怀疑 **Text 组件存在性能瓶颈**。本文档分析 Text 在 LazyColumn/LazyRow 中的复用情况。

## TL;DR

| 结论 | 说明 |
|------|------|
| ✅ 首屏无问题 | 每个 Text 生命周期只执行 1 次，无重复重组 |
| ❌ 嵌套 Lazy 无法复用 | LazyColumn + LazyRow 嵌套时，内层复用池随外层一起销毁 |
| ❌ 快滑动复用率仅 8.7% | 时序问题：measure 需要 slot 时，旧 slot 还没释放 |
| ⚠️ contentType 易误用 | 动态值（如 `"${item.type}"`）导致复用完全失效 |

**核心优化方向**：让 LazyRow 支持复用（P0）、实现预加载 Prefetch（P0）

---

## 核心结论

### ✅ 结论 1：首屏无重复重组

每个 Text 组件的完整生命周期只执行 1 次，无重复重组、测量、布局。

### ❌ 结论 2：嵌套 LazyRow 导致无法复用

- **问题**：`LazyColumn` + `LazyRow` 嵌套时，Text 的 LayoutNode 无法复用
- **原因**：每个 LazyRow 有独立的复用池，外层行滚出时整个复用池被丢弃
- **方案**：让 LazyRow 支持复用（推荐）/ 改为普通 Row

### ❌ 结论 3：动态 contentType 导致无法复用

- **问题**：使用动态值作为 `contentType`（如 `"${item.type}"`）会导致复用失效
- **原因**：复用池按 `contentType` 分组，动态值导致每个 item 都不匹配
- **方案**：使用固定的 `contentType` 值（如 `"RankingItem"`）

### ⚠️ 结论 4：快滑动复用率低

| 场景 | 复用率 |
|------|--------|
| 慢滑动 | **100%** |
| 快滑动 | **8.7%** |

- **根本原因**：时序问题 - measure 阶段需要新 slot 时，旧 slot 还没释放
- **方案**：实现预加载（Prefetch）

---

## 推荐优化方案

| 优化方向 | 可行性 | 优先级 | 详情 |
|---------|--------|--------|------|
| 支持 Lazy 组件复用 | ✅ 高 | P0 | [查看详情](./optimization/lazy-reuse.md) |
| 支持预加载（Prefetch） | ✅ 高 | P0 | [查看详情](./optimization/prefetch.md) |
| 预释放 slot | ⚠️ 中 | P2 | [查看详情](./optimization/pre-release.md) |
| 预热复用池 | ⚠️ 低 | P3 | [查看详情](./optimization/pool-warmup.md) |

---

## 相关文档

- [快滑动复用率低的详细代码分析](./text-reuse-detail.md)
- [嵌套 Lazy 复用机制技术分析](./nested-lazy-reuse.md)
- 测试日志：`demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/jank/log1.txt`

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
| ✅ 嵌套 Lazy 复用已修复 | SubcomposeLayout 改用 ReusableComposeNode 后，LazyRow 支持复用 |
| ✅ Node 和 Native View 均被复用 | 复用时 viewRef 值不变，证明 KNode 和 Text Native View 是同一实例 |
| ✅ 滑动复用率 76.2% | 滑动初期复用池为空（24 个 NewSlot），之后 100% 复用（96 个 Reuse） |
| ⚠️ contentType 易误用 | 动态值（如 `"${item.type}"`）导致复用完全失效 |

**当前状态**：LazyRow 复用优化已生效，Node 和 Text Native View 均被完整复用

---

## 核心结论

### ✅ 结论 1：首屏无重复重组

每个 Text 组件的完整生命周期只执行 1 次，无重复重组、测量、布局。

### ✅ 结论 2：嵌套 LazyRow 复用已修复，Node 和 Native View 均被复用

- **原问题**：`LazyColumn` + `LazyRow` 嵌套时，Text 的 LayoutNode 无法复用
- **原因**：带滚动的 SubcomposeLayout 使用 `ComposeNode`（非复用节点）
- **修复**：`SubcomposeLayout.kt` 第 337 行 `ComposeNode` → `ReusableComposeNode`
- **验证**：滑动时 Slot 复用率 **76.2%**，LazyRow 整体被复用
- **关键结论**：复用时 `viewRef` 值不变，证明 **KNode 和 Text Native View 是同一实例，均被完整复用**

### ❌ 结论 3：动态 contentType 导致无法复用

- **问题**：使用动态值作为 `contentType`（如 `"${item.type}"`）会导致复用失效
- **原因**：复用池按 `contentType` 分组，动态值导致每个 item 都不匹配
- **方案**：使用固定的 `contentType` 值（如 `"RankingItem"`）

### ✅ 结论 4：滑动复用率符合预期

| 场景 | 复用率 | 说明 |
|------|--------|------|
| 首屏 | **0%** | 复用池为空，全部新建（符合预期） |
| 滑动初期 | **0%** | 首屏节点未滚出，复用池仍为空 |
| 稳定滑动 | **100%** | 首屏节点入池后，后续节点全部复用 |
| **整体** | **76.2%** | 96/(96+30) |

- **原因**：滑动初期（slotId=12~15）时，首屏节点还在屏幕上，复用池为空；第一次 ToPool 后（slotId=16 开始）全部复用
- **结论**：LazyRow 复用优化生效，复用机制正常

---

## 推荐优化方案

| 优化方向 | 可行性 | 优先级 | 状态 | 详情 |
|---------|--------|--------|------|------|
| 支持 Lazy 组件复用 | ✅ 高 | P0 | ✅ 已完成 | [查看详情](./optimization/lazy-reuse.md) |
| 支持预加载（Prefetch） | ✅ 高 | P0 | 待验证 | [查看详情](./optimization/prefetch.md) |
| 预释放 slot | ⚠️ 中 | P2 | - | [查看详情](./optimization/pre-release.md) |
| 预热复用池 | ⚠️ 低 | P3 | - | [查看详情](./optimization/pool-warmup.md) |

---

## 相关文档

- [快滑动复用率低的详细代码分析](./text-reuse-detail.md)
- [嵌套 Lazy 复用机制技术分析](./nested-lazy-reuse.md)

---

## 测试日志

| 日志文件 | 说明 |
|---------|------|
| [log1.txt](../demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/jank/log1.txt) | 首屏日志 |
| [log2.txt](../demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/jank/log2.txt) | 滑动日志（LazyRow 复用优化后） |

**过滤关键词**：`TextPerfDebug`

```bash
# 过滤查看日志
cat log1.txt | grep "TextPerfDebug"
```

---

## 测试数据

### Slot 复用统计

| 指标 | log1.txt (首屏) | log2.txt (滑动) |
|------|-----------------|-----------------|
| NewSlot (新建) | 72 | 30 |
| Reuse (复用) | 0 | **96** |
| ToPool (回收入池) | 0 | 125 |
| **Slot 复用率** | 0% | **76.2%** |

### Text 组件统计

| 指标 | log1.txt (首屏) | log2.txt (滑动) |
|------|-----------------|-----------------|
| Recompose 次数 | 300 | 525 |
| bindViewRef 次数 | 300 | 525 |
| Layout 次数 | 300 | 599 |

**分析**：
- 首屏 300 个 Text 全部新建（符合预期）
- 滑动时 Slot 复用率 **76.2%**，LazyRow 嵌套复用优化生效
- 每个 Slot 约包含 4-5 个 Text 组件（525/126≈4.2）

### ✅ Text Node 复用验证

**验证方法**：对比复用前后 Text 的 `viewRef`（Native 节点引用）

| 字段 | 首屏 `slotId=0_1004` | 滑动复用 `slotId=16_1000` | 复用 |
|------|----------------------|--------------------------|------|
| rank | viewRef=93 | viewRef=93 | ✅ |
| title | viewRef=95 | viewRef=95 | ✅ |
| tag | viewRef=97 | viewRef=97 | ✅ |
| views | viewRef=99 | viewRef=99 | ✅ |
| rule | viewRef=100 | viewRef=100 | ✅ |

**结论**：
- ✅ **Slot 复用时，内部 Text 的 Native Node 也被完整复用，没有重新创建**
- ⚠️ 复用时会触发 `bindViewRef` 回调，因为 Compose 的 `remember` 状态需要重新关联
- ✅ viewRef 值不变，证明底层 KNode 是同一个实例

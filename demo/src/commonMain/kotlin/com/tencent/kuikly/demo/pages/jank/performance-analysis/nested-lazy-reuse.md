# 嵌套 Lazy 复用机制技术分析

> 相关主文档：[Text 组件复用性能分析](./text-reuse-analysis.md)

---

## 嵌套问题的结构

```
LazyColumn (外层)
    └── slot[row-0] → LazyRow (内层) ← 有自己独立的 SubcomposeLayoutState
                         └── slot[item-0] 
                         └── slot[item-1]
                         └── reusablePool (内层复用池)
    └── slot[row-1] → LazyRow (内层) ← 另一个独立的 SubcomposeLayoutState
                         └── slot[item-0]
                         └── reusablePool (内层复用池) ← 不同 LazyRow 实例的池子不共享！
```

**问题**：每个 LazyRow 有独立的复用池，外层行滚出时整个复用池被丢弃。

---

## 快滑动复用率低的原因

### 时序问题

```
measure 阶段:  subcompose(新 item) → 需要 slot → 旧 slot 还没释放 → 无法复用
placeChildren: disposeOrReuseStartingFromIndex() → 释放旧 slot → 太晚了！
```

### 大块滑动的瞬时需求爆发

复用池是"跨帧平衡"的系统，本帧释放的 slot 只能被下一帧复用。

**动态平衡模型**：

```
正常滑动（可维持平衡）:
  第 N 帧: measure 取 1 个 → placeChildren 放 1 个 → 池维持平衡
  第 N+1 帧: 本帧释放的 slot → 被下一帧复用 → 循环平衡 ✓

大块滑动（问题帧）:
  - measure 新 row × 5 → 需要 5 个 slot → 池只有 2 个
    → 复用 2 个 ✓
    → 新建 3 个 ✗ （导致白屏/卡顿）
  - placeChildren → 一次性释放 5 个旧 slot 入池

大块滑动后（恢复正常）:
  - 池里已有 5 个 slot → 后续滑动可正常复用 ✓
```

### 场景对比

| 场景 | 瞬时需求 | 池容量 | 结果 |
|------|----------|--------|------|
| 正常滑动 | 1~2 个 | 1~2 个 | ✅ 平衡复用 |
| **大块滑动** | 5+ 个 | 1~2 个 | ❌ **新建导致卡顿/白屏** |
| 大块滑动后 | 1~2 个 | 5+ 个 | ✅ 池充足，正常复用 |

---

## 详细代码分析

请参考：[快滑动复用率低的详细代码分析](./text-reuse-detail.md)

# 优化方案：预释放 slot

> 优先级：P2  
> 状态：理论可行，复杂度高

---

## 问题背景

快滑动时，measure 阶段需要新 slot，但旧 slot 要等到 placeChildren 阶段才释放，导致无法复用。

---

## 核心想法

在 measure 阶段开始时，先把确定要滑出的旧 slot 释放到复用池。

---

## 当前流程

```
measure 开始 → subcompose(新 item) → 需要 slot → 池为空 → 新建
                                                  ↑
                                                  旧 slot 还没释放！
placeChildren → disposeOrReuseStartingFromIndex() → 释放旧 slot → 太晚了
```

---

## 困难点

1. 在 measure 开始时，**还不知道哪些 slot 会被需要**
2. 必须等 measure 计算完可见区域后，才能确定新 slot 列表
3. 这是个「鸡生蛋」问题

---

## 可能的改进

- 基于**滚动方向 + 滚动距离**，**预测**哪些 slot 会滑出
- 在 measure 开始前，先释放预测要滑出的 slot
- **风险**：预测错误会导致额外的创建/销毁开销

---

## 结论

实现复杂度高，建议先验证「支持 Lazy 复用」和「预加载」这两个更靠谱的方案。

# Compose DSL vs Kuikly DSL 纯文本组件性能对比分析

## 测试场景

- **测试组件**: 900 个 Text 组件
- **测试页面**: `TextPerfComposeDemo` vs `TextPerfKuiklyDemo`
- **测试环境**: iOS Release 版本

## 性能数据对比

### Release 版本数据

| 指标 | Compose DSL | Kuikly DSL | 差异 |
|------|-------------|------------|------|
| **总耗时** | 185 ms | 107 ms | **+78 ms (+73%)** |
| **布局测量** | 98 ms | 95 ms | +3 ms (基本一致) |
| **框架开销** | 87 ms | 12 ms | **+75 ms** |

### Compose DSL 耗时分解

```
CompositionImpl#setContent: 74 ms
├── applyChanges: 60 ms
│   ├── insertTopDown: 20 ms          // 节点插入（类似 Kuikly 的 addChild）
│   ├── applyTextStyle: 21 ms         // 文本样式应用
│   └── ReusableComposeNode: 7 ms     // 节点更新
└── composeContent: 14 ms             // 组合内容
    └── _BasicText: 10 ms

KRRichTextShadow#hrv_calculateRenderViewSizeWithConstraintSize: 98 ms  // 原生布局测量
```

### Kuikly DSL 耗时分解

```
ViewContainer#addChild: 12 ms          // 节点插入

KRRichTextShadow#hrv_calculateRenderViewSizeWithConstraintSize: 95 ms  // 原生布局测量
```

## 核心差异分析

### 1. 布局测量耗时一致

两种 DSL 最终都调用相同的 OC 原生方法 `-[KRRichTextShadow hrv_calculateRenderViewSizeWithConstraintSize:]` 进行文本尺寸计算，这部分耗时基本一致（98ms vs 95ms）。

### 2. Compose DSL 额外开销来源

| 开销项 | 耗时 | 说明 |
|--------|------|------|
| **Compose Runtime 重组** | ~14 ms | `composeContent` 组合阶段 |
| **applyChanges** | ~60 ms | 变更应用阶段 |
| ├─ `insertTopDown` | 20 ms | 节点插入到视图树 |
| ├─ `applyTextStyle` | 21 ms | 文本样式设置 |
| └─ `BasicTextWithNoInlinContent` | 7~10 ms | 文本节点更新回调 |

### 3. 等价操作对比

| Compose DSL | Kuikly DSL | 说明 |
|-------------|------------|------|
| `Applier#insertTopDown` (20ms) | `ViewContainer#addChild` (12ms) | 节点插入，Compose 多 8ms |

## 结论

Compose DSL 相比 Kuikly DSL 在纯文本场景下多出约 **73% 的耗时开销**（78ms），主要来自：

1. **Compose Runtime 的组合机制** (~14ms)
2. **applyChanges 变更应用** (~60ms)
   - 节点插入抽象层开销
   - 文本样式应用逻辑 (`applyTextStyle`)
   - `BasicTextWithNoInlinContent` 节点更新

## 优化方向验证结果

### 1. 重组优化 ✅ 已验证 - 无需优化
- [x] 确认是否有多余的重组发生 → **无多余重组**
- [x] 检查 `composeContent` 阶段是否有冗余计算 → **重组次数=900，与组件数量一致**

**验证结论**: Compose DSL 的重组次数与 Text 组件数量一致，每个 BasicText 仅重组一次，无冗余重组。

### 2. BasicTextWithNoInlinContent 优化
- [ ] 分析 `ReusableComposeNode` 回调是否有优化空间
- [ ] 检查 `applyTextStyle` 是否有重复设置
- [ ] 考虑是否可以减少桥接开销

### 3. 节点插入优化 ✅ 已验证 - 无需优化
- [x] `insertTopDown` 与 `addChild` 的差距原因分析 → **两者机制对齐**
- [x] 是否可以批量插入优化 → **无需优化**

**验证结论**: 
| 指标 | Compose insertTopDown | Kuikly createFlexNode |
|------|----------------------|----------------------|
| 插入次数 | 900 | 900 |
| 单次耗时 | ~282us | ~101us |
| DOM 插入 | insertDomSubView | addChildAt |

两种 DSL 的节点插入机制基本对齐，差异在于 Compose 使用 `insertDomSubView`，Kuikly 使用 `addChildAt`，均为逐个插入，无批量优化空间。

## 相关文件

- `TextPerfComposeDemo.kt` - Compose DSL 测试页面
- `TextPerfKuiklyDemo.kt` - Kuikly DSL 测试页面
- `BasicText.kt` - Compose Text 组件实现

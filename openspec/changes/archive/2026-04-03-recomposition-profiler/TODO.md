# TODO — 已完成 / 延后事项

## 研究文档归档 ✅ 已完成

已归档到 `kuikly-harness` 的 `.ai/references/` 下：
- `research/window-layering-research.md` + `architecture-visualization.md` + `quick-reference.md` → `.ai/references/window-layering.md`（三文件合并精简）
- `research-recomposition-tracking.md` + `specs/overlay-highlight/spec.md` → `.ai/references/recomposition-profiler.md`

## 延后事项

以下功能依赖 `recomposition-analyzer` skill 完善后才有意义，移到独立 change 实现：

- **滚动上下文事件**（`ScrollContextEvent`）：LazyColumn 每次滑动 N 格 → 记录 firstVisibleItemIndex 变化，AI 可判断"每格滑动触发重组是否合理"
- **动画上下文事件**（`AnimationContextEvent`）：animate*AsState 启动/结束时间，辅助判断动画驱动的高频重组是否正常

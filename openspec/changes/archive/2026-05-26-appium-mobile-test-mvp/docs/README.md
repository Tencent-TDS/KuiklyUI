# Appium Mobile Test — 参考文档

本目录收纳 mobile-test MVP 全部设计与实施文档（原 `docs/DevGuide/ai-mobile-test-*` 与 `docs/plans/`）。

| 文档 | 用途 |
|------|------|
| [loop.md](loop.md) | AI 测试验证闭环总体方案 |
| [framework-decision.md](framework-decision.md) | Appium vs 原生框架选型 |
| [implementation-plan.md](implementation-plan.md) | MVP 分任务实施计划 |
| [view-tree-and-coordinates.md](view-tree-and-coordinates.md) | view-tree 格式与坐标语义 |
| [mvp-test-cases.md](mvp-test-cases.md) | MVP 测试用例清单 |
| [ios-e2e-pitfalls.md](ios-e2e-pitfalls.md) | iOS E2E 踩坑记录 |
| [bug-classification.md](bug-classification.md) | 失败分类与归因 |
| [progress.md](progress.md) | SDK 侧改动与验证进展 |

**运行时工具**已迁至 **kuikly-harness**（`.agents/skills/kuikly-mobile-test/`），不随 KuiklyUI SDK 发布。日常用法见 harness 内 `SKILL.md`。

# 2.12.1 变更说明

## Part 1 总体说明

2.12.1 修正了可见性回调与懒加载列表更新在部分场景下的漏报与偏移，并让 Native 与 Compose 混编时的按压状态保持一致，具体如下：

- **可见性与曝光回调的完整性**（跨端）：调整列表可见区域的忽略边距后，子组件的露出与消失回调会按新区域重新计算并通知，依赖可见性回调做曝光统计的页面不再漏报。
- **列表更新与按压状态的一致性**（跨端、Android、Compose）：懒加载列表更新不再引起内容偏移跳动；点击原生按钮时 Compose 侧的按压态被正确取消，混编页面不再残留按压高亮。

历史提交可见 [2.12.0...2.12.1](https://github.com/Tencent-TDS/KuiklyUI/compare/2.12.0...2.12.1)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix: cancel compose press state when click native btn · [#837](https://github.com/Tencent-TDS/KuiklyUI/pull/837)

### 跨端

- fix: visibleAreaIgnore change should notify appear events · [#839](https://github.com/Tencent-TDS/KuiklyUI/pull/839)
- fix: vforlazy update caused a change in the offset · [#845](https://github.com/Tencent-TDS/KuiklyUI/pull/845)

### 其他

- Time profiling doc · [#842](https://github.com/Tencent-TDS/KuiklyUI/pull/842)
- chore: restore ai chat demo code and add mac support · [#841](https://github.com/Tencent-TDS/KuiklyUI/pull/841)

---

## Part 3 破坏性变更

## 条件性行为变化

### 可见区域忽略边距变化后触发可见性事件

- 结果：修改列表的可见区域忽略边距后，子组件的 `willAppear`、`didAppear`、`willDisappear`、`didDisappear` 与 `appearPercentage` 会按新的可见区域重新计算并回调（此前不会触发）。
- 影响：曝光回调不再漏报，但运行时动态调整过该边距的页面会多收到一次可见性回调，按「每次回调即上报」统计的曝光数据可能重复。
- 迁移：无需改动业务代码；若曝光统计在每次回调时直接上报，需按组件维度补充去重。
- 说明文档：[Scroller 可见性属性](../../API/components/scroller.md)

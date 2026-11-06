# 2.20.0 变更说明

## Part 1 总体说明

2.20.0 修正了文本排版与输入长度限制的口径，让 Pager 与 vforLazy 在滚动边界上更跟手，同时把语义标识同步到原生无障碍树，并补上了鸿蒙 PAG 动画的点击能力。具体如下：

- **文本排版与输入长度口径**（鸿蒙、Android、iOS）：单行文本在设置行高后仍能垂直居中；输入长度限制可按字符、字节、视觉宽度等不同策略计算并给出一致的超限回调。
- **滚动吸附与顶部前插的稳定性**（Android、iOS、鸿蒙）：Pager 的吸附对齐与内容尺寸稳定下来；vforLazy 主动拖拽期间不再被校正动画接管，校正延后到滚动结束后再做。
- **自动化与无障碍标识**（Android、iOS）：`testTag` 与无障碍信息会同步到原生无障碍树，自动化可以用语义 selector 定位元素并断言。
- **PAG 动画的点击交互**（鸿蒙）：ArkTS 版 PAG 视图可以直接监听点击事件，动画区域也能参与交互。

历史提交可见 [2.19.1...2.20.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.19.1...2.20.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### 鸿蒙

- fix(ohos): center single-line text with lineHeight via SetTypographyVerticalAlignment · [#1384](https://github.com/Tencent-TDS/KuiklyUI/pull/1384)
- feat(ohos): arkts PagView support click event · [#1376](https://github.com/Tencent-TDS/KuiklyUI/pull/1376)

### Compose

- fix(pager): stabilize native snap alignment and pager content size · [#1387](https://github.com/Tencent-TDS/KuiklyUI/pull/1387)
- feat(a11y): testTag and accessibility improvements for mobile automation · [#1391](https://github.com/Tencent-TDS/KuiklyUI/pull/1391)
- feat(textfield): support length limit strategy SPI · [#1385](https://github.com/Tencent-TDS/KuiklyUI/pull/1385)

### 跨端

- fix(vforlazy): stabilize top front-insert dragging · [#1388](https://github.com/Tencent-TDS/KuiklyUI/pull/1388)

### 其他

- fix(demo): add code owner · [#1386](https://github.com/Tencent-TDS/KuiklyUI/pull/1386)

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

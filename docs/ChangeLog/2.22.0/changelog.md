# 2.22.0 变更说明

## Part 1 总体说明

2.22.0 集中修正了翻页与列表的滑动落位、软键盘与安全区的尺寸计算，并补齐鸿蒙端输入、截图与滚动能力，页面在复杂布局与多端差异下的表现更稳定。具体如下：

- **翻页与列表的落位精度**（Compose）：翻页惯性滑动的吸附基准、边界对齐与目标页保持得到统一修正，惯性滑动不再跳页或停在错页；懒加载网格的内容尺寸收缩、下拉刷新在悬浮头部场景下的顶部内边距缺失也一并修复。
- **软键盘与安全区计算**（Android、iOS、H5）：安全区底部内边距不再被软键盘高度虚高，输入框与多行输入的焦点跳动问题得到修复，H5 侧也关闭了 iOS 文本自动放大导致的字号异常。
- **鸿蒙输入与滚动能力**（鸿蒙）：文本编辑器支持选区颜色，截图的文件模式、负向内容偏移、下载失败后的错误处理、滚动惯性参数与嵌套触摸消费判定一并修正，并补齐新 API 等级兼容。
- **基础渲染与布局细节**（Android、跨端）：极小圆角导致的整体透明问题得到修复，颜色字符串统一按无符号 ARGB 十进制输出，嵌套滚动回弹策略回退到原有行为，并补齐渲染视图的标签获取能力。

历史提交可见 [2.21.0...2.22.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.21.0...2.22.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix(android): avoid transparent layout when borderRadius is tiny · [#1431](https://github.com/Tencent-TDS/KuiklyUI/pull/1431)
- chore: modify android rv and appcompat version · [#1453](https://github.com/Tencent-TDS/KuiklyUI/pull/1453)
- fix(android): fix safeAreaInsets.bottom inflated by IME height when soft keyboard shows · [#1462](https://github.com/Tencent-TDS/KuiklyUI/pull/1462)
- feat: revert android avoid overscroll when nest scroll (#1042) · [#1475](https://github.com/Tencent-TDS/KuiklyUI/pull/1475)
- feat: Android renderView getViewTag · [#1481](https://github.com/Tencent-TDS/KuiklyUI/pull/1481)

### 鸿蒙

- refactor(ohos): clean up entry CMake linking and public Kuikly include · [#1423](https://github.com/Tencent-TDS/KuiklyUI/pull/1423)
- feat(ohos): refine text editor runtime bridge and app integration · [#1429](https://github.com/Tencent-TDS/KuiklyUI/pull/1429)
- fix(ohos): FILE Mode toImage behave abnormal · [#1415](https://github.com/Tencent-TDS/KuiklyUI/pull/1415)
- fix(ohos): error handling after download failure in ohos NetworkCacheUtil · [#1411](https://github.com/Tencent-TDS/KuiklyUI/pull/1411)
- fix: ohos setContentOffset support negative number · [#1437](https://github.com/Tencent-TDS/KuiklyUI/pull/1437)
- fix(ohos): api23 compatibility · [#1455](https://github.com/Tencent-TDS/KuiklyUI/pull/1455)
- fix(ohos): scroller detect consume wrong for super_touch · [#1458](https://github.com/Tencent-TDS/KuiklyUI/pull/1458)
- feat(ohos): support selection color in text editor · [#1440](https://github.com/Tencent-TDS/KuiklyUI/pull/1440)
- fix(ohos): fix scroller velocity params · [#1463](https://github.com/Tencent-TDS/KuiklyUI/pull/1463)

### Compose

- fix(compose): keep snap target key during pager align corrections · [#1427](https://github.com/Tencent-TDS/KuiklyUI/pull/1427)
- fix(compose): correct pager fling snap base and align boundary keep-key · [#1439](https://github.com/Tencent-TDS/KuiklyUI/pull/1439)
- fix(compose): add pullToRefreshItem topInset for overlay header (#1325) · [#1448](https://github.com/Tencent-TDS/KuiklyUI/pull/1448)
- fix(compose): LazyGrid contentSize shrink and pager contentPadding boundary · [#1451](https://github.com/Tencent-TDS/KuiklyUI/pull/1451)
- fix(compose): sync pager snap anchor and boundary alignment from compose branch · [#1461](https://github.com/Tencent-TDS/KuiklyUI/pull/1461)
- fix(iOS): cursor jump in textField and TextArea · [#1464](https://github.com/Tencent-TDS/KuiklyUI/pull/1464)

### H5

- Bugfix/web render fix and doc · [#1428](https://github.com/Tencent-TDS/KuiklyUI/pull/1428)
- fix(web): Disable iOS text auto-sizing to prevent font enlargement in… · [#1430](https://github.com/Tencent-TDS/KuiklyUI/pull/1430)

### 跨端

- fix: Color.toString() 使用 toUInt() 输出无符号 ARGB 十进制 · [#1421](https://github.com/Tencent-TDS/KuiklyUI/pull/1421)

### 多端

- feat: align iOS and OHOS syncSendEvent policy with Android · [#1400](https://github.com/Tencent-TDS/KuiklyUI/pull/1400)

### 其他

- docs: update changelog · [#1445](https://github.com/Tencent-TDS/KuiklyUI/pull/1445)
- fix: adjust architecture.png · [#1447](https://github.com/Tencent-TDS/KuiklyUI/pull/1447)
- chore: update github codeowners · [#1449](https://github.com/Tencent-TDS/KuiklyUI/pull/1449)
- chore: .gitignore add .mcp.json · [#1450](https://github.com/Tencent-TDS/KuiklyUI/pull/1450)
- chore: optimize DragItemListDemoPage keepalive · [#1403](https://github.com/Tencent-TDS/KuiklyUI/pull/1403)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

## 条件性行为变化

### 鸿蒙页面尺寸变化事件的同步策略

- 结果：尺寸变化事件不再由框架默认同步，同步处理交由宿主策略决定。
- 影响：页面依赖旋转、分屏或容器变化时在当前布局周期内完成更新的场景。
- 迁移：在宿主 Delegate 中显式将 `rootViewSizeDidChanged` 配置为同步事件。
- 说明文档：[页面事件](../../DevGuide/pager-event.md)

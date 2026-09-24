# 2.16.0 变更说明

## Part 1 总体说明

2.16.0 为 iOS 补上裁剪与首屏渲染的新能力，列表、分页与下拉刷新在更多边界场景下不再卡住，H5 文本排版与鸿蒙的崩溃定位能力同步增强，接入文档也更加完善。具体如下：

- **iOS 裁剪与滚动控制**：新增 clipPath 裁剪能力，TurboDisplay 首屏渲染得到优化，滚动容器支持单独关闭惯性滑动，并修复渐变视图重复注册导致的崩溃与圆角归零后遮罩未清理的问题。
- **列表与刷新组件的可靠性**：LazyGrid 在布局更新后能继续加载更多，分页吸附偏移与减速计算更准确，下拉刷新在鸿蒙上不再卡在刷新状态，vforLazy 列表尺寸变化时的布局错误也被修复。
- **H5 排版与鸿蒙崩溃治理**：H5 文本支持换行间距，Compose 图片在部分填充模式下不再空白；鸿蒙接入崩溃上报并修复死锁，线上问题更容易定位。
- **接入文档与社区共建**：补充图片适配器等待时机、TurboDisplay、Compose 自定义节点基类与常见 FAQ 说明，新增 AI 预览，并上线贡献者荣誉墙与社区板块。

历史提交可见 [2.15.3...2.16.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.15.3...2.16.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### iOS

- fix: ios KRGradientView register crash on TextDemo repeatedly enter · [#1117](https://github.com/Tencent-TDS/KuiklyUI/pull/1117)
- feat(ios): support flingEnable for KRScrollView · [#1126](https://github.com/Tencent-TDS/KuiklyUI/pull/1126)
- feat: ios add TurboDisplay optimize · [#1119](https://github.com/Tencent-TDS/KuiklyUI/pull/1119)
- feat: iOS support clipPath · [#974](https://github.com/Tencent-TDS/KuiklyUI/pull/974)
- fix: restore layer mask cleanup for zero borderRadius · [#1146](https://github.com/Tencent-TDS/KuiklyUI/pull/1146)

### 鸿蒙

- feat(ohos): crash report using bugly · [#1134](https://github.com/Tencent-TDS/KuiklyUI/pull/1134)
- fix(ohos): deadlock issue · [#1144](https://github.com/Tencent-TDS/KuiklyUI/pull/1144)

### Compose

- fix: LazyGrid cannot load more when layout updated between scroll callback and scrollEnd callback · [#964](https://github.com/Tencent-TDS/KuiklyUI/pull/964)
- fix: compose image not show for some ContentScale type · [#1107](https://github.com/Tencent-TDS/KuiklyUI/pull/1107)
- fix: pager snap offset calculation and deceleration handling · [#1133](https://github.com/Tencent-TDS/KuiklyUI/pull/1133)
- fix: prevent PullToRefresh stuck in REFRESHING state on HarmonyOS · [#1139](https://github.com/Tencent-TDS/KuiklyUI/pull/1139)

### H5

- feat: h5 support text lineBrakMargin · [#1105](https://github.com/Tencent-TDS/KuiklyUI/pull/1105)

### 跨端

- fix: vforLazy layout error when resize list · [#1140](https://github.com/Tencent-TDS/KuiklyUI/pull/1140)

### 其他

- docs: add android imageadapter shouldWaitViewDidLoad · [#1106](https://github.com/Tencent-TDS/KuiklyUI/pull/1106)
- docs: 新增贡献者荣誉激励机制实施细则（试运行） · [#1110](https://github.com/Tencent-TDS/KuiklyUI/pull/1110)
- docs: optimize community section structure and content · [#1111](https://github.com/Tencent-TDS/KuiklyUI/pull/1111)
- docs: add honor wall page for community contributors · [#1112](https://github.com/Tencent-TDS/KuiklyUI/pull/1112)
- docs: update MakeKuiklyComposeNode guide with correct base classes and clearer descriptions · [#1116](https://github.com/Tencent-TDS/KuiklyUI/pull/1116)
- docs: add FAQ for TextField auto-focus issue after closing BottomSheet · [#1100](https://github.com/Tencent-TDS/KuiklyUI/pull/1100)
- docs: add list didappear notes · [#1129](https://github.com/Tencent-TDS/KuiklyUI/pull/1129)
- docs: add ohos color adapter · [#1130](https://github.com/Tencent-TDS/KuiklyUI/pull/1130)
- feat: ai preview · [#1135](https://github.com/Tencent-TDS/KuiklyUI/pull/1135)
- docs: update maxItemLoad and TextShadow docs · [#1143](https://github.com/Tencent-TDS/KuiklyUI/pull/1143)
- docs: add TurboDisplay docs · [#1138](https://github.com/Tencent-TDS/KuiklyUI/pull/1138)

---

## Part 3 破坏性变更

## 条件性行为变化

### Android `enableLazyClipChildren()`

- 结果：新增实验性裁剪优化开关，开启后会裁剪超出父容器边界的子内容。
- 影响：业务主动调用该开关时触发。
- 迁移：依赖溢出显示的页面保持关闭，启用前先完成视觉回归。
- 说明文档：[Android 工程接入](../../QuickStart/android.md)

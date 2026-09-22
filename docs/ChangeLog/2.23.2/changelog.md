# 2.23.2 变更说明

## Part 1 总体说明

2.23.2 让一次触摸只命中一个视图，TurboDisplay 页面的状态与页面创建时机保持一致，鸿蒙滚动位移动画拿到合理的阻尼默认值，并补齐了 H5 多 module 场景的接入说明。具体如下：

- **触控命中的唯一性**：修复了 iOS 与鸿蒙上触摸事件分发时多个视图同时响应的问题，一次触摸现在只会落到真正命中的那个视图上，嵌套、重叠布局下的点击与手势不再互相串扰。
- **TurboDisplay 的状态对齐**（iOS）：TurboDisplay 的开启状态改为跟随页面创建生命周期同步，页面从创建到首屏渲染期间拿到的状态是自洽的，不再出现状态与页面实际阶段错位导致的渲染异常。
- **滚动位移动画的阻尼默认值**（鸿蒙）：为滚动容器的内容偏移设置补齐了阻尼参数的默认值，未显式指定时的位移动画与其他端保持一致，不再因缺少默认值而表现僵硬或突变。
- **H5 多模块接入与遍历开销**（H5）：补充了 H5 多 module 场景下 UMD 产物的接入说明；同时修复了渲染过程中的重复遍历，布局与属性同步的额外开销被消除。

历史提交可见 [2.23.1...2.23.2](https://github.com/Tencent-TDS/KuiklyUI/compare/2.23.1...2.23.2)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### iOS

- fix(iOS): align isTurboModule state with page created lifecycle · [#1537](https://github.com/Tencent-TDS/KuiklyUI/pull/1537)

### 鸿蒙

- fix(ohos): default damping arg for SetArkUIContentOffset · [#1534](https://github.com/Tencent-TDS/KuiklyUI/pull/1534)

### H5

- fix(h5): h5 multi module umd patch doc · [#1540](https://github.com/Tencent-TDS/KuiklyUI/pull/1540)

### 多端

- fix: iOS and Ohos touch event dispatch issue causing multiple views to respond · [#1508](https://github.com/Tencent-TDS/KuiklyUI/pull/1508)

### 其他

- [Bugfix] 修复多次遍历问题 · [#1526](https://github.com/Tencent-TDS/KuiklyUI/pull/1526)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

# 2.11.0 变更说明

## Part 1 总体说明

2.11.0 让触摸与手势事件的坐标在多端回到正确口径，补齐了鸿蒙端的素材转换与性能数据能力，并让上层编译链路更快、Compose 运行时更稳，具体如下：

- **手势与触摸坐标的准确性**（iOS、鸿蒙、H5）：点击、长按、拖拽等事件拿到的坐标按组件坐标系与页面根视图坐标系正确换算，非全屏容器与带动画的 H5 页面中，自定义手势与命中判断不再整体偏移。
- **素材转换与性能数据的补齐**（鸿蒙）：PAG 素材可直接转换使用，页面创建时可指定初始尺寸，性能数据能通过 ArkTS 回调取回，鸿蒙端与 Android、iOS 的能力差距进一步缩小。
- **上层构建与 Compose 运行时**（跨端、Compose、H5）：KSP 增量编译缩短了改动后的编译耗时，Web 侧改用远端 KSP 简化接入，Compose 弹层的上下文透传与 JS 端生命周期问题得到修复。

历史提交可见 [2.10.0...2.11.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.10.0...2.11.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### iOS

- fix: event coordinates conversion issue · [#746](https://github.com/Tencent-TDS/KuiklyUI/pull/746)
- refactor: iOS update deployment target version and resolve all warnings · [#757](https://github.com/Tencent-TDS/KuiklyUI/pull/757)

### 鸿蒙

- fix(ohos): touch coordinates for non-fullscreen containers · [#573](https://github.com/Tencent-TDS/KuiklyUI/pull/573)
- fix: thread flag error when synchronously `sendEvent` on ohos · [#754](https://github.com/Tencent-TDS/KuiklyUI/pull/754)
- feat: Ohos PAG support convert asstes · [#662](https://github.com/Tencent-TDS/KuiklyUI/pull/662)
- feat: support ohos performance api arkts callback · [#729](https://github.com/Tencent-TDS/KuiklyUI/pull/729)
- chore: update version to 2.11.0 · [#766](https://github.com/Tencent-TDS/KuiklyUI/pull/766)

### Compose

- fix: local providers for dialog · [#740](https://github.com/Tencent-TDS/KuiklyUI/pull/740)
- fix: lifecycle error on js target · [#759](https://github.com/Tencent-TDS/KuiklyUI/pull/759)

### H5

- feat: fix web width is null for aniamtion · [#760](https://github.com/Tencent-TDS/KuiklyUI/pull/760)

### 跨端

- fix: thread check not working properly on ios and ohos · [#753](https://github.com/Tencent-TDS/KuiklyUI/pull/753)
- Support ksp incremental · [#748](https://github.com/Tencent-TDS/KuiklyUI/pull/748)

### 多端

- feat: ohos support initial size · [#736](https://github.com/Tencent-TDS/KuiklyUI/pull/736)
- feat: refactor view const · [#743](https://github.com/Tencent-TDS/KuiklyUI/pull/743)
- feat: web use remote ksp && add doc · [#764](https://github.com/Tencent-TDS/KuiklyUI/pull/764)

### 其他

- fix: fix ai chat demo · [#642](https://github.com/Tencent-TDS/KuiklyUI/pull/642)
- fix: fix markdown component version · [#742](https://github.com/Tencent-TDS/KuiklyUI/pull/742)
- docs: update thread-and-coroutines version · [#737](https://github.com/Tencent-TDS/KuiklyUI/pull/737)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

## 条件性行为变化

### 触摸与手势事件坐标的换算修正

- 结果：iOS 的事件坐标换算错误、鸿蒙非全屏容器下的触摸坐标偏移被修正，事件回调中的坐标按「相对组件」与「相对页面根视图」两种口径正确给出。
- 影响：此前为规避错误坐标而自行做过换算、偏移补偿的业务，升级后会拿到正确值，原有补偿会造成二次偏移；判定命中区域、浮层定位的逻辑可能表现不同。
- 迁移：移除业务侧自行添加的坐标补偿，直接读取事件参数中的坐标。
- 说明文档：[组件事件参数](../../API/components/view.md)

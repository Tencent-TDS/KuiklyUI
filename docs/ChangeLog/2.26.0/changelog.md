# 2.26.0 变更说明

## Part 1 总体说明

2.26.0 给出了一批列表与分页的精细控制能力，让鸿蒙业务第一次具备无障碍与网络失败原因的辨识手段，并修掉了几处只在特定集成方式下才会暴露的崩溃与编译问题。具体如下：

- **列表与分页控制**（多端、Compose）：ListView 新增 `initContentOffset` 接口，列表可直接以指定偏移渲染；修复 SliderPager 滚到末项时的偏移溢出，以及 `loop = 0` 时 `defaultPage` 不生效；Compose 新增可选开启的原生属性动画，并让 Pager 在原生吸附切换时保持状态稳定。
- **鸿蒙无障碍与网络诊断**（鸿蒙）：新增无障碍支持，并让网络请求回调带上 `statusCode`，业务可据此区分失败原因；同步补充了鸿蒙产物体积优化指引。
- **工程接入与崩溃修复**（iOS、Android、H5）：iOS 修复通过 SPM 以 release 配置集成 iOS Render 时的编译错误，以及缓存树 tag 链断裂引发的崩溃；Android 完成 TurboDisplay 预准备并修复后台态展开后懒加载内容错位与 PAG 替换图层的图片源问题；H5 修复分包异常。

历史提交可见 [2.25.0...2.26.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.25.0...2.26.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- feat: Android turboDisplay  pre-prepare · [#1679](https://github.com/Tencent-TDS/KuiklyUI/pull/1679)
- fix(Android): PAG ReplaceImageLayer Image src · [#1688](https://github.com/Tencent-TDS/KuiklyUI/pull/1688)

### iOS

- fix(ios): resolve compilation errors in KRReflectionModule when integrating iOS Render via SPM in release builds · [#1622](https://github.com/Tencent-TDS/KuiklyUI/pull/1622)
- fix(ios): resolve crash caused by broken tag chain in cache tree · [#1685](https://github.com/Tencent-TDS/KuiklyUI/pull/1685)

### 鸿蒙

- fix(ohos): add statusCode to network response callback · [#1623](https://github.com/Tencent-TDS/KuiklyUI/pull/1623)

### Compose

- fix(compose): stabilize Pager state across native snaps · [#1629](https://github.com/Tencent-TDS/KuiklyUI/pull/1629)
- fix(compose): realign lazy content after Android background expansion · [#1642](https://github.com/Tencent-TDS/KuiklyUI/pull/1642)
- feat(animation): add opt-in Compose native property animations · [#1633](https://github.com/Tencent-TDS/KuiklyUI/pull/1633)

### 跨端

- feat(core): listView add initContentOffset API · [#1616](https://github.com/Tencent-TDS/KuiklyUI/pull/1616)
- fix(core): sliderpager scroll to last item offset overflow problem · [#1681](https://github.com/Tencent-TDS/KuiklyUI/pull/1681)

### 多端

- feat(ohos): add ohos accessibility · [#1626](https://github.com/Tencent-TDS/KuiklyUI/pull/1626)
- feat(demo): MyModule Demo 命名优化，补齐 iOS/Android module 实现 · [#1637](https://github.com/Tencent-TDS/KuiklyUI/pull/1637)

### 其他

- chore: update tbdisplay test demo · [#1617](https://github.com/Tencent-TDS/KuiklyUI/pull/1617)
- docs(perf): supplement ohos size optimization guidelines · [#1614](https://github.com/Tencent-TDS/KuiklyUI/pull/1614)
- chore: update tbdisplay waterfall demo · [#1678](https://github.com/Tencent-TDS/KuiklyUI/pull/1678)
- fix(core): sliderpager defaultPage not work when loop = 0 · [#1680](https://github.com/Tencent-TDS/KuiklyUI/pull/1680)
- Bugfix/h5 js split fix · [#1686](https://github.com/Tencent-TDS/KuiklyUI/pull/1686)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

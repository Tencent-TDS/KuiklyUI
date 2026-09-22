# 2.21.0 变更说明

## Part 1 总体说明

2.21.0 让多行文本编辑、图片滤镜与手势交互在更多端保持一致，同时补齐多模块产物与编译期页面注册能力，页面开发与宿主集成都更顺畅。具体如下：

- **富文本与输入编辑**（鸿蒙、Compose）：多行输入补齐富文本输入能力，输入框支持自定义文字选中高亮色并提供文本选择容器，编辑态下的选区与光标表现更可控。
- **图片与文本绘制**（Compose、Android、小程序）：图片支持颜色矩阵滤镜效果，低版本 Android 上的文本换行留白兼容问题与小程序自定义字体加载问题一并修复。
- **手势与翻页惯性**（iOS、Compose）：长按不再误打断点击的触摸流程，翻页惯性滑动的吸附落位更稳定，并修复悬空指针引发的无障碍语义崩溃。
- **多模块产物与工程化**（H5、Android、鸿蒙）：JS 分包支持多模块，编译期可在入口获取页面名，键盘模块多线程调用问题与仓库内生成文件清理同步完成。

历史提交可见 [2.20.1...2.21.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.20.1...2.21.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix: Android keyboardModule multiThread problem · [#1398](https://github.com/Tencent-TDS/KuiklyUI/pull/1398)
- fix: compat lineBreakMargin on Android 7.x · [#1410](https://github.com/Tencent-TDS/KuiklyUI/pull/1410)

### iOS

- fix(ios): longpress interrupts click touch process · [#1323](https://github.com/Tencent-TDS/KuiklyUI/pull/1323)

### 鸿蒙

- feat(ohos-input): add ArkUI TextEditor rich input support · [#1397](https://github.com/Tencent-TDS/KuiklyUI/pull/1397)
- chore: remove generated file from repo and remove inline · [#1414](https://github.com/Tencent-TDS/KuiklyUI/pull/1414)

### Compose

- 1.增加输入框文字选中高亮颜色属性 · [#1390](https://github.com/Tencent-TDS/KuiklyUI/pull/1390)
- fix(compose): 修正 JS 端 IdentityHashCode 源文件目录路径 · [#1405](https://github.com/Tencent-TDS/KuiklyUI/pull/1405)
- feat: add image colorMatrix effect implementation · [#1234](https://github.com/Tencent-TDS/KuiklyUI/pull/1234)
- fix(compose): stabilize pager snap settling · [#1409](https://github.com/Tencent-TDS/KuiklyUI/pull/1409)
- Feat/text selection container · [#1260](https://github.com/Tencent-TDS/KuiklyUI/pull/1260)
- fix(Compose): Semantics NPE caused by dangling head pointer · [#1417](https://github.com/Tencent-TDS/KuiklyUI/pull/1417)

### 多端

- Bugfix/miniapp custom font · [#1408](https://github.com/Tencent-TDS/KuiklyUI/pull/1408)

### 其他

- feat(web): js split support multi modules · [#1392](https://github.com/Tencent-TDS/KuiklyUI/pull/1392)
- feat: android ksp support entry get pageName · [#1399](https://github.com/Tencent-TDS/KuiklyUI/pull/1399)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

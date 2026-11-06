# 2.23.0 变更说明

## Part 1 总体说明

2.23.0 把富文本的交互粒度细化到单个 Span，统一了安全区变化通知与像素级布局对齐，并加固鸿蒙端线程调度与绘制流程，同时在组件表现与资源加载上补齐细节。具体如下：

- **富文本的交互粒度**（Android、iOS、鸿蒙）：Span 现在可以单独绑定长按事件，选区类型也统一支持到各端，针对单段文字的交互不再需要退化到整块富文本处理。
- **安全区与像素级对齐**（Android、鸿蒙）：安全区内边距变化时主动通知页面，视图边框按物理像素对齐，避免细线、边框与分隔线出现模糊或错位。
- **鸿蒙调度与绘制稳定性**（鸿蒙）：渲染线程与主线程切换到新的事件循环实现并收敛调度边界，空阴影与零宽视图不再进入绘制流程，文本渲染回退到稳定版本并减少高频日志。
- **组件表现与资源加载**（Compose、H5、Android）：新增可自定义的侧边栏组件，图片加载失败态能够正常展示，H5 图片支持自定义资源前缀，点击补上系统触感反馈，偶发的并发修改异常一并修复。

历史提交可见 [2.22.0...2.23.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.22.0...2.23.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- 1.解决偶尔出现的ConcurrentModificationException异常问题 · [#1473](https://github.com/Tencent-TDS/KuiklyUI/pull/1473)
- feat: Android notify safeareaInsets if changed · [#1486](https://github.com/Tencent-TDS/KuiklyUI/pull/1486)
- fix(android): play touch sound effect on click · [#1488](https://github.com/Tencent-TDS/KuiklyUI/pull/1488)

### 鸿蒙

- refactor(ohos): rebuild KRThread/KRMainThread on libuv, harden scheduler boundaries, and drop dead sync APIs · [#1489](https://github.com/Tencent-TDS/KuiklyUI/pull/1489)
- fix(ohos): skip drawing when shadow is null or frame width is zero · [#1496](https://github.com/Tencent-TDS/KuiklyUI/pull/1496)
- feat(ohos): disable text render v2 and prevent high frequent logs by … · [#1498](https://github.com/Tencent-TDS/KuiklyUI/pull/1498)

### Compose

- 1.增加自定义封装侧边栏组件 · [#1436](https://github.com/Tencent-TDS/KuiklyUI/pull/1436)
- fix(compose): image error state not showing · [#1482](https://github.com/Tencent-TDS/KuiklyUI/pull/1482)
- feat: support SPAN selection type for rich text across Android, iOS and OHOS · [#1493](https://github.com/Tencent-TDS/KuiklyUI/pull/1493)
- fix(compose): migrate ComposeOnKuikly changes since 2026-06-14 · [#1495](https://github.com/Tencent-TDS/KuiklyUI/pull/1495)

### H5

- fix(web): h5 image support custom assets prefix · [#1487](https://github.com/Tencent-TDS/KuiklyUI/pull/1487)

### 跨端

- fix(core): change TextAreaEvent.keyboardHeightChange isSync default to false · [#1494](https://github.com/Tencent-TDS/KuiklyUI/pull/1494)

### 多端

- fix: view frame align to pixel in ohos, android · [#1485](https://github.com/Tencent-TDS/KuiklyUI/pull/1485)
- feat(core): support longPress event on rich-text Span · [#1490](https://github.com/Tencent-TDS/KuiklyUI/pull/1490)

---

## Part 3 破坏性变更

## 条件性行为变化

### TextArea 键盘高度变化事件的同步默认值

- 结果：`keyboardHeightChange` 事件的 `isSync` 默认值由同步改为异步，键盘高度变化回调默认不再同步派发。
- 影响：依赖该事件与键盘动画同帧更新布局的页面，例如输入框跟随键盘高度位移、键盘避让动画。
- 迁移：需要同步回调时显式传入 `isSync = true`。
- 说明文档：[TextArea 组件](../../API/components/text-area.md)

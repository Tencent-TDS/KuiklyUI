# 2.28.0 变更说明

## Part 1 总体说明

2.28.0 让鸿蒙的屏幕与安全区数据、高刷新率下的帧调度更可靠，长列表的内容尺寸不再漂移，H5 上父容器的拖拽与子视图的点击不再互相打断，并梳理了鸿蒙与 iOS 的工程接入配置。具体如下：

- **鸿蒙屏幕与安全区数据**（鸿蒙）：修复安全区与设备宽度数据不匹配的问题，旋转屏幕时页面拿到的是与当前方向一致的尺寸数据，不再出现布局错乱或界面卡死。
- **高刷新率与长列表尺寸**（鸿蒙、Compose）：鸿蒙侧用系统垂直同步信号替换固定定时器来驱动 Compose 帧调度，高刷新率设备可以按屏幕实际节奏出帧；长列表在最后一项可见后固定内容尺寸、并在代码主动跳转后及时解除固定，滚动边界与内容长度不再反复跳动。
- **H5 手势冲突与互操作性能验证**（H5）：修复父容器拖拽与子视图点击互相抢占的问题，拖拽过程中子视图的点击可以正常触发；新增互操作性能基准示例页，便于直接量化跨语言调用的开销。
- **鸿蒙与 iOS 工程接入配置**（鸿蒙、iOS）：鸿蒙侧的扩展能力改为通过编译期工厂接入，产物按需组装；清理了鸿蒙工程的依赖锁文件与拷贝流程，并更新 iOS 多 module 的资源设置与接入文档。

历史提交可见 [2.27.0...2.28.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.27.0...2.28.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### 鸿蒙

- feat(ohos): isolate render extras behind compile-time factories · [#1731](https://github.com/Tencent-TDS/KuiklyUI/pull/1731)
- chore: remove oh-package-lock.json5 and ignore it in gitignore · [#1736](https://github.com/Tencent-TDS/KuiklyUI/pull/1736)
- chore: ensure dirs before copy · [#1738](https://github.com/Tencent-TDS/KuiklyUI/pull/1738)
- fix(ohos): resolve safeArea/deviceWidth data mismatch and freeze on rotation · [#1721](https://github.com/Tencent-TDS/KuiklyUI/pull/1721)

### Compose

- fix(compose): pin Lazy exact contentSize after last item is visible · [#1734](https://github.com/Tencent-TDS/KuiklyUI/pull/1734)
- fix(compose): release Lazy exact contentSize pin after programmatic jump · [#1741](https://github.com/Tencent-TDS/KuiklyUI/pull/1741)
- feat(ohos): replace 12ms timer with native vsync to drive Compose frame dispatcher for high refresh rate support · [#1657](https://github.com/Tencent-TDS/KuiklyUI/pull/1657)

### H5

- fix(h5): fix conflict of parent drag and child click · [#1739](https://github.com/Tencent-TDS/KuiklyUI/pull/1739)

### 多端

- feat: add InteropPerfTestPage benchmark demo · [#1725](https://github.com/Tencent-TDS/KuiklyUI/pull/1725)

### 其他

- docs(ios): update iOS.md · [#1728](https://github.com/Tencent-TDS/KuiklyUI/pull/1728)
- chore: update 2.1 publish version · [#1740](https://github.com/Tencent-TDS/KuiklyUI/pull/1740)
- chore: update iOS multiModule assets settings · [#1742](https://github.com/Tencent-TDS/KuiklyUI/pull/1742)

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

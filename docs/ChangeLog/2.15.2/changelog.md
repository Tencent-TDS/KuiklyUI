# 2.15.2 变更说明

## Part 1 总体说明

2.15.2 聚焦滚动边界与渲染稳定性：分页边缘的 1px 偏差、非边缘滑动不触发回弹等问题被修正，鸿蒙渲染层的线程安全与崩溃得到治理，构建产物与依赖也更加干净。具体如下：

- **滚动边界与内容尺寸**：Android 从非边缘位置滑动也能正常触发回弹；Compose 分页在 Android 上补齐缺失的 1px 边缘（含弹簧动画场景），滚动容器的内容尺寸计算更准确，不再产生负偏移。
- **鸿蒙渲染稳定性**：修复渲染值的跨线程访问安全问题与节点移除时的崩溃，修正带行高文本的垂直对齐偏差，并解决鸿蒙编译报错。
- **动画与构建导出**：Compose 渐变支持偏移动画，旋转补齐兼容的构造函数，iOS 侧 KSP 导出的入口名称得到修正，并清理了无用依赖。

历史提交可见 [2.15.1...2.15.2](https://github.com/Tencent-TDS/KuiklyUI/compare/2.15.1...2.15.2)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix(android): Fix overscroll not triggering when swiping from non-edge · [#1016](https://github.com/Tencent-TDS/KuiklyUI/pull/1016)

### 鸿蒙

- fix: KRRenderValue thread safety improvement · [#1008](https://github.com/Tencent-TDS/KuiklyUI/pull/1008)
- fix: remove node content crash issue · [#1015](https://github.com/Tencent-TDS/KuiklyUI/pull/1015)
- fix: vertical alignment issue rendering text with line height · [#1013](https://github.com/Tencent-TDS/KuiklyUI/pull/1013)
- fix: compile error · [#1020](https://github.com/Tencent-TDS/KuiklyUI/pull/1020)

### Compose

- fix: compose pager 1px edge miss on android · [#1006](https://github.com/Tencent-TDS/KuiklyUI/pull/1006)
- fix: compose gradient support offset animation · [#1003](https://github.com/Tencent-TDS/KuiklyUI/pull/1003)
- fix: compose pager 1px edge miss when spring animation on android (#1… · [#1017](https://github.com/Tencent-TDS/KuiklyUI/pull/1017)
- fix(scroller): improve content size calculation and prevent negative offset · [#1024](https://github.com/Tencent-TDS/KuiklyUI/pull/1024)
- chore: remove unused dependencies · [#1021](https://github.com/Tencent-TDS/KuiklyUI/pull/1021)

### 跨端

- fix: iOS ksp export ojbCName · [#907](https://github.com/Tencent-TDS/KuiklyUI/pull/907)
- fix: ratate compatible constructor · [#1023](https://github.com/Tencent-TDS/KuiklyUI/pull/1023)

### 其他

- chore: enable flags for release demo build only · [#1014](https://github.com/Tencent-TDS/KuiklyUI/pull/1014)
- docs: update changelog version(2.15.1) · [#1025](https://github.com/Tencent-TDS/KuiklyUI/pull/1025)

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

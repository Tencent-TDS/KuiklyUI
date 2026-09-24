# 2.27.0 变更说明

## Part 1 总体说明

2.27.0 把鸿蒙和 iOS 上几处会造成崩溃与界面不刷新的问题修掉，同时给多模块工程补上了编译期的页面重名检查。具体如下：

- **鸿蒙崩溃与界面刷新**（鸿蒙）：修复配置变化（字体缩放、深色模式）后文本未标记脏区导致界面不刷新的问题；修掉双击手势延迟任务访问悬空对象引发的崩溃；window 相关调用加了异常保护；同步主任务的告警阈值改为可配置，宿主可按自身负载调整。
- **多模块页面重名检查**（跨端）：Compose 新增模块页面注册注解与配套 KSP 处理器，多模块工程的页面注册与发现有了编译期依据——多模块工程中若存在同名 `@Page` 页面，会在编译阶段直接失败并列出冲突页面，把页面名改为全局唯一即可。
- **iOS 首屏缓存与 H5 指示条**（iOS、H5）：TurboDisplay 刷新改为遵循节点级的自动更新关闭标记，显式关闭自动更新的节点不再被首屏缓存改写；H5 修复 Tabs 指示器位置异常。

历史提交可见 [2.26.0...2.27.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.26.0...2.27.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### iOS

- fix(ios): TurboDisplay refresh to respect node-level auto-update disable filter · [#1723](https://github.com/Tencent-TDS/KuiklyUI/pull/1723)

### 鸿蒙

- fix(ohos): config change not markText dirty · [#1511](https://github.com/Tencent-TDS/KuiklyUI/pull/1511)
- fix: try catch ohos window problem · [#1719](https://github.com/Tencent-TDS/KuiklyUI/pull/1719)
- feat(ohos): 将 kSyncMainTaskWarnTimeout 改为可配置的文件级 static 变量 · [#1720](https://github.com/Tencent-TDS/KuiklyUI/pull/1720)
- fix(ohos): 修复双击手势 250ms 延迟任务访问悬空对象导致的崩溃 · [#1714](https://github.com/Tencent-TDS/KuiklyUI/pull/1714)

### 跨端

- feat(compose): add KuiklyModulePages annotation and KSP processor support for … · [#1426](https://github.com/Tencent-TDS/KuiklyUI/pull/1426)

### 其他

- docs: update tabs aspect ratio usage note · [#1640](https://github.com/Tencent-TDS/KuiklyUI/pull/1640)
- chore: update InputSpanPage demo pageName · [#1713](https://github.com/Tencent-TDS/KuiklyUI/pull/1713)
- Bugfix/web tabs indicator fix · [#1715](https://github.com/Tencent-TDS/KuiklyUI/pull/1715)
- docs: update changelog · [#1717](https://github.com/Tencent-TDS/KuiklyUI/pull/1717)

---

## Part 3 破坏性变更

## 条件性破坏性

### #1426 同名 `@Page` 页面直接编译失败

- 结果：多模块工程中存在同名 `@Page` 页面时，编译阶段直接失败并列出冲突页面，不再运行时静默覆盖。
- 影响：启用多模块工程且页面名重复时会触发。
- 迁移：按编译报错将冲突页面名改为全局唯一。

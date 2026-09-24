# 2.24.0 变更说明

## Part 1 总体说明

2.24.0 只做了一件事：把首屏加速、下拉刷新和布局尺寸这三处已经暴露出来的精度问题修干净，顺带让依赖新版 Compose 的工程能编译通过。具体如下：

- **首屏加速与下拉刷新**（iOS）：修复 TurboDisplay 分块序列化错误，开启首屏加速的页面不再因序列化异常渲染失败；跳过取值未变化的 `contentInset` 调用，下拉刷新时的抖动消失。
- **布局尺寸精度**（多端）：把尺寸上报的布局数值统一保留两位小数，减少浮点误差累积带来的 1px 级错位与多余重排。
- **新版 Compose 编译**（Compose）：修复升级到 Compose 1.9 后的编译错误，依赖新版 Compose 的工程可直接升级。

历史提交可见 [2.23.3...2.24.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.23.3...2.24.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### iOS

- fix(ios): resolve ios TurboDisplay block serizaliaion error · [#1581](https://github.com/Tencent-TDS/KuiklyUI/pull/1581)

### Compose

- chore: fix compose 1.9 compile error · [#1580](https://github.com/Tencent-TDS/KuiklyUI/pull/1580)
- fix(ios): skip unchanged contentInset calls to prevent pull-to-refresh stutter · [#1582](https://github.com/Tencent-TDS/KuiklyUI/pull/1582)

### 多端

- fix: round layout size up to 2 decimals on size report-back · [#1588](https://github.com/Tencent-TDS/KuiklyUI/pull/1588)

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

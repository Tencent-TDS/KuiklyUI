# 2.10.0 变更说明

## Part 1 总体说明

2.10.0 消除了鸿蒙端在原生扩展调用中同步回调引发的死锁风险，并让同一套 Kuikly 页面可以直接运行在 PC 浏览器中，具体如下：

- **鸿蒙原生调用的并发安全**（鸿蒙）：在原生扩展调用中同步回调业务不再引发死锁，跨端调用密集的页面不会再出现整页卡住、无响应的情况。
- **H5 的桌面端适配**（H5）：H5 端补齐 PC 支持，页面可以在桌面浏览器中打开与交互，不再只面向移动端视口。

历史提交可见 [2.9.1...2.10.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.9.1...2.10.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### 鸿蒙

- fix: deadlock issue when invoking callback in call method · [#734](https://github.com/Tencent-TDS/KuiklyUI/pull/734)
- chore: bump version to 2.10.0 · [#735](https://github.com/Tencent-TDS/KuiklyUI/pull/735)

### H5

- feat(h5): pc support · [#733](https://github.com/Tencent-TDS/KuiklyUI/pull/733)

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。

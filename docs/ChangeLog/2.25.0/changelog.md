# 2.25.0 变更汇总

> 本版导航：[版本说明](./announcement.md) · **变更汇总** · [破坏性与迁移](./breaking.md)

## 多端

- feat: sync TextField emoji input keyboard focus handling from ComposeOnKuikly upstream · [#1593](https://github.com/Tencent-TDS/KuiklyUI/pull/1593)
- fix(compose): guard sendFrame when official Transition IOOB on shared Snapshot · [#1590](https://github.com/Tencent-TDS/KuiklyUI/pull/1590)
- fix(pager): avoid composing intermediate pages on instant scroll · [#1589](https://github.com/Tencent-TDS/KuiklyUI/pull/1589)

## 单端

### 鸿蒙

- chore: update ohos package · [#1612](https://github.com/Tencent-TDS/KuiklyUI/pull/1612)
- fix(ohos): make KN bridge exports self-contained in KRRenderCValue.h · [#1607](https://github.com/Tencent-TDS/KuiklyUI/pull/1607)
- perf(ohos): apply no-side-effect size opts for KN and libkuikly · [#1600](https://github.com/Tencent-TDS/KuiklyUI/pull/1600)
- refactor(ohos): unify ohos interop header as single source of truth · [#1596](https://github.com/Tencent-TDS/KuiklyUI/pull/1596)
- fix(ohos): increase sync main task wait timeout from 5s to 10s · [#1594](https://github.com/Tencent-TDS/KuiklyUI/pull/1594)

### H5

- fix(h5): fix remove listeners
- fix(h5App-js): support KuiklyProcessor interface
- fix(h5): fix Hover component
- fix(h5App-js): custom view support common props
- feat(h5): support textInputStateChange and selectionChange event

## 其他

### 文档

- feat: geo perf for component market · [#1608](https://github.com/Tencent-TDS/KuiklyUI/pull/1608)
- chore(docs): update roadmap && qa doc · [#1603](https://github.com/Tencent-TDS/KuiklyUI/pull/1603)
- fix(h5): update custom font doc

### 工程与示例

- fix(h5App-js): support fontLoaded and updateRootViewSize interface
- feat(h5): demo and doc of get size method after resize

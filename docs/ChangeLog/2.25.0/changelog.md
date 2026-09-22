# 2.25.0 变更说明

## Part 1 总体说明

2.25.0 几乎全部投在鸿蒙工程化上：头文件收敛、产物瘦身、等待超时放宽；另外顺手把翻页性能和 H5 输入交互做了一轮补齐。具体如下：

- **鸿蒙工程接入与产物体积**（鸿蒙）：跨端互操作的 C 头文件收敛为单一权威来源，KN 桥接导出在 `KRRenderCValue.h` 中做到自包含，宿主原生扩展不必再手动补齐依赖；为 Kotlin/Native 与 `libkuikly` 开启无副作用的大小优化；同步主线程任务的等待超时从 5s 放宽到 10s，重负载场景下的误判与中断减少。注意该头文件中的枚举值 `NULL` 已改名为 `NULL_VALUE`，直接引用的原生扩展需同步改名才能编译通过。
- **翻页与输入交互**（多端、H5、Compose）：Pager 在瞬时跳转时不再合成中间页面，翻页更轻快；H5 补齐输入框文本选择等交互；Compose 同步了输入表情时的键盘焦点处理。
- **渲染异常兜底**（Compose）：修复了官方 `Transition` 在共享 Snapshot 上抛越界异常时帧发送未受保护的问题。

历史提交可见 [2.24.0...2.25.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.24.0...2.25.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### 鸿蒙

- fix(ohos): increase sync main task wait timeout from 5s to 10s · [#1594](https://github.com/Tencent-TDS/KuiklyUI/pull/1594)
- refactor(ohos): unify ohos interop header as single source of truth · [#1596](https://github.com/Tencent-TDS/KuiklyUI/pull/1596)
- perf(ohos): apply no-side-effect size opts for KN and libkuikly · [#1600](https://github.com/Tencent-TDS/KuiklyUI/pull/1600)
- fix(ohos): make KN bridge exports self-contained in KRRenderCValue.h · [#1607](https://github.com/Tencent-TDS/KuiklyUI/pull/1607)
- chore: update ohos package · [#1612](https://github.com/Tencent-TDS/KuiklyUI/pull/1612)

### Compose

- fix(pager): avoid composing intermediate pages on instant scroll · [#1589](https://github.com/Tencent-TDS/KuiklyUI/pull/1589)
- fix(compose): guard sendFrame when official Transition IOOB on shared Snapshot · [#1590](https://github.com/Tencent-TDS/KuiklyUI/pull/1590)
- feat: sync TextField emoji input keyboard focus handling from ComposeOnKuikly upstream · [#1593](https://github.com/Tencent-TDS/KuiklyUI/pull/1593)

### 其他

- Feature/h5 input select and others · [#1601](https://github.com/Tencent-TDS/KuiklyUI/pull/1601)
- chore(docs): update roadmap && qa doc · [#1603](https://github.com/Tencent-TDS/KuiklyUI/pull/1603)
- feat: geo perf for component market · [#1608](https://github.com/Tencent-TDS/KuiklyUI/pull/1608)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

## 条件性破坏性

### #1596 鸿蒙 C 头文件枚举值改名：`NULL` → `NULL_VALUE`

- 结果：鸿蒙对外头文件 `KRRenderCValue.h` 的 `enum Type` 中 `NULL` 改名为 `NULL_VALUE`。
- 影响：宿主原生扩展代码中直接引用了该枚举值，升级后编译会失败。
- 迁移：将引用处改为 `NULL_VALUE`。

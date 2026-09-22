# 2.19.0 变更说明

## Part 1 总体说明

2.19.0 把 JS 侧的代码拆分能力落地，H5 与小程序页面不再需要一次性加载整包，首屏体积与后续页面切换的成本都随之下降。具体如下：

- **JS 产物按页面拆分**（H5、小程序）：可以按页面拆出独立产物，打开某个页面时只加载该页面所需的代码。
- **公共代码的抽取与复用**（H5、小程序）：运行时、Kotlin 标准库、框架公共代码与三方依赖被抽成共享 chunk，多页面之间只下载一份，页面切换能够命中缓存。

历史提交可见 [2.18.0...2.19.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.18.0...2.19.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### 多端

- feat: add js split code (merge request !189) · [#1370](https://github.com/Tencent-TDS/KuiklyUI/pull/1370)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

## 条件性行为变化

### JS 编译产物 moduleName 改为全小写

- 结果：`core` 模块的 JS 编译目标 `moduleName` 由 `KuiklyCore-core` 改为 `kuiklycore-core`，JS 产物目录、入口路径与产物文件名中的大写前缀随之变为小写。
- 影响：仅影响小程序 JS 分包 / 多入口场景中自行维护了 `webpack.config.d/split-chunks.js` 的业务——旧的 `cacheGroups` 正则、手写的 `kotlin/KuiklyCore-core/entry/` 路径与硬编码的 `KuiklyCore-core.js` 文件名将不再匹配，分包后会出现找不到产物、JS 加载失败。使用框架默认分包配置、未启用分包，以及 Android / iOS / 鸿蒙原生平台均不受影响。
- 迁移：把 `cacheGroups` 正则中的 `KuiklyCore-core` 改为 `kuiklycore-core`，并同步把硬编码的产物路径与产物文件名改为小写形式。

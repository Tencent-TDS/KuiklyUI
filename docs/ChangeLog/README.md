# 更新日志

Kuikly 每个正式版本提供一份变更说明，其中包含三部分：

- **总体说明**（这版能多做什么）
- **变更汇总**（按端分节的清单化事实）
- **破坏性变更**（升级风险与迁移步骤）

## 阅读指引

升级前建议先看「Part 1 总体说明」了解这版带来什么；确认升级风险时看「Part 3 破坏性变更」；需要逐条查证改动细节时，再看「Part 2 变更汇总」。

## 分节口径

「Part 2 变更汇总」按改动实际落在哪些 Native 端划分：

- **Android / iOS / 鸿蒙 / H5 / 小程序 / macOS**：单端改动，即只涉及一个 Native 端。
- **多端**：同时改动了多个 Native 端。
- **跨端**：只改 Kotlin 上层、无 Native 改动，一次改完各端都生效。
- **Compose**：Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入「多端」。
- **其他**：文档、示例与发版杂项。

## 制品源

- maven: https://repo1.maven.org/maven2/com/tencent/kuikly-open/ （2.5.0 版本后使用该 maven 源）
- maven 镜像: https://mirrors.tencent.com/nexus/repository/maven-tencent/
- cocoapods: https://cocoapods.org/pods/OpenKuiklyIOSRender
- ohpm: https://ohpm.openharmony.cn/#/cn/detail/@kuikly-open%2Frender

## 版本列表

- [2.28.0](./2.28.0/changelog.md)（2026-09-16）
- [2.27.0](./2.27.0/changelog.md)（2026-09-03）
- [2.26.0](./2.26.0/changelog.md)（2026-08-27）
- [2.25.0](./2.25.0/changelog.md)（2026-08-07）
- [2.24.0](./2.24.0/changelog.md)（2026-07-31）
- [2.23.3](./2.23.3/changelog.md)（2026-07-29）
- [2.23.2](./2.23.2/changelog.md)（2026-07-15）
- [2.23.1](./2.23.1/changelog.md)（2026-07-10）
- [2.23.0](./2.23.0/changelog.md)（2026-07-06）
- [2.22.0](./2.22.0/changelog.md)（2026-06-26）
- [2.21.0](./2.21.0/changelog.md)（2026-06-04）
- [2.20.1](./2.20.1/changelog.md)（2026-06-02）
- [2.20.0](./2.20.0/changelog.md)（2026-05-26）
- [2.19.1](./2.19.1/changelog.md)（2026-05-21）
- [2.19.0](./2.19.0/changelog.md)（2026-05-19）
- [2.18.0](./2.18.0/changelog.md)（2026-05-11）
- [2.17.0](./2.17.0/changelog.md)（2026-04-22）
- [2.16.0](./2.16.0/changelog.md)（2026-03-20）
- [2.15.3](./2.15.3/changelog.md)（2026-03-06）
- [2.15.2](./2.15.2/changelog.md)（2026-02-05）
- [2.15.1](./2.15.1/changelog.md)（2026-02-03）
- [2.15.0](./2.15.0/changelog.md)（2026-02-02）
- [2.14.0](./2.14.0/changelog.md)（2026-01-19）
- [2.13.0](./2.13.0/changelog.md)（2026-01-05）
- [2.12.1](./2.12.1/changelog.md)（2025-12-22）
- [2.12.0](./2.12.0/changelog.md)（2025-12-19）
- [2.11.0](./2.11.0/changelog.md)（2025-12-08）
- [2.10.0](./2.10.0/changelog.md)（2025-11-28）

## 历史版本

全部版本的发布日期与 Release 入口见 [历史版本列表](./changelog.md)；列表中每个版本都附有指向本目录对应变更说明的链接。

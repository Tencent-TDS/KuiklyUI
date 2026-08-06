# KuiklyUI E2E 引擎（vendored）

本目录是发布器自动化回归框架的**执行引擎**，已随仓库内置（vendored），
**不依赖任何 AI 助手 Skill**，也不在 `.kuikly-harness/` 下。

## 它是什么

- 一个纯 TypeScript 写的 **HTTP → Appium 翻译桥**（`server.ts`）。
- 监听 `7900`（可用环境变量 `MOBILE_TEST_PORT` 覆盖）。
- 把测试脚本发来的简单请求（`{"selector":{"testTag":"x"}}`）翻译成 Appium/UiAutomator2 操作。
- 默认去连 `http://127.0.0.1:4723` 的 Appium（端口用 `--port` 与脚本约定）。
- **它不是一个 AI/大模型**：运行时链路（测试脚本 → 引擎 → Appium → 手机）里没有任何 LLM 参与。

## 为什么搬到这里

原先它住在 AI 助手 Skill 目录 `.kuikly-harness/.agents/skills/kuikly-mobile-test/`，
使「非 AI 驱动」的回归框架在打包上仍绑定了一个研发辅助 Skill。
现已抽出并 vendored 进本项目 `demo/e2e-engine/`，框架运行与任何 AI Skill 解耦。

## 目录结构

```
e2e-engine/
├── src/                      # 引擎源码（纯 TS，无 skill 专属逻辑）
│   ├── server.ts             # HTTP 服务入口（监听 7900）
│   ├── mobile-driver.ts      # 驱动接口
│   ├── appium-mobile-driver.ts
│   ├── view-tree.ts
│   ├── ui-stable.ts
│   ├── quiescence-anchor.ts
│   ├── snapshot-normalizer.ts
│   ├── uiautomator2-error.ts
│   └── ...
├── package.json              # 仅保留 server 脚本与运行依赖
├── tsconfig.json
└── node_modules/             # 已装依赖（gitignored，仓库不跟踪）
```

## 手动启动（不靠一键启动器时）

```bash
# 终端 A — 起 Appium
appium --port 4723

# 终端 B — 起本引擎
cd demo/e2e-engine
npm run server        # 监听 http://localhost:7900
```

`node_modules` 已随仓库本地存在，无需联网 `npm install` 即可 `npm run server`。
若需重装：`npm install`（依赖仅 `webdriverio` + `appium-adb`）。

## 端口约定

| 服务 | 端口 | 备注 |
|---|---|---|
| Appium | 4723 | 由 Appium 自身监听 |
| E2E 引擎 | 7900 | 脚本 `ENGINE_URL` 默认值；可用 `MOBILE_TEST_PORT` 覆盖 |

## 与测试脚本的关系

`demo/mention_publisher_e2e_test.sh`（纯 `sh`+`curl`，非 AI 驱动）向 `7900` 发请求，
经由本引擎转发到 Appium。脚本自身不感知本目录路径——它只认 `localhost:7900`。

推荐用 `demo/run_publisher_local.sh` 一键拉起 Appium + 本引擎 + 装包 + 跑脚本 + 收尾。

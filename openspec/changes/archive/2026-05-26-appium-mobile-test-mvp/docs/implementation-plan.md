# Appium 移动端测试 MVP 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 结合已有的 `kuikly-app-runner` 编译/运行/日志能力与 Appium UI 自动化，验证 Android + iOS 的 AI 移动端测试闭环。

**架构：** `kuikly-app-runner` 继续负责编译、安装、启动和日志采集。新增一层薄的 `MobileDriver` 抽象，由 Appium 驱动，提供 UI 快照、选择器操作、等待、断言、截图和证据收集。MVP 先在一条简单的 Android 路径和一条简单的 iOS 路径上验证闭环，再扩展范围。

**技术栈：** KuiklyUI demo 应用、`kuikly-app-runner`、Appium Server、Appium UiAutomator2 driver、Appium XCUITest driver / WDA、Node.js 或 Python Appium 客户端、Android 模拟器、iOS 模拟器。

---

## 范围

P0 包含：

| 领域 | 包含内容 |
| --- | --- |
| 平台 | Android + iOS 模拟器/仿真器 |
| 编译 / 安装 / 启动 | 尽量复用 `kuikly-app-runner` 路径 |
| UI 自动化 | Appium 驱动的 `MobileDriver` |
| UI 状态 | Appium page source 转换为紧凑的 AI 快照 |
| 操作 | tap、input、back、waitFor、assertVisible、assertText |
| 证据 | 截图、page source、日志、步骤追踪 |
| 报告 | 本地 Markdown 或 JSON 报告 |

P0 不包含：

```text
mock 登录
mock 接口
网络抓包
特性开关
需求 / Bug 平台集成
视觉稿对比
AI 读取截图
鸿蒙
```

## 架构

```text
AI / 测试场景
  ↓
AI 测试运行器
  ↓
Kuikly App Runner 适配器  → 编译 / 安装 / 启动 / 采集日志
  ↓
MobileDriver API
  ↓
AppiumMobileDriver
  ↓
Appium Server
  ↓
Android: UiAutomator2 Driver
iOS: XCUITest Driver / WDA
```

## 关键决策

| 决策项 | 选择 |
| --- | --- |
| 编译/运行/日志归属 | `kuikly-app-runner` 保持负责 |
| UI 操作后端 | Appium MVP 后端 |
| 是否直接暴露 Appium | 不向 AI/测试场景直接暴露 Appium |
| 快照格式 | 将 Appium page source 转换为紧凑 JSON |
| 截图用途 | 仅作证据，不作为 AI 主要输入 |
| 长期后端 | 保留替换为原生 UiAutomator + XCUITest/WDA 的空间 |

## 任务 1：确认 Appium 环境

**文件：**
- 预期无仓库代码变更。
- 将发现记录到：`openspec/changes/appium-mobile-test-mvp/docs/framework-decision.md`

**步骤 1：检查本地 Appium 可用性**

运行：

```bash
appium --version
```

期望：

```text
打印 Appium 版本号。
```

如果缺失，按团队环境策略安装 Appium。

**步骤 2：检查 Appium 驱动**

运行：

```bash
appium driver list --installed
```

期望已安装的驱动：

```text
uiautomator2
xcuitest
```

**步骤 3：安装缺失的驱动（如有）**

仅对缺失的驱动运行：

```bash
appium driver install uiautomator2
appium driver install xcuitest
```

期望：

```text
驱动安装成功。
```

**步骤 4：手动启动 Appium 服务器**

运行：

```bash
appium --base-path /wd/hub
```

期望：

```text
Appium 服务器启动并监听会话请求。
```

**步骤 5：记录环境信息**

将环境相关的约束更新到 `openspec/changes/appium-mobile-test-mvp/docs/framework-decision.md`，如 Appium 版本、驱动版本、Xcode 签名问题或 Android SDK 要求等。

## 任务 2：定义 MobileDriver API

**文件：**
- 创建：`.claude/skills/kuikly-mobile-test/README.md`
- 创建：`.claude/skills/kuikly-mobile-test/src/mobile-driver.ts` 或使用其他语言的等效文件

**步骤 1：创建工具目录**

在 `.claude/skills/kuikly-mobile-test/` 下创建一个独立的小型 MVP 目录。

**步骤 2：定义接口**

添加等效于以下内容的接口：

```ts
export type Platform = "android" | "ios"

export type Selector =
  | { id: string }
  | { text: string }
  | { accessibilityId: string }
  | { xpath: string }

export interface UiElementSnapshot {
  id?: string
  text?: string
  type?: string
  enabled?: boolean
  clickable?: boolean
  visible?: boolean
  bounds?: [number, number, number, number]
}

export interface UiSnapshot {
  platform: Platform
  source: "appium"
  elements: UiElementSnapshot[]
}

export interface MobileDriver {
  startSession(): Promise<void>
  stopSession(): Promise<void>
  getSnapshot(): Promise<UiSnapshot>
  tap(selector: Selector): Promise<void>
  input(selector: Selector, text: string): Promise<void>
  back(): Promise<void>
  waitFor(selector: Selector, timeoutMs: number): Promise<void>
  assertVisible(selector: Selector): Promise<void>
  assertText(text: string): Promise<void>
  takeScreenshot(outputPath: string): Promise<void>
}
```

**步骤 3：添加 README 契约**

文档说明 AI/测试场景只能调用 `MobileDriver`，不能直接调用 Appium 原始 API。

**步骤 4：验证类型检查**

对 MVP 工具运行仓库对应的类型检查或构建命令。如果尚无工具链，在后续任务中添加最小化的包配置。

## 任务 3：实现 AppiumMobileDriver 骨架

**文件：**
- 创建：`.claude/skills/kuikly-mobile-test/src/appium-mobile-driver.ts`
- 修改：`.claude/skills/kuikly-mobile-test/src/mobile-driver.ts`

**步骤 1：添加构造函数配置**

支持：

```ts
export interface AppiumMobileDriverConfig {
  platform: Platform
  appiumUrl: string
  appPackage?: string
  appActivity?: string
  bundleId?: string
  deviceName?: string
  platformVersion?: string
}
```

**步骤 2：实现会话生命周期**

使用 Appium 客户端 API 实现 `startSession()` 和 `stopSession()`。

**步骤 3：实现选择器转换**

将内部选择器映射到 Appium 定位器：

```text
{ id } → id
{ accessibilityId } → accessibility id
{ text } → 平台相关的文本定位器或 XPath 降级
{ xpath } → xpath
```

**步骤 4：实现基本操作**

实现：

```text
tap
input
back
waitFor
assertVisible
assertText
takeScreenshot
```

**步骤 5：保持快照转换最小化**

MVP 阶段，解析 Appium page source XML，仅返回有 id/text/type/enabled/clickable/bounds 的可见或有意义元素。

## 任务 4：添加 Android MVP 场景

**文件：**
- 创建：`.claude/skills/kuikly-mobile-test/scenarios/android-smoke.ts`
- 运行时创建：`.claude/skills/kuikly-mobile-test/reports/`

**步骤 1：使用 kuikly-app-runner 进行 Android 编译/运行**

使用已知的 Android runner 流程：

```text
编译 androidApp debug
安装应用
启动应用
采集 logcat 到 ./logs/kuikly_android.log
```

遵循 `kuikly-app-runner` 规则：

```text
所有日志放在 ./logs/ 下
不使用仅输出的构建
使用描述性的 kuikly_ 日志名称
```

**步骤 2：对已启动的应用开启 Appium 会话**

使用 Android 包的 capabilities：

```text
appPackage = com.tencent.kuikly.android.demo
automationName = UiAutomator2
platformName = Android
```

**步骤 3：采集初始快照**

调用：

```ts
const snapshot = await driver.getSnapshot()
```

期望：

```text
快照包含 Kuikly demo 应用的可见文本或可交互元素。
```

**步骤 4：执行一个稳定操作**

从实际 page source 中选择一个稳定的选择器。优先使用 id/accessibility id。如不可用，MVP 阶段可使用 text。

**步骤 5：断言结果**

使用 `assertVisible` 或 `assertText` 验证确定性的 UI 结果。

**步骤 6：保存报告**

写入包含以下内容的报告：

```text
平台
步骤
快照摘要
断言结果
失败时的截图路径
日志路径
```

## 任务 5：添加 iOS MVP 场景

**文件：**
- 创建：`.claude/skills/kuikly-mobile-test/scenarios/ios-smoke.ts`

**步骤 1：使用 kuikly-app-runner 进行 iOS 编译/运行**

使用已有的 iOS 模拟器 runner 流程：

```text
如需要则 pod install
xcodebuild 模拟器构建
安装应用到模拟器
启动应用
采集控制台日志到 ./logs/kuikly_console.log
```

**步骤 2：启动 Appium 会话**

使用 capabilities：

```text
platformName = iOS
automationName = XCUITest
bundleId = com.tencent.kuiklycore.demo.<用户名>
deviceName = 选定的模拟器名称
```

**步骤 3：采集初始快照**

调用 `getSnapshot()` 并确认 accessibility 元素可见。

**步骤 4：执行一个稳定操作**

优先使用 `accessibilityIdentifier`。如不可用，MVP 阶段可使用 text，并记录后续需添加测试标识才能保证稳定性。

**步骤 5：断言结果并保存报告**

使用与 Android 相同的报告格式。

## 任务 6：添加快照压缩

**文件：**
- 创建：`.claude/skills/kuikly-mobile-test/src/snapshot-normalizer.ts`
- 测试：`.claude/skills/kuikly-mobile-test/src/snapshot-normalizer.test.ts`

**步骤 1：编写 XML 转快照的测试**

使用小型 Android XML 和 iOS XML 样本进行测试。

期望输出：

```json
{
  "elements": [
    {
      "id": "login_button",
      "text": "登录",
      "type": "Button",
      "enabled": true
    }
  ]
}
```

**步骤 2：实现最小化解析器**

仅解析 AI 需要的字段：

```text
id
text
type
enabled
clickable
visible
bounds
```

**步骤 3：添加过滤**

过滤掉没有 text、没有 id、没有 accessibility id 且没有交互信号的元素。

**步骤 4：添加大小检查**

MVP 阶段，若压缩后的快照超过 10KB 则发出警告。

## 任务 7：添加证据收集

**文件：**
- 创建：`.claude/skills/kuikly-mobile-test/src/evidence.ts`
- 修改：Android/iOS 场景文件

**步骤 1：定义证据格式**

```ts
export interface EvidenceBundle {
  platform: Platform
  startedAt: string
  endedAt: string
  status: "passed" | "failed"
  steps: Array<{ name: string; status: string; error?: string }>
  snapshotPath?: string
  screenshotPath?: string
  logPaths: string[]
}
```

**步骤 2：每次失败时保存快照**

写入压缩快照 JSON 和原始 Appium page source 供调试。

**步骤 3：每次失败时保存截图**

截图仅供人工审查的证据。

**步骤 4：关联 kuikly 日志**

引用：

```text
./logs/kuikly_android.log
./logs/kuikly_console.log
```

## 任务 8：运行稳定性门槛

**文件：**
- 创建：`.claude/skills/kuikly-mobile-test/scripts/run-stability.ts`

**步骤 1：运行 Android 场景 5 次**

期望：

```text
至少 4/5 通过。
```

**步骤 2：运行 iOS 场景 5 次**

期望：

```text
至少 4/5 通过。
```

**步骤 3：记录失败分类**

将失败归类：

```text
Appium 会话失败
选择器失败
超时
应用崩溃
断言失败
环境问题
```

**步骤 4：更新决策文档**

更新 `openspec/changes/appium-mobile-test-mvp/docs/framework-decision.md`，记录 Appium 是否满足 MVP 退出条件。

## 任务 9：决定 MVP 后续走向

**文件：**
- 修改：`openspec/changes/appium-mobile-test-mvp/docs/framework-decision.md`
- 修改：`openspec/changes/appium-mobile-test-mvp/docs/loop.md`

**步骤 1：评估 Appium 退出条件**

使用以下检查清单：

```text
会话创建时间可接受
page source 稳定
快照足够紧凑
选择器稳定
失败证据完整
每个平台 4/5 稳定性
错误可诊断
```

**步骤 2：选择下一个后端路径**

决策选项：

```text
继续使用 Appium 后端
iOS 用 Appium，Android 用原生
用原生后端替换 Appium
仅对 demo 流程使用 Maestro
```

**步骤 3：撰写最终 MVP 结论**

记录：

```text
哪些方面有效
哪些方面失败
需要哪些选择器
SDK P1 应该添加什么
Appium 是否继续作为 P0 后端
```

## 验证命令

实现文件创建后，使用以下确切命令：

```bash
npm test --prefix .claude/skills/kuikly-mobile-test
npm run lint --prefix .claude/skills/kuikly-mobile-test
npm run android:smoke --prefix .claude/skills/kuikly-mobile-test
npm run ios:smoke --prefix .claude/skills/kuikly-mobile-test
```

如果 MVP 工具选择了其他语言，在开始实现前将这些命令替换为等效的测试、lint、Android 冒烟和 iOS 冒烟命令。

## 待定问题

| 问题 | 当前倾向 |
| --- | --- |
| Node.js 还是 Python Appium 客户端 | 如需 TypeScript 接口则选 Node.js；如需更快对接 agent 工具则选 Python |
| Appium 启动应用还是附加到 runner 启动的应用 | 优先 runner 编译/安装，Appium 会话根据平台稳定性决定启动或附加 |
| 第一个测试页面 | 选择一个有可见确定性文本的稳定 Kuikly demo 页面 |
| 测试选择器 | 优先 id/accessibility id；MVP 降级使用 text |
| 报告格式 | 先用 JSON + Markdown 摘要 |

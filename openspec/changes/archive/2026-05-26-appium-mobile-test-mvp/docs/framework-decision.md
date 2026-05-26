# AI移动端测试框架选型：原生框架 vs Appium

本文档单独记录 AI 移动端测试闭环 MVP 中，原生自动化框架与 Appium 的差异、取舍和推荐路径。

## 背景

当前 P0 目标是验证 Android + iOS 的 AI 测试闭环：AI 通过结构化 UI 状态理解当前页面，调用自动化工具执行真实用户路径，并基于 UI tree / accessibility tree 做基础断言。编译、安装、启动、日志采集等能力已可通过 `kuikly-app-runner` skill 和底层设备工具跑通，因此下一步重点是选择自动化测试框架作为 UI 操作与 UI 状态获取后端。

候选方案主要是：

| 方案 | Android | iOS |
| --- | --- | --- |
| 原生框架直连 | `adb + UiAutomator` | `simctl + XCUITest / WDA` |
| Appium | Appium UiAutomator2 driver | Appium XCUITest driver / WDA |

## 为什么 Appium 看起来和原生框架一样

Appium 的底层确实也依赖平台原生自动化能力：Android 侧通常使用 UiAutomator2 driver，iOS 侧通常使用 XCUITest driver 和 WebDriverAgent。因此，从“最终触达 App 的平台能力”看，Appium 和原生框架有重叠。

但 Appium 不是原生框架本身，而是在原生能力之上增加了一层跨端 WebDriver 协议和服务端封装。

```text
Appium Android
AI / Test Client
  ↓ WebDriver HTTP
Appium Server
  ↓ Appium UiAutomator2 Driver
UiAutomator2 Server on device
  ↓
Android UiAutomator / Accessibility / Instrumentation
  ↓
App
```

```text
原生 Android
AI / Test Tool
  ↓ 直接调用
adb / instrumentation / UiAutomator test / uiautomator dump
  ↓
Android UiAutomator / Accessibility
  ↓
App
```

iOS 类似：

```text
Appium iOS
AI / Test Client
  ↓ WebDriver HTTP
Appium Server
  ↓ Appium XCUITest Driver
WebDriverAgent
  ↓ XCTest / Accessibility
  ↓
App
```

```text
原生 iOS
AI / Test Tool
  ↓ 直接调用
xcodebuild test / XCTest / WDA 自行管理
  ↓
XCTest / Accessibility
  ↓
App
```

因此，Appium 和原生框架的关系更准确地说是：

```text
Appium = 跨端 WebDriver 协议层 + Appium Server + 平台 Driver + 原生自动化能力
原生框架 = 直接使用平台自己的测试框架 / 工具链
```

## 核心差异

| 维度 | 原生框架直连 | Appium |
| --- | --- | --- |
| 底层能力 | Android `UiAutomator`，iOS `XCUITest/WDA` | Android 也用 `UiAutomator2`，iOS 也用 `XCUITest/WDA` |
| 抽象层 | 需要自己封装 | Appium 已封装成 WebDriver 协议 |
| 跨端统一 | 需要自己做 | 天然更统一 |
| 调用方式 | Android/iOS 各自不同 | HTTP/WebDriver API 一套 |
| 环境复杂度 | Android/iOS 分开配置 | Appium Server + driver + capabilities |
| 可控性 | 高 | 中等，受 Appium 抽象限制 |
| 定制能力 | 高 | 中等，复杂能力可能要绕 Appium |
| 启动速度 | 通常更快 | 通常更慢一层 |
| 调试复杂度 | 平台内问题更直接 | 多一层 Appium，需要排查 server/driver/session |
| 云真机兼容 | 需要适配不同平台 | 好，BrowserStack/Sauce/LambdaTest 等平台支持成熟 |
| AI 工具封装 | 需要自己做 `MobileDriver` | WebDriver API 接近通用工具接口 |
| 长期平台能力 | 强 | 强，但可能受 Appium 版本和 driver 影响 |
| MVP 成本 | 中到高 | 中 |
| 技术债 | 自己维护抽象 | 依赖 Appium 生态和协议 |

## Appium 的价值

Appium 的核心价值不是替代 UiAutomator/XCUITest，而是把不同平台的原生自动化能力包装成统一协议。

例如同一个操作：

```text
tap login_button
```

原生直连需要分别实现 Android 和 iOS：

```kotlin
device.findObject(By.res("login_button")).click()
```

```swift
app.buttons["login_button"].tap()
```

Appium 则可以统一到 WebDriver 风格接口：

```ts
const el = await driver.$("id=login_button")
await el.click()
```

Appium 帮助处理：

| 能力 | 说明 |
| --- | --- |
| session 管理 | 创建和维护 Android/iOS 自动化会话 |
| driver 管理 | 选择 UiAutomator2、XCUITest 等平台 driver |
| element 查找 | 提供统一的元素查找协议 |
| 操作封装 | click、input、swipe、wait 等能力统一调用 |
| 平台差异屏蔽 | 屏蔽一部分 Android/iOS 差异 |
| 云真机兼容 | 与主流云真机平台兼容度高 |

代价是链路更长：

```text
AI Tool
→ Appium Client
→ Appium Server
→ Appium Driver
→ UiAutomator2 / WDA
→ App
```

排查问题时需要判断问题来自 App、selector、Appium client、Appium server、driver、WDA、Xcode signing 或设备连接。

## 原生框架的价值

原生框架直连的价值是可控、直接、性能好，更容易按 AI 测试闭环的需求定制。

Android 可以直接使用：

```text
adb
UiAutomator
uiautomator dump
instrumentation test
Compose testTag
logcat
```

iOS 可以直接使用：

```text
xcodebuild test
XCUITest
WebDriverAgent 自管理
simctl
xcrun
```

原生方案可以从第一天就输出我们想要的结构：

```ts
getSnapshot(): 返回压缩后的 JSON，而不是冗长 XML
tap(): 只支持稳定 selector
waitFor(): 按我们的策略等待
collectEvidence(): 按我们的报告格式收集
```

代价是 MVP 阶段需要同时建设 AndroidBackend 和 IOSBackend，并自行统一 selector、snapshot、wait、assert、evidence、report 等抽象。

## 从 MVP 需求评估

| 需求 | 原生框架 | Appium | 判断 |
| --- | --- | --- | --- |
| Android + iOS P0 覆盖 | 要写两套 | 一套协议 | Appium 更快 |
| 获取 UI tree | 强，可定制 | 强，但通常先拿 XML/page source | 接近，原生更可控 |
| 压缩 UI Snapshot | 可直接输出 JSON | 需要从 XML/page source 转 JSON | 原生略胜 |
| selector 操作 | 强，但两端分别实现 | 强，跨端统一 | Appium 略胜 |
| 等待元素 | 强，可定制 | 成熟 | 接近 |
| 失败截图 | 强 | 强 | 接近 |
| 日志采集 | 强，可定制 | 一般仍要自己补 | 原生胜 |
| 执行速度 | 通常更快 | 通常更慢 | 原生胜 |
| 环境搭建 | 两端分别配置 | Appium 一套但依赖多 | 取决于团队经验 |
| 调试问题 | 平台问题直达 | 多一层 Appium | 原生胜 |
| 云真机扩展 | 后续要适配 | 天然支持 | Appium 胜 |
| AI 工具封装 | 要自己封 | WebDriver 接近现成 | Appium 胜 |
| 长期深度能力 | 最强 | 受抽象限制 | 原生胜 |
| MVP 速度 | 中 | 快 | Appium 胜 |

## 推荐选择

如果只在原生框架和 Appium 之间选择一个做 MVP，建议：

```text
MVP 选 Appium，但必须包一层 MobileDriver，不要让业务逻辑直接依赖 Appium。
```

推荐架构：

```text
AI Agent
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

保留未来替换空间：

```text
MobileDriver API
  ↓
NativeAndroidDriver = adb + UiAutomator
NativeIOSDriver = simctl + XCUITest/WDA
```

推荐理由：

| 理由 | 说明 |
| --- | --- |
| P0 要同时覆盖 Android + iOS | Appium 能更快形成统一跨端接口 |
| 当前重点是验证 AI 测试闭环 | 不应在 MVP 阶段先投入大量精力造双端自动化框架 |
| Appium 已提供标准 action API | 方便封装 `tap`、`input`、`waitFor`、`assert` 等 AI tools |
| 后续云真机可能性更好 | Appium 与云真机平台兼容成熟 |
| MobileDriver 可以隔离技术债 | 后续可替换为原生后端 |

## Appium MVP 的退出条件

MVP 期间不能盲目信任 Appium，需要设置明确门槛。如果以下条件不满足，应考虑切换或补充原生后端。

| 条件 | 目标 |
| --- | --- |
| session 创建时间 | Android 和 iOS session 创建时间可接受 |
| page source 稳定性 | `getPageSource` 能稳定获取核心元素 |
| snapshot 压缩 | page source 可压缩成 2KB-10KB 的 AI Snapshot |
| selector 稳定性 | id / accessibilityIdentifier 操作稳定 |
| 失败证据 | 能收集截图、page source、日志、执行步骤 |
| 重复执行稳定性 | 同一条路径连续跑 5 次，至少 4 次稳定通过 |
| 错误可诊断 | Appium 错误不会吞掉真实 App 问题 |

## Appium 控制原理：为什么 App 不需要内嵌 SDK

Appium 是**从外部注入**自动化能力，不是 App 内嵌的测试 SDK。这是它与 Firebase Test Lab、Sentry 等"需内嵌 SDK"方案的根本区别。

### Android 控制原理

Appium 通过 UiAutomator2 driver 在 App 进程中注入 instrumentation：

```text
Appium Server → 启动 UiAutomator2 Server APK → 注入到目标 App 进程
  → 通过 Accessibility Service 读取 UI 树
  → 通过 instrumentation 执行点击/输入等操作
```

前提条件：
- **debug 包**（`android:debuggable=true`）：✅ 可直接注入
- **release 包**（`debuggable=false`）：❌ 无法注入，Appium 控制不了
- 真机需开启 **USB 调试**；模拟器自带

### iOS 控制原理

Appium 通过 WebDriverAgent（WDA）作为中间代理：

```text
Appium Server → 启动 WDA（独立进程）→ 通过 XCTest 框架
  → 通过 Accessibility 读取 UI 树
  → 通过 XCUITest API 执行点击/输入等操作
```

前提条件：
- **模拟器 + debug 包**：✅ 可直接控制，WDA 自动安装
- **真机**：⚠️ 需要签名 WebDriverAgent（需 Apple Developer 账号），手动将 WDA 安装到设备
- 真机还需 **Settings → Developer → 开启 UI Automation**

### 跨平台对比

| 场景 | Android | iOS |
|------|---------|-----|
| debug 包 + 模拟器 | ✅ 直接控制 | ✅ 直接控制 |
| debug 包 + 真机 | ✅ 开启 USB 调试即可 | ⚠️ 需签名 WDA |
| release 包 | ❌ 无法控制 | ❌ 无法控制 |

### App 侧唯一前置条件：testTag

App 不需要引入任何额外 SDK，但需要**业务代码主动标注** `attr.testTag("xxx")`：

- 没有 testTag：AI 只能靠文本/坐标定位，不稳定（文本可能重复、坐标依赖布局）
- 有 testTag：AI 可通过 `accessibilityId` 精确定位，可靠且跨端统一

testTag 是 KuiklyUI 框架内置功能，零侵入，只是将 `accessibilityIdentifier`（iOS）/ `viewIdResourceName`（Android）设置上去。

## 完整环境依赖清单

### PC 层面

| 依赖 | 安装方式 | 说明 |
|------|---------|------|
| **Node.js** >= 18 | `brew install node` | 运行时 |
| **Appium** 3.x | `npm install -g appium` | 自动化框架服务端 |
| **appium-uiautomator2-driver** | `appium driver install uiautomator2` | Android 驱动 |
| **appium-xcuitest-driver** | `appium driver install xcuitest` | iOS 驱动 |
| **Xcode** | Mac App Store | iOS 编译 + 模拟器 + WDA 签名 |
| **Android SDK** | Android Studio 或 cmdline-tools | Android 编译 + 模拟器 |
| **Carthage** | `brew install carthage` | xcuitest driver 依赖 |
| **mobile-test 工具包** | `npm install`（`.claude/skills/kuikly-mobile-test/`） | webdriverio + vitest + tsx |

### 设备层面

| 平台 | 需要 |
|------|------|
| iOS | 模拟器（Xcode 创建）或真机（需签名 WDA + 开启 UI Automation） |
| Android | 模拟器（AVD）或真机（需开启 USB 调试） |

### App 层面

| 依赖 | 说明 | 必须？ |
|------|------|--------|
| debug 构建 | release 包无法被 Appium 注入 | P0 |
| testTag 标注 | 业务代码中 `attr.testTag("xxx")` | P0，否则 AI 无法精确定位 |

### 一键安装速查

```bash
# 1. 全局安装 Appium + 驱动
npm install -g appium
appium driver install uiautomator2
appium driver install xcuitest

# 2. 启动 Appium Server
appium

# 3. 安装工具包依赖
cd .claude/skills/kuikly-mobile-test && npm install

# 4. 运行测试
npx tsx scenarios/ios-e2e.ts
```

## 与 kuikly-app-runner 的关系

`kuikly-app-runner` 已经覆盖编译、安装、启动、日志采集等能力。Appium 不需要替代它，而是补齐 UI 自动化操作和 UI 状态获取。

两者可以按职责组合：

| 职责 | 推荐承担方 |
| --- | --- |
| 编译 Android/iOS App | `kuikly-app-runner` |
| 安装 App | `kuikly-app-runner` 或 Appium capability，MVP 建议沿用 runner |
| 启动 App | `kuikly-app-runner` 或 Appium session，MVP 可先由 Appium 拉起，失败时沿用 runner |
| 日志采集 | `kuikly-app-runner` |
| 获取 UI tree / page source | Appium |
| selector 点击 / 输入 / 等待 | Appium |
| 失败截图 | Appium 或设备工具，MVP 两者择一即可 |
| 验证报告 | MobileDriver / AI 测试层汇总 |

推荐组合方式：

```text
kuikly-app-runner: 负责 build / install / launch / logs
AppiumMobileDriver: 负责 page source / tap / input / wait / assert / screenshot
AI Test Runner: 编排两者，产出验证报告
```

这样可以复用已有能力，避免 Appium 负责过多设备生命周期工作，同时保留 Appium 的跨端 UI 自动化价值。

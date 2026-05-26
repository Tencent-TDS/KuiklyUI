# AI移动端测试验证闭环方案

本文档用于沉淀 AI 在移动端需求开发、Bug 跟进中的测试验证闭环思路。当前阶段优先讨论 Android + iOS 的正常路径端到端验证，避免过早引入 mock 登录、mock 接口、网络抓包、需求/Bug 平台集成等后续能力。

## 目标

AI 测试验证闭环的目标不是让 AI 每一步都看截图并猜测操作位置，而是让 AI 通过结构化 UI 状态理解 App 当前页面，再调用稳定工具执行真实用户路径。

核心目标如下：

| 目标 | 说明 |
| --- | --- |
| 端到端真实路径 | 优先验证真实 App、真实页面、真实交互链路 |
| 少依赖截图 | 正常路径使用 UI tree、accessibility tree、selector 作为主输入 |
| 可自动执行 | AI 可以完成编译、安装、启动、操作、断言、报告 |
| 可失败定位 | 失败时自动保存截图、UI tree、日志、执行步骤 |
| 可分阶段建设 | P0 先闭环，P1 增强语义，P2 再做 mock、网络、数据构造等能力，P3 再做视觉对比 |

## 非目标

第一阶段不优先解决以下问题：

| 非目标 | 原因 |
| --- | --- |
| mock 登录 | 属于提升效率和构造场景的能力，不是端到端主路径必需项 |
| mock 接口 | 更适合异常、边界、稳定性用例，放到 P2 |
| 网络请求分析 | 失败诊断有价值，但 P0 可以先依赖 UI 断言和日志 |
| 埋点校验 | 属于专项质量验证，非 P0 主链路 |
| 性能分析 | 属于独立专项能力，非端到端功能验证主链路 |
| 每步截图识别 | token 成本高，稳定性差，P0 只保留截图作为人工查看证据 |
| 视觉稿对比 | 需要截图和视觉算法，作为 P3 独立能力 |
| 需求 / Bug 平台集成 | 当前先不接入 TAPD / Jira / GitHub Issue，避免扩大 P0 范围 |

## 总体思路

推荐采用分层架构：

```text
AI 测试代理
  ↓
统一 MobileDriver API
  ↓
平台执行后端：Android UiAutomator / iOS XCUITest 或 WebDriverAgent
  ↓
设备控制层：adb / xcrun simctl
  ↓
App under test
```

AI 的职责是理解任务、规划路径、选择操作、判断结果、生成报告。底层工具的职责是稳定执行动作、采集 UI 状态、采集失败证据。

关键原则：

| 原则 | 说明 |
| --- | --- |
| AI 读语义，不读像素 | UI tree / accessibility tree 是主输入，P0 不让 AI 读取截图 |
| AI 发意图，不发坐标 | 优先 `tap(id=login_button)`，避免 `tap(124, 650)` |
| 工具负责确定性执行 | 等待、点击、输入、断言应由工具稳定实现 |
| 失败时再加证据 | 失败时保存截图、日志、UI tree，截图给人工查看，不作为 AI 主输入 |
| 测试标识前置 | 关键元素必须有稳定 `testID`、`accessibilityIdentifier`、`resource-id` 或同等标识 |

## 工具能力对比

移动端 AI 测试闭环涉及多类工具。它们不是互斥关系，而是分层互补关系。

## 业界探索

移动端 AI 测试闭环的诉求在业界已经比较明确，但现有方案大多分布在“通用 GUI Agent”和“测试平台 AI 化”两条线上，还没有完全等价于本文档讨论的研发闭环方案。

### 通用 GUI Agent

| 方案 | 方向 | 关键思路 | 对本文档的启发 |
| --- | --- | --- | --- |
| DroidBot-GPT | Android UI 自动化 | 将 App GUI 状态和可用动作转成 prompt，让 LLM 决策下一步动作 | 证明“结构化 UI 状态 + action space + LLM 决策”路线可行 |
| AppAgent | 手机多模态 Agent | 基于截图、ADB、多模态模型，让 Agent 像用户一样操作手机 | 纯视觉路线通用，但 token 成本和稳定性不适合作为 P0 主路径 |
| Mobile-Agent / GUI-Owl | 跨平台 GUI Agent | 面向手机、桌面、浏览器的视觉感知、grounding、多 Agent 协作、反思和记忆 | GUI grounding 是核心难点，工程场景应尽量利用 UI tree 降低难度 |
| SeeClick | 视觉 GUI Agent | 只依赖截图做 GUI automation，并强化 GUI grounding 能力 | 适合无 UI tree、无源码场景；本文档将视觉能力放到 P3 |
| AndroidWorld | Android Agent Benchmark | 在真实 Android emulator 中提供任务、环境、UI elements、screenshot 和 action API | 可借鉴 environment / task / agent 抽象，但它偏 benchmark，不是研发验证闭环 |

这类方案的共同特点是通用性强，尤其适合没有 App 源码、没有测试标识、没有业务接入能力的开放场景。但它们往往依赖截图、视觉模型或通用 grounding 能力，稳定性、成本和可重复性不一定满足研发流程里的 P0 验证闭环。

### 测试平台 AI 化

| 方案 | 方向 | 关键能力 | 对本文档的启发 |
| --- | --- | --- | --- |
| Appium | 跨端 UI 自动化框架 | 支持 iOS / Android 等多平台 UI automation，生态成熟 | 可作为 MobileDriver 的跨端执行后端 |
| Maestro | 轻量移动端 UI 自动化 | 使用 YAML Flow 描述 Android / iOS 用户路径 | 适合 P0 快速验证路径和 AI 生成可读测试脚本 |
| BrowserStack App Automate | 云真机移动测试平台 | 支持 Appium、Espresso、XCUITest、Maestro，并提供 AI 失败分析、自愈、测试选择等能力 | 说明“自动化测试 + AI 增强”是主流商业方向 |
| testRigor | 自然语言测试自动化 | 用 plain English 描述测试，系统转成可执行步骤 | 证明自然语言测试有需求，但底层执行和数据可控性有限 |

这类方案更接近工程测试体系，但通常重点在测试编写、执行、云真机、失败分析、locator 自愈和测试维护，不一定覆盖“AI 根据需求开发 / Bug 修复自动完成验证”的完整闭环。

### 本方案的定位

本文档选择一条更适合研发闭环的工程化路径：

```text
P0：自动化测试框架 + 结构化 UI Snapshot + selector 操作 + 失败截图证据
P1：轻量 SDK 增强页面语义
P2：mock / 网络 / 数据构造 / feature flag
P3：截图视觉理解和设计稿对比
```

选择该路径的原因：

| 原因 | 说明 |
| --- | --- |
| 有 App 工程和测试包 | 不需要像通用 Agent 一样完全依赖截图 |
| 可以补测试标识 | 关键元素可通过 id / accessibilityIdentifier / testTag 稳定定位 |
| 需要可重复验证 | 自动化测试框架比纯视觉 Agent 更适合回归闭环 |
| 需要低 token 成本 | UI tree / accessibility tree 比截图更适合作为 AI 主输入 |
| 需要失败证据 | 截图仍保留，但 P0 给人工查看，不让 AI 每步读取 |

因此，通用 GUI Agent 的探索可以作为 P3 视觉能力和长期 Agent 能力参考；P0 应优先建设自动化测试框架驱动的结构化验证闭环。

| 工具类型 | 代表工具 | 核心作用 |
| --- | --- | --- |
| 设备控制工具 | Android `adb`、iOS `xcrun simctl` | 编译后安装、启动、停止、截图、日志、设备控制 |
| 系统 UI 自动化 | `UiAutomator`、`XCUITest`、`WebDriverAgent` | 获取 UI 树、按元素操作、断言 |
| 跨端自动化框架 | `Appium`、`Maestro`、Detox、Airtest | 封装多端操作，统一测试脚本 |
| App 内置 Test SDK | 自研 debug/test SDK | P1/P2 暴露更语义化的页面和组件状态 |
| 视觉能力 | 截图、OCR、VLM | P0 失败留证，P3 设计稿对比 |

### 能力总表

| 能力 | ADB / Simctl | 系统 UI 自动化 | 跨端自动化框架 | App 内置 SDK | 截图 / OCR / VLM |
| --- | --- | --- | --- | --- | --- |
| 编译 App | 不负责，通常靠 Gradle/Xcode | 不负责 | 不负责 | 不负责 | 不负责 |
| 安装 App | 强 | 中 | 强 | 不负责 | 不负责 |
| 启动 App | 强 | 强 | 强 | 不负责 | 不负责 |
| 关闭 / 重启 App | 强 | 强 | 强 | 可辅助 | 不负责 |
| 清空 App 数据 | Android 强，iOS 视情况 | 一般 | 一般 | 可增强 | 不负责 |
| 授权权限 | Android 强，iOS 部分支持 | 中 | 中 | 可辅助 | 不负责 |
| 点击坐标 | Android 强，simctl 不适合做手势操作 | 强 | 强 | 不建议 | 可辅助定位 |
| 输入文本 | Android 强，simctl 不适合做主输入通道 | 强 | 强 | 不建议 | 不负责 |
| 滑动 / 返回 / Home | Android 强，simctl 不适合做手势操作 | 强 | 强 | 不建议 | 不负责 |
| 获取截图 | 强 | 强 | 强 | 可辅助 | 强 |
| 获取系统日志 | 强 | 中 | 中 | 可补充 App 内日志 | 不负责 |
| 获取崩溃日志 | 中 | 中 | 中 | 强 | 不负责 |
| 获取 UI 树 | Android 可用，iOS 弱 | 强 | 强 | 可自定义 | 弱 |
| 获取元素坐标 | Android 可用 | 强 | 强 | 强 | 视觉推断 |
| 获取元素文本 | Android 可用 | 强 | 强 | 强 | OCR 推断 |
| 获取元素 id | Android 可用 | 强 | 强 | 强 | 不稳定 |
| 获取元素状态 | Android 可用 | 强 | 强 | 强 | 弱 |
| 按 selector 点击 | 弱到中 | 强 | 强 | 可辅助 | 弱 |
| 按 text 点击 | Android 可用 | 强 | 强 | 可辅助 | OCR 兜底 |
| 等待元素出现 | 弱 | 强 | 强 | 强 | 弱 |
| 断言文案存在 | 弱到中 | 强 | 强 | 强 | OCR 兜底 |
| 断言按钮可点击 | 弱到中 | 强 | 强 | 强 | 弱 |
| 判断当前页面 | 弱 | 中 | 中 | 强 | 弱 |
| 判断 loading 状态 | 弱 | 中 | 中 | 强 | P1，不作为 P0 关键能力 |
| 判断 toast | 弱到中 | 中 | 中 | 强 | P1，不作为 P0 关键能力 |
| 判断弹窗 | 中 | 强 | 强 | 强 | P1，不作为 P0 关键能力 |
| 获取导航栈 / route | 不支持 | 弱 | 弱 | 强 | P1，不作为 P0 关键能力 |
| 获取业务状态 | 不支持 | 不支持 | 不支持 | 强 | 不支持 |
| 获取网络请求 | 可抓但重 | 不支持 | 部分支持 | 强 | 不支持 |
| mock 登录 | 不支持 | 不支持 | 不支持 | 强 | 不支持 |
| mock 接口 | 不支持 | 不支持 | 不支持 | 强 | 不支持 |
| 深链跳转 | 强 | 中 | 强 | 强 | 不负责 |
| 失败现场证据 | 强 | 强 | 强 | 强 | 强 |
| 跨端统一 | 弱 | 弱 | 强 | 需要自研协议 | 中 |
| token 成本 | 低 | 低 | 低 | 低 | 高 |
| 接入成本 | 低 | 中 | 中 | 中到高 | 低 |

## 设备控制工具

设备控制工具适合作为底层能力，不适合作为完整测试框架。

P0 的取舍是：编译、安装、启动、停止、日志、截图等设备侧能力优先使用 `adb` 和 `xcrun simctl`。Android 可以用 `adb input` 做点击、输入、滑动等兜底操作；iOS 的 `simctl` 更偏设备和模拟器管理，不适合作为点击、滑动、手势的主操作通道，iOS 操作应交给 `XCUITest` 或 `WebDriverAgent`。

### Android ADB

| 能力 | 说明 | P0 是否需要 |
| --- | --- | --- |
| 安装 App | `adb install` | 是 |
| 启动 Activity | `am start` | 是 |
| 停止 App | `am force-stop` | 是 |
| 清理数据 | `pm clear` | 可选 |
| 点击坐标 | `input tap x y` | 兜底，不作为主路径 |
| 输入文本 | `input text` | 可用，但中文和特殊字符处理复杂 |
| 滑动 | `input swipe` | 可用 |
| 返回键 | `input keyevent BACK` | 是 |
| 截图 | `screencap` | 失败留证和视觉兜底 |
| 录屏 | `screenrecord` | 失败分析可选 |
| 日志 | `logcat` | 是 |
| UI dump | `uiautomator dump` | 是，P0 规避截图的关键能力 |
| 当前 Activity | `dumpsys activity` | 可选 |
| 窗口信息 | `dumpsys window` | 可选 |
| 授权权限 | `pm grant` | 可选 |
| 端口转发 | `adb forward` | P1/P2 给 SDK bridge 使用 |

`adb + uiautomator dump` 可以在 Android 上拿到结构化 XML，例如：

```xml
<node
  text="登录"
  resource-id="com.demo:id/login_button"
  class="android.widget.Button"
  enabled="true"
  clickable="true"
  bounds="[32,600][343,660]" />
```

AI 可以读取该结构化信息判断按钮是否存在、是否可点击，而不需要看截图。

### iOS Simctl

| 能力 | 说明 | P0 是否需要 |
| --- | --- | --- |
| 安装 App | `xcrun simctl install` | 是 |
| 启动 App | `xcrun simctl launch` | 是 |
| 关闭 App | `xcrun simctl terminate` | 是 |
| 截图 | `xcrun simctl io screenshot` | 失败留证 |
| 录屏 | `xcrun simctl io recordVideo` | 失败分析可选 |
| 日志 | `log stream` 或 Xcode 日志能力 | 是 |
| 权限控制 | 部分权限可控制 | 可选 |
| UI tree | 不适合作为主来源 | 否，依赖 XCUITest / WDA |
| 点击 / 滑动 / 手势 | 不适合作为主能力 | 否，依赖 XCUITest / WDA |

iOS 的结构化 UI 状态通常应来自 `XCUITest` 或 `WebDriverAgent`，而不是 `simctl` 本身。

### 鸿蒙 HDC

P0 暂不覆盖鸿蒙。后续覆盖鸿蒙时，`hdc` 的定位与 `adb` / `simctl` 类似，主要承担设备控制、安装、启动、截图、日志等底层能力；UI tree、按元素操作和断言仍应优先依赖鸿蒙 UI Test 或 ArkUI 测试能力。

## 系统 UI 自动化

系统 UI 自动化是 P0 最值得优先依赖的执行层。

| 平台 | 推荐能力 | 主要价值 |
| --- | --- | --- |
| Android | `UiAutomator`、Compose UI Test、Accessibility | 获取 UI tree、按 id/text/class 操作、断言 |
| iOS | `XCUITest`、`WebDriverAgent` | 获取 accessibility tree、按 identifier/text/type 操作、断言 |

系统 UI 自动化可以支持：

| 能力 | 价值 |
| --- | --- |
| 获取当前页面可见元素 | 替代截图作为主输入 |
| 获取元素文本 | AI 判断页面内容 |
| 获取元素 id / test id | 稳定定位元素 |
| 获取 bounds | 必要时转坐标点击 |
| 获取 enabled / clickable / selected | 做状态判断 |
| 按 selector 点击 | 避免坐标点击 |
| 输入文本 | 真实端到端输入 |
| 等待元素出现 | 避免不稳定 sleep |
| 断言元素存在 | 验证核心结果 |
| 断言文本变化 | 验证业务结果 |
| 处理弹窗 | 权限弹窗、系统弹窗、业务弹窗 |
| 获取失败时 UI tree | 失败定位 |

主要风险：

| 风险 | 处理方式 |
| --- | --- |
| 页面语义弱 | 补充稳定测试标识 |
| 不知道业务页面名 | P1 引入轻量 Test SDK 暴露 screen / route |
| loading 判断不准 | P0 先等关键元素，P1 再暴露 loading 状态 |
| toast 短暂难捕获 | P1 由 SDK 暴露 toast 状态或事件 |
| WebView / Canvas / 自绘 UI 难处理 | 截图、OCR、SDK 兜底 |
| 多端 API 不一致 | 统一封装 MobileDriver |

### 启动方式与正常 Run 的差异

使用自动化测试框架时，App 的启动方式可能和开发者在 IDE 中点 Run 有差异，需要在设计时明确。

| 平台 | 启动方式 | 与正常 Run 的差异 | P0 建议 |
| --- | --- | --- | --- |
| Android + `UiAutomator` | 先安装正常 App，再由测试进程通过 instrumentation / shell 启动和操作目标 App | 目标 App 本身可以是正常 debug 包；额外运行一个测试 APK 或测试进程 | 接受该差异，目标 App 尽量保持正常运行方式 |
| Android + `adb uiautomator dump` | App 正常启动后，通过 adb dump 当前 UI 树 | 与正常 Run 接近，但操作和等待能力较弱 | 可作为早期低成本方案或兜底 |
| iOS + `XCUITest` | Xcode test runner 启动目标 App，测试进程通过 accessibility 操作 App | 和手工 Run 不完全相同，会有 XCTest runner 参与 | P0 可以接受，重点验证目标 App 真实 UI 路径 |
| iOS + `WebDriverAgent` | WDA 作为自动化服务，通过 XCTest/accessibility 操作已启动或被拉起的 App | 需要额外 WDA 服务，环境更重 | 如果希望更像工具服务化，可考虑 WDA |

因此，自动化测试框架不是纯粹的“正常 Run 后旁路读取状态”。它通常会引入测试 runner 或自动化服务。但只要目标 App 使用正常 debug/release-like 包、操作路径走真实 UI、断言来自系统 UI 树，仍然可以满足 P0 端到端验证目标。

如果强要求“App 完全按正常 Run 启动”，Android 可以先用 `adb` 启动 App，再用 `uiautomator dump` 获取 UI tree，并用 `adb input` 兜底操作；但 iOS 仍需要 `XCUITest` / WDA 这类能力才能稳定按元素操作和获取 accessibility tree。

## 跨端自动化框架

跨端框架可以减少多平台封装成本，但也会引入框架自身复杂度。

| 工具 | 优点 | 缺点 | 建议 |
| --- | --- | --- | --- |
| Appium | 跨端成熟，支持 iOS/Android，生态大 | 重、慢、环境复杂 | 适合平台化或需要统一协议时使用 |
| Maestro | 脚本简单，适合冒烟和端到端路径 | 深度能力不如原生框架 | 适合 P0 快速验证想法 |
| Detox | React Native 生态强，灰盒能力好 | 技术栈限制明显 | 仅 RN 项目优先考虑 |
| Airtest | 图像识别强，脚本直观 | 容易依赖截图 | 不建议作为主路径 |
| 原生封装 | 稳定、能力强、可控 | 跨端封装成本高 | 长期推荐 |

短期可以用跨端框架快速验证闭环，中长期建议抽象自己的 `MobileDriver`，底层后端可替换。

### 自动化框架选型建议

下一步的关键是确定 P0 的执行后端。结合本文档的需求，选型应从以下目标出发：

| 需求 | 说明 |
| --- | --- |
| 覆盖 Android + iOS | P0 已明确先做 Android 和 iOS |
| 正常端到端路径 | 优先真实 App、真实 UI、真实用户操作 |
| 结构化 UI 状态 | 能拿到 UI tree / accessibility tree，避免 AI 每步读截图 |
| selector 操作 | 支持按 id / text / accessibilityIdentifier 等稳定选择器操作 |
| 失败证据 | 能保存截图、日志、执行步骤、UI snapshot |
| AI 友好 | 容易封装成 `getSnapshot`、`tap`、`input`、`waitFor`、`assert` 等工具 |
| 接入成本低 | P0 不应要求大量 App 侧 SDK 改造 |
| 可演进 | 后续能切换到底层原生框架、云真机或 SDK 增强 |

候选方案如下：

| 方案 | Android | iOS | 优点 | 风险 | 建议 |
| --- | --- | --- | --- | --- | --- |
| 原生框架直连 | `adb + UiAutomator` | `simctl + XCUITest / WDA` | 能力最贴近平台，UI tree 和 selector 控制最强，可控性最好 | P0 要分别封装两套后端，工程成本较高 | 长期推荐，适合作为最终能力底座 |
| Appium | UiAutomator2 driver | XCUITest driver / WDA | 跨端协议统一，生态成熟，云真机支持好，AI 工具封装方便 | 环境重，执行链路长，稳定性和速度需要治理 | 如果希望 P0 直接跨端统一，优先评估 |
| Maestro | Android 支持 | iOS 支持 | 上手快，YAML flow 可读，AI 生成脚本友好，适合冒烟路径 | 深度 UI snapshot、复杂断言、调试可控性可能不足 | 适合 P0 原型和路径验证，不建议直接锁死为唯一底座 |
| Espresso + XCUITest | Espresso | XCUITest | App 内测试稳定，等待机制强 | Android Espresso 偏 App 内白盒，不适合跨 App 和系统弹窗；与 iOS 抽象不一致 | 不作为 P0 统一方案，可作为专项补充 |
| 纯 ADB / simctl | ADB 强 | simctl 弱 | 接入低，设备控制简单 | iOS 无法稳定做手势和 UI tree；Android 等待和 selector 能力弱 | 只做设备层，不作为自动化框架主方案 |

推荐路径：

```text
P0 原型：优先评估 Appium 与 Maestro
P0 工程化：抽象 MobileDriver，底层先接 Appium 或原生框架
长期：MobileDriver 后端可替换为 Android UiAutomator + iOS XCUITest/WDA
```

如果更关注“尽快跑通 AI 测试闭环”，建议先用 `Maestro` 或 `Appium` 做 Spike：

| Spike 目标 | 说明 |
| --- | --- |
| 能否启动 Android + iOS App | 验证设备层和框架环境 |
| 能否获取结构化元素信息 | 验证是否满足 AI 低 token 输入 |
| 能否按稳定 selector 操作 | 验证不依赖坐标和截图 |
| 能否输出失败证据 | 验证截图、日志、步骤记录 |
| 能否封装为统一工具 | 验证是否适合接入 AI agent loop |

如果更关注“长期可控性和框架能力”，建议直接建设 `MobileDriver`，并将底层后端拆成：

```text
AndroidBackend = adb + UiAutomator
IOSBackend = simctl + XCUITest 或 WDA
```

### 当前倾向

从本文档的需求出发，当前倾向是：

```text
第一选择：MobileDriver 抽象 + Appium 后端做 P0 跨端验证
第二选择：Maestro 做快速原型，验证 AI 生成路径和报告闭环
长期底座：逐步替换或补充为原生 UiAutomator + XCUITest/WDA 后端
```

理由：

| 理由 | 说明 |
| --- | --- |
| P0 同时覆盖 Android + iOS | Appium 比原生直连更快形成统一跨端接口 |
| 需要 UI tree 和 selector | Appium 基于 WebDriver / 平台 driver，适合获取元素和执行 selector 操作 |
| 不想让 AI 读截图 | Appium / 原生框架比 Maestro 更适合暴露结构化状态 |
| 需要后续云真机可能性 | Appium 与 BrowserStack 等平台兼容性好 |
| 需要避免过早绑定 | MobileDriver 抽象可以让后续替换底层实现 |

但如果一周内要做出可演示闭环，Maestro 的投入产出比可能更高。它适合先回答：AI 能否从任务描述生成可读测试步骤，并在 Android / iOS 上跑通主路径。只是如果后续要做更细的 UI snapshot、失败定位和复杂断言，仍可能需要回到 Appium 或原生后端。

## App 内置 Test SDK

App 内置 SDK 不建议在 P0 做得太大。它的主要价值是补齐 UI 自动化拿不到的语义信息，而不是一开始就做 mock。

P0 优先使用系统 UI 自动化框架获取信息，原因是它更接近黑盒端到端验证：App 走真实 UI，AI 从系统可观测的 UI tree / accessibility tree 判断页面状态。内置 SDK 放到 P1 之后再引入，用于增强页面语义，而不是替代自动化框架。

P0 选择自动化测试框架的理由：

| 理由 | 说明 |
| --- | --- |
| 更接近真实用户路径 | 操作和断言来自系统 UI 层，而不是业务内部状态 |
| App 改造少 | 优先依赖已有 accessibility / UI tree 能力 |
| 可跨项目复用 | Driver 和 snapshot schema 可以沉淀为通用工具 |
| 避免过早白盒化 | 不需要一开始暴露业务状态、网络、mock 等能力 |

SDK 延后到 P1 的理由：

| 理由 | 说明 |
| --- | --- |
| P0 不需要复杂语义 | loading、toast、弹窗、导航栈先不作为关键判断 |
| 降低接入成本 | 避免第一阶段要求业务侧接入较多代码 |
| 保持端到端属性 | 防止测试过早依赖内部状态而偏离真实用户行为 |

### P1 可选能力

| 能力 | 必要性 | 价值 |
| --- | --- | --- |
| 当前页面名 | 高 | AI 更容易判断是否跳转成功 |
| 当前路由 | 中 | 对路由型 App 有价值 |
| loading 状态 | 中 | 减少等待不稳定 |
| toast / dialog 状态 | 中 | 避免漏判短暂提示 |
| 可见组件摘要 | 中 | 补充 UI tree 语义不足 |

P1 的 SDK 输出应保持极简，例如：

```json
{
  "screen": "LoginPage",
  "route": "/login",
  "loading": false,
  "dialog": null,
  "toast": null
}
```

### P1 能力

| 能力 | 价值 |
| --- | --- |
| 页面生命周期事件 | 判断页面是否稳定 |
| 关键组件状态 | 比 UI tree 更准确 |
| 测试事件流 | 记录用户路径 |
| 深链入口 | 提升测试效率 |
| App 内错误日志 | 提升失败定位 |
| loading 状态 | 减少等待不稳定，P1 再作为显式观测能力 |
| toast / dialog 状态 | 捕获短暂提示和弹窗，P1 再作为显式观测能力 |
| 导航栈 / route | 辅助复杂导航判断 |
| 测试环境标识 | 防止跑错环境 |

### P2 能力

| 能力 | 说明 |
| --- | --- |
| mock 登录 | 跳过登录流，适合非登录功能回归 |
| mock API | 构造异常和边界场景 |
| 数据重置 | 保证用例可重复 |
| 网络请求摘要 | 定位接口问题 |
| feature flag | 覆盖配置和实验 |
| 埋点校验 | 验证上报 |
| 性能指标 | 首屏、卡顿、耗时 |

## 截图、OCR、VLM 的定位

截图能力不能完全去掉，但不应作为主路径。

| 场景 | 建议 |
| --- | --- |
| 判断按钮是否存在 | 优先 UI tree，不用截图 |
| 判断文案是否出现 | 优先 UI tree，OCR 兜底 |
| 判断页面跳转 | 优先页面元素 / screen / route |
| 点击按钮 | 优先 selector，避免坐标 |
| 布局错乱 | P3 使用截图 / VLM |
| 图片、图表、canvas、自绘组件 | P3 使用截图 / OCR / VLM |
| WebView 内容无法导出 | P3 使用截图和 OCR 兜底 |
| 失败现场留证 | P0 必须截图，但给人工查看，不让 AI 读取 |
| UI tree 和画面不一致 | P3 截图用于比对 |
| 设计稿对比 | P3 使用截图与设计稿做视觉对比 |

第一阶段建议策略：

```text
正常执行：UI tree / accessibility tree
失败留证：截图 + UI tree + 日志，截图给人工查看
视觉问题：P3 再引入截图 / OCR / VLM
```

## 统一 MobileDriver API

AI 不应直接拼接底层命令，而应调用一组稳定工具接口。

建议的 P0 接口：

```ts
interface MobileDriver {
  buildApp(platform: Platform): Promise<void>
  installApp(deviceId: string, appPath: string): Promise<void>
  launchApp(): Promise<void>
  stopApp(): Promise<void>

  getSnapshot(): Promise<UiSnapshot>

  tap(selector: Selector): Promise<void>
  input(selector: Selector, text: string): Promise<void>
  swipe(options: SwipeOptions): Promise<void>
  back(): Promise<void>

  waitFor(condition: UiCondition, timeoutMs: number): Promise<void>

  assertVisible(selector: Selector): Promise<void>
  assertText(selector: Selector, expected: string): Promise<void>
  assertEnabled(selector: Selector, expected: boolean): Promise<void>

  takeScreenshot(): Promise<string>
  collectLogs(): Promise<LogFile[]>
}
```

P1 再扩展：

```ts
interface SemanticMobileDriver extends MobileDriver {
  getSemanticSnapshot(): Promise<AppSemanticSnapshot>
  waitForScreen(screen: string, timeoutMs: number): Promise<void>
  openDeepLink(url: string): Promise<void>
}
```

P2 再扩展：

```ts
interface AdvancedMobileDriver extends SemanticMobileDriver {
  resetAppState(): Promise<void>
  mockLogin(user: TestUser): Promise<void>
  setFeatureFlag(key: string, value: boolean): Promise<void>
  getNetworkEvents(): Promise<NetworkEvent[]>
}
```

## UI Snapshot 格式

P0 的关键是将底层 UI XML / accessibility tree 压缩成 AI 友好的 JSON，避免直接传截图或完整冗长树。

示例：

```json
{
  "platform": "android",
  "screen_size": [390, 844],
  "current_app": "com.example.app",
  "activity": "MainActivity",
  "elements": [
    {
      "id": "phone_input",
      "text": "",
      "type": "EditText",
      "enabled": true,
      "clickable": true,
      "bounds": [24, 180, 366, 232]
    },
    {
      "id": "login_button",
      "text": "登录",
      "type": "Button",
      "enabled": false,
      "clickable": true,
      "bounds": [24, 260, 366, 316]
    }
  ]
}
```

还可以进一步压缩为面向 AI 决策的摘要：

```json
{
  "screen": "unknown",
  "actions": [
    "input:id=phone_input",
    "tap:id=login_button disabled"
  ],
  "texts": ["手机号", "验证码", "登录"],
  "focused": null
}
```

Snapshot 处理建议：

| 策略 | 说明 |
| --- | --- |
| 过滤不可见元素 | 降低 token 和噪音 |
| 保留可交互元素 | AI 主要需要知道可做什么 |
| 保留关键文案 | 用于判断页面和结果 |
| 保留元素状态 | enabled、selected、checked、clickable |
| 保留 bounds | 仅在 selector 操作失败时兜底 |
| 支持 diff | 后续可只传变化部分 |

## 分阶段建设

### 阶段 1：无截图主路径的 E2E 验证

目标：AI 能根据需求或 Bug 描述，执行真实 App 路径，并用结构化 UI 状态判断是否通过。

| 模块 | P0 能力 |
| --- | --- |
| Build | 编译指定平台 App |
| Device | 安装、启动、停止、必要时清数据 |
| UI Snapshot | 获取结构化 UI tree / accessibility tree |
| Action | 按 id / text / type 点击、输入、滑动、返回 |
| Wait | 等待元素出现、消失、可点击 |
| Assert | 文案存在、元素存在、按钮状态、页面跳转 |
| Evidence | 失败时截图、UI tree、日志 |
| Report | 输出执行步骤、断言结果、失败原因 |

P0 不做：

```text
mock 登录
mock API
网络抓包
feature flag
数据构造
性能分析
埋点校验
复杂视觉回归
需求 / Bug 平台集成
```

推荐组合：

| 平台 | P0 组合 |
| --- | --- |
| Android | Gradle build + adb + UiAutomator / UI dump + logcat + 失败截图 |
| iOS | xcodebuild + simctl + XCUITest / WDA + 日志 + 失败截图 |

若 P0 采用 Appium 作为跨端后端，则推荐组合可改写为：

| 平台 | P0 Appium 组合 |
| --- | --- |
| Android | Gradle build + adb + Appium UiAutomator2 driver + logcat + 失败截图 |
| iOS | xcodebuild + simctl + Appium XCUITest driver / WDA + 日志 + 失败截图 |

P0 暂不覆盖鸿蒙。鸿蒙后续可参考 Android / iOS 的分层方式补齐 `hdc + Harmony UI Test + hilog + 失败截图`。

### 阶段 2：轻量 SDK 增强语义

目标：解决 UI 自动化树语义不足、页面判断不准、loading/toast 难捕获等问题。

| 能力 | 说明 |
| --- | --- |
| 当前页面名 | 辅助判断页面跳转是否成功 |
| 当前路由 | 辅助复杂导航判断 |
| loading 状态 | 减少不稳定等待 |
| toast / dialog 状态 | 捕获短暂提示和弹窗 |
| 组件语义补充 | 让 AI 更理解页面 |
| App 内错误日志 | 提升失败定位 |

阶段 2 仍然不强依赖 mock。SDK 主要作为观测层，而不是测试作弊层。

### 阶段 3：复杂测试能力

目标：提升测试效率、覆盖异常场景、增强失败定位。

| 能力 | 说明 |
| --- | --- |
| mock 登录 | 跳过登录流，提升非登录功能回归效率 |
| mock API | 覆盖异常、边界、弱网等场景 |
| 数据重置 | 保证用例可重复 |
| 网络请求摘要 | 定位接口问题 |
| feature flag | 覆盖配置和实验 |
| 埋点校验 | 验证上报 |
| 性能指标 | 覆盖首屏、卡顿、耗时 |

### 阶段 4：视觉验证能力

目标：在功能验证闭环稳定之后，再引入截图驱动的视觉类验证。

| 能力 | 说明 |
| --- | --- |
| 设计稿对比 | 将运行截图与设计稿做视觉差异对比 |
| 布局回归 | 发现间距、字号、颜色、遮挡等视觉问题 |
| 图片 / 图表 / 自绘内容校验 | 覆盖 UI tree 难以表达的内容 |
| OCR / VLM 辅助 | 仅用于视觉专项，不作为 P0 操作主路径 |

## 测试标识规范

稳定测试标识是 AI 测试闭环能否稳定运行的前提。

| 平台 / 技术栈 | 推荐标识 |
| --- | --- |
| Android View | `resource-id` |
| Android Compose | `testTag` |
| iOS | `accessibilityIdentifier` |
| React Native | `testID` |
| Flutter | `Key` / semantics label |
| WebView | `data-testid` |
| 鸿蒙 ArkUI | 组件 id / 测试标识能力 |

规范建议：

| 规则 | 说明 |
| --- | --- |
| 关键交互元素必须有标识 | 按钮、输入框、Tab、列表项、弹窗操作 |
| 标识稳定且语义明确 | 使用 `login_button`，避免动态 id |
| 标识不绑定文案 | 文案会国际化或调整，id 应稳定 |
| 列表项需要可定位 | 支持通过 item id、index 或业务 key 定位 |
| 弹窗和 toast 后续需要可观测 | P1 再通过 UI tree 增强或 SDK 捕获 |

没有稳定测试标识时，AI 测试会退化成按文案找元素、按坐标点击、截图猜位置，稳定性会显著下降。

## 推荐结论

不建议在 `adb`、自动化测试框架、SDK、截图之间做单选。它们应分层组合：

```text
设备层：adb / simctl
执行层：UiAutomator / XCUITest / WDA / Appium
观测层：UI tree / accessibility tree
增强层：轻量 App Test SDK
证据层：截图，P0 给人工查看
视觉层：OCR / VLM，P3 再引入
```

第一阶段最小可行方案：

```text
设备控制工具
+ 系统 UI 自动化
+ 结构化 UI Snapshot
+ selector 操作
+ 基础断言
+ 失败截图和日志
```

P0 优先使用自动化测试框架获取信息和执行 selector 操作，设备控制使用 `adb` / `simctl`。SDK 可以预留接口，但不应在 P0 承担过多能力。loading、toast、弹窗、导航栈放到 P1；mock、网络、数据构造等能力放到 P2；截图设计稿对比放到 P3。

## 待讨论问题

| 问题 | 需要决策的点 |
| --- | --- |
| P0 首个平台 | 已明确先做 Android + iOS，暂不覆盖鸿蒙 |
| 执行后端 | P0 使用原生框架、Appium、Maestro，还是自研薄封装 |
| UI Snapshot schema | 需要统一到什么粒度，是否保留完整树和摘要两种格式 |
| 测试标识规范 | 是否能要求业务侧补齐关键元素标识 |
| SDK 介入时机 | 倾向 P0 优先自动化测试框架，P1 再实现 screen/loading/toast/dialog/route |
| 报告格式 | 面向开发、测试、产品分别需要哪些信息 |
| 与需求 / Bug 平台集成 | 已明确 P0 先不考虑平台集成 |

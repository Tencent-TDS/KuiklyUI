# iOS E2E 调试踩坑记录

> 记录 iOS 端到端测试从 12/18 到 18/18 过程中遇到的所有问题，供后续 Android 适配和框架迭代参考。

## 问题总览

| # | 问题 | 影响 | 根因 | 修复 |
|---|------|------|------|------|
| 1 | parsePageSource 正则不兼容 iOS XML | 6 个用例连锁失败 | `type`/`visible` 属性正则只匹配 Android 格式 | 正则改为同时匹配两种格式 |
| 2 | navigateToRootDemoPage 硬编码坐标 | 导航不稳定 | tapCoordinate 坐标受键盘/布局影响 | 改用选择器点击 |
| 3 | Test 5 同样硬编码坐标 | Test 5 间歇性失败 | 同 #2 | 改用 navigateToRootDemoPage() |
| 4 | Test 18 dismissAlert 导航逻辑错误 | Test 18 必定失败 | 在 RootDemoPage 上找 TextField | 先 restartApp 回路由页再输入 |
| 5 | iOS mobile:back 导致 App 崩溃 | 无法用系统 back 返回 | Appium back 绕过 Kuikly closePage | 用 `back()` tap `nav_back` testTag |

---

## 问题 1：parsePageSource 正则不兼容 iOS XML（核心 bug）

### 现象

`navigateToRootDemoPage` 中 `snapshot.elements.some(e => e.id?.startsWith("root_btn_"))` 始终为 false，即使页面已经跳转成功。

### 根因

iOS 和 Android 的 XML 属性命名不同：

| 属性 | Android XML | iOS XML |
|------|-------------|---------|
| 元素类型 | `class="android.widget.TextView"` | `type="XCUIElementTypeOther"` |
| 可见性 | `displayed="true"` | `visible="true"` |

原始正则只匹配 Android 格式：

```typescript
type: /\bclass="([^"]*)"/,       // iOS 用 type=，永远匹配不到
visible: /\bdisplayed="([^"]*)"/, // iOS 用 visible=，永远匹配不到
```

### 连锁影响

1. `typeVal` 为 undefined → 第 307 行 `typeVal?.startsWith("XCUIElementType")` 为 false → `el.id = name` 永远不执行 → **iOS 所有 testTag 暴露的 id 丢失**
2. `visibleVal` 为 undefined → iOS 元素可见性信息丢失

这导致 `snapshot.elements.some(e => e.id?.startsWith("root_btn_"))` 永远为 false，所有依赖 RootDemoPage 导航的测试连锁失败。

### 修复

```typescript
type: /\b(?:class|type)="([^"]*)"/,
visible: /\b(?:displayed|visible)="([^"]*)"/,
```

### 教训

parsePageSource 必须同时覆盖 iOS 和 Android 的 XML 属性差异。新增属性解析时应对照两端的 page source 格式验证。

---

## 问题 2 & 3：导航函数和 Test 5 硬编码坐标

### 现象

`tapCoordinate(347, 343)` 间歇性失败，点击不到"跳转2"按钮。

### 根因

- 坐标是基于特定设备分辨率（iPhone 17 Pro, 402x874）硬编码的
- 键盘弹出时按钮可能被遮挡，坐标偏移
- 不同状态下（TextField 是否有值、键盘是否弹出）布局可能微调

### 修复

用选择器点击替代硬编码坐标：

```typescript
// Before
await driver.tapCoordinate(347, 343)

// After
await driver.tap({ text: "跳转2" })
```

Appium 的 `click()` 会自动将不可见元素滚动到可见区域再点击。

### 教训

- 优先使用选择器（accessibilityId / text / xpath）操作元素，而非坐标
- `tapCoordinate` 只在无法用选择器定位时使用（如 Canvas、游戏场景）
- 坐标相关的断言应基于 `getElementRect()` 动态获取

---

## 问题 4：Test 18 dismissAlert 导航逻辑错误

### 现象

Test 18 总是失败，无法触发错误弹窗。

### 根因

原逻辑：

1. `navigateToRootDemoPage()` → 到达 RootDemoPage
2. 在 RootDemoPage 上 `input({ xpath: "//XCUIElementTypeTextField" }, "nonexistent_page")`

但 RootDemoPage 没有 TextField！TextField 在路由页上。

### 修复

先到 RootDemoPage 确认 app 正常，再 restartApp 回路由页触发错误：

```typescript
await navigateToRootDemoPage(driver)  // 确认 app 正常
await driver.restartApp()             // 回到路由页
await driver.input({ xpath: "//XCUIElementTypeTextField" }, "nonexistent_page")
await driver.tap({ text: "跳转2" })   // 触发 PagerNotFoundException
```

### 教训

测试用例中的导航路径必须与 App 实际页面结构一致，不要假设当前页面有某个元素。

---

## 问题 5：iOS mobile:back 导致 App 崩溃

### 现象

调用 `driver.execute("mobile: back")` 后，App 直接崩溃（"The application under test with bundle id ... is not running, possibly crashed"）。

### 根因

KuiklyUI 页面返回应走 `RouterModule.closePage()` → `popViewControllerAnimated`。Appium `mobile:back` 和左滑 pop 是 **UIKit 裸 pop**，Kotlin 侧页面栈不同步，曾导致崩溃。

### 修复（2026-05）

- Demo 导航栏返回箭头统一 `testTag("nav_back")`（`ComposeNavigationBar` + Self DSL `NavigationBar`）。
- iOS `MobileDriver.back()` / `POST /back` 改为点击 `nav_back`，不再调用 Appium 系统 back。
- OpenSpec change：`openspec/changes/appium-mobile-test-ios-back/`。

### 仍禁止

- Appium `mobile:back`、左边缘 swipe pop。
- 无 `nav_back` 的自定义页：需自行加 testTag，或 `restartApp()` + 重新导航。

### BackHandler 说明

页内软返回（退出编辑态、关 Dialog）与页面 pop 不同。Android `back()` 可触发 `onBackPressed`；iOS 暂无等价系统 back，需点页面内按钮或 debug hook（另立变更）。

---

## 更早期的选择器兼容性问题

这些问题在更早阶段已修复，记录于此供参考：

| 问题 | 原因 | 修复 |
|------|------|------|
| `~accessibilityId` 对中文不兼容 | WebdriverIO 的 `~` 前缀处理中文有 bug | 改用 `-ios predicate string:name == 'xxx'` |
| iOS class chain 匹配不到容器 | `XCUIElementTypeAny` 不匹配 `XCUIElementTypeOther` | 改用 predicate string |
| `??` 运算符阻断了空字符串 fallback | TextField 的 `value=""` 时 `??` 不跳到 name | 改用 `||` 运算符 |
| iOS `name` 属性既是 accessibilityId 又是文本 | name 和 label 相同时 id 不应重复设置 | `name !== label` 时才设为 id |

---

## 问题 6：Appium session 跨测试状态污染

### 现象

连续运行多组测试时，后一组测试在导航、断言等操作上大量失败，但单独运行时一切正常。例如 ios-e2e 18/18 通过后，紧接着跑 server-e2e 只得到 12/16。

### 根因

Appium session 在多次测试之间共享同一个 App 进程。前一个测试的副作用（错误弹窗、崩溃恢复状态、异常导航栈）会残留到后续测试：

```
Test A: 触发 PagerNotFoundException → App 弹出 "kuikly error" Alert
Test A 结束 → App 仍在弹窗状态
Test B: 新 session 连到同一个 App → App 还在弹窗状态
Test B: restartApp() → terminate + launch → 但 KuiklyUI 可能持久化了错误状态
Test B: 所有操作被弹窗遮挡 → 连锁失败
```

更危险的是：即使 `restartApp()` 清理了进程，`dismissAlert()` 也可能无法处理所有类型的残留弹窗，导致后续操作全部"打在弹窗上"。

### 修复

在 `startSession` 中自动执行状态清理：

```typescript
async handleStartSession(body) {
  this.driver = new AppiumMobileDriver(config)
  await this.driver.startSession()
  // 自动清理：重启 App + 清理残留弹窗
  await this.driver.restartApp()
  await sleep(500)
  try { await this.driver.dismissAlert() } catch {}
  return { platform: body.platform }
}
```

### 教训

- **永远不要假设 App 处于干净状态**，session 建立后第一步必须是清理
- **测试之间不能依赖执行顺序**，每个测试应该自包含（startSession 自动清理保证了这点）
- **AI agent 在操作 App 时也应遵循同样原则**：如果操作结果不符合预期，先 `restart-app` + `dismiss-alert` 清理状态再重试

### 同类问题：旧版 App 不含 testTag

模拟器被重新创建或 App 被替换为旧版时，testTag 不会出现在 XML 中。现象是 `getSnapshot` 返回的元素没有 `id` 字段，选择器 `accessibilityId` 查找全部失败。

修复：确保 App 构建包含 testTag 代码，并在模拟器上重新安装。`startSession` 的自动清理无法检测这个问题——需要在 Skill 中提醒 AI：如果 snapshot 中没有预期元素的 id，应检查 App 是否是最新构建。

# MVP 测试用例规划

基于 demo 页面和 testTag 支持，按 bug 分类和验证难度排列。

## 前提

已实现 `testTag` 属性：
- 自研 DSL：`attr { testTag("xxx") }`
- Compose DSL：`Modifier.semantics { testTag = "xxx" }`
- Android 映射：`AccessibilityNodeInfo.viewIdResourceName` → UIAutomator `By.res()`
- iOS 映射：`UIView.accessibilityIdentifier` → XCUITest `identifier`

---

## MobileDriver 接口

```ts
export type Platform = "android" | "ios"

export type Selector =
  | { id: string }            // testTag / resource-id / accessibilityIdentifier
  | { text: string }          // 文字内容匹配
  | { accessibilityId: string }
  | { xpath: string }

export interface SwipeOptions {
  startX: number
  startY: number
  endX: number
  endY: number
  durationMs?: number         // 默认 500ms
}

export interface UiElementSnapshot {
  id?: string                 // testTag 值
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
  tapCoordinate(x: number, y: number): Promise<void>   // 坐标点击，用于无 testTag 的遮罩等区域
  input(selector: Selector, text: string): Promise<void>
  back(): Promise<void>
  swipe(opts: SwipeOptions): Promise<void>               // 滑动操作，用于翻页/切换
  waitFor(selector: Selector, timeoutMs: number): Promise<void>
  assertVisible(selector: Selector): Promise<void>
  assertText(text: string): Promise<void>
  assertInViewport(selector: Selector): Promise<void>
  takeScreenshot(outputPath: string): Promise<void>
}
```

---

## P0-1：App 启动 + 首页加载（结构类 ✅）

**平台**：Android + iOS  
**Demo 页面**：RootDemoPage（首页）  
**目的**：验证 App 能启动、首页正常加载  

### Appium page source 示例（Android，简化）

```xml
<node class="android.widget.Button"
      text="KTV页面路由"
      resource-id="root_btn_KTV页面路由"
      clickable="true" />
<node class="android.widget.Button"
      text="TextViewDemo"
      resource-id="root_btn_TextViewDemo"
      clickable="true" />
```

### 解析后的 UiSnapshot

```json
{
  "elements": [
    { "id": "root_btn_KTV页面路由", "text": "KTV页面路由", "type": "Button", "clickable": true },
    { "id": "root_btn_TextViewDemo", "text": "TextViewDemo", "type": "Button", "clickable": true },
    { "id": "root_btn_ListViewDemoPage", "text": "ListViewDemoPage", "type": "Button", "clickable": true },
    ...
  ]
}
```

### 怎么识别可交互元素

```ts
const snapshot = await driver.getSnapshot()
const interactiveElements = snapshot.elements.filter(el => el.clickable === true)
```

### 执行步骤

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | 启动 App，startSession | — |
| 2 | getSnapshot | snapshot.elements.length > 0 |
| 3 | — | snapshot 中包含 text "KTV View Demo" |
| 4 | — | clickable 元素数量 >= 5 |
| 5 | — | assertVisible({ id: "root_btn_TextViewDemo" }) 通过 |

### 代码

```ts
await driver.startSession()

const snapshot = await driver.getSnapshot()
assert(snapshot.elements.length > 0, "页面无元素")

const interactiveElements = snapshot.elements.filter(el => el.clickable === true)
assert(interactiveElements.length >= 5, `可交互元素不足：${interactiveElements.length}`)

await driver.assertText("KTV View Demo")
await driver.assertVisible({ id: "root_btn_TextViewDemo" })
```

**选择器**：`{ id: "root_btn_TextViewDemo" }`（testTag）  
**稳定性**：高（testTag 全局唯一，不受文字变化影响）

---

## P0-2：页面导航 + 返回（交互类 ✅）

**平台**：Android + iOS  
**Demo 页面**：RootDemoPage → TextViewDemoPage → 返回  
**目的**：验证点击跳转和返回功能  

### 怎么做点击操作

```ts
// 通过 testTag 定位并点击
await driver.tap({ id: "root_btn_TextViewDemo" })

// 底层调用链：
// MobileDriver.tap({ id: "root_btn_TextViewDemo" })
//   → selectorToLocator() → Android: "id=root_btn_TextViewDemo"
//                          → iOS: "id==root_btn_TextViewDemo"
//   → driver.$(locator)   // 找到元素
//   → element.click()     // 执行点击
```

### 怎么判断出现新页面内容

**方法：比较点击前后 snapshot 中元素集合的变化**

```ts
// 点击前
const before = await driver.getSnapshot()
const beforeIds = new Set(before.elements.map(el => el.id))

// 点击
await driver.tap({ id: "root_btn_TextViewDemo" })
await new Promise(r => setTimeout(r, 2000))

// 点击后
const after = await driver.getSnapshot()
const afterIds = new Set(after.elements.map(el => el.id))

// 页面变化 = 有新元素出现 或 旧元素消失
const newIds = [...afterIds].filter(id => !beforeIds.has(id))
const removedIds = [...beforeIds].filter(id => !afterIds.has(id))
assert(newIds.length > 0 || removedIds.length > 0, "页面未发生变化")

// 更直接：首页按钮消失 = 离开了首页
assert(!afterIds.has("root_btn_TextViewDemo"), "首页按钮应消失")
```

### 执行步骤

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | startSession, getSnapshot | assertVisible({ id: "root_btn_TextViewDemo" }) |
| 2 | tap({ id: "root_btn_TextViewDemo" }) | — |
| 3 | 等待 2s | — |
| 4 | getSnapshot | "root_btn_TextViewDemo" 不在 snapshot 中 = 离开首页 |
| 5 | back() | — |
| 6 | 等待 1s | — |
| 7 | getSnapshot | assertVisible({ id: "root_btn_TextViewDemo" }) = 回到首页 |

### 代码

```ts
await driver.startSession()
await driver.assertVisible({ id: "root_btn_TextViewDemo" })

// 点击跳转
await driver.tap({ id: "root_btn_TextViewDemo" })
await new Promise(r => setTimeout(r, 2000))

const afterNav = await driver.getSnapshot()
assert(!afterNav.elements.some(el => el.id === "root_btn_TextViewDemo"), "应离开首页")

// 返回
await driver.back()
await new Promise(r => setTimeout(r, 1000))

await driver.assertVisible({ id: "root_btn_TextViewDemo" })
```

**选择器**：`{ id: "root_btn_TextViewDemo" }`  
**稳定性**：高

---

## P0-3：animateScrollTo 滚动到目标位置（交互类 ✅）

**平台**：Android + iOS  
**Demo 页面**：LazyColumnDemo4  
**导航路径**：首页 → KTV View Demo → LazyColumn → LazyColumnDemo4  
**目的**：验证 animateScrollTo 是否滚动到目标 index  

### 怎么判断滚动到了目标位置

**方法：滚动后 snapshot 中出现目标 index 附近的列表项文字**

```ts
// 滚动前：可见项是 0~9
const before = await driver.getSnapshot()
const visibleBefore = before.elements.filter(el => el.text?.match(/^\d+$/))
// visibleBefore.map(el => el.text) = ["0","1","2","3","4","5","6","7","8","9"]

// 触发滚动
await driver.tap({ text: "动画滚动到15" })
await new Promise(r => setTimeout(r, 2000))

// 滚动后：可见项变为 12~20 左右
const after = await driver.getSnapshot()
const visibleAfter = after.elements.filter(el => el.text?.match(/^\d+$/))
// visibleAfter.map(el => el.text) = ["12","13","14","15","16","17","18","19","20"]

assert(visibleAfter.some(el => el.text === "15"), "index=15 未出现在可见区域")
```

### 执行步骤

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | 导航到 LazyColumnDemo4 | snapshot 中包含 "动画滚动到15" |
| 2 | getSnapshot | 记录当前可见列表项 |
| 3 | tap({ text: "动画滚动到15" }) | — |
| 4 | 等待 2s | — |
| 5 | getSnapshot | "15" 出现在可见列表项中 |
| 6 | tap({ text: "请求滚动到5" }) | — |
| 7 | 等待 2s | — |
| 8 | getSnapshot | "5" 出现在可见列表项中 |

### 待补充

LazyColumnDemo4 的按钮暂无 testTag，当前用 text 定位。后续应补充 testTag。

**选择器**：`{ text: "动画滚动到15" }`、`{ text: "请求滚动到5" }`  
**稳定性**：中等（列表项文字是数字，可能和按钮文字混淆；需验证 snapshot 中数字文字元素的归属）

---

## P0-4：ViewPager + 嵌套滑动（交互类 ✅）

**平台**：Android + iOS  
**Demo 页面**：MultiLazyRowInHorizontalPager（@Page("mlr")）  
**关联 bug**：#1254、#1240  
**目的**：验证 HorizontalPager 可正常滑动翻页  

### 怎么做滑动操作

```ts
// swipe 需要起止坐标
// 在屏幕中央水平滑动（从右到左 = 左滑翻页）
await driver.swipe({
  startX: 300, startY: 400,
  endX: 100,  endY: 400,
  durationMs: 500
})

// 底层调用链：
// MobileDriver.swipe(opts)
//   → driver.performActions([
//       { type: "pointer", actions: [
//         { type: "pointerMove", duration: opts.durationMs, x: opts.startX, y: opts.startY },
//         { type: "pointerDown" },
//         { type: "pause", duration: opts.durationMs },
//         { type: "pointerMove", duration: opts.durationMs, x: opts.endX, y: opts.endY },
//         { type: "pointerUp" }
//       ]}
//     ])
```

### 怎么判断页面切换

**方法：滑动后 snapshot 中页码文字和 LazyRow 内容发生变化**

```ts
// 滑动前
const before = await driver.getSnapshot()
assert(before.elements.some(el => el.text?.includes("Page 1/3")))

// 左滑
await driver.swipe({ startX: 300, startY: 400, endX: 100, endY: 400, durationMs: 500 })
await new Promise(r => setTimeout(r, 1000))

// 滑动后
const after = await driver.getSnapshot()
assert(after.elements.some(el => el.text?.includes("Page 2/3")), "未切换到第2页")
assert(after.elements.some(el => el.text?.includes("A-1-1")), "第2页内容未出现")
```

### 执行步骤

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | 导航到 mlr 页面 | snapshot 中包含 "双指触摸测试" 和 "Page 1/3" |
| 2 | — | snapshot 中包含 "A-0-1" |
| 3 | swipe 左滑 | 等待 1s |
| 4 | getSnapshot | "Page 2/3" 出现，"A-1-1" 出现 |
| 5 | swipe 再左滑 | 等待 1s |
| 6 | getSnapshot | "Page 3/3" 出现，"A-2-1" 出现 |

**选择器**：`{ text: "双指触摸测试" }`、`{ text: "A-0-1" }`  
**稳定性**：中等（滑动操作依赖坐标，不同设备尺寸可能需调整）

---

## P0-5：Modal 弹窗 + 关闭（交互类 ✅）

**平台**：Android + iOS  
**Demo 页面**：ModalViewDemoPage  
**目的**：验证 Modal 弹出和关闭  

### 怎么点击遮罩区域

遮罩区域没有 testTag，用 `tapCoordinate` 坐标点击。ActionSheet 在屏幕底部弹出，遮罩在上方。

```ts
// 点击屏幕顶部（遮罩区域）
await driver.tapCoordinate(100, 100)
```

### 执行步骤

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | 导航到 ModalViewDemoPage | assertVisible({ id: "modal_trigger_btn" }) |
| 2 | getSnapshot | text "can点击展示modal" 存在 |
| 3 | tap({ id: "modal_trigger_btn" }) | — |
| 4 | 等待 1s | — |
| 5 | getSnapshot | id "action_item_0" ~ "action_item_3" 出现，text "Item0" ~ "Item3" 出现 |
| 6 | tapCoordinate(100, 100) 点击遮罩 | — |
| 7 | 等待 1s | — |
| 8 | getSnapshot | id "action_item_0" 消失，text 变为 "did点击展示modal" |

### 代码

```ts
await driver.assertVisible({ id: "modal_trigger_btn" })

// 点击打开
await driver.tap({ id: "modal_trigger_btn" })
await new Promise(r => setTimeout(r, 1000))

// 确认弹出
await driver.assertVisible({ id: "action_item_0" })
await driver.assertVisible({ id: "action_item_3" })

// 点击遮罩关闭
await driver.tapCoordinate(100, 100)
await new Promise(r => setTimeout(r, 1000))

// 确认关闭
const afterClose = await driver.getSnapshot()
assert(!afterClose.elements.some(el => el.id === "action_item_0"), "ActionSheet 未关闭")
assert(afterClose.elements.some(el => el.text === "did点击展示modal"), "按钮状态未变化")
```

**选择器**：`{ id: "modal_trigger_btn" }`、`{ id: "action_item_0" }` ~ `{ id: "action_item_3" }`  
**稳定性**：高

---

## P0-6：Input 输入 + 键盘（交互类 ✅）

**平台**：Android + iOS  
**Demo 页面**：InputViewDemoPage  
**目的**：验证输入框可以输入和失焦  

### iOS 和 Android 在 accessibility tree 中的差异

| 平台 | Input 类型 | 输入内容属性 |
|------|-----------|-------------|
| Android | `android.widget.EditText` | `text` 属性 = 当前输入内容 |
| iOS | `XCUIElementTypeTextField` | `value` 属性 = 当前输入内容 |

MobileDriver 的 `getSnapshot` 会同时读取 `text` 和 `value`，统一放入 `text` 字段。

### 执行步骤

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | 导航到 InputViewDemoPage | assertVisible({ id: "input_field" }) |
| 2 | tap({ id: "input_field" }) | — |
| 3 | 等待 500ms（键盘弹出） | — |
| 4 | input({ id: "input_field" }, "test123") | — |
| 5 | getSnapshot | input_field 元素的 text 包含 "test123" |

### 代码

```ts
await driver.assertVisible({ id: "input_field" })

await driver.tap({ id: "input_field" })
await new Promise(r => setTimeout(r, 500))
await driver.input({ id: "input_field" }, "test123")

const after = await driver.getSnapshot()
const inputEl = after.elements.find(el => el.id === "input_field")
assert(inputEl?.text?.includes("test123"), `输入内容不正确：${inputEl?.text}`)
```

**选择器**：`{ id: "input_field" }`  
**稳定性**：中等（键盘弹出时间可能因平台而异，需适当增加等待时间）

---

## P0-7：列表滚动 + 下拉刷新（交互类 ✅）

**平台**：Android + iOS  
**Demo 页面**：ListViewDemoPage  
**目的**：验证列表滚动和刷新  

### 执行步骤

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | 导航到 ListViewDemoPage | snapshot 中包含 "我是第0个卡片" |
| 2 | assertVisible({ id: "refresh_btn" }) | 刷新按钮可见 |
| 3 | tap({ id: "refresh_btn" }) | — |
| 4 | 等待 3s（刷新完成） | — |
| 5 | getSnapshot | "我是第0个卡片" 仍然存在（刷新后重建列表） |

### 代码

```ts
await driver.assertVisible({ id: "refresh_btn" })
await driver.assertText("我是第0个卡片")

await driver.tap({ id: "refresh_btn" })
await new Promise(r => setTimeout(r, 3000))

await driver.assertText("我是第0个卡片")
```

**选择器**：`{ id: "refresh_btn" }`、`{ text: "我是第0个卡片" }`  
**稳定性**：高

---

## P0-8：SliderPage 滑动切换（交互类 ✅）

**平台**：Android + iOS  
**Demo 页面**：SliderPageViewDemoPage  
**目的**：验证轮播图可滑动切换  

### 怎么判断轮播页面切换

**方法：滑动后 snapshot 中 testTag 对应的元素变化**

```ts
// 滑动前：slider_page_第一页 存在
await driver.assertVisible({ id: "slider_page_第一页" })

// 左滑
await driver.swipe({ startX: 300, startY: 400, endX: 100, endY: 400, durationMs: 500 })
await new Promise(r => setTimeout(r, 1000))

// 滑动后：slider_page_第二页 出现，slider_page_第一页 消失
const after = await driver.getSnapshot()
assert(after.elements.some(el => el.id === "slider_page_第二页"), "第二页未出现")
assert(!after.elements.some(el => el.id === "slider_page_第一页"), "第一页未消失")
```

### 执行步骤

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | 导航到 SliderPageViewDemoPage | assertVisible({ id: "slider_page_第一页" }) |
| 2 | swipe 左滑 | 等待 1s |
| 3 | getSnapshot | id "slider_page_第二页" 出现，"slider_page_第一页" 消失 |
| 4 | swipe 再左滑 | 等待 1s |
| 5 | getSnapshot | id "slider_page_第三页" 出现 |

**选择器**：`{ id: "slider_page_第一页" }`、`{ id: "slider_page_第二页" }`、`{ id: "slider_page_第三页" }`  
**稳定性**：中等（滑动坐标可能需要根据设备调整）

---

## 暂不纳入 P0 的用例

| 用例 | 类型 | 原因 |
|------|------|------|
| Image 加载验证（中文路径等） | 渲染类 ❌ | accessibility tree 无法判断图片是否渲染 |
| 圆角/阴影验证 | 渲染类 ❌ | accessibility tree 不包含视觉样式 |
| 旋转屏幕后列表空白 | 几何类 ⚠️ | 需验证 bounds 返回是否准确，降级到 P1 |
| 键盘并发崩溃 | 竞态类 ⚠️ | UI 自动化无法保证复现竞态 |
| ScrollView offset 偏移 | 几何类 ⚠️ | 需 assertInViewport + bounds 验证，P1 |

---

## 测试用例与 bug 分类的覆盖矩阵

| 分类 | P0 用例 | 覆盖情况 |
|------|---------|---------|
| 结构类 ✅ | P0-1（App 启动） | 已覆盖 |
| 内容类 ✅ | P0-1、P0-2 | 已覆盖 |
| 交互类 ✅ | P0-2 ~ P0-8 | 充分覆盖 |
| 几何类 ⚠️ | 无 | P1 补充 assertInViewport |
| 渲染类 ❌ | 无 | 需 SDK 支持(P1) 或截图对比(P3) |

---

## MobileDriver 新增方法说明

### `swipe(opts: SwipeOptions)`

用于滑动操作（翻页、切换 tab、滚动列表等）。

```ts
interface SwipeOptions {
  startX: number    // 起始 X 坐标（像素）
  startY: number    // 起始 Y 坐标（像素）
  endX: number      // 结束 X 坐标（像素）
  endY: number      // 结束 Y 坐标（像素）
  durationMs?: number  // 滑动时长，默认 500ms
}
```

**Appium 底层实现**：使用 W3C Actions API 的 pointer 协议。

```ts
// 实现伪代码
async swipe(opts: SwipeOptions) {
  const duration = opts.durationMs ?? 500
  await driver.performActions([{
    type: "pointer",
    id: "finger1",
    parameters: { pointerType: "touch" },
    actions: [
      { type: "pointerMove", duration: 0, x: opts.startX, y: opts.startY },
      { type: "pointerDown", button: 0 },
      { type: "pause", duration },
      { type: "pointerMove", duration, x: opts.endX, y: opts.endY },
      { type: "pointerUp", button: 0 }
    ]
  }])
}
```

**使用示例**：

```ts
// 左滑翻页（从右往左划）
await driver.swipe({ startX: 300, startY: 400, endX: 100, endY: 400 })

// 上滑滚动列表（从下往上划）
await driver.swipe({ startX: 200, startY: 600, endX: 200, endY: 200 })

// 慢速滑动
await driver.swipe({ startX: 300, startY: 400, endX: 100, endY: 400, durationMs: 1000 })
```

**坐标获取方式**：从 snapshot 中元素的 `bounds` 推算滑动区域中心点。

### `tapCoordinate(x: number, y: number)`

用于点击没有 testTag 的区域（如遮罩层、空白区域、自定义绘制内容等）。

```ts
async tapCoordinate(x: number, y: number) {
  await driver.performActions([{
    type: "pointer",
    id: "finger1",
    parameters: { pointerType: "touch" },
    actions: [
      { type: "pointerMove", duration: 0, x, y },
      { type: "pointerDown", button: 0 },
      { type: "pause", duration: 100 },
      { type: "pointerUp", button: 0 }
    ]
  }])
}
```

**使用示例**：

```ts
// 点击屏幕顶部（Modal 遮罩区域）
await driver.tapCoordinate(100, 100)

// 点击特定坐标
await driver.tapCoordinate(375, 812)
```

**注意**：优先使用 `tap(selector)` + testTag，`tapCoordinate` 仅作为降级方案，因为坐标依赖设备分辨率。

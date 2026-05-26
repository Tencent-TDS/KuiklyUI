# Appium View 树、坐标、属性与 view-tree 接口设计

## 1. Appium 可获取的节点属性

### 1.1 iOS (XCUITest) 原始 XML 属性

每个节点可获取的属性：

| 属性 | 说明 | 示例 |
|------|------|------|
| `type` | 元素类型（类名） | `XCUIElementTypeTextField` |
| `name` | accessibility identifier | `inputView` / `跳转2` |
| `label` | 可读标签 | `跳转2` |
| `value` | 当前值（输入框内容、开关状态等） | `HorizontalPagerDemo3` |
| `enabled` | 是否可用 | `true` |
| `visible` | 是否可见 | `true` |
| `accessible` | 是否可交互 | `true`/`false` |
| `x` `y` `width` `height` | 绝对坐标（相对窗口） | `20, 323, 265, 41` |
| `index` | 兄弟中的序号 | `0` |
| `traits` | 无障碍特征 | `StaticText` / `Button` / `KeyboardKey` |
| `placeholderValue` | 输入框占位文本 | `输入pageName（不区分大小写）` |
| `processId` | 进程 ID | `28765` |
| `bundleId` | 应用包 ID（仅 Application 根节点） | `com.tencent.kuiklycore.demo.luoyibu` |

### 1.2 Android (UiAutomator2) 原始 XML 属性

| 属性 | 说明 | 示例 |
|------|------|------|
| `class` | 元素类型（完整类名） | `android.widget.TextView` |
| `resource-id` | 资源 ID | `com.tencent.kuikly.android.demo:id/btn_login` |
| `text` | 显示文本 | `登录` |
| `content-desc` | 无障碍描述 | `登录按钮` |
| `checked` | 是否选中 | `true` |
| `clickable` | 是否可点击 | `true` |
| `enabled` | 是否可用 | `true` |
| `displayed` | 是否可见 | `true` |
| `focused` | 是否获焦 | `true` |
| `scrollable` | 是否可滚动 | `false` |
| `selected` | 是否选中 | `false` |
| `bounds` | 屏幕坐标 `[x1,y1][x2,y2]` | `[0,100][400,200]` |
| `index` | 兄弟中的序号 | `0` |
| `package` | 应用包名 | `com.tencent.kuikly.android.demo` |

### 1.3 Appium 无法获取的属性

**关键限制：Appium 只能拿到无障碍树（accessibility tree），不是真正的渲染树（render tree）。**

以下属性 Appium 层面**无法获取**：

| 属性 | 说明 | 影响 |
|------|------|------|
| 背景色 / 前景色 | 无障碍树不含颜色信息 | 无法验证颜色 bug |
| 圆角 (border-radius) | 无此属性 | 无法验证圆角渲染 |
| 字体大小 / 字重 | 无障碍树不含排版属性 | 无法验证字号 bug |
| 边框 / 阴影 | 无此属性 | 无法验证样式 bug |
| 透明度 (alpha) | 只有 visible true/false，无精确值 | 无法验证半透明渲染 |
| 图片内容 | 只有 Image 类型，无 URL/描述 | 无法验证图片加载 |
| padding / margin | 无此属性 | 无法验证间距 bug |
| 动画状态 | 快照是静态的，无动画进度 | 无法验证动画中间状态 |

**这意味着仅靠 Appium 无障碍属性，很多 UI bug 无法自动验证（如圆角没渲染、颜色错误等）。**

---

## 2. view-tree 接口设计

### 2.1 接口定义

| 接口 | 用途 | 过滤策略 |
|------|------|---------|
| `GET /view-tree` | 完整 UI 树，调试用 | 不过滤，保留所有节点含 `visible=false` |
| `GET /view-tree?visible=true` | AI 日常使用 | 过滤 `visible=false`、键盘子树、零尺寸节点 |

### 2.2 返回格式：缩进文本树

**核心原则：对 AI 和人类都友好，同时保持可解析性。**

```json
{
  "ok": true,
  "platform": "ios",
  "viewport": [402, 874],
  "text": "Application [0,0,402,874]\n├── Other [0,0,402,98]\n│   └── StaticText text=\"Kuikly页面路由\" [142,65,118,22]\n..."
}
```

AI 直接读 `text` 字段，人类也可直接查看。

### 2.3 缩进文本树格式规范

#### 属性展示规则

每个节点一行，格式：`Kuikly类名 (原生类型) [属性...] [x,y,w,h]`

| 属性 | 何时展示 | 格式 | 说明 |
|------|---------|------|------|
| `类名` | 始终 | `FlexboxLayout` / `StaticText` | Kuikly View 类名（debugName）；无 debugName 时回退到简化原生类名 |
| `(RawType)` | 有值时 | `(XCUIElementTypeOther)` | 原生类型，写 xpath 时用这个 |
| `testTag` | 有值时 | `testTag="root_btn_KTV"` | 对应代码中设置的 testTag |
| `text` | 有值时 | `text="跳转2"` | 显示文本 / label |
| `value` | 有值且≠text时 | `value="HorizontalPagerDemo3"` | 输入框当前内容 |
| `placeholder` | 有值时 | `placeholder="输入pageName"` | 输入框占位文本 |
| `accessible` | 为 true 时 | `accessible=true` | 表示可交互 |
| `enabled` | 为 false 时 | `enabled=false` | 只展示异常值 |
| `visible` | 为 false 时 | `visible=false` | visible 模式下不出现 |
| `clickable` | 为 true 时(Android) | `clickable=true` | Android 专有 |
| `checked` | 为 true 时(Android) | `checked=true` | Android 专有 |
| `scrollable` | 为 true 时(Android) | `scrollable=true` | Android 专有 |
| `traits` | 有值且非默认时(iOS) | `traits="Button"` | iOS 语义标记 |
| `bounds` | 始终 | `[x,y,w,h]` | 相对屏幕/窗口左上角 |

**原则：只展示和默认值不同的属性，减少噪音。**

#### Type 简化规则

| iOS 原始 | Android 原始 | 统一展示 |
|----------|-------------|---------|
| `XCUIElementTypeStaticText` | `android.widget.TextView` | `StaticText` |
| `XCUIElementTypeTextField` | `android.widget.EditText` | `TextField` |
| `XCUIElementTypeButton` | `android.widget.Button` | `Button` |
| `XCUIElementTypeImage` | `android.widget.ImageView` | `Image` |
| `XCUIElementTypeOther` | `android.view.View` | `Other` |
| `XCUIElementTypeKeyboard` | — | `Keyboard` |
| `XCUIElementTypeWindow` | — | `Window` |
| `XCUIElementTypeApplication` | — | `Application` |
| `XCUIElementTypeSwitch` | `android.widget.Switch` | `Switch` |
| `XCUIElementTypeScrollView` | `android.widget.ScrollView` | `ScrollView` |
| `XCUIElementTypeTable` | `android.widget.ListView` | `Table` |
| `XCUIElementTypeCollectionView` | `androidx.recyclerview.widget.RecyclerView` | `CollectionView` |
| `XCUIElementTypeSlider` | `android.widget.SeekBar` | `Slider` |
| `XCUIElementTypeProgressBar` | `android.widget.ProgressBar` | `ProgressBar` |
| 其他 `XCUIElementType*` | 其他 `android.widget.*` | 去掉前缀 |

#### 层级缩进

```
根节点
├── 子节点1
│   ├── 孙节点1
│   └── 孙节点2
├── 子节点2
└── 子节点3
```

使用 `├──` `└──` `│   ` 缩进，与 `tree` 命令输出一致。

### 2.4 完整案例

#### 案例 A：首页路由页面（键盘弹出时）— visible 模式

```
Application [0,0,402,874]
├── Window [0,0,402,874]
│   └── Other [0,0,402,874]
│       └── Other [0,0,402,874]
│           ├── Other [0,0,402,98]
│           │   └── StaticText text="Kuikly页面路由" [142,65,118,22]
│           ├── Other [70,118,262,186]
│           │   └── Image [80,128,242,166]
│           ├── Other [10,323,285,41]
│           │   └── TextField value="HorizontalPagerDemo3" placeholder="输入pageName（不区分大小写）" accessible=true [20,323,265,41]
│           ├── Other [307,323,80,41]
│           │   └── StaticText text="跳转2" [330,333,34,22]
│           ├── Other [10,378,295,16]
│           ├── Other [20,413,362,25]
│           ├── Other [20,477,362,25]
│           └── Other [20,541,362,25]
└── Window [0,566,402,308] (keyboard)
    └── Keyboard [0,583,402,233]
```

#### 案例 B：RootDemoPage — visible 模式

```
Application [0,0,402,874]
└── Window [0,0,402,874]
    └── Other [0,0,402,874]
        ├── Other [0,0,402,98]
        │   └── StaticText text="Root Demo" [142,65,118,22]
        ├── Other [10,118,382,45]
        │   └── StaticText testTag="root_btn_KTV页面路由" text="KTV页面路由" accessible=true [15,128,372,35]
        ├── Other [10,168,382,45]
        │   └── StaticText testTag="root_btn_弹窗演示" text="弹窗演示" accessible=true [15,178,372,35]
        ├── Other [10,218,382,45]
        │   └── StaticText testTag="root_btn_输入框演示" text="输入框演示" accessible=true [15,228,372,35]
        ├── Other [10,268,382,45]
        │   └── StaticText testTag="root_btn_列表演示" text="列表演示" accessible=true [15,278,372,35]
        └── Other [10,318,382,45]
            └── StaticText testTag="root_btn_Slider演示" text="Slider演示" accessible=true [15,328,372,35]
```

#### 案例 C：完整模式（含不可见节点）

```
Application [0,0,402,874]
├── Window [0,0,402,874] visible=true
│   └── Other [0,0,402,874]
│       └── ...（同 visible 模式）
└── Window [0,0,402,874] visible=false
    └── Other [0,0,402,874] visible=false
        ├── Other visible=false
        └── Other testTag="inputView" visible=false [0,566,402,308]
```

---

## 3. View 树完整性

### 3.1 Android UiAutomator2

**默认只返回 `isVisibleToUser() == true` 的元素。**

| 元素状态 | 默认是否在 tree 中 | 备注 |
|---------|-------------------|------|
| 正常显示 | ✅ 在 | — |
| `visibility=GONE` | ❌ 不在 | `isVisibleToUser()` 返回 false |
| `visibility=INVISIBLE` | ❌ 不在 | `isVisibleToUser()` 返回 false |
| off-screen（被 ScrollView 滚出可视区域） | ❌ 默认不在 | 被可滚动祖先裁剪 |
| alpha=0 | ⚠️ 可能不在 | 取决于系统判断 |

**获取完整 tree（含不可见元素）：**

```ts
await driver.updateSettings({ allowInvisibleElements: true })
```

### 3.2 iOS XCUITest

**默认返回所有 accessibility 元素，包括 off-screen 的。**

| 元素状态 | 是否在 tree 中 | 备注 |
|---------|---------------|------|
| 正常显示 | ✅ 在 | `visible=true` |
| off-screen | ✅ 在 | `visible=false` |
| alpha=0 / hidden | ✅ 在 | `visible=false` |
| `isAccessibilityElement=false` | ⚠️ 可能不在 | 如果无任何可访问子元素则可能跳过 |

**iOS 和 Android 的关键区别**：iOS 默认包含 off-screen 元素，Android 默认不包含。

---

## 4. bounds 坐标参考系

### 4.1 Android

**bounds 是屏幕绝对坐标，但经过"可见区域裁剪"。**

底层调用 `AccessibilityNodeInfo.getBoundsInScreen()`，然后做三层裁剪：

```text
原始 bounds (getBoundsInScreen)
  → 与屏幕尺寸取交集
  → 与窗口尺寸取交集
  → 与最近的可滚动祖先的可见区域取交集（默认开启）
  → 最终 bounds
```

| 情况 | bounds 值 |
|------|----------|
| 正常显示 | [x, y, x+w, y+h] 正常值 |
| 部分 off-screen | 被裁剪为与屏幕/祖先交集后的矩形 |
| 完全 off-screen | 空矩形或极小矩形（交集为空） |

**关闭可滚动祖先裁剪**：

```ts
await driver.updateSettings({ simpleBoundsCalculation: true })
```

### 4.2 iOS

**rect 是相对于应用窗口的坐标（不是屏幕绝对坐标，不是相对父 View）。**

```text
原点 (0,0) = 应用窗口左上角 = 状态栏下方
不包含状态栏区域
off-screen 元素的 rect 不会被裁剪，保留实际坐标值
```

| 情况 | rect 值 |
|------|--------|
| 正常显示 | {x, y, width, height} 正常值 |
| off-screen | 实际坐标值，可能是负数或超出屏幕尺寸 |

### 4.3 平台对比

| 特性 | Android | iOS |
|------|---------|-----|
| 坐标参考系 | 屏幕绝对坐标（经裁剪） | 相对于应用窗口 |
| off-screen bounds | 被裁剪为空/极小矩形 | 保留实际坐标值（可含负数） |
| 可滚动祖先裁剪 | 默认开启，可关闭 | 不存在此问题 |
| 状态栏 | 包含在坐标中 | 不包含，需手动加 |

---

## 5. 可见性判断

### 5.1 Android

`displayed` 属性 = `AccessibilityNodeInfo.isVisibleToUser()`

```text
displayed=true  → 元素至少有一部分在可视区域内
displayed=false → 元素完全不可见（GONE/INVISIBLE/off-screen/被遮挡）
```

### 5.2 iOS

`visible` 属性 = `XCAXAIsVisible`

```text
visible=true  → 元素至少有一部分在可视区域内
visible=false → 元素完全不可见（off-screen/被遮挡/alpha=0）
```

iOS 额外还有 `hittable` 属性（`isHittable`）：表示是否可以在其可见区域内被点击，比 `visible` 更严格。

---

## 6. P1 功能：SDK 暴露运行时属性

### 6.1 问题

Appium 只能获取无障碍树属性，无法获取样式属性（圆角、颜色、字号等），导致很多 UI bug 无法自动验证。

### 6.2 方案

在 KuiklyUI 渲染层，将关键样式属性序列化到无障碍节点的扩展属性中，使 Appium 可以获取。

#### Android 实现

`AccessibilityNodeInfo` 支持 `setExtras(Bundle)`（API 19+），可写入自定义键值对：

```kotlin
// KRCSSViewExtension.kt
fun setTestTag(view: View, tag: String) {
    view.viewIdResourceName = tag
}

fun setAccessibilityStyleInfo(view: View, attrs: Map<String, String>) {
    view.accessibilityDelegate = object : View.AccessibilityDelegate() {
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            val extras = Bundle()
            attrs.forEach { (k, v) -> extras.putString(k, v) }
            info.extras.putAll(extras)
        }
    }
}
```

#### iOS 实现

iOS 无 `extras` 机制，可利用现有属性编码：

- `accessibilityValue`：如果无 value，可用于存放 JSON 字符串
- `accessibilityHint`：可用于存放额外信息
- 或在 `accessibilityLabel` 中追加结构化后缀

```objc
// UIView+CSS.m
- (void)css_accessibilityStyleInfo:(NSDictionary *)attrs {
    // 方案1：用 accessibilityHint 存 JSON
    self.accessibilityHint = [self css_encodeStyleAttrs:attrs];
    
    // 方案2：用 accessibilityValue（如果无业务 value）
    if (!self.accessibilityValue) {
        self.accessibilityValue = [self css_encodeStyleAttrs:attrs];
    }
}
```

#### Appium 读取

```ts
// Android：直接读 extras
const styleJson = await el.getAttribute("styleInfo")

// iOS：读 accessibilityHint / accessibilityValue
const styleJson = await el.getAttribute("hint")  // 或 "value"
```

### 6.3 计划暴露的属性

| 属性 | 类型 | 优先级 | 验证场景 |
|------|------|--------|---------|
| `borderRadius` | number | P1 | 圆角未渲染 |
| `backgroundColor` | string(hex) | P1 | 背景色错误 |
| `foregroundColor` / `textColor` | string(hex) | P1 | 文字颜色错误 |
| `fontSize` | number | P1 | 字号错误 |
| `alpha` | number(0-1) | P1 | 透明度/半透明 |
| `cornerRadius` | number | P2 | 圆角（同 borderRadius） |
| `borderWidth` | number | P2 | 边框宽度 |
| `borderColor` | string(hex) | P2 | 边框颜色 |
| `shadow` | string | P3 | 阴影 |
| `imageUrl` | string | P3 | 图片 URL（验证加载） |

### 6.4 实施步骤

1. 在 `Attr.kt` / `IStyleAttr.kt` 增加 `accessibilityStyle` 属性控制是否暴露样式
2. Android `KRCSSViewExtension` 实现 `setAccessibilityStyleInfo`，通过 `AccessibilityNodeInfo.extras` 写入
3. iOS `UIView+CSS` 实现 `css_accessibilityStyleInfo`，通过 `accessibilityHint` 写入
4. `parsePageSource` 解析新增属性，view-tree 接口展示
5. 更新测试用例，验证样式属性可获取

---

## 7. 测试操作日志

AI 调用测试接口时，自动记录操作路径到日志文件，便于人类审查。

### 日志格式

```
[12:00:01] start-session ios
[12:00:35] snapshot → 5 visible nodes
[12:00:37] tap testTag="root_btn_KTV页面路由"
[12:00:40] snapshot → RootDemoPage (3 visible nodes)
[12:00:42] tap testTag="root_btn_弹窗演示"
[12:00:44] assert-visible testTag="modal_dialog" → PASS
[12:00:46] stop-session
```

### 存储

- 操作路径日志：`./logs/mobile_test_session_<YYYYMMDD_HHMMSS>.log`（主索引，记录每步操作与 PASS/FAIL）
- 快照文件：`./logs/mobile_test_snapshot_<时间>_step<N>.txt`（由 session 日志引用）
- 截图：`./logs/mobile_test_screenshot_<时间>.png`（FAIL 或异常时由 session 日志引用）
- Skill 要求：会话开始创建 session 日志；每次 `/view-tree` 保存快照并在 session 日志中记录路径

---

## 8. debugName 暴露：自动化测试读取 View 类名

### 8.1 问题

iOS 无障碍树中 KuiklyUI 节点 77% 为 `XCUIElementTypeOther`，系统不关心底层 UIView 子类名。AI 无法从 `type`/`rawType` 判断节点的实际 KuiklyUI 类型（如 FlexboxLayout、TextButton、ScrollerView）。

### 8.2 方案

仅在 **debug 模式**下注入 View 类名到无障碍属性，release 不变。

| 平台 | 属性 | 值 | 影响 |
|------|------|---|------|
| Android | `contentDescription` | 类名（如 `FlexboxLayout`） | TalkBack 会朗读（仅 debug 包） |
| iOS | `accessibilityIdentifier` | `"ClassName testTag"` 合并 | 和 testTag 共用同一字段 |

### 8.3 iOS 合并规则

`accessibilityIdentifier` 格式：`"{debugName} {testTag}"`，空格分隔。

| debugName | testTag | accessibilityIdentifier |
|-----------|---------|------------------------|
| `FlexboxLayout` | `login_btn` | `"FlexboxLayout login_btn"` |
| `FlexboxLayout` | (空) | `"FlexboxLayout"` |
| (空) | `login_btn` | `"login_btn"` |
| (空) | (空) | nil |

view-tree 解析层自动拆分：`accessibilityIdentifier` → `type`(debugName) + `testTag`，AI 看到的格式跨平台统一。

### 8.4 view-tree 输出示例（debug 模式）

```
FlexboxLayout (XCUIElementTypeOther) testTag="login_btn" [8,99,386,61]
    StaticText (XCUIElementTypeStaticText) text="登录" [20,111,134,37]
ScrollerView (XCUIElementTypeOther) [0,91,402,3000]
    TextButton (XCUIElementTypeOther) testTag="submit_btn" [8,168,386,61]
```

---

## 9. 滑动操作：POST /scroll

`/scroll` 使用 W3C Actions `pointerMove` 实现滑动，两端均会产生惯性滚动：

| 参数 | 说明 |
|------|------|
| `startX/startY` | 手指起始坐标 |
| `endX/endY` | 手指松开坐标（**非内容最终停止位置**） |
| `durationMs` | 滑动持续时间，默认 500ms |

**惯性滚动实测**：

| 平台 | durationMs=500 | durationMs=2000 |
|------|---------------|-----------------|
| iOS | `decelerate=YES`，有惯性 | `decelerate=NO`，无惯性 |
| Android | `SCROLL_STATE_SETTLING`，有惯性 | `SCROLL_STATE_SETTLING`，少量惯性 |

**end 坐标不精确**：手指滑动距离与 contentOffset 变化非 1:1 对应，存在 3-21% 误差。不要据此精确判断滚动距离，用 `/view-tree` 验证滚动结果。

### 9.1 与 KuiklyUI scroll 事件的对应

| 操作 | 事件序列 |
|------|---------|
| `/scroll`（有惯性） | dragBegin → scroll(isDragging=true) → dragEnd → scroll(isDragging=false, flying) → scrollEnd |

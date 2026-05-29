# Mobile-Test 自动化框架开发进展

> **文档已迁入** `openspec/changes/appium-mobile-test-mvp/docs/`。工具代码在 **kuikly-harness**（`kuikly-mobile-test` skill）。

## 总体目标

为 KuiklyUI 提供跨平台（iOS + Android）自动化测试工具，使 AI Agent 能通过 `/view-tree` 观察节点树、用语义 selector 操作 UI，实现"observe → think → act"闭环。

---

## 已完成的所有改动

### 一、iOS：Card 子节点展开（core-render-ios）

**问题**：Compose DSL 的 Card 在 iOS accessibility tree 中是叶子节点（Button），Appium 无法展开看子节点。

**根因**：Card view 被设置了 `isAccessibilityElement=YES` + `accessibilityTraits=Button`，iOS 系统将其视为语义叶子。

**修复**：`core-render-ios/Extension/Category/UIView+CSS.m` + `UIView+CSSDebug.m`

在所有设置 `isAccessibilityElement = YES` 的地方加后置检查：
```objc
if (self.css_debugName.length > 0 && self.subviews.count > 0) {
    self.isAccessibilityElement = NO;
}
```
覆盖了 4 个入口：`setCss_accessibility`、`setCss_accessibilityRole`、`setCss_accessibilityInfo`（UIView+CSS.m）、`setCss_debugName`（UIView+CSSDebug.m，安全网）。

**效果**：设置了 DebugName 且有子节点的 view 不会成为 accessibility leaf，XCUITest 可以展开子树。

---

### 二、Android：RichTextView 文本暴露（core-render-android）

**问题**：Android `KRRichTextView` 用 Canvas 自绘文字，UiAutomator2 看不到文本内容，全部显示为无 text 的 `FrameLayout`。

**修复文件**：
- `core-render-android/.../expand/component/KRRichTextView.kt`
- `core-render-android/.../css/ktx/KRCSSViewExtension.kt`
- `core-render-android/.../const/KRConst.kt`

**方案**：
1. 新增常量 `PLAIN_TEXT_FOR_A11Y = "plainTextForA11y"`
2. `KRRichTextView.setShadow()` 里把 textLayout 文本存入 `putViewData(PLAIN_TEXT_FOR_A11Y, plainText)`，并确保 `initAccessibilityDelegateIfNeeded()` 被调用
3. `KRCSSViewExtension.initAccessibilityDelegate()` 里读取 `PLAIN_TEXT_FOR_A11Y` 并写到 `info.text`（不覆盖 `contentDescription`，后者保留 debugName 类名）
4. 新增 `internal fun View.initAccessibilityDelegateIfNeeded()` 公开辅助函数

**关键细节**：不能用 `contentDescription` 暴露文本，因为 `contentDescription = "RichTextView"` 是 debugName 的存储方式，覆盖后 view-tree.ts 的 `looksLikeClassName` 逻辑会失效，type 退化为 `FrameLayout`。

---

### 三、mobile-test 工具层改动（`.claude/skills/kuikly-mobile-test`）

#### 3.1 Android selector 语义归一化（dist/appium-mobile-driver.js + src/appium-mobile-driver.ts）

**问题**：view-tree 里显示 `text="xxx"` 实际来自 Android XML 的 `content-desc`，`testTag="xxx"` 来自 `resource-id`，但原有 selector 逻辑不对应，导致 selector 命中失败。

**修复**：

| selector | 原来 | 修复后 |
|----------|------|--------|
| `{text:"xxx"}` Android | `text=xxx`（XML text 属性）| `//*[@content-desc="xxx"]`（XPath，不限 clickable） |
| `{accessibilityId:"xxx"}` Android | `UiSelector().descriptionContains(xxx)` | `UiSelector().description(xxx)` + fallback `//*[@resource-id="xxx"]` |
| `{testTag:"xxx"}` Android | 不存在 | `//*[@resource-id="xxx"]`（XPath） |
| `{testTag:"xxx"}` iOS | 不存在 | `-ios predicate string:name CONTAINS 'xxx'` |

新增 `findElementWithFallback()` 函数：primary selector 找不到时自动 fallback。

#### 3.2 新增 testTag selector 类型（src/mobile-driver.ts）

```ts
export type Selector =
  | { id: string }
  | { text: string }
  | { accessibilityId: string }
  | { testTag: string }   // 新增
  | { xpath: string }
```

#### 3.3 server 传 udid 给 Appium（dist/server.js + dist/appium-mobile-driver.js）

多设备环境下，`start-session` 的 `udid` 字段之前没有传给 Appium capabilities，导致连到错误的设备。已修复。

#### 3.4 parseSelector 新增 testTag（dist/server.js + src/server.ts）

```js
if (raw.testTag) return { testTag: raw.testTag };
```

#### 3.5 view-tree.js Android content-desc 路由修复（dist/view-tree.js）

dist 版本多了一段 `else if (contentDesc && !testTag) { testTag = contentDesc }` 逻辑（TS 源没有），导致 content-desc 被错误地当成 testTag，文本进不了 `accessibilityText`。已删除使 dist 与 TS 源一致。

---

## 当前状态（已验证）

### iOS（ComposeAllSample，iPhone 17 Pro, UDID=2A4904F7-DDBA-4581-BCC9-3D8ABADDFA30）

- Bundle ID: `com.tencent.kuiklycore.demo.luoyibu`
- 端口：7902（iOS mobile-test server）
- App 需要运行 `debugUIInspector = true` 的页面才能显示类名（ComposeAllSample 已开启）

view-tree 输出示例：
```
├── DivView (XCUIElementTypeButton) testTag="demo_card_ComposeImageDemo" text="I, Image, Image 组件示例"
│   ├── Other (XCUIElementTypeOther)        ← KRBoxShadowView._backgroundView，无类名正常
│   └── DivView (XCUIElementTypeOther)
│       ├── RichTextView (XCUIElementTypeStaticText) text="I"
│       └── DivView (XCUIElementTypeOther)
│           ├── RichTextView text="Image"
│           └── RichTextView text="Image 组件示例"
```

### Android（ComposeAllSample，emulator-5554）

- App Package: `com.tencent.kuikly.android.demo`
- 端口：7900（Android mobile-test server）

view-tree 输出示例：
```
├── TextView testTag="demo_card_ComposeImageDemo" text="I, Image, Image 组件示例" clickable=true
│   └── DivView (android.widget.FrameLayout)
│       ├── RichTextView (android.widget.FrameLayout) text="I"
│       └── DivView
│           ├── RichTextView text="Image"
│           └── RichTextView text="Image 组件示例"
```

---

## 参考文件

开发过程中生成的 view-tree 样例保存在本地 `./logs/mobile_test_snapshot_*.txt`（已 gitignore）。

---

## 已知注意事项

### iOS

1. **`Other` 节点**：Card 下第一个 `Other` 是 `KRBoxShadowView._backgroundView`（背景色+圆角层），坐标与 Card 完全重合，无类名是正常行为
2. **类名显示依赖 `debugUIInspector`**：内部节点（DivView、RichTextView）的类名通过 `AbstractBaseView.didInit()` 注入，只在 Pager 开启 `debugUIInspector=true` 时有效；Compose DSL 节点由 `KuiklySemantisHandler` 下发 testTag+debugName
3. **iOS App 需要重新编译**才能包含 render 层的 accessibility 修复

### Android

1. **RichTextView 显示为 FrameLayout**：如果 `contentDescription` 被文本覆盖（debugName 丢失），type 退化为 FrameLayout。当前修复用 `info.text` 暴露文本，`contentDescription` 保留 debugName
2. **selector `text`**：Android 上 view-tree 的 `text` 字段来自 `content-desc`，selector 用 XPath `//*[@content-desc="xxx"]`，不限 clickable
3. **多设备**：start-session 必须传 `udid` 字段才能精确指定设备

### 关于 mobile-test server 端口

目前 server 只支持单端口（`MOBILE_TEST_PORT` 环境变量，默认 7900），`--android-port/--ios-port` 参数没有被实际解析。需要启动两个进程分别服务 Android 和 iOS：
```bash
MOBILE_TEST_PORT=7900 node dist/server.js  # Android
MOBILE_TEST_PORT=7902 node dist/server.js  # iOS
```

---

---

### 四、双坐标系支持（`.claude/skills/kuikly-mobile-test`）

**需求**：view-tree 节点同时输出屏幕绝对坐标和相对父节点坐标，方便定位滑动问题。

**修复文件**：
- `src/mobile-driver.ts`：`UiTreeNode` 新增 `boundsParent?: [number, number, number, number]`
- `src/view-tree.ts` + `dist/view-tree.js`：
  - `rawNodeToTreeNode` 新增 `parentBounds?` 参数，先解析当前节点 bounds，再递归子节点时传入
  - 计算 `boundsParent = [childX - parentX, childY - parentY, w, h]`
  - `renderTreeText` 输出格式改为 `screen:[x,y,w,h] parent:[px,py,w,h]`

**输出示例**：
```
DivView screen:[0,100,1080,500] parent:[0,100,1080,500]
└── FrameLayout testTag="demo_card" screen:[20,120,780,460] parent:[20,20,780,460]
    └── RichTextView screen:[20,130,380,70] parent:[0,10,380,70]
```

- 根节点只有 `screen`（无父节点，无 `boundsParent`）
- 其余节点同时显示两套坐标

---

## 下一步可能的工作

- 把上述改动整理成 PR 提交
- 完善 mobile-test 工具：支持同时服务 Android + iOS（双端口）
- 为 mobile-test 工具增加文档/测试用例（P0：testTag 用法说明待补充）
- P2：Compose DSL Composable 函数名替换 core 类名（方案待对齐）

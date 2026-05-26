# Bug 自动化测试验证能力分类

本文档按 bug 表现分类，记录每种类型能否用当前 Appium + MobileDriver 方案做自动化验证，以及需要什么补充条件。持续更新。

## 分类总览

| 类别 | 典型表现 | 当前方案 | 补充条件 |
|------|---------|---------|---------|
| 结构类 | 元素缺失、多余、层级错误 | ✅ 可验证 | — |
| 内容类 | 文本错误、值不对、状态不对 | ✅ 可验证 | — |
| 交互类 | 点击无响应、输入异常、手势失效 | ✅ 可验证 | — |
| 几何类 | 布局偏移、越界、重叠、空白 | ⚠️ 部分可验证 | 需 bounds 坐标准确 + assertInViewport |
| 渲染类 | 图片未渲染、圆角缺失、阴影缺失、颜色错误 | ⚠️ P1 SDK后可部分验证 | 需 SDK 暴露样式属性到 accessibility node |
| 竞态类 | 并发崩溃、时序问题 | ⚠️ 部分可验证 | UI 自动化可做基本验证，核心需单元测试 |
| 动画类 | 动画未播放、动画卡顿、动画方向错误 | ⚠️ 部分可验证 | 可断言终态，无法断言过程 |

---

## ✅ 结构类：可验证

元素是否存在、数量是否正确、层级关系是否正确。

| Bug 示例 | 判断方式 |
|---------|---------|
| 页面缺少某个按钮 | `assertVisible({ id: "submit_button" })` 失败 |
| 列表多出一项 | snapshot 中元素数量 > 预期 |
| 弹窗未关闭 | snapshot 中仍能找到弹窗元素 |
| 导航后停留在旧页面 | snapshot 中旧页面元素仍在 |

**前提**：元素有 id、text 或 accessibilityId。

---

## ✅ 内容类：可验证

元素的文本、值、状态是否正确。

| Bug 示例 | 判断方式 |
|---------|---------|
| 文本显示错误 | `assertText("确认")` 实际显示为 "确定" |
| 开关状态不对 | snapshot 中 `enabled: false` 但预期为 `true` |
| 选中项不对 | snapshot 中目标元素的 `selected: true` |
| 输入框内容残留 | snapshot 中 input 元素的 text 值不为空 |

**前提**：内容能通过 accessibility tree 读取。

---

## ✅ 交互类：可验证

用户操作后是否产生预期响应。

| Bug 示例 | 判断方式 |
|---------|---------|
| 点击按钮无响应 | tap 后 assertVisible 结果页元素，失败 |
| 输入框无法输入 | input 后 snapshot 中文本未变化 |
| 返回键无效 | back 后 snapshot 仍在当前页 |
| 滑动翻页失败 | swipe 后 assertVisible 下一页元素，失败 |
| animateScrollTo 未到目标 | 触发滚动后，目标 item 的 text/id 出现在 snapshot 中 |

**前提**：操作可被 Appium 触发，结果可通过 snapshot 断言。

**animateScrollTo 具体验证路径**：

```text
1. 列表项有 text（如 "Item 5"）→ assertText("Item 5") ✅
2. 列表项有 accessibilityId → assertVisible({ accessibilityId: "item_5" }) ✅
3. 列表项无 id 无 text（纯图片）→ 无法精确定位 ❌，需 SDK 补充 testTag(P1)
```

---

## ⚠️ 几何类：部分可验证

布局位置、偏移、越界、空白区域。

| Bug 示例 | 判断方式 | 限制 |
|---------|---------|------|
| 子 View 偏移到非可见区域 | `assertInViewport(selector)` 计算bounds交集 | 需 bounds 坐标准确返回 |
| 旋转屏幕后列表空白 | 旋转后 snapshot 元素数量骤降 | 只能检测元素消失，不能检测渲染白屏 |
| 两个元素重叠遮挡 | bounds 计算交集非空 | 需知道预期不应该重叠 |
| 元素 size 为 0 | snapshot 中 bounds 宽或高为 0 | 需 accessibility tree 上报 size=0 |

**assertInViewport 验证路径**：

```text
1. 获取父容器 bounds → [0, 0, 375, 812]
2. 获取子元素 bounds → [-375, 500, 0, 600]
3. 计算交集 → 无交集 → 子元素在可见区域外 → 判定异常
```

**需要 MVP 验证的假设**：

- [ ] Android UiAutomator2 返回的 bounds 是否为准确的屏幕坐标
- [ ] iOS XCUITest 返回的 rect 是否为准确的屏幕坐标
- [ ] 自定义 View / KuiklyUI 渲染的 View bounds 是否被正确上报

---

## ❌ 渲染类：当前不可验证（P1 SDK 暴露属性后可部分验证）

像素级视觉问题，accessibility tree 默认无法反映。

| Bug 示例 | 为何不可验证 | SDK 暴露后验证方式 |
|---------|-------------|------------------|
| 图片未渲染（控件存在，size 正常，但图片空白） | tree 只知道"有图片控件"，不知道图片是否画出来了 | `imageUrl` / `imageState` 属性 |
| 圆角缺失 | tree 不包含 corner-radius 信息 | `borderRadius` 属性断言 |
| 阴影缺失 | tree 不包含 shadow 信息 | `shadow` 属性断言 |
| 颜色错误 | tree 不包含颜色信息 | `backgroundColor` / `foregroundColor` 属性断言 |
| 渐变方向错误 | tree 不包含渐变信息 | 渐变属性断言(P2) |
| 字体粗细不对 | tree 不包含字体细节 | `fontSize` / `fontWeight` 属性断言 |
| 透明度不对 | tree 只有 visible true/false | `alpha` 属性断言 |

**P1 方案：SDK 暴露运行时样式属性**

详细设计见 [ai-mobile-test-view-tree-and-coordinates.md §6](ai-mobile-test-view-tree-and-coordinates.md#6-p1-功能sdk-暴露运行时属性)

核心思路：
- Android：通过 `AccessibilityNodeInfo.extras(Bundle)` 写入自定义样式键值对
- iOS：通过 `accessibilityHint` 或 `accessibilityValue` 编码 JSON
- Appium 读取后在 view-tree 接口展示

P1 优先暴露的属性：`borderRadius`、`backgroundColor`、`foregroundColor`、`fontSize`、`alpha`

---

## ⚠️ 竞态类：部分可验证

并发、时序相关的 bug。

| Bug 示例 | 判断方式 | 限制 |
|---------|---------|------|
| 键盘监听并发崩溃 | 快速重复触发键盘弹出/收起，观察是否崩溃 | 无法保证复现竞态条件 |
| 快速切换页面崩溃 | 快速反复导航，观察是否崩溃 | 只能做压力测试，不是确定性测试 |
| 数据竞争导致显示错误 | 无法构造确定性的竞争时序 | 需单元测试 |

**UI 自动化可以做的**：反复操作（stress test）观察是否崩溃，但不能保证复现竞态条件。核心竞态问题需要单元测试。

---

## ⚠️ 动画类：部分可验证

| Bug 示例 | 判断方式 | 限制 |
|---------|---------|------|
| 动画未播放，直接跳到终态 | 无法判断（终态可能正确） | 需视频/帧对比 |
| 动画卡顿 | 无法判断 | 需性能采集 |
| animateScrollTo 方向错误 | 滚动后 snapshot 断言终态位置 | 只能断言结果，不能断言动画过程 |
| 手势回弹不正确 | 滑动后 snapshot 断言最终停留页面 | 只能断言结果 |

---

## 按实际 bug 映射

| PR | Bug 描述 | 类别 | 当前方案 | 备注 |
|----|---------|------|---------|------|
| #1321 | SliderPage 折叠屏子项尺寸不响应 | 几何类 | ⚠️ 理论可验证但鸿蒙不在 P0 | — |
| #1315 | iOS pager 拖拽回弹 + 嵌套滚动方向 | 交互类 | ⚠️ 可断言终态 | 精细滑动验证需 P1 |
| #1313 | macOS TextField mouseDown 无效 | 交互类 | ✅ 可验证但 macOS 不在 P0 | — |
| #1314 | macOS Enter 键提交 | 交互类 | ✅ 可验证但 macOS 不在 P0 | — |
| #1282 | Android 键盘监听并发崩溃 | 竞态类 | ⚠️ 压力测试可做 | 核心需单元测试 |
| #1259 | iOS 中文路径图片加载失败 | 渲染类 | ❌ 无法判断图片是否渲染 | 需 SDK 暴露加载状态(P1) |
| #1254 | ViewPager + RecyclerView 滑动冲突 | 交互类 | ✅ 可验证 | 有明确 demo 页 |
| #1240 | scrollEnabled=false 触摸被消费 | 交互类 | ✅ 可验证 | 与 #1254 共用路径 |
| — | animateScrollTo 未到目标 index | 交互类 | ✅ 可验证 | 需元素有 id/text |
| — | 旋转屏幕后列表空白 | 几何类 | ⚠️ 部分可验证 | 元素消失可检测，白屏不可检测 |
| — | 圆角或阴影缺失 | 渲染类 | ⚠️ P1 SDK后可验证 | 需 borderRadius/shadow 属性 |

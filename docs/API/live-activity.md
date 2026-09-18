# 灵动岛（Live Activity）能力接入

iOS 灵动岛（Dynamic Island）与锁屏实时活动（Live Activity）是系统级能力，用于在用户离开 App 后持续展示关键信息。Kuikly 通过 **Module 桥接原生 WidgetKit Extension** 将业务数据送达灵动岛，**UI 本身由原生 SwiftUI 承载**。

## 一、灵动岛的机制

**显示什么**

- 锁屏 / 横幅：完整 UI（文本、进度、图片）
- 灵动岛 compact（默认形态）：leading / trailing 两个极小圆形区，仅放图标 + 数字
- 灵动岛 expanded（长按展开）：leading / trailing / center / bottom 多区域，可放文本、进度、图文
- minimal：多活动并存时的极简圆形

**什么时候显示**

| App 状态 | 是否显示 |
|---------|---------|
| 前台聚焦 | 不显示 |
| 后台 / 主屏幕 | 显示（compact） |
| 其他 App 前台 | 显示（compact，可展开） |
| 锁屏 | 显示横幅 |

> 本质：灵动岛是给"不在 App 内"的用户看的实时入口，其后台运行是 iOS 设计前提，与 Kuikly 渲染循环互不干扰。

**底层技术：跨端只能桥接，不能直出**

灵动岛 UI 由 **WidgetKit Extension（SwiftUI）** 承载，运行在**独立于主 App 的 Extension 进程**，具备独立的渲染环境。Kuikly 的 KMP 产物运行在**主 App 进程**，并不进入 Widget 渲染环境，因此 **Kuikly DSL 无法直接在灵动岛上渲染 UI**。

结论：**跨端侧（Kuikly）负责 App 内的业务逻辑与数据采集，通过 Module 把数据桥接给原生 Extension；灵动岛的 UI 必须由原生 SwiftUI 实现**。这是平台约束，非 Kuikly 限制。

## 二、Kuikly 可以往灵动岛放什么

由上一节可知，灵动岛能显示文本、进度、图片（expanded / 锁屏）。对应地，Kuikly 通过 Module 桥接可推送的数据包括：

- 进度数值（compact 圆形区直接呈现）
- 实时文本（如实时翻译原文 / 译文，在 expanded 与锁屏展示）
- 图片（在 App 内转为 `Data` 后随 `ActivityContent` 传入，在 expanded / 锁屏展示）
- 跳转链接等业务参数

Kuikly **不能**直接编写灵动岛 UI——那是原生 Extension 的职责。

## 三、如何实现

数据流向：

```
Kuikly 页面 → Module.toNative → iOS Bridge Module → Activity.request/update/end → 原生 Widget 渲染
```

实现三块：

1. **原生 Widget Extension**：定义 `ContentState`（业务数据模型，需 `Codable`）+ `ActivityConfiguration` 锁屏布局 + `dynamicIsland` 的 compact / expanded 布局
2. **iOS Bridge Module**：继承 `KRBaseModule`，解析参数、调用 `Activity` API
3. **Kuikly Module**：继承 `Module`，封装 `startActivity` / `updateActivity` / `endActivity`，经 `toNative` 传参

关键要点：

- 发起新活动前先 `end` 已有活动，避免 `targetMaximumExceeded`
- `updateActivity` 采用合并更新，避免只更新一项清空其余字段
- `ActivityContent` 的 `staleDate` 为必填参数

> 完整可运行示例见仓库 `demo` 的 `LiveActivityDemo` 页面与 `LiveActivityModule`，以及 `iosApp` 中的 `KRLiveActivityModule` 与 `KuiklyLiveActivity` Extension。

## 四、业务自定义

扩展自有功能（如实时翻译、带图文章）复用同一套 Module 与 Widget，闭环五步：

1. 在 Widget `ContentState` 增加业务字段（保持 `Codable` + 默认值）
2. 在 Widget 布局对应区域按条件渲染新字段
3. Bridge Module 解析并写入新字段（合并更新）
4. Kuikly Module 新增 / 扩展方法传参
5. 业务页面调用

> 带图文章差异点：图片需以 `Data` 在 App 内下载后随 `ActivityContent` 传入，Widget 用 `Image(uiImage:)` 渲染（Live Activity 无后台网络刷新能力）。

## 常见排查

- 前台看不到灵动岛：符合预期，回主屏 / 锁屏 / 长按展开验证
- `targetMaximumExceeded`：数量超限，发起前先 `end`
- `bundle identifier not prefixed`：Extension Bundle ID 须以主 App 为前缀
- `staleDate` 缺失：构造器必填，传 `Date.distantFuture`

# ohos-back-to-top Specification

## Purpose

定义 HarmonyOS 上"点击状态栏回顶"的行为契约：关闭 ArkUI 内置回顶后，由渲染层感知系统状态栏点击事件并按渲染实例分发，业务注册回调时交由业务处理，未注册回调时保持默认回顶行为，使 OHOS 与 iOS/Android 的 `scrollToTop` 语义一致。

## ADDED Requirements

### Requirement: OHOS 关闭系统内置回顶能力

在 HarmonyOS 且系统 API 版本支持该属性（API 15 及以上）时，渲染层 SHALL 关闭 ArkUI 滚动容器内置的"点击状态栏回顶"能力，使回顶行为完全由框架控制；在不支持该属性的低版本系统上，渲染层 SHALL NOT 因该属性缺失而改变原有滚动行为。

#### Scenario: 支持该属性的系统上关闭内置回顶

- **GIVEN** 设备为 HarmonyOS 且系统 API 版本不低于 15
- **WHEN** 渲染层创建滚动容器
- **THEN** 该滚动容器 SHALL 被设置为不响应系统状态栏点击回顶
- **AND** 点击状态栏时系统 SHALL NOT 自行滚动该容器

#### Scenario: 低版本系统降级

- **GIVEN** 设备为 HarmonyOS 且系统 API 版本低于 15
- **WHEN** 渲染层创建滚动容器
- **THEN** 渲染层 SHALL 跳过该属性的设置
- **AND** 滚动容器的滚动行为 SHALL 与改动前保持一致

### Requirement: 状态栏点击事件可被渲染层感知

渲染层 SHALL 订阅系统状态栏点击事件；当事件发生时，渲染层 SHALL 按事件名取出已登记的渲染实例标识，并仅向这些实例分发。渲染层 SHALL NOT 依赖系统内置回顶行为来判断事件是否发生。

#### Scenario: 订阅成功后收到系统事件

- **GIVEN** 至少有一个渲染实例已完成状态栏点击事件的订阅
- **WHEN** 用户点击系统状态栏
- **THEN** 渲染层 SHALL 收到该公共事件
- **AND** 渲染层 SHALL 能读取到事件名以区分事件类型

#### Scenario: 未创建或已销毁的实例被忽略

- **GIVEN** 事件发生时某个已登记实例标识对应的渲染实例不存在（未创建完成或已销毁）
- **WHEN** 渲染层分发该事件
- **THEN** 渲染层 SHALL 忽略该实例标识
- **AND** 其余有效实例的接收 SHALL NOT 受影响

### Requirement: 仅活跃实例响应状态栏点击

渲染实例 SHALL 根据页面可见性维护自身活跃状态：页面可见时标记为活跃，页面不可见时标记为非活跃。渲染层 SHALL 仅在实例处于活跃状态时处理状态栏点击事件。

#### Scenario: 活跃实例响应

- **GIVEN** 某渲染实例的页面已可见（收到可见生命周期通知）且尚未不可见
- **WHEN** 状态栏点击事件到达该实例
- **THEN** 该实例 SHALL 执行回顶处理

#### Scenario: 非活跃实例不响应

- **GIVEN** 某渲染实例的页面已不可见（收到不可见生命周期通知）
- **WHEN** 状态栏点击事件到达该实例
- **THEN** 该实例 SHALL NOT 执行任何回顶处理

#### Scenario: 页面重新可见后恢复响应

- **GIVEN** 某渲染实例曾收到不可见通知
- **WHEN** 该实例再次收到可见通知，随后发生状态栏点击
- **THEN** 该实例 SHALL 恢复执行回顶处理

### Requirement: 注册 scrollToTop 回调时由业务接管

当业务为滚动容器注册了 `scrollToTop` 回调时，收到状态栏点击后框架 SHALL 触发该回调，且 SHALL NOT 由渲染层自动滚动该容器（对齐 iOS `scrollViewShouldScrollToTop:` 返回 `NO` 的语义）。

#### Scenario: 业务回调被触发且不自动回顶

- **GIVEN** 某滚动容器已注册 `scrollToTop` 回调，且页面处于活跃状态，容器未处于顶部
- **WHEN** 用户点击系统状态栏
- **THEN** 业务注册的 `scrollToTop` 回调 SHALL 被调用
- **AND** 渲染层 SHALL NOT 自动将该容器滚动到顶部
- **AND** 容器偏移量 SHALL 保持业务处理前的值

### Requirement: 未注册回调时保持默认回顶行为

当业务未为滚动容器注册 `scrollToTop` 回调时，收到状态栏点击后框架 SHALL 将容器滚动到顶部，以补齐被关闭的系统内置行为。

#### Scenario: 默认回顶

- **GIVEN** 某滚动容器未注册 `scrollToTop` 回调，且页面处于活跃状态，容器未处于顶部
- **WHEN** 用户点击系统状态栏
- **THEN** 该容器 SHALL 被滚动至顶部（偏移量为 0）

#### Scenario: 已在顶部时无副作用

- **GIVEN** 某滚动容器已位于顶部
- **WHEN** 用户点击系统状态栏
- **THEN** 容器偏移量 SHALL 保持为 0

### Requirement: 同一页面内多个滚动容器均被覆盖

状态栏点击处理 SHALL 覆盖该渲染实例视图树内的全部滚动容器（包括由同一实现承载的列表类容器），而不仅是最外层容器。

#### Scenario: 多个滚动容器同时回顶

- **GIVEN** 同一活跃页面内存在多个滚动容器，且均未注册 `scrollToTop` 回调、均未处于顶部
- **WHEN** 用户点击系统状态栏
- **THEN** 每个滚动容器 SHALL 被滚动至顶部

#### Scenario: 混合场景

- **GIVEN** 同一活跃页面内既有注册了 `scrollToTop` 回调的容器，也有未注册的容器
- **WHEN** 用户点击系统状态栏
- **THEN** 已注册回调的容器 SHALL 触发回调且自身不被自动回顶
- **AND** 未注册回调的容器 SHALL 被滚动至顶部

### Requirement: 订阅按实例引用计数，避免空订阅常驻

渲染层 SHALL 为每个系统事件维护"事件名 → 已订阅实例集合"的映射：首个实例订阅时向系统发起订阅，集合中最后一个实例退订后再向系统退订；集合非空期间 SHALL NOT 重复向系统订阅或提前退订。

#### Scenario: 首个订阅者触发系统订阅

- **GIVEN** 某系统事件当前没有任何订阅实例
- **WHEN** 第一个渲染实例完成该事件的订阅
- **THEN** 渲染层 SHALL 向系统发起一次订阅

#### Scenario: 多实例共享订阅

- **GIVEN** 已有实例订阅了某系统事件
- **WHEN** 第二个渲染实例订阅同一事件
- **THEN** 渲染层 SHALL 仅登记该实例
- **AND** 渲染层 SHALL NOT 重复向系统发起订阅

#### Scenario: 最后一个实例退订后释放系统订阅

- **GIVEN** 某系统事件仅剩最后一个订阅实例
- **WHEN** 该实例退订（如实例销毁）
- **THEN** 渲染层 SHALL 从映射中移除该实例
- **AND** 渲染层 SHALL 向系统发起一次退订

#### Scenario: 仍有其他实例时保留系统订阅

- **GIVEN** 某系统事件有多个订阅实例
- **WHEN** 其中一个实例退订
- **THEN** 渲染层 SHALL NOT 向系统退订
- **AND** 其余实例 SHALL 继续能收到该事件

### Requirement: 其他平台与既有 API 行为不变

本能力 SHALL 仅影响 HarmonyOS 渲染层；iOS、Android、Web、小程序的状态栏点击回顶行为 SHALL NOT 改变；业务侧既有 API（`scrollToTop` 事件、Compose `Modifier.scrollToTop`）签名与语义 SHALL NOT 改变。

#### Scenario: iOS 行为不变

- **GIVEN** 业务未在本改动中修改 iOS 代码
- **WHEN** 在 iOS 上点击状态栏
- **THEN** 已注册 `scrollToTop` 回调时仍保持"回调被触发且系统不自动回顶"的既有行为
- **AND** 未注册回调时仍保持系统默认回顶行为

#### Scenario: 业务 API 兼容

- **GIVEN** 业务代码已使用 `scrollToTop` 事件或在 Compose 中使用 `Modifier.scrollToTop`
- **WHEN** 运行在 HarmonyOS 上
- **THEN** 该 API SHALL 无需修改即可在 HarmonyOS 上生效
- **AND** 其他平台上该 API 的行为 SHALL NOT 改变

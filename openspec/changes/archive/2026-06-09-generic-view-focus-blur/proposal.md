## Why

KuiklyUI 当前只有 InputView / TextAreaView / AutoHeightTextAreaView 支持 focus/blur 操作，但实际开发中经常需要对任意 View 进行焦点管理（如卡片选中、自定义输入容器等）。缺少通用焦点支持导致开发者需要通过 hack 方式实现焦点控制。

## What Changes

- 在 `DeclarativeBaseView` 基类新增 `focus()` 和 `blur()` 方法，使任意 View 都能获取/失去焦点
- 新增 `VIEW_FOCUS_CHANGE` 事件名，统一焦点变化事件类型
- 在基类 `Event` 中新增 `onFocusChange(handler)` 事件注册方法
- InputView / TextAreaView / AutoHeightTextAreaView 的 focus/blur 添加 `override` 修饰符
- Android 端 `KRView` 实现 focus/blur callMethod 和 focusChange 事件分发

## Non-goals

- 不改 iOS / HarmonyOS 渲染器（后续跟进）
- 不改 Web 渲染器
- 不做焦点顺序管理（Tab navigation）

## Capabilities

### New Capabilities

- **generic-view-focus**: 任意 View 支持 focus()/blur() 和 onFocusChange 事件

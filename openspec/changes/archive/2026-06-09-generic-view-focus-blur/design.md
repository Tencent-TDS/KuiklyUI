## Design

### 核心层改动

在 `DeclarativeBaseView` 基类添加 `open fun focus()` 和 `open fun blur()`，通过 callMethod 向 native 发送焦点请求。已有的 InputView/TextAreaView/AutoHeightTextAreaView 改为 `override`。

### 事件机制

新增 `EventName.VIEW_FOCUS_CHANGE("viewFocusChange")` 枚举值，在基类 `Event` 中提供 `onFocusChange(handler: (isFocused: Boolean) -> Unit)` 注册方法，回调参数通过解析 JSON `{ "isFocused": true/false }` 获取。

### Android 渲染器

`KRView` 中：
- 处理 `focus` callMethod：设置 view.isFocusable = true + view.isFocusableInTouchMode = true，然后 requestFocus()
- 处理 `blur` callMethod：clearFocus()
- 添加 OnFocusChangeListener，焦点变化时通过 `sendEvent("viewFocusChange", json)` 上报

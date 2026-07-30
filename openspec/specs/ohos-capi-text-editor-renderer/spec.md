## Purpose

TBD.

## Requirements

### Requirement: 新节点类型创建

`core-render-ohos` SHALL 新增两个 `IKRRenderViewExport` 子类 `KRTextEditorFieldView` / `KRTextEditorAreaView`，**基于** `ARKUI_NODE_TEXT_EDITOR` 节点类型创建原生节点。

#### Scenario: 单行创建（HarmonyOS）

- **GIVEN** 设备运行 HarmonyOS API 24 及以上
- **WHEN** `KRTextEditorFieldView::CreateNode()` 被调用
- **THEN** 该方法 SHALL 返回 `kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_TEXT_EDITOR)` 的结果
- **AND** `DidInit()` SHALL 设置 `NODE_TEXT_EDITOR_MAX_LINES = 1`（或等价属性），强制单行显示

#### Scenario: 多行创建（HarmonyOS）

- **GIVEN** 设备运行 HarmonyOS API 24 及以上
- **WHEN** `KRTextEditorAreaView::CreateNode()` 被调用
- **THEN** 该方法 SHALL 返回 `kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_TEXT_EDITOR)` 的结果
- **AND** `DidInit()` SHALL **不设置** `MAX_LINES`，允许多行显示

#### Scenario: SDK header 不可用兜底

- **GIVEN** 编译使用的 SDK header 未包含 `ARKUI_NODE_TEXT_EDITOR` 枚举（`KUIKLY_TEXT_EDITOR_AVAILABLE` 宏为 `0`）
- **WHEN** `KRTextEditorFieldView::CreateNode()` / `KRTextEditorAreaView::CreateNode()` 被调用
- **THEN** 该方法 SHALL 返回 `nullptr`
- **AND** 注册闭包的运行时分支（见 `ohos-text-input-runtime-switch`）SHALL 保证永不调用到新实现，避免返回 `nullptr` 到渲染管线

### Requirement: 组件结构与继承链

`KRTextEditorAreaView` SHALL 继承自 `KRTextEditorFieldView`（与老实现 `KRTextAreaView : KRTextFieldView` 的组织惯例保持一致）。两者 SHALL 都 **不继承**老实现 `KRTextFieldView` / `KRTextAreaView`，以避免 `ARKUI_NODE_TEXT_INPUT` 相关虚函数耦合到新节点类型。

#### Scenario: 类继承链

- **GIVEN** 代码静态分析
- **WHEN** 检查两个新类的继承链
- **THEN** `KRTextEditorFieldView` SHALL 直接继承 `IKRRenderViewExport`
- **AND** `KRTextEditorAreaView` SHALL 继承 `KRTextEditorFieldView`
- **AND** 两者 SHALL **不继承** `KRTextFieldView` 或 `KRTextAreaView`

#### Scenario: 单 / 多行差异通过虚函数 hook 表达

- **GIVEN** `KRTextEditorFieldView` 提供 `IsSingleLine()` / `InterceptNewline()` 虚函数（默认返回 `true`）
- **WHEN** `KRTextEditorAreaView` 声明时
- **THEN** 多行类 SHALL override 两个虚函数返回 `false`
- **AND** 其他共享逻辑（属性映射、事件分发、长度过滤）SHALL 由基类统一提供

### Requirement: 共享逻辑抽出

`core-render-ohos` SHALL 新增 `KRTextEditorCommon.h`（必要时可配 cpp），以 `namespace kuikly::text_editor` 组织单 / 多行共享的工具函数、属性键名常量、事件类型常量、以及状态结构 `KRTextEditorState`。

#### Scenario: 共享工具函数

- **GIVEN** `KRTextEditorFieldView.cpp` 与 `KRTextEditorAreaView.cpp`
- **WHEN** 需要执行 UTF-8/UTF-16 长度计算、文本截断、属性键查找、事件类型映射等通用操作
- **THEN** 两者 SHALL 从 `kuikly::text_editor` 命名空间调用同一个函数，不复制代码到各 cpp
- **AND** 单 / 多行特有逻辑（`maxLines`、IME 回车拦截）SHALL 各自在子类中实现

#### Scenario: 共享状态结构

- **GIVEN** `KRTextEditorFieldView` 与 `KRTextEditorAreaView`
- **WHEN** 需要持有 `max_length_` / `length_limit_type_` / `font_size_` / `font_weight_` / 事件回调等 per-view 状态
- **THEN** 两者 SHALL 各自持有一个 `kuikly::text_editor::KRTextEditorState` 实例成员 `state_`
- **AND** 共享工具函数 SHALL 接收 `KRTextEditorState&` 参数操作状态

### Requirement: 属性映射对齐老实现

`KRTextEditorFieldView` / `KRTextEditorAreaView` SHALL 对齐 `KRTextFieldView` / `KRTextAreaView` 的全部已有属性键，`setProp` 的行为 SHALL 与老实现**可见等价**。

#### Scenario: 属性键覆盖

- **GIVEN** core 层通过 `setProp` 下发以下属性键
- **THEN** 新实现 SHALL 支持并正确处理：`text` / `placeholder` / `placeholderColor` / `fontSize` / `fontWeight` / `color` / `editable` / `tintColor` / `textAlign` / `keyboardType` / `returnKeyType` / `maxTextLength` / `lengthLimitType` / `autoHideKeyboardOnImeAction`
- **AND** 每个属性在 API 24+ 设备上的可见效果 SHALL 与 API 23 设备上的老实现等价

#### Scenario: 字体设置

- **GIVEN** `setProp("fontSize", 16)` 和 `setProp("fontWeight", 500)` 被依次调用
- **WHEN** 新实现处理这些属性
- **THEN** SHALL 调用 `kuikly::text_editor::SetFont(node, state_.font_size_, state_.font_weight_, /*is_field=*/true|false)`
- **AND** 占位字体 SHALL 通过 `UpdatePlaceholderFont` 同步更新（与老实现一致，若 core 未单独下发 placeholder 字体）

#### Scenario: 最大长度与长度限制类型

- **GIVEN** `setProp("lengthLimitType", 0)`（BYTE）和 `setProp("maxTextLength", 10)` 被下发
- **WHEN** 新实现处理
- **THEN** SHALL 按 `BYTE` 口径计算文本长度
- **AND** 在 `ON_WILL_INSERT` / `ON_WILL_CHANGE` / `ON_PASTE` 事件中手动过滤超限输入
- **AND** 超限时 SHALL 触发 `textLengthBeyondLimit` 事件

- **GIVEN** `setProp("maxTextLength", 10)` 但未设置 `lengthLimitType`（`length_limit_type_ == -1`）
- **THEN** SHALL 使用节点级属性（`NODE_TEXT_EDITOR_MAX_LENGTH` 或等价）直接限制
- **AND** SHALL 不启用手动过滤回调（性能优先）

#### Scenario: keyboardType

- **GIVEN** `KRTextEditorFieldView` 或 `KRTextEditorAreaView` 收到 `setProp("keyboardType", X)`（X 为 `number` / `email` / `phone` / `password` 等非 default 值）
- **WHEN** 新实现处理
- **THEN** SHALL 打 warn 日志（`ARKUI_NODE_TEXT_EDITOR` 不提供 keyboardType 属性）
- **AND** SHALL 降级为系统默认键盘（本属性对新节点无感知）

- **GIVEN** `KRTextEditorAreaView`（多行）收到 `setProp("keyboardType", "password")`
- **THEN** SHALL 打 warn 日志并降级为默认键盘（与老 `KRTextAreaView` 行为一致）

#### Scenario: autoHideKeyboardOnImeAction（单行专属）

- **GIVEN** `KRTextEditorFieldView` 配置 `autoHideKeyboardOnImeAction = true`
- **WHEN** 用户按下 IME 回车触发 `inputReturn` 事件
- **THEN** 组件 SHALL 主动触发 `Blur()` 收起键盘

- **GIVEN** `KRTextEditorAreaView` 收到同属性
- **THEN** SHALL 记录 `state_.auto_hide_KeyBoard_on_ImeAction_` 但不强制隐藏（对齐老实现，多行中该属性一般不生效）

### Requirement: 事件映射对齐老实现

新实现 SHALL 对齐 capi 老实现的事件集，事件名、触发时机、payload 与老实现等价。

#### Scenario: 事件集覆盖

- **GIVEN** core 层订阅事件
- **THEN** 新实现 SHALL 支持：`textDidChange` / `inputFocus` / `inputBlur` / `inputReturn` / `textLengthBeyondLimit` / `keyboardHeightChange`
- **AND** 事件触发条件与 payload 结构 SHALL 与 `KRTextFieldView` / `KRTextAreaView` 等价

#### Scenario: textDidChange 默认注册

- **GIVEN** `DidInit()` 被调用
- **THEN** SHALL 默认注册 `NODE_TEXT_EDITOR_ON_DID_CHANGE` 事件（TEXT_EDITOR 无 `ON_CHANGE` 专属事件）
- **AND** 由于 `ON_DID_CHANGE` 的 payload 不包含文本，回调内 SHALL 主动调 `OH_ArkUI_TextEditorStyledStringController_GetStyledString` 读全文后再触发业务 `textDidChange`
- **AND** 其他事件（focus / blur / submit / will-change / paste）SHALL 在对应 `setProp` 的事件回调非空时**按需注册**，避免无用回调
- **AND** `inputFocus` / `inputBlur` SHALL 使用通用的 `NODE_ON_FOCUS` / `NODE_ON_BLUR`（TEXT_EDITOR 未提供专属 focus / blur 事件）

#### Scenario: inputReturn（Field 单行）

- **GIVEN** `KRTextEditorFieldView` 用户按下 IME 回车
- **WHEN** `NODE_TEXT_EDITOR_ON_SUBMIT` 触发
- **THEN** SHALL 触发 `inputReturn` 事件
- **AND** 若 `state_.auto_hide_KeyBoard_on_ImeAction_ == true`，SHALL 额外调用 `Blur()`

#### Scenario: 单行换行拦截

- **GIVEN** `KRTextEditorFieldView` 用户按下系统键盘的 Enter 导致 `\n` 即将插入
- **WHEN** `NODE_TEXT_EDITOR_ON_WILL_CHANGE` 触发（TEXT_EDITOR 无 `ON_WILL_INSERT` 专属事件，统一走 `ON_WILL_CHANGE`）
- **AND** 组件通过 `OH_ArkUI_TextEditorChangeEvent_GetReplacementStyledString` 取到替换串，若包含 `\n`
- **THEN** SHALL 通过 `OH_ArkUI_NodeEvent_SetReturnNumberValue({i32=0}, 1)` 拒绝该变更
- **AND** SHALL 不阻断 `NODE_TEXT_EDITOR_ON_SUBMIT` 的正常触发（业务订阅的 `inputReturn` 仍由 Submit 事件提供）

#### Scenario: 多行允许换行

- **GIVEN** `KRTextEditorAreaView` 用户按下系统键盘的 Enter
- **WHEN** `NODE_TEXT_EDITOR_ON_WILL_CHANGE` 触发
- **THEN** SHALL **不**拦截 `\n`，允许换行插入
- **AND** SHALL 正常触发 `textDidChange` 事件

#### Scenario: textLengthBeyondLimit

- **GIVEN** 当前文本长度已达 `maxTextLength` 限制
- **WHEN** 用户尝试继续输入
- **THEN** SHALL 拦截输入
- **AND** SHALL 触发 `textLengthBeyondLimit` 事件

### Requirement: 方法映射对齐老实现

`KRTextEditorFieldView` / `KRTextEditorAreaView` 的 `CallMethod` SHALL 支持与老实现一致的方法集。

#### Scenario: 方法集覆盖

- **GIVEN** core 层通过 `callMethod` 调用方法
- **THEN** 以下方法 SHALL 被支持：
  - `focus` → 节点进入编辑态（触发 `inputFocus`）
  - `blur` → 节点离开编辑态（触发 `inputBlur`）
  - `setText` → 替换节点文本内容；等价于调 `UpdateContentText`
  - `getCursorIndex` → 回传当前光标位置（UTF-16 index）通过 callback 返回 `{cursorIndex: N}`
  - `setCursorIndex` → 将光标设置到指定 UTF-16 index

#### Scenario: 光标读写

- **GIVEN** 用户在文本 "AB中CD" 的 "中" 之后点击
- **WHEN** core 层调 `getCursorIndex`
- **THEN** callback SHALL 返回 `{cursorIndex: 3}`（A/B/中 各占 1 个 UTF-16 code unit）

- **GIVEN** 当前文本为 "AB中CD"
- **WHEN** core 层调 `setCursorIndex(2)`
- **THEN** 节点选区起点 SHALL 被设为 UTF-16 offset 2（即 "A" 之后、"B" 之前）

### Requirement: 默认样式对齐老实现

`KRTextEditorFieldView` / `KRTextEditorAreaView` 在 `DidInit()` 中 SHALL 显式设置以下默认样式，确保与老实现视觉一致。

#### Scenario: 默认背景 / 圆角 / padding

- **GIVEN** 新 View 被创建
- **WHEN** `DidInit()` 执行
- **THEN** SHALL 设置背景为透明
- **AND** SHALL 设置 `borderRadius` 为 0（消除系统默认圆角）
- **AND** SHALL 设置 `padding` 上下左右为 0（消除系统默认 padding）

#### Scenario: 默认字体

- **GIVEN** 新 View 刚创建
- **THEN** SHALL 设置默认 `font_size_ = 15`、`font_weight_ = ARKUI_FONT_WEIGHT_NORMAL`（与老实现一致）

#### Scenario: 默认 BlurOnSubmit（Field）

- **GIVEN** `KRTextEditorFieldView::DidInit()` 执行
- **THEN** SHALL 设置节点 `BlurOnSubmit` 为 `false`（对齐老实现：回车默认不收键盘，由 `autoHideKeyboardOnImeAction` 控制）

### Requirement: 文本长度计算算法

`KRTextEditorCommon` SHALL 提供与老实现等价的三种长度计算口径。

#### Scenario: BYTE 计数

- **GIVEN** 文本为 "中国人"（UTF-8 每字 3 字节）
- **WHEN** 按 `BYTE` 计算长度
- **THEN** 长度 SHALL 为 9

#### Scenario: CHARACTER 计数

- **GIVEN** 文本为 "中国人abc"
- **WHEN** 按 `CHARACTER`（Unicode code point）计算长度
- **THEN** 长度 SHALL 为 6

#### Scenario: VISUAL_WIDTH 计数

- **GIVEN** 文本为 "中国人abc"
- **WHEN** 按 `VISUAL_WIDTH` 计算长度（ASCII=1，非 ASCII=2）
- **THEN** 长度 SHALL 为 2×3 + 1×3 = 9

### Requirement: 生命周期与资源清理

新 View 在销毁时 SHALL 正确释放资源。

#### Scenario: OnDestroy 清理键盘观察者

- **GIVEN** 组件曾调用 `keyboardHeightChange` 相关订阅
- **WHEN** `OnDestroy()` 被触发
- **THEN** SHALL 调用 `KRKeyboardManager::GetInstance().RemoveKeyboardTask(windowId, key)`
- **AND** SHALL 清空 `state_.keyboard_height_changed_callback_`
- **AND** SHALL 清空其他所有事件回调引用，避免悬挂指针

### Requirement: 日志可观测

为便于现场诊断分支命中，新实现 SHALL 在首次创建 View 时通过 `KR_LOG`（或项目既有 logging 宏）打印一次版本诊断信息。

#### Scenario: 首次创建日志

- **GIVEN** 进程启动后第一次创建 `KRTextEditorFieldView` 实例
- **WHEN** `DidInit()` 首次被触发
- **THEN** SHALL 通过 `static bool logged` 保护的方式打印一次日志："KRTextEditorFieldView initialized (ARKUI_NODE_TEXT_EDITOR, apiVersion=X)"
- **AND** 后续创建 SHALL 不再重复打印

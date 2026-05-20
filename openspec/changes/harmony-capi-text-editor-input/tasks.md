## 0. 设计偏差说明（实施期确认）

> ⚠️ 实施期根据 API 24 SDK header 与官方示例 [TextEditorMaker.cpp](https://gitcode.com/openharmony/applications_app_samples/blob/master/code/DocsSample/ArkUISample/native_node_sample/entry/src/main/cpp/TextEditorMaker.cpp) 对原 design.md / spec 做如下偏差：
>
> 1. **`ARKUI_NODE_TEXT_EDITOR` 属性模型与 `ARKUI_NODE_TEXT_INPUT` 完全不同**：没有 `NODE_TEXT_EDITOR_TEXT/PLACEHOLDER_COLOR/FONT_COLOR/TEXT_ALIGN/KEYBOARD_TYPE` 等属性，文本 / 样式 / 段落对齐走 **`OH_ArkUI_TextEditorStyledStringController` + `TextEditorTextStyle` + `TextEditorParagraphStyle`**；占位符走 **`OH_ArkUI_TextEditorPlaceholderOptions`** 结构体 → 通过 `NODE_TEXT_EDITOR_PLACEHOLDER` 属性的 `.object` 绑定。
> 2. **TEXT_EDITOR 无 keyboardType 映射**：`number/email/phone/password` 无对应枚举 → 按 1A 方案：API 24+ 路径下打 warn 日志，降级为默认键盘。
> 3. **TEXT_EDITOR 无 `ON_FOCUS/ON_BLUR/ON_CHANGE/ON_WILL_INSERT` 专属事件**：`focus/blur` 使用通用 `NODE_ON_FOCUS / NODE_ON_BLUR`；`textDidChange` 走 `NODE_TEXT_EDITOR_ON_DID_CHANGE`（事件 payload 不含文本，通过 `GetStyledString` 读全文）；`onWillInsert` 并入 `NODE_TEXT_EDITOR_ON_WILL_CHANGE`。
> 4. **TEXT_EDITOR `ON_PASTE` 不提供文本**：按 2A 方案：`ON_PASTE` 不拦截，统一由 `ON_WILL_CHANGE.GetReplacementStyledString` 路径做超限过滤。
> 5. **单 / 多行结构最终采用继承**（`KRTextEditorAreaView : public KRTextEditorFieldView`）而非 design.md 原"并列"方案。原因：实际差异点仅 4 处（`SingleLine`、换行拦截、password warn、lineHeight），继承可避免 ~400 行重复代码；与老 `KRTextAreaView : KRTextFieldView` 的组织惯例也一致。设计文档 / spec 已相应同步。
> 6. **初始文本字体样式**按 3A 方案：在 state 中缓存 fontSize/Weight/Color/textAlign，`SetProp` 时先更新 state → `ApplyTypingStyle` 写回 controller；`text` 改变时重建 descriptor（纯文本 + typing style 生效）。

## 1. 探查 API 24 SDK header

- [x] 1.1 定位 `ARKUI_NODE_TEXT_EDITOR` 枚举值（`native_node.h`）
- [x] 1.2 列出 `NODE_TEXT_EDITOR_*` 属性枚举（实际：`ENTER_KEY_TYPE` / `CARET_COLOR` / `SCROLL_BAR_COLOR` / `BAR_STATE` / `PLACEHOLDER` / `STYLED_STRING_CONTROLLER` / `SELECTED_BACKGROUND_COLOR` / `MAX_LENGTH` / `MAX_LINES` / `KEYBOARD_APPEARANCE` / `SINGLE_LINE` 等）
- [x] 1.3 列出事件枚举（实际：`ON_SELECTION_CHANGE` / `ON_READY` / `ON_PASTE` / `ON_EDITING_CHANGE` / `ON_SUBMIT` / `ON_CUT` / `ON_COPY` / `ON_WILL_CHANGE` / `ON_DID_CHANGE`；**无** `ON_FOCUS/ON_BLUR/ON_CHANGE/ON_WILL_INSERT`）
- [x] 1.4 确认：`kuikly::util::*` 的通用工具（`UpdateNodeBackgroundColor` / `UpdateNodeBorderRadius` / `SetArkUIPadding`）直接适用；`UpdateInputNode*` 系列（硬编码 `NODE_TEXT_INPUT_*`）**不复用**，在 `KRTextEditorCommon` 中另行实现

## 2. KRTextEditorCommon 工具头

- [x] 2.1 新建 [KRTextEditorCommon.h](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorCommon.h)
- [x] 2.2 SDK header guard：始终显式定义 `KUIKLY_TEXT_EDITOR_AVAILABLE`为 0/1（`OH_CURRENT_API_VERSION >= 24` → `1`，否则 `0`）
- [x] 2.3 所有 TEXT_EDITOR API 调用被 `#if KUIKLY_TEXT_EDITOR_AVAILABLE` 保护
- [x] 2.4 `namespace kuikly::text_editor`
- [x] 2.5 属性键常量：`kText` / `kPlaceholder` / `kPlaceholderColor` / `kFontSize` / `kFontWeight` / `kColor` / `kEditable` / `kTintColor` / `kTextAlign` / `kKeyboardType` / `kReturnKeyType` / `kMaxTextLength` / `kLengthLimitType` / `kAutoHideKeyBoardOnIMEAction` / `kLineHeight`
- [x] 2.6 方法 / 事件名常量（与老实现完全一致）
- [x] 2.7 `struct KRTextEditorState`：font / max_length / length_limit / 6 回调 + `controller_` + `cached_text_` + `enter_key_type_`
- [x] 2.8 `CreateTextStyleFromState` / `CreateParagraphStyleFromState`（typing style / paragraph style 生成）
- [x] 2.9 `ApplyTypingStyle(state)` / `SetStyledText(state, text)` / `GetStyledText(state)` / `ApplyPlaceholder(node, state)` / `UpdateCaretColor` / `UpdateEnterKeyType` / `UpdateMaxLengthAttr` / `ResetMaxLengthAttr` / `UpdateMaxLines` / `UpdateSingleLine` / `UpdateFocusStatus` / `UpdateFocusable`
- [x] 2.10 `GetCaretOffset(state)` / `SetCaretOffset(state, offset)`（controller 级 API）
- [x] 2.11 `SetStyledText` / `GetStyledText`（通过 `OH_ArkUI_StyledString_Descriptor_CreateWithString / SetStyledString / GetStyledString / Descriptor_GetString`）
- [x] 2.12 复刻长度计算工具：`GetUTF8ByteLengthOfFirstCharacter` / `GetUTF8ByteLengthOfCodePoint` / `GetVisualWidthOfCodePoint` / `GetUTF16Length` / `GetUTF8ByteCount` / `CalculateTextLength` / `CalculateTruncateIndex`
- [x] 2.13 `FilterSource(source[], dest, dStart, dEnd, state) -> bool`（语义与老 `KRTextFieldView::filter` 完全一致）
- [x] 2.14 Common SetProp 集中处理：偏差说明：因为 Area 子类差异少，SetProp 集中逻辑直接放在 `KRTextEditorFieldView::SetProp` 中，Area 仅追加 `lineHeight`；未引入 `HandleSetPropCommon` 独立 free function（维持当前设计更简洁）

## 3. KRTextEditorFieldView 单行实现

- [x] 3.1 新建 [KRTextEditorFieldView.h](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorFieldView.h)
- [x] 3.2 include `IKRRenderViewExport.h` / `KRTextEditorCommon.h`
- [x] 3.3 `class KRTextEditorFieldView : public IKRRenderViewExport`，覆盖 6 个方法
- [x] 3.4 `protected: kuikly::text_editor::KRTextEditorState state_`
- [x] 3.5 声明所有私有方法：`Focus/Blur/GetCursorIndex/SetCursorIndex/OnTextDidChanged/OnInputFocus/OnInputBlur/OnInputReturn/OnWillChangeText/OnPasteText/SetContentText/GetContentText/LimitInputContentTextInMaxLength/NotifyTextLengthBeyondLimit/SetupLengthInputFilter/DoResetMaxLength/ApplyKeyboardType/ApplyReturnKeyType`；新增 `IsSingleLine()` / `InterceptNewline()` 虚函数供子类覆盖
- [x] 3.6 新建 [KRTextEditorFieldView.cpp](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorFieldView.cpp)
- [x] 3.7 `CreateNode()`：`ARKUI_NODE_TEXT_EDITOR`，带 `KUIKLY_TEXT_EDITOR_AVAILABLE == 0` 兜底
- [x] 3.8 `DidInit()`：背景透明 / 圆角 0 / padding 0 / SingleLine=true / 创建并绑定 controller / 默认注册 `ON_DID_CHANGE` / 首次 ApplyTypingStyle / 首次日志
- [x] 3.9 `OnDestroy()`：清理 keyboard observer + Destroy controller
- [x] 3.10 `SetProp()`：text/placeholder/placeholderColor/fontSize/fontWeight/color/tintColor/textAlign/editable/keyboardType(降级warn)/returnKeyType/lengthLimitType/maxTextLength/autoHideKeyboardOnImeAction + 6 个 event callback
- [x] 3.11 `OnEvent()`：dispatch ON_DID_CHANGE/ON_FOCUS/ON_BLUR/ON_SUBMIT/ON_WILL_CHANGE/ON_PASTE
- [x] 3.12 `CallMethod()`：focus/blur/setText/getCursorIndex/setCursorIndex
- [x] 3.13 `Focus()` 走 `NODE_FOCUS_STATUS=1`；`Blur()` 优先走 `StopEditing`（controller 级），兜底 `NODE_FOCUS_STATUS=0`
- [x] 3.14 `OnTextDidChanged`：主动 `GetStyledText` 读全文 + 超限截断 + 回调
- [x] 3.15 `OnInputFocus/OnInputBlur/OnInputReturn`（`inputReturn` 回调后按 `auto_hide_KeyBoard_on_ImeAction_` 决定 `Blur()`）
- [x] 3.16 max-length 过滤：`OnWillChangeText` 拦截（`GetRangeBefore` + `GetReplacementStyledString` + `FilterSource` + `SetReturnNumberValue(0/1)`）；换行符在单行时拦截；`OnPasteText` 不拦截（由 ON_WILL_CHANGE 兜底）
- [x] 3.17 `LimitInputContentTextInMaxLength` / `NotifyTextLengthBeyondLimit`（对齐老实现双路径）
- [x] 3.18（追加）`ApplyReturnKeyType`：通过 `NODE_TEXT_EDITOR_ENTER_KEY_TYPE` 节点属性设置
- [x] 3.19（追加）`ApplyKeyboardType`：打 warn 日志，不做映射（TEXT_EDITOR 无对应属性）

## 4. KRTextEditorAreaView 多行实现

- [x] 4.1 新建 [KRTextEditorAreaView.h](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorAreaView.h)
- [x] ~~4.2 不继承 Field~~ → **偏差**：最终采用 `class KRTextEditorAreaView : public KRTextEditorFieldView` 继承复用（差异只有 4 点，见第 0 节偏差说明 5）
- [x] 4.3 覆盖 `DidInit/SetProp`；通过 `IsSingleLine()=false` / `InterceptNewline()=false` 两个虚函数 override 切换行为；`state_` 由基类提供
- [x] 4.4 新建 [KRTextEditorAreaView.cpp](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorAreaView.cpp)
- [x] 4.5 `CreateNode()` 沿用基类（`ARKUI_NODE_TEXT_EDITOR`）
- [x] 4.6 `DidInit()`：调基类 + 多行特殊配置（当前无额外配置，MAX_LINES 默认值即允许多行）
- [x] 4.7 `SetProp()`：先处理 `lineHeight`（TextStyle.SetLineHeight + SetTypingStyle），其余转发基类；`keyboardType == "password"` 在重写的 `ApplyKeyboardType` 中打 warn 降级
- [x] 4.8 `OnEvent()` 沿用基类（`InterceptNewline()=false` 保证不拦换行）
- [x] 4.9 `CallMethod()` 沿用基类
- [x] 4.10 Focus/Blur/光标索引操作 沿用基类
- [x] 4.11 max-length 过滤 沿用基类

## 5. 注册入口改造

- [x] 5.1 修改 [ComponentsRegisterEntry.h](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/ComponentsRegisterEntry.h)：include 两个新头文件
- [x] 5.2 `#include <deviceinfo.h>`
- [x] 5.3 `"KRTextFieldView"` 注册闭包切换 Field（API 24+）/ KRTextFieldView（老）
- [x] 5.4 `"KRTextAreaView"` 注册闭包切换 Area（API 24+）/ KRTextAreaView（老）
- [x] 5.5 在 View 首次创建时（`KRTextEditorFieldView::DidInit` 里）打印一次 `KRTextEditorFieldView initialized (ARKUI_NODE_TEXT_EDITOR)`；闭包内不再额外打印（避免重复日志）
- [x] 5.6 老实现文件保持 0 行改动

## 6. CMake 构建

- [x] 6.1 [CMakeLists.txt](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/CMakeLists.txt) 追加 `KRTextEditorFieldView.cpp` / `KRTextEditorAreaView.cpp`
- [x] 6.2 Common 采用 header-only（inline functions），无需额外 cpp
- [ ] 6.3 实机编译验证（需 DevEco Studio 打包，当前本地无法执行）

## 7. 属性 / 事件 / 方法覆盖矩阵（代码完成，待回归）

### 7.1 属性覆盖（代码已实现）

- [x] 7.1.1 `text` → `SetStyledText(state, text)`
- [x] 7.1.2 `placeholder` → `ApplyPlaceholder`（TextEditorPlaceholderOptions.SetValue）
- [x] 7.1.3 `placeholderColor` → `ApplyPlaceholder`（Options.SetFontColor）
- [x] 7.1.4 `fontSize` → state + `ApplyTypingStyle` + `ApplyPlaceholder`
- [x] 7.1.5 `fontWeight` → 同上
- [x] 7.1.6 `color` → `TextEditorTextStyle.SetFontColor` + `SetTypingStyle`
- [x] 7.1.7 `editable` → `NODE_FOCUSABLE`
- [x] 7.1.8 `tintColor` → `NODE_TEXT_EDITOR_CARET_COLOR`
- [x] 7.1.9 `textAlign` → `TextEditorParagraphStyle.SetTextAlign` + `SetTypingParagraphStyle`
- [x] 7.1.10 `keyboardType` → **降级**：warn 日志，不映射（TEXT_EDITOR 无对应属性）
- [x] 7.1.11 `returnKeyType` → `NODE_TEXT_EDITOR_ENTER_KEY_TYPE`
- [x] 7.1.12 `maxTextLength` → `NODE_TEXT_EDITOR_MAX_LENGTH` 或手动过滤（依 `lengthLimitType`）
- [x] 7.1.13 `lengthLimitType` → 切换过滤策略
- [x] 7.1.14 `autoHideKeyboardOnImeAction` → Field 专属（`OnInputReturn` 中 `Blur()`）
- [x] 7.1.15 `lineHeight`（Area 专属）→ `TextEditorTextStyle.SetLineHeight` + `SetTypingStyle`

### 7.2 事件覆盖（代码已实现）

- [x] 7.2.1 `textDidChange` → `NODE_TEXT_EDITOR_ON_DID_CHANGE`（默认注册）
- [x] 7.2.2 `inputFocus` → `NODE_ON_FOCUS`（按需注册）
- [x] 7.2.3 `inputBlur` → `NODE_ON_BLUR`（按需注册）
- [x] 7.2.4 `inputReturn` → `NODE_TEXT_EDITOR_ON_SUBMIT`（按需注册）
- [x] 7.2.5 `textLengthBeyondLimit` → `ON_WILL_CHANGE` 过滤后手动触发
- [x] 7.2.6 `keyboardHeightChange` → 复用 `KRKeyboardManager`（与老实现一致）

### 7.3 方法覆盖（代码已实现）

- [x] 7.3.1 `focus` → `NODE_FOCUS_STATUS=1`
- [x] 7.3.2 `blur` → `StopEditing`（controller）兜底 `NODE_FOCUS_STATUS=0`
- [x] 7.3.3 `setText` → `SetStyledText`
- [x] 7.3.4 `getCursorIndex` → `GetSelection` / `GetCaretOffset`（controller）
- [x] 7.3.5 `setCursorIndex` → `SetCaretOffset`（controller）

## 8. 设备回归测试（待真机验证）

- [ ] 8.1 API 23 及以下设备运行 `InputViewDemoPage`，确认老实现路径未被破坏
- [ ] 8.2 API 24+ 设备 / 模拟器运行同一 demo，核对属性 / 事件 / 方法
- [ ] 8.3 API 24+ 跑 `MaxTextLengthDemoPage`（若存在）：三种 `LengthLimitType` 截断与 `textLengthBeyondLimit` 回调
- [ ] 8.4 API 24+ 跑 `InputMeasureDemoPage`：测量 / 聚焦 / 占位符
- [ ] 8.5 API 24+ 键盘类型（`default` 正常；`number/email/phone/password` 确认 warn 日志并降级）与 returnKeyType（`send/search/done/go/next`）
- [ ] 8.6 API 24+ 光标：`setCursorIndex(3)` 后读回 `getCursorIndex() == 3`；手动移动光标读回一致
- [ ] 8.7 API 24+ `autoHideKeyboardOnImeAction=true` 时回车收键盘
- [ ] 8.8 日志：首次 View 创建时打印 `KRTextEditorFieldView initialized (ARKUI_NODE_TEXT_EDITOR)`

## 9. 回滚演练（待真机验证）

- [ ] 9.1 临时将注册闭包 `>= 24` 改为 `>= 999`，重新编译后确认 API 24 设备也走老实现
- [ ] 9.2 回滚后 baseline 无残留

## 10. 提交 commits（按需聚合提交）

- [ ] 10.1 一次性提交所有新增与改动（建议消息：`feat(ohos): switch TextField/TextArea to ARKUI_NODE_TEXT_EDITOR on API 24+`）

## 11. SDK descriptor lifecycle work-around（Create() 缺陷规避）

> 实施期发现 `OH_ArkUI_StyledString_Descriptor_Create()` 返回的 descriptor 内部指针未初始化，
> 后续 `Destroy()` 在 `free_default` 时崩溃；同时 `_Create()` 路径生成的 descriptor 也无法被
> `OH_ArkUI_StyledString_Descriptor_GetString` 正确处理。改为使用安全的 `_CreateWithString("", spanStyles, 1)`
> 创建空 descriptor，放开所有被注释掉的 `Destroy` 调用，避免泄漏。详细背景见
> [.ai/references/ohos-styledstring-descriptor-quirks.md](/Users/steven/code/KuiklyUI/.ai/references/ohos-styledstring-descriptor-quirks.md)。

- [x] 11.1 `KRTextEditorCommon.h::GetStyledText`：用 `OH_ArkUI_StyledString_Descriptor_CreateWithString("", spanStyles, 1)` 替换 `_Create()`，恢复 `Destroy(spanStyle)` / `Destroy(descriptor)`
- [x] 11.2 `KRTextEditorCommon.h::SetStyledText`：主路径恢复 `Destroy(descriptor)`
- [x] 11.3 `KRTextEditorFieldView.cpp::OnWillChangeText`：两处分支（`GetReplacementStyledString` 改写路径与原样回写路径）改用 `_CreateWithString` + 完整 `Destroy` 清理
- [x] 11.4 知识库归档：新增 [.ai/references/ohos-styledstring-descriptor-quirks.md](/Users/steven/code/KuiklyUI/.ai/references/ohos-styledstring-descriptor-quirks.md) 说明缺陷、推荐写法与代码定位
- [x] 11.5 真机/模拟器回归：连续输入 / 删除 / setText / OnWillChange 截断路径均不再出现 `free_default` 崩溃

/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef CORE_RENDER_OHOS_KRTEXTEDITORCOMMON_H
#define CORE_RENDER_OHOS_KRTEXTEDITORCOMMON_H

#include <arkui/native_node.h>
#include <arkui/native_type.h>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>

// ARKUI_NODE_TEXT_EDITOR 及配套 StyledString / Placeholder / TextStyle 等 API
// 均为 OpenHarmony API 24 引入。编译期门控宏 KUIKLY_TEXT_EDITOR_AVAILABLE 的
// 定义已上提到 KRTextEditorSwitch.h；此处只 include 该头，避免出现两份定义与顺序
// 依赖问题。当前头里通过该宏将所有 API 24 类型/工具函数整体裁剪：
//   * SDK header < 24 时本文件主体被 #ifndef 包护，不参与编译；
//   * 注册入口再以运行时 OH_GetSdkApiVersion() >= 24 兜底，永远走老实现路径。
#include "libohos_render/expand/components/input/KRTextEditorSwitch.h"

#if KUIKLY_TEXT_EDITOR_AVAILABLE
#include <arkui/styled_string.h>
#endif

// 编译期 guard：本头之下的全部内容（KRTextEditorState 结构体、ApplyTypingStyle / SetStyledText
// 等工具函数）都直接引用了 OH_ArkUI_TextEditor* / OH_ArkUI_StyledString_* 等 API 24 才有的
// 类型。因此在 SDK header < API 24 时整体被同一宏 guard 掉，避免低 SDK header 编译失败。
// 唯一保留下来的是文件顶部的协议键常量（kText、kPlaceholder 等），它们目前也仅被 editor 路径
// 使用，但即便被 guard 掉也无影响，简单起见不再细分。
#if KUIKLY_TEXT_EDITOR_AVAILABLE

// ============================================================================
// 弱符号重声明块（γ-2 运行时弱链接核心）
//
// 问题：编译期 SDK header >= 24 时，所有 OH_ArkUI_TextEditor* / OH_ArkUI_StyledString*
// / OH_ArkUI_*Style* 等 API 24 入口在 SDK 头里以**强符号**声明。.so 链接后这些符号
// 进入 .dynsym，运行系统 API < 24 时 dlopen 解析重定位会因符号缺失整体失败：
//   "Error relocating /data/storage/.../libkuikly.so:
//      OH_ArkUI_TextEditorStyledStringController_Create: symbol not found"
//
// 修复：在所有调用点之前对**全部** API 24 入口重声明一次同名同签名的版本，并附上
// __attribute__((weak))。clang/gcc 在合并多次声明时，weak 属性是 sticky 的——一旦
// 任意一次声明带上 weak，最终生成的 ELF 引用就是弱符号。运行时若未解析则地址为
// nullptr，注册期 KRIsTextEditorRuntimeAvailable() 探测到后整体回退老控件，loader
// 不再因未解析符号失败。
//
// 注意事项：
//   1. 必须紧跟在 <arkui/styled_string.h> / <arkui/native_node.h> 等 SDK header
//      include **之后**，确保所有 API 24 类型已可见；
//   2. 必须 extern "C"，与 SDK header 声明的链接方式一致，否则名称修饰会冲突；
//   3. 必须保持与 SDK 完全一致的签名（参数 const 限定、返回类型一字不差）。任何
//      签名漂移都会导致 clang 报 "conflicting types"。本块每次随 SDK 升级需复核
//      （来源：DevEco-Studio.app SDK 头 native_type.h / styled_string.h / native_node.h）。
//   4. 仅声明使用到的 API 24 入口；未来代码新增 API 24 调用，必须同步往本块追加，
//      否则那个新符号仍是强引用，等于又把 .so 卡死在低 API 系统。
//
// 本块规模较大但都是机械重声明，集中放置便于审计；按"控件控制器 / 样式构造 /
// StyledString 描述符 / Image attachment / 节点事件 / 占位符选项"分组排列。
// ============================================================================
extern "C" {

// ---- TextEditor StyledString Controller（API 24，native_type.h） ----
OH_ArkUI_TextEditorStyledStringController *
OH_ArkUI_TextEditorStyledStringController_Create() __attribute__((weak));
void OH_ArkUI_TextEditorStyledStringController_Destroy(
    OH_ArkUI_TextEditorStyledStringController *controller) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorStyledStringController_SetTypingStyle(
    OH_ArkUI_TextEditorStyledStringController *controller,
    OH_ArkUI_TextEditorTextStyle *style) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorStyledStringController_SetTypingParagraphStyle(
    OH_ArkUI_TextEditorStyledStringController *controller,
    OH_ArkUI_TextEditorParagraphStyle *style) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorStyledStringController_SetStyledString(
    const OH_ArkUI_TextEditorStyledStringController *controller,
    const ArkUI_StyledString_Descriptor *descriptor) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorStyledStringController_GetStyledString(
    const OH_ArkUI_TextEditorStyledStringController *controller,
    ArkUI_StyledString_Descriptor *descriptor) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorStyledStringController_GetSelection(
    const OH_ArkUI_TextEditorStyledStringController *controller, uint32_t *start,
    uint32_t *end) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorStyledStringController_SetSelection(
    OH_ArkUI_TextEditorStyledStringController *controller, uint32_t start, uint32_t end,
    ArkUI_MenuPolicy menuPolicy) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorStyledStringController_GetCaretOffset(
    OH_ArkUI_TextEditorStyledStringController *controller, int32_t *caretOffset)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorStyledStringController_SetCaretOffset(
    OH_ArkUI_TextEditorStyledStringController *controller, int32_t caretOffset)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorStyledStringController_StopEditing(
    OH_ArkUI_TextEditorStyledStringController *controller) __attribute__((weak));

// ---- TextEditor TextStyle / ParagraphStyle（API 24，native_type.h） ----
OH_ArkUI_TextEditorTextStyle *OH_ArkUI_TextEditorTextStyle_Create() __attribute__((weak));
void OH_ArkUI_TextEditorTextStyle_Destroy(OH_ArkUI_TextEditorTextStyle *style)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorTextStyle_SetFontColor(OH_ArkUI_TextEditorTextStyle *style,
                                                         uint32_t color) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorTextStyle_SetFontSize(OH_ArkUI_TextEditorTextStyle *style,
                                                        float size) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorTextStyle_SetFontWeight(OH_ArkUI_TextEditorTextStyle *style,
                                                          uint32_t fontWeight)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorTextStyle_SetLineHeight(OH_ArkUI_TextEditorTextStyle *style,
                                                          int32_t lineHeight)
    __attribute__((weak));

OH_ArkUI_TextEditorParagraphStyle *OH_ArkUI_TextEditorParagraphStyle_Create()
    __attribute__((weak));
void OH_ArkUI_TextEditorParagraphStyle_Destroy(OH_ArkUI_TextEditorParagraphStyle *style)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorParagraphStyle_SetTextAlign(
    OH_ArkUI_TextEditorParagraphStyle *style, ArkUI_TextAlignment align) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorParagraphStyle_SetTextVerticalAlign(
    OH_ArkUI_TextEditorParagraphStyle *style, ArkUI_TextVerticalAlignment verticalAlignment)
    __attribute__((weak));

// ---- TextEditor Placeholder Options（API 24，native_type.h） ----
OH_ArkUI_TextEditorPlaceholderOptions *OH_ArkUI_TextEditorPlaceholderOptions_Create()
    __attribute__((weak));
void OH_ArkUI_TextEditorPlaceholderOptions_Destroy(OH_ArkUI_TextEditorPlaceholderOptions *options)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorPlaceholderOptions_SetValue(
    OH_ArkUI_TextEditorPlaceholderOptions *options, const char *value) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorPlaceholderOptions_SetFontSize(
    OH_ArkUI_TextEditorPlaceholderOptions *options, float fontSize) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorPlaceholderOptions_SetFontWeight(
    OH_ArkUI_TextEditorPlaceholderOptions *options, uint32_t fontWeight) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorPlaceholderOptions_SetFontColor(
    OH_ArkUI_TextEditorPlaceholderOptions *options, uint32_t fontColor) __attribute__((weak));

// ---- StyledString Descriptor（CreateWithString/AppendStyledString 等多为 API 24，
//      但 _Create / _Destroy 实为 API 14 引入，这里同样附上 weak 不会有副作用——
//      系统 API 14+ 必然解析得到，弱符号属性只是降低链接强度，运行无影响） ----
ArkUI_StyledString_Descriptor *OH_ArkUI_StyledString_Descriptor_Create(void) __attribute__((weak));
void OH_ArkUI_StyledString_Descriptor_Destroy(ArkUI_StyledString_Descriptor *descriptor)
    __attribute__((weak));
ArkUI_StyledString_Descriptor *OH_ArkUI_StyledString_Descriptor_CreateWithString(
    const char *value, const OH_ArkUI_SpanStyle **styles, int32_t length) __attribute__((weak));
ArkUI_StyledString_Descriptor *OH_ArkUI_StyledString_Descriptor_CreateWithImageAttachment(
    const OH_ArkUI_ImageAttachment *value) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_StyledString_Descriptor_AppendStyledString(
    ArkUI_StyledString_Descriptor *descriptor, const ArkUI_StyledString_Descriptor *other)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_StyledString_Descriptor_GetString(
    const ArkUI_StyledString_Descriptor *descriptor, char *buffer, int32_t bufferSize,
    int32_t *writeLength) __attribute__((weak));

// ---- Image Attachment（API 24，styled_string.h） ----
OH_ArkUI_ImageAttachment *OH_ArkUI_ImageAttachment_Create() __attribute__((weak));
void OH_ArkUI_ImageAttachment_Destroy(OH_ArkUI_ImageAttachment *imageAttachment)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_ImageAttachment_SetResource(OH_ArkUI_ImageAttachment *imageAttachment,
                                                    const char *resource) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_ImageAttachment_SetSizeWidth(OH_ArkUI_ImageAttachment *imageAttachment,
                                                     float width) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_ImageAttachment_SetSizeHeight(OH_ArkUI_ImageAttachment *imageAttachment,
                                                      float height) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_ImageAttachment_SetVerticalAlign(
    OH_ArkUI_ImageAttachment *imageAttachment, ArkUI_ImageSpanAlignment verticalAlign)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_ImageAttachment_SetObjectFit(OH_ArkUI_ImageAttachment *imageAttachment,
                                                     ArkUI_ObjectFit objectFit)
    __attribute__((weak));

// ---- TextStyle / SpanStyle / ParagraphStyle / LineHeightStyle（API 24，styled_string.h） ----
OH_ArkUI_TextStyle *OH_ArkUI_TextStyle_Create() __attribute__((weak));
void OH_ArkUI_TextStyle_Destroy(OH_ArkUI_TextStyle *textStyle) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextStyle_SetFontColor(OH_ArkUI_TextStyle *textStyle, uint32_t fontColor)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextStyle_SetFontSize(OH_ArkUI_TextStyle *textStyle, float fontSize)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextStyle_SetFontWeight(OH_ArkUI_TextStyle *textStyle,
                                                uint32_t fontWeight) __attribute__((weak));

OH_ArkUI_SpanStyle *OH_ArkUI_SpanStyle_Create() __attribute__((weak));
void OH_ArkUI_SpanStyle_Destroy(OH_ArkUI_SpanStyle *spanStyle) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_SpanStyle_SetStart(OH_ArkUI_SpanStyle *spanStyle, int32_t start)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_SpanStyle_SetLength(OH_ArkUI_SpanStyle *spanStyle, int32_t length)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_SpanStyle_SetTextStyle(OH_ArkUI_SpanStyle *spanStyle,
                                               const OH_ArkUI_TextStyle *textStyle)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_SpanStyle_SetParagraphStyle(
    OH_ArkUI_SpanStyle *spanStyle, const OH_ArkUI_ParagraphStyle *paragraphStyle)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_SpanStyle_SetLineHeightStyle(
    OH_ArkUI_SpanStyle *spanStyle, const OH_ArkUI_LineHeightStyle *lineHeightStyle)
    __attribute__((weak));

OH_ArkUI_ParagraphStyle *OH_ArkUI_ParagraphStyle_Create() __attribute__((weak));
void OH_ArkUI_ParagraphStyle_Destroy(OH_ArkUI_ParagraphStyle *paragraphStyle)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_ParagraphStyle_SetTextAlign(OH_ArkUI_ParagraphStyle *paragraphStyle,
                                                    ArkUI_TextAlignment align)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_ParagraphStyle_SetTextVerticalAlign(
    OH_ArkUI_ParagraphStyle *paragraphStyle, ArkUI_TextVerticalAlignment verticalAlignment)
    __attribute__((weak));

OH_ArkUI_LineHeightStyle *OH_ArkUI_LineHeightStyle_Create() __attribute__((weak));
void OH_ArkUI_LineHeightStyle_Destroy(OH_ArkUI_LineHeightStyle *lineHeightStyle)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_LineHeightStyle_SetLineHeight(OH_ArkUI_LineHeightStyle *lineHeightStyle,
                                                      float lineHeight) __attribute__((weak));

// ---- TextEditor 节点事件（API 24，native_node.h / styled_string.h） ----
OH_ArkUI_TextEditorChangeEvent *OH_ArkUI_NodeEvent_GetTextEditorOnWillChangeEvent(
    ArkUI_NodeEvent *event) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorChangeEvent_GetReplacementStyledString(
    const OH_ArkUI_TextEditorChangeEvent *event, ArkUI_StyledString_Descriptor *descriptor)
    __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_TextEditorChangeEvent_GetRangeBefore(
    const OH_ArkUI_TextEditorChangeEvent *event, uint32_t *start, uint32_t *end)
    __attribute__((weak));

}  // extern "C"
// ============================================================================
// 弱符号重声明块结束
// ============================================================================

#include "libohos_render/api/src/KRTextPostProcessor.h"
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/utils/KRURIHelper.h"
#include "libohos_render/utils/KRViewUtil.h"

namespace kuikly {
namespace text_editor {

// ========== 协议键 / 事件名 / 方法名（与老 KRTextFieldView 完全一致） ==========
// 这些键与老实现中的同名常量严格一致，保证跨端协议零破坏。
static constexpr const char *kText = "text";
static constexpr const char *kPlaceholder = "placeholder";
static constexpr const char *kPlaceholderColor = "placeholderColor";
static constexpr const char *kFontSize = "fontSize";
static constexpr const char *kFontWeight = "fontWeight";
static constexpr const char *kColor = "color";
static constexpr const char *kEditable = "editable";
static constexpr const char *kTintColor = "tintColor";
static constexpr const char *kTextAlign = "textAlign";
static constexpr const char *kKeyboardType = "keyboardType";
static constexpr const char *kReturnKeyType = "returnKeyType";
static constexpr const char *kMaxTextLength = "maxTextLength";
static constexpr const char *kLengthLimitType = "lengthLimitType";
static constexpr const char *kAutoHideKeyBoardOnIMEAction = "autoHideKeyboardOnImeAction";
static constexpr const char *kLineHeight = "lineHeight";  // TextArea 专属
// 与老 KRTextFieldView 同名：原子化文本输入状态属性 / 方法 / 事件协议。
// Kotlin 侧 InputView.setTextInputState(...) 会通过
//   * attr 路径：setProp("textInputState", json)
//   * method 路径：callMethod("setTextInputState", json)
// 两条通路下发，render 端两条都需要兜住，与 Android KRTextFieldView 保持一致。
static constexpr const char *kTextInputState = "textInputState";

static constexpr const char kMethodFocus[] = "focus";
static constexpr const char kMethodBlur[] = "blur";
static constexpr const char kMethodSetText[] = "setText";
static constexpr const char kMethodGetCursorIndex[] = "getCursorIndex";
static constexpr const char kMethodSetCursorIndex[] = "setCursorIndex";
static constexpr const char kMethodSetTextInputState[] = "setTextInputState";
static constexpr const char kMethodGetTextInputState[] = "getTextInputState";

static constexpr const char kEventTextDidChanged[] = "textDidChange";
static constexpr const char kEventInputFocus[] = "inputFocus";
static constexpr const char kEventInputBlur[] = "inputBlur";
static constexpr const char kEventInputReturn[] = "inputReturn";
static constexpr const char kEventTextLengthBeyondLimit[] = "textLengthBeyondLimit";
static constexpr const char kEventKeyboardHeightChange[] = "keyboardHeightChange";
// textInputState 协议新增的两个事件：与 Android/iOS 同名，Kotlin 侧通过
// InputEvent.textInputStateChange / selectionChange 注册。
static constexpr const char kEventTextInputStateChange[] = "textInputStateChange";
static constexpr const char kEventSelectionChange[] = "selectionChange";

// textInputState payload 字段（与 Kotlin TextInputState.encode/decode 完全对齐）
static constexpr const char kKeyText[] = "text";
static constexpr const char kKeySelectionStart[] = "selectionStart";
static constexpr const char kKeySelectionEnd[] = "selectionEnd";
static constexpr const char kKeyCompositionStart[] = "compositionStart";
static constexpr const char kKeyCompositionEnd[] = "compositionEnd";
static constexpr const char kKeyLength[] = "length";
// composition 区间的"无值"哨兵，与 Android NO_COMPOSITION / iOS -1 一致。
// OHOS TEXT_EDITOR API 不暴露 IME composition 区间，本端固定回填 -1。
static constexpr int32_t kNoComposition = -1;

// ========== 共享状态 ==========

struct KRTextEditorState {
    float font_size_ = 15;  // fp，默认与老实现一致
    ArkUI_FontWeight font_weight_ = ARKUI_FONT_WEIGHT_NORMAL;
    uint32_t font_color_ = 0xFF000000;  // 默认黑色（老实现未显式设，这里落一个稳定默认）
    ArkUI_TextAlignment text_align_ = ARKUI_TEXT_ALIGNMENT_START;
    std::string placeholder_text_;
    uint32_t placeholder_color_ = 0x66000000;  // 淡灰
    bool placeholder_color_set_ = false;
    bool focusable_ = true;
    int32_t max_length_ = -1;
    int length_limit_type_ = -1;  // -1: unset, 0: BYTE, 1: CHARACTER, 2: VISUAL_WIDTH
    bool length_input_filter_ = false;
    bool drag_entered_ = false;
    bool auto_hide_KeyBoard_on_ImeAction_ = false;
    // 行高（vp 单位）。<=0 视为未设置，最终生效值由 ResolveLineHeightVp 推导。
    // 历史决策（B2 方案）：未主动设置时按 fontSize * kDefaultLineHeightMultiplier 自动推导，
    // 让 single/multi-line 在不同机型上的行距视觉保持稳定，避免系统默认行距偏紧。
    float line_height_ = 0.0f;
    bool line_height_set_ = false;
    // 记录"文本发生过变化但 textDidChange 回调尚未注册"的挂起状态。
    // Kuikly 侧先下发属性（包括 text），再下发事件；若首次 SetContentText 早于
    // textDidChange 事件注册，则 OnTextDidChanged 会因 callback==null 丢失一次回调。
    // 我们在此打标，等事件注册到达时立即补发一次，保证 UI 上能观察到初始文本的变化事件。
    bool pending_text_did_change_ = false;
    // 与 pending_text_did_change_ 同语义：textInputState 事件可能晚于首次 set 文本到达，
    // 此时打挂起标记，事件注册分支同步补发一次。
    bool pending_text_input_state_change_ = false;
    // 防回环 guard。语义对齐 Android KRTextFieldView.isSettingTextInputState 与
    // iOS KRTextAreaView._ignoreTextDidChanged：当 set/get 主动写入文本/选区时，
    // 抑制 textDidChange / textInputStateChange / selectionChange 三个回调，
    // 避免业务层 set->callback->set 形成回环。
    bool is_setting_text_input_state_ = false;

    KRRenderCallback text_did_change_callback_;
    KRRenderCallback input_focus_callback_;
    KRRenderCallback input_blur_callback_;
    KRRenderCallback input_return_callback_;
    KRRenderCallback text_length_beyond_limit_callback_;
    KRRenderCallback keyboard_height_changed_callback_;
    // textInputState 协议新增回调：textInputStateChange 携带完整 state 在文本变化时发；
    // selectionChange 仅在选区/光标变化（文本未变）时发。
    KRRenderCallback text_input_state_change_callback_;
    KRRenderCallback selection_change_callback_;

#if KUIKLY_TEXT_EDITOR_AVAILABLE
    // 注意：controller 由调用侧 Create，并通过 NODE_TEXT_EDITOR_STYLED_STRING_CONTROLLER 绑定到
    // 节点；节点销毁时要 Destroy。
    OH_ArkUI_TextEditorStyledStringController *controller_ = nullptr;
#else
    void *controller_ = nullptr;
#endif

    // 当前已设置到节点的文本（逻辑层缓存，供 SetProp text 幂等判断及过滤使用）
    std::string cached_text_;

    // ===== Image span 权威映射表（输入框编辑差分回写算法的核心） =====
    //
    // 每条记录描述「当前 ArkUI 节点中的一个 image span」：它在 ArkUI flat 文本中
    // 占用 1 个 ASCII 空格（0x20）作为占位字符，flat_offset_ 即该占位空格的字节偏移；
    // raw_literal_ 是它在「raw 原始文本」中对应的字面量（如 "[smile]"）。
    //
    // 维护时机（仅这两处写）：
    //   1) SetStyledText：以业务 adapter 返回的 spans 为权威源，遍历时把每个 image
    //      span 的 {flat_offset, raw_literal} 顺序 push 进来。
    //   2) ReconstructRawFromFlat（OnTextDidChanged 调用）：基于 lcp/lcs 求差异区段，
    //      对落在差异区段之前 / 之后 / 之内的 image 分别做「保留 / 平移 / 删除」三类
    //      O(N) 增量平移，最终输出新的 image_spans_ 列表（覆盖式赋值）。
    //
    // 与 iOS 方案对齐：iOS 的 NSTextStorage 自动维护 NSTextAttachment 的位置，SDK
    // 侧只需 enumerateAttribute:NSAttachmentAttributeName 还原 raw 即可；OHOS 这里
    // 维护一份「权威映射表」是 NSTextStorage 行为的人工等价物。
    //
    // 不变量：
    //   * flat_offset_ 严格单调递增；
    //   * 每条记录在 ArkUI flat 文本对应 flat_offset_ 处的字节为 0x20（空格占位符）；
    //   * 当 raw_literal_ 为空时，差分回写阶段把该位置当成普通空格处理（与不携带
    //     raw_literal 的旧 AppendImageSpan 接口语义一致）。
    struct KRImageSpanRecord {
        // ArkUI flat 中该 image 占位空格的字节偏移（UTF-8 byte），用于
        // ReconstructRawFromFlat 字节级差分回写。
        size_t flat_offset = 0;
        // ArkUI flat 中该 image 占位空格的 UTF-16 code unit 偏移，用于 selection
        // 坐标的 flat ↔ raw 双向映射（ArkUI GetSelection / SetSelection 单位为
        // UTF-16 code unit，与 byte_offset 在含中文等多字节字符时不相等）。
        uint32_t utf16_offset = 0;
        std::string raw_literal;
    };
    std::vector<KRImageSpanRecord> image_spans_;

    // returnKeyType 上一次设置值的原始字符串，用于 OnSubmit 回调时回传 ime_action。
    ArkUI_EnterKeyType enter_key_type_ = ARKUI_ENTER_KEY_TYPE_DONE;

    // 资源根目录（KRConfig::GetAssetsDir 的运行时快照），由 view 在 InitControllerIfNeeded
    // 时灌入，供 SetStyledText 内部解析 emoji shortcode 的 "assets://..." 资源路径——
    // 与 KRImageView::LoadFromAssets 的 URI 拼接逻辑一致（见 KRURIHelper::URIForResFile）。
    // 留空字符串视为未注入：emoji 段会被跳过（保持纯文本旧行为，零回归）。
    std::string assets_dir_;
};

// ========== Text Post Processor 派发（业务自定义文本预处理） ==========
//
// SetStyledText 写入控件前，会把原始文本分发到名为 "input" 的 TextPostProcessor adapter
// （由业务通过 KRRegisterTextPostProcessorAdapter("input", ...) 注册）。adapter 负责：
//   * 扫描自定义短码（如 [smile]）；
//   * 解析为可寻址 URI（file:// / http(s):// / data:image;base64,...）；
//   * 通过 KRTextProcessedResultAppendTextSpan / AppendImageSpan 顺序灌入 builder。
// SDK 这一侧不再持有任何 emoji 短码 / 资源路径硬编码，与 Android `IKRTextPostProcessorAdapter`、
// iOS `hr_customTextWithAttributedString:textPostProcessor:` 在职责边界上对齐。
//
// ⚠️ 选区/getTextInputState 的 raw <-> rendered 坐标映射本轮不做：SDK 侧每个 image
//    attachment 占用 1 个 UTF-16 占位字符，与 Android 保留 raw 短码字面量的口径
//    存在差异；后续如需跨端一致，再在 BuildTextInputStatePayload 中做映射。
static constexpr const char kTextPostProcessorNameInput[] = "input";

// ========== StyledString / Controller 读写 ==========

// 前向声明：SetStyledText 需要在 span 长度上使用 UTF-16 code unit 口径，与文件下方
// "文本长度计算" 块中 GetUTF16Length 复用实现。这里仅作声明，定义在同文件末尾。
inline int GetUTF16Length(const std::string &text);

#if KUIKLY_TEXT_EDITOR_AVAILABLE
// 默认行高倍数：未主动设置 lineHeight 时按 fontSize * kDefaultLineHeightMultiplier 推导。
// 1.4 与 ArkTS 默认 lineSpacingMultiple 接近，实测视觉与老 NODE_TEXT_INPUT 默认行距相近。
static constexpr float kDefaultLineHeightMultiplier = 1.4f;

// 计算最终生效的 lineHeight（vp）。覆盖两种场景：
//   A. 调用方主动设置 lineHeight：直接返回用户值（按 fontSize 做最小下限保护，避免裁切）。
//   B. 未主动设置：按 fontSize * kDefaultLineHeightMultiplier 推导。
// 返回 <=0 表示"不主动调 SetLineHeight"（仅当 fontSize<=0 等异常情况），让系统走默认。
inline float ResolveLineHeightVp(const KRTextEditorState &state) {
    if (state.line_height_set_ && state.line_height_ > 0) {
        // A. 主动设置：保证不小于 fontSize（行高低于字号会出现上下截断）。
        return state.line_height_ < state.font_size_ ? state.font_size_ : state.line_height_;
    }
    // B. 未设置：按字体大小推导。fontSize<=0 兜底返回 0，让系统走默认。
    if (state.font_size_ <= 0) {
        return 0.0f;
    }
    return state.font_size_ * kDefaultLineHeightMultiplier;
}

// 基于当前 state 的 typing 样式创建一个 TextStyle，调用方负责 Destroy。
inline OH_ArkUI_TextEditorTextStyle *CreateTextStyleFromState(const KRTextEditorState &state,
                                                              float font_size_px_if_fixed) {
    OH_ArkUI_TextEditorTextStyle *style = OH_ArkUI_TextEditorTextStyle_Create();
    if (!style) {
        return nullptr;
    }
    OH_ArkUI_TextEditorTextStyle_SetFontColor(style, state.font_color_);
    // font_size: 老实现只会把 fp 级别的 size 透传；API 的 SetFontSize 单位按 vp/fp，
    // 与老 NODE_TEXT_INPUT_PLACEHOLDER_FONT 行为一致即可。
    OH_ArkUI_TextEditorTextStyle_SetFontSize(style, state.font_size_);
    OH_ArkUI_TextEditorTextStyle_SetFontWeight(style, static_cast<uint32_t>(state.font_weight_));
    // 行高（typing 路径）：注意 OH_ArkUI_TextEditorTextStyle_SetLineHeight 入参为 int32_t（vp）。
    // 由 ResolveLineHeightVp 统一处理"主动 / 默认推导"两种语义，避免在多个调用点重复判断。
    float lh = ResolveLineHeightVp(state);
    if (lh > 0) {
        OH_ArkUI_TextEditorTextStyle_SetLineHeight(
            style, static_cast<int32_t>(lh + 0.5f));
    }
    (void)font_size_px_if_fixed;  // 预留：若 fontSizeScaleFollowSystem=false 时切换 px 单位
    return style;
}

inline OH_ArkUI_TextEditorParagraphStyle *CreateParagraphStyleFromState(const KRTextEditorState &state) {
    OH_ArkUI_TextEditorParagraphStyle *style = OH_ArkUI_TextEditorParagraphStyle_Create();
    if (!style) {
        return nullptr;
    }
    OH_ArkUI_TextEditorParagraphStyle_SetTextAlign(style, state.text_align_);
    // 段落内垂直对齐恢复默认 BASELINE，避免与节点级 NODE_TEXT_CONTENT_ALIGN 叠加干扰。
    // 历史尝试：CENTER / BOTTOM 在单行场景下视觉表现均不理想（仍偏上），改为在节点层使用
    // NODE_TEXT_CONTENT_ALIGN = ARKUI_TEXT_CONTENT_ALIGN_CENTER 做整体容器居中。
    OH_ArkUI_TextEditorParagraphStyle_SetTextVerticalAlign(style, ArkUI_TextVerticalAlignment::ARKUI_TEXT_VERTICAL_ALIGNMENT_CENTER);
    return style;
}

// 把 state 中的 typing 样式 + 段落样式应用到 controller。调用时机：
//   * 首次绑定 controller；
//   * fontSize/fontWeight/color/textAlign 任一变化。
inline void ApplyTypingStyle(KRTextEditorState &state) {
    if (!state.controller_) {
        return;
    }
    OH_ArkUI_TextEditorTextStyle *text_style = CreateTextStyleFromState(state, 0);
    if (text_style) {
        OH_ArkUI_TextEditorStyledStringController_SetTypingStyle(state.controller_, text_style);
        OH_ArkUI_TextEditorTextStyle_Destroy(text_style);
    }
    OH_ArkUI_TextEditorParagraphStyle *para_style = CreateParagraphStyleFromState(state);
    if (para_style) {
        OH_ArkUI_TextEditorStyledStringController_SetTypingParagraphStyle(state.controller_, para_style);
        OH_ArkUI_TextEditorParagraphStyle_Destroy(para_style);
    }
}

// 构造一段 image span 对应的 ArkUI_StyledString_Descriptor。
// 调用方负责后续 Append；attachment 仅作为构造材料，构造完即可销毁，所有权由
// descriptor 接管（与 SDK CreateWith*Attachment 文档一致）。
//
// 业务通过 KRTextProcessedResultAppendImageSpan 给出 src（必须是可寻址 URI：file:// /
// http(s):// / data:image;base64,...）以及 width/height（vp，<=0 表示按字号自适应）。
// SDK 不再做 "assets://" 这类私有协议解析——业务负责把 hap rawfile 拷到沙盒后给 file://。
//
// fallback_size_vp 兜底：当 width/height 都为 <=0 时按当前 fontSize 自适应；fontSize<=0
// 时再退回 16vp，避免出现 0×0 的不可见 attachment。
inline ArkUI_StyledString_Descriptor *BuildImageSpanDescriptor(const std::string &resource_uri,
                                                                float width_vp,
                                                                float height_vp,
                                                                float fallback_size_vp) {
    if (resource_uri.empty()) {
        return nullptr;
    }
    OH_ArkUI_ImageAttachment *attachment = OH_ArkUI_ImageAttachment_Create();
    if (!attachment) {
        return nullptr;
    }
    OH_ArkUI_ImageAttachment_SetResource(attachment, resource_uri.c_str());
    float fb = fallback_size_vp > 0 ? fallback_size_vp : 16.0f;
    float w = width_vp > 0 ? width_vp : fb;
    float h = height_vp > 0 ? height_vp : w;  // 仅 width 有值时按方形展开
    OH_ArkUI_ImageAttachment_SetSizeWidth(attachment, w);
    OH_ArkUI_ImageAttachment_SetSizeHeight(attachment, h);
    OH_ArkUI_ImageAttachment_SetVerticalAlign(attachment, ARKUI_IMAGE_SPAN_ALIGNMENT_CENTER);
    OH_ArkUI_ImageAttachment_SetObjectFit(attachment, ARKUI_OBJECT_FIT_CONTAIN);

    ArkUI_StyledString_Descriptor *desc =
        OH_ArkUI_StyledString_Descriptor_CreateWithImageAttachment(attachment);
    // 销毁本地 attachment——descriptor 已持有所需信息（与 SDK 约定一致）。
    OH_ArkUI_ImageAttachment_Destroy(attachment);
    return desc;
}

// 构造一段纯文本 descriptor（带覆盖全长的 SpanStyle / TextStyle / ParagraphStyle /
// LineHeightStyle）。返回 nullptr 表示构造失败。
// 把 span/text/para/lineHeight 四个临时资源传出供调用方统一 Destroy。
inline ArkUI_StyledString_Descriptor *BuildPlainTextDescriptor(
    const KRTextEditorState &state, const std::string &text,
    OH_ArkUI_TextStyle **out_text_style, OH_ArkUI_SpanStyle **out_span_style,
    OH_ArkUI_ParagraphStyle **out_para_style, OH_ArkUI_LineHeightStyle **out_line_height_style) {
    *out_text_style = OH_ArkUI_TextStyle_Create();
    *out_span_style = OH_ArkUI_SpanStyle_Create();
    *out_para_style = OH_ArkUI_ParagraphStyle_Create();
    *out_line_height_style = nullptr;
    if (!*out_text_style || !*out_span_style) {
        return nullptr;
    }
    int32_t u16_len = GetUTF16Length(text);
    OH_ArkUI_TextStyle_SetFontColor(*out_text_style, state.font_color_);
    OH_ArkUI_TextStyle_SetFontSize(*out_text_style, state.font_size_);
    OH_ArkUI_TextStyle_SetFontWeight(*out_text_style, static_cast<uint32_t>(state.font_weight_));
    OH_ArkUI_SpanStyle_SetStart(*out_span_style, 0);
    OH_ArkUI_SpanStyle_SetLength(*out_span_style, u16_len);
    OH_ArkUI_SpanStyle_SetTextStyle(*out_span_style, *out_text_style);
    float lh = ResolveLineHeightVp(state);
    if (lh > 0) {
        *out_line_height_style = OH_ArkUI_LineHeightStyle_Create();
        if (*out_line_height_style) {
            OH_ArkUI_LineHeightStyle_SetLineHeight(*out_line_height_style, lh);
            OH_ArkUI_SpanStyle_SetLineHeightStyle(*out_span_style, *out_line_height_style);
        }
    }
    if (*out_para_style) {
        OH_ArkUI_ParagraphStyle_SetTextAlign(*out_para_style, state.text_align_);
        OH_ArkUI_ParagraphStyle_SetTextVerticalAlign(
            *out_para_style, ArkUI_TextVerticalAlignment::ARKUI_TEXT_VERTICAL_ALIGNMENT_CENTER);
        OH_ArkUI_SpanStyle_SetParagraphStyle(*out_span_style, *out_para_style);
    }
    const OH_ArkUI_SpanStyle *span_styles[] = {*out_span_style};
    return OH_ArkUI_StyledString_Descriptor_CreateWithString(text.c_str(), span_styles, 1);
}

inline void DestroyTextSpanResources(OH_ArkUI_TextStyle *text_style, OH_ArkUI_SpanStyle *span_style,
                                     OH_ArkUI_ParagraphStyle *para_style,
                                     OH_ArkUI_LineHeightStyle *line_height_style) {
    if (span_style) OH_ArkUI_SpanStyle_Destroy(span_style);
    if (text_style) OH_ArkUI_TextStyle_Destroy(text_style);
    if (para_style) OH_ArkUI_ParagraphStyle_Destroy(para_style);
    if (line_height_style) OH_ArkUI_LineHeightStyle_Destroy(line_height_style);
}

// 把纯文本写入 controller。
//
// 实现要点：
//   * 官方示例 `TextEditorMaker.cpp` 中 `CreateWithString` 永远带至少一个覆盖全长的
//     `OH_ArkUI_SpanStyle`（含 `OH_ArkUI_TextStyle`）。实测若传 `nullptr, 0`，系统侧
//     `SetStyledString` 不会把文本写进节点（观感是"设置后文本为空"）。因此这里必须
//     构造一个覆盖全文本长度 `[0, utf16_len)` 的 span，TextStyle 使用当前 state 的
//     字体颜色 / 字号 / 字重，使得首次写入后即有正确的视觉样式（后续键入由 typing
//     style 控制）。
//   * TypingStyle（SetTypingParagraphStyle/SetTypingStyle）只影响**后续键入**，不会
//     回写到已有文本；所以写入时必须同时带上 span style。
//   * 文本预处理（与 Android KRTextPostProcessorAdapter / iOS hr_customTextWithAttributedString
//     等价）：通过 kuikly::text::RunTextPostProcessor("input", text, spans) 把原始文本
//     交给业务侧 adapter 处理，拿到 [TextSpan/ImageSpan ...] 序列，按段构建 descriptor
//     再 OH_ArkUI_StyledString_Descriptor_AppendStyledString 串接。
//     adapter 未注册或返回空段时走原有最快路径，零回归。
inline void SetStyledText(KRTextEditorState &state, const std::string &text) {
    if (!state.controller_) {
        return;
    }

    // 入口处先清空 image_spans_：本函数是「权威映射」的唯一构建点，无论走 adapter
    // 路径还是纯文本旧路径，都需要保证 image_spans_ 与 ArkUI 节点中的 image span
    // 一一对应。adapter 未命中时纯文本无 image，image_spans_ 维持空即可。
    state.image_spans_.clear();

    // ---- TextPostProcessor 分支：业务侧把原始文本切为 [Text/Image Span ...] ----
    // 业务在 adapter 中负责：
    //   1) 识别自定义短码（如 [smile]）；
    //   2) 把图片资源解析为可寻址 URI（file:// / http(s):// / data:image;base64,...）。
    // SDK 这里仅负责按段构建 descriptor 并串接，不再做任何资源协议解析。
    {
        std::vector<kuikly::text::KRTextPostProcessSpan> spans;
        if (kuikly::text::RunTextPostProcessor(kTextPostProcessorNameInput, text, spans)) {
            ArkUI_StyledString_Descriptor *root_desc = nullptr;
            // 跟踪 build segment 期间的临时资源，统一在尾部一次性 Destroy（与原实现
            // 同生命周期：style 资源 Destroy 安全；descriptor 由于已知所有权问题不
            // Destroy，与原 SetStyledText 注释保持一致）。
            std::vector<OH_ArkUI_TextStyle *> temp_text_styles;
            std::vector<OH_ArkUI_SpanStyle *> temp_span_styles;
            std::vector<OH_ArkUI_ParagraphStyle *> temp_para_styles;
            std::vector<OH_ArkUI_LineHeightStyle *> temp_lh_styles;

            auto append_text_span = [&](const std::string &seg) {
                // 长度为 0 的 text span 仅在"image 居首需要建空 root"场景使用，外部跳过；
                // 这里允许空字符串落到 BuildPlainTextDescriptor，让 SDK 走与外层旧路径
                // 一致的"必须带 span"语义。
                OH_ArkUI_TextStyle *ts = nullptr;
                OH_ArkUI_SpanStyle *ss = nullptr;
                OH_ArkUI_ParagraphStyle *ps = nullptr;
                OH_ArkUI_LineHeightStyle *ls = nullptr;
                ArkUI_StyledString_Descriptor *seg_desc =
                    BuildPlainTextDescriptor(state, seg, &ts, &ss, &ps, &ls);
                if (seg_desc) {
                    if (!root_desc) {
                        root_desc = seg_desc;
                    } else {
                        OH_ArkUI_StyledString_Descriptor_AppendStyledString(root_desc, seg_desc);
                    }
                }
                if (ts) temp_text_styles.push_back(ts);
                if (ss) temp_span_styles.push_back(ss);
                if (ps) temp_para_styles.push_back(ps);
                if (ls) temp_lh_styles.push_back(ls);
            };

            auto append_image_span = [&](const std::string &uri, float w, float h) {
                ArkUI_StyledString_Descriptor *img_desc =
                    BuildImageSpanDescriptor(uri, w, h, state.font_size_);
                if (!img_desc) return;
                if (!root_desc) {
                    // 文本以 image 开头：root 必须是个 string descriptor 才能被 Append
                    // 成功——为安全起见先建一个空文本 descriptor 作为 root。
                    OH_ArkUI_TextStyle *ts = nullptr;
                    OH_ArkUI_SpanStyle *ss = nullptr;
                    OH_ArkUI_ParagraphStyle *ps = nullptr;
                    OH_ArkUI_LineHeightStyle *ls = nullptr;
                    root_desc = BuildPlainTextDescriptor(state, "", &ts, &ss, &ps, &ls);
                    if (ts) temp_text_styles.push_back(ts);
                    if (ss) temp_span_styles.push_back(ss);
                    if (ps) temp_para_styles.push_back(ps);
                    if (ls) temp_lh_styles.push_back(ls);
                }
                if (root_desc) {
                    OH_ArkUI_StyledString_Descriptor_AppendStyledString(root_desc, img_desc);
                }
            };

            // image_spans_ 维护：随 spans 顺序遍历，跟踪每段在 ArkUI flat 字节流中
            // 的当前偏移（byte）以及 UTF-16 code unit 偏移；image 段在 flat 中占
            // 1 个 ASCII 空格（见 BuildImageSpanDescriptor / kArkUIImageSpanPlaceholder），
            // text 段按 UTF-8 字节 / UTF-16 code unit 原样占据。
            size_t flat_byte_cursor = 0;
            uint32_t flat_utf16_cursor = 0;
            for (const auto &span : spans) {
                if (span.type == kuikly::text::KRTextPostProcessSpan::Type::kText) {
                    if (!span.text_or_src.empty()) {
                        append_text_span(span.text_or_src);
                        flat_byte_cursor += span.text_or_src.size();
                        flat_utf16_cursor += static_cast<uint32_t>(GetUTF16Length(span.text_or_src));
                    }
                } else {
                    append_image_span(span.text_or_src, span.width, span.height);
                    KRTextEditorState::KRImageSpanRecord rec;
                    rec.flat_offset = flat_byte_cursor;
                    rec.utf16_offset = flat_utf16_cursor;
                    rec.raw_literal = span.raw_literal;
                    state.image_spans_.push_back(std::move(rec));
                    flat_byte_cursor += 1;
                    flat_utf16_cursor += 1;  // ASCII space = 1 UTF-16 code unit
                }
            }

            if (root_desc) {
                OH_ArkUI_TextEditorStyledStringController_SetStyledString(state.controller_, root_desc);
                // 与原实现一致：descriptor 不主动 Destroy，规避真机 free_default 崩溃。
            }
            // Style 临时资源安全 Destroy（与原实现同等条件）。
            for (auto *ts : temp_text_styles) OH_ArkUI_TextStyle_Destroy(ts);
            for (auto *ss : temp_span_styles) OH_ArkUI_SpanStyle_Destroy(ss);
            for (auto *ps : temp_para_styles) OH_ArkUI_ParagraphStyle_Destroy(ps);
            for (auto *ls : temp_lh_styles) OH_ArkUI_LineHeightStyle_Destroy(ls);
            state.cached_text_ = text;
            return;
        }
    }
    // ---- 旧路径：纯文本（adapter 未注册或返回空 span） ----
    // 计算 UTF-16 长度（与 SDK SpanStyle_SetLength 的口径一致）
    int32_t u16_len = GetUTF16Length(text);

    OH_ArkUI_TextStyle *text_style = OH_ArkUI_TextStyle_Create();
    OH_ArkUI_SpanStyle *span_style = OH_ArkUI_SpanStyle_Create();
    // 段落级样式（textAlign 等）：SpanStyle 走 OH_ArkUI_ParagraphStyle（非 TextEditor
    // 特化版本），是段落绑定到 span 范围的正式通道。仅靠 TypingParagraphStyle 只会
    // 影响「后续键入」，不会回写已有文本，因此必须在 span 层带上。
    OH_ArkUI_ParagraphStyle *para_style = OH_ArkUI_ParagraphStyle_Create();
    // 行高（span 路径）：OH_ArkUI_TextStyle 自身没有 SetLineHeight，需通过
    // OH_ArkUI_LineHeightStyle + OH_ArkUI_SpanStyle_SetLineHeightStyle 设置；
    // 仅这条路径才能让"已有文本"的行高立即生效（typing style 只影响后续键入）。
    OH_ArkUI_LineHeightStyle *line_height_style = nullptr;
    ArkUI_StyledString_Descriptor *desc = nullptr;

    if (text_style && span_style) {
        OH_ArkUI_TextStyle_SetFontColor(text_style, state.font_color_);
        OH_ArkUI_TextStyle_SetFontSize(text_style, state.font_size_);
        OH_ArkUI_TextStyle_SetFontWeight(text_style, static_cast<uint32_t>(state.font_weight_));

        OH_ArkUI_SpanStyle_SetStart(span_style, 0);
        OH_ArkUI_SpanStyle_SetLength(span_style, u16_len);
        OH_ArkUI_SpanStyle_SetTextStyle(span_style, text_style);

        // 把 lineHeight 通过 LineHeightStyle 绑定到 span：覆盖整段已有文本。
        float lh = ResolveLineHeightVp(state);
        if (lh > 0) {
            line_height_style = OH_ArkUI_LineHeightStyle_Create();
            if (line_height_style) {
                OH_ArkUI_LineHeightStyle_SetLineHeight(line_height_style, lh);
                OH_ArkUI_SpanStyle_SetLineHeightStyle(span_style, line_height_style);
            }
        }

        if (para_style) {
            OH_ArkUI_ParagraphStyle_SetTextAlign(para_style, state.text_align_);
            // 垂直居中与 CreateParagraphStyleFromState 中一致，避免视觉差异。
            OH_ArkUI_ParagraphStyle_SetTextVerticalAlign(
                para_style, ArkUI_TextVerticalAlignment::ARKUI_TEXT_VERTICAL_ALIGNMENT_CENTER);
            OH_ArkUI_SpanStyle_SetParagraphStyle(span_style, para_style);
        }

        const OH_ArkUI_SpanStyle *span_styles[] = {span_style};
        // 注意：span 数量不能传 0。实测若传 `nullptr, 0` 或非 nullptr 的空 spans 数组，
        // 系统侧 SetStyledString 不会把文本写进节点——对 `text == ""`（清空）场景尤其
        // 致命：控件里的旧文本会保持不变，表现为"setText("") 无效"。
        // 因此这里即便 u16_len == 0，也保留一个覆盖 [0, 0) 的空 span 传进去，让
        // SDK 进入正常的"写入路径"，从而真正把空文本落到节点上。
        desc = OH_ArkUI_StyledString_Descriptor_CreateWithString(
            text.c_str(), span_styles, 1);
    }

    if (desc) {
        OH_ArkUI_TextEditorStyledStringController_SetStyledString(state.controller_, desc);
        // TODO(kuikly-text-editor): OH_ArkUI_StyledString_Descriptor_Destroy 在部分真机场景下
        // 仍会触发 free_default / StyledStringAdapter::DestroyArkUIStyledStringDescriptor
        // 崩溃。根因怀疑为 SDK 内部对该 descriptor 的所有权关调格式与 API 文档不符：
        //   * `CreateWithString` + `SetStyledString` 后，系统侧可能接管了其内部
        //     `span_styles` / `text` 指针，调用方再 `Destroy` 就会 double-free。
        // 暂缓解方案：暂注释掉 `Destroy`，允许每次 SetStyledText 泄漏一个 descriptor
        // 对象（生命周期随节点销毁问题在文本改变时重复发生，实测一次填几百
        // 字节，累计损耗可接受）。后续需要跟进：
        //   1. 咨询华为确认 Create/Destroy 配对规则
        //   2. 或改用 StyledStringController_SetText（如有）替代 SetStyledString 线路
        //   3. 建立长生命周期 desc 缓存（只在 dtor 中一次 Destroy）
        // OH_ArkUI_StyledString_Descriptor_Destroy(desc);
    }
    // SpanStyle / TextStyle 是 Create 时的独立资源，Destroy 是安全的（与 Descriptor
    // 不同，这两者没有所有权被系统接管的问题，官方示例也明确一对一 Destroy）。
    if (span_style) {
        OH_ArkUI_SpanStyle_Destroy(span_style);
    }
    if (text_style) {
        OH_ArkUI_TextStyle_Destroy(text_style);
    }
    if (para_style) {
        OH_ArkUI_ParagraphStyle_Destroy(para_style);
    }
    if (line_height_style) {
        // LineHeightStyle 与 TextStyle/ParagraphStyle 同属"由调用方 Create/Destroy"
        // 的子样式资源，与 Descriptor 不同，没有所有权被系统接管的问题，可安全 Destroy。
        OH_ArkUI_LineHeightStyle_Destroy(line_height_style);
    }
    state.cached_text_ = text;
}

// ============================================================================
// Raw <-> Flat 文本映射工具（基于 image_spans_ 权威映射的差分回写算法）
//
// 背景：对外暴露 / 业务持有的 text 永远是「raw shortcode 文本」（如 "[smile]a"）；
// 但 ArkUI StyledString 内部把每段 image span 在 GetStyledString 读回时统一扁平化为
// 1 个 ASCII 空格占位符，因此 GetContentText() 拿到的是「flat 文本」（如 " a"）。
//
// 一旦把 flat 文本通过 textDidChange / textInputStateChange 上抛给业务，业务通常会
// 把 state 写回给 setTextInputState（受控组件模式），SDK 这一侧再走 SetStyledText，
// flat 中的"空格"会被当成纯文本写回——image span 永久消失，表情视觉破碎。
//
// 解决方案（与 iOS `KRTextAttachmentStringProtocol` 镜像）：
//   1) 业务 adapter 通过 KRTextProcessedResultAppendImageSpanWithRaw 显式回传每个
//      image 的「raw 字面量」（如 "[smile]"）；SetStyledText 在构造 ArkUI 节点的
//      同时把 {flat_offset, raw_literal} push 进 KRTextEditorState::image_spans_，
//      作为权威映射表。
//   2) OnTextDidChanged 拿到 new_flat 后，调用 ReconstructRawFromFlat：基于 lcp/lcs
//      求差异区段，对落在差异区段之前 / 之后 / 之内的 image 分别做「保留 / 平移 /
//      删除」三类 O(N) 增量平移，得到新的 image_spans_，最后从右到左把 new_flat 中
//      仍存活的 image 占位空格替换回 raw_literal，输出 new_raw。
//
// 关键不变量：
//   * image span 在 ArkUI flat 中**永远是 1 个 ASCII 空格 (0x20)**；
//   * image_spans_ 按 flat_offset 严格单调递增；
//   * 差分回写算法不依赖 adapter 二次推断，不做任何 find / 模式匹配，正确性由
//     权威映射保证（与 iOS 通过 NSTextStorage 自动维护 NSTextAttachment 位置等价）。
//
// 退化情形：业务 adapter 未注册或未传 raw_literal 时，image_spans_ 为空或 raw_literal
// 为空——前者完全等价"raw == flat"，与原行为一致；后者把该 image 的占位空格当作
// 普通空格处理（编辑后 raw 文本中该位置变成空格，shortcode 信息丢失，但不破坏
// 其他 image 的还原；业务侧应优先使用 WithRaw 接口避免此退化）。
// ============================================================================

// Image span 在 ArkUI flat 文本中的占位字符（1 个 ASCII 空格 0x20）。
// 该值与 OH_ArkUI_StyledString_Descriptor_GetString 的实测产物一致；如未来 SDK 行为
// 变化，仅需在此调整。
static constexpr char kArkUIImageSpanPlaceholder = ' ';

// 计算 [a, b) 字节区间向左退到合法 UTF-8 字符边界的位置。
// 用于 lcp / lcs 切到多字节序列中间时回退。
inline size_t SnapToUtf8CharStart(const std::string &s, size_t pos) {
    while (pos > 0 && pos < s.size()) {
        unsigned char c = static_cast<unsigned char>(s[pos]);
        // UTF-8 续字节：10xxxxxx；首字节是 0xxxxxxx 或 11xxxxxx。
        if ((c & 0xC0) != 0x80) {
            break;
        }
        --pos;
    }
    return pos;
}

// 基于 image_spans_ 权威映射做差分回写：把 ArkUI flat 文本还原成 raw 文本，
// 同时输出新的 image_spans_（已按用户编辑动作做过位置增量平移 / 删除）。
//
// 入参：
//   * prev_image_spans : 上一次 SetStyledText / 上一次 OnTextDidChanged 写入的权威映射
//   * prev_flat        : 与 prev_image_spans 配套的上一次 flat 文本（由调用方提供，
//                        通常等价于「ArkUI 节点上一次 GetContentText 的结果」）
//   * new_flat         : 当前 ArkUI flat 文本（GetContentText 实测）
// 出参：
//   * out_new_image_spans : 编辑后新的权威映射（按 flat_offset 升序）
// 返回值：还原后的 raw 文本。
//
// 算法（与 iOS p_outputText 同构，但 OHOS 维护权威映射而非依赖 NSTextStorage）：
//   Step A. 求 lcp/lcs 得到差异区段 [diff_s, prev_e) → [diff_s, new_e)，UTF-8 边界回退。
//   Step B. 遍历 prev_image_spans，按 flat_offset 与差异区段的位置关系分三类处理：
//             - flat_offset < diff_s             → 幸存，flat_offset 不变。
//             - flat_offset >= prev_e            → 幸存，flat_offset += (new_e - prev_e)。
//             - 在 [diff_s, prev_e) 内           → 被吞掉，丢弃。
//   Step C. 以 new_flat 为底，按 out_new_image_spans **从右到左** 把每个 flat_offset
//           处的占位空格替换为该 image 的 raw_literal（从右到左避免影响后续偏移）。
//           raw_literal 为空时跳过替换（保留单空格）。
inline std::string ReconstructRawFromFlat(
    const std::vector<KRTextEditorState::KRImageSpanRecord> &prev_image_spans,
    const std::string &prev_flat,
    const std::string &new_flat,
    std::vector<KRTextEditorState::KRImageSpanRecord> &out_new_image_spans) {
    out_new_image_spans.clear();

    // 退化路径 1：prev_flat == new_flat（无变化）→ 映射全部原样幸存。
    if (prev_flat == new_flat) {
        out_new_image_spans = prev_image_spans;
        // 直接基于 prev_image_spans 把 new_flat 还原为 raw（无平移，仅替换占位空格）。
        if (prev_image_spans.empty()) {
            return new_flat;
        }
        std::string raw = new_flat;
        for (auto it = prev_image_spans.rbegin(); it != prev_image_spans.rend(); ++it) {
            if (it->raw_literal.empty()) continue;
            if (it->flat_offset >= raw.size()) continue;
            raw.replace(it->flat_offset, 1, it->raw_literal);
        }
        return raw;
    }
    // 退化路径 2：prev_image_spans 为空（prev raw 中无 image）→ raw == flat。
    if (prev_image_spans.empty()) {
        return new_flat;
    }

    // Step A: lcp/lcs 求差异区段。
    size_t prev_len = prev_flat.size();
    size_t new_len = new_flat.size();
    size_t lcp = 0;
    while (lcp < prev_len && lcp < new_len && prev_flat[lcp] == new_flat[lcp]) {
        ++lcp;
    }
    size_t lcs = 0;
    while (lcs < prev_len - lcp && lcs < new_len - lcp &&
           prev_flat[prev_len - 1 - lcs] == new_flat[new_len - 1 - lcs]) {
        ++lcs;
    }
    lcp = SnapToUtf8CharStart(prev_flat, lcp);
    size_t diff_s = lcp;
    size_t prev_e = prev_len - lcs;
    size_t new_e = new_len - lcs;
    if (prev_e < prev_len) prev_e = SnapToUtf8CharStart(prev_flat, prev_e);
    if (new_e < new_len) new_e = SnapToUtf8CharStart(new_flat, new_e);
    if (prev_e < diff_s) prev_e = diff_s;
    if (new_e < diff_s) new_e = diff_s;

    // Step B: 按差异区段做增量平移（prev → new image_spans）。
    // 删除 prev 中落在差异区段内的所有 image —— 这与 iOS NSTextStorage 在用户编辑
    // 时把整个 NSTextAttachment 当作 1 个字符删除的行为一致：用户键入或删除时，
    // 占位空格（attachment 在 flat 中的 1-char 表示）会被一同覆盖，故视为整段被吞。
    // 落在差异区段之后的 image 整体平移 (new_e - prev_e)，可能为负（缩短）或正（扩展）。
    long long shift = static_cast<long long>(new_e) - static_cast<long long>(prev_e);
    out_new_image_spans.reserve(prev_image_spans.size());
    for (const auto &rec : prev_image_spans) {
        if (rec.flat_offset < diff_s) {
            out_new_image_spans.push_back(rec);  // 不变
        } else if (rec.flat_offset >= prev_e) {
            KRTextEditorState::KRImageSpanRecord moved = rec;
            // 极端 corner case 防御：shift 不应让 flat_offset 越过 0；按理论模型不
            // 会发生（prev_e 之后的 image 经平移后只会落到 new_e 之后的合法位置），
            // 但仍 saturate 一下避免环绕。
            long long new_offset = static_cast<long long>(rec.flat_offset) + shift;
            if (new_offset < 0) new_offset = 0;
            moved.flat_offset = static_cast<size_t>(new_offset);
            out_new_image_spans.push_back(std::move(moved));
        } else {
            // 落在差异区段内：被吞掉，丢弃。
        }
    }

    // Step C: 以 new_flat 为底，从右到左把 image 占位空格替换回 raw_literal。
    std::string raw = new_flat;
    for (auto it = out_new_image_spans.rbegin(); it != out_new_image_spans.rend(); ++it) {
        if (it->raw_literal.empty()) continue;
        if (it->flat_offset >= raw.size()) continue;
        // 若该位置在 new_flat 中不再是占位空格（极端：用户编辑使其变成别的字符），
        // 仍然按权威映射强制替换为 raw_literal —— 因为 image_spans_ 是这条逻辑的权威源；
        // 但实际触发概率极低（差分平移已把所有"被编辑"的 image 剔除）。
        raw.replace(it->flat_offset, 1, it->raw_literal);
    }

    // Step D: 同步重算 out_new_image_spans 中每条记录的 utf16_offset —— 它依赖
    // new_flat 内 image 占位空格之前的 UTF-16 长度。差分平移按字节做，UTF-16 偏移
    // 在新 flat 中需要重新扫描；为避免 O(N²) substr，这里基于已升序的 flat_offset
    // 单趟扫描 new_flat 的 UTF-8 字节，跨过每个字符就把 utf16 cursor 累加。
    {
        size_t byte_idx = 0;
        size_t utf16_idx = 0;
        size_t flat_size = new_flat.size();
        size_t span_idx = 0;
        size_t span_count = out_new_image_spans.size();
        while (byte_idx < flat_size && span_idx < span_count) {
            // 在到达下一个待标定的 image 占位字节之前，按字节累加 UTF-16 长度。
            while (span_idx < span_count &&
                   byte_idx == out_new_image_spans[span_idx].flat_offset) {
                out_new_image_spans[span_idx].utf16_offset = static_cast<uint32_t>(utf16_idx);
                ++span_idx;
            }
            if (span_idx >= span_count) break;
            unsigned char c = static_cast<unsigned char>(new_flat[byte_idx]);
            int char_byte_len = (c < 0x80) ? 1
                              : (c < 0xC0) ? 1   // 续字节（异常容错，按 1 推进，避免死循环）
                              : (c < 0xE0) ? 2
                              : (c < 0xF0) ? 3
                              :              4;
            byte_idx += char_byte_len;
            utf16_idx += (char_byte_len >= 4) ? 2 : 1;
        }
        // 文末仍有未标定的 image 记录（极端：image 落在 new_flat 末尾）。
        while (span_idx < span_count) {
            out_new_image_spans[span_idx].utf16_offset = static_cast<uint32_t>(utf16_idx);
            ++span_idx;
        }
    }
    return raw;
}

// 由 image_spans_ 与给定 raw 反推出对应 ArkUI flat 文本。
// 用途：OnTextDidChanged 调用 ReconstructRawFromFlat 时需要传 prev_flat；prev_flat
// 不需要每次重新去 ArkUI 节点 GetContentText（那是 new_flat），而是基于「上一次
// 写入 ArkUI 时的 raw + image_spans_」推导出来——这是 prev_flat 的权威定义。
//
// 推导规则：把 raw 中每条 image_spans_ 记录的 raw_literal 替换为单空格占位符；
// 由于 image_spans_ 持有的是 flat_offset（不是 raw 中的偏移），无法直接基于它做
// 切片，因此这里采用「以 image_spans_ 顺序为权威，依次在 raw 中查找 raw_literal
// 并替换」的策略——线性扫描，O(N+M)。
inline std::string DeriveFlatFromRaw(
    const std::string &raw,
    const std::vector<KRTextEditorState::KRImageSpanRecord> &image_spans) {
    if (image_spans.empty()) {
        return raw;
    }
    // 以 image_spans 顺序逐条把 raw_literal 替换为单空格；从前往后线性推进 cursor，
    // 与 SetStyledText 中 spans 顺序一致 → adapter 在 raw 中匹配 image 的相对顺序。
    std::string flat;
    flat.reserve(raw.size());
    size_t raw_cursor = 0;
    for (const auto &rec : image_spans) {
        if (rec.raw_literal.empty()) {
            // 未携带 raw_literal 的 image：无法从 raw 中定位它，跳过 → 后续直接把
            // raw 剩余部分整体追加。这种退化只在业务用旧的 AppendImageSpan 接口
            // （不传 raw_literal）时出现。
            continue;
        }
        size_t pos = raw.find(rec.raw_literal, raw_cursor);
        if (pos == std::string::npos) {
            // 异常：image_spans_ 与 raw 不匹配（可能业务在 SetStyledText 后又用其他
            // 路径直接修改了 cached_text_）。退化：把 raw 剩余部分整体追加。
            break;
        }
        flat.append(raw, raw_cursor, pos - raw_cursor);
        flat.push_back(kArkUIImageSpanPlaceholder);
        raw_cursor = pos + rec.raw_literal.size();
    }
    if (raw_cursor < raw.size()) {
        flat.append(raw, raw_cursor, raw.size() - raw_cursor);
    }
    return flat;
}

// OnTextDidChanged 入口的便捷封装：基于 state 中的 prev raw / image_spans_ 与
// 当前 ArkUI flat 文本，推算新的 raw 文本，并就地更新 state.image_spans_。
// 调用方（KRTextEditorFieldView::OnTextDidChanged）只需要：
//   auto new_raw = RebuildRawAfterUserEdit(state, GetContentText());
//   state_.cached_text_ = new_raw;
inline std::string RebuildRawAfterUserEdit(KRTextEditorState &state,
                                           const std::string &new_flat) {
    std::string prev_flat = DeriveFlatFromRaw(state.cached_text_, state.image_spans_);
    std::vector<KRTextEditorState::KRImageSpanRecord> new_spans;
    std::string new_raw = ReconstructRawFromFlat(state.image_spans_, prev_flat,
                                                  new_flat, new_spans);
    state.image_spans_ = std::move(new_spans);
    return new_raw;
}

// ============================================================================
// Selection 坐标双向映射（基于 image_spans_）
//
// ArkUI GetSelection / SetSelection 的单位是 UTF-16 code unit，且作用于「flat」
// 文本（image span 在 flat 里占 1 个 UTF-16 code unit = 1 个 ASCII 空格）。
// 但业务侧（Kuikly Kotlin / Android / iOS）拿到的 selection 必须是「raw」文本上
// 的 UTF-16 code unit 偏移——例如 raw="[smile]a"（utf16Len=8），光标点在 'a'
// 之前对应 raw 的 7，对应 flat 的 1。
//
// 缺少这层映射时，业务把 flat selection 直接当 raw selection 用，会出现：
//   * 受控插入：业务 substring(0, 4) + insert + substring(4) —— 把 [smile] 切到
//     `[smi` 与 `le]` 之间，bug 表现为 "heart 被插到 smi 和 le 之间"。
//   * 选区高亮范围错位、删除区段错位等。
//
// 算法（与 iOS p_getInputCursorIndex / p_getOutputCursorIndex 同构）：
//   * FlatUtf16ToRawUtf16: 上抛业务时使用。从 0 开始扫描 image_spans_，每一条
//     flat_offset_utf16 < flat_cursor 的 image 把 cursor 加上
//     (utf16Len(raw_literal) - 1)；若 cursor 命中 image 占位字符（==flat_offset_utf16），
//     约定归到该 image 之前（不展开），与 iOS 行为一致。
//   * RawUtf16ToFlatUtf16: 业务下发时使用。逆向遍历，每条 image 对应 raw 中起点
//     = utf16_offset + Σ(prev images: utf16Len(raw_literal) - 1)；cursor 落在 image
//     的 raw 区间内时，扩展到 image 起点（shortcode 不可分割原子）。
// ============================================================================

inline uint32_t FlatUtf16ToRawUtf16(
    const std::vector<KRTextEditorState::KRImageSpanRecord> &image_spans,
    uint32_t flat_cursor) {
    uint32_t raw_cursor = flat_cursor;
    for (const auto &rec : image_spans) {
        if (rec.utf16_offset >= flat_cursor) {
            // 该 image 在光标位置或之后，无须为 cursor 之前的 image 做扩展。
            break;
        }
        // image 在 cursor 之前：raw 中占 utf16Len(raw_literal)，flat 中占 1 → 多出
        // (utf16Len(raw_literal) - 1)。
        if (rec.raw_literal.empty()) continue;  // 兼容旧 AppendImageSpan 的退化路径
        int lit_u16 = GetUTF16Length(rec.raw_literal);
        if (lit_u16 > 1) {
            raw_cursor += static_cast<uint32_t>(lit_u16 - 1);
        }
    }
    return raw_cursor;
}

inline uint32_t RawUtf16ToFlatUtf16(
    const std::vector<KRTextEditorState::KRImageSpanRecord> &image_spans,
    uint32_t raw_cursor) {
    uint32_t flat_cursor = raw_cursor;
    uint32_t raw_consumed_extra = 0;  // 已遍历 image 在 raw 中比 flat 多出的 UTF-16 长度合计
    for (const auto &rec : image_spans) {
        if (rec.raw_literal.empty()) continue;
        int lit_u16 = GetUTF16Length(rec.raw_literal);
        if (lit_u16 <= 0) continue;
        // 该 image 在 raw 文本中的起点（UTF-16 偏移）：
        //   raw_image_start = flat 上 image 的 UTF-16 偏移 + 此前 image 的额外长度
        uint32_t raw_image_start = rec.utf16_offset + raw_consumed_extra;
        if (raw_cursor <= raw_image_start) {
            // cursor 在该 image 之前，无须再扣减；后续 image 也都在 cursor 之后，break。
            break;
        }
        uint32_t raw_image_end = raw_image_start + static_cast<uint32_t>(lit_u16);
        if (raw_cursor < raw_image_end) {
            // cursor 落在 image 的 raw 区间内 —— shortcode 不可分割，收缩到 image 起点。
            return rec.utf16_offset;
        }
        // cursor 在该 image 之后：扣减 (lit_u16 - 1)。
        flat_cursor -= static_cast<uint32_t>(lit_u16 - 1);
        raw_consumed_extra += static_cast<uint32_t>(lit_u16 - 1);
    }
    return flat_cursor;
}

//
// 注：以上 ReconstructRawFromFlat / DeriveFlatFromRaw / RebuildRawAfterUserEdit
// 已替换掉旧的 FlattenRawForArkUI + 二次推断 lcp/lcs 实现 —— 旧实现依赖
// 「再调一次 adapter 推断 image 在 raw 中的字节区间」，对相同 shortcode 重复出现、
// shortcode 与相邻文本字符重叠等场景脆弱（详见 commit 历史）。新实现以 SetStyledText
// 写入时记录的 image_spans_ 作为权威映射，编辑差分回写不再依赖任何二次推断。
// ============================================================================


// 从给定 descriptor 中安全读取文本内容（UTF-8）。
//
// 实现要点（依据官方 TextEditorMaker.cpp 示例 + 实测崩溃修复）：
//   * **禁止** 使用 `probe-then-alloc` 模式（即先 `GetString(desc, nullptr, 0, &len)` 探测
//     再按大小分配）。实测这种模式会让 SDK 侧 descriptor 进入非法状态，随后 `Destroy`
//     就会在 `StyledStringAdapter::DestroyArkUIStyledStringDescriptor -> free_default`
//     崩溃。
//   * 改为一次性分配固定大小（默认 8KB）栈 / 堆 buffer，直接 `GetString`，与官方示例
//     `char buffer[BUFFER_SIZE]` 的模式一致。
//   * 对于 > 8KB 的输入（极罕见），当前实现会截断到 8KB；如遇业务超长场景，可在此调大。
static constexpr size_t kDescriptorReadBufferSize = 8192;

inline std::string ReadDescriptorString(const ArkUI_StyledString_Descriptor *desc) {
    if (!desc) {
        return "";
    }
    std::vector<char> buf(kDescriptorReadBufferSize, '\0');
    int32_t actual = 0;
    ArkUI_ErrorCode code = OH_ArkUI_StyledString_Descriptor_GetString(
        desc, buf.data(), static_cast<int32_t>(buf.size()), &actual);
    if (code != ARKUI_ERROR_CODE_NO_ERROR) {
        return "";
    }
    // actual 可能包含或不包含尾 '\0'；用 strnlen 裁掉哨兵字节并兼容 actual == 0 的空串场景
    size_t real_len = strnlen(buf.data(), static_cast<size_t>(actual > 0 ? actual : 0));
    return std::string(buf.data(), real_len);
}

// 读取 controller 当前完整文本（UTF-8）。
//
// 实现：
//   * `Create()` 一个空 desc → `GetStyledString(ctrl, desc)` 让系统填充内容
//     → 通过 `ReadDescriptorString` 一次性读 UTF-8 → `Destroy(desc)`
//   * `ReadDescriptorString` 使用固定大小 buffer，不使用 probe 模式（详见其函数注释）
inline std::string GetStyledText(const KRTextEditorState &state) {
    if (!state.controller_) {
        return "";
    }
    ArkUI_StyledString_Descriptor *desc = OH_ArkUI_StyledString_Descriptor_Create();
    if (!desc) {
        return "";
    }
    std::string ret;
    if (OH_ArkUI_TextEditorStyledStringController_GetStyledString(state.controller_, desc) ==
        ARKUI_ERROR_CODE_NO_ERROR) {
        ret = ReadDescriptorString(desc);
    }
    // TODO(kuikly-text-editor): 同 SetStyledText 中的注释——OH_ArkUI_StyledString_Descriptor_Destroy
    // 在真机上会触发 free_default 崩溃（GetStyledText 被 ON_DID_CHANGE 高频调用，
    // 任何一次 Destroy 失败就会直接停机）。暂注释掉 Destroy，每次泄漏一个
    // 空壳 descriptor（无附加数据），作为后续跟进点。
    // OH_ArkUI_StyledString_Descriptor_Destroy(desc);
    (void)desc;
    return ret;
}

// 把 state 中的 placeholder_text_ / font / color 打包成 Options，
// 通过 NODE_TEXT_EDITOR_PLACEHOLDER 属性生效。
inline void ApplyPlaceholder(ArkUI_NodeHandle node, const KRTextEditorState &state) {
    if (!node) {
        return;
    }
    OH_ArkUI_TextEditorPlaceholderOptions *options = OH_ArkUI_TextEditorPlaceholderOptions_Create();
    if (!options) {
        return;
    }
    OH_ArkUI_TextEditorPlaceholderOptions_SetValue(options, state.placeholder_text_.c_str());
    OH_ArkUI_TextEditorPlaceholderOptions_SetFontSize(options, state.font_size_);
    OH_ArkUI_TextEditorPlaceholderOptions_SetFontWeight(options, static_cast<uint32_t>(state.font_weight_));
    if (state.placeholder_color_set_) {
        OH_ArkUI_TextEditorPlaceholderOptions_SetFontColor(options, state.placeholder_color_);
    }
    ArkUI_AttributeItem item = {};
    item.object = options;
    kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_EDITOR_PLACEHOLDER, &item);
    OH_ArkUI_TextEditorPlaceholderOptions_Destroy(options);
}

// 节点级属性：光标颜色 NODE_TEXT_EDITOR_CARET_COLOR
inline void UpdateCaretColor(ArkUI_NodeHandle node, uint32_t hex_color) {
    ArkUI_NumberValue value = {.u32 = hex_color};
    ArkUI_AttributeItem item = {&value, sizeof(ArkUI_NumberValue) / sizeof(ArkUI_NumberValue)};
    kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_EDITOR_CARET_COLOR, &item);
}

// 节点级属性：EnterKeyType
inline void UpdateEnterKeyType(ArkUI_NodeHandle node, ArkUI_EnterKeyType type) {
    ArkUI_NumberValue value = {.i32 = static_cast<int32_t>(type)};
    ArkUI_AttributeItem item = {&value, 1};
    kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_EDITOR_ENTER_KEY_TYPE, &item);
}

// 节点级属性：MaxLength（按 UTF-16 code unit 计算，与老实现同口径）
inline void UpdateMaxLengthAttr(ArkUI_NodeHandle node, int32_t max_length) {
    ArkUI_NumberValue value = {.i32 = max_length};
    ArkUI_AttributeItem item = {&value, 1};
    kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_EDITOR_MAX_LENGTH, &item);
}
inline void ResetMaxLengthAttr(ArkUI_NodeHandle node) {
    kuikly::util::GetNodeApi()->resetAttribute(node, NODE_TEXT_EDITOR_MAX_LENGTH);
}

// 节点级属性：MaxLines（仅多行使用）
inline void UpdateMaxLines(ArkUI_NodeHandle node, int32_t max_lines) {
    ArkUI_NumberValue value = {.i32 = max_lines};
    ArkUI_AttributeItem item = {&value, 1};
    kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_EDITOR_MAX_LINES, &item);
}

// 节点级属性：SingleLine（仅单行使用）
inline void UpdateSingleLine(ArkUI_NodeHandle node, bool single_line) {
    ArkUI_NumberValue value = {.i32 = single_line ? 1 : 0};
    ArkUI_AttributeItem item = {&value, 1};
    kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_EDITOR_SINGLE_LINE, &item);
}

// Focus / Blur：使用通用 NODE_FOCUS_STATUS。
inline void UpdateFocusStatus(ArkUI_NodeHandle node, bool focus) {
    ArkUI_NumberValue value = {.i32 = focus ? 1 : 0};
    ArkUI_AttributeItem item = {&value, 1};
    kuikly::util::GetNodeApi()->setAttribute(node, NODE_FOCUS_STATUS, &item);
}

// focusable
inline void UpdateFocusable(ArkUI_NodeHandle node, bool focusable) {
    ArkUI_NumberValue value = {.i32 = focusable ? 1 : 0};
    ArkUI_AttributeItem item = {&value, 1};
    kuikly::util::GetNodeApi()->setAttribute(node, NODE_FOCUSABLE, &item);
}

// 光标偏移 via controller
inline int32_t GetCaretOffset(const KRTextEditorState &state) {
    if (!state.controller_) {
        return 0;
    }
    int32_t offset = 0;
    OH_ArkUI_TextEditorStyledStringController_GetCaretOffset(state.controller_, &offset);
    return offset;
}

inline void SetCaretOffset(KRTextEditorState &state, int32_t offset) {
    if (!state.controller_) {
        return;
    }
    OH_ArkUI_TextEditorStyledStringController_SetCaretOffset(state.controller_, offset);
}
#endif  // KUIKLY_TEXT_EDITOR_AVAILABLE

// ========== 文本长度计算（与老 KRTextFieldView 同算法的独立复刻） ==========

inline int GetUTF8ByteLengthOfFirstCharacter(unsigned char c) {
    if ((c & 0x80) == 0) {
        return 1;
    } else if ((c & 0xE0) == 0xC0) {
        return 2;
    } else if ((c & 0xF0) == 0xE0) {
        return 3;
    } else /*if ((c & 0xF8) == 0xF0)*/ {
        return 4;
    }
}

inline int GetUTF8ByteLengthOfCodePoint(char32_t codePoint) {
    if (codePoint <= 0x7F) {
        return 1;
    } else if (codePoint <= 0x7FF) {
        return 2;
    } else if (codePoint <= 0xFFFF) {
        return 3;
    } else {
        return 4;
    }
}

inline int GetVisualWidthOfCodePoint(char32_t codePoint) {
    if (codePoint < 128) {
        return 1;
    }
    if (codePoint >= 0x200B && codePoint <= 0x200D) {
        return 1;
    }
    if (codePoint == 0xFEFF) {
        return 1;
    }
    return 2;
}

inline int GetUTF8ByteCount(const std::string &text, size_t u8Start, size_t u16Count) {
    size_t textLength = text.length();
    size_t u8Index = u8Start;
    size_t u16Index = 0;
    while (u16Index < u16Count && u8Index < textLength) {
        unsigned char c = static_cast<unsigned char>(text[u8Index]);
        int byteLength = GetUTF8ByteLengthOfFirstCharacter(c);
        u8Index += byteLength;
        u16Index += byteLength >= 4 ? 2 : 1;
    }
    return static_cast<int>(u8Index - u8Start);
}

inline int GetUTF16Length(const std::string &text) {
    size_t textLength = text.length();
    size_t u8Index = 0;
    size_t u16Index = 0;
    while (u8Index < textLength) {
        unsigned char c = static_cast<unsigned char>(text[u8Index]);
        int byteLength = GetUTF8ByteLengthOfFirstCharacter(c);
        u8Index += byteLength;
        u16Index += byteLength >= 4 ? 2 : 1;
    }
    return static_cast<int>(u16Index);
}

inline int CalculateTextLength(int length_limit_type, const std::string &text, size_t rmStart = 0,
                               size_t rmEnd = 0) {
    switch (length_limit_type) {
        case 0: {  // BYTE
            auto size = text.length();
            if (rmEnd > rmStart) {
                auto byteCountToStart = GetUTF8ByteCount(text, 0, rmStart);
                auto byteCountToEnd = GetUTF8ByteCount(text, byteCountToStart, rmEnd - rmStart);
                size -= byteCountToEnd;
            }
            return static_cast<int>(size);
        }
        case 1: {  // CHARACTER
            auto u32text = kuikly::util::ConvertToU32String(text);
            auto size = u32text.length();
            if (rmEnd > rmStart) {
                size_t u32Index = 0;
                size_t u16Index = 0;
                while (u16Index < rmStart && u32Index < size) {
                    u16Index += (u32text[u32Index] > 0xFFFF) ? 2 : 1;
                    u32Index++;
                }
                auto u32Start = u32Index;
                while (u16Index < rmEnd && u32Index < size) {
                    u16Index += (u32text[u32Index] > 0xFFFF) ? 2 : 1;
                    u32Index++;
                }
                size -= (u32Index - u32Start);
            }
            return static_cast<int>(size);
        }
        case 2: {  // VISUAL_WIDTH
            auto u32text = kuikly::util::ConvertToU32String(text);
            auto size = u32text.length();
            int visualWidth = 0;
            size_t u16Index = 0;
            for (size_t i = 0; i < size; ++i) {
                char32_t codePoint = u32text[i];
                if (u16Index < rmStart || u16Index >= rmEnd) {
                    visualWidth += GetVisualWidthOfCodePoint(codePoint);
                }
                u16Index += (codePoint > 0xFFFF) ? 2 : 1;
            }
            return visualWidth;
        }
        default:
            return 0;
    }
}

inline int CalculateTruncateIndex(int length_limit_type, const std::string &text, size_t keep) {
    switch (length_limit_type) {
        case 0: {
            size_t textLength = text.length();
            size_t byteIndex = 0;
            while (byteIndex < textLength) {
                unsigned char c = static_cast<unsigned char>(text[byteIndex]);
                int pointBytes = GetUTF8ByteLengthOfFirstCharacter(c);
                if (byteIndex + pointBytes > keep) {
                    break;
                }
                byteIndex += pointBytes;
            }
            return static_cast<int>(byteIndex);
        }
        case 1: {
            size_t textLength = text.length();
            size_t byteIndex = 0;
            for (size_t i = 0; i < keep && byteIndex < textLength; ++i) {
                unsigned char c = static_cast<unsigned char>(text[byteIndex]);
                byteIndex += GetUTF8ByteLengthOfFirstCharacter(c);
            }
            return static_cast<int>(byteIndex);
        }
        case 2: {
            auto u32text = kuikly::util::ConvertToU32String(text);
            size_t u32Length = u32text.length();
            size_t visualWidth = 0;
            size_t byteIndex = 0;
            for (size_t i = 0; i < u32Length; ++i) {
                auto u32Char = u32text[i];
                int charWidth = GetVisualWidthOfCodePoint(u32Char);
                if (visualWidth + charWidth > keep) {
                    break;
                }
                visualWidth += charWidth;
                byteIndex += GetUTF8ByteLengthOfCodePoint(u32Char);
            }
            return static_cast<int>(byteIndex);
        }
        default:
            return 0;
    }
}

// 根据 state.length_limit_type_ / state.max_length_ 对 source 进行过滤。语义与老
// KRTextFieldView::filter 完全一致：返回 true 表示截断发生（source 已被 '\0' 结尾改写）。
inline bool FilterSource(char source[], const std::string &dest, size_t dStart, size_t dEnd,
                         const KRTextEditorState &state) {
    if (source[0] == '\0') {
        return false;
    }
    int32_t keep = state.max_length_ -
                   CalculateTextLength(state.length_limit_type_, dest, dStart, dEnd);
    if (keep >= CalculateTextLength(state.length_limit_type_, source)) {
        return false;
    } else if (keep <= 0) {
        source[0] = '\0';
        return true;
    } else {
        auto index = CalculateTruncateIndex(state.length_limit_type_, source, static_cast<size_t>(keep));
        source[index] = '\0';
        return true;
    }
}

// ========== textInputState 协议工具 ==========
//
// 与 Android KRTextFieldView.createTextInputStateParamMap / setTextInputState 行为对齐：
//   * payload 字段：text / selectionStart / selectionEnd / compositionStart / compositionEnd / length?
//   * selection 钳制到 [0, utf16Len(text)]，以 UTF-16 code unit 为单位（Kotlin/iOS/ArkUI 选区 API 同口径）
//   * compositionStart / compositionEnd 始终 -1（OHOS TEXT_EDITOR API 不暴露 IME 组合区间）
//   * length 仅在 lengthLimitType != -1 时填充
//
// 选区设置：优先 OH_ArkUI_TextEditorStyledStringController_SetSelection（API 24+），
// 失败时降级到 SetCaretOffset(end)，把选区折叠为光标。

#if KUIKLY_TEXT_EDITOR_AVAILABLE
// 把 [start, end] 区间设到 controller。menuPolicy 取 DEFAULT，避免主动弹/收选择菜单。
// 当 SDK 调用失败时降级到 SetCaretOffset(end)，与老 KRTextFieldView 行为兜底语义一致：
// 至少光标位置正确。
inline bool SetSelectionRange(KRTextEditorState &state, uint32_t start, uint32_t end) {
    if (!state.controller_) {
        return false;
    }
    ArkUI_ErrorCode code = OH_ArkUI_TextEditorStyledStringController_SetSelection(
        state.controller_, start, end, ArkUI_MenuPolicy::ARKUI_MENU_POLICY_DEFAULT);
    if (code == ARKUI_ERROR_CODE_NO_ERROR) {
        return true;
    }
    // 降级：折叠为光标到 end。
    OH_ArkUI_TextEditorStyledStringController_SetCaretOffset(state.controller_,
                                                             static_cast<int32_t>(end));
    return false;
}
#endif

// 读取当前选区（UTF-16 单位）。无 selection 区间时返回 caret 折叠：start==end==caret。
// out_start / out_end 已钳制到 [0, utf16Len(text)]。
inline void ReadSelection(const KRTextEditorState &state, const std::string &text,
                          uint32_t *out_start, uint32_t *out_end) {
    uint32_t start = 0;
    uint32_t end = 0;
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    if (state.controller_) {
        if (OH_ArkUI_TextEditorStyledStringController_GetSelection(state.controller_, &start, &end) !=
            ARKUI_ERROR_CODE_NO_ERROR) {
            start = end = 0;
        }
        // 无选区（start==end）时 GetSelection 可能返回 0/0；用 caret 兜底，使 caret 位置回流到 state。
        if (start == end) {
            int32_t caret = 0;
            if (OH_ArkUI_TextEditorStyledStringController_GetCaretOffset(state.controller_,
                                                                         &caret) ==
                ARKUI_ERROR_CODE_NO_ERROR &&
                caret > 0) {
                start = end = static_cast<uint32_t>(caret);
            }
        }
    }
#endif
    uint32_t max_pos = static_cast<uint32_t>(GetUTF16Length(text));
    if (start > max_pos) start = max_pos;
    if (end > max_pos) end = max_pos;
    if (out_start) *out_start = start;
    if (out_end) *out_end = end;
}

// 构造 textInputState payload（与 Android createTextInputStateParamMap 字段对齐）。
inline KRRenderValueMap BuildTextInputStatePayload(const KRTextEditorState &state) {
    // 对外暴露的 text 必须是「raw shortcode 文本」（与 iOS/Android 一致），
    // 而非 ArkUI flat（image span 被占位空格扁平化后的产物）。SetStyledText /
    // OnTextDidChanged / SetTextInputStateInternal 三条路径都会同步更新 cached_text_
    // 为最新 raw，这里直接读 cached_text_ 即可。
    // 兜底：cached_text_ 为空但控件中有内容（极端 dispose 顺序）时，回退到
    // GetStyledText（拿到的可能是 flat，仅作为不丢失数据的最后一道防线）。
    std::string text = state.cached_text_;
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    if (text.empty()) {
        text = GetStyledText(state);
    }
#endif
    uint32_t sel_start = 0;
    uint32_t sel_end = 0;
    ReadSelection(state, text, &sel_start, &sel_end);
    // ReadSelection 拿到的是 ArkUI flat 上的 UTF-16 偏移；上抛业务前必须映射到
    // raw 上的 UTF-16 偏移，否则业务侧把 flat 坐标当 raw 坐标用会切碎 shortcode
    // （bug 表现：插入新 emoji 时落到已有 shortcode 字面量中间）。
    sel_start = FlatUtf16ToRawUtf16(state.image_spans_, sel_start);
    sel_end = FlatUtf16ToRawUtf16(state.image_spans_, sel_end);

    KRRenderValueMap map;
    map[kKeyText] = NewKRRenderValue(text);
    map[kKeySelectionStart] = NewKRRenderValue(static_cast<int>(sel_start));
    map[kKeySelectionEnd] = NewKRRenderValue(static_cast<int>(sel_end));
    map[kKeyCompositionStart] = NewKRRenderValue(static_cast<int>(kNoComposition));
    map[kKeyCompositionEnd] = NewKRRenderValue(static_cast<int>(kNoComposition));
    if (state.length_limit_type_ != -1) {
        int length = CalculateTextLength(state.length_limit_type_, text);
        map[kKeyLength] = NewKRRenderValue(length);
    }
    return map;
}

// textInputState JSON 解析结果。出参语义与 Android setTextInputState 解析一致：
//   * 缺省 selectionStart 时，使用 text 的 utf16Len 作为兜底（即光标置末尾）
//   * 缺省 selectionEnd 时，等于 selectionStart
//   * 钳制到 [0, utf16Len(text)]
struct ParsedTextInputState {
    std::string text;
    uint32_t selection_start = 0;
    uint32_t selection_end = 0;
};

// 解析 JSON 字符串到 ParsedTextInputState；解析失败返回空 text + 0 光标，与 Android 一致。
inline ParsedTextInputState ParseTextInputStateJson(const std::string &json) {
    ParsedTextInputState ret;
    if (json.empty()) {
        return ret;
    }
    auto value = NewKRRenderValue(json);
    auto map = value->toMap();  // KRRenderValue::toMap() 在字符串场景下走 cJSON_Parse
    auto text_it = map.find(kKeyText);
    if (text_it != map.end() && text_it->second) {
        ret.text = text_it->second->toString();
    }
    uint32_t max_pos = static_cast<uint32_t>(GetUTF16Length(ret.text));
    bool has_start = false;
    auto start_it = map.find(kKeySelectionStart);
    if (start_it != map.end() && start_it->second) {
        int v = start_it->second->toInt();
        ret.selection_start = v < 0 ? 0 : (static_cast<uint32_t>(v) > max_pos ? max_pos
                                                                              : static_cast<uint32_t>(v));
        has_start = true;
    } else {
        ret.selection_start = max_pos;
    }
    auto end_it = map.find(kKeySelectionEnd);
    if (end_it != map.end() && end_it->second) {
        int v = end_it->second->toInt();
        ret.selection_end = v < 0 ? 0 : (static_cast<uint32_t>(v) > max_pos ? max_pos
                                                                            : static_cast<uint32_t>(v));
    } else {
        ret.selection_end = ret.selection_start;
    }
    (void)has_start;
    return ret;
}

}  // namespace text_editor
}  // namespace kuikly

#endif  // KUIKLY_TEXT_EDITOR_AVAILABLE

#endif  // CORE_RENDER_OHOS_KRTEXTEDITORCOMMON_H

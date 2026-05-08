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
// 均为 OpenHarmony API 24 引入。若当前 SDK header 不足 API 24，则通过该 guard 将新实现禁用：
//   * CreateNode() 返回 nullptr；
//   * 注册入口再以运行时 OH_GetSdkApiVersion() >= 24 兜底，永远走老实现路径。
#if defined(OH_CURRENT_API_VERSION) && OH_CURRENT_API_VERSION < 24
#define KUIKLY_TEXT_EDITOR_UNAVAILABLE 1
#endif

#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
#include <arkui/styled_string.h>
#endif

#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/utils/KRConvertUtil.h"
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

static constexpr const char kMethodFocus[] = "focus";
static constexpr const char kMethodBlur[] = "blur";
static constexpr const char kMethodSetText[] = "setText";
static constexpr const char kMethodGetCursorIndex[] = "getCursorIndex";
static constexpr const char kMethodSetCursorIndex[] = "setCursorIndex";

static constexpr const char kEventTextDidChanged[] = "textDidChange";
static constexpr const char kEventInputFocus[] = "inputFocus";
static constexpr const char kEventInputBlur[] = "inputBlur";
static constexpr const char kEventInputReturn[] = "inputReturn";
static constexpr const char kEventTextLengthBeyondLimit[] = "textLengthBeyondLimit";
static constexpr const char kEventKeyboardHeightChange[] = "keyboardHeightChange";

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
    // 记录"文本发生过变化但 textDidChange 回调尚未注册"的挂起状态。
    // Kuikly 侧先下发属性（包括 text），再下发事件；若首次 SetContentText 早于
    // textDidChange 事件注册，则 OnTextDidChanged 会因 callback==null 丢失一次回调。
    // 我们在此打标，等事件注册到达时立即补发一次，保证 UI 上能观察到初始文本的变化事件。
    bool pending_text_did_change_ = false;

    KRRenderCallback text_did_change_callback_;
    KRRenderCallback input_focus_callback_;
    KRRenderCallback input_blur_callback_;
    KRRenderCallback input_return_callback_;
    KRRenderCallback text_length_beyond_limit_callback_;
    KRRenderCallback keyboard_height_changed_callback_;

#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    // 注意：controller 由调用侧 Create，并通过 NODE_TEXT_EDITOR_STYLED_STRING_CONTROLLER 绑定到
    // 节点；节点销毁时要 Destroy。
    OH_ArkUI_TextEditorStyledStringController *controller_ = nullptr;
#else
    void *controller_ = nullptr;
#endif

    // 当前已设置到节点的文本（逻辑层缓存，供 SetProp text 幂等判断及过滤使用）
    std::string cached_text_;

    // returnKeyType 上一次设置值的原始字符串，用于 OnSubmit 回调时回传 ime_action。
    ArkUI_EnterKeyType enter_key_type_ = ARKUI_ENTER_KEY_TYPE_DONE;
};

// ========== StyledString / Controller 读写 ==========

// 前向声明：SetStyledText 需要在 span 长度上使用 UTF-16 code unit 口径，与文件下方
// "文本长度计算" 块中 GetUTF16Length 复用实现。这里仅作声明，定义在同文件末尾。
inline int GetUTF16Length(const std::string &text);

#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
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
inline void SetStyledText(KRTextEditorState &state, const std::string &text) {
    if (!state.controller_) {
        return;
    }
    // 计算 UTF-16 长度（与 SDK SpanStyle_SetLength 的口径一致）
    int32_t u16_len = GetUTF16Length(text);

    OH_ArkUI_TextStyle *text_style = OH_ArkUI_TextStyle_Create();
    OH_ArkUI_SpanStyle *span_style = OH_ArkUI_SpanStyle_Create();
    // 段落级样式（textAlign 等）：SpanStyle 走 OH_ArkUI_ParagraphStyle（非 TextEditor
    // 特化版本），是段落绑定到 span 范围的正式通道。仅靠 TypingParagraphStyle 只会
    // 影响「后续键入」，不会回写已有文本，因此必须在 span 层带上。
    OH_ArkUI_ParagraphStyle *para_style = OH_ArkUI_ParagraphStyle_Create();
    ArkUI_StyledString_Descriptor *desc = nullptr;

    if (text_style && span_style) {
        OH_ArkUI_TextStyle_SetFontColor(text_style, state.font_color_);
        OH_ArkUI_TextStyle_SetFontSize(text_style, state.font_size_);
        OH_ArkUI_TextStyle_SetFontWeight(text_style, static_cast<uint32_t>(state.font_weight_));

        OH_ArkUI_SpanStyle_SetStart(span_style, 0);
        OH_ArkUI_SpanStyle_SetLength(span_style, u16_len);
        OH_ArkUI_SpanStyle_SetTextStyle(span_style, text_style);

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
    state.cached_text_ = text;
}

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
#endif  // !KUIKLY_TEXT_EDITOR_UNAVAILABLE

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

}  // namespace text_editor
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRTEXTEDITORCOMMON_H

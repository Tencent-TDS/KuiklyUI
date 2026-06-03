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

// =============================================================================
// KRTextEditorCommon.cpp — KRTextEditorCommon.h 中大型 inline 实现的外置定义
//
// 历史背景：
//   原 KRTextEditorCommon.h 单文件 1652 行 / 87 KB，将所有 styledstring 构造、
//   raw↔flat 双向映射、UTF-16 偏移换算、文本长度计算、textInputState 序列化
//   等实现以 inline 方式直接写在头文件，导致每个 include 该头的 TU 都要重复
//   解析约 1000 行 inline 函数体，编译时间和调试体验都受拖累。
//
// 本次拆分原则：
//   1) 头文件保留：前置 include 块、KUIKLY_TEXT_EDITOR_AVAILABLE 编译宏、
//      全部 extern "C" 弱符号声明、KRTextEditorState / KRImageSpanRecord /
//      ParsedTextInputState 等结构体、协议字符串常量；以及 1-3 行的小型
//      节点 attribute setter（保留 inline 让编译器仍可内联）。
//   2) 本 cpp 接收：所有 ≥ 10 行的工具函数定义。它们在头文件中改为同名
//      非 inline 前向声明（保持 namespace 与原来一致：
//      `kuikly::text_editor::FuncName(...)`）。
//   3) 编译宏：原本被 `#if KUIKLY_TEXT_EDITOR_AVAILABLE ... #endif` 包护的
//      函数（即 SDK API 24 引入的 styledstring / controller 相关），这里
//      同样被同名宏包护；SDK header < 24 时整体不参与编译，与原有行为零差异。
//   4) Namespace：与头文件相同，仍在 `kuikly::text_editor` 命名空间内定义，
//      保证 ABI / ADL / 调用点签名零变化。
// =============================================================================

#include "libohos_render/expand/components/input/KRTextEditorCommon.h"

#if KUIKLY_TEXT_EDITOR_AVAILABLE
namespace kuikly {
namespace text_editor {

// ----------------------------------------------------------------------
// RebuildRawByMergingImageSpans — 把"占位空格 flat + 严格升序 image_spans"
// 单趟拼装为 raw（O(N + M)）。
//
// 不变量（详见 KRTextEditorCommon.h ReconstructRawFromFlat 注释）：
//   * 每条 image span 在 flat 中占 **1 个 ASCII 空格 (0x20)**；
//   * image_spans 按 flat_offset 严格单调递增；
//   * 跨过 raw_literal.empty() 或 flat_offset 越界的 span（防御兜底）。
//
// 复杂度：
//   旧实现：N 次 raw.replace(pos, 1, raw_literal) → 每次 O(M_tail)，
//         合计 O(N · M)（M = flat 末尾平均长度）；100+ emoji 富文本明显放慢。
//   本实现：append-only 单趟扫描 → O(N + M_total)，提前 reserve 终态长度
//         消除任何中途扩容。
// ----------------------------------------------------------------------
namespace {
inline std::string RebuildRawByMergingImageSpans(
    const std::string &flat,
    const std::vector<KRTextEditorState::KRImageSpanRecord> &spans) {
    // 1) 预算 raw 终态长度：每条有效 span 把 1 字节占位扩展为 raw_literal.size() 字节，
    //    净增量 = (raw_literal.size() - 1)。一次 reserve 让后续 append 全部 zero-realloc。
    size_t final_len = flat.size();
    for (const auto &s : spans) {
        if (s.raw_literal.empty()) {
            continue;
        }
        if (s.flat_offset >= flat.size()) {
            continue;
        }
        final_len += (s.raw_literal.size() - 1);
    }
    std::string raw;
    raw.reserve(final_len);

    // 2) 单趟扫描：用 cursor 跟踪 flat 已 copy 的位置；遇到 span 就 append 间隔
    //    + raw_literal，cursor 跨过那 1 个占位空格。
    size_t cursor = 0;
    for (const auto &s : spans) {
        if (s.raw_literal.empty()) {
            continue;
        }
        if (s.flat_offset >= flat.size()) {
            continue;
        }
        if (s.flat_offset < cursor) {
            continue;  // 防御：违反升序不变量则跳过
        }
        raw.append(flat, cursor, s.flat_offset - cursor);
        raw.append(s.raw_literal);
        cursor = s.flat_offset + 1;  // 跨过占位空格
    }
    // 3) 末尾剩余 flat 原文
    raw.append(flat, cursor, std::string::npos);
    return raw;
}
}  // namespace


#if KUIKLY_TEXT_EDITOR_AVAILABLE

// ----------------------------------------------------------------------
// ResolveLineHeightVp（迁自 KRTextEditorCommon.h 原行 444~454）
// ----------------------------------------------------------------------
float ResolveLineHeightVp(const KRTextEditorState &state) {
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

// ----------------------------------------------------------------------
// CreateTextStyleFromState（迁自 KRTextEditorCommon.h 原行 457~477）
// ----------------------------------------------------------------------
OH_ArkUI_TextEditorTextStyle *CreateTextStyleFromState(const KRTextEditorState &state,
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

// ----------------------------------------------------------------------
// CreateParagraphStyleFromState（迁自 KRTextEditorCommon.h 原行 479~490）
// ----------------------------------------------------------------------
OH_ArkUI_TextEditorParagraphStyle *CreateParagraphStyleFromState(const KRTextEditorState &state) {
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

// ----------------------------------------------------------------------
// ApplyTypingStyle（迁自 KRTextEditorCommon.h 原行 495~509）
// ----------------------------------------------------------------------
void ApplyTypingStyle(KRTextEditorState &state) {
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

// ----------------------------------------------------------------------
// BuildImageSpanDescriptor（迁自 KRTextEditorCommon.h 原行 521~577）
// ----------------------------------------------------------------------
ArkUI_StyledString_Descriptor *BuildImageSpanDescriptor(const std::string &resource_uri,
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
    // 优先走 SetPixelMap：当 KRCustomEmojiPixmapCache 已经在 RichText / 此前的
    // 输入流程中解码过同一 uri 时，这里同步拿到 OH_PixelmapNative*，直接绑定到
    // attachment，SDK 内部不会再发起异步 image loader 请求，从根源上消除
    // "插入自定义表情时一帧空白 / 闪动" 的问题。
    //
    // 缓存未命中：保持原有 SetResource 路径——首帧仍可能闪一次（取决于 SDK 内
    // 部的图片管线是否复用），但同时主动触发一次 Prefetch，把解码结果落到进程
    // 级缓存里，后续相同 uri 的输入都会走上面的 SetPixelMap 快路径。
    //
    // 注意：cache 持有 pixmap 所有权（caller 不释放），attachment 内部一般会
    // 自行 retain 或读取数据；descriptor 后续会接管 attachment，整体生命周期
    // 与原 SetResource 路径无差异。
    bool used_pixmap = false;
    if (OH_ArkUI_ImageAttachment_SetPixelMap) {
        // 持有 shared_ptr 强引用：在本 if 块作用域内，即便此刻被并发 Evict / TrimLocked
        // 从 cache 中移除，底层 pixmap 仍保活——SDK 已在 SetPixelMap 内部接管所需内容。
        // pm_holder 是局部变量，退出作用域时若 cache 那份引用尚未释放，析构只是
        // 把计数从 2 降到 1；若 cache 已先释放，那本地这份析构才真正触发底层 Release。
        if (auto pm_holder = KRCustomEmojiPixmapCache::GetInstance().Get(resource_uri)) {
            if (OH_ArkUI_ImageAttachment_SetPixelMap(attachment, pm_holder.get()) ==
                ARKUI_ERROR_CODE_NO_ERROR) {
                used_pixmap = true;
            }
        }
    }
    if (!used_pixmap) {
        OH_ArkUI_ImageAttachment_SetResource(attachment, resource_uri.c_str());
        // 异步预解码：下次插入同一 uri 的表情就能命中 SetPixelMap 快路径。
        // on_loaded 留空——仅触发预解码，不需要回调；调用是幂等的（同 uri 不
        // 重复发起解码）。
        KRCustomEmojiPixmapCache::GetInstance().Prefetch(resource_uri, nullptr);
    }
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

// ----------------------------------------------------------------------
// BuildPlainTextDescriptor（迁自 KRTextEditorCommon.h 原行 582~616）
// ----------------------------------------------------------------------
ArkUI_StyledString_Descriptor *BuildPlainTextDescriptor(
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

// ----------------------------------------------------------------------
// DestroyTextSpanResources（迁自 KRTextEditorCommon.h 原行 618~625）
// ----------------------------------------------------------------------
void DestroyTextSpanResources(OH_ArkUI_TextStyle *text_style, OH_ArkUI_SpanStyle *span_style,
                                     OH_ArkUI_ParagraphStyle *para_style,
                                     OH_ArkUI_LineHeightStyle *line_height_style) {
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
        OH_ArkUI_LineHeightStyle_Destroy(line_height_style);
    }
}

// ----------------------------------------------------------------------
// SetPlainStyledText
// ----------------------------------------------------------------------
void SetPlainStyledText(KRTextEditorState &state, const std::string &text) {
    if (!state.controller_) {
        return;
    }
    OH_ArkUI_TextStyle *text_style = nullptr;
    OH_ArkUI_SpanStyle *span_style = nullptr;
    OH_ArkUI_ParagraphStyle *para_style = nullptr;
    OH_ArkUI_LineHeightStyle *line_height_style = nullptr;
    ArkUI_StyledString_Descriptor *desc = BuildPlainTextDescriptor(
        state, text, &text_style, &span_style, &para_style, &line_height_style);
    if (desc) {
        OH_ArkUI_TextEditorStyledStringController_SetStyledString(state.controller_, desc);
        OH_ArkUI_StyledString_Descriptor_Destroy(desc);
    }
    DestroyTextSpanResources(text_style, span_style, para_style, line_height_style);
}

// ----------------------------------------------------------------------
// SetStyledText（迁自 KRTextEditorCommon.h 原行 643~831）
// ----------------------------------------------------------------------
void SetStyledText(KRTextEditorState &state, const std::string &text) {
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
            // 跟踪 build segment 期间的临时资源，统一在尾部一次性 Destroy。
            // descriptor 在 AppendStyledString / SetStyledString 后即释放，保持与纯文本路径一致。
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
                        OH_ArkUI_StyledString_Descriptor_Destroy(seg_desc);
                    }
                }
                if (ts) {
                    temp_text_styles.push_back(ts);
                }
                if (ss) {
                    temp_span_styles.push_back(ss);
                }
                if (ps) {
                    temp_para_styles.push_back(ps);
                }
                if (ls) {
                    temp_lh_styles.push_back(ls);
                }
            };

            auto append_image_span = [&](const std::string &uri, float w, float h) {
                ArkUI_StyledString_Descriptor *img_desc =
                    BuildImageSpanDescriptor(uri, w, h, state.font_size_);
                if (!img_desc) {
                    return;
                }
                if (!root_desc) {
                    // 文本以 image 开头：root 必须是个 string descriptor 才能被 Append
                    // 成功——为安全起见先建一个空文本 descriptor 作为 root。
                    OH_ArkUI_TextStyle *ts = nullptr;
                    OH_ArkUI_SpanStyle *ss = nullptr;
                    OH_ArkUI_ParagraphStyle *ps = nullptr;
                    OH_ArkUI_LineHeightStyle *ls = nullptr;
                    root_desc = BuildPlainTextDescriptor(state, "", &ts, &ss, &ps, &ls);
                    if (ts) {
                        temp_text_styles.push_back(ts);
                    }
                    if (ss) {
                        temp_span_styles.push_back(ss);
                    }
                    if (ps) {
                        temp_para_styles.push_back(ps);
                    }
                    if (ls) {
                        temp_lh_styles.push_back(ls);
                    }
                }
                if (root_desc) {
                    OH_ArkUI_StyledString_Descriptor_AppendStyledString(root_desc, img_desc);
                }
                OH_ArkUI_StyledString_Descriptor_Destroy(img_desc);
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
                OH_ArkUI_StyledString_Descriptor_Destroy(root_desc);
            }
            // Style 临时资源安全 Destroy（与原实现同等条件）。
            for (auto *ts : temp_text_styles) {
                OH_ArkUI_TextStyle_Destroy(ts);
            }
            for (auto *ss : temp_span_styles) {
                OH_ArkUI_SpanStyle_Destroy(ss);
            }
            for (auto *ps : temp_para_styles) {
                OH_ArkUI_ParagraphStyle_Destroy(ps);
            }
            for (auto *ls : temp_lh_styles) {
                OH_ArkUI_LineHeightStyle_Destroy(ls);
            }
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
        // SDK 缺陷规避（详见 .ai/references/ohos-styledstring-descriptor-quirks.md）：
        // 此处 desc 是通过 `_CreateWithString` 走 SDK 正常初始化路径构造的，
        // Destroy 是安全的；不可改用 `_Create()`，那条路径会返回内部指针未初始化
        // 的 struct，Destroy 时会 free 野指针并崩溃。
        OH_ArkUI_StyledString_Descriptor_Destroy(desc);
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

// ----------------------------------------------------------------------
// SnapToUtf8CharStart（迁自 KRTextEditorCommon.h 原行 873~883）
// ----------------------------------------------------------------------
size_t SnapToUtf8CharStart(const std::string &s, size_t pos) {
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

static size_t Utf16CursorToUtf8ByteOffset(const std::string &s, uint32_t utf16_cursor) {
    size_t byte_idx = 0;
    uint32_t utf16_idx = 0;
    while (byte_idx < s.size() && utf16_idx < utf16_cursor) {
        unsigned char c = static_cast<unsigned char>(s[byte_idx]);
        size_t char_byte_len = (c < 0x80) ? 1
                             : (c < 0xC0) ? 1
                             : (c < 0xE0) ? 2
                             : (c < 0xF0) ? 3
                             :              4;
        uint32_t char_utf16_len = (char_byte_len >= 4) ? 2 : 1;
        if (utf16_idx + char_utf16_len > utf16_cursor) {
            break;
        }
        byte_idx += char_byte_len;
        utf16_idx += char_utf16_len;
    }
    return SnapToUtf8CharStart(s, byte_idx);
}

// ----------------------------------------------------------------------
// ReconstructRawFromFlat（迁自 KRTextEditorCommon.h 原行 906~1023）
// ----------------------------------------------------------------------
std::string ReconstructRawFromFlat(
    const std::vector<KRTextEditorState::KRImageSpanRecord> &prev_image_spans,
    const std::string &prev_flat,
    const std::string &new_flat,
    uint32_t new_flat_selection_start,
    uint32_t new_flat_selection_end,
    std::vector<KRTextEditorState::KRImageSpanRecord> &out_new_image_spans) {
    out_new_image_spans.clear();

    // 退化路径 1：prev_flat == new_flat（无变化）→ 映射全部原样幸存。
    if (prev_flat == new_flat) {
        out_new_image_spans = prev_image_spans;
        // 直接基于 prev_image_spans 把 new_flat 还原为 raw（无平移，仅替换占位空格）。
        if (prev_image_spans.empty()) {
            return new_flat;
        }
        // 退化路径单趟构建：O(N + M)，避免 prev_image_spans 多次 raw.replace 累计 O(N · M)。
        return RebuildRawByMergingImageSpans(new_flat, prev_image_spans);
    }
    // 退化路径 2：prev_image_spans 为空（prev raw 中无 image）→ raw == flat。
    if (prev_image_spans.empty()) {
        return new_flat;
    }

    // Step A: 求差异区段。
    // 默认用 lcp/lcs；但当多个连续 image span 都被 ArkUI 扁平化成同一个空格时，
    // "   a" -> "  a" 这类删除仅靠文本 diff 无法区分删的是第几个空格。
    // 删除场景下 ArkUI 的变更后折叠光标是可靠锚点：back delete 后光标停在被删区间起点，
    // 因此优先用 new selection 修正 diff 区间，避免误删最右侧 image。
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
    if (prev_e < prev_len) {
        prev_e = SnapToUtf8CharStart(prev_flat, prev_e);
    }
    if (new_e < new_len) {
        new_e = SnapToUtf8CharStart(new_flat, new_e);
    }
    // lcp 向 UTF-8 字符起点回退后，右侧 lcs 推导出的 end 也可能回退到 diff_s 左侧。
    // 此时不能把 end 直接抬到 diff_s 形成空差异区间，否则紧邻多字节字符的 image span
    // 可能被误判为幸存；统一向左扩展 diff_s，保留实际被 UTF-8 边界覆盖的差异范围。
    size_t snapped_diff_s = diff_s;
    if (prev_e < snapped_diff_s) {
        snapped_diff_s = prev_e;
    }
    if (new_e < snapped_diff_s) {
        snapped_diff_s = new_e;
    }
    diff_s = snapped_diff_s;

    bool has_valid_selection = new_flat_selection_start != static_cast<uint32_t>(-1) &&
                               new_flat_selection_end != static_cast<uint32_t>(-1);
    bool is_pure_delete_by_lcs = prev_len > new_len && new_e == diff_s;
    if (has_valid_selection && is_pure_delete_by_lcs &&
        new_flat_selection_start == new_flat_selection_end) {
        size_t delete_len = prev_len - new_len;
        size_t caret_byte = Utf16CursorToUtf8ByteOffset(new_flat, new_flat_selection_start);
        if (caret_byte <= new_len && caret_byte + delete_len <= prev_len) {
            diff_s = caret_byte;
            new_e = caret_byte;
            prev_e = caret_byte + delete_len;
        }
    }

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
            if (new_offset < 0) {
                new_offset = 0;
            }
            moved.flat_offset = static_cast<size_t>(new_offset);
            out_new_image_spans.push_back(std::move(moved));
        } else {
            // 落在差异区段内：被吞掉，丢弃。
        }
    }

    // Step C: 以 new_flat 为底，把 image 占位空格替换回 raw_literal。
    // 单趟构建，O(N + M)，替代原本"从右到左 N 次 raw.replace"的 O(N · M)。
    // 若 new_flat 中对应位置不再是占位空格（极端：用户编辑使其变成别的字符），
    // 仍按 image_spans_ 权威映射强制替换 —— 该值是 raw 构造逻辑的真理之源；
    // 实际触发概率极低（差分平移已把所有"被编辑"的 image 剔除）。
    std::string raw = RebuildRawByMergingImageSpans(new_flat, out_new_image_spans);

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
            if (span_idx >= span_count) {
                break;
            }
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

// ----------------------------------------------------------------------
// DeriveFlatFromRaw（迁自 KRTextEditorCommon.h 原行 1034~1066）
// ----------------------------------------------------------------------
std::string DeriveFlatFromRaw(
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

// ----------------------------------------------------------------------
// RebuildRawAfterUserEdit（迁自 KRTextEditorCommon.h 原行 1073~1081）
// ----------------------------------------------------------------------
std::string RebuildRawAfterUserEdit(KRTextEditorState &state,
                                    const std::string &new_flat,
                                    uint32_t new_flat_selection_start,
                                    uint32_t new_flat_selection_end) {
    std::string prev_flat = DeriveFlatFromRaw(state.cached_text_, state.image_spans_);
    std::vector<KRTextEditorState::KRImageSpanRecord> new_spans;
    std::string new_raw = ReconstructRawFromFlat(state.image_spans_, prev_flat,
                                                 new_flat, new_flat_selection_start,
                                                 new_flat_selection_end, new_spans);
    state.image_spans_ = std::move(new_spans);
    return new_raw;
}

// ----------------------------------------------------------------------
// FlatUtf16ToRawUtf16（迁自 KRTextEditorCommon.h 原行 1107~1125）
// ----------------------------------------------------------------------
uint32_t FlatUtf16ToRawUtf16(
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
        if (rec.raw_literal.empty()) {
            continue;  // 兼容旧 AppendImageSpan 的退化路径
        }
        int lit_u16 = GetUTF16Length(rec.raw_literal);
        if (lit_u16 > 1) {
            raw_cursor += static_cast<uint32_t>(lit_u16 - 1);
        }
    }
    return raw_cursor;
}

// ----------------------------------------------------------------------
// RawUtf16ToFlatUtf16（迁自 KRTextEditorCommon.h 原行 1127~1153）
// ----------------------------------------------------------------------
uint32_t RawUtf16ToFlatUtf16(
    const std::vector<KRTextEditorState::KRImageSpanRecord> &image_spans,
    uint32_t raw_cursor) {
    uint32_t flat_cursor = raw_cursor;
    uint32_t raw_consumed_extra = 0;  // 已遍历 image 在 raw 中比 flat 多出的 UTF-16 长度合计
    for (const auto &rec : image_spans) {
        if (rec.raw_literal.empty()) {
            continue;
        }
        int lit_u16 = GetUTF16Length(rec.raw_literal);
        if (lit_u16 <= 0) {
            continue;
        }
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

// ----------------------------------------------------------------------
// ReadDescriptorString（迁自 KRTextEditorCommon.h 原行 1176~1190）
// ----------------------------------------------------------------------
std::string ReadDescriptorString(const ArkUI_StyledString_Descriptor *desc) {
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

// ----------------------------------------------------------------------
// GetStyledText（迁自 KRTextEditorCommon.h 原行 1204~1228）
// ----------------------------------------------------------------------
std::string GetStyledText(const KRTextEditorState &state) {
    if (!state.controller_) {
        return "";
    }
    // RAII 守卫：替代原本"SpanStyle_Create + Descriptor_CreateWithString("",..)
    // + 三处手动 Destroy"模式。析构由作用域自动接管，不可能漏 Destroy。
    EmptyStyledStringDescGuard g;
    if (!g) {
        return "";
    }
    std::string ret;
    if (OH_ArkUI_TextEditorStyledStringController_GetStyledString(state.controller_, g.desc()) ==
        ARKUI_ERROR_CODE_NO_ERROR) {
        ret = ReadDescriptorString(g.desc());
    }
    return ret;
}

// ----------------------------------------------------------------------
// ApplyPlaceholder（迁自 KRTextEditorCommon.h 原行 1232~1250）
// ----------------------------------------------------------------------
void ApplyPlaceholder(ArkUI_NodeHandle node, const KRTextEditorState &state) {
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

#endif  // KUIKLY_TEXT_EDITOR_AVAILABLE

// =============================================================================
// 以下函数不在 KUIKLY_TEXT_EDITOR_AVAILABLE 包护范围内（文本长度计算 / textInputState
// 协议工具），即便低 SDK header 也参与编译；个别函数体内部按需用 #if 局部包护。
// =============================================================================

// ----------------------------------------------------------------------
// CalculateTextLength（迁自 KRTextEditorCommon.h 原行 1387~1435）
// ----------------------------------------------------------------------
int CalculateTextLength(int length_limit_type, const std::string &text, size_t rmStart,
                               size_t rmEnd) {
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

// ----------------------------------------------------------------------
// CalculateRenderedTextLength
// ----------------------------------------------------------------------
int CalculateRenderedTextLength(const KRTextEditorState &state, const std::string &text) {
    if (state.length_limit_type_ == 0 || state.image_spans_.empty()) {
        return CalculateTextLength(state.length_limit_type_, text);
    }
    if (state.length_limit_type_ == 1) {
        int length = CalculateTextLength(state.length_limit_type_, text);
        for (const auto &span : state.image_spans_) {
            if (span.raw_literal.empty()) {
                continue;
            }
            length -= CalculateTextLength(state.length_limit_type_, span.raw_literal);
            length += 1;
        }
        return length;
    }
    if (state.length_limit_type_ == 2) {
        int length = CalculateTextLength(state.length_limit_type_, text);
        for (const auto &span : state.image_spans_) {
            if (span.raw_literal.empty()) {
                continue;
            }
            length -= CalculateTextLength(state.length_limit_type_, span.raw_literal);
            length += 2;
        }
        return length;
    }
    return CalculateTextLength(state.length_limit_type_, text);
}

// ----------------------------------------------------------------------
// CalculateCandidateRenderedTextLength
// ----------------------------------------------------------------------
int CalculateCandidateRenderedTextLength(int length_limit_type, const std::string &text) {
    if (length_limit_type == 0) {
        return CalculateTextLength(length_limit_type, text);
    }
    std::vector<kuikly::text::KRTextPostProcessSpan> spans;
    if (!kuikly::text::RunTextPostProcessor(kTextPostProcessorNameInput, text, spans)) {
        return CalculateTextLength(length_limit_type, text);
    }
    int length = 0;
    for (const auto &span : spans) {
        if (span.type == kuikly::text::KRTextPostProcessSpan::Type::kText) {
            length += CalculateTextLength(length_limit_type, span.text_or_src);
        } else if (length_limit_type == 1) {
            length += 1;
        } else if (length_limit_type == 2) {
            length += 2;
        }
    }
    return length;
}

// ----------------------------------------------------------------------
// CalculateTruncateIndex（迁自 KRTextEditorCommon.h 原行 1437~1480）
// ----------------------------------------------------------------------
int CalculateTruncateIndex(int length_limit_type, const std::string &text, size_t keep) {
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

// ----------------------------------------------------------------------
// FilterSource（迁自 KRTextEditorCommon.h 原行 1484~1501）
// ----------------------------------------------------------------------
bool FilterSource(char source[], const std::string &dest, size_t dStart, size_t dEnd,
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

// ----------------------------------------------------------------------
// BuildTextInputStatePayload（迁自 KRTextEditorCommon.h 原行 1566~1599）
// ----------------------------------------------------------------------
KRRenderValueMap BuildTextInputStatePayload(const KRTextEditorState &state) {
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
        int length = CalculateRenderedTextLength(state, text);
        map[kKeyLength] = NewKRRenderValue(length);
    }
    return map;
}

// ----------------------------------------------------------------------
// ParseTextInputStateJson（迁自 KRTextEditorCommon.h 原行 1612~1644）
// ----------------------------------------------------------------------
ParsedTextInputState ParseTextInputStateJson(const std::string &json) {
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

#endif 
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

#include "libohos_render/expand/components/richtext/KRRichTextView.h"

#include <codecvt>
#include <locale>
#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_point.h>
#include <native_drawing/drawing_shader_effect.h>
#include "libohos_render/expand/components/base/KRCustomUserCallback.h"
#include "libohos_render/expand/components/richtext/KRRichTextShadow.h"
#include "libohos_render/foundation/KRConfig.h"
#include "libohos_render/foundation/KRPoint.h"
#include "libohos_render/export/IKRRenderViewExport.h"

#ifdef __cplusplus
extern "C" {
#endif
// Remove this declaration if compatable api is raised to 14 and above
extern size_t OH_Drawing_GetDrawingArraySize(OH_Drawing_Array* drawingArray) __attribute__((weak));
extern OH_Drawing_TextLine* OH_Drawing_TextLineCreateTruncatedLine(OH_Drawing_TextLine* line, double width, int mode,
    const char* ellipsis) __attribute__((weak));
extern void OH_Drawing_TextLinePaint(OH_Drawing_TextLine* line, OH_Drawing_Canvas* canvas, double x, double y) __attribute__((weak));
extern OH_Drawing_TextLine* OH_Drawing_GetTextLineByIndex(OH_Drawing_Array* lines, size_t index) __attribute__((weak));
extern void OH_Drawing_DestroyTextLine(OH_Drawing_TextLine* line) __attribute__((weak));
// Weak extern for selection - API compatibility (OH_Drawing_TypographyGetLineCount/GetLineInfo provided by SDK)
extern OH_Drawing_Range* OH_Drawing_TypographyGetLineTextRange(OH_Drawing_Typography* typography, int index,
    bool includeNewLine) __attribute__((weak));
extern size_t OH_Drawing_GetStartFromRange(OH_Drawing_Range* range) __attribute__((weak));
extern size_t OH_Drawing_GetEndFromRange(OH_Drawing_Range* range) __attribute__((weak));
#ifdef __cplusplus
}
#endif

// UTF-8 to UTF-16
static std::u16string utf8_to_utf16(const std::string& utf8_string) {
    std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
    return converter.from_bytes(utf8_string);
}
// UTF-16 to UTF-8
static std::string utf16_to_utf8(const std::u16string& utf16_string) {
    std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
    return converter.to_bytes(utf16_string);
}

static const char * kPropNameLineBreakMargin = "lineBreakMargin";
static const char * kPropNameClick = "click";

ArkUI_NodeHandle KRRichTextView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_TEXT);
}

void KRRichTextView::OnDestroy() {
    kuikly::util::GetNodeApi()->resetAttribute(GetNode(), NODE_TEXT_CONTENT_WITH_STYLED_STRING);

    IKRRenderViewExport::OnDestroy();
    auto self = shared_from_this();
    KREventDispatchCenter::GetInstance().UnregisterCustomEvent(self);
    shadow_ = nullptr;
}

bool KRRichTextView::ReuseEnable() {
    return true;
}

void KRRichTextView::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
}

void KRRichTextView::OnCustomEvent(ArkUI_NodeCustomEvent *event, const ArkUI_NodeCustomEventType &event_type) {
    if (event_type == ARKUI_NODE_CUSTOM_EVENT_ON_FOREGROUND_DRAW) {
        OnForegroundDraw(event);
    }
}

void KRRichTextView::DidInit() {
    IKRRenderViewExport::DidInit();
}

void KRRichTextView::SetShadow(const std::shared_ptr<IKRRenderShadowExport> &shadow) {
    shadow_ = shadow;

    auto textShadow = std::dynamic_pointer_cast<KRRichTextShadow>(shadow);
    if(textShadow && textShadow->StyledStringEnabled()){
        ArkUI_AttributeItem item;
        if(std::shared_ptr<KRParagraph> paragraph = std::dynamic_pointer_cast<KRRichTextShadow>(shadow)->GetParagraph()){
            item.object = paragraph->GetStyledString();
            kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_CONTENT_WITH_STYLED_STRING, &item);
            // Note:
            // The ownership of the styled string is not going to be transferred,
            // by setting the style item to the note by calling 
            // setAttribute with NODE_TEXT_CONTENT_WITH_STYLED_STRING.
            // Besides, it is not reference counted,
            // we need to make sure it is alive after setting it to the node.
            paragraph_ = paragraph;
        }
    }else {
        KREventDispatchCenter::GetInstance().RegisterCustomEvent(shared_from_this(), ARKUI_NODE_CUSTOM_EVENT_ON_FOREGROUND_DRAW);
        kuikly::util::GetNodeApi()->markDirty(GetNode(), NODE_NEED_RENDER);
    }
}

void KRRichTextView::DidMoveToParentView() {
    IKRRenderViewExport::DidMoveToParentView();
    auto self = shared_from_this();
    KREventDispatchCenter::GetInstance().RegisterCustomEvent(self, ARKUI_NODE_CUSTOM_EVENT_ON_FOREGROUND_DRAW);
}

void KRRichTextView::DidRemoveFromParentView() {
    kuikly::util::GetNodeApi()->resetAttribute(GetNode(), NODE_TEXT_CONTENT_WITH_STYLED_STRING);
    IKRRenderViewExport::DidRemoveFromParentView();
    shadow_ = nullptr;
    paragraph_ = nullptr;
    last_draw_frame_width_ = -1.0;
}

void KRRichTextView::OnForegroundDraw(ArkUI_NodeCustomEvent *event) {
    if (shadow_ == nullptr && GetFrame().width == 0) {
        KR_LOG_ERROR << "OnForegroundDraw, shadow or frame not ready, shadow:" << shadow_.get()
                     << ", frame width:" << GetFrame().width;
        return;
    }
    if (auto rootView = GetRootView().lock()) {
        if (rootView->IsPerformMainTasking()) {
            std::weak_ptr<IKRRenderViewExport> weakSelf = shared_from_this();
            KRMainThread::RunOnMainThreadForNextLoop([weakSelf] {
                if(auto strongSelf = weakSelf.lock()){
                    kuikly::util::GetNodeApi()->markDirty(strongSelf->GetNode(), NODE_NEED_RENDER);
                }
            });
#ifndef NDEBUG
            KR_LOG_ERROR << "OnForegroundDraw, IsPerformMainTasking Skip:" << shadow_.get();
#endif
            return;
        }
    }
    auto richTextShadow = reinterpret_cast<KRRichTextShadow *>(shadow_.get());
    if (richTextShadow == nullptr) {
        KR_LOG_ERROR << "OnForegroundDraw, richTextShadow null";
        return;
    }
    OH_Drawing_Typography *textTypo = richTextShadow->MainThreadTypography();
    if (textTypo == nullptr) {
        KR_LOG_ERROR << "OnForegroundDraw, textTypo null, shadow:" << richTextShadow;
        return;
    }
    double drawOffsetY = richTextShadow->DrawOffsetY();
    OH_Drawing_TextAlign textAlign = richTextShadow->TextAlign();
    auto textTypoSize = richTextShadow->MainMeasureSize();
    // 在容器前景上绘制额外图形，实现图形显示在子组件之上。
    auto *drawContext = OH_ArkUI_NodeCustomEvent_GetDrawContextInDraw(event);
    auto *drawingHandle = reinterpret_cast<OH_Drawing_Canvas *>(OH_ArkUI_DrawContext_GetCanvas(drawContext));
    auto frameWidth = GetFrame().width;
    bool needReLayout = false;
    if (last_draw_frame_width_ > 0 && fabs(last_draw_frame_width_ - frameWidth) > 0.01) {
        needReLayout = true;
    }
    if (fabs(textTypoSize.width - frameWidth) > 1 || textAlign != TEXT_ALIGN_LEFT) {
        needReLayout = true;
    }
    if (needReLayout) {
        auto dpi = KRConfig::GetDpi();
        OH_Drawing_TypographyLayout(textTypo, frameWidth * dpi);
        last_draw_frame_width_ = frameWidth;
        if (textAlign != TEXT_ALIGN_LEFT) {
            richTextShadow->ResetTextAlign();
        }
    }

    if (!selection_rects_.selection_rects.empty()) {
        double density = KRConfig::GetDpi();
        OH_Drawing_Brush *backgroundBrush = OH_Drawing_BrushCreate();
        OH_Drawing_BrushSetColor(backgroundBrush, 0x33007DFF);
        OH_Drawing_CanvasAttachBrush(drawingHandle, backgroundBrush);
        OH_Drawing_Path *backgroundPath = OH_Drawing_PathCreate();
        for (const KRRect &rect : selection_rects_.selection_rects) {
            OH_Drawing_PathReset(backgroundPath);
            OH_Drawing_PathMoveTo(backgroundPath, rect.x * density, rect.y * density);
            OH_Drawing_PathLineTo(backgroundPath, (rect.x + rect.width) * density, rect.y * density);
            OH_Drawing_PathLineTo(backgroundPath, (rect.x + rect.width) * density,
                                  (rect.y + rect.height) * density);
            OH_Drawing_PathLineTo(backgroundPath, rect.x * density, (rect.y + rect.height) * density);
            OH_Drawing_PathClose(backgroundPath);
            OH_Drawing_CanvasDrawPath(drawingHandle, backgroundPath);
        }
        OH_Drawing_CanvasDetachBrush(drawingHandle);
        OH_Drawing_BrushDestroy(backgroundBrush);
        OH_Drawing_PathDestroy(backgroundPath);
    }

    if(OH_Drawing_TextLinePaint && line_break_margin_ > 0 && richTextShadow->DidExceedMaxLines()){
        auto text_lines = richTextShadow->GetTextLines();
        size_t line_count = OH_Drawing_GetDrawingArraySize(text_lines);
        for(int i = 0; i < line_count; ++i){
            OH_Drawing_TextLine* line = OH_Drawing_GetTextLineByIndex(text_lines, i);

            if(i + 1 == line_count && richTextShadow->DidExceedMaxLines()){
                OH_Drawing_TextLine *truncated_text_line = OH_Drawing_TextLineCreateTruncatedLine(line, (frameWidth - line_break_margin_) * KRConfig::GetDpi(), ELLIPSIS_MODAL_TAIL, "...");
                OH_Drawing_TextLinePaint( truncated_text_line, drawingHandle, 0, -drawOffsetY);
                OH_Drawing_DestroyTextLine(truncated_text_line);
            }else{
                OH_Drawing_TextLinePaint( line, drawingHandle, 0, -drawOffsetY);
            }
        }
        if(line_count > 0){
            return;
        }
    }
    // fallback
    OH_Drawing_TypographyPaint(textTypo, drawingHandle, 0, -drawOffsetY);
}

void KRRichTextView::ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                               const KRRenderCallback event_callback) {
    if (kuikly::util::isEqual(prop_key, kPropNameClick)) {
        std::weak_ptr<KRRichTextView> weakSelf = std::dynamic_pointer_cast<KRRichTextView>(shared_from_this());
        KRRenderCallback middleManCallback = [weakSelf, event_callback](KRAnyValue res) {
            auto strongSelf = weakSelf.lock();
            if(strongSelf == nullptr){
                return;
            }
            if (res->isMap()) {
                const auto oldParam = res->toMap();
                const auto x = oldParam.find("x");
                const auto y = oldParam.find("y");

                KRRenderValueMap params;
                if (x != oldParam.end()) {
                    params["x"] = x->second;
                }

                if (y != oldParam.end()) {
                    params["y"] = y->second;
                }

                const auto pageX = oldParam.find("pageX");
                const auto pageY = oldParam.find("pageY");
                if (pageX != oldParam.end()) {
                    params["pageX"] = pageX->second;
                }
                if (pageY != oldParam.end()) {
                    params["pageY"] = pageY->second;
                }

                if (auto richTextShadow = std::dynamic_pointer_cast<KRRichTextShadow>(strongSelf->shadow_)) {
                    int index = richTextShadow->SpanIndexAt(x->second->toFloat(), y->second->toFloat());
                    if (index < 0) {
                        index = 0;
                    }
                    params["index"] = NewKRRenderValue(index);
                }
                event_callback(NewKRRenderValue(params));
            } else {
                event_callback(res);
            }
        };
        IKRRenderViewExport::ToSetProp(prop_key, prop_value, middleManCallback);
    } else if(prop_key == kPropNameLineBreakMargin) {
        line_break_margin_ = prop_value->toFloat();
    }else {
        IKRRenderViewExport::ToSetProp(prop_key, prop_value, event_callback);
    }
}

void KRRichTextView::ClearSelection() {
    selection_rects_.start = 0;
    selection_rects_.end = 0;
    selection_rects_.selection_rects.clear();
    SetSelected(false);
    SetNeedsDisplay();
}

void KRRichTextView::SetSelectionAll() {
    KRParagraphInfo info = GetParagraphInfo();
    selection_rects_ = info.GetSelectionRectsAll();
    SetSelected(true);
    SetNeedsDisplay();
}

const KRParagraphSelectionInfo &KRRichTextView::GetSelectionInfo() {
    return selection_rects_;
}

const KRParagraphSelectionInfo &KRRichTextView::SetSelection(KRPoint start, KRPoint end, int type) {
    KRParagraphInfo info = GetParagraphInfo();
    selection_rects_ = info.GetSelectionRects2(start, end, type);
    SetSelected(true);
    SetNeedsDisplay();
    return selection_rects_;
}

static int GetOffsetInLine(KRLineInfo &info, KRPoint point_in, SelectionStrategy &which_part, bool &first_in_line,
                           bool &last_in_line) {
    float density = KRConfig::GetDpi();
    KRPoint point(point_in.x * density, point_in.y * density);

    float min_distance = 100000;
    int index = -1;

    info.ForEach([&index, &min_distance, point](int i, KRRect rect) {
        float dist = std::fabs(point.x - rect.x - rect.width / 2);
        if (dist < min_distance) {
            min_distance = dist;
            index = i;
        }
    });
    first_in_line = index == info.FrontIndex();
    last_in_line = index == info.BackIndex();
    if (index == -1) {
        index = info.FrontIndex();
    }

    auto rect = info.Get(index);
    which_part = point.x - rect.x - rect.width / 2 > 0 ? SelectionStrategy::Trailing : SelectionStrategy::Leading;
    return index;
}

std::pair<int, int> KRParagraphInfo::GetSentenceBoundary(int offset) {
    (void)utf8_to_utf16(text_content_);
    return std::make_pair(offset, offset);
}

std::pair<int, int> KRParagraphInfo::GetParagraphBoundary(int offset) {
    (void)utf8_to_utf16(text_content_);
    return std::make_pair(offset, offset);
}

KRParagraphSelectionInfo KRParagraphInfo::GetSelectionRectsAll() {
    float density = KRConfig::GetDpi();
    std::vector<KRRect> selected_rect_list;

    for (size_t line_index = 0; line_index < line_info_list_.size(); ++line_index) {
        const KRLineInfo &line_info = line_info_list_[line_index];

        const KRRect start_rect = line_info.Front();
        const KRRect end_rect = line_info.Back();
        KRRect result(start_rect.x / density, line_info.line_metrics_.y / density,
                      (end_rect.x - start_rect.x + end_rect.width) / density,
                      line_info.line_metrics_.height / density);

        selected_rect_list.push_back(result);
    }
    KRParagraphSelectionInfo info;
    info.start = 0;
    info.text_content = text_content_;
    info.end = line_info_list_.empty() ? 0 : line_info_list_.back().line_metrics_.endIndex;
    info.selection_rects = selected_rect_list;
    return info;
}

KRParagraphSelectionInfo KRParagraphInfo::GetSelectionRects2(KRPoint p0, KRPoint p1, int type) {
    (void)type;
    float density = KRConfig::GetDpi();
    auto points = std::array{p0, p1};
    int line_numbers[2];
    int point_index = 0;
    for (auto p : points) {
        KRPoint point(p.x * density, p.y * density);
        float min_distance = 100000;
        int which_line = -1;
        for (size_t line_index = 0; line_index < line_info_list_.size(); ++line_index) {
            const KRLineInfo &line_info = line_info_list_[line_index];
            float center = line_info.line_metrics_.y + line_info.line_metrics_.height / 2;
            float distance = std::fabs(center - point.y);
            if (distance < min_distance) {
                min_distance = distance;
                which_line = static_cast<int>(line_index);
            }
        }
        line_numbers[point_index] = which_line >= 0 ? which_line : 0;
        ++point_index;
    }

    if (line_numbers[0] > line_numbers[1]) {
        std::swap(line_numbers[0], line_numbers[1]);
        std::swap(points[0], points[1]);
    }

    int first_line_text_offset = -1;
    bool first_line_text_is_first_in_line = true;
    bool first_line_text_is_last_in_line = true;
    SelectionStrategy which_part_of_first_line_text = SelectionStrategy::Leading;
    int last_line_text_offset = -1;
    bool last_line_text_is_first_in_line = true;
    bool last_line_text_is_last_in_line = true;
    SelectionStrategy which_part_of_last_line_text = SelectionStrategy::Trailing;
    std::vector<KRRect> rects;
    {
        auto line_info = line_info_list_[line_numbers[0]];
        KRPoint first_point(points[0].x * density, points[0].y * density);
        if (first_point.y < line_info_list_[line_numbers[0]].line_metrics_.y) {
            first_line_text_offset = line_info_list_[line_numbers[0]].line_metrics_.startIndex;
        } else {
            first_line_text_offset = GetOffsetInLine(line_info, points[0], which_part_of_first_line_text,
                                                     first_line_text_is_first_in_line, first_line_text_is_last_in_line);
        }
    }

    {
        auto line_info = line_info_list_[line_numbers[1]];
        KRPoint last_point(points[1].x * density, points[1].y * density);
        if (last_point.y > line_info_list_[line_numbers[1]].line_metrics_.y +
                              line_info_list_[line_numbers[1]].line_metrics_.height) {
            last_line_text_offset = line_info_list_[line_numbers[1]].BackOffset();
        } else {
            last_line_text_offset = GetOffsetInLine(line_info, points[1], which_part_of_last_line_text,
                                                    last_line_text_is_first_in_line, last_line_text_is_last_in_line);
        }
    }

    if (first_line_text_offset > last_line_text_offset) {
        std::swap(first_line_text_offset, last_line_text_offset);
        std::swap(first_line_text_is_first_in_line, last_line_text_is_first_in_line);
        std::swap(first_line_text_is_last_in_line, last_line_text_is_last_in_line);
    }

    float first_char_width = -1;
    float last_char_width = -1;
    if (line_numbers[0] == line_numbers[1]) {
        if (last_line_text_is_first_in_line && first_line_text_is_first_in_line &&
            which_part_of_last_line_text == which_part_of_first_line_text) {
            auto line_info = line_info_list_[line_numbers[0]];
            const KRRect &start_rect = line_info.Get(first_line_text_offset);
            KRRect result(start_rect.x / density, line_info.line_metrics_.y / density, 0,
                          line_info.line_metrics_.height / density);
            rects.push_back(result);
            first_char_width = start_rect.width;
        } else {
            auto line_info = line_info_list_[line_numbers[0]];
            const KRRect start_rect = line_info.Get(first_line_text_offset);
            const KRRect end_rect = line_info.Get(last_line_text_offset);
            KRRect result(start_rect.x / density, line_info.line_metrics_.y / density,
                          (end_rect.x - start_rect.x + end_rect.width) / density,
                          line_info.line_metrics_.height / density);
            rects.push_back(result);
            first_char_width = start_rect.width;
            last_char_width = end_rect.width;
        }
    } else {
        if (!(first_line_text_is_last_in_line && which_part_of_first_line_text == SelectionStrategy::Trailing)) {
            auto line_info = line_info_list_[line_numbers[0]];
            const KRRect &start_rect = line_info.Get(first_line_text_offset);
            const KRRect &end_rect = line_info.Back();
            KRRect result(start_rect.x / density, line_info.line_metrics_.y / density,
                          (end_rect.x - start_rect.x + end_rect.width) / density,
                          line_info.line_metrics_.height / density);
            rects.push_back(result);
            first_char_width = start_rect.width;
        } else {
            auto line_info = line_info_list_[line_numbers[0]];
            const KRRect &end_rect = line_info.Back();
            KRRect result((end_rect.x + end_rect.width) / density, line_info.line_metrics_.y / density, 0,
                          line_info.line_metrics_.height / density);
            rects.push_back(result);
            first_char_width = end_rect.width;
        }
        for (int i = line_numbers[0] + 1; i < line_numbers[1]; ++i) {
            auto line_info = line_info_list_[i];
            KRRect result(line_info.line_metrics_.x / density, line_info.line_metrics_.y / density,
                          line_info.line_metrics_.width / density, line_info.line_metrics_.height / density);
            rects.push_back(result);
            if (first_char_width < 0) {
                first_char_width = line_info.Front().width;
            } else {
                last_char_width = line_info.Back().width;
            }
        }
        if (!(last_line_text_is_first_in_line && which_part_of_last_line_text == SelectionStrategy::Leading)) {
            auto line_info = line_info_list_[line_numbers[1]];
            const KRRect &end_rect = line_info.Get(last_line_text_offset);
            const KRRect &start_rect = line_info.Front();
            KRRect result(start_rect.x / density, line_info.line_metrics_.y / density,
                          (end_rect.x - start_rect.x + end_rect.width) / density,
                          line_info.line_metrics_.height / density);
            rects.push_back(result);
            last_char_width = end_rect.width;
        } else {
            auto line_info = line_info_list_[line_numbers[1]];
            const KRRect &end_rect = line_info.Get(last_line_text_offset);
            const KRRect &start_rect = line_info.Front();
            KRRect result(start_rect.x / density, line_info.line_metrics_.y / density, 0,
                          line_info.line_metrics_.height / density);
            rects.push_back(result);
            last_char_width = end_rect.width;
        }
    }

    KRParagraphSelectionInfo info;
    info.text_content = text_content_;
    info.start = first_line_text_offset;
    info.end = rects.empty() || rects.front().width <= 0 ? last_line_text_offset : last_line_text_offset + 1;
    info.selection_rects = rects;
    info.first_char_width = first_char_width > 0 ? first_char_width / density : 0;
    info.last_char_width = last_char_width > 0 ? last_char_width / density : 0;
    return info;
}

KRParagraphInfo KRRichTextView::GetParagraphInfo() {
    KRParagraphInfo paragraph_info;
    auto richTextShadow = reinterpret_cast<KRRichTextShadow *>(shadow_.get());
    if (richTextShadow) {
        OH_Drawing_Typography *textTypo = richTextShadow->MainThreadTypography();
        paragraph_info.typography_ = textTypo;
    }

    auto textShadow = std::dynamic_pointer_cast<KRRichTextShadow>(shadow_);
    if (!textShadow) {
        return paragraph_info;
    }
    OH_Drawing_Typography *textTypo = textShadow->MainThreadTypography();
    if (textTypo == nullptr) {
        return paragraph_info;
    }

    auto frame = GetFrame();
    paragraph_info.width_ = frame.width;
    paragraph_info.height_ = frame.height;

    std::string text_content = textShadow->GetTextContent();
    paragraph_info.text_content_ = text_content;
    size_t lineCount = OH_Drawing_TypographyGetLineCount(textTypo);
    for (size_t i = 0; i < lineCount; ++i) {
        KRLineInfo line_info;
        OH_Drawing_TypographyGetLineInfo(textTypo, i, true, true, &line_info.line_metrics_);
        OH_Drawing_Range *text_range =
            OH_Drawing_TypographyGetLineTextRange ? OH_Drawing_TypographyGetLineTextRange(textTypo, i, true) : nullptr;
        if (text_range && OH_Drawing_GetStartFromRange && OH_Drawing_GetEndFromRange) {
            size_t text_start = OH_Drawing_GetStartFromRange(text_range);
            size_t text_end = OH_Drawing_GetEndFromRange(text_range);
            (void)text_start;
            (void)text_end;
        }

        int advance_count = 1;
        for (int index = line_info.line_metrics_.startIndex; index < line_info.line_metrics_.endIndex;
             index += advance_count) {
            advance_count = 1;

            OH_Drawing_TextBox *boxes = nullptr;
            size_t count = 0;
            while (advance_count < 4) {
                boxes = OH_Drawing_TypographyGetRectsForRange(textTypo, index, index + advance_count,
                                                              RECT_HEIGHT_STYLE_TIGHT, RECT_WIDTH_STYLE_TIGHT);
                count = OH_Drawing_GetSizeOfTextBox(boxes);
                if (count > 0) {
                    break;
                }
                ++advance_count;
            }

            if (count == 1) {
                for (size_t bi = 0; bi < count; ++bi) {
                    float left = OH_Drawing_GetLeftFromTextBox(boxes, bi);
                    float right = OH_Drawing_GetRightFromTextBox(boxes, bi);
                    float top = OH_Drawing_GetTopFromTextBox(boxes, bi);
                    float bottom = OH_Drawing_GetBottomFromTextBox(boxes, bi);
                    float width = right - left;
                    float height = bottom - top;
                    line_info.Insert(index, KRRect(left, top, width, height));
                }
            }
            if (boxes) {
                OH_Drawing_TypographyDestroyTextBox(boxes);
            }
        }
        paragraph_info.line_info_list_.emplace_back(line_info);
    }

    return paragraph_info;
}

std::string KRRichTextView::GetSelectedContent(std::string &pre, std::string &post) {
    std::u16string str16 = utf8_to_utf16(selection_rects_.text_content);

    if (selection_rects_.start > 0) {
        std::u16string pre_u16 = str16.substr(0, selection_rects_.start);
        pre = utf16_to_utf8(pre_u16);
    }
    size_t sel_end = static_cast<size_t>(selection_rects_.end);
    if (sel_end > str16.size()) {
        sel_end = str16.size();
    }
    std::u16string selected_u16 = str16.substr(selection_rects_.start, sel_end - selection_rects_.start);
    std::string selected_u8 = utf16_to_utf8(selected_u16);

    if (sel_end < str16.size()) {
        std::u16string post_u16 = str16.substr(sel_end);
        post = utf16_to_utf8(post_u16);
    }

    return selected_u8;
}

bool KRRichTextView::UpdateSelection(std::shared_ptr<IKRRenderViewExport> ancestor_view, KRPoint ancestor_point1,
                                     KRPoint ancestor_point2, int type) {
    bool has_intersection = false;

    auto [frame, selection_frame] = GetSelectionFrameInAncestorCoordinate(ancestor_view, ancestor_point1,
                                                                          ancestor_point2, type);
    if (selection_frame.IsValid()) {
        has_intersection = selection_frame.IsIntersect(frame);
    } else {
        KRRect reverse_frame(std::min(ancestor_point1.x, ancestor_point2.x),
                             std::min(ancestor_point1.y, ancestor_point2.y),
                             std::abs(ancestor_point1.x - ancestor_point2.x),
                             std::abs(ancestor_point1.y - ancestor_point2.y));
        has_intersection = reverse_frame.IsIntersect(frame);
    }

    if (has_intersection) {
        KRPoint start = selection_frame.Origin();
        KRPoint end(selection_frame.x + selection_frame.width, selection_frame.y + selection_frame.height);
        KRPoint view_start = ConvertPointToChildCoordinate(start, ancestor_view->GetNode(), GetNode());
        KRPoint view_end = ConvertPointToChildCoordinate(end, ancestor_view->GetNode(), GetNode());

        SetSelection(view_start, view_end, type);
    }

    SetSelected(has_intersection);

    return has_intersection;
}


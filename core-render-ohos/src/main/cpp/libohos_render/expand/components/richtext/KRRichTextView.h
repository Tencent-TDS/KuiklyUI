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

#ifndef CORE_RENDER_OHOS_KRRICHTEXTVIEW_H
#define CORE_RENDER_OHOS_KRRICHTEXTVIEW_H

#include <arkui/native_node.h>
#include <functional>
#include <map>
#include "libohos_render/expand/components/base/KRCustomUserCallback.h"
#include "libohos_render/expand/components/richtext/KRParagraph.h"
#include "libohos_render/expand/components/richtext/KRRichTextShadow.h"
#include "libohos_render/export/IKRRenderShadowExport.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/foundation/KRPoint.h"
#include "libohos_render/foundation/KRRect.h"
#include "libohos_render/view/IKRRenderView.h"

#include <native_drawing/drawing_text_line.h>

enum KRTextSelectionType {
    CHARACTER,
    WORD,
    PARAGRAPH,
    SENTENCE,
    ALL = 9999
};

class KRLineInfo {
 public:
    OH_Drawing_LineMetrics line_metrics_;
    void Insert(int i, const KRRect &rect) {
        rects_[i] = rect;
    }
    KRRect Front() const {
        if (auto begin = rects_.begin(); begin != rects_.end()) {
            return begin->second;
        }
        return KRRect();
    }
    KRRect Back() const {
        if (auto back = rects_.rbegin(); back != rects_.rend()) {
            return back->second;
        }
        return KRRect();
    }
    int BackOffset() const {
        if (auto back = rects_.rbegin(); back != rects_.rend()) {
            return back->first;
        }
        return 0;
    }
    KRRect Get(int i) const {
        if (auto it = rects_.find(i); it != rects_.end()) {
            return it->second;
        }
        return KRRect();
    }
    int Size() const {
        return rects_.size();
    }
    void ForEach(std::function<void(int, KRRect)> visitor) {
        for (const auto &it : rects_) {
            visitor(it.first, it.second);
        }
    }
    int FrontIndex() {
        return rects_.begin() == rects_.end() ? 0 : rects_.begin()->first;
    }
    int BackIndex() {
        return rects_.begin() == rects_.end() ? 0 : rects_.rbegin()->first;
    }

 private:
    std::map<int, KRRect> rects_;
};

class KRParagraphSelectionInfo {
 public:
    std::vector<KRRect> selection_rects;
    std::string text_content;
    int start = 0;
    int end = 0;
    float first_char_width = 0;
    float last_char_width = 0;
};

enum SelectionStrategy {
    Inside,
    Leading,
    Trailing
};

class KRParagraphInfo {
 public:
    KRParagraphSelectionInfo GetSelectionRects2(KRPoint start, KRPoint end, int type);
    KRParagraphSelectionInfo GetSelectionRectsAll();

    std::vector<KRLineInfo> line_info_list_;
    std::string text_content_;
    OH_Drawing_Typography *typography_ = nullptr;
    float width_ = 0;
    float height_ = 0;

 private:
    std::pair<int, int> GetSentenceBoundary(int offset);
    std::pair<int, int> GetParagraphBoundary(int offset);
};

class KRRichTextView : public IKRRenderViewExport {
 public:
    ArkUI_NodeHandle CreateNode() override;

    void DidInit() override;

    void OnDestroy() override;

    bool ReuseEnable() override;

    void SetShadow(const std::shared_ptr<IKRRenderShadowExport> &shadow) override;

    void DidMoveToParentView() override;
    void DidRemoveFromParentView() override;

    void OnCustomEvent(ArkUI_NodeCustomEvent *event, const ArkUI_NodeCustomEventType &event_type) override;

    void SetRenderViewFrame(const KRRect &frame) override;

    void ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                   const KRRenderCallback event_call_back = nullptr) override;

    void ClearSelection() override;
    void SetSelectionAll();
    const KRParagraphSelectionInfo &SetSelection(KRPoint start, KRPoint end, int type);
    const KRParagraphSelectionInfo &GetSelectionInfo();

    KRParagraphInfo GetParagraphInfo();
    KRRect FirstSelectionRect() {
        return selection_rects_.selection_rects.empty() ? KRRect() : *selection_rects_.selection_rects.begin();
    }
    KRRect LastSelectionRect() {
        return selection_rects_.selection_rects.empty() ? KRRect() : selection_rects_.selection_rects.back();
    }
    std::string GetTextContent() {
        return std::dynamic_pointer_cast<KRRichTextShadow>(shadow_)->GetTextContent();
    }
    std::string GetSelectedContent(std::string &pre, std::string &post);
    bool IsTextView() override {
        return true;
    }
    bool UpdateSelection(std::shared_ptr<IKRRenderViewExport> ancestor_view, KRPoint ancestor_point1,
                         KRPoint ancestor_point2, int type) override;

 private:
    std::shared_ptr<KRParagraph> paragraph_;
    std::shared_ptr<IKRRenderShadowExport> shadow_;
    float last_draw_frame_width_ = -1.0;
    float line_break_margin_ = 0;
    KRParagraphSelectionInfo selection_rects_;
    void OnForegroundDraw(ArkUI_NodeCustomEvent *event);
};

#endif  // CORE_RENDER_OHOS_KRRICHTEXTVIEW_H

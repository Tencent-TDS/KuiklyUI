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

#ifndef CORE_RENDER_OHOS_KRCANVASVIEW_H
#define CORE_RENDER_OHOS_KRCANVASVIEW_H

#include <unordered_set>

#include "libohos_render/expand/components/richtext/KRRichTextShadow.h"
#include "libohos_render/expand/components/view/KRView.h"
#include "libohos_render/export/IKRRenderViewExport.h"

class TextFeature {
 public:
    double fontSize = 15.0;  // 默认字号
    std::string fontFamily = "";
    OH_Drawing_TextAlign textAlign = TEXT_ALIGN_LEFT;
    OH_Drawing_FontStyle fontStyle = FONT_STYLE_NORMAL;
    OH_Drawing_FontWeight fontWeight = FONT_WEIGHT_400;
};

class KRCanvasView : public KRView {
 public:
    static constexpr std::string_view Name = "KRCanvasView";
    KRCanvasView();

    virtual ArkUI_NodeHandle CreateNode() override {
        ArkUI_NodeHandle handle = kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_CUSTOM);
        return handle;
    }

    void CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &cb) override;
    void OnCustomEvent(ArkUI_NodeCustomEvent *event, const ArkUI_NodeCustomEventType &event_type) override;

    void DidInit() override;
    void DidMoveToParentView() override;

 private:
    void SetLineCap(const std::string &params);
    void SetLineWidth(const std::string &params);
    void SetLineDash(const std::string &params);
    void SetStrokeStyle(const std::string &params);
    void SetFillStyle(const std::string &params);
    void BeginPath();
    void MoveTo(const std::string &params);
    void LineTo(const std::string &params);
    void Arc(const std::string &params);
    void ClosePath();
    void Stroke();
    void Fill();
    void FillText(const std::string &params);
    void StrokeText(const std::string &params);
    void SetTextAlign(const std::string &params);
    void SetFont(const std::string &params);
    void QuadraticCurveTo(const std::string &params);
    void BezierCurveTo(const std::string &params);
    void Reset();
    void DrawText(std::string params, std::shared_ptr<struct KRFontCollectionWrapper> wrapper, std::string_view type);

    void AddOp(const std::string &method, const KRAnyValue &params);
    void OnDraw(ArkUI_NodeCustomEvent *event);

    bool ShouldCacheOp(const std::string &method);
    bool MarkDirtyIfNeeded(const std::string &method);

 private:
    OH_Drawing_Canvas *canvas_ = nullptr;
    OH_Drawing_Path *drawingPath_ = nullptr;
    OH_Drawing_Brush *brush_ = nullptr;
    OH_Drawing_Pen *pen_ = nullptr;
    std::vector<std::pair<std::string, std::string>> ops_;
    std::unordered_set<std::string_view> cachable_methods_;
    TextFeature text_feature_;
};

#endif  // CORE_RENDER_OHOS_KRCANVASVIEW_H

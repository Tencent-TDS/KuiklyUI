/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_point.h>
#include <native_drawing/drawing_shader_effect.h>
#include "libohos_render/expand/components/base/KRCustomUserCallback.h"
#include "libohos_render/expand/components/richtext/KRRichTextShadow.h"

ArkUI_NodeHandle KRRichTextView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_CUSTOM);
}

void KRRichTextView::OnDestroy() {
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
    auto self = shared_from_this();
    KREventDispatchCenter::GetInstance().RegisterCustomEvent(self, ARKUI_NODE_CUSTOM_EVENT_ON_FOREGROUND_DRAW);
}

void KRRichTextView::SetShadow(const std::shared_ptr<IKRRenderShadowExport> &shadow) {
    shadow_ = shadow;
    kuikly::util::GetNodeApi()->markDirty(GetNode(), NODE_NEED_RENDER);
}

void KRRichTextView::DidMoveToParentView() {
    IKRRenderViewExport::DidMoveToParentView();
    auto self = shared_from_this();
    KREventDispatchCenter::GetInstance().RegisterCustomEvent(self, ARKUI_NODE_CUSTOM_EVENT_ON_FOREGROUND_DRAW);
}

void KRRichTextView::DidRemoveFromParentView() {
    IKRRenderViewExport::DidRemoveFromParentView();
    shadow_ = nullptr;
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
    if (fabs(textTypoSize.width - frameWidth) > 1 || textAlign != TEXT_ALIGN_LEFT) {  // 文本非左对齐，需要重新排版一次
        auto dpi = KRConfig::GetDpi();
        OH_Drawing_TypographyLayout(textTypo, frameWidth * dpi);
        if (textAlign != TEXT_ALIGN_LEFT) {
            richTextShadow->ResetTextAlign();  // 重设避免重复排版
        }
    }
    // Note: turn this on only when absolutely needed in testing build
    // KR_LOG_INFO<<"OnForegroundDraw, frameWidth:"<<frameWidth<<", shadow:"<<richTextShadow<<", node
    // handle"<<this->GetNode();
    OH_Drawing_TypographyPaint(textTypo, drawingHandle, 0, -drawOffsetY);
}

void KRRichTextView::ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                               const KRRenderCallback event_callback) {
    if (kuikly::util::isEqual(prop_key, "click")) {
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

                if (auto richTextShadow = dynamic_pointer_cast<KRRichTextShadow>(strongSelf->shadow_)) {
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
    } else {
        IKRRenderViewExport::ToSetProp(prop_key, prop_value, event_callback);
    }
}

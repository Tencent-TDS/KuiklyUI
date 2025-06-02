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

#ifndef CORE_RENDER_OHOS_KRRICHTEXTVIEW_H
#define CORE_RENDER_OHOS_KRRICHTEXTVIEW_H

#include <arkui/native_node.h>
#include "libohos_render/expand/components/base/KRCustomUserCallback.h"
#include "libohos_render/export/IKRRenderShadowExport.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/view/IKRRenderView.h"
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

 private:
    std::shared_ptr<IKRRenderShadowExport> shadow_;
    void OnForegroundDraw(ArkUI_NodeCustomEvent *event);
};

#endif  // CORE_RENDER_OHOS_KRRICHTEXTVIEW_H

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

#ifndef CORE_RENDER_OHOS_KRFORWARDARKTSVIEW_H
#define CORE_RENDER_OHOS_KRFORWARDARKTSVIEW_H

#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/foundation/KRRect.h"

class KRForwardArkTSView : public IKRRenderViewExport {
 public:
    void DidInit() override;

    void OnDestroy() override;

    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    void FireViewEventFromArkTS(std::string eventKey, KRAnyValue data) override;

    void SetRenderViewFrame(const KRRect &frame) override;

    void CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &callback) override;
    /**
     * view添加到父节点中后调用
     */
    void DidMoveToParentView() override;

    bool ToSetBaseProp(const std::string &prop_key, const KRAnyValue &prop_value,
                       const KRRenderCallback event_call_back) override;

    bool ReuseEnable() override {
        return false;
    }

 private:
    std::unordered_map<std::string, KRRenderCallback> event_registry_;
    ArkUI_NodeHandle ark_node_ = nullptr;
};

#endif  // CORE_RENDER_OHOS_KRFORWARDARKTSVIEW_H

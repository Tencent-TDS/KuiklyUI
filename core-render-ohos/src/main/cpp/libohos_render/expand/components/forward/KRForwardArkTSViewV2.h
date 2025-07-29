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

#ifndef CORE_RENDER_OHOS_KRFORWARDARKTSVIEWV2_H
#define CORE_RENDER_OHOS_KRFORWARDARKTSVIEWV2_H

#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/foundation/KRRect.h"
#include <cstdint>

class KRForwardArkTSViewV2 : public IKRRenderViewExport {
 public:
    ArkUI_NodeHandle CreateNode() override;
    std::shared_ptr<KRBaseEventHandler>  CreateBaseEventHandler(std::shared_ptr<IKRRenderView> rootView) override;
    std::shared_ptr<KRBasePropsHandler>  CreateBasePropHandler(std::shared_ptr<IKRRenderView> rootView) override;
    
    void DidInit() override;

    void OnDestroy() override;
    void DestroyNode() override;
    void ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                           const KRRenderCallback event_call_back = nullptr) override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    void FireViewEventFromArkTS(std::string eventKey, KRAnyValue data) override;

    void SetRenderViewFrame(const KRRect &frame) override;

    void CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &callback) override;
    void InsertChildNode(ArkUI_NodeHandle parent, ArkUI_NodeHandle child, int index) override{
        if(node_content_handle_  && child  && kuikly::util::ArkUINativeNodeAPI::GetInstance()->IsNodeAlive(child)){
            kuikly::util::GetNodeApi()->insertChildAt(parent, child, index);
            int32_t err = OH_ArkUI_NodeContent_AddNode(node_content_handle_, child);
            if(err != ARKUI_ERROR_CODE_NO_ERROR){
                KR_LOG_ERROR<<"Failed to add node content";
            }   
        }
    }
    void RemoveChildNode(ArkUI_NodeHandle parent, ArkUI_NodeHandle child) override {
        if(node_content_handle_ && child && kuikly::util::ArkUINativeNodeAPI::GetInstance()->IsNodeAlive(child)){
            OH_ArkUI_NodeContent_RemoveNode(node_content_handle_, child);
        }
    }
    void WillMoveToParentView() override;
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
    ArkUI_NodeContentHandle node_content_handle_ = nullptr;
};

#endif  // CORE_RENDER_OHOS_KRFORWARDARKTSVIEWV2_H

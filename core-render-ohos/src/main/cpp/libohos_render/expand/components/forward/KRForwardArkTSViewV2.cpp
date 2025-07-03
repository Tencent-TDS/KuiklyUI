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

#include "libohos_render/expand/components/forward/KRForwardArkTSViewV2.h"

#include "libohos_render/manager/KRArkTSManager.h"

std::shared_ptr<KRBaseEventHandler>  KRForwardArkTSViewV2::CreateBaseEventHandler(std::shared_ptr<IKRRenderView> rootView) {
    if(rootView){
        return std::make_shared<KRArkTSBaseEventHandler>(rootView->GetContext()->Config());
    }
    return nullptr;
}
std::shared_ptr<KRBasePropsHandler>  KRForwardArkTSViewV2::CreateBasePropHandler(std::shared_ptr<IKRRenderView> rootView) {
    if(rootView){
        return std::make_shared<KRArkTSViewBasePropsHandler>(shared_from_this());
    }
    return nullptr;
}

void KRForwardArkTSViewV2::DidInit() {
    // blank
}

void KRForwardArkTSViewV2::OnDestroy() {
    // blank
}

void KRForwardArkTSViewV2::DestroyNode(){
     KRArkTSManager::GetInstance().CallArkTSMethod(this->GetInstanceId(), KRNativeCallArkTSMethod::RemoveView,
                                                  std::make_shared<KRRenderValue>(this->GetViewTag()), nullptr, nullptr,
                                                  nullptr, nullptr, nullptr);
    kuikly::util::GetNodeApi()->registerNodeCreatedFromArkTS(node_);
    ark_node_ = nullptr;
    node_ = nullptr;
}
void KRForwardArkTSViewV2::ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                           const KRRenderCallback event_call_back){
    SetProp(prop_key, prop_value, event_call_back);
}

bool KRForwardArkTSViewV2::ToSetBaseProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                       const KRRenderCallback event_call_back) {
    bool handled = IKRRenderViewExport::ToSetBaseProp(prop_key, prop_value, event_call_back);
    if (handled) {
        if (prop_key == kBackgroundColor || prop_key == kBackgroundImage) {
            KRArkTSManager::GetInstance().CallArkTSMethod(this->GetInstanceId(), KRNativeCallArkTSMethod::SetViewProp,
                                                          std::make_shared<KRRenderValue>(this->GetViewTag()),
                                                          std::make_shared<KRRenderValue>(prop_key), prop_value,
                                                          nullptr, nullptr, nullptr);
        }
    }
    return handled;
}

bool KRForwardArkTSViewV2::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                 const KRRenderCallback event_call_back) {
    if (event_call_back) {  // is event
        event_registry_[prop_key] = event_call_back;
        // 设置事件
        KRArkTSManager::GetInstance().CallArkTSMethod(this->GetInstanceId(), KRNativeCallArkTSMethod::SetViewEvent,
                                                      std::make_shared<KRRenderValue>(this->GetViewTag()),
                                                      std::make_shared<KRRenderValue>(prop_key), nullptr, nullptr,
                                                      nullptr, nullptr);
    } else {  // is prop
        // 设置属性
        if(prop_key == "frame"){
            KRRect frame;
            const std::string &s = prop_value->toString();
            memcpy(&frame, s.data(), s.size());
            std::string serialized(64,0);
            snprintf(serialized.data(), serialized.size() - 1, "%.3f %.3f %.3f %.3f %d", frame.x, frame.y, frame.width, frame.height, frame.isDefaultZero());
            KRArkTSManager::GetInstance().CallArkTSMethod(this->GetInstanceId(), KRNativeCallArkTSMethod::SetViewProp,
                                                                  std::make_shared<KRRenderValue>(this->GetViewTag()),
                                                                  std::make_shared<KRRenderValue>(prop_key), std::make_shared<KRRenderValue>(serialized), nullptr,
                                                                  nullptr, nullptr);            
        }else{
            KRArkTSManager::GetInstance().CallArkTSMethod(this->GetInstanceId(), KRNativeCallArkTSMethod::SetViewProp,
                                                                  std::make_shared<KRRenderValue>(this->GetViewTag()),
                                                                  std::make_shared<KRRenderValue>(prop_key), prop_value, nullptr,
                                                                  nullptr, nullptr);            
        }
        
    }
    return true;
}

void KRForwardArkTSViewV2::SetRenderViewFrame(const KRRect &frame) {
    if (ark_node_ != nullptr) {
        KRArkTSManager::GetInstance().CallArkTSMethod(
            this->GetInstanceId(), KRNativeCallArkTSMethod::SetViewSize,
            std::make_shared<KRRenderValue>(this->GetViewTag()), std::make_shared<KRRenderValue>(frame.width),
            std::make_shared<KRRenderValue>(frame.height), nullptr, nullptr, nullptr);
    }
}
ArkUI_NodeHandle KRForwardArkTSViewV2::CreateNode(){
    if (auto rootView = GetRootView().lock()) {
        auto uiContext = rootView->GetUIContextHandle();
        if (!uiContext) {
            return nullptr;
        }
        KRArkTSManager::GetInstance().CallArkTSMethod(
        this->GetInstanceId(), KRNativeCallArkTSMethod::CreateView, std::make_shared<KRRenderValue>(this->GetViewTag()),
        std::make_shared<KRRenderValue>(this->GetViewName()), nullptr, nullptr, nullptr, nullptr);

        napi_handle_scope scope;
        napi_env g_env = KRArkTSManager::GetInstance().GetEnv();
        napi_open_handle_scope(g_env, &scope);
        ArkUI_NodeHandle node = nullptr;
        
        KRArkTSManager::GetInstance().CallArkTSMethod(this->GetInstanceId(), KRNativeCallArkTSMethod::CreateArkUINode,
                                                      std::make_shared<KRRenderValue>(this->GetViewTag()),
                                                      std::make_shared<KRRenderValue>(this->GetNodeId()),
                                                      nullptr, nullptr, nullptr, nullptr, false, &node, false, &node_content_handle_);
        if (node == nullptr) {
            return nullptr;
        }
        ark_node_ = node;
        kuikly::util::GetNodeApi()->registerNodeCreatedFromArkTS(node);
        return node;
    }
    return nullptr;
}

void KRForwardArkTSViewV2::WillMoveToParentView() {
    // blank
}
/**
 * view添加到父节点中后调用
 */
void KRForwardArkTSViewV2::DidMoveToParentView() {
    // blank
}

void KRForwardArkTSViewV2::FireViewEventFromArkTS(std::string eventKey, KRAnyValue data) {
    auto callback = event_registry_[eventKey];
    if (callback != nullptr) {
        callback(data);
    }
}

void KRForwardArkTSViewV2::CallMethod(const std::string &method, const KRAnyValue &params,
                                    const KRRenderCallback &callback) {
    KRArkTSManager::GetInstance().CallArkTSMethod(this->GetInstanceId(), KRNativeCallArkTSMethod::CallViewMethod,
                                                  std::make_shared<KRRenderValue>(this->GetViewTag()),
                                                  std::make_shared<KRRenderValue>(method), params, nullptr, nullptr,
                                                  callback);
}

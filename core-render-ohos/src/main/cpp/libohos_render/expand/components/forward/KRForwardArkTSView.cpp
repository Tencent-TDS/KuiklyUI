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

#include "libohos_render/expand/components/forward/KRForwardArkTSView.h"

#include "libohos_render/manager/KRArkTSManager.h"

void KRForwardArkTSView::DidInit() {
    KRArkTSManager::GetInstance().CallArkTSMethod(
        GetInstanceId(), KRNativeCallArkTSMethod::CreateView, std::make_shared<KRRenderValue>(GetViewTag()),
        std::make_shared<KRRenderValue>(GetViewName()), nullptr, nullptr, nullptr, nullptr);
}

void KRForwardArkTSView::OnDestroy() {
    KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::RemoveView,
                                                  std::make_shared<KRRenderValue>(GetViewTag()), nullptr, nullptr,
                                                  nullptr, nullptr, nullptr);
    if (ark_node_ != nullptr) {
        kuikly::util::GetNodeApi()->disposeNode(ark_node_);
    }
    ark_node_ = nullptr;
}

bool KRForwardArkTSView::ToSetBaseProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                       const KRRenderCallback event_call_back) {
    bool handled = IKRRenderViewExport::ToSetBaseProp(prop_key, prop_value, event_call_back);
    if (handled) {
        if (prop_key == kBackgroundColor || prop_key == kBackgroundImage) {
            KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::SetViewProp,
                                                          std::make_shared<KRRenderValue>(GetViewTag()),
                                                          std::make_shared<KRRenderValue>(prop_key), prop_value,
                                                          nullptr, nullptr, nullptr);
        }
    }
    return handled;
}

bool KRForwardArkTSView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                 const KRRenderCallback event_call_back) {
    if (event_call_back) {  // is event
        event_registry_[prop_key] = event_call_back;
        // 设置事件
        KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::SetViewEvent,
                                                      std::make_shared<KRRenderValue>(GetViewTag()),
                                                      std::make_shared<KRRenderValue>(prop_key), nullptr, nullptr,
                                                      nullptr, nullptr);
    } else {  // is prop
        // 设置属性
        KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::SetViewProp,
                                                      std::make_shared<KRRenderValue>(GetViewTag()),
                                                      std::make_shared<KRRenderValue>(prop_key), prop_value, nullptr,
                                                      nullptr, nullptr);
    }
    return true;
}

void KRForwardArkTSView::SetRenderViewFrame(const KRRect &frame) {
    if (ark_node_ != nullptr) {
        KRArkTSManager::GetInstance().CallArkTSMethod(
            GetInstanceId(), KRNativeCallArkTSMethod::SetViewSize,
            std::make_shared<KRRenderValue>(GetViewTag()), std::make_shared<KRRenderValue>(frame.width),
            std::make_shared<KRRenderValue>(frame.height), nullptr, nullptr, nullptr);
    }
}

/**
 * view添加到父节点中后调用
 */
void KRForwardArkTSView::DidMoveToParentView() {
    if (auto rootView = GetRootView().lock()) {
        auto uiContext = rootView->GetUIContextHandle();
        if (!uiContext) {
            return;
        }
        napi_handle_scope scope;
        napi_env g_env = KRArkTSManager::GetInstance().GetEnv();
        napi_open_handle_scope(g_env, &scope);
        ArkUI_NodeHandle node = nullptr;
        KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::CreateArkUINode,
                                                      std::make_shared<KRRenderValue>(GetViewTag()),
                                                      std::make_shared<KRRenderValue>(GetNodeId()),
                                                      nullptr, nullptr, nullptr, nullptr, false, &node);
        if (node == nullptr) {
            return;
        }
        ark_node_ = node;
        kuikly::util::GetNodeApi()->registerNodeCreatedFromArkTS(node);
        kuikly::util::GetNodeApi()->addChild(GetNode(), node);
        KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(),
                                                      KRNativeCallArkTSMethod::DidMoveToParentView,
                                                      std::make_shared<KRRenderValue>(GetViewTag()), nullptr,
                                                      nullptr, nullptr, nullptr, nullptr, false, &node);
    }
}

void KRForwardArkTSView::FireViewEventFromArkTS(std::string eventKey, KRAnyValue data) {
    auto callback = event_registry_[eventKey];
    if (callback != nullptr) {
        callback(data);
    }
}

void KRForwardArkTSView::CallMethod(const std::string &method, const KRAnyValue &params,
                                    const KRRenderCallback &callback) {
    KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::CallViewMethod,
                                                  std::make_shared<KRRenderValue>(GetViewTag()),
                                                  std::make_shared<KRRenderValue>(method), params, nullptr, nullptr,
                                                  callback);
}

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

#include "libohos_render/expand/components/forward/KRForwardArkTSView.h"
#include "libohos_render/manager/KRArkTSManager.h"

void KRForwardArkTSView::DidInit() {
    KRArkTSManager::GetInstance().CallArkTSMethod(
        GetInstanceId(), KRNativeCallArkTSMethod::CreateView, KRRenderValue::Make(GetViewTag()),
        KRRenderValue::Make(GetViewName()), nullptr, nullptr, nullptr, nullptr);
    // 设置ARKUI_HIT_TEST_MODE_NONE,否则ForwardArkTSView会遮挡同层级事件
    kuikly::util::UpdateNodeHitTestMode(GetNode(), ARKUI_HIT_TEST_MODE_NONE);
}

void KRForwardArkTSView::OnDestroy() {
    KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::RemoveView,
                                                  KRRenderValue::Make(GetViewTag()), nullptr, nullptr,
                                                  nullptr, nullptr, nullptr);
    if (ark_node_ != nullptr) {
        kuikly::util::GetNodeApi()->disposeNode(ark_node_);
    }
    ark_node_ = nullptr;
}

bool KRForwardArkTSView::ToSetBaseProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                       const KRRenderCallback event_call_back) {
    bool handled = IKRRenderViewExport::ToSetBaseProp(prop_key, prop_value, event_call_back);
    // IKRRenderViewExport 处理当前的事件的处理与绑定
    // IKRRenderViewExport 中的处理顺序是 ToSetBaseProp -> base_event_handler_.SetProp -> SetProp
    // 把 setProp 中处理event 提前到 ToSetBaseProp 中，不让 KRBaseEventHandler 截胡
    if (event_call_back) {
        event_registry_[prop_key] = event_call_back;
        KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::SetViewEvent,
                                                      KRRenderValue::Make(GetViewTag()),
                                                      KRRenderValue::Make(prop_key), nullptr, nullptr,
                                                      nullptr, nullptr);
        handled = true;
    }

    if (handled && !event_call_back) {
        if (prop_key == kBackgroundColor || prop_key == kBackgroundImage) {
            KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::SetViewProp,
                                                          KRRenderValue::Make(GetViewTag()),
                                                          KRRenderValue::Make(prop_key), prop_value,
                                                          nullptr, nullptr, nullptr);
        }
    }
    return handled;
}

bool KRForwardArkTSView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                 const KRRenderCallback event_call_back) {
    // 当前SetProp 是 RenderViewExport 中属性设置的最后一层，处理事件的绑定的应该在第一层 toSetProp 中就被记录下来
    if (event_call_back) {
        // 事件已在 ToSetBaseProp 中处理，避免重复
        return true;
    }
    // 设置属性
    KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::SetViewProp,
                                                  KRRenderValue::Make(GetViewTag()),
                                                  KRRenderValue::Make(prop_key), prop_value, nullptr,
                                                  nullptr, nullptr);
    return true;
}

void KRForwardArkTSView::SetRenderViewFrame(const KRRect &frame) {
    KRArkTSManager::GetInstance().CallArkTSMethod(
        GetInstanceId(), KRNativeCallArkTSMethod::SetViewSize,
        KRRenderValue::Make(GetViewTag()), KRRenderValue::Make(frame.width),
        KRRenderValue::Make(frame.height), nullptr, nullptr, nullptr);
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
                                                      KRRenderValue::Make(GetViewTag()),
                                                      KRRenderValue::Make(GetNodeId()),
                                                      nullptr, nullptr, nullptr, nullptr, false, &node);
        if (node == nullptr) {
            return;
        }
        ark_node_ = node;
        kuikly::util::GetNodeApi()->registerNodeCreatedFromArkTS(node);
        kuikly::util::GetNodeApi()->addChild(GetNode(), node);
        KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(),
                                                      KRNativeCallArkTSMethod::DidMoveToParentView,
                                                      KRRenderValue::Make(GetViewTag()), nullptr,
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
                                                  KRRenderValue::Make(GetViewTag()),
                                                  KRRenderValue::Make(method), params, nullptr, nullptr,
                                                  callback);
}

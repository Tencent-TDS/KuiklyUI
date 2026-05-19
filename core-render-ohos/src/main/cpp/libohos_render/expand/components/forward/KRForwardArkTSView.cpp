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
#include "libohos_render/utils/KRRenderLoger.h"

static constexpr char kClickPropKey[] = "click";
static constexpr char kGetEditableLayersMethod[] = "getEditableLayersUnderPoint";
static constexpr char kPAGViewName[] = "KRPAGView";

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
    if (handled) {
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
    if (event_call_back) {  // is event
        event_registry_[prop_key] = event_call_back;
        // 设置事件
        KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::SetViewEvent,
                                                      KRRenderValue::Make(GetViewTag()),
                                                      KRRenderValue::Make(prop_key), nullptr, nullptr,
                                                      nullptr, nullptr);
    } else {  // is prop
        // 设置属性
        KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::SetViewProp,
                                                      KRRenderValue::Make(GetViewTag()),
                                                      KRRenderValue::Make(prop_key), prop_value, nullptr,
                                                      nullptr, nullptr);
    }
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

void KRForwardArkTSView::ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                   const KRRenderCallback event_call_back) {
    if (event_call_back != nullptr && prop_key == kClickPropKey) {
        KR_LOG_INFO << "[PAGClick] ToSetProp click event, ViewName=" << GetViewName()
                    << ", isPAG=" << (GetViewName() == kPAGViewName ? "YES" : "NO");
    }
    // 主动拦截PAGView上的click事件
    if (event_call_back != nullptr && prop_key == kClickPropKey &&
        GetViewName() == kPAGViewName) {
        // 拦截 click 事件，用包装后的 callback 替代原始 callback
        auto wrapped_callback = WrapClickCallbackWithLayers(event_call_back);
        // DidInit 中设置了 HIT_TEST_MODE_NONE 导致 C Node 不接收手势，
        // 改为 TRANSPARENT 使手势可触发，同时不遮挡兄弟节点
        kuikly::util::UpdateNodeHitTestMode(GetNode(), ARKUI_HIT_TEST_MODE_TRANSPARENT);
        IKRRenderViewExport::ToSetProp(prop_key, prop_value, wrapped_callback);
    } else {
        IKRRenderViewExport::ToSetProp(prop_key, prop_value, event_call_back);
    }
}

KRRenderCallback KRForwardArkTSView::WrapClickCallbackWithLayers(const KRRenderCallback &original_callback) {
    std::weak_ptr<IKRRenderViewExport> weak_self = shared_from_this();
    return [original_callback, weak_self](KRAnyValue click_params) {
        auto self = weak_self.lock();
        if (!self || !original_callback) {
            if (original_callback) {
                original_callback(click_params);
            }
            return;
        }
        // 从 click params 中提取 x, y 坐标
        auto params_map = click_params ? click_params->toMap() : KRRenderValueMap();
        float x = 0.0f;
        float y = 0.0f;
        if (params_map.count("x")) {
            x = params_map["x"]->toFloat();
        }
        if (params_map.count("y")) {
            y = params_map["y"]->toFloat();
        }
        // 构造参数调用 ArkTS 侧的 getEditableLayersUnderPoint
        KRRenderValueMap method_params;
        method_params["x"] = NewKRRenderValue(x);
        method_params["y"] = NewKRRenderValue(y);
        auto method_params_json = NewKRRenderValue(method_params)->toString();
        // 同步调用 ArkTS 层获取 layers，callback 在 napi_call_function 内同步回调
        KRAnyValue layers_result;
        bool callback_invoked = false;
        self->CallMethod(kGetEditableLayersMethod, NewKRRenderValue(method_params_json),
                         [&layers_result, &callback_invoked](KRAnyValue result) {
                             callback_invoked = true;
                             layers_result = result;
                         });
        // 注入 layers 到 click params 中
        if (layers_result && !layers_result->isNull()) {
            params_map["layers"] = layers_result;
        } else {
            params_map["layers"] = NewKRRenderValue(std::string("[]"));
        }
        original_callback(NewKRRenderValue(params_map));
    };
}

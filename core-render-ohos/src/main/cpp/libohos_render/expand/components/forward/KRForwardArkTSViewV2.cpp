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
                                                  KRRenderValue::Make(this->GetViewTag()), nullptr, nullptr,
                                                  nullptr, nullptr, nullptr);
    kuikly::util::GetNodeApi()->registerNodeCreatedFromArkTS(node_);
    ark_node_ = nullptr;
    node_ = nullptr;
}
void KRForwardArkTSViewV2::ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                           const KRRenderCallback event_call_back){
    // V2 转发 view 原先把属性整个透传给 ArkTS，绕过了基类 ToSetProp。而基类里才做了两件事：
    //   1) 把 frame 应用到原生节点（转发节点因此才有尺寸）；
    //   2) frame 变化时回调 SetRenderViewFrame → ArkTS 的 sizeDidChanged。
    // 跳过之后 ArkTS 组件永远拿不到自身尺寸，依赖尺寸的布局（如 Navigation 标题栏高度）
    // 会一直是 0，表现为"节点树里存在、屏幕上不渲染"。
    // 这里对 frame 补一次基类处理，其余属性仍照旧透传给 ArkTS。
    if (prop_key == "frame" && prop_value != nullptr) {
        IKRRenderViewExport::ToSetProp(prop_key, prop_value, nullptr);
    }
    SetProp(prop_key, prop_value, event_call_back);
}

bool KRForwardArkTSViewV2::ToSetBaseProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                       const KRRenderCallback event_call_back) {
    bool handled = IKRRenderViewExport::ToSetBaseProp(prop_key, prop_value, event_call_back);
    if (handled) {
        if (prop_key == kBackgroundColor || prop_key == kBackgroundImage) {
            KRArkTSManager::GetInstance().CallArkTSMethod(this->GetInstanceId(), KRNativeCallArkTSMethod::SetViewProp,
                                                          KRRenderValue::Make(this->GetViewTag()),
                                                          KRRenderValue::Make(prop_key), prop_value,
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
                                                      KRRenderValue::Make(this->GetViewTag()),
                                                      KRRenderValue::Make(prop_key), nullptr, nullptr,
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
                                                                  KRRenderValue::Make(this->GetViewTag()),
                                                                  KRRenderValue::Make(prop_key), KRRenderValue::Make(serialized), nullptr,
                                                                  nullptr, nullptr);            
        }else{
            KRArkTSManager::GetInstance().CallArkTSMethod(this->GetInstanceId(), KRNativeCallArkTSMethod::SetViewProp,
                                                                  KRRenderValue::Make(this->GetViewTag()),
                                                                  KRRenderValue::Make(prop_key), prop_value, nullptr,
                                                                  nullptr, nullptr);            
        }
        
    }
    return true;
}

void KRForwardArkTSViewV2::SetRenderViewFrame(const KRRect &frame) {
    if (ark_node_ != nullptr) {
        // V2 的 props handler（KRArkTSViewBasePropsHandler）对所有属性都返回 false，
        // 通用属性链路不会执行，因此节点尺寸只能在这里显式维护；
        // 否则 ArkTS 转发节点没有尺寸，其子树里的百分比布局（width/height 100%）无从计算。
        kuikly::util::UpdateNodeFrame(ark_node_, frame);
        KR_LOG_INFO << "ImmLightTest: SetRenderViewFrame w=" << frame.width << ", h=" << frame.height
                    << ", x=" << frame.x << ", y=" << frame.y;
        KRArkTSManager::GetInstance().CallArkTSMethod(
            this->GetInstanceId(), KRNativeCallArkTSMethod::SetViewSize,
            KRRenderValue::Make(this->GetViewTag()), KRRenderValue::Make(frame.width),
            KRRenderValue::Make(frame.height), nullptr, nullptr, nullptr);
    }
}
ArkUI_NodeHandle KRForwardArkTSViewV2::CreateNode(){
    if (auto rootView = GetRootView().lock()) {
        auto uiContext = rootView->GetUIContextHandle();
        if (!uiContext) {
            return nullptr;
        }
        KRArkTSManager::GetInstance().CallArkTSMethod(
        this->GetInstanceId(), KRNativeCallArkTSMethod::CreateView, KRRenderValue::Make(this->GetViewTag()),
        KRRenderValue::Make(this->GetViewName()), nullptr, nullptr, nullptr, nullptr);

        napi_handle_scope scope;
        napi_env g_env = KRArkTSManager::GetInstance().GetEnv();
        napi_open_handle_scope(g_env, &scope);
        ArkUI_NodeHandle node = nullptr;
        
        KRArkTSManager::GetInstance().CallArkTSMethod(this->GetInstanceId(), KRNativeCallArkTSMethod::CreateArkUINode,
                                                      KRRenderValue::Make(this->GetViewTag()),
                                                      KRRenderValue::Make(this->GetNodeId()),
                                                      nullptr, nullptr, nullptr, nullptr, false, &node, false, &node_content_handle_);
        if (node == nullptr) {
            return nullptr;
        }
        ark_node_ = node;
        kuikly::util::GetNodeApi()->registerNodeCreatedFromArkTS(node);
        // 若 frame 早于节点创建到达（props handler 不处理通用属性，会丢掉那次 frame），
        // 这里补一次当前已知的 frame。
        kuikly::util::UpdateNodeFrame(node, GetFrame());
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
                                                  KRRenderValue::Make(this->GetViewTag()),
                                                  KRRenderValue::Make(method), params, nullptr, nullptr,
                                                  callback);
}

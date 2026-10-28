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
#include "libohos_render/manager/KRSnapshotManager.h"
#include "libohos_render/scheduler/KRContextScheduler.h"

#include <algorithm>
#include <cmath>

extern int32_t OH_ArkUI_NodeUtils_GetLayoutSize(ArkUI_NodeHandle node, ArkUI_IntSize *size) __attribute__((weak));

namespace {
int CaptureIntervalMs(const KRAnyValue &params) {
    if (!params) {
        return 100;
    }
    const std::string value = params->toString();
    const auto keyPosition = value.find("intervalMs");
    if (keyPosition == std::string::npos) {
        return 100;
    }
    const auto colonPosition = value.find(':', keyPosition);
    if (colonPosition == std::string::npos) {
        return 100;
    }
    try {
        return std::max(1, std::stoi(value.substr(colonPosition + 1)));
    } catch (...) {
        return 100;
    }
}

float CaptureDimension(const KRAnyValue &params, const char *key) {
    if (!params) {
        return 0;
    }
    const std::string value = params->toString();
    const auto keyPosition = value.find(key);
    if (keyPosition == std::string::npos) {
        return 0;
    }
    const auto colonPosition = value.find(':', keyPosition);
    if (colonPosition == std::string::npos) {
        return 0;
    }
    try {
        return std::stof(value.substr(colonPosition + 1));
    } catch (...) {
        return 0;
    }
}
}

void KRForwardArkTSView::DidInit() {
    KRArkTSManager::GetInstance().CallArkTSMethod(
        GetInstanceId(), KRNativeCallArkTSMethod::CreateView, KRRenderValue::Make(GetViewTag()),
        KRRenderValue::Make(GetViewName()), nullptr, nullptr, nullptr, nullptr);
    // 设置 ARKUI_HIT_TEST_MODE_TRANSPARENT, ForwardArkTSView 不会遮挡同层级事件
    kuikly::util::UpdateNodeHitTestMode(GetNode(), ARKUI_HIT_TEST_MODE_TRANSPARENT);
}

void KRForwardArkTSView::OnDestroy() {
    StopPixelMapCapture();
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
    // If ark_node_ already exists, this is a re-parent (movableContent move).
    // Skip re-creating ComponentContent — the existing one moved with the STACK node.
    if (ark_node_ != nullptr) {
        // Still notify ArkTS side so it can resume state (e.g. video playback)
        KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(),
                                                      KRNativeCallArkTSMethod::DidMoveToParentView,
                                                      KRRenderValue::Make(GetViewTag()), nullptr,
                                                      nullptr, nullptr, nullptr, nullptr);
        return;
    }
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
    if (GetViewName() == "KRNaviMirrorCaptureView" && method == "startCapture") {
        StartPixelMapCapture(CaptureIntervalMs(params));
        pixel_map_capture_output_width_px_ = CaptureDimension(params, "outputWidthPx");
        pixel_map_capture_output_height_px_ = CaptureDimension(params, "outputHeightPx");
        if (callback) {
            KRRenderValue::Map result;
            result["code"] = KRRenderValue::Make(0);
            callback(KRRenderValue::Make(result));
        }
        return;
    }
    if (GetViewName() == "KRNaviMirrorCaptureView" && method == "stopCapture") {
        StopPixelMapCapture();
        if (callback) {
            KRRenderValue::Map result;
            result["code"] = KRRenderValue::Make(0);
            callback(KRRenderValue::Make(result));
        }
        return;
    }
    KRArkTSManager::GetInstance().CallArkTSMethod(GetInstanceId(), KRNativeCallArkTSMethod::CallViewMethod,
                                                  KRRenderValue::Make(GetViewTag()),
                                                  KRRenderValue::Make(method), params, nullptr, nullptr,
                                                  callback);
}

void KRForwardArkTSView::StartPixelMapCapture(int interval_ms) {
    pixel_map_capture_interval_ms_ = std::max(1, interval_ms);
    if (pixel_map_capture_running_) {
        return;
    }
    pixel_map_capture_running_ = true;
    SchedulePixelMapCapture(0);
}

void KRForwardArkTSView::StopPixelMapCapture() {
    pixel_map_capture_running_ = false;
}

void KRForwardArkTSView::SchedulePixelMapCapture(int delay_ms) {
    std::weak_ptr<IKRRenderViewExport> weakSelf = shared_from_this();
    KRContextScheduler::ScheduleTask(delay_ms, [weakSelf] {
        auto self = weakSelf.lock();
        auto forwardView = std::dynamic_pointer_cast<KRForwardArkTSView>(self);
        if (!forwardView || !forwardView->pixel_map_capture_running_) {
            return;
        }

        // Get the physical CAPI layout size. The Kuikly width/height values are
        // logical vp/dp values and cannot be compared directly with codec pixels.
        if (OH_ArkUI_NodeUtils_GetLayoutSize && forwardView->pixel_map_capture_output_width_px_ > 0 &&
            forwardView->pixel_map_capture_output_height_px_ > 0) {
            ArkUI_IntSize layoutSize = {0, 0};
            if (OH_ArkUI_NodeUtils_GetLayoutSize(forwardView->GetNode(), &layoutSize) == 0 &&
                layoutSize.width > 0 && layoutSize.height > 0) {
                const float widthRatio = static_cast<float>(layoutSize.width) /
                    forwardView->pixel_map_capture_output_width_px_;
                const float heightRatio = static_cast<float>(layoutSize.height) /
                    forwardView->pixel_map_capture_output_height_px_;
                forwardView->pixel_map_capture_sample_size_ = std::max(
                    1, static_cast<int>(std::floor(std::min(widthRatio, heightRatio))));
            }
        }
        forwardView->CapturePixelMapOnce();
    });
}

void KRForwardArkTSView::CapturePixelMapOnce() {
    std::weak_ptr<IKRRenderViewExport> weakSelf = shared_from_this();
    KRContextScheduler::ScheduleTaskOnMainThread(false, [weakSelf] {
        auto self = weakSelf.lock();
        auto forwardView = std::dynamic_pointer_cast<KRForwardArkTSView>(self);
        if (!forwardView || !forwardView->pixel_map_capture_running_) {
            return;
        }

        // 与 IKRRenderViewExport::CallMethod("toImage") 一致，在主线程上把
        // nodeId 写入 Forward View 对应的 CAPI 原生节点。
        const std::string nodeId = forwardView->GetNodeId();
        ArkUI_AttributeItem item = {nullptr, 0, nodeId.c_str(), nullptr};
        kuikly::util::GetNodeApi()->setAttribute(forwardView->GetNode(), NODE_ID, &item);
        if (auto root = forwardView->GetRootView().lock()) {
            root->GetSnapshotManager()->TakePixelMapSnapshot(
                forwardView->GetInstanceId(), forwardView->GetViewTag(), nodeId,
                forwardView->pixel_map_capture_sample_size_, forwardView,
                [weakSelf] {
                    auto self = weakSelf.lock();
                    auto forwardView = std::dynamic_pointer_cast<KRForwardArkTSView>(self);
                    if (forwardView && forwardView->pixel_map_capture_running_) {
                        forwardView->SchedulePixelMapCapture(forwardView->pixel_map_capture_interval_ms_);
                    }
                });
        }
    });
}

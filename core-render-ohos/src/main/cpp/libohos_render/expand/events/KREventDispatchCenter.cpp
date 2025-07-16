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

#include "libohos_render/expand/events/KREventDispatchCenter.h"

#include <arkui/native_gesture.h>
#include "libohos_render/expand/components/base/KRCustomUserCallback.h"
#include "libohos_render/expand/events/gesture/KRGestueEventType.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/manager/KRWeakObjectManager.h"
#include "libohos_render/utils/KRThreadChecker.h"
#include "libohos_render/utils/KRViewUtil.h"

#ifdef __cplusplus
extern "C" {
#endif
// Remove this declaration if compatable api is raised to 18 and above
extern void* OH_ArkUI_GestureInterrupter_GetUserData(ArkUI_GestureInterruptInfo* event) __attribute__((weak));
#ifdef __cplusplus
};
#endif

bool returnIfNull(const ArkUI_NodeHandle ark_ui_node_handle) {
    if (!ark_ui_node_handle) {
        // TODO(userName): log
        return true;
    }
    return false;
}

KREventDispatchCenter &KREventDispatchCenter::GetInstance() {
    static KREventDispatchCenter dispatchCenter;
    return dispatchCenter;
}

KREventDispatchCenter::KREventDispatchCenter() {
    // The global dispatcher could cause conflicts with other libs, e.g. Hippy.
    // Instead of using registerNodeEventReceiver and registerNodeCustomEventReceiver,
    // we'll use addNodeEventReceiver/removeNodeEventReceiver and
    // addNodeCustomEventReceiver/removeNodeCustomEventReceiver on demand.
}

void KREventDispatchCenter::OnReceiverEvent(ArkUI_NodeEvent *event) {
    KREnsureMainThread();

    auto ark_ui_node_handle = kuikly::util::GetArkUINodeHandle(event);
    if (returnIfNull(ark_ui_node_handle)) {
        return;
    }
    auto it = event_handler_map_.find(ark_ui_node_handle);
    if (it == event_handler_map_.end()) {
        return;
    }
    it->second->ViewExport()->ToOnEvent(event, kuikly::util::GetArkUINodeEventType(event));
}

void KREventDispatchCenter::OnReceiverCustomEvent(ArkUI_NodeCustomEvent *event) {
    KREnsureMainThread();

    auto ark_ui_node_handle = OH_ArkUI_NodeCustomEvent_GetNodeHandle(event);
    if (returnIfNull(ark_ui_node_handle)) {
        return;
    }
    auto it = custom_event_handler_map_.find(ark_ui_node_handle);
    if (it == custom_event_handler_map_.end()) {
        return;
    }
    it->second->ViewExport()->ToOnCustomEvent(event, OH_ArkUI_NodeCustomEvent_GetEventType(event));
}

static void KRNodeEventReceiver(ArkUI_NodeEvent *event) {
    KREventDispatchCenter::GetInstance().OnReceiverEvent(event);
}

static void KRNodeCustomEventReceiver(ArkUI_NodeCustomEvent *event) {
    KREventDispatchCenter::GetInstance().OnReceiverCustomEvent(event);
}

void KREventDispatchCenter::RegisterEvent(const std::shared_ptr<IKRRenderViewExport> &view_export,
                                          const ArkUI_NodeEventType &event_type) {
    KREnsureMainThread();

    auto ark_ui_node_handle = view_export->GetNode();

    if (returnIfNull(ark_ui_node_handle)) {
        return;
    }

    auto it = event_handler_map_.find(ark_ui_node_handle);
    if (it == event_handler_map_.end()) {
        kuikly::util::GetNodeApi()->addNodeEventReceiver(ark_ui_node_handle, KRNodeEventReceiver);
        auto entry = std::make_shared<KREventRegisterEntry>(view_export);
        entry->AddEvent(event_type);
        event_handler_map_[ark_ui_node_handle] = entry;
    } else {
        it->second->AddEvent(event_type);
    }

    kuikly::util::GetNodeApi()->registerNodeEvent(ark_ui_node_handle, event_type, event_type, nullptr);
}

void KREventDispatchCenter::UnregisterEvent(const std::shared_ptr<IKRRenderViewExport> &view_export) {
    KREnsureMainThread();

    auto ark_ui_node_handle = view_export->GetNode();
    if (returnIfNull(ark_ui_node_handle)) {
        return;
    }
    auto it = event_handler_map_.find(ark_ui_node_handle);
    if (it == event_handler_map_.end()) {
        return;
    }

    kuikly::util::GetNodeApi()->removeNodeEventReceiver(ark_ui_node_handle, KRNodeEventReceiver);
    const auto &register_event = it->second->RegisterEvents();
    std::for_each(register_event.begin(), register_event.end(), [ark_ui_node_handle](auto event_type){
        kuikly::util::GetNodeApi()->unregisterNodeEvent(ark_ui_node_handle, event_type);
    });
    event_handler_map_.erase(ark_ui_node_handle);
}

void KREventDispatchCenter::RegisterCustomEvent(const std::shared_ptr<IKRRenderViewExport> &view_export,
                                                const ArkUI_NodeCustomEventType &event_type) {
    KREnsureMainThread();

    auto ark_ui_node_handle = view_export->GetNode();
    if (returnIfNull(ark_ui_node_handle)) {
        return;
    }
    kuikly::util::GetNodeApi()->addNodeCustomEventReceiver(ark_ui_node_handle, KRNodeCustomEventReceiver);

    auto it = custom_event_handler_map_.find(ark_ui_node_handle);
    if (it == custom_event_handler_map_.end()) {
        auto entry = std::make_shared<KRCustomEventRegisterEntry>(view_export);
        entry->AddEvent(event_type);
        custom_event_handler_map_[ark_ui_node_handle] = entry;
    } else {
        it->second->AddEvent(event_type);
    }
    kuikly::util::GetNodeApi()->registerNodeCustomEvent(ark_ui_node_handle, event_type, 0, nullptr);
}
void KREventDispatchCenter::UnregisterCustomEvent(const std::shared_ptr<IKRRenderViewExport> &view_export) {
    KREnsureMainThread();

    auto ark_ui_node_handle = view_export->GetNode();
    if (returnIfNull(ark_ui_node_handle)) {
        return;
    }
    auto it = custom_event_handler_map_.find(ark_ui_node_handle);
    if (it == custom_event_handler_map_.end()) {
        return;
    }
    kuikly::util::GetNodeApi()->removeNodeCustomEventReceiver(ark_ui_node_handle, KRNodeCustomEventReceiver);

    const auto &register_event = it->second->RegisterEvents();
    std::for_each(register_event.begin(), register_event.end(), [ark_ui_node_handle](auto event_type){
        kuikly::util::GetNodeApi()->unregisterNodeCustomEvent(ark_ui_node_handle, event_type);
    });
    custom_event_handler_map_.erase(ark_ui_node_handle);
}

KREventDispatchCenter::KRGestureEventRegisterEntry::KRGestureEventRegisterEntry(
    const std::shared_ptr<IKRRenderViewExport> &view_export)
    : view_export_(view_export) {
    KREnsureMainThread();

    gesture_group_handler_ = std::make_shared<KRGestureGroupHandler>(
        view_export_->GetNode(),
        [](const ArkUI_NodeHandle node_handle, const std::shared_ptr<KRGestureEventData> &gesture_event_data,
           const KRGestureEventType &event_type) {
            KREventDispatchCenter::GetInstance().OnReceiverGestureEvent(node_handle, gesture_event_data, event_type);
        });
}

bool KREventDispatchCenter::KRGestureEventRegisterEntry::TestCaptureRule(
    const ArkUI_GestureRecognizerType gesture_type,
    const ArkUI_GestureEvent *gesture_event) {
    auto x = view_export_->GetInterruptX();
    auto y = view_export_->GetInterruptY();
    return gesture_group_handler_->TestCaptureRule(gesture_type, gesture_event, x, y);
}

void KREventDispatchCenter::RegisterGestureEvent(const std::shared_ptr<IKRRenderViewExport> &view_export,
                                                 const KRGestureEventType &event_type) {
    KREnsureMainThread();

    auto ark_ui_node_handle = view_export->GetNode();
    if (returnIfNull(ark_ui_node_handle)) {
        return;
    }
    if (event_type == KRGestureEventType::kUnknown) {
        return;
    }

    auto it = gesture_event_handler_map_.find(ark_ui_node_handle);
    if (it == gesture_event_handler_map_.end()) {
        auto entry = std::make_shared<KRGestureEventRegisterEntry>(view_export);
        entry->AddGestureEvent(event_type);
        gesture_event_handler_map_[ark_ui_node_handle] = entry;
    } else {
        it->second->AddGestureEvent(event_type);
    }
}

void KREventDispatchCenter::UnregisterGestureEvent(const std::shared_ptr<IKRRenderViewExport> &view_export) {
    KREnsureMainThread();

    auto ark_ui_node_handle = view_export->GetNode();
    if (returnIfNull(ark_ui_node_handle)) {
        return;
    }
    gesture_event_handler_map_.erase(ark_ui_node_handle);
}

void KREventDispatchCenter::SetCaptureRule(const std::shared_ptr<IKRRenderViewExport> &view_export,
                                           const std::vector<KRGestureCaptureRule> &&rules) {
    KREnsureMainThread();

    auto ark_ui_node_handle = view_export->GetNode();
    if (returnIfNull(ark_ui_node_handle)) {
        return;
    }

    std::shared_ptr<KRGestureEventRegisterEntry> entry;
    auto it = gesture_event_handler_map_.find(ark_ui_node_handle);
    if (it == gesture_event_handler_map_.end()) {
        entry = std::make_shared<KRGestureEventRegisterEntry>(view_export);
        gesture_event_handler_map_[ark_ui_node_handle] = entry;
    } else {
        entry = it->second;
    }
    entry->SetCaptureRule(std::move(rules));
}

void KREventDispatchCenter::RegisterGestureInterrupter(const std::shared_ptr<IKRRenderViewExport> &view_export) {
    KREnsureMainThread();

    auto node_handle = view_export->GetNode();
    if (returnIfNull(node_handle)) {
        return;
    }

    if (gesture_interrupter_handler_map_.find(node_handle) == gesture_interrupter_handler_map_.end()) {
        std::shared_ptr<KRGestureEventRegisterEntry> entry;
        auto gesture_event_it = gesture_event_handler_map_.find(node_handle);
        if (gesture_event_it == gesture_event_handler_map_.end()) {
            entry = std::make_shared<KRGestureEventRegisterEntry>(view_export);
            gesture_event_handler_map_[node_handle] = entry;
        } else {
            entry = gesture_event_it->second;
        }
        gesture_interrupter_handler_map_[node_handle] = entry;
        legacy_gesture_interrupter_handler_map_[view_export->GetNodeId()] = entry;
        void *userData = KRWeakObjectManagerRegisterWeakObject(view_export);
        kuikly::util::GetGestureApi()->setGestureInterrupterToNode(node_handle, userData,  [](ArkUI_GestureInterruptInfo *info) {
            return KREventDispatchCenter::GetInstance().OnInterruptGestureEvent(info);
        });
    }
}

void KREventDispatchCenter::UnregisterGestureInterrupter(const std::shared_ptr<IKRRenderViewExport> &view_export) {
    KREnsureMainThread();

    auto ark_ui_node_handle = view_export->GetNode();
    if (returnIfNull(ark_ui_node_handle)) {
        return;
    }
    if (gesture_interrupter_handler_map_.find(ark_ui_node_handle) != gesture_interrupter_handler_map_.end()) {
        kuikly::util::GetGestureApi()->setGestureInterrupterToNode(
            ark_ui_node_handle, nullptr, [](ArkUI_GestureInterruptInfo *info) { return GESTURE_INTERRUPT_RESULT_CONTINUE; });
        gesture_interrupter_handler_map_.erase(ark_ui_node_handle);
        legacy_gesture_interrupter_handler_map_.erase(view_export->GetNodeId());
    }
}

void KREventDispatchCenter::OnReceiverGestureEvent(const ArkUI_NodeHandle node_handle,
                                                   const std::shared_ptr<KRGestureEventData> &gesture_event_data,
                                                   const KRGestureEventType &event_type) {
    if (returnIfNull(node_handle)) {
        return;
    }
    auto it = gesture_event_handler_map_.find(node_handle);
    if (it == gesture_event_handler_map_.end()) {
        return;
    }
    it->second->ViewExport()->ToOnGestureEvent(gesture_event_data, event_type);
}

ArkUI_GestureInterruptResult KREventDispatchCenter::OnInterruptGestureEvent(const ArkUI_GestureInterruptInfo *info) {
    ArkUI_NodeHandle node_handle = nullptr;
    ArkUI_GestureRecognizer *recognizer = OH_ArkUI_GestureInterruptInfo_GetRecognizer(info);
    if(OH_ArkUI_GestureInterrupter_GetUserData){
        // new api available (systems with api version >= 18)
        void* userData = OH_ArkUI_GestureInterrupter_GetUserData((ArkUI_GestureInterruptInfo*)info);
        if(userData){
            std::shared_ptr<IKRRenderViewExport> view = KRWeakObjectManagerGetWeakObject<IKRRenderViewExport>(userData).lock();
            if(view){
                node_handle = view->GetNode();
            }
        }
    }else{
        // fallback
        node_handle = kuikly::util::GetGestureApi()->GetAttachedNodeForRecognizer(recognizer);
    }

    std::shared_ptr<KRGestureEventRegisterEntry> entry;
    if (node_handle) {
        auto it = gesture_interrupter_handler_map_.find(node_handle);
        if (it != gesture_interrupter_handler_map_.end()) {
            entry = it->second;
        }
    } else {
        auto node_id = kuikly::util::GetGestureApi()->GetGestureBindNodeId(recognizer);
        auto it = legacy_gesture_interrupter_handler_map_.find(node_id);
        if (it != legacy_gesture_interrupter_handler_map_.end()) {
            entry = it->second;
        }
    }
    if (!entry) {
        // 获取是否系统手势
        auto systemFlag = OH_ArkUI_GestureInterruptInfo_GetSystemFlag(info);
        return systemFlag ? GESTURE_INTERRUPT_RESULT_CONTINUE : GESTURE_INTERRUPT_RESULT_REJECT;
    }
    if (entry->HasSetCaptureRule()) {
        auto gesture_type = kuikly::util::GetGestureApi()->getGestureType(recognizer);
        auto gesture_event = OH_ArkUI_GestureInterruptInfo_GetGestureEvent(info);
        if (!entry->TestCaptureRule(gesture_type, gesture_event)) {
            return GESTURE_INTERRUPT_RESULT_REJECT;
        }
    }
    return entry->ViewExport()->OnInterruptGestureEvent(info);
}

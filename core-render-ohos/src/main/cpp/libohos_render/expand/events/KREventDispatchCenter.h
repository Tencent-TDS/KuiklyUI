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

#ifndef CORE_RENDER_OHOS_KREVENTDISPATCHCENTER_H
#define CORE_RENDER_OHOS_KREVENTDISPATCHCENTER_H

#include <arkui/native_gesture.h>
#include <arkui/native_node.h>
#include <arkui/native_type.h>
#include <set>
#include <unordered_map>
#include "gesture/KRGestueEventType.h"
#include "libohos_render/expand/events/gesture/KRGestureGroupHandler.h"
#include "libohos_render/utils/KRViewUtil.h"
#include "libohos_render/view/IKRRenderView.h"

class KREventDispatchCenter {
 public:
    KREventDispatchCenter(const KREventDispatchCenter &) = delete;
    KREventDispatchCenter(const KREventDispatchCenter &&) = delete;
    KREventDispatchCenter &operator=(const KREventDispatchCenter &) = delete;
    KREventDispatchCenter &operator=(const KREventDispatchCenter &&) = delete;

    void RegisterEvent(const std::shared_ptr<IKRRenderViewExport> &view_export, const ArkUI_NodeEventType &event_type);
    void UnregisterEvent(const std::shared_ptr<IKRRenderViewExport> &view_export);

    void RegisterCustomEvent(const std::shared_ptr<IKRRenderViewExport> &view_export,
                             const ArkUI_NodeCustomEventType &event_type);
    void UnregisterCustomEvent(const std::shared_ptr<IKRRenderViewExport> &view_export);

    void RegisterGestureEvent(const std::shared_ptr<IKRRenderViewExport> &view_export,
                              const KRGestureEventType &event_type);
    void UnregisterGestureEvent(const std::shared_ptr<IKRRenderViewExport> &view_export);
    void SetCaptureRule(const std::shared_ptr<IKRRenderViewExport> &view_export,
                        const std::vector<KRGestureCaptureRule> &&rules);
    void RegisterGestureInterrupter(const std::shared_ptr<IKRRenderViewExport> &view_export);
    void UnregisterGestureInterrupter(const std::shared_ptr<IKRRenderViewExport> &view_export);

    static KREventDispatchCenter &GetInstance();

    void OnReceiverEvent(ArkUI_NodeEvent *event);
    void OnReceiverCustomEvent(ArkUI_NodeCustomEvent *event);
    void OnReceiverGestureEvent(const ArkUI_NodeHandle node_handle,
                                const std::shared_ptr<KRGestureEventData> &gesture_event_data,
                                const KRGestureEventType &event_type);
    ArkUI_GestureInterruptResult OnInterruptGestureEvent(const ArkUI_GestureInterruptInfo *info);

 public:
    class KREventRegisterEntry {
     private:
        std::shared_ptr<IKRRenderViewExport> view_export_;
        std::set<ArkUI_NodeEventType> register_events_;

     public:
        explicit KREventRegisterEntry(const std::shared_ptr<IKRRenderViewExport> &view_export)
            : view_export_(view_export) {}

        void AddEvent(const ArkUI_NodeEventType event_type) {
            register_events_.insert(event_type);
        }

        const std::set<ArkUI_NodeEventType> &RegisterEvents() {
            return register_events_;
        }

        std::shared_ptr<IKRRenderViewExport> &ViewExport() {
            return view_export_;
        }
    };

    class KRCustomEventRegisterEntry {
     private:
        std::shared_ptr<IKRRenderViewExport> view_export_;
        std::set<ArkUI_NodeCustomEventType> register_events_;

     public:
        explicit KRCustomEventRegisterEntry(const std::shared_ptr<IKRRenderViewExport> &view_export)
            : view_export_(view_export) {}

        void AddEvent(const ArkUI_NodeCustomEventType event_type) {
            register_events_.insert(event_type);
        }

        std::set<ArkUI_NodeCustomEventType> &RegisterEvents() {
            return register_events_;
        }

        std::shared_ptr<IKRRenderViewExport> &ViewExport() {
            return view_export_;
        }
    };

    class KRGestureEventRegisterEntry {
     private:
        std::shared_ptr<IKRRenderViewExport> view_export_;
        std::shared_ptr<KRGestureGroupHandler> gesture_group_handler_;
        bool has_capture_rule_ = false;

     public:
        explicit KRGestureEventRegisterEntry(const std::shared_ptr<IKRRenderViewExport> &view_export);

        void AddGestureEvent(const KRGestureEventType &event_type) {
            gesture_group_handler_->RegisterGestureEvent(event_type);
        }

        std::shared_ptr<IKRRenderViewExport> &ViewExport() {
            return view_export_;
        }

        void SetCaptureRule(const std::vector<KRGestureCaptureRule> &&rules) {
            has_capture_rule_ = true;
            gesture_group_handler_->SetCaptureRule(std::move(rules));
        }
        bool HasSetCaptureRule() { return has_capture_rule_; }
        bool TestCaptureRule(const ArkUI_GestureRecognizerType gesture_type,
                             const ArkUI_GestureEvent *gesture_event);
    };

 private:
    KREventDispatchCenter();

 private:
    std::unordered_map<ArkUI_NodeHandle, std::shared_ptr<KREventRegisterEntry>> event_handler_map_;
    std::unordered_map<ArkUI_NodeHandle, std::shared_ptr<KRCustomEventRegisterEntry>> custom_event_handler_map_;
    std::unordered_map<ArkUI_NodeHandle, std::shared_ptr<KRGestureEventRegisterEntry>> gesture_event_handler_map_;
    std::unordered_map<ArkUI_NodeHandle, std::shared_ptr<KRGestureEventRegisterEntry>> gesture_interrupter_handler_map_;
    std::unordered_map<std::string, std::shared_ptr<KRGestureEventRegisterEntry>> legacy_gesture_interrupter_handler_map_;
};

#endif  // CORE_RENDER_OHOS_KREVENTDISPATCHCENTER_H

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

#ifndef CORE_RENDER_OHOS_KRBASEEVENTHANDLER_H
#define CORE_RENDER_OHOS_KRBASEEVENTHANDLER_H

#include <arkui/native_node.h>
#include <string>
#include "gesture/KRGestueEventType.h"
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/utils/KREventUtil.h"
#include "libohos_render/view/IKRRenderView.h"

class KRBaseEventHandler : public std::enable_shared_from_this<KRBaseEventHandler> {
 public:
    explicit KRBaseEventHandler(const std::shared_ptr<KRConfig> &kr_config);
    virtual ~KRBaseEventHandler() = default;
    KRBaseEventHandler(const KRBaseEventHandler &) = delete;
    KRBaseEventHandler(const KRBaseEventHandler &&) = delete;
    KRBaseEventHandler &operator=(const KRBaseEventHandler &) = delete;
    KRBaseEventHandler &operator=(const KRBaseEventHandler &&) = delete;

    virtual bool SetProp(const std::shared_ptr<IKRRenderViewExport> &view_export, const std::string &prop_key,
                 const KRAnyValue &prop_value, const KRRenderCallback event_call_back = nullptr);
    virtual bool OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type);
    virtual bool OnCustomEvent(ArkUI_NodeCustomEvent *event, const ArkUI_NodeCustomEventType &event_type);
    virtual bool OnGestureEvent(const std::shared_ptr<KRGestureEventData> &gesture_event_data,
                        const KRGestureEventType &event_type);
    virtual bool ResetProp(const std::string &prop_key);
    virtual void OnDestroy();
    // 是否含有手势事件监听
    virtual bool HasTouchEvent();
    virtual bool HasCaptureRule();

 private:
    bool RegisterOnClick(const std::shared_ptr<IKRRenderViewExport> &view_export,
                         const KRRenderCallback &event_callback);
    bool FireOnClickCallback(const std::shared_ptr<KRGestureEventData> &gesture_event_data);

    bool RegisterOnDoubleClick(const std::shared_ptr<IKRRenderViewExport> &view_export,
                               const KRRenderCallback &event_callback);
    bool FireOnDoubleClickCallback(const std::shared_ptr<KRGestureEventData> &gesture_event_data);

    bool RegisterOnLongPress(const std::shared_ptr<IKRRenderViewExport> &view_export,
                             const KRRenderCallback &event_callback);
    bool FireOnLongPressCallback(const std::shared_ptr<KRGestureEventData> &gesture_event_data);

    bool RegisterOnPan(const std::shared_ptr<IKRRenderViewExport> &view_export, const KRRenderCallback &event_callback);
    bool FireOnPanCallback(const std::shared_ptr<KRGestureEventData> &gesture_event_data);

    bool RegisterOnPinch(const std::shared_ptr<IKRRenderViewExport> &view_export,
                         const KRRenderCallback &event_callback);
    bool FireOnPinchCallback(const std::shared_ptr<KRGestureEventData> &gesture_event_data);
    bool SetCaptureRule(const std::shared_ptr<IKRRenderViewExport> &view_export, const std::string &rule_data);

 private:
    KRRenderCallback click_callback_ = nullptr;
    KRRenderCallback double_click_callback_ = nullptr;
    KRRenderCallback long_press_callback_ = nullptr;
    KRRenderCallback pan_event_callback_ = nullptr;
    KRRenderCallback pinch_event_callback_ = nullptr;
    std::shared_ptr<KRConfig> kr_config_;
    bool has_capture_rule_ = false;
    bool is_long_press_happening = false;
};

class KRArkTSBaseEventHandler : public KRBaseEventHandler {
 public:
    KRArkTSBaseEventHandler(const std::shared_ptr<KRConfig> &kr_config):
    KRBaseEventHandler(kr_config){
            // blank
    }
    bool SetProp(const std::shared_ptr<IKRRenderViewExport> &view_export, const std::string &prop_key,
                 const KRAnyValue &prop_value, const KRRenderCallback event_call_back = nullptr) override {
        return false;
    }
    bool OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override {
        return false;
    }
    bool OnCustomEvent(ArkUI_NodeCustomEvent *event, const ArkUI_NodeCustomEventType &event_type) override {
        return false;
    }
    bool OnGestureEvent(const std::shared_ptr<KRGestureEventData> &gesture_event_data,
                        const KRGestureEventType &event_type) override {
        return false;
    }
    bool ResetProp(const std::string &prop_key) override {
        return false;
    }
    void OnDestroy() override {
        KRBaseEventHandler::OnDestroy();
    }
    // 是否含有手势事件监听
    bool HasTouchEvent() override {
        return false;
    }
    bool HasCaptureRule() override {
        return false;
    }
};
#endif  // CORE_RENDER_OHOS_KRBASEEVENTHANDLER_H

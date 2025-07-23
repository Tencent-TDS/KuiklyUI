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

#ifndef CORE_RENDER_OHOS_KRVIEW_H
#define CORE_RENDER_OHOS_KRVIEW_H

#include "libohos_render/expand/components/view/SuperTouchHandler.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/view/IKRRenderView.h"

class KRView : public IKRRenderViewExport {
 public:
    bool ReuseEnable() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    bool ResetProp(const std::string &prop_key) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;
    void WillReuse() override;
    void DidSetProp(const std::string &prop_key) override;
    void CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &callback) override;
    std::shared_ptr<SuperTouchHandler> GetSuperTouchHandler() { return super_touch_handler_; }

 private:
    void EnsureRegisterTouchEvent();
    bool RegisterTouchDownEvent(const KRRenderCallback &event_call_back);
    bool RegisterTouchMoveEvent(const KRRenderCallback &event_call_back);
    bool RegisterTouchUpEvent(const KRRenderCallback &event_call_back);
	bool SetTargetHitTestMode(const std::string &mode);
    void ProcessTouchEvent(ArkUI_NodeEvent *event);
    void TryFireOnTouchDownEvent(ArkUI_UIInputEvent *input_event);
    void TryFireOnTouchMoveEvent(ArkUI_UIInputEvent *input_event);
    void TryFireOnTouchUpEvent(ArkUI_UIInputEvent *input_event);
    void TryFireOnTouchCancelEvent(ArkUI_UIInputEvent *input_event);
    bool TryFireSuperTouchCancelEvent(ArkUI_UIInputEvent *input_event);
    KRAnyValue GenerateBaseParamsWithTouch(ArkUI_UIInputEvent *input_event, const std::string &action);
    bool HasTouchEvent();
    void UpdateHitTestMode(bool shouldUseTarget);

 private:
    KRRenderCallback touch_down_callback_ = nullptr;
    KRRenderCallback touch_move_callback_ = nullptr;
    KRRenderCallback touch_up_callback_ = nullptr;

    bool register_touch_event_ = false;
    short using_target_hit_test_mode = -1;
    ArkUI_HitTestMode target_hit_test_mode = ARKUI_HIT_TEST_MODE_DEFAULT;
	std::shared_ptr<SuperTouchHandler> super_touch_handler_ = nullptr;
};

#endif  // CORE_RENDER_OHOS_KRVIEW_H

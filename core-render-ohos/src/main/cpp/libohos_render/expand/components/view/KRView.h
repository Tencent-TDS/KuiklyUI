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

#include "libohos_render/expand/components/richtext/KRRichTextView.h"
#include "libohos_render/expand/components/view/SuperTouchHandler.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/foundation/KRPoint.h"
#include "libohos_render/view/IKRRenderView.h"

class KRViewInternalScrollViewObserver;
class KRRenderView;

class KRView : public IKRRenderViewExport {
 public:
    enum SelectableOption {
        INHERIT = 0,
        ENABLE = 1,
        DISABLE = 2
    };

    KRView();
    ~KRView();

    bool ReuseEnable() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    bool ResetProp(const std::string &prop_key) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;
    void WillReuse() override;
    void DidSetProp(const std::string &prop_key) override;
    void CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &callback) override;
    void WillRemoveFromParentView() override;
    std::shared_ptr<SuperTouchHandler> GetSuperTouchHandler() { return super_touch_handler_; }

 protected:
    bool IsSelectable() override;

 private:
    void StopObservingInternalScrollViews();
    void CalculateHandleFramesAndDoUpdate();
    void OnInternalScrollViewDidScroll(float offsetX, float offsetY);
    void EnsureRegisterTouchEvent();
    bool RegisterTouchDownEvent(const KRRenderCallback &event_call_back);
    bool RegisterTouchMoveEvent(const KRRenderCallback &event_call_back);
    bool RegisterTouchUpEvent(const KRRenderCallback &event_call_back);
	bool SetTargetHitTestMode(const std::string &mode);
    void ProcessTouchEvent(ArkUI_NodeEvent *event);
    bool TryFireOnTouchDownEvent(ArkUI_UIInputEvent *input_event);
    bool TryFireOnTouchMoveEvent(ArkUI_UIInputEvent *input_event);
    bool TryFireOnTouchUpEvent(ArkUI_UIInputEvent *input_event);
    bool TryFireOnTouchCancelEvent(ArkUI_UIInputEvent *input_event);
    bool TryFireSuperTouchCancelEvent(ArkUI_UIInputEvent *input_event);
    KRAnyValue GenerateBaseParamsWithTouch(ArkUI_UIInputEvent *input_event, const std::string &action);
    bool HasTouchEvent();
    void UpdateHitTestMode(bool shouldUseTarget);
    void EnsureSuperTouchType();

    bool HandleTextSelectionMethods(const std::string &method, const KRAnyValue &params, const KRRenderCallback &cb);
    void HandleCreateSelection(const KRAnyValue &params);
    void HandleGetSelection(const KRAnyValue &params, const KRRenderCallback &cb);
    void HandleClearSelection();
    void HandleCreateSelectionAll();

    void CreateSelection(KRPoint point, KRPoint point2, int type);

    std::vector<std::shared_ptr<KRRichTextView>> GetSelectedNodes(KRPoint p0, KRPoint p1);
    void GetSelectedNodes(std::shared_ptr<KRRenderView> root_render_view,
                          std::vector<std::shared_ptr<KRRichTextView>> &result, ArkUI_NodeHandle node,
                          const KRRect &selection_area);
    void GetSubTextNodes(std::shared_ptr<KRRenderView> root_render_view,
                         std::vector<std::shared_ptr<KRRichTextView>> &result, ArkUI_NodeHandle node);

    std::vector<std::shared_ptr<KRRichTextView>> GetTextViewsBetweenPoints(KRPoint p0, KRPoint p1);
    void UpdateSelectionHandles();
    void CleanupHandleNodes();
    static void OnSelectorPanGestureEventCB(ArkUI_GestureEvent *event, void *extraParams);
    void OnSelectorPanGestureEvent(ArkUI_GestureEvent *event, void *extraParams);
    KRRect GetSelectionFrame();
    enum SelectionEventKind { START, CHANGE, END, CANCEL };
    void FireSelectionEvent(SelectionEventKind kind);

 private:
    enum SuperTouchType {
        UNKNOWN,
        NONE,
        SELF,
        PARENT
    };

    KRRenderCallback touch_down_callback_ = nullptr;
    KRRenderCallback touch_move_callback_ = nullptr;
    KRRenderCallback touch_up_callback_ = nullptr;

    bool register_touch_event_ = false;
    short using_target_hit_test_mode = -1;
    ArkUI_HitTestMode target_hit_test_mode = ARKUI_HIT_TEST_MODE_DEFAULT;
    std::shared_ptr<SuperTouchHandler> super_touch_handler_ = nullptr;
    std::weak_ptr<SuperTouchHandler> parent_super_touch_handler_;
    SuperTouchType super_touch_type_ = UNKNOWN;
    bool stop_propagation_ = false;

    SelectableOption selectable_option_ = SelectableOption::ENABLE;

    KRRenderCallback select_start_callback_ = nullptr;
    KRRenderCallback select_change_callback_ = nullptr;
    KRRenderCallback select_end_callback_ = nullptr;
    KRRenderCallback select_cancel_callback_ = nullptr;

    struct {
        KRRect start;
        KRRect end;
        bool visible = false;
        KRPoint panning_start_point_in_window;
        KRPoint panning_selection_point_when_start;
        bool is_panning = false;
        KRPoint selection_points[2];
        struct {
            ArkUI_NodeHandle wrapper = nullptr;
            ArkUI_NodeHandle head = nullptr;
            ArkUI_NodeHandle body = nullptr;
            ArkUI_GestureRecognizer *text_selection_pan_recognizer = nullptr;
        } handle_nodes[2];
        bool sent_start_event = false;
    } selection_info_;

    std::set<std::shared_ptr<IKRRenderViewExport>> last_selected_text_views_;

    friend class KRViewInternalScrollViewObserver;
    KRViewInternalScrollViewObserver *scroll_observer_ = nullptr;
};

#endif  // CORE_RENDER_OHOS_KRVIEW_H

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

#include "libohos_render/expand/components/view/KRView.h"

#include <algorithm>
#include "libohos_render/expand/components/richtext/KRRichTextView.h"
#include "libohos_render/expand/components/scroller/KRScrollerView.h"
#include "libohos_render/foundation/KRBorderRadiuses.h"
#include "libohos_render/foundation/KRConfig.h"
#include "libohos_render/foundation/type/KRRenderValue.h"
#include "libohos_render/manager/KRSnapshotManager.h"
#include "libohos_render/utils/KRJSONObject.h"
#include "libohos_render/utils/KRViewUtil.h"
#include "libohos_render/view/KRRenderView.h"

#define NS_PER_MS 1000000

static const char *KRTextSelectionMethodCreateSelection = "createSelection";
static const char *KRTextSelectionMethodGetSelection = "getSelection";
static const char *KRTextSelectionMethodClearSelection = "clearSelection";
static const char *KRTextSelectionMethodCreateSelectionAll = "createSelectionAll";
static const char *kTextSelectable = "selectable";
static const char *kTextSelectStart = "selectStart";
static const char *kTextSelectEnd = "selectEnd";
static const char *kTextSelectChange = "selectChange";
static const char *kTextSelectCancel = "selectCancel";
static constexpr int SelectionCursorWidth = 2;

class KRViewInternalScrollViewObserver : public IKRScrollObserver {
 public:
    explicit KRViewInternalScrollViewObserver(KRView *view) : view_(view) {}
    ~KRViewInternalScrollViewObserver() { view_ = nullptr; }

    void OnDidScroll(float offsetX, float offsetY) override {
        if (view_) {
            view_->OnInternalScrollViewDidScroll(offsetX, offsetY);
        }
    }

    KRView *view_ = nullptr;
};

constexpr char kPropNameTouchDown[] = "touchDown";
constexpr char kPropNameTouchMove[] = "touchMove";
constexpr char kPropNameTouchUp[] = "touchUp";
constexpr char kPropNameTouchCancel[] = "touchCancel";
constexpr char kPropNamePreventTouch[] = "preventTouch";
constexpr char kPropNameSuperTouch[] = "superTouch";
constexpr char kPropNameHitTestModeOhos[] = "hit-test-ohos";
constexpr char kPropNameStopPropagation[] = "stop-propagation-ohos";

constexpr char kOhosHitTestModeDefault[] = "default";
constexpr char kOhosHitTestModeBlock[] = "block";
constexpr char kOhosHitTestModeNone[] = "none";
constexpr char kOhosHitTestModeTransparent[] = "transparent";


KRView::KRView() {
    scroll_observer_ = new KRViewInternalScrollViewObserver(this);
}

KRView::~KRView() {
    StopObservingInternalScrollViews();
    CleanupHandleNodes();
    if (scroll_observer_) {
        delete scroll_observer_;
        scroll_observer_ = nullptr;
    }
}

void KRView::OnInternalScrollViewDidScroll(float offsetX, float offsetY) {
    (void)offsetX;
    (void)offsetY;
    CalculateHandleFramesAndDoUpdate();
}

void KRView::StopObservingInternalScrollViews() {
    for (auto view : GetInternalScrollViews()) {
        auto scroll_view = std::dynamic_pointer_cast<KRScrollerView>(view);
        if (scroll_view && scroll_observer_) {
            scroll_view->RemoveScrollObserver(scroll_observer_);
        }
    }
}

bool KRView::ReuseEnable() {
    return true;
}

void KRView::WillReuse() {
    UpdateHitTestMode(true);
}

bool KRView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                     const KRRenderCallback event_call_back) {
    auto didHand = false;
    if (kuikly::util::isEqual(prop_key, kPropNameTouchDown)) {
        didHand = RegisterTouchDownEvent(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kPropNameTouchMove)) {
        didHand = RegisterTouchMoveEvent(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kPropNameTouchUp)) {
        didHand = RegisterTouchUpEvent(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kPropNamePreventTouch)) {
        if (super_touch_handler_) {
            super_touch_handler_->PreventTouch(prop_value->toBool());
        }
        didHand = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameSuperTouch)) {
        if (prop_value->toBool()) {
            if (!super_touch_handler_) {
                super_touch_handler_ = std::make_shared<SuperTouchHandler>();
            }
        } else {
            if (super_touch_handler_) {
                super_touch_handler_ = nullptr;
            }
        }
        didHand = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameHitTestModeOhos)) {
        didHand = SetTargetHitTestMode(prop_value->toString());
    } else if (kuikly::util::isEqual(prop_key, kPropNameStopPropagation)) {
        stop_propagation_ = prop_value->toBool();
        didHand = true;
    } else if (kuikly::util::isEqual(prop_key, kTextSelectable)) {
        selectable_option_ = static_cast<SelectableOption>(prop_value->toInt());
        didHand = true;
    } else if (kuikly::util::isEqual(prop_key, kTextSelectStart)) {
        select_start_callback_ = event_call_back;
        didHand = true;
    } else if (kuikly::util::isEqual(prop_key, kTextSelectEnd)) {
        select_end_callback_ = event_call_back;
        didHand = true;
    } else if (kuikly::util::isEqual(prop_key, kTextSelectChange)) {
        select_change_callback_ = event_call_back;
        didHand = true;
    } else if (kuikly::util::isEqual(prop_key, kTextSelectCancel)) {
        select_cancel_callback_ = event_call_back;
        didHand = true;
    }
    return didHand;
}

void KRView::DidSetProp(const std::string &prop_key) {
    UpdateHitTestMode(HasBaseEvent() || HasTouchEvent());
}

void KRView::CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &cb) {
    if (HandleTextSelectionMethods(method, params, cb)) {
        return;
    }
    IKRRenderViewExport::CallMethod(method, params, cb);
}

bool KRView::IsSelectable() {
    return selectable_option_ != DISABLE;
}

void KRView::HandleCreateSelection(const KRAnyValue &params) {
    std::string str = params->toString();
    if (auto paramObj = kuikly::util::JSONObject::Parse(str)) {
        static constexpr double INVALID_NUMBER = -10000;
        double x = paramObj->GetNumber("x", INVALID_NUMBER);
        double y = paramObj->GetNumber("y", INVALID_NUMBER);
        int type = static_cast<int>(paramObj->GetNumber("type", INVALID_NUMBER));

        if (x != INVALID_NUMBER && y != INVALID_NUMBER && type != INVALID_NUMBER) {
            CreateSelection(KRPoint(static_cast<float>(x), static_cast<float>(y)),
                           KRPoint(static_cast<float>(x), static_cast<float>(y)), type);
        }
    }
}

void KRView::CalculateHandleFramesAndDoUpdate() {
    auto [selected_text_views, selected_scroll_views] = GetSelectedTextAndScrollViews();
    if (selected_text_views.empty()) {
        return;
    }

    std::set<std::shared_ptr<IKRRenderViewExport>> selected_text_view_set(selected_text_views.begin(),
                                                                           selected_text_views.end());
    for (auto item : last_selected_text_views_) {
        if (selected_text_view_set.find(item) == selected_text_view_set.end()) {
            item->ClearSelection();
        }
    }

    for (auto item : selected_scroll_views) {
        if (!item->IsScrollView()) {
            continue;
        }
        auto scroll_view = std::dynamic_pointer_cast<KRScrollerView>(item);
        if (scroll_view && scroll_observer_) {
            scroll_view->AddScrollObserver(scroll_observer_);
        }
    }

    last_selected_text_views_ = std::move(selected_text_view_set);

    float first_char_width = 0;
    float last_char_width = 0;
    KRRect first_selection_rect2(100000, 100000, 0, 0);
    KRRect last_selection_rect2(-1, -1, 0, 0);
    {
        KRRect first_rect =
            std::dynamic_pointer_cast<KRRichTextView>(selected_text_views.front())->FirstSelectionRect();
        KRRect last_rect =
            std::dynamic_pointer_cast<KRRichTextView>(selected_text_views.back())->LastSelectionRect();

        KRPoint first_origin =
            ConvertPointToAncestorCoordinate(first_rect.Origin(), selected_text_views.front()->GetNode(), GetNode());
        first_rect.x = first_origin.x;
        first_rect.y = first_origin.y;
        first_char_width =
            std::dynamic_pointer_cast<KRRichTextView>(selected_text_views.front())->GetSelectionInfo().first_char_width;

        KRPoint last_origin =
            ConvertPointToAncestorCoordinate(last_rect.Origin(), selected_text_views.back()->GetNode(), GetNode());
        last_rect.x = last_origin.x;
        last_rect.y = last_origin.y;
        first_selection_rect2 = first_rect;
        last_selection_rect2 = last_rect;
        last_char_width =
            std::dynamic_pointer_cast<KRRichTextView>(selected_text_views.back())->GetSelectionInfo().last_char_width;
    }

    selection_info_.start =
        KRRect(first_selection_rect2.x, first_selection_rect2.y, SelectionCursorWidth, first_selection_rect2.height);
    selection_info_.end = KRRect(last_selection_rect2.x + last_selection_rect2.width, last_selection_rect2.y,
                                 SelectionCursorWidth, last_selection_rect2.height);
    selection_info_.selection_points[0] =
        KRPoint(selection_info_.start.x + first_char_width / 2, selection_info_.start.y + selection_info_.start.height / 2);
    selection_info_.selection_points[1] =
        KRPoint(selection_info_.end.x - last_char_width / 2, selection_info_.end.y + selection_info_.end.height / 2);

    if (!selection_info_.sent_start_event) {
        FireSelectionEvent(SelectionEventKind::START);
        selection_info_.sent_start_event = true;
    } else {
        FireSelectionEvent(SelectionEventKind::CHANGE);
    }
    selection_info_.visible = true;

    UpdateSelectionHandles();
}

void KRView::CreateSelection(KRPoint p0, KRPoint p1, int type) {
    UpdateSelection(shared_from_this(), p0, p1, type);
    CalculateHandleFramesAndDoUpdate();
}

void KRView::HandleGetSelection(const KRAnyValue &params, const KRRenderCallback &cb) {
    (void)params;
    if (!cb) {
        return;
    }
    auto irender_view = GetRootView().lock();
    if (irender_view == nullptr) {
        return;
    }
    auto root_render_view = std::dynamic_pointer_cast<KRRenderView>(irender_view);
    if (root_render_view == nullptr) {
        return;
    }
    std::vector<std::shared_ptr<KRRichTextView>> result;
    GetSubTextNodes(root_render_view, result, GetNode());
    float container_width = GetBounds().width;
    std::sort(result.begin(), result.end(),
              [this, container_width](const std::shared_ptr<IKRRenderViewExport> &a,
                                     const std::shared_ptr<IKRRenderViewExport> &b) {
                  auto frame1 = GetSubnodeFrame(a->GetNode());
                  auto frame2 = GetSubnodeFrame(b->GetNode());
                  return frame1.x + frame1.y * container_width < frame2.x + frame2.y * container_width;
              });

    std::shared_ptr<KRRichTextView> pre;
    std::vector<std::shared_ptr<KRRichTextView>> selected_text_views;
    std::shared_ptr<KRRichTextView> post;
    for (auto text_view : result) {
        if (last_selected_text_views_.find(text_view) != last_selected_text_views_.end()) {
            selected_text_views.push_back(text_view);
        } else {
            if (selected_text_views.size() > 0 && post == nullptr) {
                post = text_view;
            }
            if (selected_text_views.size() == 0) {
                pre = text_view;
            }
        }
    }

    KRRenderValueArray preContent;
    bool preContentInserted = false;
    KRRenderValueArray content;
    KRRenderValueArray postContent;
    bool postContentInserted = false;

    if (pre && !pre->GetTextContent().empty()) {
        preContent.push_back(NewKRRenderValue(pre->GetTextContent()));
    }

    if (post && !post->GetTextContent().empty()) {
        postContent.push_back(NewKRRenderValue(post->GetTextContent()));
    }
    for (auto item : selected_text_views) {
        std::string preText;
        std::string postText;
        content.push_back(NewKRRenderValue(item->GetSelectedContent(preText, postText)));

        if (!preContentInserted && !preText.empty()) {
            preContent.push_back(NewKRRenderValue(preText));
            preContentInserted = true;
        }

        if (!postContentInserted && !postText.empty()) {
            postContent.insert(postContent.begin(), NewKRRenderValue(postText));
            postContentInserted = true;
        }
    }

    if (content.size() == 0) {
        preContent.clear();
        postContent.clear();
    }

    KRRenderValueMap dict;
    dict["preContent"] = NewKRRenderValue(preContent);
    dict["content"] = NewKRRenderValue(content);
    dict["postContent"] = NewKRRenderValue(postContent);
    cb(NewKRRenderValue(dict));
}

void KRView::HandleClearSelection() {
    FireSelectionEvent(SelectionEventKind::CANCEL);

    selection_info_.visible = false;
    for (auto item : last_selected_text_views_) {
        item->ClearSelection();
    }
    UpdateSelectionHandles();
}

void KRView::HandleCreateSelectionAll() {
    KRRect bounds = GetBounds();
    CreateSelection(KRPoint(), KRPoint(bounds.width, bounds.height), KRTextSelectionType::ALL);
}

bool KRView::HandleTextSelectionMethods(const std::string &method, const KRAnyValue &params,
                                        const KRRenderCallback &cb) {
    if (method == KRTextSelectionMethodCreateSelection) {
        HandleCreateSelection(params);
    } else if (method == KRTextSelectionMethodGetSelection) {
        HandleGetSelection(params, cb);
    } else if (method == KRTextSelectionMethodClearSelection) {
        HandleClearSelection();
    } else if (method == KRTextSelectionMethodCreateSelectionAll) {
        HandleCreateSelectionAll();
    } else {
        return false;
    }
    return true;
}

void KRView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    if (event_type == NODE_TOUCH_EVENT) {
        ProcessTouchEvent(event);
    }
}

bool KRView::ResetProp(const std::string &prop_key) {
    auto didHande = false;
    register_touch_event_ = false;
    if (kuikly::util::isEqual(prop_key, kPropNameTouchDown)) {
        touch_down_callback_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameTouchMove)) {
        touch_move_callback_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameTouchUp)) {
        touch_up_callback_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNamePreventTouch)) {
        // reset handled by kPropNameSuperTouch, do nothing here
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameSuperTouch)) {
        super_touch_handler_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameHitTestModeOhos)) {
        target_hit_test_mode = ARKUI_HIT_TEST_MODE_DEFAULT;
        UpdateHitTestMode(HasBaseEvent() || HasTouchEvent());
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameStopPropagation)) {
        stop_propagation_ = false;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kTextSelectable)) {
        selectable_option_ = ENABLE;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kTextSelectStart)) {
        select_start_callback_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kTextSelectEnd)) {
        select_end_callback_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kTextSelectChange)) {
        select_change_callback_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kTextSelectCancel)) {
        select_cancel_callback_ = nullptr;
        didHande = true;
    } else {
        didHande = IKRRenderViewExport::ResetProp(prop_key);
    }
    return didHande;
}

void KRView::ProcessTouchEvent(ArkUI_NodeEvent *event) {
    auto input_event = kuikly::util::GetArkUIInputEvent(event);
    TryFireSuperTouchCancelEvent(input_event);
    auto action = kuikly::util::GetArkUIInputEventAction(input_event);
    EnsureSuperTouchType();
    if (super_touch_type_ == PARENT) {
        auto parent_super_touch_handler = parent_super_touch_handler_.lock();
        if (parent_super_touch_handler && parent_super_touch_handler->GetStopPropagation(action)) {
            return;
        }
    }
    bool handled = false;
    if (action == UI_TOUCH_EVENT_ACTION_DOWN) {
        handled = TryFireOnTouchDownEvent(input_event);
    } else if (action == UI_TOUCH_EVENT_ACTION_MOVE) {
        handled = TryFireOnTouchMoveEvent(input_event);
    } else if (action == UI_TOUCH_EVENT_ACTION_UP) {
        handled = TryFireOnTouchUpEvent(input_event);
    } else if (action == UI_TOUCH_EVENT_ACTION_CANCEL) {
        handled = TryFireOnTouchCancelEvent(input_event);
    }
    if (super_touch_type_ == SELF) {
        if (super_touch_handler_->GetStopPropagation(action)) {
            kuikly::util::StopPropagation(event);
            super_touch_handler_->SetStopPropagation(action, false);
        }
    } else if (handled) {
        if (stop_propagation_) {
            if (super_touch_type_ == PARENT) {
                auto parent_super_touch_handler = parent_super_touch_handler_.lock();
                if (parent_super_touch_handler) {
                    parent_super_touch_handler->SetStopPropagation(action, true);
                }
            } else if (super_touch_type_ == NONE) {
                kuikly::util::StopPropagation(event);
            }
        }
    }
}

void KRView::EnsureRegisterTouchEvent() {
    if (register_touch_event_) {
        return;
    }

    RegisterEvent(NODE_TOUCH_EVENT);
    register_touch_event_ = true;
}

bool KRView::RegisterTouchDownEvent(const KRRenderCallback &event_call_back) {
    EnsureRegisterTouchEvent();
    touch_down_callback_ = event_call_back;
    return true;
}

bool KRView::RegisterTouchMoveEvent(const KRRenderCallback &event_call_back) {
    EnsureRegisterTouchEvent();
    touch_move_callback_ = event_call_back;
    return true;
}

bool KRView::RegisterTouchUpEvent(const KRRenderCallback &event_call_back) {
    EnsureRegisterTouchEvent();
    touch_up_callback_ = event_call_back;
    return true;
}

bool KRView::SetTargetHitTestMode(const std::string &mode) {
    if (kuikly::util::isEqual(mode, kOhosHitTestModeBlock)) {
        target_hit_test_mode = ARKUI_HIT_TEST_MODE_BLOCK;
    } else if (kuikly::util::isEqual(mode, kOhosHitTestModeNone)) {
        target_hit_test_mode = ARKUI_HIT_TEST_MODE_NONE;
    } else if (kuikly::util::isEqual(mode, kOhosHitTestModeTransparent)) {
        target_hit_test_mode = ARKUI_HIT_TEST_MODE_TRANSPARENT;
    } else if (kuikly::util::isEqual(mode, kOhosHitTestModeDefault)) {
        target_hit_test_mode = ARKUI_HIT_TEST_MODE_DEFAULT;
    }
    return true;
}

bool KRView::TryFireOnTouchDownEvent(ArkUI_UIInputEvent *input_event) {
    if (!touch_down_callback_) {
        return false;
    }
    touch_down_callback_(GenerateBaseParamsWithTouch(input_event, kPropNameTouchDown));
    return true;
}

bool KRView::TryFireOnTouchMoveEvent(ArkUI_UIInputEvent *input_event) {
    if (!touch_move_callback_) {
        return false;
    }
    touch_move_callback_(GenerateBaseParamsWithTouch(input_event, kPropNameTouchMove));
    return true;
}

bool KRView::TryFireOnTouchUpEvent(ArkUI_UIInputEvent *input_event) {
    if (!touch_up_callback_) {
        return false;
    }
    touch_up_callback_(GenerateBaseParamsWithTouch(input_event, kPropNameTouchUp));
    return true;
}

bool KRView::TryFireOnTouchCancelEvent(ArkUI_UIInputEvent *input_event) {
    if (!touch_up_callback_) {
        return false;
    }
    touch_up_callback_(GenerateBaseParamsWithTouch(input_event, kPropNameTouchCancel));
    return true;
}

bool KRView::TryFireSuperTouchCancelEvent(ArkUI_UIInputEvent *input_event) {
    if (!super_touch_handler_) {
        return false;
    }
    auto action = kuikly::util::GetArkUIInputEventAction(input_event);
    auto pointer_count = kuikly::util::GetArkUIInputEventPointerCount(input_event);
    if (action == UI_TOUCH_EVENT_ACTION_DOWN && pointer_count == 1) {
        super_touch_handler_->ResetCancel();
        return false;
    }
    bool canceled = false;
    if (super_touch_handler_->IsCanceled()) {
        canceled = true;
    } else if (super_touch_handler_->ProcessCancel()) {
        canceled = true;
    }
    if ((action == UI_TOUCH_EVENT_ACTION_UP && pointer_count == 1) || action == UI_TOUCH_EVENT_ACTION_CANCEL) {
        super_touch_handler_->ResetCancel();
    }
    return canceled;
}

KRAnyValue KRView::GenerateBaseParamsWithTouch(ArkUI_UIInputEvent *input_event, const std::string &action) {
    if (!input_event) {
        return KREmptyValue();
    }

    auto pointer_count = kuikly::util::GetArkUIInputEventPointerCount(input_event);
    if (pointer_count <= 0) {
        return KREmptyValue();
    }

    KRPoint container_position{0.0f, 0.0f};

    if (auto root_view = GetRootView().lock()) {
        container_position = root_view->GetRootNodePositionInWindow();
    }

    KRRenderValueArray touches;
    for (int i = 0; i < pointer_count; i++) {
        auto point = kuikly::util::GetArkUIInputEventPoint(input_event, i);
        auto window_point = kuikly::util::GetArkUIInputEventWindowPoint(input_event, i);

        float container_relative_x = window_point.x - container_position.x;
        float container_relative_y = window_point.y - container_position.y;

        KRRenderValueMap touch_map;
        touch_map["x"] = NewKRRenderValue(point.x);
        touch_map["y"] = NewKRRenderValue(point.y);
        touch_map["pageX"] = NewKRRenderValue(container_relative_x);
        touch_map["pageY"] = NewKRRenderValue(container_relative_y);
        touch_map["pointerId"] = NewKRRenderValue(OH_ArkUI_PointerEvent_GetPointerId(input_event, i));
        touches.push_back(NewKRRenderValue(touch_map));
    }
    auto first_touch = touches[0]->toMap();
    first_touch["touches"] = NewKRRenderValue(touches);
    first_touch["action"] = NewKRRenderValue(action);
    auto event_time_millis = kuikly::util::GetArkUIInputEventTime(input_event) / NS_PER_MS;
    first_touch["timestamp"] = NewKRRenderValue(event_time_millis);
    if (super_touch_handler_) {
        first_touch["consumed"] = NewKRRenderValue(super_touch_handler_->IsCanceled() ? 1 : 0);
    }
    return NewKRRenderValue(first_touch);
}

bool KRView::HasTouchEvent() {
    return touch_up_callback_ != nullptr || touch_down_callback_ != nullptr || touch_move_callback_ != nullptr;
}

void KRView::UpdateHitTestMode(bool shouldUseTarget) {
    if (using_target_hit_test_mode != (shouldUseTarget ? 1 : 0)) {
        using_target_hit_test_mode = shouldUseTarget;
        kuikly::util::UpdateNodeHitTestMode(GetNode(), shouldUseTarget ? target_hit_test_mode : ARKUI_HIT_TEST_MODE_NONE);
    }
}

void KRView::WillRemoveFromParentView() {
    IKRRenderViewExport::WillRemoveFromParentView();
    parent_super_touch_handler_.reset();
    super_touch_type_ = UNKNOWN;
}

std::vector<std::shared_ptr<KRRichTextView>> KRView::GetSelectedNodes(KRPoint p0, KRPoint p1) {
    std::vector<std::shared_ptr<KRRichTextView>> result;
    auto irender_view = GetRootView().lock();
    if (irender_view == nullptr) {
        return result;
    }
    auto root_render_view = std::dynamic_pointer_cast<KRRenderView>(irender_view);
    if (root_render_view == nullptr) {
        return result;
    }

    KRRect selection_area(std::min(p0.x, p1.x), std::min(p0.y, p1.y),
                          std::fabs(p0.x - p1.x) > 0 ? std::fabs(p0.x - p1.x) : 1,
                          std::fabs(p0.y - p1.y) > 0 ? std::fabs(p0.y - p1.y) : 1);

    GetSelectedNodes(root_render_view, result, GetNode(), selection_area);
    return result;
}

static bool IsViewFrameIntersectWithSelectionArea(const KRRect &view_frame, const KRRect &selection_area) {
    if (view_frame.IsIntersect(selection_area)) {
        return true;
    }
    if (selection_area.Origin() < view_frame.Origin() &&
        selection_area.y + selection_area.height > view_frame.y + view_frame.height) {
        return true;
    }

    if (selection_area.Origin() < view_frame.Origin()) {
        KRRect r(view_frame.x, selection_area.y, selection_area.x + selection_area.width - view_frame.x,
                 selection_area.height);
        return view_frame.IsIntersect(r);
    }

    return false;
}

void KRView::GetSubTextNodes(std::shared_ptr<KRRenderView> root_render_view,
                             std::vector<std::shared_ptr<KRRichTextView>> &result, ArkUI_NodeHandle node) {
    auto nodeApi = kuikly::util::GetNodeApi();
    for (uint32_t i = 0; i < nodeApi->getTotalChildCount(node); ++i) {
        ArkUI_NodeHandle child = nodeApi->getChildAt(node, i);
        auto child_view = root_render_view->GetView(child);
        if (child_view == nullptr) {
            continue;
        }
        if (child_view->GetViewName() == GetViewName()) {
            auto kv = std::dynamic_pointer_cast<KRView>(child_view);
            if (kv && !kv->IsSelectable()) {
                continue;
            }
        }
        if (child_view->GetViewName() == "KRGradientRichTextView" || child_view->GetViewName() == "KRRichTextView") {
            result.push_back(std::dynamic_pointer_cast<KRRichTextView>(child_view));
        } else {
            GetSubTextNodes(root_render_view, result, child);
        }
    }
}

void KRView::GetSelectedNodes(std::shared_ptr<KRRenderView> root_render_view,
                              std::vector<std::shared_ptr<KRRichTextView>> &result, ArkUI_NodeHandle node,
                              const KRRect &selection_area) {
    auto nodeApi = kuikly::util::GetNodeApi();
    KRRect bounds = GetSubnodeFrame(node);
    if (!IsViewFrameIntersectWithSelectionArea(bounds, selection_area)) {
        return;
    }
    auto view = root_render_view->GetView(node);
    if (view && (view->GetViewName() == "KRGradientRichTextView" || view->GetViewName() == "KRRichTextView")) {
        result.push_back(std::dynamic_pointer_cast<KRRichTextView>(view));
    }

    for (uint32_t i = 0; i < nodeApi->getTotalChildCount(node); ++i) {
        ArkUI_NodeHandle child = nodeApi->getChildAt(node, i);

        KRRect child_bounds = GetSubnodeFrame(child);
        auto child_view = root_render_view->GetView(child);
        if (child_view == nullptr) {
            continue;
        }
        if (child_view->GetViewName() == GetViewName()) {
            auto kv = std::dynamic_pointer_cast<KRView>(child_view);
            if (kv && !kv->IsSelectable()) {
                continue;
            }
        }

        if (IsViewFrameIntersectWithSelectionArea(child_bounds, selection_area)) {
            GetSelectedNodes(root_render_view, result, child, selection_area);
        }
    }
}

std::vector<std::shared_ptr<KRRichTextView>> KRView::GetTextViewsBetweenPoints(KRPoint p0, KRPoint p1) {
    return GetSelectedNodes(p0, p1);
}

void KRView::CleanupHandleNodes() {
    if (selection_info_.handle_nodes[0].wrapper) {
        auto gesture_api = kuikly::util::GetGestureApi();
        for (size_t i = 0; i < sizeof(selection_info_.handle_nodes) / sizeof(selection_info_.handle_nodes[0]); ++i) {
            auto handles = &selection_info_.handle_nodes[i];
            if (handles->text_selection_pan_recognizer) {
                gesture_api->removeGestureFromNode(handles->wrapper, handles->text_selection_pan_recognizer);
                gesture_api->dispose(handles->text_selection_pan_recognizer);
                handles->text_selection_pan_recognizer = nullptr;
            }
        }
    }
    selection_info_ = {};
}

void KRView::UpdateSelectionHandles() {
    constexpr int kSelectorWidth = 20;
    constexpr int kSelectorCapWidth = 12;
    constexpr int kSelectorColor = 0xFF0000FF;
    if (selection_info_.handle_nodes[0].wrapper == nullptr) {
        auto nodeApi = kuikly::util::GetNodeApi();
        auto gesture_api = kuikly::util::GetGestureApi();
        for (size_t i = 0; i < sizeof(selection_info_.handle_nodes) / sizeof(selection_info_.handle_nodes[0]); ++i) {
            auto handles = &selection_info_.handle_nodes[i];

            ArkUI_NodeHandle wrapper = nodeApi->createNode(ARKUI_NODE_STACK);
            ArkUI_NodeHandle head = nodeApi->createNode(ARKUI_NODE_STACK);
            ArkUI_NodeHandle body = nodeApi->createNode(ARKUI_NODE_STACK);

            kuikly::util::UpdateNodeBorderRadius(head,
                                                 KRBorderRadiuses(kSelectorWidth / 2, kSelectorWidth / 2,
                                                                  kSelectorWidth / 2, kSelectorWidth / 2));
            kuikly::util::UpdateNodeBackgroundColor(head, kSelectorColor);
            kuikly::util::UpdateNodeBackgroundColor(body, kSelectorColor);

            kuikly::util::UpdateNodeVisibility(wrapper, 0);
            nodeApi->addChild(wrapper, head);
            nodeApi->addChild(wrapper, body);
            nodeApi->addChild(GetNode(), wrapper);

            handles->text_selection_pan_recognizer = gesture_api->createPanGesture(1, GESTURE_DIRECTION_ALL, 5);
            gesture_api->setGestureEventTarget(handles->text_selection_pan_recognizer,
                                               GESTURE_EVENT_ACTION_ACCEPT | GESTURE_EVENT_ACTION_UPDATE |
                                                   GESTURE_EVENT_ACTION_END,
                                               this, KRView::OnSelectorPanGestureEventCB);
            gesture_api->addGestureToNode(wrapper, handles->text_selection_pan_recognizer, NORMAL, NORMAL_GESTURE_MASK);

            handles->wrapper = wrapper;
            handles->head = head;
            handles->body = body;
        }
    }

    if (selection_info_.visible) {
        constexpr int kSelectorWidth = 20;
        constexpr int kSelectorCapWidth = 12;
        KRRect wrapper_rect(selection_info_.start.x - kSelectorWidth / 2,
                            selection_info_.start.y - kSelectorCapWidth, kSelectorWidth,
                            selection_info_.start.height + kSelectorCapWidth);
        KRRect head_rect((kSelectorWidth - kSelectorCapWidth) / 2, 0, kSelectorCapWidth, kSelectorCapWidth);
        KRRect body_rect((kSelectorWidth - selection_info_.start.width) / 2, kSelectorCapWidth,
                         selection_info_.start.width, selection_info_.start.height);
        kuikly::util::UpdateNodeFrame(selection_info_.handle_nodes[0].wrapper, wrapper_rect);
        kuikly::util::UpdateNodeFrame(selection_info_.handle_nodes[0].head, head_rect);
        kuikly::util::UpdateNodeFrame(selection_info_.handle_nodes[0].body, body_rect);
        kuikly::util::UpdateNodeVisibility(selection_info_.handle_nodes[0].wrapper, 1);

        KRRect end_wrapper_rect(selection_info_.end.x - kSelectorWidth / 2, selection_info_.end.y, kSelectorWidth,
                                selection_info_.end.height + kSelectorCapWidth);
        KRRect end_head_rect((kSelectorWidth - kSelectorCapWidth) / 2, selection_info_.end.height, kSelectorCapWidth,
                             kSelectorCapWidth);
        KRRect end_body_rect((kSelectorWidth - selection_info_.end.width) / 2, 0, selection_info_.end.width,
                             selection_info_.end.height);
        kuikly::util::UpdateNodeFrame(selection_info_.handle_nodes[1].wrapper, end_wrapper_rect);
        kuikly::util::UpdateNodeFrame(selection_info_.handle_nodes[1].head, end_head_rect);
        kuikly::util::UpdateNodeFrame(selection_info_.handle_nodes[1].body, end_body_rect);
        kuikly::util::UpdateNodeVisibility(selection_info_.handle_nodes[1].wrapper, 1);
    } else {
        kuikly::util::UpdateNodeVisibility(selection_info_.handle_nodes[0].wrapper, 0);
        kuikly::util::UpdateNodeVisibility(selection_info_.handle_nodes[1].wrapper, 0);
    }
}

KRRect KRView::GetSelectionFrame() {
    KRPoint start_point = selection_info_.start.Origin();
    KRPoint end_point = selection_info_.end.Origin();
    if (start_point < end_point) {
        return KRRect(start_point.x, start_point.y, std::fabs(selection_info_.end.x - selection_info_.start.x),
                      std::fabs(selection_info_.end.y - selection_info_.start.y) + selection_info_.end.height);
    } else {
        return KRRect(end_point.x, end_point.y, std::fabs(selection_info_.start.x - selection_info_.end.x),
                      std::fabs(selection_info_.start.y - selection_info_.end.y) + selection_info_.start.height);
    }
}

void KRView::FireSelectionEvent(SelectionEventKind kind) {
    KRRect rect = GetSelectionFrame();

    KRRenderValueMap dict;
    dict["x"] = NewKRRenderValue(rect.x);
    dict["y"] = NewKRRenderValue(rect.y);
    dict["width"] = NewKRRenderValue(rect.width);
    dict["height"] = NewKRRenderValue(rect.height);
    auto param = NewKRRenderValue(dict);

    switch (kind) {
    case SelectionEventKind::START:
        if (select_start_callback_) {
            select_start_callback_(param);
        }
        break;
    case SelectionEventKind::CHANGE:
        if (select_change_callback_) {
            select_change_callback_(param);
        }
        break;
    case SelectionEventKind::END:
        if (select_end_callback_) {
            select_end_callback_(param);
        }
        break;
    case SelectionEventKind::CANCEL:
        if (select_cancel_callback_) {
            select_cancel_callback_(param);
        }
        break;
    }
}

void KRView::OnSelectorPanGestureEventCB(ArkUI_GestureEvent *event, void *extraParams) {
    auto view = static_cast<KRView *>(extraParams);
    view->OnSelectorPanGestureEvent(event, extraParams);
}

void KRView::OnSelectorPanGestureEvent(ArkUI_GestureEvent *event, void *extraParams) {
    (void)extraParams;
    ArkUI_NodeHandle node = OH_ArkUI_GestureEvent_GetNode(event);

    auto gesture_event_window_point = kuikly::util::GetArkUIGestureEventWindowPoint(event);
    gesture_event_window_point.x = gesture_event_window_point.x / KRConfig::GetDpi();
    gesture_event_window_point.y = gesture_event_window_point.y / KRConfig::GetDpi();

    ArkUI_GestureEventActionType action_type = kuikly::util::GetArkUIGestureActionType(event);
    switch (action_type) {
    case GESTURE_EVENT_ACTION_ACCEPT:
        selection_info_.is_panning = true;
        selection_info_.panning_start_point_in_window = gesture_event_window_point;
        if (node == selection_info_.handle_nodes[0].wrapper) {
            selection_info_.panning_selection_point_when_start = selection_info_.selection_points[0];
            OH_ArkUI_SetGestureRecognizerEnabled(selection_info_.handle_nodes[1].text_selection_pan_recognizer, false);
        } else {
            selection_info_.panning_selection_point_when_start = selection_info_.selection_points[1];
            OH_ArkUI_SetGestureRecognizerEnabled(selection_info_.handle_nodes[0].text_selection_pan_recognizer, false);
        }
        break;
    case GESTURE_EVENT_ACTION_UPDATE: {
        KRPoint new_point(selection_info_.panning_selection_point_when_start.x +
                              gesture_event_window_point.x - selection_info_.panning_start_point_in_window.x,
                          selection_info_.panning_selection_point_when_start.y +
                              gesture_event_window_point.y - selection_info_.panning_start_point_in_window.y);

        if (node == selection_info_.handle_nodes[0].wrapper) {
            CreateSelection(new_point, selection_info_.selection_points[1], 0);
        } else {
            CreateSelection(selection_info_.selection_points[0], new_point, 0);
        }
    } break;
    case GESTURE_EVENT_ACTION_END:
    case GESTURE_EVENT_ACTION_CANCEL:
        selection_info_.sent_start_event = false;
        OH_ArkUI_SetGestureRecognizerEnabled(selection_info_.handle_nodes[0].text_selection_pan_recognizer, true);
        OH_ArkUI_SetGestureRecognizerEnabled(selection_info_.handle_nodes[1].text_selection_pan_recognizer, true);
        selection_info_.is_panning = false;
        FireSelectionEvent(SelectionEventKind::END);
        break;
    default:
        break;
    }
}

void KRView::EnsureSuperTouchType() {
    if (super_touch_type_ != UNKNOWN) {
        return;
    }
    
    if (super_touch_handler_) {
        super_touch_type_ = SELF;
        return;
    }

    auto parent_view = GetParentView();
    while (parent_view != nullptr) {
        if (auto view = std::dynamic_pointer_cast<KRView>(parent_view)) {
            auto handler = view->GetSuperTouchHandler();
            if (handler) {
                parent_super_touch_handler_ = handler;
                super_touch_type_ = PARENT;
                return;
            }
        }
        parent_view = parent_view->GetParentView();
    }

    super_touch_type_ = NONE;
}

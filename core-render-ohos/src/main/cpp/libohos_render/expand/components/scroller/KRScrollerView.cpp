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

#include "libohos_render/expand/components/scroller/KRScrollerView.h"

#include "libohos_render/expand/components/view/KRView.h"
#include "libohos_render/foundation/type/KRRenderValue.h"
#include "libohos_render/utils/KRJSONObject.h"


#ifdef __cplusplus
extern "C" {
#endif
// Remove this declaration if compatable api is raised to 18 and above
extern void* OH_ArkUI_GestureInterrupter_GetUserData(ArkUI_GestureInterruptInfo* event) __attribute__((weak));
#ifdef __cplusplus
};
#endif

constexpr char kPropNameDirectionRow[] = "directionRow";
constexpr char kPropNamePagingEnabled[] = "pagingEnabled";
constexpr char kPropNameScrollEnabled[] = "scrollEnabled";
constexpr char kPropNameVerticalBounces[] = "verticalbounces";
constexpr char kPropNameHorizontalBounces[] = "horizontalbounces";
constexpr char kPropNameBouncesEnable[] = "bouncesEnable";
constexpr char kPropNameLimitHeaderBounces[] = "limitHeaderBounces";
constexpr char kPropNameShowScrollerIndicator[] = "showScrollerIndicator";
constexpr char kPropNameNestedScroll[] = "nestedScroll";
constexpr char kPropKeyNestedScrollForward[] = "forward";
constexpr char kPropKeyNestedScrollBackward[] = "backward";

constexpr char kEventNameScroll[] = "scroll";
constexpr char kEventNameDragBegin[] = "dragBegin";
constexpr char kEventNameWillDragEnd[] = "willDragEnd";
constexpr char kEventNameDragEnd[] = "dragEnd";
constexpr char kEventNameScrollEnd[] = "scrollEnd";
constexpr char kEventKeyOffsetX[] = "offsetX";
constexpr char kEventKeyOffsetY[] = "offsetY";
constexpr char kEventKeyContentWidth[] = "contentWidth";
constexpr char kEventKeyContentHeight[] = "contentHeight";
constexpr char kEventKeyViewWidth[] = "viewWidth";
constexpr char kEventKeyViewHeight[] = "viewHeight";
constexpr char kEventKeyIsDragging[] = "isDragging";
constexpr char kEventKeyVelocityX[] = "velocityX";
constexpr char kEventKeyVelocityY[] = "velocityY";

constexpr char kMethodNameContentOffset[] = "contentOffset";
constexpr char kMethodNameContentInset[] = "contentInset";
constexpr char kMethodNameContentInsetWhenDragEnd[] = "contentInsetWhenEndDrag";

ArkUI_NodeHandle KRScrollerContentView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_STACK);
}

bool KRScrollerContentView::CustomSetViewFrame() {
    return true;
}

void KRScrollerContentView::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
    kuikly::util::UpdateNodeSize(GetNode(), frame.width, frame.height);
}

void KRScrollerContentView::AddContentScrollObserver(IKRContentScrollObserver *observer) {
    contentScrollObservers_.push_back(observer);
}

void KRScrollerContentView::RemoveContentScrollObserver(IKRContentScrollObserver *observer) {
    auto it = std::find(contentScrollObservers_.begin(), contentScrollObservers_.end(), observer);
    if (it != contentScrollObservers_.end()) {
        contentScrollObservers_.erase(it);
    }
}

void KRScrollerContentView::DidInsertSubRenderView(const std::shared_ptr<IKRRenderViewExport> &sub_render_view,
                                                   int index) {
    IKRRenderViewExport::DidInsertSubRenderView(sub_render_view, index);
    for (IKRContentScrollObserver *observer : contentScrollObservers_) {
        observer->ContentViewDidInsertSubview();
    }
}

bool isBouncesEnableProp(const std::string &prop_key) {
    auto prop_key_c_str = prop_key.c_str();
    return std::strcmp(prop_key_c_str, kPropNameVerticalBounces) == 0 ||
           std::strcmp(prop_key_c_str, kPropNameHorizontalBounces) == 0 ||
           std::strcmp(prop_key_c_str, kPropNameBouncesEnable) == 0;
}

void KRScrollerView::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
    if (!is_set_frame_) {
        is_set_frame_ = true;
        if (is_need_set_content_offset_) {
            kuikly::util::SetArkUIContentOffset(GetNode(), first_offset_x_, first_offset_y_, first_animate_, first_duration_);
            is_need_set_content_offset_ = false;
        }
    }
}

ArkUI_NodeHandle KRScrollerView::CreateNode() {
    auto node = kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_SCROLL);
    // Scroll 默认会对ContentView居中展示, 这里强制不居中
    kuikly::util::SetArkUIStackNodeContentAlignment(node, ArkUI_Alignment::ARKUI_ALIGNMENT_TOP_START);
    return node;
}

void KRScrollerView::DidInit() {
    current_scroll_state_ = ArkUI_ScrollState::ARKUI_SCROLL_STATE_IDLE;
    last_scroll_time_ = 0;
    last_scroll_x_ = 0;
    last_scroll_y_ = 0;
    velocity_x_ = 0;
    velocity_y_ = 0;
    SetBouncesEnable(NewKRRenderValue(bounces_enabled_));
    RegisterEvent(NODE_SCROLL_EVENT_ON_SCROLL_FRAME_BEGIN);
    RegisterEvent(NODE_SCROLL_EVENT_ON_SCROLL_START);
    RegisterEvent(NODE_SCROLL_EVENT_ON_WILL_SCROLL);
    RegisterEvent(NODE_SCROLL_EVENT_ON_SCROLL_STOP);
}

bool KRScrollerView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                             const KRRenderCallback event_call_back) {
    auto didHanded = false;
    if (std::strcmp(prop_key.c_str(), kPropNameDirectionRow) == 0) {
        didHanded = SetScrollDirection(prop_value);
    } else if (std::strcmp(prop_key.c_str(), kPropNamePagingEnabled) == 0) {
        didHanded = SetPagingEnabled(prop_value);
    } else if (std::strcmp(prop_key.c_str(), kEventNameScroll) == 0) {
        didHanded = RegisterOnScrollEvent(event_call_back);
    } else if (std::strcmp(prop_key.c_str(), kPropNameScrollEnabled) == 0) {
        didHanded = SetScrollEnabled(prop_value);
    } else if (isBouncesEnableProp(prop_key)) {
        didHanded = SetBouncesEnable(prop_value);
    } else if (std::strcmp(prop_key.c_str(), kPropNameShowScrollerIndicator) == 0) {
        didHanded = SetShowScrollerIndicator(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kEventNameDragBegin)) {
        didHanded = RegisterOnDragBeginEvent(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kEventNameDragEnd)) {
        didHanded = RegisterOnDragEndEvent(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kEventNameScrollEnd)) {
        didHanded = RegisterOnScrollEndEvent(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kEventNameWillDragEnd)) {
        didHanded = RegisterWillDragEndEvent(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kPropNameLimitHeaderBounces)) {
        didHanded = SetLimitHeaderBounces(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kPropNameNestedScroll)) {
        didHanded = SetNestedScroll(prop_value);
    }
    return didHanded;
}

bool KRScrollerView::ResetProp(const std::string &prop_key) {
    is_set_frame_ = false;
    is_need_set_content_offset_ = false;
    first_offset_x_ = 0;
    first_offset_y_ = 0;
    first_animate_ = false;
    auto didHanded = IKRRenderViewExport::ResetProp(prop_key);
    if (!didHanded) {
        if (prop_key == kPropNameNestedScroll) {
            didHanded = true;
            kuikly::util::ResetArkUINestedScroll(GetNode());
        }
    }
    return didHanded;
}

void KRScrollerView::CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &callback) {
    if (kuikly::util::isEqual(method, kMethodNameContentOffset)) {
        SetContentOffset(params);
    } else if (kuikly::util::isEqual(method, kMethodNameContentInset)) {
        SetContentInset(params);
    } else if (kuikly::util::isEqual(method, kMethodNameContentInsetWhenDragEnd)) {
        SetContentInsetWhenDragEnd(params);
    } else {
        IKRRenderViewExport::CallMethod(method, params, callback);
    }
}

void KRScrollerView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    if (event_type == NODE_SCROLL_EVENT_ON_SCROLL) {
        FireOnScrollEvent(event);
    } else if (event_type == NODE_SCROLL_EVENT_ON_SCROLL_FRAME_BEGIN) {
        OnScrollFrameBegin(event);
    } else if (event_type == NODE_SCROLL_EVENT_ON_SCROLL_START) {
        OnScrollStart(event);
    } else if (event_type == NODE_SCROLL_EVENT_ON_SCROLL_STOP) {
        OnScrollStop(event);
    } else if (event_type == NODE_SCROLL_EVENT_ON_WILL_SCROLL) {
        OnWillScroll(event);
    } else if (event_type == NODE_SCROLL_EVENT_ON_REACH_START) {
        OnScrollReachStart(event);
    }
}

void KRScrollerView::FireOnScrollEvent(ArkUI_NodeEvent *event) {
    // 分发滚动事件
    DispatchDidScrollToObservers();
    if (!on_scroll_callback_) {
        return;
    }
    on_scroll_callback_(GetCommonScrollParams());
}

void KRScrollerView::FireBeginDragEvent(ArkUI_NodeEvent *event) {
    if (!on_drag_begin_callback_) {
        return;
    }
    on_drag_begin_callback_(GetCommonScrollParams());
}

void KRScrollerView::FireWillDragEndEvent(ArkUI_NodeEvent *event) {
    KR_LOG_INFO << "fire will drag end";
    if (!on_will_drag_end_callback_) {
        return;
    }
    // TODO(userName): 补充加速度参数
    on_will_drag_end_callback_(GetCommonScrollParams());
}

void KRScrollerView::FireEndDragEvent(ArkUI_NodeEvent *event) {
    if (!on_drag_end_callback_) {
        return;
    }
    on_drag_end_callback_(GetCommonScrollParams());
}

void KRScrollerView::FireEndScrollEvent(ArkUI_NodeEvent *event) {
    if (!on_scroll_end_callback_) {
        return;
    }
    on_scroll_end_callback_(GetCommonScrollParams());
}

void KRScrollerView::DidInsertSubRenderView(const std::shared_ptr<IKRRenderViewExport> &sub_render_view, int index) {
    if (content_view_) {
        return;
    }

    content_view_ = std::dynamic_pointer_cast<KRScrollerContentView>(sub_render_view);
}

void KRScrollerView::OnDestroy() {
    if (!content_view_) {
        return;
    }
    content_view_ = nullptr;
    scroll_observers_.clear();
}

static ArkUI_ScrollNestedMode ParseOption(const std::string &option) {
    if (option == "SELF_FIRST") {
        return ARKUI_SCROLL_NESTED_MODE_SELF_FIRST;
    }
    if (option == "SELF_ONLY") {
        return ARKUI_SCROLL_NESTED_MODE_SELF_ONLY;
    }
    if (option == "PARENT_FIRST") {
        return ARKUI_SCROLL_NESTED_MODE_PARENT_FIRST;
    }
    if (option == "PARALLEL") {
        return ARKUI_SCROLL_NESTED_MODE_PARALLEL;
    }
    return ARKUI_SCROLL_NESTED_MODE_SELF_ONLY;
}

bool KRScrollerView::SetNestedScroll(const KRAnyValue &value) {
    const std::string &str = value->toString();
    auto paramObj = kuikly::util::JSONObject::Parse(str);
    const std::string &forwardStr = paramObj->GetString(kPropKeyNestedScrollForward);
    const std::string &backwardStr = paramObj->GetString(kPropKeyNestedScrollBackward);
    ArkUI_ScrollNestedMode forward = ParseOption(forwardStr);
    ArkUI_ScrollNestedMode backward = ParseOption(backwardStr);

    kuikly::util::SetArkUINestedScroll(GetNode(), forward, backward);
    return true;
}

bool KRScrollerView::SetScrollEnabled(const KRAnyValue &value) {
    auto scrollEnabled = value->toBool();
    kuikly::util::SetArkUIScrollEnabled(GetNode(), scrollEnabled);
    return true;
}

bool KRScrollerView::SetScrollDirection(const KRAnyValue &value) {
    auto direction_row = value->toBool();
    kuikly::util::SetArkUIScrollDirection(GetNode(), direction_row);
    return true;
}

bool KRScrollerView::SetPagingEnabled(const KRAnyValue &value) {
    auto pagingEnabled = value->toBool();
    kuikly::util::SetArkUIScrollPagingEnabled(GetNode(), pagingEnabled);
    return true;
}

bool KRScrollerView::SetBouncesEnable(const KRAnyValue &value) {
    bounces_enabled_ = value->toBool();
    InnerSetBouncesEnable(bounces_enabled_);
    return true;
}

bool KRScrollerView::SetLimitHeaderBounces(const KRAnyValue &value) {
    limit_header_bounces_ = value->toBool();
    RegisterEvent(NODE_SCROLL_EVENT_ON_SCROLL_EDGE);
    return true;
}

bool KRScrollerView::SetShowScrollerIndicator(const KRAnyValue &value) {
    auto enable = value->toBool();
    kuikly::util::SetArkUIShowScrollerIndicator(GetNode(), enable);
    return true;
}

bool KRScrollerView::RegisterOnScrollEvent(const KRRenderCallback event_call_back) {
    RegisterEvent(NODE_SCROLL_EVENT_ON_SCROLL);
    on_scroll_callback_ = event_call_back;
    return true;
}

bool KRScrollerView::RegisterOnDragBeginEvent(const KRRenderCallback event_callback) {
    on_drag_begin_callback_ = event_callback;
    return true;
}

bool KRScrollerView::RegisterOnDragEndEvent(const KRRenderCallback event_callback) {
    on_drag_end_callback_ = event_callback;
    return true;
}

bool KRScrollerView::RegisterOnScrollEndEvent(const KRRenderCallback event_callback) {
    on_scroll_end_callback_ = event_callback;
    return true;
}

bool KRScrollerView::RegisterWillDragEndEvent(const KRRenderCallback event_callback) {
    on_will_drag_end_callback_ = event_callback;
    return true;
}
/**
 *     NSArray<NSString *> *points = [params componentsSeparatedByString:@" "];
    BOOL animated = [points count] > 2 ? [points[2] boolValue] : NO;
    CGFloat duration = [points count] > 3 ? [points[3] floatValue] : 0;
    CGFloat damping = [points count] > 4 ? [points[4] floatValue] : 0;
    CGFloat velocity = [points count] > 5 ? [points[5] floatValue] : 0;
    CGPoint contentOffset = CGPointMake([points.firstObject doubleValue], [points[1] doubleValue]);
 * @param value
 */
void KRScrollerView::SetContentOffset(const KRAnyValue &value) {
    auto content_offset_splits = kuikly::util::SplitString(value->toString(), ' ');
    auto offset_x = content_offset_splits[0]->toFloat();
    auto offset_y = content_offset_splits[1]->toFloat();
    auto animate = content_offset_splits[2]->toBool();
    auto duration = content_offset_splits.size() > 3 ? content_offset_splits[3]->toInt() : 0;

    if (!is_set_frame_) {
        first_offset_x_ = offset_x;
        first_offset_y_ = offset_y;
        first_animate_ = animate;
        first_duration_ = duration;
        is_need_set_content_offset_ = true;
        return;
    }
    kuikly::util::SetArkUIContentOffset(GetNode(), offset_x, offset_y, animate, duration);
}

void KRScrollerView::SetContentInset(const KRAnyValue &value) {
    auto content_inset = std::make_shared<KRScrollerContentInset>(value);
    SetContentInset(content_inset);
}

void KRScrollerView::SetContentInsetWhenDragEnd(const KRAnyValue &value) {
    if (!content_view_) {
        return;
    }
    content_inset_when_drag_end_ = std::make_shared<KRScrollerContentInset>(value);
}

void KRScrollerView::SetContentInset(const std::shared_ptr<KRScrollerContentInset> &content_inset) {
    if (!content_view_) {
        return;
    }
    auto top = content_inset->top;
    auto start = content_inset->start;
    auto bottom = content_inset->bottom;
    auto end = content_inset->end;
    auto animate = content_inset->animate;
    if (animate) {
        auto root_view = GetRootView().lock();
        if (!root_view) {
            kuikly::util::SetArkUIMargin(content_view_->GetNode(), start, top, end, bottom);
            return;
        } else {
            auto animate_option = std::make_shared<KRAnimateOption>();
            animate_option->SetDuration(200);
            content_inset_animate_ = std::make_shared<KRAnimation>(
                root_view->GetUIContextHandle(), animate_option, [this, top, start, bottom, end]() {
                    kuikly::util::SetArkUIMargin(content_view_->GetNode(), start, top, end, bottom);
                });
            std::weak_ptr<KRScrollerView> weakSelf = std::dynamic_pointer_cast<KRScrollerView>(shared_from_this());
            content_inset_animate_->SetCompleteCallback(
                ArkUI_FinishCallbackType::ARKUI_FINISH_CALLBACK_LOGICALLY, [weakSelf]() {
                    if (std::shared_ptr<KRScrollerView> strongSelf = weakSelf.lock()) {
                        strongSelf->content_inset_animate_ = nullptr;
                    }
                });
            content_inset_animate_->Start();
        }
    } else {
        kuikly::util::SetArkUIMargin(content_view_->GetNode(), start, top, end, bottom);
    }
}

void KRScrollerView::OnScrollFrameBegin(ArkUI_NodeEvent *event) {
    auto scroll_state = kuikly::util::GetArkUIScrollerState(event, 1);
    if (current_scroll_state_ == scroll_state) {
        return;
    }

    auto current_time = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    auto point = kuikly::util::GetArkUIScrollContentOffset(GetNode());

    if (last_scroll_time_ > 0) {
        auto dt = current_time - last_scroll_time_;
        if (dt > 0) {
            velocity_x_ = (point.x - last_scroll_x_) * 1000.0f / dt;
            velocity_y_ = (point.y - last_scroll_y_) * 1000.0f / dt;
        }
    }

    last_scroll_time_ = current_time;
    last_scroll_x_ = point.x;
    last_scroll_y_ = point.y;
    if (IsIdeaStateToDraggingState(scroll_state) || IsFlingStateToDraggingState(scroll_state)) {
        is_dragging_ = true;
        FireBeginDragEvent(event);
    }
    if (IsDraggingStateToFlingState(scroll_state) || IsDraggingStateToIdeaState(scroll_state)) {
        OnWillDragEnd(event);
    }
    current_scroll_state_ = scroll_state;
    if (auto handler = weak_super_touch_handler_.lock()) {
        handler->SetNativeTouchConsumer(shared_from_this());
    }
}

void KRScrollerView::OnScrollStop(ArkUI_NodeEvent *event) {
    current_scroll_state_ = ArkUI_ScrollState::ARKUI_SCROLL_STATE_IDLE;
    if (is_dragging_) {
        OnWillDragEnd(event);
    }
    FireEndScrollEvent(event);
    if (auto handler = weak_super_touch_handler_.lock()) {
        handler->ClearNativeTouchConsumer(shared_from_this());
    }
}

void KRScrollerView::OnWillScroll(ArkUI_NodeEvent *event) {
    AdjustHeaderBouncesEnableWhenWillScroll(event);

    auto new_scroll_state = kuikly::util::GetArkUIScrollerState(event, 2);
    if (new_scroll_state == current_scroll_state_) {
        return;
    }
    if (is_dragging_ &&
        (IsDraggingStateToFlingState(new_scroll_state) || IsDraggingStateToIdeaState(new_scroll_state))) {
        OnWillDragEnd(event);
    }
    current_scroll_state_ = new_scroll_state;
}

void KRScrollerView::OnWillDragEnd(ArkUI_NodeEvent *event) {
    is_dragging_ = false;
    ApplyContentInsetWhenDragEnd();
    FireWillDragEndEvent(event);
    FireEndDragEvent(event);
}

void KRScrollerView::OnScrollStart(ArkUI_NodeEvent *event) {}

void KRScrollerView::OnScrollReachStart(ArkUI_NodeEvent *event) {
    if (!limit_header_bounces_) {
        return;
    }
    InnerSetBouncesEnable(false);
}

bool KRScrollerView::IsIdeaStateToDraggingState(ArkUI_ScrollState new_scroll_state) {
    return current_scroll_state_ == ArkUI_ScrollState::ARKUI_SCROLL_STATE_IDLE &&
           new_scroll_state == ArkUI_ScrollState::ARKUI_SCROLL_STATE_SCROLL;
}

bool KRScrollerView::IsFlingStateToDraggingState(ArkUI_ScrollState new_scroll_state) {
    return current_scroll_state_ == ArkUI_ScrollState::ARKUI_SCROLL_STATE_FLING &&
           new_scroll_state == ArkUI_ScrollState::ARKUI_SCROLL_STATE_SCROLL;
}

bool KRScrollerView::IsDraggingStateToFlingState(ArkUI_ScrollState new_scroll_state) {
    return current_scroll_state_ == ArkUI_ScrollState::ARKUI_SCROLL_STATE_SCROLL &&
           new_scroll_state == ArkUI_ScrollState::ARKUI_SCROLL_STATE_FLING;
}

bool KRScrollerView::IsDraggingStateToIdeaState(ArkUI_ScrollState new_scroll_state) {
    return current_scroll_state_ == ArkUI_ScrollState::ARKUI_SCROLL_STATE_SCROLL &&
           new_scroll_state == ArkUI_ScrollState::ARKUI_SCROLL_STATE_IDLE;
}

std::shared_ptr<KRRenderValue> KRScrollerView::GetCommonScrollParams() {
    KRRenderValueMap map;
    auto point = kuikly::util::GetArkUIScrollContentOffset(GetNode());
    map[kEventKeyOffsetX] = NewKRRenderValue(point.x);
    map[kEventKeyOffsetY] = NewKRRenderValue(point.y);

    auto frame = GetFrame();
    map[kEventKeyViewWidth] = NewKRRenderValue(frame.width);
    map[kEventKeyViewHeight] = NewKRRenderValue(frame.height);

    if (content_view_) {
        auto content_view_frame = content_view_->GetFrame();
        map[kEventKeyContentWidth] = NewKRRenderValue(content_view_frame.width);
        map[kEventKeyContentHeight] = NewKRRenderValue(content_view_frame.height);
    }
    map[kEventKeyIsDragging] = NewKRRenderValue(is_dragging_ ? 1 : 0);
    map[kEventKeyVelocityX] = NewKRRenderValue(velocity_x_);
    map[kEventKeyVelocityY] = NewKRRenderValue(velocity_y_);
    return NewKRRenderValue(std::move(map));
}

void KRScrollerView::ApplyContentInsetWhenDragEnd() {
    if (!content_inset_when_drag_end_) {
        return;
    }
    KR_LOG_INFO << "apply content inset: " << content_inset_when_drag_end_->top;
    SetContentInset(content_inset_when_drag_end_);
}

void KRScrollerView::InnerSetBouncesEnable(bool enable) {
    if (current_bounces_enabled_ == enable) {
        return;
    }

    current_bounces_enabled_ = enable;
    kuikly::util::SetArkUIBouncesEnabled(GetNode(), current_bounces_enabled_);
}

void KRScrollerView::AdjustHeaderBouncesEnableWhenWillScroll(ArkUI_NodeEvent *event) {
    if (!limit_header_bounces_) {
        return;
    }
    auto content_offset = kuikly::util::GetArkUIScrollContentOffset(GetNode());
    if (content_offset.y <= 0) {
        InnerSetBouncesEnable(false);
    } else {
        InnerSetBouncesEnable(true);
    }
}

void KRScrollerView::AddScrollObserver(IKRScrollObserver *observer) {
    scroll_observers_.push_back(observer);
}
void KRScrollerView::RemoveScrollObserver(IKRScrollObserver *observer) {
    auto it = std::find(scroll_observers_.begin(), scroll_observers_.end(), observer);
    if (it != scroll_observers_.end()) {
        scroll_observers_.erase(it);
    }
}

void KRScrollerView::DispatchDidScrollToObservers() {
    auto point = kuikly::util::GetArkUIScrollContentOffset(GetNode());
    for (IKRScrollObserver *observer : scroll_observers_) {
        observer->OnDidScroll(point.x, point.y);
    }
}

KRPoint KRScrollerView::GetContentOffset() {
    return kuikly::util::GetArkUIScrollContentOffset(GetNode());
}

void KRScrollerView::DidMoveToParentView() {
    IKRRenderViewExport::DidMoveToParentView();
    auto parent_view = GetParentView();
    while (parent_view != nullptr) {
        if (auto view = std::dynamic_pointer_cast<KRView>(parent_view)) {
            auto handler = view->GetSuperTouchHandler();
            if (handler) {
                weak_super_touch_handler_ = handler;
                if(!OH_ArkUI_GestureInterrupter_GetUserData){
                    // Only needed when `OH_ArkUI_GestureInterrupter_GetUserData` unavailable
                    SetViewTag(GetViewTag());
                }
                RegisterGestureInterrupter();
                break;
            }
        }
        parent_view = parent_view->GetParentView();
    }
}

void KRScrollerView::WillRemoveFromParentView() {
    IKRRenderViewExport::WillRemoveFromParentView();
    weak_super_touch_handler_.reset();
}

ArkUI_GestureInterruptResult KRScrollerView::OnInterruptGestureEvent(const ArkUI_GestureInterruptInfo *info) {
    if (auto handler = weak_super_touch_handler_.lock()) {
        auto recognizer = OH_ArkUI_GestureInterruptInfo_GetRecognizer(info);
        handler->CollectGestureRecognizer(recognizer);
        if (handler->IsPreventTouch()) {
            return GESTURE_INTERRUPT_RESULT_REJECT;
        }
    }
    return IKRRenderViewExport::OnInterruptGestureEvent(info);
}


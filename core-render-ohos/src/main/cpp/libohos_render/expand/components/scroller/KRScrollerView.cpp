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

#include <memory>

#include "libohos_render/expand/components/scroller/KRScrollerView.h"

#include <algorithm>
#include <cfloat>
#include <chrono>
#include <cmath>
#include <deviceinfo.h>
#include "libohos_render/expand/components/view/KRView.h"
#include "libohos_render/foundation/type/KRRenderValue.h"
#include "libohos_render/scheduler/KRInputFirstGate.h"
#include "libohos_render/utils/KRJSONObject.h"


#ifdef __cplusplus
extern "C" {
#endif
// Remove this declaration if compatable api is raised to 18 and above
extern void* OH_ArkUI_GestureInterrupter_GetUserData(ArkUI_GestureInterruptInfo* event) __attribute__((weak));
#ifdef __cplusplus
};
#endif

constexpr int FLING_SPEED_LIMIT_API_LEVEL = 18;
constexpr ArkUI_NodeAttributeType kScrollFlingSpeedLimitAttr =
    static_cast<ArkUI_NodeAttributeType>(1002019);
// 系统内置"点击状态栏回顶"属性（NODE_SCROLL_BACK_TO_TOP）从 API 15 开始支持
constexpr int SCROLL_BACK_TO_TOP_API_LEVEL = 15;

static bool IsFlingSpeedLimitApiAvailable() {
    return OH_GetSdkApiVersion() >= FLING_SPEED_LIMIT_API_LEVEL;
}

static bool IsScrollBackToTopApiAvailable() {
    return OH_GetSdkApiVersion() >= SCROLL_BACK_TO_TOP_API_LEVEL;
}

constexpr char kPropNameDirectionRow[] = "directionRow";
constexpr char kPropNamePagingEnabled[] = "pagingEnabled";
constexpr char kPropNameScrollEnabled[] = "scrollEnabled";
constexpr char kPropNameVerticalBounces[] = "verticalbounces";
constexpr char kPropNameHorizontalBounces[] = "horizontalbounces";
constexpr char kPropNameBouncesEnable[] = "bouncesEnable";
constexpr char kPropNameLimitHeaderBounces[] = "limitHeaderBounces";
constexpr char kPropNameShowScrollerIndicator[] = "showScrollerIndicator";
constexpr char kPropNameNestedScroll[] = "nestedScroll";
constexpr char kPropNameFlingEnable[] = "flingEnable";
constexpr char kPropNameFlingSpeedLimit[] = "flingSpeedLimit";
constexpr char kPropKeyNestedScrollForward[] = "forward";
constexpr char kPropKeyNestedScrollBackward[] = "backward";

constexpr char kEventNameScroll[] = "scroll";
constexpr char kEventNameDragBegin[] = "dragBegin";
constexpr char kEventNameWillDragEnd[] = "willDragEnd";
constexpr char kEventNameDragEnd[] = "dragEnd";
constexpr char kEventNameScrollEnd[] = "scrollEnd";
constexpr char kEventNameScrollToTop[] = "scrollToTop";
constexpr char16_t kEventKeyOffsetX[] = u"offsetX";
constexpr char16_t kEventKeyOffsetY[] = u"offsetY";
constexpr char16_t kEventKeyContentWidth[] = u"contentWidth";
constexpr char16_t kEventKeyContentHeight[] = u"contentHeight";
constexpr char16_t kEventKeyViewWidth[] = u"viewWidth";
constexpr char16_t kEventKeyViewHeight[] = u"viewHeight";
constexpr char16_t kEventKeyIsDragging[] = u"isDragging";
constexpr char16_t kEventKeyVelocityX[] = u"velocityX";
constexpr char16_t kEventKeyVelocityY[] = u"velocityY";

constexpr char kMethodNameContentOffset[] = "contentOffset";
constexpr char kMethodNameContentInset[] = "contentInset";
constexpr char kMethodNameContentInsetWhenDragEnd[] = "contentInsetWhenEndDrag";
constexpr char kMethodNameAbortContentOffsetAnimate[] = "abortContentOffsetAnimate";
constexpr char kMethodNamePrepareForComposeReuse[] = "prepareForComposeReuse";

ArkUI_NodeHandle KRScrollerContentView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_STACK);
}

void KRScrollerContentView::DidInit() {
    RegisterEvent(NODE_EVENT_ON_AREA_CHANGE);
}

bool KRScrollerContentView::CustomSetViewFrame() {
    return true;
}

void KRScrollerContentView::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
    kuikly::util::UpdateNodeSize(GetNode(), frame.width, frame.height);
    handling_set_view_frame_ = true;
}

void KRScrollerContentView::AddContentScrollObserver(IKRContentScrollObserver *observer) {
    contentScrollObservers_.insert(observer);
}

void KRScrollerContentView::RemoveContentScrollObserver(IKRContentScrollObserver *observer) {
    contentScrollObservers_.erase(observer);
}

void KRScrollerContentView::DidInsertSubRenderView(const std::shared_ptr<IKRRenderViewExport> &sub_render_view,
                                                   int index) {
    IKRRenderViewExport::DidInsertSubRenderView(sub_render_view, index);
    for (IKRContentScrollObserver *observer : contentScrollObservers_) {
        observer->ContentViewDidInsertSubview();
    }
}

void KRScrollerContentView::DidMoveToParentView() {
    IKRRenderViewExport::DidMoveToParentView();
    for (IKRContentScrollObserver *observer : contentScrollObservers_) {
        observer->ContentViewDidMoveToParentView();
    }
}

void KRScrollerContentView::WillRemoveFromParentView() {
    IKRRenderViewExport::WillRemoveFromParentView();
    for (IKRContentScrollObserver *observer : contentScrollObservers_) {
        observer->ContentViewWillRemoveFromParentView();
    }
}

void KRScrollerContentView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    if (event_type == NODE_EVENT_ON_AREA_CHANGE) {
        if (handling_set_view_frame_) {
            handling_set_view_frame_ = false;
            if (auto parentView = std::dynamic_pointer_cast<KRScrollerView>(GetParentView())) {
                parentView->TryApplyPendingFireOnScroll();
            }
        }
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
            kuikly::util::SetArkUIContentOffset(GetNode(), first_offset_x_, first_offset_y_, first_animate_,
                                                first_duration_, first_curve_, first_damping_);
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
    last_move_time_ = 0;
    velocity_x_ = 0;
    velocity_y_ = 0;
    // 关闭系统内置回顶，改由框架通过系统公共事件实现（见 openspec change: ohos-custom-back-to-top）
    DisableSystemBackToTopIfNeed();
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
    } else if (kuikly::util::isEqual(prop_key, kPropNameFlingEnable)) {
        didHanded = SetFlingEnable(prop_value->toBool());
    } else if (kuikly::util::isEqual(prop_key, kPropNameFlingSpeedLimit)) {
        didHanded = SetFlingSpeedLimit(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kEventNameScrollToTop)) {
        // 业务监听了"点击状态栏回顶"，后续由该回调决定行为（框架不再自动回顶）
        on_scroll_to_top_callback_ = event_call_back;
        didHanded = true;
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
        } else if (prop_key == kPropNameFlingEnable) {
            didHanded = true;
            SetFlingEnable(true);
        } else if (prop_key == kPropNameFlingSpeedLimit) {
            didHanded = true;
            if (IsFlingSpeedLimitApiAvailable()) {
                kuikly::util::GetNodeApi()->resetAttribute(GetNode(), kScrollFlingSpeedLimitAttr);
            }
        } else if (kuikly::util::isEqual(prop_key, kEventNameScrollToTop)) {
            didHanded = true;
            on_scroll_to_top_callback_ = nullptr;
        }
    }
    return didHanded;
}

void KRScrollerView::DisableSystemBackToTopIfNeed() {
    if (GetNode() == nullptr || !IsScrollBackToTopApiAvailable()) {
        // 低版本系统没有该属性，保持原有滚动行为
        return;
    }
    ArkUI_NumberValue value[] = {{.i32 = 0}};
    ArkUI_AttributeItem item = {value, 1};
    const auto set_result = kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_SCROLL_BACK_TO_TOP, &item);
    KR_LOG_INFO_WITH_TAG("KRBackToTop") << "disable system back-to-top: node=" << GetNode()
                                        << ", result=" << set_result;
}

void KRScrollerView::ScrollToTopByFramework() {
    if (GetNode() == nullptr) {
        return;
    }
    auto content_offset = kuikly::util::GetArkUIScrollContentOffset(GetNode());
    if (std::fabs(content_offset.x) < 0.5f && std::fabs(content_offset.y) < 0.5f) {
        // 已经在顶部，无需滚动
        return;
    }
    kuikly::util::SetArkUIContentOffset(GetNode(), 0, 0, true, 0, 0, 0);
}

void KRScrollerView::OnStatusBarClicked() {
    KREnsureMainThread();
    if (on_scroll_to_top_callback_ != nullptr) {
        // 业务已接管：仅回调，不做自动回顶（对齐 iOS scrollViewShouldScrollToTop: 返回 NO）
        on_scroll_to_top_callback_(KREmptyValue());
        return;
    }
    // 未注册回调：补回被关闭的系统默认行为
    ScrollToTopByFramework();
}

void KRScrollerView::CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &callback) {
    if (kuikly::util::isEqual(method, kMethodNameContentOffset)) {
        SetContentOffset(params);
    } else if (kuikly::util::isEqual(method, kMethodNameContentInset)) {
        SetContentInset(params);
    } else if (kuikly::util::isEqual(method, kMethodNameContentInsetWhenDragEnd)) {
        SetContentInsetWhenDragEnd(params);
    } else if (kuikly::util::isEqual(method, kMethodNameAbortContentOffsetAnimate)) {
        AbortContentOffsetAnimate();
    } else if (kuikly::util::isEqual(method, kMethodNamePrepareForComposeReuse)) {
        PrepareForComposeReuse();
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
    auto point = kuikly::util::GetArkUIScrollContentOffset(GetNode());
    if (point.x == last_fired_scroll_x_ && point.y == last_fired_scroll_y_) {
        return;
    }
    last_fired_scroll_x_ = point.x;
    last_fired_scroll_y_ = point.y;
    // 分发滚动事件
    DispatchDidScrollToObservers(point);
    if (!on_scroll_callback_) {
        return;
    }
    on_scroll_callback_(GetCommonScrollParams());
}

void KRScrollerView::FireBeginDragEvent(ArkUI_NodeEvent *event) {
    // 新的拖拽接上还在跑的弹簧滚动，scroll 会话继续，不在这里结束。
    CancelSpringScrollAnimation(false);
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
    EndScrollSession();
    DisposeSpringScrollAnimator();
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
    direction_row_ = value->toBool();
    kuikly::util::SetArkUIScrollDirection(GetNode(), direction_row_);
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
    auto damping = content_offset_splits.size() > 4 ? content_offset_splits[4]->toFloat() : 0;
    auto velocity = content_offset_splits.size() > 5 ? content_offset_splits[5]->toFloat() : 0;
    auto curve = content_offset_splits.size() > 6 ? content_offset_splits[6]->toInt() : 0;
    const bool start_spring = animate && duration > 0 && damping >= 1.0f && curve == 0;
    if (!is_set_frame_) {
        // 还没布局，这次不会接着播弹簧滚动，取消就是滚动结束。
        CancelSpringScrollAnimation(true);
        first_offset_x_ = offset_x;
        first_offset_y_ = offset_y;
        first_animate_ = animate;
        first_duration_ = duration;
        first_curve_ = curve;
        first_damping_ = damping;
        is_need_set_content_offset_ = true;
        return;
    }
    // 接着播新的弹簧滚动时，上一笔 scroll 会话继续；否则这次取消就是滚动结束。
    CancelSpringScrollAnimation(!start_spring);
    if (start_spring) {
        float velocity_x = direction_row_ ? velocity : 0;
        float velocity_y = direction_row_ ? 0 : velocity;
        if (StartSpringScrollAnimation(offset_x, offset_y, duration, velocity_x, velocity_y)) {
            return;
        }
    }
    kuikly::util::SetArkUIContentOffset(GetNode(), offset_x, offset_y, animate, duration, curve, damping);
}

namespace {
struct SpringScrollAnimatorUserData {
    std::weak_ptr<KRScrollerView> weak_view;
    uint32_t generation = 0;
};

// Critically damped spring settles to ~0.1% of the initial distance at t = duration.
constexpr float kSpringSettleFactor = 9.5f;

int64_t SpringNowUs() {
    return std::chrono::duration_cast<std::chrono::microseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();
}

// x(t) = target + (x0 + (v0 + w * x0) * t) * e^(-w * t), x0 = start - target
float CriticallyDampedSpringPosition(float start, float target, float velocity, float omega, float t_sec) {
    float x0 = start - target;
    return target + (x0 + (velocity + omega * x0) * t_sec) * expf(-omega * t_sec);
}

// Moving towards the target faster than w * |x0| overshoots; moving away bounces past the start.
float ClampSpringVelocity(float start, float target, float velocity, float omega) {
    float x0 = start - target;
    if (x0 == 0 || velocity * x0 >= 0) {
        return 0;
    }
    float max_speed = omega * fabsf(x0);
    return fabsf(velocity) > max_speed ? -omega * x0 : velocity;
}
}  // namespace

bool KRScrollerView::StartSpringScrollAnimation(float target_x, float target_y, int duration_ms, float velocity_x,
                                                float velocity_y) {
    auto root_view = GetRootView().lock();
    auto animate_api = GetAnimateApi();
    if (!root_view || !animate_api || !animate_api->createAnimator) {
        return false;
    }
    DisposeSpringScrollAnimator();

    auto start = kuikly::util::GetArkUIScrollContentOffset(GetNode());
    float omega = kSpringSettleFactor * 1000.0f / duration_ms;
    spring_anim_omega_ = omega;
    spring_anim_duration_ms_ = duration_ms;
    spring_anim_start_x_ = start.x;
    spring_anim_start_y_ = start.y;
    spring_anim_target_x_ = target_x;
    spring_anim_target_y_ = target_y;
    spring_anim_velocity_x_ = ClampSpringVelocity(start.x, target_x, velocity_x, omega);
    spring_anim_velocity_y_ = ClampSpringVelocity(start.y, target_y, velocity_y, omega);
    spring_anim_generation_++;

    auto user_data = new SpringScrollAnimatorUserData();
    user_data->weak_view = std::dynamic_pointer_cast<KRScrollerView>(shared_from_this());
    user_data->generation = spring_anim_generation_;
    spring_animator_user_data_ = user_data;

    auto option = OH_ArkUI_AnimatorOption_Create(0);
    OH_ArkUI_AnimatorOption_SetDuration(option, duration_ms);
    OH_ArkUI_AnimatorOption_SetIterations(option, 1);
    OH_ArkUI_AnimatorOption_SetBegin(option, 0);
    OH_ArkUI_AnimatorOption_SetEnd(option, 1);
    OH_ArkUI_AnimatorOption_RegisterOnFrameCallback(option, user_data, [](ArkUI_AnimatorOnFrameEvent *event) {
        auto data = static_cast<SpringScrollAnimatorUserData *>(OH_ArkUI_AnimatorOnFrameEvent_GetUserData(event));
        if (auto view = data ? data->weak_view.lock() : nullptr) {
            view->OnSpringScrollAnimationFrame(data->generation);
        }
    });
    OH_ArkUI_AnimatorOption_RegisterOnFinishCallback(option, user_data, [](ArkUI_AnimatorEvent *event) {
        auto data = static_cast<SpringScrollAnimatorUserData *>(OH_ArkUI_AnimatorEvent_GetUserData(event));
        if (auto view = data ? data->weak_view.lock() : nullptr) {
            view->OnSpringScrollAnimationEnd(data->generation);
        }
    });
    spring_animator_option_ = option;
    spring_animator_ = animate_api->createAnimator(root_view->GetUIContextHandle(), option);
    if (!spring_animator_) {
        DisposeSpringScrollAnimator();
        return false;
    }
    spring_anim_running_ = true;
    spring_anim_start_us_ = SpringNowUs();
    OH_ArkUI_Animator_Play(spring_animator_);
    // 弹簧滚动的 scrollTo 会让 OnScrollStop 直接返回，同步 scroll 的会话保持到 OnSpringScrollAnimationEnd。
    OnScrollStart(nullptr);
    return true;
}

void KRScrollerView::OnSpringScrollAnimationFrame(uint32_t generation) {
    if (!spring_anim_running_ || generation != spring_anim_generation_) {
        return;
    }
    auto now_us = SpringNowUs();
    float t_sec = std::min<int64_t>(now_us - spring_anim_start_us_, spring_anim_duration_ms_ * 1000) / 1e6f;
    float x = CriticallyDampedSpringPosition(spring_anim_start_x_, spring_anim_target_x_, spring_anim_velocity_x_,
                                             spring_anim_omega_, t_sec);
    float y = CriticallyDampedSpringPosition(spring_anim_start_y_, spring_anim_target_y_, spring_anim_velocity_y_,
                                             spring_anim_omega_, t_sec);
    kuikly::util::SetArkUIContentOffset(GetNode(), x, y, false, 0, 0, 0);
}

void KRScrollerView::OnSpringScrollAnimationEnd(uint32_t generation) {
    if (!spring_anim_running_ || generation != spring_anim_generation_) {
        return;
    }
    spring_anim_running_ = false;
    kuikly::util::SetArkUIContentOffset(GetNode(), spring_anim_target_x_, spring_anim_target_y_, false, 0, 0, 0);
    current_scroll_state_ = ArkUI_ScrollState::ARKUI_SCROLL_STATE_IDLE;
    EndScrollSession();
    FireEndScrollEvent(nullptr);
    if (auto handler = weak_super_touch_handler_.lock()) {
        handler->ClearNativeTouchConsumer(shared_from_this());
    }
}

void KRScrollerView::CancelSpringScrollAnimation(bool end_session) {
    if (!spring_anim_running_) {
        return;
    }
    spring_anim_running_ = false;
    spring_anim_generation_++;
    if (spring_animator_) {
        OH_ArkUI_Animator_Cancel(spring_animator_);
    }
    if (end_session) {
        EndScrollSession();
    }
}

// StartSpringScrollAnimation 会先调用这里换掉上一个 animator，此时滚动还没结束，不能结束 scroll 会话。
// 会话在 OnSpringScrollAnimationEnd、取消且不再滚动、WillRemoveFromParentView、OnDestroy 结束。
void KRScrollerView::DisposeSpringScrollAnimator() {
    spring_anim_running_ = false;
    spring_anim_generation_++;
    if (spring_animator_) {
        OH_ArkUI_Animator_Cancel(spring_animator_);
        if (auto animate_api = GetAnimateApi()) {
            animate_api->disposeAnimator(spring_animator_);
        }
        spring_animator_ = nullptr;
    }
    if (spring_animator_option_) {
        OH_ArkUI_AnimatorOption_Dispose(spring_animator_option_);
        spring_animator_option_ = nullptr;
    }
    if (spring_animator_user_data_) {
        delete static_cast<SpringScrollAnimatorUserData *>(spring_animator_user_data_);
        spring_animator_user_data_ = nullptr;
    }
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
        // 对齐 iOS 逻辑：若当前 offset 超出新 inset 的合法范围，先滚回合法位置
        auto current_offset = GetContentOffset();
        auto target_offset = MaxContentOffsetInContentInset(content_inset);
        if (target_offset.x != current_offset.x || target_offset.y != current_offset.y) {
            kuikly::util::SetArkUIContentOffset(GetNode(), target_offset.x, target_offset.y, true, 0, 0, 0);
        }
        // 再用原有动画逻辑设置 margin
        auto root_view = GetRootView().lock();
        if (!root_view) {
            kuikly::util::SetArkUIMargin(content_view_->GetNode(), start, top, end, bottom);
        } else {
            auto animate_option = std::make_shared<KRAnimateOption>();
            animate_option->SetDuration(200);
            auto weak_this = std::weak_ptr<KRScrollerView>(std::dynamic_pointer_cast<KRScrollerView>(shared_from_this()));
            content_inset_animate_ = std::make_shared<KRAnimation>(
                root_view->GetUIContextHandle(), animate_option, [weak_this, top, start, bottom, end]() {
                    if (auto strong_this = weak_this.lock()) {
                        kuikly::util::SetArkUIMargin(strong_this->content_view_->GetNode(), start, top, end, bottom);
                    }
                });
            content_inset_animate_->SetCompleteCallback(
                ArkUI_FinishCallbackType::ARKUI_FINISH_CALLBACK_LOGICALLY, [weak_this]() {
                    if (auto strong_this = weak_this.lock()) {
                        strong_this->content_inset_animate_ = nullptr;
                    }
                });
            content_inset_animate_->Start();
        }
    } else {
        kuikly::util::SetArkUIMargin(content_view_->GetNode(), start, top, end, bottom);
    }
}

// 计算在指定 contentInset 下 offset 的合法位置（对齐 iOS p_maxContentOffsetInContentInset）
KRPoint KRScrollerView::MaxContentOffsetInContentInset(
    const std::shared_ptr<KRScrollerContentInset> &content_inset) {
    if (!content_view_) {
        return KRPoint();
    }

    auto frame = GetFrame();
    auto content_frame = content_view_->GetFrame();
    auto current_offset = GetContentOffset();

    if (direction_row_) {
        float content_size = content_frame.width;
        float frame_size = frame.width;
        if (content_size <= frame_size) {
            return KRPoint();
        }
        // 上/左越界：offset 滚到了 inset 头部之前
        if (current_offset.x < -content_inset->start) {
            return KRPoint{-content_inset->start, 0};
        }
        // 下/右越界：offset 滚过了内容尾部
        float max_offset = content_size + content_inset->end - frame_size;
        if (current_offset.x > max_offset) {
            return KRPoint{max_offset, 0};
        }
    } else {
        float content_size = content_frame.height;
        float frame_size = frame.height;
        if (content_size <= frame_size) {
            return KRPoint();
        }
        // 上越界
        if (current_offset.y < -content_inset->top) {
            return KRPoint{0, -content_inset->top};
        }
        // 下越界
        float max_offset = content_size + content_inset->bottom - frame_size;
        if (current_offset.y > max_offset) {
            return KRPoint{0, max_offset};
        }
    }

    return current_offset;
}

void KRScrollerView::OnScrollFrameBegin(ArkUI_NodeEvent *event) {
    auto current_time = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    auto point = kuikly::util::GetArkUIScrollContentOffset(GetNode());

    if (last_scroll_time_ > 0) {
        auto dt = current_time - last_scroll_time_;
        if (dt > 0) {
            float instant_vx = (point.x - last_scroll_x_) * 1000.0f / dt;
            float instant_vy = (point.y - last_scroll_y_) * 1000.0f / dt;
            // EMA 平滑，抑制帧间抖动（alpha=0.3，半衰期约 2 帧）
            constexpr float kEmaAlpha = 0.3f;
            velocity_x_ = kEmaAlpha * instant_vx + (1.0f - kEmaAlpha) * velocity_x_;
            velocity_y_ = kEmaAlpha * instant_vy + (1.0f - kEmaAlpha) * velocity_y_;
            // 追踪产生有效位移的时间点，用于 OnWillDragEnd 的 stale 检测
            constexpr float kMinOffsetDelta = 0.5f;
            if (fabsf(point.x - last_scroll_x_) > kMinOffsetDelta ||
                fabsf(point.y - last_scroll_y_) > kMinOffsetDelta) {
                last_move_time_ = current_time;
            }
        }
    }

    last_scroll_time_ = current_time;
    last_scroll_x_ = point.x;
    last_scroll_y_ = point.y;
}

void KRScrollerView::OnScrollStop(ArkUI_NodeEvent *event) {
    if (spring_anim_running_) {
        // each frame of the spring animation is a non-animated scrollTo, which may report a stop
        return;
    }
    EndScrollSession();
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
    if (spring_anim_running_) {
        constexpr int32_t kScrollSourceDrag = 0;
        auto source = OH_ArkUI_NodeEvent_GetNodeComponentEvent(event)->data[3].i32;
        if (source != kScrollSourceDrag) {
            return;
        }
    }
    if (IsIdeaStateToDraggingState(new_scroll_state) || IsFlingStateToDraggingState(new_scroll_state)) {
        is_dragging_ = true;
        FireBeginDragEvent(event);
    } else if (is_dragging_ &&
        (IsDraggingStateToFlingState(new_scroll_state) || IsDraggingStateToIdeaState(new_scroll_state))) {
        OnWillDragEnd(event);
    }
    current_scroll_state_ = new_scroll_state;
    if (auto handler = weak_super_touch_handler_.lock()) {
        handler->SetNativeTouchConsumer(shared_from_this());
    }
}

void KRScrollerView::OnWillDragEnd(ArkUI_NodeEvent *event) {
    is_dragging_ = false;
    if (!is_fling_enabled_) {
        // call scrollBy with 0 velocity to stop fling
        ArkUI_NumberValue values[] = {{.f32 = 0}, {.f32 = 0}};
        ArkUI_AttributeItem item = {values, 2};
        kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_SCROLL_BY, &item);
    }
    ApplyContentInsetWhenDragEnd();
    FireWillDragEndEvent(event);
    FireEndDragEvent(event);
}

// 注册了同步 scroll 时，ArkUI 滚动开始就通知 KRInputFirstGate。滚动停止、弹簧滚动结束或取消、
// view 被移除或销毁时通知结束。
void KRScrollerView::OnScrollStart(ArkUI_NodeEvent *event) {
    if (scroll_sync_ && on_scroll_callback_ && !scroll_session_active_) {
        scroll_session_active_ = true;
        KRInputFirstGate::GetInstance().BeginSession(this, KRInputFirstGate::SessionType::kScroll);
    }
}

void KRScrollerView::SetEventSync(const std::string &event_name, bool sync) {
    if (kuikly::util::isEqual(event_name, kEventNameScroll)) {
        scroll_sync_ = sync;
    }
}

void KRScrollerView::EndScrollSession() {
    if (scroll_session_active_) {
        scroll_session_active_ = false;
        KRInputFirstGate::GetInstance().EndSession(this, KRInputFirstGate::SessionType::kScroll);
    }
}

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

KRAnyValue KRScrollerView::GetCommonScrollParams() {
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

    // 统一计算有效速度：基于最后位移的 stale 检测 + 最小阈值过滤
    auto now = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    constexpr int64_t kVelocityDecayThresholdMs = 150;
    constexpr float kMinVelocityThreshold = 100.0f;  // px/s

    float effective_vx = velocity_x_;
    float effective_vy = velocity_y_;
    if (now - last_move_time_ > kVelocityDecayThresholdMs) {
        effective_vx = 0;
        effective_vy = 0;
    }
    if (fabsf(effective_vx) < kMinVelocityThreshold) {
        effective_vx = 0;
    }
    if (fabsf(effective_vy) < kMinVelocityThreshold) {
        effective_vy = 0;
    }

    map[kEventKeyVelocityX] = NewKRRenderValue(effective_vx);
    map[kEventKeyVelocityY] = NewKRRenderValue(effective_vy);
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
    scroll_observers_.insert(observer);
}
void KRScrollerView::RemoveScrollObserver(IKRScrollObserver *observer) {
    scroll_observers_.erase(observer);
}

void KRScrollerView::DispatchDidScrollToObservers(KRPoint point) {
    for (IKRScrollObserver *observer : scroll_observers_) {
        observer->OnDidScroll(point.x, point.y);
    }
}

KRPoint KRScrollerView::GetContentOffset() {
    return kuikly::util::GetArkUIScrollContentOffset(GetNode());
}

void KRScrollerView::DidMoveToParentView() {
    IKRRenderViewExport::DidMoveToParentView();
    // 兜底：容器可能在系统事件订阅生效之前就被创建，挂载到视图树时再尝试一次（幂等）
    DisableSystemBackToTopIfNeed();
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
    EndScrollSession();
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

bool KRScrollerView::SetFlingEnable(bool enable) {
    is_fling_enabled_ = enable;
    return true;
}

bool KRScrollerView::SetFlingSpeedLimit(const KRAnyValue &value) {
    if (!IsFlingSpeedLimitApiAvailable()) {
        return true;
    }
    auto speed = value->toFloat();
    if (speed <= 0) {
        kuikly::util::GetNodeApi()->resetAttribute(GetNode(), kScrollFlingSpeedLimitAttr);
    } else {
        ArkUI_NumberValue values[] = {{.f32 = speed}};
        ArkUI_AttributeItem item = {values, 1};
        kuikly::util::GetNodeApi()->setAttribute(GetNode(), kScrollFlingSpeedLimitAttr, &item);
    }
    return true;
}

void KRScrollerView::TryApplyPendingFireOnScroll() {
    FireOnScrollEvent(nullptr);
}

// Clear transient native state for Compose DSL reuse (not the native reuse pool).
void KRScrollerView::PrepareForComposeReuse() {
    // Reset scroll event dedup cache so restored offset fires a scroll event
    last_fired_scroll_x_ = -FLT_MAX;
    last_fired_scroll_y_ = -FLT_MAX;
    // Reset scroll state machine
    current_scroll_state_ = ArkUI_ScrollState::ARKUI_SCROLL_STATE_IDLE;
    // Reset drag state
    is_dragging_ = false;
    // Reset bounces dedup flag
    current_bounces_enabled_ = false;
    // Clear PullToRefresh residual
    content_inset_when_drag_end_ = nullptr;
    // Reset velocity calculation state
    last_scroll_time_ = 0;
    last_scroll_x_ = 0;
    last_scroll_y_ = 0;
    last_move_time_ = 0;
    velocity_x_ = 0;
    velocity_y_ = 0;
}

void KRScrollerView::AbortContentOffsetAnimate() {
    // 手指还在拖时不能结束 scroll 会话。弹簧滚动只在没有拖动时取消并结束会话。
    CancelSpringScrollAnimation(!is_dragging_);
    // 正在拖动时 scrollBy(0, 0) 停不住手势，这次滚动还要等 OnScrollStop 结束会话。
    // 空闲且没有拖动时没有后续的 OnScrollStop，若会话还在就在这里结束。
    if (!is_dragging_ && current_scroll_state_ == ArkUI_ScrollState::ARKUI_SCROLL_STATE_IDLE) {
        EndScrollSession();
    }
    // 停止 ContentInset 动画（释放动画对象）
    if (content_inset_animate_) {
        content_inset_animate_ = nullptr;
    }

    // 停止滚动：通过 scrollBy(0, 0) 来停止当前的滚动/Fling 动画
    ArkUI_NumberValue values[] = {{.f32 = 0}, {.f32 = 0}};
    ArkUI_AttributeItem item = {values, 2};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_SCROLL_BY, &item);
}

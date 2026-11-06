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

#ifndef CORE_RENDER_OHOS_KRSCROLLERVIEW_H
#define CORE_RENDER_OHOS_KRSCROLLERVIEW_H

#include <unordered_set>
#include "KRScrollerContentInset.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/foundation/KRPoint.h"
#include "libohos_render/foundation/KRRect.h"
#include "libohos_render/utils/animate/KRAnimation.h"
#include "libohos_render/expand/components/view/SuperTouchHandler.h"

class IKRScrollObserver {
 public:
    // 滚动变化回调
    virtual void OnDidScroll(float offsetX, float offsetY) {}
};

class IKRContentScrollObserver {
 public:
    // 子孩子有插入时回调
    virtual void ContentViewDidInsertSubview() {}
    virtual void ContentViewDidMoveToParentView() {}
    virtual void ContentViewWillRemoveFromParentView() {}
};

class KRScrollerContentView : public IKRRenderViewExport {
 public:
    KRScrollerContentView() = default;
    KRScrollerContentView(const KRScrollerContentView &) = delete;
    KRScrollerContentView(KRScrollerContentView &&) = delete;
    KRScrollerContentView &operator=(const KRScrollerContentView &) = delete;
    KRScrollerContentView &operator=(KRScrollerContentView &&) = delete;

    ArkUI_NodeHandle CreateNode() override;
    void DidInit() override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;
    bool CustomSetViewFrame() override;
    void SetRenderViewFrame(const KRRect &frame) override;
    void DidInsertSubRenderView(const std::shared_ptr<IKRRenderViewExport> &sub_render_view, int index) override;
    void DidMoveToParentView() override;
    void WillRemoveFromParentView() override;
    void AddContentScrollObserver(IKRContentScrollObserver *observer);
    void RemoveContentScrollObserver(IKRContentScrollObserver *observer);

    const std::unordered_set<IKRContentScrollObserver *> &GetContentScrollObservers() {
        return contentScrollObservers_;
    }

 private:
    std::unordered_set<IKRContentScrollObserver *> contentScrollObservers_;
    bool handling_set_view_frame_ = false;
};

class KRScrollerView : public IKRRenderViewExport {
 public:
    KRScrollerView() = default;
    KRScrollerView(const KRScrollerView &) = delete;
    KRScrollerView(KRScrollerView &&) = delete;
    KRScrollerView &operator=(const KRScrollerView &) = delete;
    KRScrollerView &operator=(KRScrollerView &&) = delete;

    ArkUI_NodeHandle CreateNode() override;
    void DidInit() override;
    void SetRenderViewFrame(const KRRect &frame) override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    bool ResetProp(const std::string &prop_key) override;
    void CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &callback) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;
    void OnDestroy() override;
    void DidInsertSubRenderView(const std::shared_ptr<IKRRenderViewExport> &sub_render_view, int index) override;
    void AddScrollObserver(IKRScrollObserver *observer);
    void RemoveScrollObserver(IKRScrollObserver *observer);
    KRPoint GetContentOffset();
    void DidMoveToParentView() override;
    void WillRemoveFromParentView() override;
    void SetEventSync(const std::string &event_name, bool sync) override;
    ArkUI_GestureInterruptResult OnInterruptGestureEvent(const ArkUI_GestureInterruptInfo *info) override;
    void TryApplyPendingFireOnScroll();
    void OnStatusBarClicked() override;

    bool IsScrollView() override {
        return true;
    }

 private:
    bool SetNestedScroll(const KRAnyValue &value);
    bool SetScrollEnabled(const KRAnyValue &value);
    bool SetScrollDirection(const KRAnyValue &value);
    bool SetPagingEnabled(const KRAnyValue &value);
    bool SetBouncesEnable(const KRAnyValue &value);
    bool SetLimitHeaderBounces(const KRAnyValue &value);
    bool SetShowScrollerIndicator(const KRAnyValue &value);
    bool RegisterOnScrollEvent(const KRRenderCallback event_call_back);
    bool RegisterOnDragBeginEvent(const KRRenderCallback event_callback);
    bool RegisterOnDragEndEvent(const KRRenderCallback event_callback);
    bool RegisterOnScrollEndEvent(const KRRenderCallback event_callback);
    bool RegisterWillDragEndEvent(const KRRenderCallback event_callback);
    void FireOnScrollEvent(ArkUI_NodeEvent *event);
    void FireBeginDragEvent(ArkUI_NodeEvent *event);
    void FireEndDragEvent(ArkUI_NodeEvent *event);
    void FireEndScrollEvent(ArkUI_NodeEvent *event);
    void FireWillDragEndEvent(ArkUI_NodeEvent *event);
    void SetContentOffset(const KRAnyValue &value);
    void SetContentInset(const KRAnyValue &value);
    void SetContentInset(const std::shared_ptr<KRScrollerContentInset> &content_inset);
    void SetContentInsetWhenDragEnd(const KRAnyValue &value);
    void AbortContentOffsetAnimate();
    void PrepareForComposeReuse();
    void OnScrollFrameBegin(ArkUI_NodeEvent *event);
    void OnScrollStop(ArkUI_NodeEvent *event);
    void OnWillScroll(ArkUI_NodeEvent *event);
    void OnWillDragEnd(ArkUI_NodeEvent *event);
    void OnScrollStart(ArkUI_NodeEvent *event);
    void OnScrollReachStart(ArkUI_NodeEvent *event);
    bool IsIdeaStateToDraggingState(ArkUI_ScrollState new_scroll_state);
    bool IsFlingStateToDraggingState(ArkUI_ScrollState new_scroll_state);
    bool IsDraggingStateToFlingState(ArkUI_ScrollState new_scroll_state);
    bool IsDraggingStateToIdeaState(ArkUI_ScrollState new_scroll_state);
    KRAnyValue GetCommonScrollParams();
    void ApplyContentInsetWhenDragEnd();
    void InnerSetBouncesEnable(bool enable);
    void AdjustHeaderBouncesEnableWhenWillScroll(ArkUI_NodeEvent *event);
    void DispatchDidScrollToObservers(KRPoint point);
    void EndScrollSession();
    bool SetFlingEnable(bool enable);
    bool SetFlingSpeedLimit(const KRAnyValue &value);
    KRPoint MaxContentOffsetInContentInset(const std::shared_ptr<KRScrollerContentInset> &content_inset);
    bool StartSpringScrollAnimation(float target_x, float target_y, int duration_ms, float velocity_x,
                                    float velocity_y);
    void CancelSpringScrollAnimation(bool end_session);
    void DisposeSpringScrollAnimator();
    /** 关闭系统内置的"点击状态栏回顶"。节点为空或系统 API < 15 时不设置，保持原有滚动行为。 */
    void DisableSystemBackToTopIfNeed();
    /** 框架实现的默认回顶：滚动到顶部 */
    void ScrollToTopByFramework();

 public:
    void OnSpringScrollAnimationFrame(uint32_t generation);
    void OnSpringScrollAnimationEnd(uint32_t generation);

 private:
    KRRenderCallback on_scroll_callback_ = nullptr;
    KRRenderCallback on_drag_begin_callback_ = nullptr;
    KRRenderCallback on_drag_end_callback_ = nullptr;
    KRRenderCallback on_scroll_end_callback_ = nullptr;
    KRRenderCallback on_will_drag_end_callback_ = nullptr;
    KRRenderCallback on_scroll_to_top_callback_ = nullptr;
    // scroll 是否以同步方式注册，以及这次滚动是否已通知 KRInputFirstGate。
    bool scroll_sync_ = false;
    bool scroll_session_active_ = false;
    std::shared_ptr<KRScrollerContentView> content_view_;
    bool bounces_enabled_ = true;
    bool limit_header_bounces_ = false;
    bool current_bounces_enabled_ = false;
    bool is_dragging_ = false;
    bool is_set_frame_ = false;
    bool is_need_set_content_offset_ = false;
    float first_offset_x_ = 0;
    float first_offset_y_ = 0;
    bool first_animate_ = false;
    int first_duration_ = 0;
    int first_curve_ = 0;
    float first_damping_ = 0;
    ArkUI_ScrollState current_scroll_state_;

    std::shared_ptr<KRAnimation> content_inset_animate_;
    std::shared_ptr<KRScrollerContentInset> content_inset_when_drag_end_;
    std::unordered_set<IKRScrollObserver *> scroll_observers_;

    // 滚动相关的成员变量
    int64_t last_scroll_time_ = 0;
    float last_scroll_x_ = 0;
    float last_scroll_y_ = 0;
    float velocity_x_ = 0;
    float velocity_y_ = 0;
    int64_t last_move_time_ = 0;  // 上次产生有效位移的时间，用于判断速度是否过期
    std::weak_ptr<SuperTouchHandler> weak_super_touch_handler_;
    bool is_fling_enabled_ = true;
    float last_fired_scroll_x_ = 0;
    float last_fired_scroll_y_ = 0;
    bool direction_row_ = false;

    // setContentOffset spring animation driven frame by frame (aligned with iOS UIView spring animation)
    ArkUI_AnimatorHandle spring_animator_ = nullptr;
    ArkUI_AnimatorOption *spring_animator_option_ = nullptr;
    void *spring_animator_user_data_ = nullptr;
    bool spring_anim_running_ = false;
    uint32_t spring_anim_generation_ = 0;
    int64_t spring_anim_start_us_ = 0;
    int spring_anim_duration_ms_ = 0;
    float spring_anim_omega_ = 0;
    float spring_anim_start_x_ = 0;
    float spring_anim_start_y_ = 0;
    float spring_anim_target_x_ = 0;
    float spring_anim_target_y_ = 0;
    float spring_anim_velocity_x_ = 0;
    float spring_anim_velocity_y_ = 0;
};

#endif  // CORE_RENDER_OHOS_KRSCROLLERVIEW_H

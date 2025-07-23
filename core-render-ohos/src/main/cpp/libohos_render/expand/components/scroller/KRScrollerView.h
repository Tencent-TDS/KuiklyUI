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
};

class KRScrollerContentView : public IKRRenderViewExport {
 public:
    KRScrollerContentView() = default;
    KRScrollerContentView(const KRScrollerContentView &) = delete;
    KRScrollerContentView(KRScrollerContentView &&) = delete;
    KRScrollerContentView &operator=(const KRScrollerContentView &) = delete;
    KRScrollerContentView &operator=(KRScrollerContentView &&) = delete;

    ArkUI_NodeHandle CreateNode() override;
    bool CustomSetViewFrame() override;
    void SetRenderViewFrame(const KRRect &frame) override;
    void DidInsertSubRenderView(const std::shared_ptr<IKRRenderViewExport> &sub_render_view, int index) override;
    void AddContentScrollObserver(IKRContentScrollObserver *observer);
    void RemoveContentScrollObserver(IKRContentScrollObserver *observer);

    const std::vector<IKRContentScrollObserver *> &GetContentScrollObservers() {
        return contentScrollObservers_;
    }

 private:
    std::vector<IKRContentScrollObserver *> contentScrollObservers_;
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
    ArkUI_GestureInterruptResult OnInterruptGestureEvent(const ArkUI_GestureInterruptInfo *info) override;

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
    std::shared_ptr<KRRenderValue> GetCommonScrollParams();
    void ApplyContentInsetWhenDragEnd();
    void InnerSetBouncesEnable(bool enable);
    void AdjustHeaderBouncesEnableWhenWillScroll(ArkUI_NodeEvent *event);
    void DispatchDidScrollToObservers();

 private:
    KRRenderCallback on_scroll_callback_ = nullptr;
    KRRenderCallback on_drag_begin_callback_ = nullptr;
    KRRenderCallback on_drag_end_callback_ = nullptr;
    KRRenderCallback on_scroll_end_callback_ = nullptr;
    KRRenderCallback on_will_drag_end_callback_ = nullptr;
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
    ArkUI_ScrollState current_scroll_state_;

    std::shared_ptr<KRAnimation> content_inset_animate_;
    std::shared_ptr<KRScrollerContentInset> content_inset_when_drag_end_;
    std::vector<IKRScrollObserver *> scroll_observers_;

    // 滚动相关的成员变量
    int64_t last_scroll_time_ = 0;
    float last_scroll_x_ = 0;
    float last_scroll_y_ = 0;
    float velocity_x_ = 0;
    float velocity_y_ = 0;
    std::weak_ptr<SuperTouchHandler> weak_super_touch_handler_;
};

#endif  // CORE_RENDER_OHOS_KRSCROLLERVIEW_H

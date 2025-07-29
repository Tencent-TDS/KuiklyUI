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

#include "libohos_render/expand/components/hover/KRHoverView.h"

#include "libohos_render/expand/components/scroller/KRScrollerView.h"

bool KRHoverView::ReuseEnable() {
    return false;
}

bool KRHoverView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                          const KRRenderCallback event_call_back) {
    if (prop_key == "hoverMarginTop") {
        css_hover_margin_top_ = prop_value->toFloat();
        return true;
    }
    if (prop_key == "bringIndex") {
        css_bring_index_ = prop_value->toInt();
        return true;
    }
    return KRView::SetProp(prop_key, prop_value, event_call_back);
}

void KRHoverView::DidMoveToParentView() {
    KRView::DidMoveToParentView();
    auto contentView = GetParentView();
    if (contentView == nullptr) {
        return;
    }

    if (auto contentScrollViewPtr = dynamic_cast<KRScrollerContentView *>(contentView.get())) {
        contentScrollViewPtr->AddContentScrollObserver(this);
    }

    if (auto scrollView = contentView->GetParentView()) {
        if (auto scrollViewPtr = dynamic_cast<KRScrollerView *>(scrollView.get())) {
            scrollViewPtr->AddScrollObserver(this);
        }
    }
    UpdateFrameToHoverIfNeed();
}

void KRHoverView::SetRenderViewFrame(const KRRect &frame) {
    KRView::SetRenderViewFrame(frame);
    css_frame_ = frame;
    frame_changed_ = true;
    UpdateFrameToHoverIfNeed();
}

void KRHoverView::WillRemoveFromParentView() {
    KRView::WillRemoveFromParentView();
    auto contentView = GetParentView();
    if (contentView == nullptr) {
        return;
    }

    if (auto contentScrollViewPtr = dynamic_cast<KRScrollerContentView *>(contentView.get())) {
        contentScrollViewPtr->RemoveContentScrollObserver(this);
    }

    if (auto scrollView = contentView->GetParentView()) {
        if (auto scrollViewPtr = dynamic_cast<KRScrollerView *>(scrollView.get())) {
            scrollViewPtr->RemoveScrollObserver(this);
        }
    }
}

void KRHoverView::OnDidScroll(float offsetX, float offsetY) {
    UpdateFrameToHoverIfNeed();
}

void KRHoverView::ContentViewDidInsertSubview() {
    UpdateFrameToHoverIfNeed();
}

void KRHoverView::UpdateFrameToHoverIfNeed() {
    auto contentView = GetParentView();
    auto offset = GetContentOffset();
    auto frame = css_frame_;
    // 仅支持垂直方向置顶
    if (offset.y > frame.y - css_hover_margin_top_) {
        frame.y = offset.y + css_hover_margin_top_;
    }
    if (css_frame_.y != frame.y || css_frame_.x != frame.x) {
        frame_changed_ = true;
        kuikly::util::UpdateNodeFrame(GetNode(), frame);
    } else if (frame_changed_) {
        frame_changed_ = false;
        kuikly::util::UpdateNodeFrame(GetNode(), frame);
    }
    AdjustHoverViewLayerIfNeed();
}

KRPoint KRHoverView::GetContentOffset() {
    auto contentView = GetParentView();
    if (contentView == nullptr) {
        return KRPoint();
    }
    if (auto scrollView = contentView->GetParentView()) {
        if (auto scrollViewPtr = dynamic_cast<KRScrollerView *>(scrollView.get())) {
            return scrollViewPtr->GetContentOffset();
        }
    }
    return KRPoint();
}

void KRHoverView::AdjustHoverViewLayerIfNeed() {
    std::vector<KRHoverView *> hoverViews;
    auto contentView = GetParentView();
    if (contentView == nullptr) {
        return;
    }
    // 收集所有 KRHoverView 子视图
    if (auto viewPtr = dynamic_cast<KRScrollerContentView *>(contentView.get())) {
        for (IKRContentScrollObserver *observer : viewPtr->GetContentScrollObservers()) {
            if (auto hoverView = dynamic_cast<KRHoverView *>(observer)) {
                hoverViews.push_back(hoverView);
            }
        }
    }
    // 根据 css_bringIndex 和 z_index 进行排序
    std::sort(hoverViews.begin(), hoverViews.end(), [](KRHoverView *obj1, KRHoverView *obj2) {
        int obj1Index = std::max(obj1->GetBringIndex(), obj1->GetBasePropsHandler()->GetZIndex());
        int obj2Index = std::max(obj2->GetBringIndex(), obj2->GetBasePropsHandler()->GetZIndex());
        if (obj1Index == obj2Index) {
            return false;
        }
        return obj1Index < obj2Index;
    });
    // 将排序后的 hoverViews 置于前景
    for (KRHoverView *obj : hoverViews) {
        kuikly::util::GetNodeApi()->removeChild(contentView->GetNode(), obj->GetNode());
        kuikly::util::GetNodeApi()->addChild(contentView->GetNode(), obj->GetNode());
    }
}

int KRHoverView::GetBringIndex() {
    return css_bring_index_;
}

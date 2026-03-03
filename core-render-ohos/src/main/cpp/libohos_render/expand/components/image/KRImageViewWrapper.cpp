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

#include "libohos_render/expand/components/image/KRImageViewWrapper.h"

constexpr char kPropNameResize[] = "resize";
constexpr char kPropNameSrc[] = "src";
constexpr char kPropNamePlaceHolder[] = "placeholder";

void KRImageViewWrapper::DidInit() {
    place_holder_image_view_ = std::make_shared<KRImageView>();
    InitImageView(place_holder_image_view_);
    image_view_ = std::make_shared<KRImageView>();
    InitImageView(image_view_);
    // 设置 wrapper 弱引用
    image_view_->wrapper_ = std::static_pointer_cast<KRImageViewWrapper>(shared_from_this());
}

void KRImageViewWrapper::InitImageView(std::shared_ptr<KRImageView> image_view) {
    image_view->SetRootView(GetRootView(), GetInstanceId());
    image_view->SetViewName(GetViewName());
    image_view->SetViewTag(GetViewTag());
    image_view->ToInit();
}

bool KRImageViewWrapper::ReuseEnable() {
    return true;
}

void KRImageViewWrapper::DidMoveToParentView() {
    IKRRenderViewExport::DidMoveToParentView();
    if (!did_insert_image_view_) {
        did_insert_image_view_ = true;
        // 调整插入顺序，imageView 先插入（底层），placeholderView 后插入（上层遮挡）
        ToInsertSubRenderView(image_view_, -1);
        ToInsertSubRenderView(place_holder_image_view_, -1);
    }
    image_view_->SetViewTag(GetViewTag());
    place_holder_image_view_->SetViewTag(GetViewTag());
}

void KRImageViewWrapper::OnDestroy() {
    // 先清理 callback，避免销毁后回调被触发
    if (image_view_) {
        image_view_->ClearOnDecodedCallback();
    }
    IKRRenderViewExport::OnDestroy();
    place_holder_image_view_->ToDestroy();
    image_view_->ToDestroy();
}

void KRImageViewWrapper::DoClearPlaceholder() {
    if (placeholder_cleared_) {
        return;
    }
    placeholder_cleared_ = true;
    pending_clear_placeholder_ = false;
    // 清除 placeholder 的 src
    place_holder_image_view_->ResetProp(kPropNameSrc);
}

void KRImageViewWrapper::CheckAndClearPlaceholder() {
    if (placeholder_cleared_ || pending_clear_placeholder_) {
        return;
    }
    
    // 检查 image_view_ 是否已经解码完成（status=1）
    if (image_view_->IsDecoded()) {
        // 已经解码完成，立即清除
        DoClearPlaceholder();
    } else {
        // 图片未解码完成，注册回调等待解码完成
        pending_clear_placeholder_ = true;
        auto weak_wrapper = std::weak_ptr<KRImageViewWrapper>(
            std::static_pointer_cast<KRImageViewWrapper>(shared_from_this()));
        image_view_->SetOnDecodedCallback([weak_wrapper]() {
            if (auto wrapper = weak_wrapper.lock()) {
                wrapper->DoClearPlaceholder();
            } 
        });
    }
}

bool KRImageViewWrapper::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                 const KRRenderCallback event_call_back) {
    auto didHanded = false;
    didHanded = image_view_->SetProp(prop_key, prop_value, event_call_back);
    if (std::strcmp(prop_key.c_str(), kPropNameResize) == 0) {
        place_holder_image_view_->SetProp(prop_key, prop_value, event_call_back);
    } else if (std::strcmp(prop_key.c_str(), kPropNamePlaceHolder) == 0) {
        auto src_str = prop_value->toString();
        if (!src_str.empty()) {
            // 设置新的 placeholder，重置清除状态
            placeholder_cleared_ = false;
            pending_clear_placeholder_ = false;
            place_holder_image_view_->SetProp(kPropNameSrc, prop_value, event_call_back);
        } else {
            // clearPlaceholder 被调用（来自 Kotlin）
            // 使用延迟清除策略：等待图片解码完成后再清除
            CheckAndClearPlaceholder();
        }
        didHanded = true;
    } else if (std::strcmp(prop_key.c_str(), kPropNameSrc) == 0) {
        // 重置 placeholder 清除状态，因为设置了新的 src
        placeholder_cleared_ = false;
        pending_clear_placeholder_ = false;
        image_view_->ClearOnDecodedCallback();
    }
    return didHanded;
}

bool KRImageViewWrapper::ResetProp(const std::string &prop_key) {
    IKRRenderViewExport::ResetProp(prop_key);
    auto didHanded = image_view_->ResetProp(prop_key);
    if (std::strcmp(prop_key.c_str(), kPropNameResize) == 0) {
        place_holder_image_view_->ResetProp(kPropNameResize);
    } else if (std::strcmp(prop_key.c_str(), kPropNamePlaceHolder) == 0) {
        place_holder_image_view_->ResetProp(kPropNameSrc);
        placeholder_cleared_ = false;
        pending_clear_placeholder_ = false;
        didHanded = true;
    }
    return didHanded;
}

void KRImageViewWrapper::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
    kuikly::util::UpdateNodeFrame(image_view_->GetNode(), KRRect(0, 0, frame.width, frame.height));
    kuikly::util::UpdateNodeFrame(place_holder_image_view_->GetNode(), KRRect(0, 0, frame.width, frame.height));
}


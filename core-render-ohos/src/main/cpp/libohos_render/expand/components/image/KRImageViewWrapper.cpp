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
        ToInsertSubRenderView(place_holder_image_view_, -1);
        ToInsertSubRenderView(image_view_, -1);
    }
    image_view_->SetViewTag(GetViewTag());
    place_holder_image_view_->SetViewTag(GetViewTag());
}

void KRImageViewWrapper::OnDestroy() {
    IKRRenderViewExport::OnDestroy();
    place_holder_image_view_->ToDestroy();
    image_view_->ToDestroy();
}

bool KRImageViewWrapper::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                 const KRRenderCallback event_call_back) {
    auto didHanded = false;
    didHanded = image_view_->SetProp(prop_key, prop_value, event_call_back);
    if (std::strcmp(prop_key.c_str(), kPropNameResize) == 0) {
        place_holder_image_view_->SetProp(prop_key, prop_value, event_call_back);
    } else if (std::strcmp(prop_key.c_str(), kPropNamePlaceHolder) == 0) {
        place_holder_image_view_->SetProp(kPropNameSrc, prop_value, event_call_back);
        didHanded = true;
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
        didHanded = true;
    }
    return didHanded;
}

void KRImageViewWrapper::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
    kuikly::util::UpdateNodeFrame(image_view_->GetNode(), KRRect(0, 0, frame.width, frame.height));
    kuikly::util::UpdateNodeFrame(place_holder_image_view_->GetNode(), KRRect(0, 0, frame.width, frame.height));
}

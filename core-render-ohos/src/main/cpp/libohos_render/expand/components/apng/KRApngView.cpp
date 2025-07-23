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

#include "libohos_render/expand/components/apng/KRApngView.h"

#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>
#include <vector>
#include "libohos_render/expand/components/apng/APNGAnimateView.h"
#include "libohos_render/expand/components/image/KRImageView.h"
#include "libohos_render/expand/modules/network/KRNetworkModule.h"
#include "libohos_render/utils/KRURIHelper.h"
#include "libohos_render/utils/KRStringUtil.h"

constexpr char kPropNameSrc[] = "src";
constexpr char kPropNameAutoPlay[] = "autoPlay";
constexpr char kPropNameRepeatCount[] = "repeatCount";
constexpr char kEventLoadFailure[] = "loadFailure";
constexpr char kEventAnimatedStart[] = "animationStart";
constexpr char kEventAnimatedEnd[] = "animationEnd";

bool KRApngView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                         const KRRenderCallback event_call_back) {
    if (kuikly::util::isEqual(prop_key, kPropNameSrc)) {  // setSrc
        auto src = prop_value->toString();
        SetSrc(src);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kPropNameAutoPlay)) {  // setSrc
        SetAutoPlay(prop_value->toBool());
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kPropNameRepeatCount)) {  //
        SetRepeatCount(prop_value->toInt());
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kEventLoadFailure)) {  //
        load_failure_callback_ = event_call_back;
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kEventAnimatedStart)) {  //
        animation_start_callback_ = event_call_back;
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kEventAnimatedEnd)) {  //
        animation_end_callback_ = event_call_back;
        return true;
    }

    return IKRRenderViewExport::SetProp(prop_key, prop_value, event_call_back);
}

void KRApngView::OnDestroy() {
    IKRRenderViewExport::OnDestroy();
    if (apng_view_) {
        apng_view_->Destroy();
        apng_view_ = nullptr;
    }
}
void KRApngView::DidMoveToParentView() {
    IKRRenderViewExport::DidMoveToParentView();
    did_mounted_ = true;
    CreateAnimatedViewIfNeed();
}

void KRApngView::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
    if (apng_view_) {
        apng_view_->SetFrame(frame);
    }
    CreateAnimatedViewIfNeed();
}

void KRApngView::SetSrc(std::string &src) {
    if (src_ != src) {
        src_ = src;
        if (src.rfind(KR_ASSET_PREFIX, 0) == 0) {
            const auto &rootView = GetRootView().lock();
            if (rootView == nullptr) {
                return;
            }
            auto ctx = rootView->GetContext();
            if (ctx == nullptr) {
                return;
            }
            auto cfg = ctx->Config();
            if (cfg == nullptr) {
                return;
            }
            const std::string &assetsDir = cfg->GetAssetsDir();
            if (!assetsDir.empty()) {
                std::string uri = assetsDir + "/" + src.substr(KR_ASSET_PREFIX.size());
                LoadFile(uri);
                return;
            }
        } else {
            auto module_name = std::string(kNetworkModuleName);
            auto network_module = std::dynamic_pointer_cast<KRNetworkModule>(GetModule(module_name));
            if (network_module) {
                auto url = src;
                std::weak_ptr<IKRRenderViewExport> weak_self = shared_from_this();
                network_module->FetchFileByDownloadOrCache(url, [weak_self](KRAnyValue res) {
                    if (res == nullptr) {
                        return;
                    }
                    auto filePath = res->toString();
                    if (auto self = weak_self.lock()) {
                        reinterpret_cast<KRApngView *>(self.get())->LoadFile(filePath);
                    }
                });
            }
        }
    }
}

void KRApngView::SetAutoPlay(bool auto_play) {
    auto_play_ = auto_play;
    if (apng_view_) {
        apng_view_->SetAutoPlay(auto_play);
    }
}

void KRApngView::SetRepeatCount(int count) {
    if (count <= 0) {
        repeat_count_ = INT32_MAX;
    } else {
        repeat_count_ = count;
    }
    if (apng_view_) {
        apng_view_->SetRepeatCount(count);
    }
}

void KRApngView::LoadFile(std::string &file_path) {
    if (file_path.empty()) {
        // 加载失败
        return;
    }
    if (file_path != file_path_) {
        // 删除旧的APNG AnimatedAPNGView
        file_path_ = file_path;
        if (apng_view_) {
            apng_view_->Destroy();
            apng_view_ = nullptr;
        }
        CreateAnimatedViewIfNeed();
    }
}

ArkUI_NodeHandle KRApngView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_IMAGE);
}

void KRApngView::CreateAnimatedViewIfNeed() {
    if (!did_mounted_ || file_path_.empty() || GetFrame().width == 0 || apng_view_ || !GetNode()) {
        return;
    }
    apng_view_ = std::make_shared<APNGAnimateView>();
    apng_view_->Init(file_path_, GetNode(), auto_play_, GetFrame());
    apng_view_->SetRepeatCount(repeat_count_);
    std::weak_ptr<IKRRenderViewExport> weak_self = shared_from_this();
    apng_view_->RegisterLoadFailure([weak_self]() {
        if (auto self = weak_self.lock()) {
            reinterpret_cast<KRApngView *>(self.get())->FireLoadFailure();
        }
    });
    apng_view_->RegisterAnimationStart([weak_self]() {
        if (auto self = weak_self.lock()) {
            reinterpret_cast<KRApngView *>(self.get())->FireAnimationStart();
        }
    });
    apng_view_->RegisterAnimationEnd([weak_self]() {
        if (auto self = weak_self.lock()) {
            reinterpret_cast<KRApngView *>(self.get())->FireAnimationEnd();
        }
    });
}

void KRApngView::FireLoadFailure() {
    if (load_failure_callback_) {
        KRRenderValueMap map;
        this->load_failure_callback_(NewKRRenderValue(map));
    }
}

void KRApngView::FireAnimationStart() {
    if (animation_start_callback_) {
        KRRenderValueMap map;
        this->animation_start_callback_(NewKRRenderValue(map));
    }
}

void KRApngView::FireAnimationEnd() {
    if (animation_end_callback_) {
        KRRenderValueMap map;
        animation_end_callback_(NewKRRenderValue(map));
    }
}

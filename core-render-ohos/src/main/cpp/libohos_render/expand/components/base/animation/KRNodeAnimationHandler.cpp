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

#include "libohos_render/expand/components/base/animation/KRNodeAnimationHandler.h"

#include "libohos_render/export/IKRRenderViewExport.h"

void KRNodeAnimationHandler::start(std::weak_ptr<KRBasePropsHandler> target,
                                   const KRNodeAnimationOperationEndCallback &endCallback) {
    KR_LOG_DEBUG << "[KRNodeAnimationHandler] start: propKey=" << this->propKey;

    end_callback_ = endCallback;
    auto view = weakView;
    auto strongView = weakView.lock();
    if (strongView == nullptr) {
        return;
    }
    auto propsHandler = strongView->GetBasePropsHandler();
    if (propsHandler == nullptr) {
        return;
    }
    auto context = propsHandler->GetUIContext();
    auto propKey = this->propKey;
    auto propVal = this->finalValue;

    currentAnimateOption = buildAnimateOption();

    animation_ = std::make_shared<KRAnimation>(context, currentAnimateOption, [view, propKey, propVal]() {
        auto selfView = view.lock();
        if (selfView == nullptr) {
            return;
        }
        if (auto handler = selfView->GetBasePropsHandler()) {
            handler->SetPropWithoutAnimation(propKey, propVal, nullptr);
        }
    });
    std::weak_ptr<KRNodeAnimationHandler> weakSelf = shared_from_this();
    animation_->SetCompleteCallback(ArkUI_FinishCallbackType::ARKUI_FINISH_CALLBACK_LOGICALLY,
                                    [view, weakSelf, propKey]() {
                                        auto strongView = view.lock();
                                        auto self = weakSelf.lock();
                                        if (strongView != nullptr && self != nullptr) {
                                            self->playing_ = false;
                                            self->end_callback_(self->getFinishValue(false), propKey);
                                        }
                                    });
    animation_->Start();

    playing_ = true;
}

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
    // An unset ArkUI attribute is visually at its platform default, but animateTo may not
    // register that implicit value as an interpolation endpoint. Materialize it once without
    // touching properties that already have a logical or in-flight presentation state.
    propsHandler->PrepareFirstAnimationProperty(propKey);

    if (isSnap) {
        startSnapAnimation();
        return;
    }

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

void KRNodeAnimationHandler::startSnapAnimation() {
    playing_ = true;
    auto view = weakView;
    auto snapPropKey = propKey;
    auto propVal = finalValue;
    std::weak_ptr<KRNodeAnimationHandler> weakSelf = shared_from_this();
    auto applySnap = [view, weakSelf, snapPropKey, propVal]() {
        auto self = weakSelf.lock();
        if (self == nullptr) {
            return;
        }
        if (self->playing_) {
            if (auto selfView = view.lock()) {
                if (auto handler = selfView->GetBasePropsHandler()) {
                    handler->SetPropWithoutAnimation(snapPropKey, propVal, nullptr);
                }
            }
            self->playing_ = false;
            self->end_callback_(self->getFinishValue(false), snapPropKey);
        } else {
            // A newer animation replaced this delayed snap before its deadline.
            self->end_callback_(self->getFinishValue(true), snapPropKey);
        }
    };
    const auto delayMs = static_cast<int>(delayS * UNIT_S_TO_MS);
    if (delayMs > 0) {
        KRMainThread::RunOnMainThread(std::move(applySnap), delayMs);
    } else {
        // Keep completion asynchronous. The owner increments its running count after start().
        KRMainThread::RunOnMainThreadForNextLoop(std::move(applySnap));
    }
}

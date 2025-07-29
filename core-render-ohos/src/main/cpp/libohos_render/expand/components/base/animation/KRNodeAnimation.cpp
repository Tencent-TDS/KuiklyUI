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
#include "libohos_render/expand/components/base/animation/KRNodeAnimation.h"

#include "libohos_render/export/IKRRenderViewExport.h"

void KRNodeAnimation::commitAnimationOperations() {
    if (animationCommit) {
        return;
    }
    animationCommit = true;
    auto realView = weakView.lock();
    if (realView == nullptr) {
        return;
    }
    auto propsHandler = realView->GetBasePropsHandler();
    if (propsHandler == nullptr) {
        return;
    }

    std::weak_ptr<IKRNodeAnimation> weakSelf = this->shared_from_this();
    operationCallback_ = [weakSelf](bool finished, std::string propKey) {
        auto selfRef = weakSelf.lock();
        if (selfRef == nullptr) {
            return;
        }
        auto self = std::static_pointer_cast<KRNodeAnimation>(selfRef).get();
        if (self == nullptr) {
            return;
        }
        self->animationRunningCount--;
        if (self->onAnimationEndCallback) {
            self->onAnimationEndCallback(selfRef, finished, propKey, self->animationKey);
        }
    };

    for (const auto &[_, animHandler] : animationOperationMap) {
        configAnimationHandler(animHandler);
        animHandler->start(propsHandler, operationCallback_);
        animationRunningCount++;
    }
}

void KRNodeAnimation::removeFromAnimationQueue() {
    auto view = weakView.lock();
    if (view == nullptr) {
        return;
    }
    if (auto propHandler = view->GetBasePropsHandler()) {
        propHandler->RemoveAnimation(IKRNodeAnimation::shared_from_this());
    }
}

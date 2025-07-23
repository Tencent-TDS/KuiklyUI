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

#ifndef CORE_RENDER_OHOS_KRANIMATION_H
#define CORE_RENDER_OHOS_KRANIMATION_H

#include <functional>
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/utils/animate/KRAnimateOption.h"
#include "libohos_render/utils/animate/KRAnimationUtils.h"

class KRAnimation;
class KRAnimationUserData {
 public:
    std::shared_ptr<KRAnimation> animation_;
    explicit KRAnimationUserData(std::shared_ptr<KRAnimation> &animation) : animation_(animation) {}
    ~KRAnimationUserData() {
        animation_.reset();
    }
};

class KRAnimation : public std::enable_shared_from_this<KRAnimation> {
 public:
    KRAnimation(const ArkUI_ContextHandle &context_handle, const std::shared_ptr<KRAnimateOption> &animate_option,
                const std::function<void()> &animate_scope)
        : context_handle_(context_handle), animate_option_(animate_option), animate_update_callback_(animate_scope),
          complete_callback_(nullptr) {}

    ~KRAnimation() {
        complete_callback_ = nullptr;
        animate_update_callback_ = nullptr;
    }

    void Start() {
        struct ContextCallbackWrapper {
            explicit ContextCallbackWrapper(std::shared_ptr<KRAnimation> anim) : weakAnimation(anim) {
                // blank
            }
            ~ContextCallbackWrapper() {
                weakAnimation.reset();
            }
            std::weak_ptr<KRAnimation> weakAnimation;
        };
        struct ContextCallbackWrapper *wrapper = new ContextCallbackWrapper(shared_from_this());
        ArkUI_ContextCallback callback;
        callback.userData = wrapper;
        callback.callback = [](void *userData) {
            struct ContextCallbackWrapper *wrapper = static_cast<struct ContextCallbackWrapper *>(userData);
            if (wrapper == nullptr) {
                return;
            }
            auto strongSelf = wrapper->weakAnimation.lock();
            if (strongSelf && strongSelf->animate_update_callback_) {
                strongSelf->animate_update_callback_();
            }
            delete wrapper;
        };
        GetAnimateApi()->animateTo(context_handle_, animate_option_->ToArkUIAnimateOption(), &callback,
                                   complete_callback_ ? &arkui_complete_callback_ : nullptr);
    }

    void SetCompleteCallback(const ArkUI_FinishCallbackType &complete_type, const std::function<void()> &complete) {
        complete_callback_ = complete;
        std::shared_ptr<KRAnimation> self = shared_from_this();
        KRAnimationUserData *user_data = new KRAnimationUserData(self);
        arkui_complete_callback_.type = complete_type;
        arkui_complete_callback_.userData = user_data;
        arkui_complete_callback_.callback = [](void *userData) {
            KRAnimationUserData *animationUserData = (static_cast<KRAnimationUserData *>(userData));
            if (animationUserData && animationUserData->animation_) {
                animationUserData->animation_->InvokeCompleteCallback();
                delete animationUserData;
            }
        };
    }

    void InvokeCompleteCallback() {
        if (complete_callback_) {
            complete_callback_();
        }
    }

 private:
    std::shared_ptr<KRAnimateOption> animate_option_;
    ArkUI_ContextHandle context_handle_ = nullptr;
    std::function<void()> animate_update_callback_;
    std::function<void()> complete_callback_;
    ArkUI_AnimateCompleteCallback arkui_complete_callback_;
};

#endif  // CORE_RENDER_OHOS_KRANIMATION_H

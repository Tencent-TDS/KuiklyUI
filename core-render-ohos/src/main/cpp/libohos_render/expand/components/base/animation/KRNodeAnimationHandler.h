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

#ifndef CORE_RENDER_OHOS_KRNODEANIMATIONHANDLER_H
#define CORE_RENDER_OHOS_KRNODEANIMATIONHANDLER_H

#include <arkui/native_animate.h>
#include <arkui/native_interface.h>
#include <arkui/native_node.h>
#include <arkui/native_node_napi.h>
#include "libohos_render/expand/components/base/KRBasePropsHandler.h"
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/animate/KRAnimateOption.h"
#include "libohos_render/utils/animate/KRAnimation.h"

static const int32_t UNIT_S_TO_MS = 1000;             // s->ms 单位转换
static const float_t DEFAULT_ANIMATION_TEMPO = 1.0f;  // 默认动画速率

/**
 * 动画逻辑处理器基类，目前动画分为两种类型
 * 1.弹性动画，对应的处理器基类：[KRNodeSpringAnimationHandler]
 * 2.属性动画，对应的处理器基类：[KRNodePlainAnimationHandler]
 */
class KRNodeAnimationHandler : public std::enable_shared_from_this<KRNodeAnimationHandler> {
 public:
    // 动画作用的view引用
    std::weak_ptr<IKRRenderViewExport> weakView;

    float delayS = 0;
    float durationS = 0;
    bool repeatForever = false;
    bool forceNotCancel = false;

    std::string propKey;
    KRAnyValue finalValue = nullptr;

    std::shared_ptr<KRAnimateOption> currentAnimateOption;

    ~KRNodeAnimationHandler() {
        weakView.reset();
        currentAnimateOption.reset();
    }

    /**
     * 动画是否正在执行
     */
    bool isPlaying() {
        return playing_;
    }

    virtual std::shared_ptr<KRAnimateOption> buildAnimateOption() {
        auto option = std::make_shared<KRAnimateOption>();

        option->SetDuration(durationS * UNIT_S_TO_MS);
        option->SetDelay(delayS * UNIT_S_TO_MS);
        option->SetPlayMode(ARKUI_ANIMATION_PLAY_MODE_NORMAL);
        option->SetTempo(DEFAULT_ANIMATION_TEMPO);
        option->SetIterations(repeatForever ? -1 : 1);

        return option;
    }

    /**
     * 启动动画
     * @param target 动画作用的view
     * @param endCallback 动画结束回调
     */
    void start(std::weak_ptr<KRBasePropsHandler> target, const KRNodeAnimationOperationEndCallback &endCallback);
#if 0  // implementation moved to cpp file
    {
        KR_LOG_DEBUG << "[KRNodeAnimationHandler] start: propKey=" << this->propKey;

        end_callback_ = endCallback;
        auto view = weakView;
        auto context = weakView.lock()->GetUIContext();
        auto propKey = this->propKey;
        auto propVal = this->finalValue;

        currentAnimateOption = buildAnimateOption();

        animation_ = std::make_shared<KRAnimation>(context, currentAnimateOption, [view, propKey, propVal]() {
            if (auto selfView = view.lock()) {
                selfView->SetPropWithoutAnimation(propKey, propVal, nullptr);
            }
        });
        std::weak_ptr<KRNodeAnimationHandler> weakSelf = shared_from_this();
        animation_->SetCompleteCallback(ArkUI_FinishCallbackType::ARKUI_FINISH_CALLBACK_LOGICALLY,
                                        [weakSelf, propKey]() {
                                            if (auto self = weakSelf.lock()) {
                                                self->playing_ = false;
                                                self->end_callback_(self->getFinishValue(false), propKey);
                                            }
                                        });
        animation_->Start();

        playing_ = true;
    }
#endif
    /**
     * 取消动画
     */
    void cancel() {
        // do nothing (通过覆盖新差值动画取消&不复用)
    }

    bool getFinishValue(bool isCancel) {
        if (forceNotCancel) {
            return true;
        }

        return !isCancel;
    }

 private:
    bool playing_ = false;
    std::shared_ptr<KRAnimation> animation_;
    KRNodeAnimationOperationEndCallback end_callback_;
};

#endif  // CORE_RENDER_OHOS_KRNODEANIMATIONHANDLER_H

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

#ifndef CORE_RENDER_OHOS_KRNODEANIMATION_H
#define CORE_RENDER_OHOS_KRNODEANIMATION_H

#include "libohos_render/expand/components/base/KRBasePropsHandler.h"
#include "libohos_render/expand/components/base/animation/IKRNodeAnimation.h"
#include "libohos_render/expand/components/base/animation/KRNodePlainAnimation.h"
#include "libohos_render/expand/components/base/animation/KRNodeSpringAnimation.h"
#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/utils/KRRenderLoger.h"

class KRNodeAnimation : public IKRNodeAnimation {
 public:
    // 正在执行动画的数量。0代表动画结束
    int animationRunningCount = 0;

    // 动画作用的view引用
    std::weak_ptr<IKRRenderViewExport> weakView;

    // 动画结束回调
    KRNodeAnimationEndCallback onAnimationEndCallback = nullptr;

    // 动画时间
    // 该字段只在plain动画上生效
    float duration = 0;

    // 动画差值器类型
    int timingFuncType = 0;

    // 动画类型
    int animationType = 0;

    // spring动画的弹簧系数
    float damping = 0;

    // spring动画的开始速率
    float velocity = 0;

    // 动画延迟执行时间
    float delay = 0;

    // 循环动画
    bool repeatForever = 0;

    // 动画key
    std::string animationKey = "";

    std::unordered_map<std::string, KRNodeAnimationHandlerCreator> supportAnimationHandlerCreator;
    std::unordered_map<std::string, std::shared_ptr<KRNodeAnimationHandler>> animationOperationMap;

    bool animationCommit = false;

    ~KRNodeAnimation() {
        weakView.reset();
        supportAnimationHandlerCreator.clear();
        animationOperationMap.clear();
        operationCallback_ = nullptr;
        onAnimationEndCallback = nullptr;
    }

    KRNodeAnimation(const std::string &animationStr, std::weak_ptr<IKRRenderViewExport> view) {
        weakView = view;
        parseAnimation(animationStr);
        setupAnimationHandler();
    }

    /**
     * 判断propKey是否支持动画，目前支持以下属性动画类型：
     * 1.[PROP_KEY_FRAME] KRNodePlainFrameAnimationHandler | KRNodeSpringFrameAnimationHandler
     * 2.[PROP_KEY_OPACITY] KRNodePlainOpacityAnimationHandler | KRNodeSpringOpacityAnimationHandler
     * 3.[PROP_KEY_TRANSFORM] KRNodePlainTransformAnimationHandler | KRNodeSpringTransformAnimationHandler
     * 4.[PROP_KEY_BACKGROUND_COLOR] KRNodePlainBackgroundColorAnimationHandler |
     * KRNodeSpringBackgroundColorAnimationHandler
     * @param propKey 属性key
     * @ret 该propKey是否支持动画
     */
    bool isPropSupportAnimation(const std::string &propKey) override {
        return supportAnimationHandlerCreator.find(propKey) != supportAnimationHandlerCreator.end();
    }

    /**
     * 记录待执行的动画，动画执行的时机为[commitAnimation]
     * @param propKey 属性，目前支持以下属性动画
     * 1.[PROP_KEY_FRAME]
     * 2.[PROP_KEY_OPACITY]
     * 3.[PROP_KEY_TRANSFORM]
     * 4.[PROP_KEY_BACKGROUND_COLOR]
     * @param finalValue 动画最终值
     */
    void addAnimationOperation(const std::string &propKey, const KRAnyValue &prop_value) override {
        auto handlerCreator = supportAnimationHandlerCreator[propKey];
        if (handlerCreator == nullptr) {
            return;
        }

        std::shared_ptr<KRNodeAnimationHandler> handler = handlerCreator();
        handler->propKey = propKey;
        handler->finalValue = prop_value;
        animationOperationMap[propKey] = handler;
    }

    /**
     * 批量执行先前调用过[addAnimationOperation]方法记录下的动画
     */
    void commitAnimationOperations() override;
#if 0  // implentation move to cpp file
    {
        if (animationCommit) {
            return;
        }
        animationCommit = true;
        auto targetView = weakView.lock();
        if (targetView == nullptr) {
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
            animHandler->start(targetView, operationCallback_);
            animationRunningCount++;
        }
    }
#endif

    /**
     * 动画是否在播放中
     * @return
     */
    bool isPlaying() override {
        return animationRunningCount > 0;
    }

    /**
     * 获取支持动画的属性列表
     * @return
     */
    std::vector<std::string> supportPropKeys() override {
        std::vector<std::string> keys;
        for (const auto &[key, value] : animationOperationMap) {
            keys.push_back(key);
        }
        return keys;
    }

    /**
     * 批量取消正在执行的动画
     * @param cancelAnimationKeys
     */
    void cancelAnimationOperations(const std::vector<std::string> &cancelAnimationKeys) override {
        auto isAllCancel = cancelAnimationKeys.size() == 0;

        for (const auto &[animType, animHandler] : animationOperationMap) {
            if (isAllCancel) {
                animHandler->cancel();
            } else {
                auto exist = std::find(cancelAnimationKeys.begin(), cancelAnimationKeys.end(), animType) !=
                             cancelAnimationKeys.end();
                if (animHandler->isPlaying() && exist) {
                    animHandler->forceNotCancel = true;
                    animHandler->cancel();
                }
            }
        }
    }

    /**
     * 将动画配置从元素的动画队列中删除
     */
    void removeFromAnimationQueue();
#if 0  // moved to cpp file
    {
        auto view = weakView.lock();
        if (view == nullptr) {
            return;
        }
        view->RemoveAnimation(IKRNodeAnimation::shared_from_this());
    }
#endif

 private:
    // 动画配置字符串中各个属性的位置索引，空格分割
    static const int ANIMATION_TYPE_INDEX = 0;
    static const int TIMING_FUNC_TYPE_INDEX = 1;
    static const int DURATION_INDEX = 2;
    static const int DAMPING_INDEX = 3;
    static const int VELOCITY_INDEX = 4;
    static const int DELAY_INDEX = 5;
    static const int REPEAT_INDEX = 6;
    static const int ANIMATION_KEY_INDEX = 7;

    KRNodeAnimationOperationEndCallback operationCallback_;

    /**
     * 解析字符串中的动画属性，并设置动画属性
     * @param animation
     */
    void parseAnimation(const std::string &animation) {
        std::vector<std::string> animationSpilt = kuikly::util::ConvertSplit(animation, " ");
        animationType = std::stoi(animationSpilt[ANIMATION_TYPE_INDEX]);
        timingFuncType = std::stoi(animationSpilt[TIMING_FUNC_TYPE_INDEX]);
        duration = std::stof(animationSpilt[DURATION_INDEX]);
        damping = std::stof(animationSpilt[DAMPING_INDEX]);
        velocity = std::stof(animationSpilt[VELOCITY_INDEX]);
        // 兼容旧版本
        if (animationSpilt.size() > DELAY_INDEX) {
            delay = std::stof(animationSpilt[DELAY_INDEX]);
        }
        if (animationSpilt.size() > REPEAT_INDEX) {
            repeatForever = std::stoi(animationSpilt[REPEAT_INDEX]) == 1;
        }
        if (animationSpilt.size() > ANIMATION_KEY_INDEX) {
            animationKey = animationSpilt[ANIMATION_KEY_INDEX];
        }
    }

    /**
     * 初始化目前支持的属性动画
     */
    void setupAnimationHandler() {
        switch (animationType) {
        case ANIMATION_TYPE_SPRING: {
            supportAnimationHandlerCreator = {
                {PROP_KEY_FRAME, []() { return std::make_shared<KRNodeSpringFrameAnimationHandler>(); }},
                {PROP_KEY_OPACITY, []() { return std::make_shared<KRNodeSpringOpacityAnimationHandler>(); }},
                {PROP_KEY_TRANSFORM, []() { return std::make_shared<KRNodeSpringTransformAnimationHandler>(); }},
                {PROP_KEY_BACKGROUND_COLOR,
                 []() { return std::make_shared<KRNodeSpringBackgroundColorAnimationHandler>(); }},
            };
            break;
        }
        case ANIMATION_TYPE_PLAIN: {
            supportAnimationHandlerCreator = {
                {PROP_KEY_FRAME, []() { return std::make_shared<KRNodePlainFrameAnimationHandler>(); }},
                {PROP_KEY_OPACITY, []() { return std::make_shared<KRNodePlainOpacityAnimationHandler>(); }},
                {PROP_KEY_TRANSFORM, []() { return std::make_shared<KRNodePlainTransformAnimationHandler>(); }},
                {PROP_KEY_BACKGROUND_COLOR,
                 []() { return std::make_shared<KRNodePlainBackgroundColorAnimationHandler>(); }},
            };
            break;
        }
        default: {
            KR_LOG_ERROR << "[KRAnimation] " << "Unsupported animation type:" << animationType;
        }
        }
    }

    /**
     * 配置每个待执行动画的参数
     * @param handler_
     */
    void configAnimationHandler(const std::shared_ptr<KRNodeAnimationHandler> &handler_) {
        auto handler = handler_.get();
        if (handler == nullptr) {
            return;
        }

        handler->delayS = delay;
        handler->durationS = duration;
        handler->repeatForever = repeatForever;
        handler->weakView = weakView;

        std::shared_ptr<KRNodeSpringAnimationHandler> springHandler =
            std::dynamic_pointer_cast<KRNodeSpringAnimationHandler>(handler_);
        if (springHandler != nullptr) {
            springHandler->damping = damping;
            springHandler->velocity = velocity;
        } else {
            std::shared_ptr<KRNodePlainAnimationHandler> plainHandler =
                std::dynamic_pointer_cast<KRNodePlainAnimationHandler>(handler_);
            if (plainHandler != nullptr) {
                plainHandler->timingFuncType = timingFuncType;
            }
        }
    }
};

#endif  // CORE_RENDER_OHOS_KRNODEANIMATION_H

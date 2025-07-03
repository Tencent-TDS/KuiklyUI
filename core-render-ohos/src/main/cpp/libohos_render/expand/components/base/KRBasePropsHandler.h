/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_KRBASEPROPSHANDLER_H
#define CORE_RENDER_OHOS_KRBASEPROPSHANDLER_H

#include <arkui/native_gesture.h>
#include <arkui/native_type.h>
#include <string>
#include "libohos_render/expand/components/base/animation/IKRNodeAnimation.h"
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/foundation/KRRect.h"
#include "libohos_render/view/IKRRenderView.h"

extern const char *kBackgroundColor;
extern const char *kBackgroundImage;

class KRBasePropsHandler : public std::enable_shared_from_this<KRBasePropsHandler> {
 public:
    KRBasePropsHandler(std::weak_ptr<IKRRenderViewExport> weakView, ArkUI_NodeHandle node, ArkUI_ContextHandle context)
        : weakView_(weakView), node_(node), context_(context) {
        // blank
    }
    virtual ~KRBasePropsHandler() = default;

    virtual bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value, const KRRenderCallback event_call_back);
    virtual bool SetPropWithoutAnimation(const std::string &prop_key, const KRAnyValue &prop_value,
                                 const KRRenderCallback event_call_back);

    virtual bool ResetProp(const std::string &prop_key);

    virtual void OnDestroy();

    const KRRect GetFrame() const {
        return frame_;
    }
    const ArkUI_NodeHandle GetNode() const {
        return node_;
    }
    const ArkUI_ContextHandle GetUIContext() const {
        return context_;
    }

    // 新增动画配置
    virtual void AddAnimation(std::shared_ptr<IKRNodeAnimation> anim);
    // 触发所有配置动画
    virtual void CommitAnimations();
    // 删除动画配置
    virtual bool RemoveAnimation(const std::shared_ptr<IKRNodeAnimation> &animation);
    // 删除所有动画配置
    virtual void RemoveAllAnimations();
    // 动画配置完成回调
    virtual void OnAnimationCompletion(std::shared_ptr<IKRNodeAnimation> animation, bool finished, const std::string &propKey,
                               const std::string &animationKey);
    // zIndex
    int GetZIndex() {
        return z_index_;
    }
    // 是否为动画（做过动画）节点
    bool isAnimationNode() {
        return did_set_animation_;
    }

 private:
    void ResetTransformIfNeed();
    void UpdateTransform(const std::string &css_transform);

    std::weak_ptr<IKRRenderViewExport> weakView_;
    ArkUI_NodeHandle node_ = nullptr;
    KRRect frame_;

    std::string css_transform_;
    int css_overflow_ = 0;
    int z_index_ = 0;
    bool force_overflow_ = false;
    bool did_set_animation_ = false;

    ArkUI_ContextHandle context_ = nullptr;

    KRRenderCallback animation_completion_callback_;

    // 当前正在设置的动画配置
    std::shared_ptr<IKRNodeAnimation> currentAnimation = nullptr;
    // 动画配置队列
    std::vector<std::shared_ptr<IKRNodeAnimation>> animationQueue;
    // 记录当前动画配置的动画操作
    bool tryAddCurrentAnimationOperation(const std::string &prop_key, const KRAnyValue &prop_value);
};


class KRArkTSViewBasePropsHandler : public KRBasePropsHandler{
 public:
    KRArkTSViewBasePropsHandler(std::weak_ptr<IKRRenderViewExport> weakView)
        : KRBasePropsHandler(weakView, nullptr, nullptr) {
        // blank
    }
    virtual ~KRArkTSViewBasePropsHandler() = default;

    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value, const KRRenderCallback event_call_back) override {
        return false;
    }
    bool SetPropWithoutAnimation(const std::string &prop_key, const KRAnyValue &prop_value,
                                 const KRRenderCallback event_call_back) override {
        return false;
    }

    bool ResetProp(const std::string &prop_key) override {
        return false;
    }

    void OnDestroy() override {
        // blank
    }

    // 新增动画配置
    void AddAnimation(std::shared_ptr<IKRNodeAnimation> anim) override {
        // blank
    }
    // 触发所有配置动画
    void CommitAnimations() override {
        // blank
    }
    // 删除动画配置
    bool RemoveAnimation(const std::shared_ptr<IKRNodeAnimation> &animation) override {
        return false;
    }
    // 删除所有动画配置
    void RemoveAllAnimations() override {
        // blank
    }
    // 动画配置完成回调
    void OnAnimationCompletion(std::shared_ptr<IKRNodeAnimation> animation, bool finished, const std::string &propKey,
                               const std::string &animationKey) override {
        // blank
    }
};
#endif  // CORE_RENDER_OHOS_KRBASEPROPSHANDLER_H

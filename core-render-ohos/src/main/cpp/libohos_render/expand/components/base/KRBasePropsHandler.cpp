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

#include "libohos_render/expand/components/base/KRBasePropsHandler.h"

#include <multimedia/image_framework/image/image_common.h>
#include <cfloat>
#include "libohos_render/foundation/KRConfig.h"
#include "libohos_render/foundation/KRRect.h"
#include "libohos_render/utils/KREventUtil.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/KRViewUtil.h"

const char *kBackgroundColor = "backgroundColor";
const char *kFrame = "frame";
const char *kBorderRadius = "borderRadius";
const char *kBorder = "border";
const char *kBackgroundImage = "backgroundImage";
const char *kTransform = "transform";
const char *kOpacity = "opacity";
const char *kVisibility = "visibility";
const char *kOverflow = "overflow";
const char *kZIndex = "zIndex";
const char *kTouchEnable = "touchEnable";
const char *kAccessibility = "accessibility";
const char *kBoxShadow = "boxShadow";
const char *KAnimation = "animation";
const char *kAnimationCompletion = "animationCompletion";

// 动画完成回调事件参数
constexpr char kParamKeyFinish[] = "finish";
constexpr char kParamKeyAnimationKey[] = "animationKey";
constexpr char kParamKeyAttr[] = "attr";

// kuikly::util::UpdateNodeBorder
void KRBasePropsHandler::OnDestroy() {
    node_ = nullptr;
    context_ = nullptr;
    animation_completion_callback_ = nullptr;
}

bool KRBasePropsHandler::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                 const KRRenderCallback event_call_back) {
    if (tryAddCurrentAnimationOperation(prop_key, prop_value)) {
        return true;
    }

    return SetPropWithoutAnimation(prop_key, prop_value, event_call_back);
}

bool KRBasePropsHandler::SetPropWithoutAnimation(const std::string &prop_key, const KRAnyValue &prop_value,
                                                 const KRRenderCallback event_call_back) {
    if (node_ == nullptr) {
        return false;
    }
    if (strcmp(prop_key.c_str(), kBackgroundColor) == 0) {  // 背景色
        kuikly::util::UpdateNodeBackgroundColor(node_, kuikly::util::ConvertToHexColor(prop_value->toString()));
        return true;
    }
    if (strcmp(prop_key.c_str(), kBorderRadius) == 0) {  // 圆角
        auto borderRadiuses = kuikly::util::ConverToBorderRadiuses(prop_value->toString());
        kuikly::util::UpdateNodeBorderRadius(node_, borderRadiuses);
        if (!borderRadiuses.isAllZero()) {  // 圆角不为0，需要强制clip 子孩子，避免超出自身边界
            force_overflow_ = true;
            kuikly::util::UpdateNodeOverflow(node_, 1);
        } else {
            force_overflow_ = false;
            kuikly::util::UpdateNodeOverflow(node_, css_overflow_);
        }
        return true;
    }
    if (strcmp(prop_key.c_str(), kBorder) == 0) {  // 边框样式
        kuikly::util::UpdateNodeBorder(node_, prop_value->toString());
        return true;
    }
    if (strcmp(prop_key.c_str(), kFrame) == 0) {
        if (prop_value->isString()) {
            KRRect frame;
            const std::string &s = prop_value->toString();
            memcpy(&frame, s.data(), s.size());
            ResetTransformIfNeed();
            kuikly::util::UpdateNodeFrame(node_, frame);
            frame_ = frame;
            if (css_transform_.length()) {
                UpdateTransform(css_transform_);
            }
            return true;
        }
    }
    if (strcmp(prop_key.c_str(), kBackgroundImage) == 0) {  // 背景渐变
        kuikly::util::UpdateNodeBackgroundImage(node_, prop_value->toString());
        return true;
    }
    if (strcmp(prop_key.c_str(), kTransform) == 0) {  // transform(旋转，位移，缩放，倾斜) （+anchor）
        css_transform_ = prop_value->toString();
        UpdateTransform(css_transform_);
        return true;
    }
    if (strcmp(prop_key.c_str(), kOpacity) == 0) {  // 透明度
        kuikly::util::UpdateNodeOpacity(node_, prop_value->toDouble());
        return true;
    }

    if (strcmp(prop_key.c_str(), kVisibility) == 0) {  // Visibility
        kuikly::util::UpdateNodeVisibility(node_, prop_value->toInt());
        return true;
    }

    if (strcmp(prop_key.c_str(), kOverflow) == 0) {  // 裁剪
        css_overflow_ = prop_value->toInt();
        kuikly::util::UpdateNodeOverflow(node_, css_overflow_);
        if (force_overflow_) {
            kuikly::util::UpdateNodeOverflow(node_, 1);
        }
        return true;
    }

    if (strcmp(prop_key.c_str(), kZIndex) == 0) {  // z-index
        z_index_ = prop_value->toInt();
        kuikly::util::UpdateNodeZIndex(node_, z_index_);
        return true;
    }

    if (strcmp(prop_key.c_str(), kTouchEnable) == 0) {  // 禁用手势
        kuikly::util::UpdateNodeHitTest(node_, prop_value->toBool());
        return true;
    }

    if (strcmp(prop_key.c_str(), kAccessibility) == 0) {  // 无障碍化
        kuikly::util::UpdateNodeAccessibility(node_, prop_value->toString());
        return true;
    }

    if (strcmp(prop_key.c_str(), kBoxShadow) == 0) {  // 阴影
        kuikly::util::UpdateNodeBoxShadow(node_, prop_value->toString());
        return true;
    }

    if (strcmp(prop_key.c_str(), KAnimation) == 0) {
        auto animationStr = prop_value->toString();
        kuikly::util::SetNodeAnimation(weakView_, &animationStr);
        return true;
    }

    if (strcmp(prop_key.c_str(), kAnimationCompletion) == 0) {
        animation_completion_callback_ = event_call_back;
        return true;
    }

    return false;
}

bool KRBasePropsHandler::ResetProp(const std::string &prop_key) {
    if (node_ == nullptr) {
        return false;
    }
    force_overflow_ = false;
    if (strcmp(prop_key.c_str(), kBackgroundColor) == 0) {
        kuikly::util::UpdateNodeBackgroundColor(node_, 0x00000000);  // 透明
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_BACKGROUND_COLOR);
        return true;
    }
    if (strcmp(prop_key.c_str(), kBorderRadius) == 0) {  // 圆角
        kuikly::util::UpdateNodeBorderRadius(node_, KRBorderRadiuses());
        kuikly::util::UpdateNodeOverflow(node_, 0);
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_CLIP);
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_BORDER_RADIUS);
        return true;
    }
    if (strcmp(prop_key.c_str(), kBorder) == 0) {
        kuikly::util::UpdateNodeBorder(node_, "0 solid 0");
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_BORDER_WIDTH);
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_BORDER_COLOR);
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_BORDER_STYLE);
        return true;
    }
    if (strcmp(prop_key.c_str(), kFrame) == 0) {
        KRRect frame;
        kuikly::util::UpdateNodeFrame(node_, frame);
        frame_ = frame;
        return true;
    }

    if (strcmp(prop_key.c_str(), kBackgroundImage) == 0) {
        kuikly::util::UpdateNodeBackgroundImage(node_, "8,0 0,0 1");  // 重置为不渐变，且透明
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_LINEAR_GRADIENT);
        return true;
    }

    if (strcmp(prop_key.c_str(), kTransform) == 0) {
        ResetTransformIfNeed();
        css_transform_ = "";
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_TRANSFORM_CENTER);
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_TRANSFORM);
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_ROTATE);
        return true;
    }

    if (strcmp(prop_key.c_str(), kOpacity) == 0) {
        kuikly::util::UpdateNodeOpacity(node_, 1);
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_OPACITY);
        return true;
    }

    if (strcmp(prop_key.c_str(), kVisibility) == 0) {  // 透明度
        kuikly::util::UpdateNodeVisibility(node_, 1);
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_VISIBILITY);
        return true;
    }

    if (strcmp(prop_key.c_str(), kOverflow) == 0) {  // 裁剪子孩子
        kuikly::util::UpdateNodeOverflow(node_, 0);
        css_overflow_ = 0;
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_CLIP);
        return true;
    }

    if (strcmp(prop_key.c_str(), kZIndex) == 0) {  // z-index
        z_index_ = 0;
        kuikly::util::UpdateNodeZIndex(node_, 0);
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_Z_INDEX);
        return true;
    }

    if (strcmp(prop_key.c_str(), kTouchEnable) == 0) {  // 禁用手势
        kuikly::util::UpdateNodeHitTest(node_, true);
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_ENABLED);
        return true;
    }
    if (strcmp(prop_key.c_str(), kAccessibility) == 0) {  // 无障碍化
        kuikly::util::UpdateNodeAccessibility(node_, "");
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_ACCESSIBILITY_TEXT);
        return true;
    }

    if (strcmp(prop_key.c_str(), kBoxShadow) == 0) {  // 阴影
        kuikly::util::UpdateNodeBoxShadow(node_, "0 0 0 0");
        kuikly::util::GetNodeApi()->resetAttribute(node_, NODE_CUSTOM_SHADOW);
    }

    if (strcmp(prop_key.c_str(), KAnimation) == 0) {
        kuikly::util::SetNodeAnimation(weakView_, nullptr);
        return true;
    }

    return false;
}

void KRBasePropsHandler::ResetTransformIfNeed() {
    if (css_transform_.length()) {
        auto default_css_transform = "0|1 1|0 0|0.5 0.5|0 0";  // 默认值
        UpdateTransform(default_css_transform);
    }
}

void KRBasePropsHandler::UpdateTransform(const std::string &css_transform) {
    if (css_transform.length()) {
        double dpi = KRConfig::GetDpi();
        kuikly::util::UpdateNodeTransform(node_, css_transform_, KRSize(GetFrame().width * dpi, GetFrame().height * dpi));
    }
}

void KRBasePropsHandler::AddAnimation(std::shared_ptr<IKRNodeAnimation> anim) {
    // 判断是否已经存在
    auto exist = std::find(animationQueue.begin(), animationQueue.end(), anim) != animationQueue.end();
    if (!exist) {
        currentAnimation = anim;
        did_set_animation_ = true;
        animationQueue.push_back(anim);
    }
}

void KRBasePropsHandler::CommitAnimations() {
    auto commitAnimation = currentAnimation;
    currentAnimation = nullptr;
    for (auto it = animationQueue.begin(); it != animationQueue.end();) {
        auto item = it->get();
        if (item != nullptr) {
            item->commitAnimationOperations();
        }
        it++;
    }
}

bool KRBasePropsHandler::RemoveAnimation(const std::shared_ptr<IKRNodeAnimation> &animation) {
    auto it = std::find_if(animationQueue.begin(), animationQueue.end(),
                           [&](const std::shared_ptr<IKRNodeAnimation> &anim) { return anim == animation; });
    if (it != animationQueue.end()) {
        animationQueue.erase(it);
        if (animation == currentAnimation) {
            currentAnimation = nullptr;
        }
        return true;
    }
    return false;
}

void KRBasePropsHandler::RemoveAllAnimations() {
    // 遍历动画队列，停止所有动画
    for (auto &animation : animationQueue) {
        IKRNodeAnimation *item = animation.get();
        if (item != nullptr) {
            item->cancelAnimationOperations(std::vector<std::string>());
        }
    }
    // 清空动画队列
    animationQueue.clear();
    currentAnimation = nullptr;
}

void KRBasePropsHandler::OnAnimationCompletion(std::shared_ptr<IKRNodeAnimation> animation, bool finished,
                                               const std::string &propKey, const std::string &animationKey) {
    if (animation_completion_callback_ == nullptr) {
        return;
    }

    KRRenderValueMap params;
    if (finished) {
        params[kParamKeyFinish] = NewKRRenderValue(1);
    }
    params[kParamKeyAttr] = NewKRRenderValue(propKey);
    params[kParamKeyAnimationKey] = NewKRRenderValue(animationKey);
    animation_completion_callback_(NewKRRenderValue(params));
}

/**
 * 记录当前动画配置的动画操作
 * @param prop_key
 * @param prop_value
 * @return true 代表已设置动画，false代表未设置动画
 */
bool KRBasePropsHandler::tryAddCurrentAnimationOperation(const std::string &prop_key, const KRAnyValue &prop_value) {
    if (currentAnimation == nullptr) {
        return false;
    }

    if (!currentAnimation->isPropSupportAnimation(prop_key)) {
        return false;
    }

    currentAnimation->addAnimationOperation(prop_key, prop_value);
    return true;
}

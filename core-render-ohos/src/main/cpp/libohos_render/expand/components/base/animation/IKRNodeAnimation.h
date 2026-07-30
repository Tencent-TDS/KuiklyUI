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

#ifndef CORE_RENDER_OHOS_IKRNODEANIMATION_H
#define CORE_RENDER_OHOS_IKRNODEANIMATION_H

#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/utils/KRRenderLoger.h"

// 支持的动画类型
static const int ANIMATION_TYPE_PLAIN = 0;
static const int ANIMATION_TYPE_SPRING = 1;

// 支持动画的属性名称
static const char PROP_KEY_FRAME[] = "frame";
static const char PROP_KEY_OPACITY[] = "opacity";
static const char PROP_KEY_TRANSFORM[] = "transform";
static const char PROP_KEY_BACKGROUND_COLOR[] = "backgroundColor";

class IKRNodeAnimation : public std::enable_shared_from_this<IKRNodeAnimation> {
 public:
    virtual bool isPropSupportAnimation(const std::string &propKey) = 0;
    virtual std::vector<std::string> supportPropKeys() = 0;
    virtual bool isPlaying() = 0;
    virtual void addAnimationOperation(const std::string &propKey, const KRAnyValue &prop_value) = 0;
    virtual void commitAnimationOperations() = 0;
    virtual void cancelAnimationOperations(const std::vector<std::string> &cancelAnimationKeys) = 0;
};

// 动画处理器类型
class KRNodeAnimationHandler;

// 动画处理器创建函数类型
typedef std::function<std::shared_ptr<KRNodeAnimationHandler>()> KRNodeAnimationHandlerCreator;

// 动画（任意Operation）结束回调函数类型
using KRNodeAnimationEndCallback = std::function<void(std::shared_ptr<IKRNodeAnimation> animation, bool finished,
                                                      const std::string &propKey, const std::string &animationKey)>;

// // 单个动画Operation结束回调
using KRNodeAnimationOperationEndCallback = std::function<void(bool finished, const std::string &propKey)>;

#endif  // CORE_RENDER_OHOS_IKRNODEANIMATION_H

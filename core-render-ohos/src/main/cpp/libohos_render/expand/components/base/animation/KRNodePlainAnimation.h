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

#ifndef CORE_RENDER_OHOS_KRNODEPLAINANIMATION_H
#define CORE_RENDER_OHOS_KRNODEPLAINANIMATION_H

#include <sys/stat.h>
#include "libohos_render/expand/components/base/animation/IKRNodeAnimation.h"
#include "libohos_render/expand/components/base/animation/KRNodeAnimationHandler.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/animate/KRAnimateOption.h"
#include "libohos_render/utils/animate/KRAnimation.h"
#include "libohos_render/utils/animate/KRAnimationUtils.h"

static const int TIMING_FUNC_TYPE_LINEAR = 0;                 // 线性
static const int TIMING_FUNC_TYPE_ACCELERATE = 1;             // 加速
static const int TIMING_FUNC_TYPE_DECELERATE = 2;             // 减速
static const int TIMING_FUNC_TYPE_ACCELERATE_DECELERATE = 3;  // 先加速后减速

// 动画曲线对应，https://developer.huawei.com/consumer/cn/doc/harmonyos-references-V5/_ark_u_i___native_module-V5#ZH-CN_TOPIC_0000001884918042__arkui_animationcurve
static std::unordered_map<int, ArkUI_AnimationCurve> TIME_FUNC_2_ARKUI_CURVE_MAP = {
    {TIMING_FUNC_TYPE_LINEAR, ARKUI_CURVE_LINEAR},
    {TIMING_FUNC_TYPE_ACCELERATE, ARKUI_CURVE_FAST_OUT_LINEAR_IN},
    {TIMING_FUNC_TYPE_DECELERATE, ARKUI_CURVE_LINEAR_OUT_SLOW_IN},
    {TIMING_FUNC_TYPE_ACCELERATE_DECELERATE, ARKUI_CURVE_EASE},
};

/**
 * 属性动画基类
 */
class KRNodePlainAnimationHandler : public KRNodeAnimationHandler {
 public:
    int timingFuncType = TIMING_FUNC_TYPE_LINEAR;

    std::shared_ptr<KRAnimateOption> buildAnimateOption() override {
        auto option = KRNodeAnimationHandler::buildAnimateOption();
        auto curve = TIME_FUNC_2_ARKUI_CURVE_MAP[timingFuncType];
        option->SetCurve(curve);
        return option;
    }
};

/**
 * Frame属性动画处理器
 */
class KRNodePlainFrameAnimationHandler : public KRNodePlainAnimationHandler {};

/**
 * alpha属性动画处理器
 */
class KRNodePlainOpacityAnimationHandler : public KRNodePlainAnimationHandler {};

/**
 * transform属性动画处理器
 */
class KRNodePlainTransformAnimationHandler : public KRNodePlainAnimationHandler {
    std::shared_ptr<KRAnimateOption> buildAnimateOption() override {
        return KRNodePlainAnimationHandler::buildAnimateOption();
    }
};

/**
 * 背景颜色属性动画处理器
 */
class KRNodePlainBackgroundColorAnimationHandler : public KRNodePlainAnimationHandler {};

#endif  // CORE_RENDER_OHOS_KRNODEPLAINANIMATION_H

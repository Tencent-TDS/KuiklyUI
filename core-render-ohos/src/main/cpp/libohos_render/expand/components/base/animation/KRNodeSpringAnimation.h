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

#ifndef CORE_RENDER_OHOS_KRNODESPRINGANIMATION_H
#define CORE_RENDER_OHOS_KRNODESPRINGANIMATION_H

#include "libohos_render/expand/components/base/animation/IKRNodeAnimation.h"
#include "libohos_render/expand/components/base/animation/KRNodeAnimationHandler.h"
#include "libohos_render/utils/KRRenderLoger.h"

/**
 * 弹性动画基类
 */
class KRNodeSpringAnimationHandler : public KRNodeAnimationHandler {
 public:
    float damping = 0;
    float velocity = 0;
    ArkUI_CurveHandle curveHandle = nullptr;

    std::shared_ptr<KRAnimateOption> buildAnimateOption() override {
        auto option = KRNodeAnimationHandler::buildAnimateOption();
        /**
         * mass 0.2 调参对齐iOS 、Android
         * stiffness 1.2 调参对齐iOS 、Android
         */
        auto curve = OH_ArkUI_Curve_CreateSpringCurve(velocity, 0.2, 1.2, damping);
        option->SetSpringCurve(curve);
        SetCurHandle(curve);
        return option;
    }

    void SetCurHandle(ArkUI_CurveHandle curve) {
        if (curveHandle) {
            OH_ArkUI_Curve_DisposeCurve(curveHandle);
            curveHandle = nullptr;
        }
        curveHandle = curve;
    }

    ~KRNodeSpringAnimationHandler() {
        SetCurHandle(nullptr);
    }
};

/**
 * Frame弹性帧动画处理器
 */
class KRNodeSpringFrameAnimationHandler : public KRNodeSpringAnimationHandler {};

/**
 * alpha弹性帧动画处理器
 */
class KRNodeSpringOpacityAnimationHandler : public KRNodeSpringAnimationHandler {};

/**
 * transform弹性帧动画处理器
 */
class KRNodeSpringTransformAnimationHandler : public KRNodeSpringAnimationHandler {};

/**
 * 背景颜色弹性帧动画处理器
 */
class KRNodeSpringBackgroundColorAnimationHandler : public KRNodeSpringAnimationHandler {};

#endif  // CORE_RENDER_OHOS_KRNODESPRINGANIMATION_H

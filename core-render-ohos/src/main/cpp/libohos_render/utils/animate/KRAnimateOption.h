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

#ifndef CORE_RENDER_OHOS_KRANIMATEOPTION_H
#define CORE_RENDER_OHOS_KRANIMATEOPTION_H

#include <arkui/native_animate.h>

class KRAnimateOption {
 public:
    KRAnimateOption() {
        arkui_animate_option_ = OH_ArkUI_AnimateOption_Create();
    }

    void SetDuration(int32_t duration) {
        OH_ArkUI_AnimateOption_SetDuration(arkui_animate_option_, duration);
    }

    void SetTempo(float value) {
        OH_ArkUI_AnimateOption_SetTempo(arkui_animate_option_, value);
    }

    void SetCurve(const ArkUI_AnimationCurve &value) {
        OH_ArkUI_AnimateOption_SetCurve(arkui_animate_option_, value);
    }

    void SetSpringCurve(const ArkUI_CurveHandle value) {
        OH_ArkUI_AnimateOption_SetICurve(arkui_animate_option_, value);
    }

    void SetDelay(int32_t value) {
        OH_ArkUI_AnimateOption_SetDelay(arkui_animate_option_, value);
    }

    void SetIterations(int32_t value) {
        OH_ArkUI_AnimateOption_SetIterations(arkui_animate_option_, value);
    }

    void SetPlayMode(const ArkUI_AnimationPlayMode &value) {
        OH_ArkUI_AnimateOption_SetPlayMode(arkui_animate_option_, value);
    }

    void SetExpectedFrameRateRange(ArkUI_ExpectedFrameRateRange *value) {
        OH_ArkUI_AnimateOption_SetExpectedFrameRateRange(arkui_animate_option_, value);
    }

    ArkUI_AnimateOption *ToArkUIAnimateOption() {
        return arkui_animate_option_;
    }

    ~KRAnimateOption() {
        OH_ArkUI_AnimateOption_Dispose(arkui_animate_option_);
    }

 private:
    ArkUI_AnimateOption *arkui_animate_option_ = nullptr;
};

#endif  // CORE_RENDER_OHOS_KRANIMATEOPTION_H

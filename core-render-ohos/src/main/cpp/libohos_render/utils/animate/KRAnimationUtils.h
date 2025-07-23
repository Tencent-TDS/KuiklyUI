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

#ifndef CORE_RENDER_OHOS_KRANIMATIONUTILS_H
#define CORE_RENDER_OHOS_KRANIMATIONUTILS_H

#include <arkui/native_animate.h>
#include <arkui/native_interface.h>

static ArkUI_NativeAnimateAPI_1 *GetAnimateApi() {
    static ArkUI_NativeAnimateAPI_1 *animate_api = nullptr;
    if (!animate_api) {
        OH_ArkUI_GetModuleInterface(ARKUI_NATIVE_ANIMATE, ArkUI_NativeAnimateAPI_1, animate_api);
    }
    return animate_api;
}

#endif  // CORE_RENDER_OHOS_KRANIMATIONUTILS_H

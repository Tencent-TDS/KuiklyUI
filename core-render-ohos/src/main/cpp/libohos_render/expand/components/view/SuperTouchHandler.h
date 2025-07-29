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

#ifndef CORE_RENDER_OHOS_SUPERTOUCHHANDLER_H
#define CORE_RENDER_OHOS_SUPERTOUCHHANDLER_H

#include "libohos_render/export/IKRRenderViewExport.h"
#include <arkui/native_gesture.h>
#include <unordered_set>
class SuperTouchHandler {
public:
    SuperTouchHandler() {}
    ~SuperTouchHandler();
    void PreventTouch(bool prevent);
    bool IsPreventTouch() { return prevent_touch_; }
    void SetNativeTouchConsumer(const std::shared_ptr<IKRRenderViewExport> &native_touch_consumer);
    void ClearNativeTouchConsumer(const std::shared_ptr<IKRRenderViewExport> &native_touch_consumer);
    void CollectGestureRecognizer(ArkUI_GestureRecognizer *recognizer);
    bool IsCanceled();
    bool ProcessCancel();
    void ResetCancel();
private:
    std::unordered_set<ArkUI_GestureRecognizer *> gesture_recognizers_;
    bool prevent_touch_ = false;
    std::weak_ptr<IKRRenderViewExport> native_touch_consumer_;
    bool super_touch_canceled_ = false;
};

#endif //CORE_RENDER_OHOS_SUPERTOUCHHANDLER_H

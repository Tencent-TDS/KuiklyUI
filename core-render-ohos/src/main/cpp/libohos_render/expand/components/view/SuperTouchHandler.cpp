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

#include "SuperTouchHandler.h"
SuperTouchHandler::~SuperTouchHandler() {
    for (auto recognizer : gesture_recognizers_) {
        OH_ArkUI_SetGestureRecognizerEnabled(recognizer, true);
        OH_ArkUI_SetArkUIGestureRecognizerDisposeNotify(
            recognizer, [](ArkUI_GestureRecognizer *recognizer, void *userData) {}, nullptr);
    }
    gesture_recognizers_.clear();
}

void SuperTouchHandler::PreventTouch(bool prevent) {
    if (prevent_touch_ == prevent) {
        return;
    }
    prevent_touch_ = prevent;
    for (auto recognizer : gesture_recognizers_) {
        OH_ArkUI_SetGestureRecognizerEnabled(recognizer, !prevent);
    }
}

void SuperTouchHandler::SetNativeTouchConsumer(const std::shared_ptr<IKRRenderViewExport> &native_touch_consumer) {
    native_touch_consumer_ = native_touch_consumer;
}

void SuperTouchHandler::ClearNativeTouchConsumer(const std::shared_ptr<IKRRenderViewExport> &native_touch_consumer) {
    if (native_touch_consumer_.lock() == native_touch_consumer) {
        native_touch_consumer_.reset();
    }
}

void SuperTouchHandler::CollectGestureRecognizer(ArkUI_GestureRecognizer *recognizer) {
    if (gesture_recognizers_.find(recognizer) != gesture_recognizers_.end()) {
        // already collected
        return;
    }
    // TODO: consider to erase the recognizer when it's owner ark_ui_node detached
    gesture_recognizers_.insert(recognizer);
    OH_ArkUI_SetArkUIGestureRecognizerDisposeNotify(
        recognizer,
        [](ArkUI_GestureRecognizer *recognizer, void *userData) {
            auto self = static_cast<SuperTouchHandler *>(userData);
            self->gesture_recognizers_.erase(recognizer);
        },
        this);
    if (prevent_touch_) {
        OH_ArkUI_SetGestureRecognizerEnabled(recognizer, false);
    }
}

bool SuperTouchHandler::IsCanceled() { return super_touch_canceled_; }

bool SuperTouchHandler::ProcessCancel() {
    if (!super_touch_canceled_ && !native_touch_consumer_.expired()) {
        super_touch_canceled_ = true;
        return true;
    }
    return false;
}

void SuperTouchHandler::ResetCancel() { super_touch_canceled_ = false; }
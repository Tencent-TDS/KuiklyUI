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

#ifndef CORE_RENDER_OHOS_KREVENTUTIL_H
#define CORE_RENDER_OHOS_KREVENTUTIL_H

#include <arkui/native_gesture.h>
#include <arkui/native_node.h>
#include <arkui/native_type.h>
#include <string>
#include "libohos_render/foundation/KRPoint.h"
#include "libohos_render/foundation/KRSize.h"

namespace kuikly {
namespace util {

class ArkUINativeGestureAPI {
 public:
    ArkUI_GestureRecognizer *createTapGesture(int32_t countNum, int32_t fingersNum);
    ArkUI_GestureRecognizer *createLongPressGesture(int32_t fingersNum, bool repeatResult, int32_t durationNum);
    ArkUI_GestureRecognizer *createPanGesture(int32_t fingersNum, ArkUI_GestureDirectionMask directions,
                                              double distanceNum);
    ArkUI_GestureRecognizer *createPinchGesture(int32_t fingersNum, double distanceNum);
    ArkUI_GestureRecognizer *createRotationGesture(int32_t fingersNum, double angleNum);
    ArkUI_GestureRecognizer *createSwipeGesture(int32_t fingersNum, ArkUI_GestureDirectionMask directions,
                                                double speedNum);
    ArkUI_GestureRecognizer *createGroupGesture(ArkUI_GroupGestureMode gestureMode);
    void dispose(ArkUI_GestureRecognizer *recognizer);
    int32_t addChildGesture(ArkUI_GestureRecognizer *group, ArkUI_GestureRecognizer *child);
    int32_t removeChildGesture(ArkUI_GestureRecognizer *group, ArkUI_GestureRecognizer *child);
    int32_t setGestureEventTarget(ArkUI_GestureRecognizer *recognizer, ArkUI_GestureEventActionTypeMask actionTypeMask,
                                  void *extraParams,
                                  void (*targetReceiver)(ArkUI_GestureEvent *event, void *extraParams));
    int32_t addGestureToNode(ArkUI_NodeHandle node, ArkUI_GestureRecognizer *recognizer, ArkUI_GesturePriority mode,
                             ArkUI_GestureMask mask);
    int32_t removeGestureFromNode(ArkUI_NodeHandle node, ArkUI_GestureRecognizer *recognizer);
    int32_t setGestureInterrupterToNode(ArkUI_NodeHandle node, void* userData,
                                        ArkUI_GestureInterruptResult (*interrupter)(ArkUI_GestureInterruptInfo *info));
    ArkUI_GestureRecognizerType getGestureType(ArkUI_GestureRecognizer *recognizer);
    int32_t
    setInnerGestureParallelTo(ArkUI_NodeHandle node, void *userData,
                              ArkUI_GestureRecognizer *(*parallelInnerGesture)(ArkUI_ParallelInnerGestureEvent *event));
    ArkUI_GestureRecognizer *createTapGestureWithDistanceThreshold(int32_t countNum, int32_t fingersNum,
                                                                   double distanceThreshold);

    ArkUI_NodeHandle GetAttachedNodeForRecognizer(ArkUI_GestureRecognizer *recognizer);
    std::string GetGestureBindNodeId(ArkUI_GestureRecognizer *recognizer);
    static ArkUINativeGestureAPI *GetInstance();

 private:
    ArkUINativeGestureAPI();
    ~ArkUINativeGestureAPI() = default;
    ArkUI_NativeGestureAPI_1 *impl_ = nullptr;
    ArkUI_NativeGestureAPI_2 *impl2_ = nullptr;

    // map from gesture to parent gesture
    std::unordered_map<void *, void *> gestureToParentGesture_;
    // map from gesture to attached node
    std::unordered_map<void *, void *> gestureToNode_;
};

ArkUINativeGestureAPI *GetGestureApi();

ArkUI_NodeHandle GetArkUINodeHandle(ArkUI_NodeEvent *event);

ArkUI_NodeEventType GetArkUINodeEventType(ArkUI_NodeEvent *event);

int32_t GetImageLoadSuccessStatusCode(ArkUI_NodeEvent *event);

bool IsImageLoadSuccessStatus(ArkUI_NodeEvent *event);

KRSize GetArkUINodeImagePicSize(ArkUI_NodeEvent *event);

ArkUI_UIInputEvent *GetArkUIInputEvent(ArkUI_NodeEvent *event);

int32_t GetArkUIInputEventAction(ArkUI_UIInputEvent *event);

int GetArkUIInputEventPointerCount(ArkUI_UIInputEvent *event);

int64_t GetArkUIInputEventTime(ArkUI_UIInputEvent *event);

int GetArkUIInputEventPointerId(ArkUI_UIInputEvent *event, int pointer_index);

KRPoint GetArkUIInputEventPoint(ArkUI_UIInputEvent *event, int pointer_index);

KRPoint GetArkUIInputEventWindowPoint(ArkUI_UIInputEvent *event, int pointer_index);

KRPoint GetArkUIEventPoint(ArkUI_NodeEvent *event);

KRPoint GetArkUIGestureEventPoint(ArkUI_GestureEvent *event);

KRPoint GetArkUIGestureEventWindowPoint(ArkUI_GestureEvent *event);

ArkUI_GestureEventActionType GetArkUIGestureActionType(ArkUI_GestureEvent *event);

std::string GetArkUIGestureActionState(ArkUI_GestureEvent *event);

float GetArkUIGesturePinchScale(ArkUI_GestureEvent *event);

void *GetUserData(ArkUI_NodeEvent *event);

void StopPropagation(ArkUI_NodeEvent *event);

}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KREVENTUTIL_H

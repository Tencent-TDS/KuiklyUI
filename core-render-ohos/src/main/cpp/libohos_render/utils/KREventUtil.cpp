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

#include "libohos_render/utils/KREventUtil.h"

#include <mutex>

#include <info/application_target_sdk_version.h>
#include "libohos_render/utils/KRThreadChecker.h"
#include "libohos_render/utils/KRViewUtil.h"

static_assert(OH_CURRENT_API_VERSION >= 18, "DevEco Studio too old, please upgrade to 5.1.0 or newer!");

// typically the levels of gestures are 2 or 3, set max to 10 should be good enough
#define GESTURE_MAX_LEVELS 10

#define MAX_NODE_ID_LENGTH 128

#if KUIKLY_ENABLE_ARKUI_NODE_VALID_CHECK
#define KUIKLY_CHECK_NODE_OR_RETURN(NODE)                                                                              \
    do {                                                                                                               \
        if (!ArkUINativeNodeAPI::GetInstance()->IsNodeAlive(NODE)) {                                                   \
            KR_LOG_ERROR << "Node DEAD";                                                                               \
            assert(false);                                                                                             \
            return;                                                                                                    \
        }                                                                                                              \
    \
} while (0)

#define KUIKLY_CHECK_NODE_OR_RETURN_ERROR(NODE)                                                                        \
    do {                                                                                                               \
        if (!ArkUINativeNodeAPI::GetInstance()->IsNodeAlive(NODE)) {                                                   \
            KR_LOG_ERROR << "Node DEAD";                                                                               \
            assert(false);                                                                                             \
            return ARKUI_ERROR_CODE_PARAM_INVALID;                                                                     \
        }                                                                                                              \
    \
} while (0)

#define KUIKLY_CHECK_NODE_OR_RETURN_NULL(NODE)                                                                         \
    do {                                                                                                               \
        if (!ArkUINativeNodeAPI::GetInstance()->IsNodeAlive(NODE)) {                                                   \
            KR_LOG_ERROR << "Node DEAD";                                                                               \
            assert(false);                                                                                             \
            return nullptr;                                                                                            \
        }                                                                                                              \
    \
} while (0)

#define KUIKLY_CHECK_NODE_OR_RETURN_ZERO(NODE)                                                                         \
    do {                                                                                                               \
        if (!ArkUINativeNodeAPI::GetInstance()->IsNodeAlive(NODE)) {                                                   \
            KR_LOG_ERROR << "Node DEAD";                                                                               \
            assert(false);                                                                                             \
            return 0;                                                                                                  \
        }                                                                                                              \
    \
} while (0)

#else

#define KUIKLY_CHECK_NODE_OR_RETURN(NODE)
#define KUIKLY_CHECK_NODE_OR_RETURN_ERROR(NODE)
#define KUIKLY_CHECK_NODE_OR_RETURN_NULL(NODE)
#define KUIKLY_CHECK_NODE_OR_RETURN_ZERO(NODE)
#endif

namespace kuikly {
namespace util {

ArkUI_GestureRecognizer *ArkUINativeGestureAPI::createTapGesture(int32_t countNum, int32_t fingersNum) {
    KREnsureMainThread();
    return impl_->createTapGesture(countNum, fingersNum);
}

ArkUI_GestureRecognizer *ArkUINativeGestureAPI::createLongPressGesture(int32_t fingersNum, bool repeatResult,
                                                                       int32_t durationNum) {
    KREnsureMainThread();
    return impl_->createLongPressGesture(fingersNum, repeatResult, durationNum);
}

ArkUI_GestureRecognizer *
ArkUINativeGestureAPI::createPanGesture(int32_t fingersNum, ArkUI_GestureDirectionMask directions, double distanceNum) {
    KREnsureMainThread();
    return impl_->createPanGesture(fingersNum, directions, distanceNum);
}

ArkUI_GestureRecognizer *ArkUINativeGestureAPI::createPinchGesture(int32_t fingersNum, double distanceNum) {
    KREnsureMainThread();
    return impl_->createPinchGesture(fingersNum, distanceNum);
}

ArkUI_GestureRecognizer *ArkUINativeGestureAPI::createRotationGesture(int32_t fingersNum, double angleNum) {
    KREnsureMainThread();
    return impl_->createRotationGesture(fingersNum, angleNum);
}

ArkUI_GestureRecognizer *
ArkUINativeGestureAPI::createSwipeGesture(int32_t fingersNum, ArkUI_GestureDirectionMask directions, double speedNum) {
    KREnsureMainThread();
    return impl_->createSwipeGesture(fingersNum, directions, speedNum);
}

ArkUI_GestureRecognizer *ArkUINativeGestureAPI::createGroupGesture(ArkUI_GroupGestureMode gestureMode) {
    KREnsureMainThread();
    return impl_->createGroupGesture(gestureMode);
}

void ArkUINativeGestureAPI::dispose(ArkUI_GestureRecognizer *recognizer) {
    KREnsureMainThread();
    impl_->dispose(recognizer);
}

static void OnAddChildGesture(std::unordered_map<void *, void *> &gestureToParentGesture,
                              ArkUI_GestureRecognizer *group, ArkUI_GestureRecognizer *child) {
    gestureToParentGesture[child] = group;
}

static void OnRemoveChildGesture(std::unordered_map<void *, void *> &gestureToParentGesture,
                                 ArkUI_GestureRecognizer *group, ArkUI_GestureRecognizer *child) {
    gestureToParentGesture.erase(child);
}

static void OnAddGestureToNode(std::unordered_map<void *, void *> &gestureToNode, ArkUI_GestureRecognizer *recognizer,
                               ArkUI_NodeHandle node) {
    gestureToNode[recognizer] = node;
}

static void OnRemoveGestureFromNode(std::unordered_map<void *, void *> &gestureToNode,
                                    ArkUI_GestureRecognizer *recognizer, ArkUI_NodeHandle node) {
    gestureToNode.erase(recognizer);
}

ArkUI_NodeHandle ArkUINativeGestureAPI::GetAttachedNodeForRecognizer(ArkUI_GestureRecognizer *recognizer) {
    void *rootGesture = recognizer;
    int levels = GESTURE_MAX_LEVELS;
    do {
        auto it = gestureToParentGesture_.find(rootGesture);
        if (it == gestureToParentGesture_.end()) {
            break;
        }
        rootGesture = it->second;
    } while (--levels > 0);

    if (auto it = gestureToNode_.find(rootGesture); it != gestureToNode_.end()) {
        return (ArkUI_NodeHandle)it->second;
    }

    return nullptr;
}

std::string ArkUINativeGestureAPI::GetGestureBindNodeId(ArkUI_GestureRecognizer *recognizer) {
    char node_id[MAX_NODE_ID_LENGTH];
    int32_t result;
    int32_t code = OH_ArkUI_GetGestureBindNodeId(recognizer, node_id, MAX_NODE_ID_LENGTH, &result);
    if (code != ARKUI_ERROR_CODE_NO_ERROR || result <= 0) {
        node_id[0] = '\0';
    }
    return node_id;
}

int32_t ArkUINativeGestureAPI::addChildGesture(ArkUI_GestureRecognizer *group, ArkUI_GestureRecognizer *child) {
    KREnsureMainThread();
    OnAddChildGesture(gestureToParentGesture_, group, child);
    return impl_->addChildGesture(group, child);
}

int32_t ArkUINativeGestureAPI::removeChildGesture(ArkUI_GestureRecognizer *group, ArkUI_GestureRecognizer *child) {
    KREnsureMainThread();
    OnRemoveChildGesture(gestureToParentGesture_, group, child);
    return impl_->removeChildGesture(group, child);
}

int32_t ArkUINativeGestureAPI::setGestureEventTarget(ArkUI_GestureRecognizer *recognizer,
                                                     ArkUI_GestureEventActionTypeMask actionTypeMask, void *extraParams,
                                                     void (*targetReceiver)(ArkUI_GestureEvent *event,
                                                                            void *extraParams)) {
    KREnsureMainThread();
    return impl_->setGestureEventTarget(recognizer, actionTypeMask, extraParams, targetReceiver);
}

int32_t ArkUINativeGestureAPI::addGestureToNode(ArkUI_NodeHandle node, ArkUI_GestureRecognizer *recognizer,
                                                ArkUI_GesturePriority mode, ArkUI_GestureMask mask) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    OnAddGestureToNode(gestureToNode_, recognizer, node);
    return impl_->addGestureToNode(node, recognizer, mode, mask);
}

int32_t ArkUINativeGestureAPI::removeGestureFromNode(ArkUI_NodeHandle node, ArkUI_GestureRecognizer *recognizer) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    OnRemoveGestureFromNode(gestureToNode_, recognizer, node);

    return impl_->removeGestureFromNode(node, recognizer);
}

int32_t ArkUINativeGestureAPI::setGestureInterrupterToNode(
    ArkUI_NodeHandle node, void* userData, ArkUI_GestureInterruptResult (*interrupter)(ArkUI_GestureInterruptInfo *info)) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    if(impl2_){
        return impl2_->setGestureInterrupterToNode(node, userData, interrupter);
    }
    return impl_->setGestureInterrupterToNode(node, interrupter);
}

ArkUI_GestureRecognizerType ArkUINativeGestureAPI::getGestureType(ArkUI_GestureRecognizer *recognizer) {
    KREnsureMainThread();
    return impl_->getGestureType(recognizer);
}

int32_t ArkUINativeGestureAPI::setInnerGestureParallelTo(
    ArkUI_NodeHandle node, void *userData,
    ArkUI_GestureRecognizer *(*parallelInnerGesture)(ArkUI_ParallelInnerGestureEvent *event)) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    return impl_->setInnerGestureParallelTo(node, userData, parallelInnerGesture);
}

ArkUI_GestureRecognizer *ArkUINativeGestureAPI::createTapGestureWithDistanceThreshold(int32_t countNum,
                                                                                      int32_t fingersNum,
                                                                                      double distanceThreshold) {
    KREnsureMainThread();
    return impl_->createTapGestureWithDistanceThreshold(countNum, fingersNum, distanceThreshold);
}

ArkUINativeGestureAPI *ArkUINativeGestureAPI::GetInstance() {
    static ArkUINativeGestureAPI *instance_ = nullptr;
    static std::once_flag flag;
    std::call_once(flag, []() { instance_ = new ArkUINativeGestureAPI(); });
    return instance_;
}


ArkUINativeGestureAPI::ArkUINativeGestureAPI(){
    
    void* ptr = OH_ArkUI_QueryModuleInterfaceByName(ARKUI_NATIVE_GESTURE, "ArkUI_NativeGestureAPI_2");
    void* p1 = nullptr;
    if(ptr){
        impl2_ = (ArkUI_NativeGestureAPI_2 *)ptr;
        impl_ = impl2_->gestureApi1;
        return;
    }
    OH_ArkUI_GetModuleInterface(ARKUI_NATIVE_GESTURE, ArkUI_NativeGestureAPI_1, impl_);
}

ArkUINativeGestureAPI *GetGestureApi() {
    return ArkUINativeGestureAPI::GetInstance();
}

ArkUI_NodeHandle GetArkUINodeHandle(ArkUI_NodeEvent *event) {
    return OH_ArkUI_NodeEvent_GetNodeHandle(event);
}

ArkUI_NodeEventType GetArkUINodeEventType(ArkUI_NodeEvent *event) {
    return OH_ArkUI_NodeEvent_GetEventType(event);
}

int32_t GetImageLoadSuccessStatusCode(ArkUI_NodeEvent *event) {
    if (!event) {
        return -1;
    }

    auto component_event = OH_ArkUI_NodeEvent_GetNodeComponentEvent(event);
    if (!component_event) {
        return -1;
    }

    return component_event->data[0].i32;
}

bool IsImageLoadSuccessStatus(ArkUI_NodeEvent *event) {
    if (!event) {
        return false;
    }

    auto component_event = OH_ArkUI_NodeEvent_GetNodeComponentEvent(event);
    if (!component_event) {
        return false;
    }

    // 表示加载状态，0表示数据加载成功，1表示解码成功, 先返回0，再返回1
    // 当visibility(false)时，只会返回0，因此需要判断是否为0
    return component_event->data[0].i32 == 0;
}

KRSize GetArkUINodeImagePicSize(ArkUI_NodeEvent *event) {
    if (!event) {
        return {};
    }

    auto component_event = OH_ArkUI_NodeEvent_GetNodeComponentEvent(event);
    if (!component_event) {
        return {};
    }

    return {component_event->data[1].f32, component_event->data[2].f32};
}

ArkUI_UIInputEvent *GetArkUIInputEvent(ArkUI_NodeEvent *event) {
    if (!event) {
        return nullptr;
    }
    return OH_ArkUI_NodeEvent_GetInputEvent(event);
}

int32_t GetArkUIInputEventAction(ArkUI_UIInputEvent *event) {
    if (!event) {
        return UI_TOUCH_EVENT_ACTION_CANCEL;
    }
    return OH_ArkUI_UIInputEvent_GetAction(event);
}

int GetArkUIInputEventPointerCount(ArkUI_UIInputEvent *event) {
    if (!event) {
        return 0;
    }
    return OH_ArkUI_PointerEvent_GetPointerCount(event);
}

int64_t GetArkUIInputEventTime(ArkUI_UIInputEvent *event) {
    if (!event) {
        return 0;
    }
    return OH_ArkUI_UIInputEvent_GetEventTime(event);
}

int GetArkUIInputEventPointerId(ArkUI_UIInputEvent *event, int pointer_index) {
    if (!event) {
        return 0;
    }
    return OH_ArkUI_PointerEvent_GetPointerId(event, pointer_index);
}

KRPoint GetArkUIInputEventPoint(ArkUI_UIInputEvent *event, int pointer_index) {
    if (!event) {
        return {};
    }
    auto x = OH_ArkUI_PointerEvent_GetXByIndex(event, pointer_index);
    auto y = OH_ArkUI_PointerEvent_GetYByIndex(event, pointer_index);
    return {x, y};
}

KRPoint GetArkUIInputEventWindowPoint(ArkUI_UIInputEvent *event, int pointer_index) {
    if (!event) {
        return {};
    }
    auto window_x = OH_ArkUI_PointerEvent_GetWindowXByIndex(event, pointer_index);
    auto window_y = OH_ArkUI_PointerEvent_GetWindowYByIndex(event, pointer_index);
    return {window_x, window_y};
}

KRPoint GetArkUIEventPoint(ArkUI_NodeEvent *event) {
    if (!event) {
        return {};
    }
    auto input_event = OH_ArkUI_NodeEvent_GetInputEvent(event);
    if (!input_event) {
        return {};
    }

    auto x = OH_ArkUI_PointerEvent_GetX(input_event);
    auto y = OH_ArkUI_PointerEvent_GetY(input_event);
    return {x, y};
}

KRPoint GetArkUIGestureEventPoint(ArkUI_GestureEvent *event) {
    if (!event) {
        return {};
    }
    auto input_event = OH_ArkUI_GestureEvent_GetRawInputEvent(event);
    if (!input_event) {
        return {};
    }

    auto x = OH_ArkUI_PointerEvent_GetX(input_event);
    auto y = OH_ArkUI_PointerEvent_GetY(input_event);
    return {x, y};
}

KRPoint GetArkUIGestureEventWindowPoint(ArkUI_GestureEvent *event) {
    if (!event) {
        return {};
    }
    auto input_event = OH_ArkUI_GestureEvent_GetRawInputEvent(event);
    if (!input_event) {
        return {};
    }

    auto x = OH_ArkUI_PointerEvent_GetWindowX(input_event);
    auto y = OH_ArkUI_PointerEvent_GetWindowY(input_event);
    return {x, y};
}

ArkUI_GestureEventActionType GetArkUIGestureActionType(ArkUI_GestureEvent *event) {
    if (!event) {
        return GESTURE_EVENT_ACTION_CANCEL;
    }
    return OH_ArkUI_GestureEvent_GetActionType(event);
}

std::string GetArkUIGestureActionState(ArkUI_GestureEvent *event) {
    auto action = GetArkUIGestureActionType(event);
    std::string state;
    if (action == GESTURE_EVENT_ACTION_ACCEPT) {
        state = "start";
    } else if (action == GESTURE_EVENT_ACTION_UPDATE) {
        state = "move";
    } else {
        state = "end";
    }
    return std::move(state);
}

float GetArkUIGesturePinchScale(ArkUI_GestureEvent *event) {
    return OH_ArkUI_PinchGesture_GetScale(event);
}

void *GetUserData(ArkUI_NodeEvent *event) {
    if (!event) {
        return nullptr;
    }
    return OH_ArkUI_NodeEvent_GetUserData(event);
}

void StopPropagation(ArkUI_NodeEvent *event) {
    if (!event) {
        return;
    }
    auto input_event = OH_ArkUI_NodeEvent_GetInputEvent(event);
    OH_ArkUI_PointerEvent_SetStopPropagation(input_event, true);
}

}  // namespace util
}  // namespace kuikly
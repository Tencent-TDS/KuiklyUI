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

#include "IKRRenderViewExport.h"

#include "libohos_render/api/include/Kuikly/Kuikly.h"
#include "libohos_render/api/src/KRAnyDataInternal.h"
#include "libohos_render/manager/KRSnapshotManager.h"

#ifdef __cplusplus
extern "C" {
#endif

static KRRenderViewOnSetProp gExternalPropHandlerOnSet = nullptr;
static KRRenderViewOnResetProp gExternalPropHandlerOnReset = nullptr;

void KRRenderViewSetExternalPropHandler(KRRenderViewOnSetProp set, KRRenderViewOnResetProp reset) {
    gExternalPropHandlerOnSet = set;
    gExternalPropHandlerOnReset = reset;
}

#ifdef __cplusplus
}
#endif

std::shared_ptr<IKRRenderViewExport> IKRRenderViewExport::CreateView(const std::string &view_name) {
    KREnsureMainThread();

    auto &ViewCreatorMap = GetRegisterViewCreator();
    if (ViewCreatorMap.find(view_name) != ViewCreatorMap.end()) {
        return ViewCreatorMap[view_name]();
    } else {  // 使用通用View，转发到ArkTS层Module
        switch(KRArkTSViewNameRegistry::GetInstance().KindOfView(view_name)){
        case KRArkTSViewNameRegistry::ViewKindV1:{
            auto it = GetRegisterViewCreator().find(std::string(FORWARD_ARKTS_VIEW_NAME));
            if (it != GetRegisterViewCreator().end()) {
                KR_LOG_DEBUG << "Creating View with Forwarder V1 for View:"<<view_name;
                return it->second();
            }
        }
        break;
        case KRArkTSViewNameRegistry::ViewKindV2:{
            auto it = GetRegisterViewCreator().find(std::string(FORWARD_ARKTS_VIEW_NAME_V2));
            if (it != GetRegisterViewCreator().end()) {
                KR_LOG_DEBUG << "Creating View with Forwarder V1 for View:"<<view_name;
                return it->second();
            }
        }
        break;
        }
    }
    KR_LOG_ERROR << "View Creator Not Found! View Name:"<<view_name;
    return nullptr;
}

void IKRRenderViewExport::CallMethod(const std::string &method, const KRAnyValue &params,
                                     const KRRenderCallback &callback) {
    if (method == "toImage") {
        std::string instance_id = this->GetInstanceId();
        std::string method_name = method;
        // set node id before taking a snapshot
        std::string nodeId = GetNodeId();
        ArkUI_AttributeItem item = {nullptr, 0, nodeId.c_str(), nullptr};
        auto status = kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_ID, &item);

        std::weak_ptr<IKRRenderViewExport> weak_view =
            std::dynamic_pointer_cast<IKRRenderViewExport>(shared_from_this());
        if (auto root = GetRootView().lock()) {
            auto manager = root->GetSnapshotManager();
            manager->TakeSnapshot(instance_id, method_name, nodeId, params, callback, weak_view);
        }
    }
}

/**
 * SetupTouchInterrupter有两个作用：
 * 1. 触发拦截手势的时候，阻止子节点同时触发touch事件
 * 2. 解决ArkUI_GestureInterruptInfo里面无法正确获取按下坐标的问题
 */
void IKRRenderViewExport::SetupTouchInterrupter() {
    if (node_ == nullptr) {
        return;
    }
    if (!touch_interrupt_node_) {
        auto nodeApi = kuikly::util::GetNodeApi();
        // create a stack with size = 100% x 100%
        touch_interrupt_node_ = nodeApi->createNode(ARKUI_NODE_STACK);
        ArkUI_NumberValue position_value[] = {{.f32 = 0}, {.f32 = 0}};
        ArkUI_AttributeItem position_item = {position_value, 2};
        nodeApi->setAttribute(touch_interrupt_node_, NODE_POSITION, &position_item);
        ArkUI_NumberValue size_value[] = {{.f32 = 1}};
        ArkUI_AttributeItem size_item = {size_value, 1};
        nodeApi->setAttribute(touch_interrupt_node_, NODE_WIDTH_PERCENT, &size_item);
        nodeApi->setAttribute(touch_interrupt_node_, NODE_HEIGHT_PERCENT, &size_item);
        // set hit test mode transparent, so its sibling node can receive event
        ArkUI_NumberValue hit_test_value[] = {{.i32 = ARKUI_HIT_TEST_MODE_TRANSPARENT}};
        ArkUI_AttributeItem hit_test_item = {hit_test_value, 1};
        nodeApi->setAttribute(touch_interrupt_node_, NODE_HIT_TEST_BEHAVIOR, &hit_test_item);
        // register NODE_TOUCH_EVENT
        nodeApi->addNodeEventReceiver(touch_interrupt_node_, [](ArkUI_NodeEvent *event) {
            auto view_export = static_cast<IKRRenderViewExport *>(kuikly::util::GetUserData(event));
            if (!view_export) {
                return;
            }
            auto input_event = kuikly::util::GetArkUIInputEvent(event);
            auto action_type = kuikly::util::GetArkUIInputEventAction(input_event);
            // since getting x/y with setGestureInterrupterToNode's ArkUI_GestureInterruptInfo is buggy,
            // we record touch down position here
            if (action_type == UI_TOUCH_EVENT_ACTION_DOWN && kuikly::util::GetArkUIInputEventPointerCount(input_event) == 1) {
                view_export->interrupt_x_ = OH_ArkUI_PointerEvent_GetXByIndex(input_event, 0);
                view_export->interrupt_y_ = OH_ArkUI_PointerEvent_GetYByIndex(input_event, 0);
            }
            if (view_export->handling_capture_event_ &&
                (action_type == UI_TOUCH_EVENT_ACTION_DOWN || action_type == UI_TOUCH_EVENT_ACTION_MOVE)) {
                kuikly::util::StopPropagation(event);
            }
        });
        nodeApi->registerNodeEvent(touch_interrupt_node_, NODE_TOUCH_EVENT, NODE_TOUCH_EVENT, this);
    }
    if (!touch_interrupt_node_attached_) {
        kuikly::util::GetNodeApi()->addChild(node_, touch_interrupt_node_);
        touch_interrupt_node_attached_ = true;
    }
}

void IKRRenderViewExport::ResetTouchInterrupter() {
    if (touch_interrupt_node_attached_) {
        kuikly::util::GetNodeApi()->removeChild(node_, touch_interrupt_node_);
        touch_interrupt_node_attached_ = false;
    }
    handling_capture_event_ = false;
}

void IKRRenderViewExport::DestroyTouchInterrupter() {
    if (touch_interrupt_node_) {
        kuikly::util::GetNodeApi()->unregisterNodeEvent(touch_interrupt_node_, NODE_TOUCH_EVENT);
        auto node = touch_interrupt_node_;
        KRMainThread::RunOnMainThreadForNextLoop([node] { kuikly::util::GetNodeApi()->disposeNode(node); });
        touch_interrupt_node_ = nullptr;
    }
}

void IKRRenderViewExport::ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                    const KRRenderCallback event_call_back) {
    if (node_ == nullptr) {
        return;
    }
    // 把设置过的key收集下, 以便ResetProp
    if (CanReuse()) {
        CollectReuseKeyIfNeed(prop_key);
    }

    auto didHanded = false;
    if (base_props_handler_ != nullptr) {
        auto isFrameProp = kuikly::util::isEqual(prop_key, "frame");
        if (!(isFrameProp && CustomSetViewFrame())) {
            didHanded = ToSetBaseProp(prop_key, prop_value, event_call_back);  // 基础属性设置分发处理
        }
        if (isFrameProp) {
            const std::string &s = prop_value->toString();
            memcpy(&frame_, s.data(), s.size());
            SetRenderViewFrame(frame_);
        }
    }
    if (!didHanded && base_event_handler_ != nullptr) {
        didHanded = base_event_handler_->SetProp(shared_from_this(), prop_key, prop_value,
                                                 event_call_back);  // 基础事件分发处理
    }
    if (!didHanded) {
        if (!SetProp(prop_key, prop_value, event_call_back)) {
            // prop not handled, pass it forward to extern handler
            if (gExternalPropHandlerOnSet) {
                struct KRAnyDataInternal anyDataInternal;
                anyDataInternal.anyValue = prop_value;
                gExternalPropHandlerOnSet(GetNode(), prop_key.c_str(), &anyDataInternal);
            }
        }
    }
    DidSetProp(prop_key);
}

bool IKRRenderViewExport::ResetProp(const std::string &prop_key) {
    return gExternalPropHandlerOnReset ? gExternalPropHandlerOnReset(GetNode(), prop_key.c_str()) : false;
}

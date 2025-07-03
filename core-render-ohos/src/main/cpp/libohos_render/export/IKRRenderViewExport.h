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

#ifndef CORE_RENDER_OHOS_IKRRENDERVIEWEXPORT_H
#define CORE_RENDER_OHOS_IKRRENDERVIEWEXPORT_H

#include <arkui/native_interface.h>
#include <arkui/native_node.h>
#include <functional>
#include <map>
#include <set>
#include <string>
#include "libohos_render/expand/components/base/KRBasePropsHandler.h"
#include "libohos_render/expand/events/KRBaseEventHandler.h"
#include "libohos_render/expand/events/KREventDispatchCenter.h"
#include "libohos_render/export/IKRRenderModuleExport.h"
#include "libohos_render/export/IKRRenderShadowExport.h"
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/foundation/KRRect.h"
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/manager/KRArkTSManager.h"
#include "libohos_render/utils/KRThreadChecker.h"
#include "libohos_render/utils/KRStringUtil.h"
#include "libohos_render/utils/KRViewUtil.h"
#include "libohos_render/view/IKRRenderView.h"

#define FORWARD_ARKTS_VIEW_NAME "FORWARD_ARKTS_VIEW_NAME"
#define FORWARD_ARKTS_VIEW_NAME_V2 "FORWARD_ARKTS_VIEW_NAME_V2"
extern int g_kuikly_disable_view_reuse;

class IKRRenderViewExport;
using KRViewCreator = std::function<std::shared_ptr<IKRRenderViewExport>()>;

class IKRRenderViewExport : public std::enable_shared_from_this<IKRRenderViewExport> {
 public:
    virtual ~IKRRenderViewExport() = default;

    virtual ArkUI_NodeHandle CreateNode() {
        return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_STACK);
    }

    const ArkUI_NodeHandle GetNode() {
        return node_;
    }

    virtual void DidInit() {}

    void ToInit() {
        KREnsureMainThread();

        auto strongRoot = GetRootView().lock();
        if (strongRoot == nullptr || did_init_) {
            KR_LOG_ERROR << "ToInit unexpected error, strongRoot:" << strongRoot.get() << "did_init_:" << did_init_;
            return;
        }
        did_init_ = true;
        node_ = CreateNode();
        base_props_handler_ = CreateBasePropHandler(strongRoot);
        base_event_handler_ = CreateBaseEventHandler(strongRoot);

        DidInit();
    }

    /**
     * UI节点事件回调
     * @param event 事件参数对象
     * @param event_type 事件类型
     */
    virtual void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {}
    /**
     * 自定义Node事件回调（Custom_Node)）
     * @param event
     * @param event_type
     */
    virtual void OnCustomEvent(ArkUI_NodeCustomEvent *event, const ArkUI_NodeCustomEventType &event_type) {}
    /**
     * 手势事件回调
     * @param gesture_event_data
     * @param event_type
     */
    virtual void OnGestureEvent(const std::shared_ptr<KRGestureEventData> &gesture_event_data,
                                const KRGestureEventType &event_type) {}

    /**
     * 手势打断事件回调，执行RegisterGestureInterrupter后，当手势发生时，会回调该方法
     * @param info
     * @return ArkUI_GestureInterruptResult
     */
    virtual ArkUI_GestureInterruptResult OnInterruptGestureEvent(const ArkUI_GestureInterruptInfo *info) {
        return GESTURE_INTERRUPT_RESULT_CONTINUE;
    }

    /**
     * Kotlin侧设置View属性时调用（注：如果ReuseEnable为true，重写ResetProp进行属性复原）
     * @param prop_key 属性名
     * @param prop_value 属性值 （有事件时该值为空）
     * @param event_call_back 事件callback，绑定事件时该值不为空，其他情况为空
     * @return 是否设置完成
     */
    virtual bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                         const KRRenderCallback event_call_back = nullptr) {
        return false;
    }
    /**
     * 设置属性完成后调用（包括基础属性）
     */
    virtual void DidSetProp(const std::string &prop_key) {}
    /**
     * 该View是否支持复用
     * @return 是否支持复用, 默认为false
     */
    virtual bool ReuseEnable() {
        return false;
    }
    /**
     * ReuseEnable()为true且View回收复用时进行该接口的回调，如果子类override这个方法，应该在已经处理属性重置后返回true，否则交给父类处理重置操作。
     * @param prop_key 当时设置过的属性
     * @return 是否重设完成
     */
    virtual bool ResetProp(const std::string &prop_key);
    /**
     * 设置View的绝对位置（相对父容器坐标系）
     * @param frame
     */
    virtual void SetRenderViewFrame(const KRRect &frame) {}
    /**
     * 收到来自ArkTS侧的事件回调响应
     */
    virtual void FireViewEventFromArkTS(std::string eventKey, KRAnyValue data) {}

    /**
     * 自定义设置ViewFrame, 基类不处理SetFrame操作，子类自身通过重写SetRenderViewFrame实现
     * @return
     */
    virtual bool CustomSetViewFrame() {
        return false;
    }
    /**
     * 绑定设置预排版shadow产物
     * @param shadow
     */
    virtual void SetShadow(const std::shared_ptr<IKRRenderShadowExport> &shadow) {}
    /**
     * Kotlin侧对该View的方法进行调用时回调用
     * @param method
     * @param params
     * @param callback 可空
     */
    virtual void CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &callback);
    /**
     * 插入子孩子后调用
     * @param sub_render_view
     * @param index
     */
    virtual void DidInsertSubRenderView(const std::shared_ptr<IKRRenderViewExport> &sub_render_view, int index) {}

    /**
     * view添加到父节点前调用
     */
    virtual void WillMoveToParentView() {}
    
    /**
     * view添加到父节点中后调用
     */
    virtual void DidMoveToParentView() {}

    /**
     * view删除完成后调用(准备复用时调用或被删除时)
     */
    virtual void DidRemoveFromParentView() {}
    /**
     * view即将删除完成后调用(准备复用时调用或即将被删除时)
     */
    virtual void WillRemoveFromParentView() {}

    /**
     * 被销毁时调用
     */
    virtual void OnDestroy() {}
    /**
     * 将要复用时调用
     */
    virtual void WillReuse() {}

    void ToDestroy() {
        KREnsureMainThread();

        OnDestroy();
        if (parent_node_ != nullptr && node_ != nullptr) {
            ToRemoveFromSuperView();
        }
        UnregisterEvent();
        ResetTouchInterrupter();
        DestroyTouchInterrupter();
        base_props_handler_->OnDestroy();
        base_event_handler_->OnDestroy();
        DestroyNode();
        parent_node_ = nullptr;
    }
    
    virtual void DestroyNode(){
        if (node_) {
            auto node = node_;
            KRMainThread::RunOnMainThreadForNextLoop([node] { kuikly::util::GetNodeApi()->disposeNode(node); });
            node_ = nullptr;
        }
    }
    /**
     * 最终判断能否复用Api
     */
    bool CanReuse() {
        if(g_kuikly_disable_view_reuse){
            return false;
        }
        if (base_props_handler_->isAnimationNode()) {  // 对齐iOS（避免执行中动画影响新的复用）
            return false;
        }
        return ReuseEnable();
    }

    /**
     * 注册View创建器
     * @param creator
     */
    static void RegisterViewCreator(const std::string &view_name, const KRViewCreator &creator) {
        GetRegisterViewCreator()[view_name] = creator;
    }

    /**
     * 注册通用转发ArkTS层View创建器
     * @param creator
     */
    static void RegisterForwardArkTSViewCreator(const KRViewCreator &creator) {
        RegisterViewCreator(std::string(FORWARD_ARKTS_VIEW_NAME), creator);
    }
    static void RegisterForwardArkTSViewCreatorV2(const KRViewCreator &creator) {
        RegisterViewCreator(std::string(FORWARD_ARKTS_VIEW_NAME_V2), creator);
    }

    /**
     * 指定ViewName生成View
     * @param view_name
     * @return 新的View实例
     */
    static std::shared_ptr<IKRRenderViewExport> CreateView(const std::string &view_name);

    static std::unordered_map<std::string, KRViewCreator> &GetRegisterViewCreator() {
        static std::unordered_map<std::string, KRViewCreator> gRegisterViewCreator;
        return gRegisterViewCreator;
    }

    KRRect &GetFrame() {
        return frame_;
    }

    void RegisterEvent(const ArkUI_NodeEventType &event_type) {
        KREventDispatchCenter::GetInstance().RegisterEvent(shared_from_this(), event_type);
    }

    void ToOnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
        KREnsureMainThread();

        base_event_handler_->OnEvent(event, event_type);
        OnEvent(event, event_type);
    }

    void ToOnCustomEvent(ArkUI_NodeCustomEvent *event, const ArkUI_NodeCustomEventType &event_type) {
        KREnsureMainThread();

        base_event_handler_->OnCustomEvent(event, event_type);
        OnCustomEvent(event, event_type);
    }

    void ToOnGestureEvent(const std::shared_ptr<KRGestureEventData> &gesture_event_data,
                          const KRGestureEventType &event_type) {
        KREnsureMainThread();

        base_event_handler_->OnGestureEvent(gesture_event_data, event_type);
        if (base_event_handler_->HasCaptureRule()) {
            auto action_type = kuikly::util::GetArkUIGestureActionType(gesture_event_data->gesture_event_);
            handling_capture_event_ =
                action_type == GESTURE_EVENT_ACTION_ACCEPT || action_type == GESTURE_EVENT_ACTION_UPDATE;
        }
        OnGestureEvent(gesture_event_data, event_type);
    }

    void RegisterGestureInterrupter() {
        KREventDispatchCenter::GetInstance().RegisterGestureInterrupter(shared_from_this());
    }

    virtual bool ToSetBaseProp(const std::string &prop_key, const KRAnyValue &prop_value,
                               const KRRenderCallback event_call_back = nullptr) {
        KREnsureMainThread();

        return base_props_handler_->SetProp(prop_key, prop_value, event_call_back);
    }

    virtual void ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                           const KRRenderCallback event_call_back = nullptr);
#if 0  // implementation move to cpp file
    {
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
                frame_ = *reinterpret_cast<KRRect *>(prop_value->toObjectAddress());
                SetRenderViewFrame(frame_);
            }
        }
        if (!didHanded && base_event_handler_ != nullptr) {
            didHanded = base_event_handler_->SetProp(shared_from_this(), prop_key, prop_value,
                                                     event_call_back);  // 基础事件分发处理
        }
        if (!didHanded) {
            SetProp(prop_key, prop_value, event_call_back);
        }
        DidSetProp(prop_key);
    }
#endif
    void ToResetProp(const std::string &prop_key) {
        KREnsureMainThread();

        if (node_ == nullptr) {
            return;
        }
        auto didHanded = false;
        if (base_props_handler_ != nullptr && base_props_handler_->ResetProp(prop_key)) {
            didHanded = true;
        }
        if (!didHanded && base_event_handler_ != nullptr) {
            didHanded = base_event_handler_->ResetProp(prop_key);
        }
        if (!didHanded) {
            ResetProp(prop_key);
        }
        frame_ = KRRect(0, 0, 0, 0);
    }

    void ToReuse() {
        KREnsureMainThread();

        if (node_ == nullptr) {
            return;
        }
        if (did_set_props_.size() > 0) {
            for (const auto &prop_key : did_set_props_) {
                ToResetProp(prop_key);
            }
            did_set_props_.clear();
        }
        UnregisterEvent();
        ResetTouchInterrupter();
    }

    virtual void RemoveChildNode(ArkUI_NodeHandle parent, ArkUI_NodeHandle child){
        kuikly::util::GetNodeApi()->removeChild(parent, child);
    }
    void ToRemoveFromSuperView() {
        KREnsureMainThread();

        WillRemoveFromParentView();
        if (parent_node_ != nullptr) {
            RemoveChildNode(parent_node_, GetNode());
            parent_node_ = nullptr;
        }
        parent_tag_ = -1;
        DidRemoveFromParentView();
    }

    virtual void InsertChildNode(ArkUI_NodeHandle parent, ArkUI_NodeHandle child, int index){
        kuikly::util::GetNodeApi()->insertChildAt(parent, child, index);
    }
    void ToInsertSubRenderView(const std::shared_ptr<IKRRenderViewExport> &sub_render_view, int index) {
        KREnsureMainThread();

        sub_render_view->WillMoveToParentView();
        if (node_ == nullptr || sub_render_view->GetNode() == nullptr) {
            return;
        }
        auto childrenCount = GetChildCount();
        if (index < 0 || index > childrenCount) {
            index = childrenCount;
        }
        sub_render_view->parent_node_ = GetNode();
        sub_render_view->parent_tag_ = this->GetViewTag();

        InsertChildNode(GetNode(), sub_render_view->GetNode(), index);
        sub_render_view->DidMoveToParentView();
        DidInsertSubRenderView(sub_render_view, index);
    }

    int32_t GetChildCount() {
        if (node_ == nullptr) {
            return 0;
        }
        return kuikly::util::GetNodeApi()->getTotalChildCount(node_) - touch_interrupt_node_attached_;
    }

    // 是否含有手势事件监听
    bool HasBaseEvent() {
        return base_event_handler_->HasTouchEvent();
    }

    void SetRootView(std::weak_ptr<IKRRenderView> root_view, std::string instance_id) {
        root_view_ = root_view;
        instance_id_ = instance_id;
    }

    const std::weak_ptr<IKRRenderView> GetRootView() {
        return root_view_;
    }

    void SetViewName(const std::string &view_name) {
        view_name_ = view_name;
    }

    const std::string &GetViewName() const {
        return view_name_;
    }

    std::string GetInstanceId() {
        return instance_id_;
    }

    void SetViewTag(int view_tag) {
        view_tag_ = view_tag;
        // #ifndef NDEBUG
        //  Since ohos underlying NODE_ID calls are error prone, enable it only in debugging mode.
        //  If we don't set it here, it would crash the app when somebody call getAttribute for NODE_ID,
        //  thus, we'd better keep it here for a while...
        if (node_) {
            std::string id = GetNodeId();
            ArkUI_AttributeItem item = {nullptr, 0, id.c_str(), nullptr};
            auto status = kuikly::util::GetNodeApi()->setAttribute(node_, NODE_ID, &item);
        }
        // #endif
    }
    const int GetViewTag() const {
        return view_tag_;
    }
    const std::string GetNodeId() {
        std::stringstream ss;
        ss << view_name_ << "_" << instance_id_ << "_" << view_tag_;
        return ss.str();
    }
    const std::shared_ptr<KRBasePropsHandler> GetBasePropsHandler() {
        return base_props_handler_;
    }

    std::shared_ptr<IKRRenderViewExport> GetParentView() {
        if (auto self = root_view_.lock()) {
            return self->GetView(parent_tag_);
        }
        return nullptr;
    }

    std::shared_ptr<IKRRenderModuleExport> GetModule(std::string &name) {
        auto root_view = GetRootView().lock();
        if (!root_view) {
            return nullptr;
        }
        auto module = root_view->GetModuleOrCreate(name);
        if (!module) {
            return nullptr;
        }
        return module;
    }

    void SetupTouchInterrupter();
    void ResetTouchInterrupter();
    void DestroyTouchInterrupter();

    [[deprecated("will remove when ohos fix ArkUI_GestureInterruptInfo")]]
    float GetInterruptX() {
        return interrupt_x_;
    }

    [[deprecated("will remove when ohos fix ArkUI_GestureInterruptInfo")]]
    float GetInterruptY() {
        return interrupt_y_;
    }

 protected:
    virtual std::shared_ptr<KRBaseEventHandler>  CreateBaseEventHandler(std::shared_ptr<IKRRenderView> rootView){
        if(rootView){
            return std::make_shared<KRBaseEventHandler>(rootView->GetContext()->Config());
        }
        return nullptr;
    }
    virtual std::shared_ptr<KRBasePropsHandler>  CreateBasePropHandler(std::shared_ptr<IKRRenderView> rootView){
        if(rootView){
            return std::make_shared<KRBasePropsHandler>(shared_from_this(), node_, rootView->GetUIContextHandle());
        }
        return nullptr;
    }

 private:
    void UnregisterEvent() {
        KREventDispatchCenter::GetInstance().UnregisterEvent(shared_from_this());
        KREventDispatchCenter::GetInstance().UnregisterCustomEvent(shared_from_this());
        KREventDispatchCenter::GetInstance().UnregisterGestureEvent(shared_from_this());
        KREventDispatchCenter::GetInstance().UnregisterGestureInterrupter(shared_from_this());
    }
    void CollectReuseKeyIfNeed(const std::string &prop_key) {
        if (std::find(did_set_props_.begin(), did_set_props_.end(), prop_key) == did_set_props_.end()) {
            did_set_props_.push_back(prop_key);
        }
    }

 protected:
    ArkUI_NodeHandle node_ = nullptr;
 private:
    std::weak_ptr<IKRRenderView> root_view_;
    std::string instance_id_;
    std::shared_ptr<KRBaseEventHandler> base_event_handler_;
    std::string view_name_;
    int view_tag_ = 0;
    std::vector<std::string> did_set_props_;
    
    ArkUI_NodeHandle parent_node_ = nullptr;
    int parent_tag_ = -1;
    std::shared_ptr<KRBasePropsHandler> base_props_handler_;
    KRRect frame_;
    bool did_init_ = false;
    ArkUI_NodeHandle touch_interrupt_node_ = nullptr;
    bool touch_interrupt_node_attached_ = false;
    float interrupt_x_ = -1;
    float interrupt_y_ = -1;
    bool handling_capture_event_ = false;
};

#endif  // CORE_RENDER_OHOS_IKRRENDERVIEWEXPORT_H

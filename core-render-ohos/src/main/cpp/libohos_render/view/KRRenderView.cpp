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

#include "libohos_render/view/KRRenderView.h"

#include <functional>
#include "libohos_render/context/IKRRenderNativeContextHandler.h"
#include "libohos_render/scheduler/IKRScheduler.h"
#include "libohos_render/scheduler/KRContextScheduler.h"
#include "libohos_render/scheduler/KRUIScheduler.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/KRViewUtil.h"

static constexpr char PAGER_EVENT_FIRST_FRAME_PAINT[] = "pageFirstFramePaint";

const unsigned int LOG_PRINT_DOMAIN = 0xFF01;
static std::string GetIncreaseCallbackId() {
    static int gCallbackId = 0;
    gCallbackId++;
    return NewKRRenderValue(gCallbackId)->toString();
}
KRRenderView::~KRRenderView() {
    if (node_content_handle_ && root_node_){
        ArkUI_NodeContentHandle content_handle = node_content_handle_;
        ArkUI_NodeHandle root_node = root_node_;

        KRMainThread::RunOnMainThread([content_handle, root_node] {
            OH_ArkUI_NodeContent_RemoveNode(content_handle, root_node);
        });
    }
    if(root_node_ != nullptr){
        ArkUI_NodeHandle root_node = root_node_;
        KRMainThread::RunOnMainThread([root_node] {
            kuikly::util::GetNodeApi()->disposeNode(root_node);
        });
        root_node_ = nullptr;
    }
    node_content_handle_ = nullptr;
    native_resources_manager_ = nullptr;
    method_arg_callback_map_.clear();
}

void KRRenderView::WillDestroy(const std::string &instanceId) {
    core_->WillDealloc(instanceId);
    // send event to call
    // delay destroy for core
}

/**
 * 发送页面事件到kotlin侧
 * @param event_name 事件名
 * @param json_data json数据字符串）
 */
void KRRenderView::SendEvent(std::string event_name, const std::string &json_data) {
    if (core_) {
        return core_->SendEvent(event_name, json_data);
    }
}

/**
 * 获取渲染节点视图（要求在主线程调用）
 * @param tag 所在tag
 * @return 对应节点view
 */
std::shared_ptr<IKRRenderViewExport> KRRenderView::GetView(int tag) {
    if (core_) {
        return core_->GetView(tag);
    }
    return nullptr;
}

/**
 * 获取渲染节点视图（要求在主线程调用）
 * @param tag 所在tag
 * @return 对应节点view
 */
std::shared_ptr<IKRRenderModuleExport> KRRenderView::GetModule(std::string &module_name) {
    if (core_) {
        return core_->GetModule(module_name);
    }
    return nullptr;
}

std::shared_ptr<IKRRenderModuleExport> KRRenderView::GetModuleOrCreate(std::string &module_name) {
    if (core_) {
        return core_->GetModuleOrCreate(module_name);
    }
    return nullptr;
}

void KRRenderView::AddContentView(const std::shared_ptr<IKRRenderViewExport> contentView, int index) {
    if (root_node_ == nullptr) {
        return;
    }
    kuikly::util::GetNodeApi()->addChild(root_node_, contentView->GetNode());
    if (!is_load_finish) {  //  首帧事件
        is_load_finish = true;
        OnFirstFramePaint();
    }
}

/**
 * 添加任务到主线程队列中，注意：调用接口所在线程须是context线程
 * @param task
 */
void KRRenderView::AddTaskToMainQueueWithTask(const KRSchedulerTask &task) {
    if (core_ != nullptr) {
        core_->AddTaskToMainQueueWithTask(task);
    }
}

void KRRenderView::PerformTaskWhenMainThreadEnd(const KRSchedulerTask &task) {
    if (core_ != nullptr) {
        core_->PerformTaskWhenMainThreadEnd(task);
    }
}

bool KRRenderView::IsPerformMainTasking() {
    if (core_ != nullptr) {
        return core_->IsPerformMainTasking();
    }
    return false;
}

const ArkUI_ContextHandle &KRRenderView::GetUIContextHandle() const {
    return ui_context_handle_;
}

KRSnapshotManager *KRRenderView::GetSnapshotManager() {
    return &snapshot_manager_;
}

NativeResourceManager *KRRenderView::GetNativeResourceManager() {
    return native_resources_manager_;
}

std::shared_ptr<KRPerformanceManager> KRRenderView::GetPerformanceManager() {
    return performance_manager_;
}

std::shared_ptr<KRRenderContextParams> KRRenderView::GetContext() {
    return context_;
}

void KRRenderView::Init(std::shared_ptr<KRRenderContextParams> context, ArkUI_ContextHandle &ui_context_handle,
                        NativeResourceManager *native_resources_manager, float width, float height,
                        int64_t launch_time) {
    context_ = context;
    ui_context_handle_ = ui_context_handle;
    native_resources_manager_ = native_resources_manager;
    performance_manager_ = std::make_shared<KRPerformanceManager>(context->PageName(), context->ExecuteMode());
    performance_manager_->SetArkLaunchTime(launch_time);
    root_view_width_ = width;
    root_view_height_ = height;
    InitRender(width, height);
}

void KRRenderView::OnRenderViewSizeChanged(float width, float height) {
    KR_LOG_INFO << "KRRenderView CreateRenderNode";
    if (root_node_ == nullptr) {
        return;
    }
    if (fabs(root_view_width_ - width) >= 0.1 || fabs(root_view_height_ - height) >= 0.1) {
        root_view_width_ = width;
        root_view_height_ = height;
        KR_LOG_INFO << "KRRenderView render view size did changed";
        kuikly::util::UpdateNodeSize(root_node_, width, height);
        // 尺寸变化更新到Kotlin
        KRRenderValue::Map data;
        data["width"] = std::make_shared<KRRenderValue>(width);
        data["height"] = std::make_shared<KRRenderValue>(height);
        auto json_data = std::make_shared<KRRenderValue>(data)->toString();
        SendEvent("rootViewSizeDidChanged", json_data);
    }
}

void KRRenderView::InitRender(float width, float height) {
    if (root_node_ != nullptr ||  node_content_handle_ == nullptr ||
        context_ == nullptr) {
        return;
    }
    DispatchInitState(KRInitState::kStateKRRenderViewInit);
    auto start = std::chrono::steady_clock::now();

    auto nodeAPI = kuikly::util::GetNodeApi();
    root_node_ = nodeAPI->createNode(ARKUI_NODE_STACK);
    kuikly::util::UpdateNodeSize(root_node_, width, height);
    kuikly::util::UpdateNodeBackgroundColor(root_node_, 0);
     if (node_content_handle_) {
        OH_ArkUI_NodeContent_AddNode(node_content_handle_, root_node_);
    }
    auto self = shared_from_this();
    DispatchInitState(KRInitState::kStateInitCoreStart);
    core_ = std::make_shared<KRRenderCore>(self, context_);
    core_->DidInit();
    DispatchInitState(KRInitState::kStateInitCoreFinish);

    // 获取操作完成后的时间点
    auto end = std::chrono::steady_clock::now();
    // 计算时间差（以毫秒为单位）
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
    KR_LOG_INFO << "cost time first screen: " << duration.count();
}

/**
 * 注册参数Callback
 * @return 该Callback索引ID, 用于GetArgCallback
 */
std::string KRRenderView::GenerateArgCallbackId(const KRRenderCallback &callback, bool callback_keep_alive,
                                                bool arg_prefer_raw_napi_value) {
    auto callback_id = GetIncreaseCallbackId();
    method_arg_callback_map_[callback_id] =
        std::make_shared<KRArkTsCallbackWrapper>(callback, callback_keep_alive, arg_prefer_raw_napi_value);
    return callback_id;
}

/**
 * 根据callbackid获取Callback
 */
KRRenderCallback KRRenderView::GetArgCallback(std::string callbackId, bool &arg_prefer_raw_napi_value) {
    if (method_arg_callback_map_.find(callbackId) != method_arg_callback_map_.end()) {
        auto callback_wrapper = method_arg_callback_map_[callbackId];
        if (!callback_wrapper->IsKeepAlive()) {
            method_arg_callback_map_.erase(callbackId);
        }
        arg_prefer_raw_napi_value = callback_wrapper->ArgPrefersRawNapiValue();
        return callback_wrapper->GetCallback();
    }
    return nullptr;
}

void KRRenderView::OnFirstFramePaint() {
    performance_manager_->OnFirstFramePaint();
    SendEvent(PAGER_EVENT_FIRST_FRAME_PAINT, "{}");
}

void KRRenderView::DispatchInitState(KRInitState state) {
    switch (state) {
    case KRInitState::kStateKRRenderViewInit:
        performance_manager_->OnKRRenderViewInit();
        break;
    case KRInitState::kStateInitCoreStart:
        performance_manager_->OnInitCoreStart();
        break;
    case KRInitState::kStateInitCoreFinish:
        performance_manager_->OnInitCoreFinish();
        break;
    case KRInitState::kStateInitContextStart:
        performance_manager_->OnInitContextStart();
        break;
    case KRInitState::kStateInitContextFinish:
        performance_manager_->OnInitContextFinish();
        break;
    case KRInitState::kStateCreateInstanceStart:
        performance_manager_->OnCreateInstanceStart();
        break;
    case KRInitState::kStateCreateInstanceFinish:
        performance_manager_->OnCreateInstanceFinish();
        break;
    default:
        break;
    }
}

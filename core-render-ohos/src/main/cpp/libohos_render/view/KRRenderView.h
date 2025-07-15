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

#ifndef CORE_RENDER_OHOS_KRRENDERVIEW_H
#define CORE_RENDER_OHOS_KRRENDERVIEW_H

#include <ace/xcomponent/native_interface_xcomponent.h>
#include <arkui/native_node.h>
#include <rawfile/raw_file_manager.h>
#include "libohos_render/context/KRRenderContextParams.h"
#include "libohos_render/core/KRRenderCore.h"
#include "libohos_render/foundation/KRCallbackData.h"
#include "libohos_render/manager/KRSnapshotManager.h"
#include "libohos_render/performance/KRPerformanceManager.h"
#include "libohos_render/scheduler/IKRScheduler.h"
#include "libohos_render/view/IKRRenderView.h"

class KRRenderView : public IKRRenderView {
 public:
    explicit KRRenderView(ArkUI_NodeContentHandle handle) : IKRRenderView(), node_content_handle_((handle)) {
        // blank
    }
    void Init(std::shared_ptr<KRRenderContextParams> context, ArkUI_ContextHandle &ui_context_handle,
              NativeResourceManager *native_resources_manager, float width, float height, int64_t launch_time);
    void OnRenderViewSizeChanged(float width, float height);
    void WillDestroy(const std::string &instanceId);
    /**
     * 发送页面事件到kotlin侧
     * @param event_name 事件名
     * @param json_data json数据字符串）
     */
    void SendEvent(std::string event_name, const std::string &json_data) override;

    /**
     * 获取渲染节点视图（要求在主线程调用）
     * @param tag 所在tag
     * @return 对应节点view
     */
    std::shared_ptr<IKRRenderViewExport> GetView(int tag) override;

    /**
     * 获取渲染节点视图（要求在主线程调用）
     * @param tag 所在tag
     * @return 对应节点view
     */
    std::shared_ptr<IKRRenderModuleExport> GetModule(std::string &module_name) override;
    ~KRRenderView();

    std::shared_ptr<IKRRenderModuleExport> GetModuleOrCreate(std::string &module_name) override;

    void AddContentView(const std::shared_ptr<IKRRenderViewExport> contentView, int index) override;
    /**
     * 添加任务到主线程队列中，注意：调用接口所在线程须是context线程
     * @param task
     */
    void AddTaskToMainQueueWithTask(const KRSchedulerTask &task) override;
    /**
     * 执行任务当该主线程loop结束时
     */
    void PerformTaskWhenMainThreadEnd(const KRSchedulerTask &task) override;

    /**
     * 是否正在执行主线程任务中
     */
    bool IsPerformMainTasking() override;

    std::shared_ptr<KRRenderContextParams> GetContext() override;

    const ArkUI_ContextHandle &GetUIContextHandle() const override;

    KRSnapshotManager *GetSnapshotManager() override;

    NativeResourceManager *GetNativeResourceManager() override;

    std::shared_ptr<KRPerformanceManager> GetPerformanceManager() override;
    void OnFirstFramePaint();
    /**
     * 根据Callback生成callback_id
     * @return 该Callback索引ID, 用于GetArgCallback
     */
    std::string GenerateArgCallbackId(const KRRenderCallback &callback, bool callback_keep_alive,
                                      bool arg_prefer_raw_napi_value);

    /**
     * 根据callbackid获取Callback
     */
    KRRenderCallback GetArgCallback(std::string callbackId, bool &arg_prefer_raw_napi_value);

    /**
     * 派发页面加载初始化事件
     * @param state
     */
    void DispatchInitState(KRInitState state);

    class KRArkTsCallbackWrapper {
     public:
        KRArkTsCallbackWrapper(const KRRenderCallback &callback, bool callback_keep_alive,
                               bool arg_prefers_raw_napi_value)
            : callback_keep_alive_(callback_keep_alive), callback_(callback),
              arg_prefers_raw_napi_value_(arg_prefers_raw_napi_value) {}

        const bool IsKeepAlive() const {
            return callback_keep_alive_;
        }

        KRRenderCallback GetCallback() {
            return callback_;
        }

        const bool ArgPrefersRawNapiValue() {
            return arg_prefers_raw_napi_value_;
        }

     private:
        bool callback_keep_alive_;
        bool arg_prefers_raw_napi_value_;
        KRRenderCallback callback_;
    };

 private:
    ArkUI_NodeContentHandle node_content_handle_ = nullptr;
    ArkUI_NodeHandle root_node_ = nullptr;
    float root_view_width_ = 0.0;
    float root_view_height_ = 0.0;
    std::shared_ptr<KRRenderContextParams> context_;
    ArkUI_ContextHandle ui_context_handle_;
    NativeResourceManager *native_resources_manager_;
    std::shared_ptr<KRRenderCore> core_;
    // Callback管理索引表
    std::unordered_map<std::string, std::shared_ptr<KRArkTsCallbackWrapper>> method_arg_callback_map_;
    KRSnapshotManager snapshot_manager_;
    std::shared_ptr<KRPerformanceManager> performance_manager_ = nullptr;
    bool is_load_finish = false;  //  是否已经初始化过标记
    void InitRender(float width, float height);
};

#endif  // CORE_RENDER_OHOS_KRRENDERVIEW_H

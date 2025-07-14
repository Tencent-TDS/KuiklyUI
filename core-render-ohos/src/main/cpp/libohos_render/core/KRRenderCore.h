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

#ifndef CORE_RENDER_OHOS_KRRENDERCORE_H
#define CORE_RENDER_OHOS_KRRENDERCORE_H

/**
 * 负责渲染流程核心逻辑模块。
 */
#include "libohos_render/context/IKRRenderNativeContextHandler.h"
#include "libohos_render/context/KRRenderContextParams.h"
#include "libohos_render/layer/IKRRenderLayer.h"
#include "libohos_render/scheduler/KRUIScheduler.h"
#include "libohos_render/view/IKRRenderView.h"

enum class KRInitState {
    kStateKRRenderViewInit = 1,  //  Native层RootView初始化
    kStateInitCoreStart = 2,
    kStateInitCoreFinish = 3,
    kStateInitContextStart = 4,
    kStateInitContextFinish = 5,
    kStateCreateInstanceStart = 6,
    kStateCreateInstanceFinish = 7
};

class KRRenderCore : public std::enable_shared_from_this<KRRenderCore>,
                     public ICallNativeCallback,
                     public KRRenderUISchedulerDelegate {
 public:
    /**
     * 唯一初始化构造方法
     * @praram renderView 根渲染容器
     * @param context 页面上下文
     * */
    KRRenderCore(std::weak_ptr<IKRRenderView> renderView, std::shared_ptr<KRRenderContextParams> context);
    ~KRRenderCore(){
        if (uiScheduler_){
            uiScheduler_->ResetDelegate();
            uiScheduler_ = nullptr;
        }
        contextHandler_->RegisterCallNative(nullptr);
    }

    /** ICallNativeCallback interface override */
    std::shared_ptr<KRRenderValue>
    OnCallNative(const KuiklyRenderNativeMethod &method, std::shared_ptr<KRRenderValue> &arg0,
                 std::shared_ptr<KRRenderValue> &arg1, std::shared_ptr<KRRenderValue> &arg2,
                 std::shared_ptr<KRRenderValue> &arg3, std::shared_ptr<KRRenderValue> &arg4,
                 std::shared_ptr<KRRenderValue> &arg5) override;
    /** KRRenderUISchedulerDelegate interface override */
    void WillPerformUITasksWithScheduler() override;
    /** core初始化之后必须调用该DidInit进行初始化 */
    void DidInit();
    /**
     * 发送页面事件到kotlin侧
     * @param event_name 事件名
     * @param json_data json数据字符串）
     */
    void SendEvent(std::string event_name, const std::string &json_data);

    /**
     * 获取渲染节点视图（要求在主线程调用）
     * @param tag 所在tag
     * @return 对应节点view
     */
    std::shared_ptr<IKRRenderViewExport> GetView(int tag);

    /**
     * 获取渲染节点视图（要求在主线程调用）
     * @param tag 所在tag
     * @return 对应节点view
     */
    std::shared_ptr<IKRRenderModuleExport> GetModule(std::string &module_name);

    /**
     * 获取渲染节点视图（要求在主线程调用）
     * @param tag 所在tag
     * @return 对应节点view
     */
    std::shared_ptr<IKRRenderModuleExport> GetModuleOrCreate(std::string &module_name);

    /**
     * @brief Core销毁前调用，用于Core提前发送事件到Kotlin侧销毁内在资源.
     * @param instanceId 用于回调销毁RenderManager存储的RenderView的标识
     */
    void WillDealloc(const std::string &instanceId);
    /**
     * 添加任务到主线程中
     * @param task
     */
    void AddTaskToMainQueueWithTask(const KRSchedulerTask &task);
    /**
     * 执行任务当该主线程loop结束时
     */
    void PerformTaskWhenMainThreadEnd(const KRSchedulerTask &task);
    /**
     * 是否正在执行主线程任务中
     */
    bool IsPerformMainTasking();
    /**
     * 通知core初始化事件
     * @param state
     */
    void notifyInitState(KRInitState state);

 private:
    /** UI任务调度器 */
    std::shared_ptr<KRUIScheduler> uiScheduler_;
    /** 根渲染容器 */
    std::weak_ptr<IKRRenderView> renderView_;
    /** 页面上下文，包含page_name, page_data, 执行模式*/
    std::shared_ptr<KRRenderContextParams> context_;
    /** Kotlin产物侧协议的实现者 */
    std::shared_ptr<IKRRenderNativeContextHandler> contextHandler_;
    /** C-API渲染层协议的实现者 */
    std::shared_ptr<IKRRenderLayer> renderLayerHandler_;
    /** 默认NUll值 */
    std::shared_ptr<KRRenderValue> defaultNullValue_;
    /** 正在从主线程同步任务到context线程 */
    bool syncingPerformTaskMainThreadToContextThread = false;

    /** callback 是否为同步方法 */
    bool IsSyncCallback(const KRAnyValue &params);
    /** callback 是否为 keep alive 类型 */
    bool IsCallbackKeepAlive(const KRAnyValue &params);
    /** 调用kotlin侧方法，实现与kotlin侧通信 */
    void CallKotlinMethod(const KuiklyRenderContextMethod &method, const KRAnyValue &arg1, const KRAnyValue &arg2,
                          const KRAnyValue &arg3, const KRAnyValue &arg4, const KRAnyValue &arg5);
    /** 执行kotlin call native方法*/
    KRAnyValue PerformNativeCallback(const KuiklyRenderNativeMethod &method, const KRAnyValue &arg1, const KRAnyValue &arg2,
                                     const KRAnyValue &arg3, const KRAnyValue &arg4, const KRAnyValue &arg5, bool sync);
    bool ShouldSyncCallMethod(const KuiklyRenderNativeMethod &method, std::shared_ptr<KRRenderValue> &arg5);

    void OnDestroy();
};

#endif  // CORE_RENDER_OHOS_KRRenderCore_H

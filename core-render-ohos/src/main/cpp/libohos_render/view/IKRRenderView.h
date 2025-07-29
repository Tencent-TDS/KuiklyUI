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

#ifndef CORE_RENDER_OHOS_IKRRENDERVIEW_H
#define CORE_RENDER_OHOS_IKRRENDERVIEW_H

#include <arkui/native_type.h>
#include <rawfile/raw_file_manager.h>
#include <memory>
#include "libohos_render/context/KRRenderContextParams.h"
#include "libohos_render/performance/KRPerformanceManager.h"
#include "libohos_render/scheduler/IKRScheduler.h"

class IKRRenderViewExport;
class IKRRenderModuleExport;
class KRSnapshotManager;

class IKRRenderView : public std::enable_shared_from_this<IKRRenderView> {
 public:
    IKRRenderView() {}

    /**
     * 发送页面事件到kotlin侧
     * @param event_name 事件名
     * @param json_data json数据字符串）,该data可参考以下代码：
     *  参考代码：
     *    KRRenderValue::Map data;
     *    data["width"] = std::make_shared<KRRenderValue>(width);
     *    data["height"] = std::make_shared<KRRenderValue>(height);
     *    auto json_data = std::make_shared<KRRenderValue>(data)->toString();
     */
    virtual void SendEvent(std::string event_name, const std::string &json_data) = 0;

    /**
     * 获取渲染节点视图（要求在主线程调用）
     * @param tag 所在tag
     * @return 对应节点view
     */
    virtual std::shared_ptr<IKRRenderViewExport> GetView(int tag) = 0;

    /**
     * 获取渲染节点视图（要求在主线程调用）
     * @param tag 所在tag
     * @return 对应节点view
     */
    virtual std::shared_ptr<IKRRenderModuleExport> GetModule(std::string &module_name) = 0;

    /**
     * 获取渲染节点视图（要求在主线程调用）
     * @param tag 所在tag
     * @return 对应Module（不存在则新建）
     */
    virtual std::shared_ptr<IKRRenderModuleExport> GetModuleOrCreate(std::string &module_name) = 0;

    /**
     * 添加内容View
     * @param contentView
     * @param index
     */
    virtual void AddContentView(const std::shared_ptr<IKRRenderViewExport> contentView, int index) = 0;

    /**
     * 添加任务到主线程中执行，注意在context线程中调用
     */
    virtual void AddTaskToMainQueueWithTask(const KRSchedulerTask &task) = 0;

    /**
     * 执行任务当该主线程loop结束时
     */
    virtual void PerformTaskWhenMainThreadEnd(const KRSchedulerTask &task) = 0;

    /**
     * 是否正在执行主线程任务中
     */
    virtual bool IsPerformMainTasking() = 0;

    /**
     * 获取上下文
     * @return
     */
    virtual std::shared_ptr<KRRenderContextParams> GetContext() = 0;
    /**
     * 获取arkts层传递下来的UIContext
     */
    virtual const ArkUI_ContextHandle &GetUIContextHandle() const = 0;

    virtual KRSnapshotManager *GetSnapshotManager() = 0;
    /**
     * 获取本地资源管理器
     * @return
     */
    virtual NativeResourceManager *GetNativeResourceManager() = 0;

    virtual std::shared_ptr<KRPerformanceManager> GetPerformanceManager() = 0;  //  性能采集管理类
};

#endif  // CORE_RENDER_OHOS_IKRRENDERVIEW_H

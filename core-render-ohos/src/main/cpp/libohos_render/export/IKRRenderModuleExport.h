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

#ifndef CORE_RENDER_OHOS_IKRRENDERMODULEEXPORT_H
#define CORE_RENDER_OHOS_IKRRENDERMODULEEXPORT_H

#define FOAWARD_ARTKS_MODULE_NAME "FOAWARD_ARTKS_MODULE_NAME"

#include <unordered_map>
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/manager/KRArkTSManager.h"
#include "libohos_render/scheduler/KRContextScheduler.h"
#include "libohos_render/view/IKRRenderView.h"

class IKRRenderModuleExport;
using KRModuleCreator = std::function<std::shared_ptr<IKRRenderModuleExport>()>;

class IKRRenderModuleExport : public std::enable_shared_from_this<IKRRenderModuleExport> {
 public:
    /**
     * Kotlin侧调用当前 module 方法
     * @param sync 是否为同步调用该方法
     * @param method 方法名
     * @param params 方法数据
     * @param callback 可空的方法参数callback闭包
     * @return
     */
    virtual KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                  const KRRenderCallback &callback) {
        return nullptr;
    }

    /**
     * Kotlin侧调用当前 module 方法
     * @param sync 是否为同步调用该方法
     * @param method 方法名
     * @param params 方法数据
     * @param callback 可空的方法参数callback闭包
     * @param callback_keep_alive callback是否keep alive
     * @return
     */
    virtual KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                  const KRRenderCallback &callback, bool callback_keep_alive) {
        return this->CallMethod(sync, method, params, callback);
    }

    /**
     * Module 销毁时，此方法会被调用，用于销毁 module 中的资源
     */
    virtual void OnDestroy() {}

    /**
     * 异步调用ArkTS侧Module方法
     * @param method 方法名
     * @param params 方法传参
     * @param callback 回调闭包参数
     * @return 返回值
     */
    KRAnyValue CallArkTSMethod(const std::string &method, KRAnyValue params, const KRRenderCallback &callback,
                               bool callback_keep_alive = false) {
        return ToCallArkTSMethod(false, GetModuleName(), method, params, callback, callback_keep_alive);
    }

    /**
     * 同步调用ArkTS侧Module方法
     * @param method 方法名
     * @param params 方法传参
     * @param callback 回调闭包参数
     * @param callback_keep_alive callback 是否 keep alive
     * @return 返回值
     */
    KRAnyValue SyncCallArkTSMethod(const std::string &method, KRAnyValue params, const KRRenderCallback &callback,
                                   bool callback_keep_alive = false) {
        return ToCallArkTSMethod(true, GetModuleName(), method, params, callback, callback_keep_alive);
    }

    /**
     * 调用ArkTS侧Module方法
     * @param isSync 是否同步调用module方法。
     *               - isSync=false（hot path）：lambda 投递回主线程后立即返回 nullptr，
     *                 调用方不应依赖返回值。零额外堆分配。
     *               - isSync=true （cold path）：阻塞等待主线程 lambda 执行完毕后返回
     *                 ArkTS 侧的返回值。ScheduleTaskOnMainThread(sync=true) 保证 task
     *                 在本函数返回前已执行完毕（worker 线程经 promise/future 等待，
     *                 主线程上 inline 执行），lambda 通过引用捕获的栈变量在使用期间
     *                 始终有效。
     * @param module_name 传入所调用的Module名字，注：和当前Module注册时同名, 默认可以通过GetModuleName()获取
     * @param method 方法名
     * @param params 方法传参
     * @param callback 回调闭包参数
     * @param callback_keep_alive callback 是否 keep alive
     * @return isSync=true 时返回 ArkTS 侧的返回值；isSync=false 时恒为 nullptr
     */
    KRAnyValue ToCallArkTSMethod(bool isSync, const std::string &module_name, const std::string &method,
                                 KRAnyValue params, const KRRenderCallback &callback,
                                 bool callback_keep_alive = false) {
        auto instance_id = instance_id_;
        if (!isSync) {
            // hot path：异步投递，无需回传结果，全部按值捕获，零额外堆分配。
            KRContextScheduler::ScheduleTaskOnMainThread(
                false, [module_name, method, instance_id, params, callback, callback_keep_alive] {
                    auto module_name_value = KRRenderValue::Make(module_name);
                    auto method_name = KRRenderValue::Make(method);
                    (void)KRArkTSManager::GetInstance().CallArkTSMethod(
                        instance_id, KRNativeCallArkTSMethod::CallModuleMethod, module_name_value, method_name,
                        params, nullptr, nullptr, callback, callback_keep_alive);
                });
            return nullptr;
        }
        // cold path：同步等待主线程返回值。ScheduleTaskOnMainThread(sync=true) 保证 task
        // 在本函数返回前一定执行完毕，因此 lambda 引用捕获的栈变量 result 在 lambda 使用期间
        // 始终有效；lambda 不会在 result 析构后再被读到。
        KRAnyValue result = nullptr;
        KRContextScheduler::ScheduleTaskOnMainThread(
            true, [&result, &module_name, &method, &instance_id, &params, &callback, callback_keep_alive] {
                auto module_name_value = KRRenderValue::Make(module_name);
                auto method_name = KRRenderValue::Make(method);
                result = KRArkTSManager::GetInstance().CallArkTSMethod(
                    instance_id, KRNativeCallArkTSMethod::CallModuleMethod, module_name_value, method_name, params,
                    nullptr, nullptr, callback, callback_keep_alive);
            });
        return result;
    }

    /**
     * 注册Module创建器
     * @param creator
     */
    static void RegisterModuleCreator(const std::string &module_name, const KRModuleCreator &creator) {
        GetRegisterModuleCreator()[module_name] = creator;
    }

    /**
     * 注册通用转发ArkTS层Module创建器
     * @param creator
     */
    static void RegisterForwardArkTSModuleCreator(const KRModuleCreator &creator) {
        RegisterModuleCreator(std::string(FOAWARD_ARTKS_MODULE_NAME), creator);
    }

    /**
     * 指定ModuleName生成Module
     * @param module_name
     * @return 新的Module实例
     */
    static std::shared_ptr<IKRRenderModuleExport> CreateModule(const std::string &module_name) {
        auto it = GetRegisterModuleCreator().find(module_name);
        if (it != GetRegisterModuleCreator().end()) {
            return it->second();
        } else {  // 使用通用Module，转发到ArkTS层Module
            it = GetRegisterModuleCreator().find(std::string(FOAWARD_ARTKS_MODULE_NAME));
            if (it != GetRegisterModuleCreator().end()) {
                return it->second();
            }
        }
        return nullptr;
    }

    static std::unordered_map<std::string, KRModuleCreator> &GetRegisterModuleCreator() {
        static std::unordered_map<std::string, KRModuleCreator> gRegisterModuleCreator;
        return gRegisterModuleCreator;
    }

    static KRModuleCreator &GetOrSetForwardArkTModuleCreator(KRModuleCreator module_creator) {
        static KRModuleCreator gForwardArkTSModuleCreator;
        gForwardArkTSModuleCreator = module_creator;
        return gForwardArkTSModuleCreator;
    }

    void SetRootView(std::weak_ptr<IKRRenderView> root_view, std::string instance_id) {
        root_view_ = root_view;
        instance_id_ = instance_id;
    }

    void SetModuleName(const std::string &module_name) {
        module_name_ = module_name;
    }

    // 获取KT层Module得对应该Module注册模块名
    std::string GetModuleName() {
        return module_name_;
    }

    const std::string& GetInstanceId() {
        return instance_id_;
    }

    const std::weak_ptr<IKRRenderView> GetRootView() {
        return root_view_;
    }

 private:
    std::weak_ptr<IKRRenderView> root_view_;
    std::string instance_id_;
    std::string module_name_;
};

#endif  // CORE_RENDER_OHOS_IKRRENDERMODULEEXPORT_H

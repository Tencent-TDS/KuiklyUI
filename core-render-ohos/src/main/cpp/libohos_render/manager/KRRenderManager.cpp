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

#include "libohos_render/manager/KRRenderManager.h"

#include "libohos_render/context/KRRenderNativeContextHandlerManager.h"
#include "libohos_render/expand/components/ComponentsRegisterEntry.h"
#include "libohos_render/expand/events/KREventDispatchCenter.h"
#include "libohos_render/expand/modules/ModulesRegisterEntry.h"
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/KRViewUtil.h"
#include "libohos_render/utils/KRScopedSpinLock.h"

/**
 * Log print domain.
 */
const unsigned int LOG_PRINT_DOMAIN = 0xFF00;

KRRenderManager &KRRenderManager::GetInstance() {
    static KRRenderManager instance;  // 静态局部变量
    return instance;
}

KRRenderManager::KRRenderManager(){
    // 注册内置UI组件
    ComponentsRegisterEntry();
    // 注册内置Module
    ModulesRegisterEntry();
    // 触发EventCenter init
    KREventDispatchCenter::GetInstance();
}

KRRenderManager::~KRRenderManager(){
    // Add destroy calls even though they won't actually get called
}

void KRRenderManager::CreateRenderViewIfNeeded(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value args[2] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);

    ArkUI_NodeContentHandle contentHandle;
    OH_ArkUI_GetNodeContentFromNapiValue(env, args[0], &contentHandle);

    auto id = kuikly::util::getNApiArgsStdString(env, args[1]);
    if (id.empty() || contentHandle == nullptr) {
        return;
    }

    auto renderView = GetRenderView(id);

    if (renderView == nullptr) {
        auto render_view = std::make_shared<KRRenderView>(contentHandle);
        SetRenderView(id, render_view);
    }
}

void KRRenderManager::Export(napi_env env, napi_value exports) {
    KRMainThread::Export(env, exports);
}

std::shared_ptr<KRRenderView> KRRenderManager::GetRenderView(const std::string &instanceId) {
    KRScopedSpinLock lock(&render_view_map_lock_);
    if (render_view_map_.find(instanceId) == render_view_map_.end()) {
        return nullptr;
    }
    return render_view_map_[instanceId];
}

bool KRRenderManager::SetRenderView(std::string &instanceId, std::shared_ptr<KRRenderView> &renderView) {
    KRScopedSpinLock lock(&render_view_map_lock_);
    if (render_view_map_.find(instanceId) == render_view_map_.end()) {
        render_view_map_[instanceId] = renderView;
        return true;
    }
    return false;
}

void KRRenderManager::DestroyRenderView(std::string &instanceId) {
    {
        KRScopedSpinLock lock(&render_view_map_lock_);
        if (auto it = render_view_map_.find(instanceId); it != render_view_map_.end()) {
            if (it->second != nullptr) {
                it->second->WillDestroy(instanceId);
            }
        }
    }
}

void KRRenderManager::DestroyRenderViewCallBack(const std::string &instanceId) {
    {
        KRScopedSpinLock lock(&render_view_map_lock_);
        if (render_view_map_.find(instanceId) != render_view_map_.end()) {
            render_view_map_.erase(instanceId);
        }
    }
    {
        KRScopedSpinLock lock(&launch_init_time_map_lock_);
        if (launch_init_time_map_.find(instanceId) != launch_init_time_map_.end()) {
            launch_init_time_map_.erase(instanceId);
        }
    }
}


void KRRenderManager::OnLaunchStart(std::string &instanceId) {
    auto now_system = std::chrono::system_clock::
        now();  //  性能采集这里使用system_clock，原因是KuiklyCore返回的页面加载耗时是时间戳值。
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now_system.time_since_epoch());
    KRScopedSpinLock lock(&launch_init_time_map_lock_);
    launch_init_time_map_[instanceId] =
        duration.count();  //  先把ArkTs层的启动时间缓存一下，等Native RootView初始化时传入
}

int64_t KRRenderManager::GetLaunchStartTime(std::string &instanceId) {
    KRScopedSpinLock lock(&launch_init_time_map_lock_);
    if (launch_init_time_map_.find(instanceId) != launch_init_time_map_.end()) {
        return launch_init_time_map_[instanceId];
    }
    return 0;
}

void KRRenderManager::RegisterExcuteModeCreator(
    const std::shared_ptr<KRRenderExecuteModeWrapper> &execute_mode_wrapper) {
    if (execute_mode_wrapper) {
        KRRenderExecuteMode::RegisterExecuteModeCreator(execute_mode_wrapper->GetMode(),
                                                        execute_mode_wrapper->GetExecuteModeCreator());
        KRRenderNativeContextHandlerManager::RegisterContextHandlerCreator(
            execute_mode_wrapper->GetMode(), execute_mode_wrapper->GetContextHandlerCreator());
    }
}
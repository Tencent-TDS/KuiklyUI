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

#ifndef CORE_RENDER_OHOS_KRRENDERMANAGER_H
#define CORE_RENDER_OHOS_KRRENDERMANAGER_H

#include <string>
#include <pthread.h>
#include "libohos_render/context/KRRenderExecuteModeWrapper.h"
#include "libohos_render/view/KRRenderView.h"
#include "libohos_render/utils/KRScopedSpinLock.h"
/** 全局递增实例ID */
static int gGlobalInstanceId = 0;

class KRRenderManager {
 public:
    static KRRenderManager &GetInstance();

    // 禁止复制和赋值
    KRRenderManager(const KRRenderManager &) = delete;
    KRRenderManager &operator=(const KRRenderManager &) = delete;

    void Export(napi_env env, napi_value exports);
    std::shared_ptr<KRRenderView> GetRenderView(const std::string &instanceId);

    void DestroyRenderView(std::string &instanceId);

    void DestroyRenderViewCallBack(const std::string &instanceId);

    void OnLaunchStart(std::string &instanceId);  //  ArkTS层页面启动事件
    int64_t GetLaunchStartTime(std::string &instanceId);
    void RegisterExcuteModeCreator(const std::shared_ptr<KRRenderExecuteModeWrapper> &execute_mode_wrapper);

    void CreateRenderViewIfNeeded(napi_env env, napi_callback_info info);

 private:
    KRSpinLock render_view_map_lock_;
    std::unordered_map<std::string, std::shared_ptr<KRRenderView>> render_view_map_;
    KRSpinLock launch_init_time_map_lock_;
    std::unordered_map<std::string, int64_t> launch_init_time_map_;
    KRRenderManager();  // 构造函数私有化
    ~KRRenderManager();
    bool SetRenderView(std::string &instanceId, std::shared_ptr<KRRenderView> &renderView);
    // 在这里添加其他私有成员变量和函数
};

#endif  // CORE_RENDER_OHOS_KRRENDERMANAGER_H

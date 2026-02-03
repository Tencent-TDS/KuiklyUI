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

#ifndef OHOSAPP_KRRENDERCONTEXTPARAMSEXT_H
#define OHOSAPP_KRRENDERCONTEXTPARAMSEXT_H

#include <string>
#include <memory>

#if __has_include("libohos_render/layer/KRTurboDisplayRenderLayerHandler.h")
  #define TURBO_DISPLAY_AVAILABLE 1
#else
  #define TURBO_DISPLAY_AVAILABLE 0
#endif

class KRRenderContextParams;

#if TURBO_DISPLAY_AVAILABLE
/**
 * TurboDisplay 配置数据结构
 */
struct KRTurboDisplayConfigData {
    int diff_dom_mode = 1;       // 默认 KRStructureAwareDiffDOM
    int diff_view_mode = 1;      // 默认 KRNormalDiffView（禁用延迟 diff）
    bool auto_update = true;     // 默认启用自动更新
    bool enable_persistent_update = true;   // 默认真实DOM树会接收变化
};

/**
 * 从 Context 获取 TurboDisplayKey
 * @param context 渲染上下文参数
 * @return TurboDisplayKey，如果不存在返回空字符串
 */
std::string GetTurboDisplayKeyFromContext(std::shared_ptr<KRRenderContextParams> context);

/**
 * 从 Context 获取 TurboDisplayConfig
 * @param context 渲染上下文参数
 * @return TurboDisplayConfig，如果不存在返回默认配置
 */
std::shared_ptr<KRTurboDisplayConfigData> GetTurboDisplayConfigFromContext(
    std::shared_ptr<KRRenderContextParams> context);

#endif // TURBO_DISPLAY_AVAILABLE

#endif //OHOSAPP_KRRENDERCONTEXTPARAMSEXT_H

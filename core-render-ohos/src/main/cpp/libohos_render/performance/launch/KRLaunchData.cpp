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

#include "KRLaunchData.h"

#include "libohos_render/utils/KRJSONObject.h"
#include "thirdparty/cJSON/cJSON.h"

constexpr char kKeyInitViewCost[] = "initViewCost";
constexpr char kKeyFetchContextCodeCost[] = "fetchContextCodeCost";
constexpr char kKeyInitRenderCoreCost[] = "initRenderCoreCost";
constexpr char kKeyInitRenderContextCost[] = "initRenderContextCost";
constexpr char kKeyNewPageCost[] = "newPageCost";
constexpr char kKeyPageBuildCost[] = "pageBuildCost";
constexpr char kKeyPageLayoutCost[] = "pageLayoutCost";
constexpr char kKeyOnCreatePageCost[] = "createPageCost";
constexpr char kKeyOnCreateInstanceCost[] = "createInstanceCost";
constexpr char kKeyOnRenderCost[] = "renderCost";
constexpr char kKeyFirstFramePaintCost[] = "firstPaintCost";

KRLaunchData::KRLaunchData(long *timestamps, int size) {
    timestamps_array_ = timestamps;
    array_size_ = size;
    init_render_view_cost_ = GetCost(kEventOnInitCoreStart, kEventOnInit);
    init_render_core_cost_ = GetCost(kEventOnInitCoreFinish, kEventOnInitCoreStart);
    init_render_context_cost_ = GetCost(kEventOnContextInitFinish, kEventOnContextInitStart);
    create_instance_cost_ = GetCost(kEventOnCreateInstanceFinish, kEventOnCreateInstanceStart);
    new_page_cost_ = GetCost(kEventOnNewPageFinish, kEventOnNewPageStart);
    page_build_cost_ = GetCost(kEventOnPageBuildFinish, kEventOnPageBuildStart);
    page_layout_cost_ = GetCost(kEventOnPageLayoutFinish, kEventOnPageLayoutStart);
    page_create_cost_ = GetCost(kEventOnCreatePageFinish, kEventOnCreatePageStart);
    render_cost_ = GetCost(kEventOnFirstFramePaint, kEventOnCreatePageFinish);
    first_frame_paint_cost_ = GetCost(kEventOnFirstFramePaint, kEventOnInit);
}

long KRLaunchData::GetCost(KRLaunchEvent cur, KRLaunchEvent pre) {
    if (cur > array_size_ || pre > array_size_) {
        return 0L;
    }
    return timestamps_array_[cur] - timestamps_array_[pre];
}

std::string KRLaunchData::ToJSONString() {
    cJSON *launch_monitor = cJSON_CreateObject();
    cJSON_AddNumberToObject(launch_monitor, kKeyFirstFramePaintCost, first_frame_paint_cost_);
    cJSON_AddNumberToObject(launch_monitor, kKeyInitViewCost, init_render_view_cost_);
    cJSON_AddNumberToObject(launch_monitor, kKeyInitRenderCoreCost, init_render_core_cost_);
    cJSON_AddNumberToObject(launch_monitor, kKeyInitRenderContextCost, init_render_context_cost_);
    cJSON_AddNumberToObject(launch_monitor, kKeyOnCreateInstanceCost, create_instance_cost_);
    cJSON_AddNumberToObject(launch_monitor, kKeyNewPageCost, new_page_cost_);
    cJSON_AddNumberToObject(launch_monitor, kKeyPageBuildCost, page_build_cost_);
    cJSON_AddNumberToObject(launch_monitor, kKeyPageLayoutCost, page_layout_cost_);
    cJSON_AddNumberToObject(launch_monitor, kKeyOnCreatePageCost, page_create_cost_);
    cJSON_AddNumberToObject(launch_monitor, kKeyOnRenderCost, render_cost_);
    std::string result = cJSON_Print(launch_monitor);
    cJSON_Delete(launch_monitor);
    return result;
}
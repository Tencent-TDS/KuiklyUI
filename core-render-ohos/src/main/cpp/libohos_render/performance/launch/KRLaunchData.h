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

#ifndef CORE_RENDER_OHOS_KRLAUNCHDATA_H
#define CORE_RENDER_OHOS_KRLAUNCHDATA_H
#include <string>

enum KRLaunchEvent {
    kEventOnInit = 0,
    kEventOnKRRenderViewInit = 1,
    kEventOnInitCoreStart = 2,
    kEventOnInitCoreFinish = 3,
    kEventOnContextInitStart = 4,
    kEventOnContextInitFinish = 5,
    kEventOnCreateInstanceStart = 6,
    kEventOnNewPageStart = 7,
    kEventOnNewPageFinish = 8,
    kEventOnCreatePageStart = 9,
    kEventOnPageBuildStart = 10,
    kEventOnPageBuildFinish = 11,
    kEventOnPageLayoutStart = 12,
    kEventOnPageLayoutFinish = 13,
    kEventOnCreatePageFinish = 14,
    kEventOnCreateInstanceFinish = 15,
    kEventOnFirstFramePaint = 16,
    kEventOnPause = 17,
    kEventCont = 18  //  事件数组大小
};

class KRLaunchData {
 public:
    KRLaunchData(long *timestamps, int size);
    std::string ToJSONString();

 private:
    long GetCost(KRLaunchEvent cur, KRLaunchEvent pre);
    int64_t *timestamps_array_;
    int array_size_;
    long init_render_view_cost_ = 0;
    long init_render_core_cost_ = 0;
    long init_render_context_cost_ = 0;
    long create_instance_cost_ = 0;
    long new_page_cost_ = 0;
    long page_build_cost_ = 0;
    long page_layout_cost_ = 0;
    long page_create_cost_ = 0;
    long render_cost_ = 0;
    long first_frame_paint_cost_ = 0;
};
#endif  // CORE_RENDER_OHOS_KRLAUNCHDATA_H

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

#include "KRLaunchMonitor.h"

#include "libohos_render/utils/KRRenderLoger.h"

const char KRLaunchMonitor::kMonitorName[] = "LaunchMonitor";
constexpr char kKRLaunchMonitorTag[] = "LaunchMonitor";

void KRLaunchMonitor::SetArkLaunchTime(int64_t timestamp) {
    event_time_stamps_[kEventOnInit] = timestamp;
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "SetArkLaunchTime：" << event_time_stamps_[kEventOnInit];
}

void KRLaunchMonitor::OnKRRenderViewInit() {
    event_time_stamps_[kEventOnKRRenderViewInit] = CurrentTimeMillis();
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "OnKRRenderViewInit：" <<
    //    event_time_stamps_[kEventOnKRRenderViewInit];
}

void KRLaunchMonitor::OnInitCoreStart() {
    event_time_stamps_[kEventOnInitCoreStart] = CurrentTimeMillis();
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "OnInitCoreStart：" << event_time_stamps_[kEventOnInitCoreStart];
}
void KRLaunchMonitor::OnInitCoreFinish() {
    event_time_stamps_[kEventOnInitCoreFinish] = CurrentTimeMillis();
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "OnInitCoreFinish：" <<
    //    event_time_stamps_[kEventOnInitCoreFinish];
}
void KRLaunchMonitor::OnInitContextStart() {
    event_time_stamps_[kEventOnContextInitStart] = CurrentTimeMillis();
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "OnInitContextStart：" <<
    //    event_time_stamps_[kEventOnContextInitStart];
}
void KRLaunchMonitor::OnInitContextFinish() {
    event_time_stamps_[kEventOnContextInitFinish] = CurrentTimeMillis();
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "OnInitContextFinish：" <<
    //    event_time_stamps_[kEventOnContextInitFinish];
}
void KRLaunchMonitor::OnCreateInstanceStart() {
    event_time_stamps_[kEventOnCreateInstanceStart] = CurrentTimeMillis();
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "OnCreateInstanceStart：" <<
    //    event_time_stamps_[kEventOnCreateInstanceStart];
}
void KRLaunchMonitor::OnCreateInstanceFinish() {
    event_time_stamps_[kEventOnCreateInstanceFinish] = CurrentTimeMillis();
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "OnCreateInstanceFinish：" <<
    //    event_time_stamps_[kEventOnCreateInstanceFinish];
}
void KRLaunchMonitor::OnFirstFramePaint() {
    event_time_stamps_[kEventOnFirstFramePaint] = CurrentTimeMillis();
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "OnFirstFramePaint：" <<
    //    event_time_stamps_[kEventOnFirstFramePaint];
}

void KRLaunchMonitor::OnPageCreateFinish(KRPageCreateTrace &trace) {
    event_time_stamps_[kEventOnNewPageStart] = trace.newPage_start_timeMills;
    event_time_stamps_[kEventOnNewPageFinish] = trace.newPage_end_timeMills;
    event_time_stamps_[kEventOnCreatePageStart] = trace.create_start_timeMills;
    event_time_stamps_[kEventOnPageBuildStart] = trace.build_start_timeMills;
    event_time_stamps_[kEventOnPageBuildFinish] = trace.build_end_timeMills;
    event_time_stamps_[kEventOnPageLayoutStart] = trace.layout_start_timeMills;
    event_time_stamps_[kEventOnPageLayoutFinish] = trace.layout_end_timeMills;
    event_time_stamps_[kEventOnCreatePageFinish] = trace.create_end_timeMills;
    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "OnPageCreateFinish";
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "kEventOnNewPageStart:" <<
    //    event_time_stamps_[kEventOnNewPageStart]; KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) <<
    //    "kEventOnNewPageFinish:" << event_time_stamps_[kEventOnNewPageFinish];
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "kEventOnCreatePageStart:" <<
    //    event_time_stamps_[kEventOnCreatePageStart]; KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) <<
    //    "kEventOnPageBuildStart:" << event_time_stamps_[kEventOnPageBuildStart];
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "kEventOnPageBuildFinish:" <<
    //    event_time_stamps_[kEventOnPageBuildFinish]; KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) <<
    //    "kEventOnPageLayoutStart:" << event_time_stamps_[kEventOnPageLayoutStart];
    //    KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) << "kEventOnPageLayoutFinish:" <<
    //    event_time_stamps_[kEventOnPageLayoutFinish]; KR_LOG_INFO_WITH_TAG(kKRLaunchMonitorTag) <<
    //    "kEventOnCreatePageFinish:" << event_time_stamps_[kEventOnCreatePageFinish];
}

int64_t KRLaunchMonitor::CurrentTimeMillis() {
    auto now = std::chrono::system_clock::now();  //  考虑到和KuiklyCore页面创建事件对比，这里使用system_clock获取时间戳
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());
    return duration.count();
}

std::string KRLaunchMonitor::GetMonitorData() {
    KRLaunchData launch_data = KRLaunchData(event_time_stamps_, kEventCont);
    return launch_data.ToJSONString();
}
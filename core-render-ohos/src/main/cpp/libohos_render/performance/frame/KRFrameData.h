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

#ifndef CORE_RENDER_OHOS_KRFRAMEDATA_H
#define CORE_RENDER_OHOS_KRFRAMEDATA_H

#include <string>

constexpr char kKeyTotalDuration[] = "totalDuration";
constexpr char kKeyHitchesDuration[] = "hitchesDuration";
constexpr char kKeyFrameCount[] = "frameCount";
constexpr char kKeyMainFPS[] = "mainFPS";
constexpr char kKeyKotlinFPS[] = "kotlinFPS";

class KRFrameData {
public:
    long long total_duration = 0;            // 总耗时 (ms)
    long long hitches_duration = 0;          // 卡顿耗时 (ms)
    long long drive_hitches_duration = 0;     // 驱动卡顿耗时 (ms)
    long long frame_count = 0;               // 总帧数
    long long drive_frame_count = 0;          // 驱动帧数

    // 获取 FPS
    double getFps() const;

    // todo
    double getKotlinFps() const;

    std::string ToJSONString();

    void Reset();
};

#endif //CORE_RENDER_OHOS_KRFRAMEDATA_H
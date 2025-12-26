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

#include "KRFrameData.h"
#include "thirdparty/cJSON/cJSON.h"

double KRFrameData::getFps() const {
    if (totalDuration <= 0) return 0;
    return ((float)(totalDuration - hitchesDuration) / totalDuration) * 60;
}

std::string KRFrameData::ToJSONString() {
    cJSON *frame_data = cJSON_CreateObject();
    cJSON_AddNumberToObject(frame_data, kKeyTotalDuration, totalDuration);
    cJSON_AddNumberToObject(frame_data, kKeyHitchesDuration, totalDuration);
    cJSON_AddNumberToObject(frame_data, kKeyFrameCount, totalDuration);
    cJSON_AddNumberToObject(frame_data, kKeyMainFPS, getFps());
    char* json_str = cJSON_Print(frame_data);
    std::string result = json_str;
    free(json_str);
    cJSON_Delete(frame_data);
    return result;
}

void KRFrameData::Reset() {
    totalDuration = 0;
    hitchesDuration = 0;
    frameCount = 0;
}
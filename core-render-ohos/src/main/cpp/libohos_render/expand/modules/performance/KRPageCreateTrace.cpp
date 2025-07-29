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

#include "libohos_render/expand/modules/performance/KRPageCreateTrace.h"

#include "libohos_render/utils/KRJSONObject.h"

constexpr char kEventOnCreateStart[] = "on_create_start";
constexpr char kEventOnNewPageStart[] = "on_new_page_start";
constexpr char kEventOnNewPageEnd[] = "on_new_page_end";
constexpr char kEventOnBuildStart[] = "on_build_start";
constexpr char kEventOnBuildEnd[] = "on_build_end";
constexpr char kEventOnLayoutStart[] = "on_layout_start";
constexpr char kEventOnLayoutEnd[] = "on_layout_end";
constexpr char kEventOnCreateEnd[] = "on_create_end";

KRPageCreateTrace::KRPageCreateTrace(std::string json_str) {
    auto json = kuikly::util::JSONObject::Parse(json_str);
    create_start_timeMills = json->GetNumber(kEventOnCreateStart);
    newPage_start_timeMills = json->GetNumber(kEventOnNewPageStart);
    newPage_end_timeMills = json->GetNumber(kEventOnNewPageEnd);
    build_start_timeMills = json->GetNumber(kEventOnBuildStart);
    build_end_timeMills = json->GetNumber(kEventOnBuildEnd);
    layout_start_timeMills = json->GetNumber(kEventOnLayoutStart);
    layout_end_timeMills = json->GetNumber(kEventOnLayoutEnd);
    create_end_timeMills = json->GetNumber(kEventOnCreateEnd);
}
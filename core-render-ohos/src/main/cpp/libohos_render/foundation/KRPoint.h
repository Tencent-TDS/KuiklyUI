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

#ifndef CORE_RENDER_OHOS_KRPOINT_H
#define CORE_RENDER_OHOS_KRPOINT_H

struct KRPoint {
    float x;
    float y;

    // 默认构造函数
    KRPoint() : x(0), y(0) {}

    // 参数构造函数
    KRPoint(float x_, float y_) : x(x_), y(y_) {}
};
#endif  // CORE_RENDER_OHOS_KRPOINT_H

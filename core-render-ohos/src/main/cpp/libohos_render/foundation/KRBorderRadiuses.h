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

#ifndef CORE_RENDER_OHOS_KRBORDERRADIUSES_H
#define CORE_RENDER_OHOS_KRBORDERRADIUSES_H

struct KRBorderRadiuses {
    float topLeft;
    float topRight;
    float bottomLeft;
    float bottomRight;

    // 默认构造函数
    KRBorderRadiuses() : topLeft(0), topRight(0), bottomLeft(0), bottomRight(0) {
        isDefault_ = true;
    }

    // 参数构造函数
    KRBorderRadiuses(float topLeft_, float topRight_, float bottomLeft_, float bottomRight_)
        : topLeft(topLeft_), topRight(topRight_), bottomLeft(bottomLeft_), bottomRight(bottomRight_) {}

    bool isDefaultZero() {
        return isDefault_;
    }

    bool isAllZero() {
        return topLeft == 0 && topRight == 0 && bottomLeft == 0 && bottomRight == 0;
    }

 private:
    bool isDefault_ = false;
};

#endif  // CORE_RENDER_OHOS_KRBORDERRADIUSES_H

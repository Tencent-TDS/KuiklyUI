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

#ifndef CORE_RENDER_OHOS_KRRECT_H
#define CORE_RENDER_OHOS_KRRECT_H

struct KRRect {
    float x;
    float y;
    float width;
    float height;

    // 默认构造函数
    KRRect() : x(0), y(0), width(0), height(0) {
        isDefault_ = true;
    }

    // 参数构造函数
    KRRect(float x_, float y_, float width_, float height_) : x(x_), y(y_), width(width_), height(height_) {}

    // 零大小的静态常量成员
    static const KRRect zero;

    bool isDefaultZero() {
        return isDefault_;
    }

 private:
    bool isDefault_ = false;
};

// 在类外初始化静态成员
// const KRRect KRRect::zero = KRRect(0, 0, 0, 0);

#endif  // CORE_RENDER_OHOS_KRRECT_H

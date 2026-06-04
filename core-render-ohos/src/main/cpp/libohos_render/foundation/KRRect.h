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

#include "libohos_render/foundation/KRPoint.h"
#include "libohos_render/foundation/KRSize.h"

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

    static KRRect RectFrom(KRPoint top_left, KRPoint bottom_right) {
        return KRRect(top_left.x, top_left.y, bottom_right.x - top_left.x, bottom_right.y - top_left.y);
    }
    bool IsValid() const {
        return width >= 0 && height >= 0;
    }
    KRPoint Origin() const {
        return KRPoint(x, y);
    }
    KRSize Size() const {
        return KRSize(width, height);
    }
    bool ContainsPoint(float px, float py) const {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
    bool ContainsPoint(KRPoint point) const {
        return ContainsPoint(point.x, point.y);
    }
    bool IsIntersect(const KRRect &other) const {
        float tmp_x = x > other.x ? x : other.x;
        float tmp_y = y > other.y ? y : other.y;
        float tmp_right = (x + width) < (other.x + other.width) ? (x + width) : (other.x + other.width);
        float tmp_bottom = (y + height) < (other.y + other.height) ? (y + height) : (other.y + other.height);
        return tmp_x <= tmp_right && tmp_y <= tmp_bottom;
    }
    bool operator==(const KRRect &other) const {
        return x == other.x && y == other.y && width == other.width && height == other.height;
    }

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

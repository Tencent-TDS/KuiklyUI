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

#ifndef CORE_RENDER_OHOS_KRSIZE_H
#define CORE_RENDER_OHOS_KRSIZE_H

struct KRSize {
    double width;
    double height;

    // 默认构造函数
    KRSize() : width(0), height(0) {}

    // 参数构造函数
    KRSize(double w, double h) : width(w), height(h) {}

    // 零大小的静态常量成员
    static const KRSize zero;
};

// 在类外初始化静态成员
// const KRSize KRSize::zero = KRSize(0, 0);

#endif  // CORE_RENDER_OHOS_KRSIZE_H

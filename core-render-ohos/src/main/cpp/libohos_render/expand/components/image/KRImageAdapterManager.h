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

#pragma once

#include "libohos_render/api/include/Kuikly/Kuikly.h"

class KRImageAdapterManager final {
 public:
    static KRImageAdapterManager *GetInstance();
    void RegisterImageAdapter(KRImageAdapter adapter);
    KRImageAdapter GetAdapter();
    void RegisterImageAdapterV2(KRImageAdapterV2 adapter);
    KRImageAdapterV2 GetAdapterV2();
 private:
    KRImageAdapterManager() = default;
    ~KRImageAdapterManager() = delete;

    KRImageAdapter adapter = nullptr;
    KRImageAdapterV2 adapter_v2 = nullptr;
};

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

#ifndef CORE_RENDER_OHOS_KRSCROLLERCONTENTINSET_H
#define CORE_RENDER_OHOS_KRSCROLLERCONTENTINSET_H

#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/utils/KRStringUtil.h"

class KRScrollerContentInset {
 public:
    explicit KRScrollerContentInset(const KRAnyValue &value) {
        auto content_inset_splits = kuikly::util::SplitString(value->toString(), ' ');
        top = content_inset_splits[0]->toFloat();
        start = content_inset_splits[1]->toFloat();
        bottom = content_inset_splits[2]->toFloat();
        end = content_inset_splits[3]->toFloat();
        if (content_inset_splits.size() >= 5) {
            animate = content_inset_splits[4]->toBool();
        }
    }

    bool animate = false;
    float start = 0.0;
    float top = 0.0;
    float end = 0.0;
    float bottom = 0.0;
};
#endif  // CORE_RENDER_OHOS_KRSCROLLERCONTENTINSET_H

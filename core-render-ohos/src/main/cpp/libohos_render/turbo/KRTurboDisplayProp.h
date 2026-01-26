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

#ifndef KUIKLY_TURBO_DISPLAY_PROP_H
#define KUIKLY_TURBO_DISPLAY_PROP_H

#include <string>
#include <memory>
#include <any>
#include <functional>
#include <vector>
#include "../foundation/KRCommon.h"

namespace KuiklyOhos {

enum KRTurboDisplayPropType {
    PROP_TYPE_ATTR = 0,
    PROP_TYPE_EVENT = 1,
    PROP_TYPE_FRAME = 2,
    PROP_TYPE_SHADOW = 3,
    PROP_TYPE_INSERT = 4
};

class KRTurboDisplayProp {
public:
    KRTurboDisplayProp(KRTurboDisplayPropType type, 
                       const std::string& key, 
                       const std::any& value);
    ~KRTurboDisplayProp();

    KRTurboDisplayPropType GetPropType() const { return prop_type_; }
    std::string GetPropKey() const { return prop_key_; }
    const std::any& GetPropValue() const { return prop_value_; }

    void SetPropType(const KRTurboDisplayPropType type) { prop_type_ = type; }
    void SetPropKey(const std::string& key) { prop_key_ = key; }
    void SetPropValue(const std::any& value) { prop_value_ = value; }

    std::shared_ptr<KRTurboDisplayProp> DeepCopy() const;
    void LazyEventIfNeed();
    void PerformLazyEventToCallback(const KRRenderCallback& callback);
    const std::vector<KRAnyValue>& GetLazyEventCallbackResults() const { return lazy_event_callback_results_; }

    std::vector<uint8_t> ToByteArray() const;
    static std::shared_ptr<KRTurboDisplayProp> CreateFromByteArray(const std::vector<uint8_t>& data);

private:
    KRTurboDisplayPropType prop_type_;
    std::string prop_key_;
    std::any prop_value_;
    std::vector<KRAnyValue> lazy_event_callback_results_;
};

} // namespace KuiklyOhos

#endif // KUIKLY_TURBO_DISPLAY_PROP_H

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

#ifndef KUIKLY_TURBO_DISPLAY_SHADOW_H
#define KUIKLY_TURBO_DISPLAY_SHADOW_H

#include <string>
#include <vector>
#include <memory>
#include "KRTurboDisplayProp.h"
#include "KRTurboDisplayNodeMethod.h"

namespace KuiklyOhos {

// ConstraintSize结构体
struct ConstraintSize {
    float width;
    float height;
    
    ConstraintSize() : width(0.0f), height(0.0f) {}
    ConstraintSize(float w, float h) : width(w), height(h) {}
    
    bool operator==(const ConstraintSize& other) const {
        return width == other.width && height == other.height;
    }
};

// TurboDisplay Shadow节点
class KRTurboDisplayShadow {
public:
    KRTurboDisplayShadow(int tag, const std::string& view_name);
    ~KRTurboDisplayShadow();

    // Getter
    int GetTag() const { return tag_; }
    std::string GetViewName() const { return view_name_; }
    
    // constraintSize getter
    ConstraintSize GetConstraintSize() const { return constraint_size_; }
    
    // 兼容旧接口
    float GetConstraintWidth() const { return constraint_size_.width; }
    float GetConstraintHeight() const { return constraint_size_.height; }

    const std::vector<std::shared_ptr<KRTurboDisplayProp>>& GetProps() const { return props_; }
    const std::vector<std::shared_ptr<KRTurboDisplayNodeMethod>>& GetCallMethods() const { return call_methods_; }

    // 属性管理
    void SetProp(const std::string& key, const std::any& prop_value);
    
    // calculateWithConstraintSize方法
    void CalculateWithConstraintSize(const ConstraintSize& size);
    
    // 兼容旧接口（重载）
    void CalculateWithConstraintSize(float width, float height);
    
    void AddMethodWithName(const std::string& name, const std::string& params);

    // 深拷贝
    std::shared_ptr<KRTurboDisplayShadow> DeepCopy() const;
    
    // 序列化/反序列化
    std::vector<uint8_t> ToByteArray() const;
    static std::shared_ptr<KRTurboDisplayShadow> CreateFromByteArray(const std::vector<uint8_t>& data);

private:
    std::shared_ptr<KRTurboDisplayProp> GetProp(const std::string& key);

    int tag_;
    std::string view_name_;
    ConstraintSize constraint_size_;  // constraintSize存储
    std::vector<std::shared_ptr<KRTurboDisplayProp>> props_;
    std::vector<std::shared_ptr<KRTurboDisplayNodeMethod>> call_methods_;
};

} // namespace KuiklyOhos

#endif // KUIKLY_TURBO_DISPLAY_SHADOW_H

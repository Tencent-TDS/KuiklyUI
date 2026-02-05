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

#include "KRTurboDisplayShadow.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include <cstring>

namespace KuiklyOhos {

KRTurboDisplayShadow::KRTurboDisplayShadow(int tag, const std::string& view_name)
    : tag_(tag), view_name_(view_name) {}

KRTurboDisplayShadow::~KRTurboDisplayShadow() = default;


void KRTurboDisplayShadow::CalculateWithConstraintSize(const ConstraintSize& size) {
    constraint_size_ = size;
}

void KRTurboDisplayShadow::CalculateWithConstraintSize(float width, float height) {
    constraint_size_.width = width;
    constraint_size_.height = height;
}

void KRTurboDisplayShadow::SetProp(const std::string& key, const std::any& prop_value) {
    auto prop = GetProp(key);
    if (prop == nullptr) {
        auto display_prop = std::make_shared<KRTurboDisplayProp>(PROP_TYPE_ATTR, key, prop_value);
        props_.push_back(display_prop);
    } else {
        prop->SetPropValue(prop_value);
    }
}

void KRTurboDisplayShadow::AddMethodWithName(const std::string& name, const std::string& params) {
    auto method = std::make_shared<KRTurboDisplayNodeMethod>();
    method->SetMethod(name);
    method->SetParams(params);
    call_methods_.push_back(method);
}

std::shared_ptr<KRTurboDisplayProp> KRTurboDisplayShadow::GetProp(const std::string& key) {
    for (auto& prop : props_) {
        if (prop->GetPropKey() == key) {
            return prop;
        }
    }
    return nullptr;
}

std::shared_ptr<KRTurboDisplayShadow> KRTurboDisplayShadow::DeepCopy() const {
    auto shadow = std::make_shared<KRTurboDisplayShadow>(tag_, view_name_);

    // 深拷贝props
    for (const auto& prop : props_) {
        shadow->props_.push_back(prop->DeepCopy());
    }

    // 深拷贝call_methods
    for (const auto& method : call_methods_) {
        shadow->call_methods_.push_back(method->DeepCopy());
    }

    return shadow;
}

// 序列化辅助函数
static void WriteInt(std::vector<uint8_t>& bytes, int value) {
    bytes.push_back((value >> 24) & 0xFF);
    bytes.push_back((value >> 16) & 0xFF);
    bytes.push_back((value >> 8) & 0xFF);
    bytes.push_back(value & 0xFF);
}

static void WriteString(std::vector<uint8_t>& bytes, const std::string& str) {
    WriteInt(bytes, static_cast<int>(str.size()));
    bytes.insert(bytes.end(), str.begin(), str.end());
}

static void WriteFloat(std::vector<uint8_t>& bytes, float value) {
    uint32_t bits;
    std::memcpy(&bits, &value, sizeof(float));
    bytes.push_back((bits >> 24) & 0xFF);
    bytes.push_back((bits >> 16) & 0xFF);
    bytes.push_back((bits >> 8) & 0xFF);
    bytes.push_back(bits & 0xFF);
}

static int ReadInt(const std::vector<uint8_t>& data, size_t& offset) {
    if (offset + 4 > data.size()) {
        throw std::runtime_error("ReadInt: insufficient data");
    }
    int value = (static_cast<int>(data[offset]) << 24) |
                (static_cast<int>(data[offset + 1]) << 16) |
                (static_cast<int>(data[offset + 2]) << 8) |
                static_cast<int>(data[offset + 3]);
    offset += 4;
    return value;
}

static std::string ReadString(const std::vector<uint8_t>& data, size_t& offset) {
    int length = ReadInt(data, offset);
    if (length < 0 || offset + length > data.size()) {
        throw std::runtime_error("ReadString: invalid length");
    }
    std::string str(data.begin() + offset, data.begin() + offset + length);
    offset += length;
    return str;
}

static float ReadFloat(const std::vector<uint8_t>& data, size_t& offset) {
    if (offset + 4 > data.size()) {
        throw std::runtime_error("ReadFloat: insufficient data");
    }
    uint32_t bits = (static_cast<uint32_t>(data[offset]) << 24) |
                    (static_cast<uint32_t>(data[offset + 1]) << 16) |
                    (static_cast<uint32_t>(data[offset + 2]) << 8) |
                    static_cast<uint32_t>(data[offset + 3]);
    offset += 4;
    float value;
    std::memcpy(&value, &bits, sizeof(float));
    return value;
}

std::vector<uint8_t> KRTurboDisplayShadow::ToByteArray() const {
    std::vector<uint8_t> bytes;
    
    try {
        // 1. 写入 tag
        WriteInt(bytes, tag_);
        
        // 2. 写入 viewName
        WriteString(bytes, view_name_);
        
        // 3. 写入 constraintSize（对齐 Android 的 "${width}|${height}" 格式）
        WriteFloat(bytes, constraint_size_.width);
        WriteFloat(bytes, constraint_size_.height);
        
        // 4. 写入 props 数量和内容
        WriteInt(bytes, static_cast<int>(props_.size()));
        for (const auto& prop : props_) {
            auto prop_bytes = prop->ToByteArray();
            WriteInt(bytes, static_cast<int>(prop_bytes.size()));
            bytes.insert(bytes.end(), prop_bytes.begin(), prop_bytes.end());
        }
        
        // 5. 写入 callMethods 数量和内容
        WriteInt(bytes, static_cast<int>(call_methods_.size()));
        for (const auto& method : call_methods_) {
            auto method_bytes = method->ToByteArray();
            WriteInt(bytes, static_cast<int>(method_bytes.size()));
            bytes.insert(bytes.end(), method_bytes.begin(), method_bytes.end());
        }
        
    } catch (const std::exception& e) {
        KR_LOG_ERROR << "[TurboDisplay-Serialize] Shadow serialization failed: " << e.what();
        return {};
    }
    
    return bytes;
}

std::shared_ptr<KRTurboDisplayShadow> KRTurboDisplayShadow::CreateFromByteArray(
    const std::vector<uint8_t>& data) {
    
    if (data.empty()) {
        return nullptr;
    }
    
    size_t offset = 0;
    
    try {
        // 1. 读取 tag
        int tag = ReadInt(data, offset);
        
        // 2. 读取 viewName
        std::string view_name = ReadString(data, offset);
        
        auto shadow = std::make_shared<KRTurboDisplayShadow>(tag, view_name);
        
        // 3. 读取 constraintSize
        float width = ReadFloat(data, offset);
        float height = ReadFloat(data, offset);
        shadow->constraint_size_ = ConstraintSize(width, height);
        
        // 4. 读取 props
        int props_count = ReadInt(data, offset);
        for (int i = 0; i < props_count; i++) {
            int prop_size = ReadInt(data, offset);
            if (offset + prop_size > data.size()) {
                throw std::runtime_error("Invalid prop data size");
            }
            std::vector<uint8_t> prop_data(data.begin() + offset, data.begin() + offset + prop_size);
            offset += prop_size;
            
            auto prop = KRTurboDisplayProp::CreateFromByteArray(prop_data);
            if (prop) {
                shadow->props_.push_back(prop);
            }
        }
        
        // 5. 读取 callMethods
        int methods_count = ReadInt(data, offset);
        for (int i = 0; i < methods_count; i++) {
            int method_size = ReadInt(data, offset);
            if (offset + method_size > data.size()) {
                throw std::runtime_error("Invalid method data size");
            }
            std::vector<uint8_t> method_data(data.begin() + offset, data.begin() + offset + method_size);
            offset += method_size;
            
            auto method = KRTurboDisplayNodeMethod::CreateFromByteArray(method_data);
            if (method) {
                shadow->call_methods_.push_back(method);
            }
        }
        
        return shadow;
        
    } catch (const std::exception& e) {
        KR_LOG_ERROR << "[TurboDisplay-Deserialize] Shadow deserialization failed: " << e.what();
        return nullptr;
    }
}

} // namespace KuiklyOhos

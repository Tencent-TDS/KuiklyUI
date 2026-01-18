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

#include "KRTurboDisplayProp.h"
#include "KRTurboDisplayShadow.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include <sstream>
#include <cstring>

namespace KuiklyOhos {

KRTurboDisplayProp::KRTurboDisplayProp(KRTurboDisplayPropType type,
                                       const std::string& key,
                                       const std::any& value)
    : prop_type_(type), prop_key_(key), prop_value_(value) {}

KRTurboDisplayProp::~KRTurboDisplayProp() = default;

void KRTurboDisplayProp::LazyEventIfNeed() {
    if (prop_value_.has_value()) {
        if (prop_value_.type() == typeid(KRRenderCallback) || 
            prop_value_.type() == typeid(std::shared_ptr<KRRenderCallback>)) {
            return;
        }
        return;
    }
    auto callback_ptr = std::make_shared<KRRenderCallback>([this](KRAnyValue result) {
        lazy_event_callback_results_.push_back(result ? result : NewKRRenderValue(KRRenderValueMap{}));
    });
    prop_value_ = callback_ptr;
}

void KRTurboDisplayProp::PerformLazyEventToCallback(const KRRenderCallback& callback) {
    if (!callback) {
        return;
    }
    KR_LOG_INFO << " lazy_event_callback_results_ size: " << lazy_event_callback_results_.size();
    for (const auto& result : lazy_event_callback_results_) {
        callback(result);
    }
}

std::shared_ptr<KRTurboDisplayProp> KRTurboDisplayProp::DeepCopy() const {
    std::any copied_value = prop_value_;
    if (prop_type_ == PROP_TYPE_SHADOW && prop_value_.has_value()) {
        try {
            if (prop_value_.type() == typeid(std::shared_ptr<KRTurboDisplayShadow>)) {
                auto shadow = std::any_cast<std::shared_ptr<KRTurboDisplayShadow>>(prop_value_);
                if (shadow) {
                    copied_value = shadow->DeepCopy();
                }
            }
        } catch (const std::bad_any_cast& e) {}
    }
    auto copy = std::make_shared<KRTurboDisplayProp>(prop_type_, prop_key_, copied_value);
    return copy;
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

static void WriteBool(std::vector<uint8_t>& bytes, bool value) {
    bytes.push_back(value ? 1 : 0);
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

// 序列化实现
std::vector<uint8_t> KRTurboDisplayProp::ToByteArray() const {
    std::vector<uint8_t> bytes;
    
    try {
        WriteInt(bytes, static_cast<int>(prop_type_));
        WriteString(bytes, prop_key_);
        
        bool should_serialize = false;
        if (!prop_value_.has_value()) {
            should_serialize = false;
        } else if (prop_value_.type() == typeid(KRRenderCallback) || 
                   prop_value_.type() == typeid(std::shared_ptr<KRRenderCallback>)) {
            should_serialize = false;
        } else if (prop_type_ == PROP_TYPE_SHADOW) {
            should_serialize = false;
        } else if (prop_value_.type() == typeid(int) ||
                   prop_value_.type() == typeid(float) ||
                   prop_value_.type() == typeid(double) ||
                   prop_value_.type() == typeid(std::string) ||
                   prop_value_.type() == typeid(bool)) {
            should_serialize = true;
        } else if (prop_type_ == PROP_TYPE_FRAME) {
            should_serialize = true;
        } else {
            should_serialize = false;
        }
        
        WriteBool(bytes, should_serialize);
        if (!should_serialize) {
            return bytes;
        }
        
        if (prop_value_.type() == typeid(int)) {
            WriteInt(bytes, 1);
            WriteInt(bytes, std::any_cast<int>(prop_value_));
        } else if (prop_value_.type() == typeid(float)) {
            WriteInt(bytes, 2);
            WriteFloat(bytes, std::any_cast<float>(prop_value_));
        } else if (prop_value_.type() == typeid(double)) {
            WriteInt(bytes, 3);
            WriteFloat(bytes, static_cast<float>(std::any_cast<double>(prop_value_)));
        } else if (prop_value_.type() == typeid(std::string)) {
            WriteInt(bytes, 4);
            WriteString(bytes, std::any_cast<std::string>(prop_value_));
        } else if (prop_value_.type() == typeid(bool)) {
            WriteInt(bytes, 5);
            bytes.push_back(std::any_cast<bool>(prop_value_) ? 1 : 0);
        } else if (prop_type_ == PROP_TYPE_FRAME) {
            WriteInt(bytes, 6);
            struct Frame { float x, y, width, height; };
            auto frame = std::any_cast<Frame>(prop_value_);
            WriteFloat(bytes, frame.x);
            WriteFloat(bytes, frame.y);
            WriteFloat(bytes, frame.width);
            WriteFloat(bytes, frame.height);
        }
    } catch (const std::exception& e) {
        KR_LOG_ERROR << "[TurboDisplay-Serialize] Prop serialization failed: " << e.what();
        return {};
    }
    
    return bytes;
}

// 反序列化实现
std::shared_ptr<KRTurboDisplayProp> KRTurboDisplayProp::CreateFromByteArray(const std::vector<uint8_t>& data) {
    if (data.empty()) {
        return nullptr;
    }
    
    size_t offset = 0;
    try {
        int type_int = ReadInt(data, offset);
        KRTurboDisplayPropType prop_type = static_cast<KRTurboDisplayPropType>(type_int);
        std::string prop_key = ReadString(data, offset);
        
        if (offset >= data.size()) {
            throw std::runtime_error("ReadBool: insufficient data for should_serialize flag");
        }
        bool should_deserialize = (data[offset++] != 0);
        std::any prop_value;
        
        if (!should_deserialize) {
            KR_LOG_DEBUG << "[TurboDisplay-Deserialize] Prop without value - key:" << prop_key 
                         << ", type:" << static_cast<int>(prop_type);
        } else {
            int value_type = ReadInt(data, offset);
            switch (value_type) {
                case 1:
                    prop_value = ReadInt(data, offset);
                    break;
                case 2:
                    prop_value = ReadFloat(data, offset);
                    break;
                case 3:
                    prop_value = static_cast<double>(ReadFloat(data, offset));
                    break;
                case 4:
                    prop_value = ReadString(data, offset);
                    break;
                case 5:
                    if (offset >= data.size()) {
                        throw std::runtime_error("ReadBool: insufficient data");
                    }
                    prop_value = (data[offset++] != 0);
                    break;
                case 6: {
                    struct Frame { float x, y, width, height; };
                    Frame frame;
                    frame.x = ReadFloat(data, offset);
                    frame.y = ReadFloat(data, offset);
                    frame.width = ReadFloat(data, offset);
                    frame.height = ReadFloat(data, offset);
                    prop_value = frame;
                    break;
                }
                default:
                    KR_LOG_ERROR << "[TurboDisplay-Deserialize] Unknown value type: " << value_type;
                    break;
            }
        }
        
        auto prop = std::make_shared<KRTurboDisplayProp>(prop_type, prop_key, prop_value);
        if (prop_type == PROP_TYPE_EVENT && !prop_value.has_value()) {
            prop->LazyEventIfNeed();
        }
        return prop;
    } catch (const std::exception& e) {
        KR_LOG_ERROR << "[TurboDisplay-Deserialize] Prop deserialization failed: " << e.what();
        return nullptr;
    }
}

} // namespace KuiklyOhos

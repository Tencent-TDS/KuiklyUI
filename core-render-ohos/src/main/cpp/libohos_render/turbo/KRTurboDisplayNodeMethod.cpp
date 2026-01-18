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

#include "KRTurboDisplayNodeMethod.h"
#include "libohos_render/utils/KRRenderLoger.h"

namespace KuiklyOhos {

std::shared_ptr<KRTurboDisplayNodeMethod> KRTurboDisplayNodeMethod::DeepCopy() const {
    auto method = std::make_shared<KRTurboDisplayNodeMethod>();
    method->type_ = type_;
    method->name_ = name_;
    method->method_ = method_;
    method->params_ = params_;
    return method;
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

// 序列化实现

std::vector<uint8_t> KRTurboDisplayNodeMethod::ToByteArray() const {
    std::vector<uint8_t> bytes;
    
    try {
        // 1. 写入方法类型
        WriteInt(bytes, static_cast<int>(type_));
        
        // 2. 写入名称
        WriteString(bytes, name_);
        
        // 3. 写入方法名
        WriteString(bytes, method_);
        
        // 4. 写入参数
        WriteString(bytes, params_);
        
    } catch (const std::exception& e) {
        KR_LOG_ERROR << "[TurboDisplay-Serialize] Method serialization failed: " << e.what();
        return {};
    }
    
    return bytes;
}

// 反序列化实现

std::shared_ptr<KRTurboDisplayNodeMethod> KRTurboDisplayNodeMethod::CreateFromByteArray(
    const std::vector<uint8_t>& data) {
    
    if (data.empty()) {
        return nullptr;
    }
    
    size_t offset = 0;
    
    try {
        auto method = std::make_shared<KRTurboDisplayNodeMethod>();
        
        // 1. 读取方法类型
        int type_int = ReadInt(data, offset);
        method->type_ = static_cast<KRTurboDisplayMethodType>(type_int);
        
        // 2. 读取名称
        method->name_ = ReadString(data, offset);
        
        // 3. 读取方法名
        method->method_ = ReadString(data, offset);
        
        // 4. 读取参数
        method->params_ = ReadString(data, offset);
        
        return method;
        
    } catch (const std::exception& e) {
        return nullptr;
    }
}

} // namespace KuiklyOhos

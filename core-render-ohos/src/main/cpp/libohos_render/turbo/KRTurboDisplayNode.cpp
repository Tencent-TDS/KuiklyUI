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

#include "KRTurboDisplayNode.h"
#include "KRTurboDisplayShadow.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include <algorithm>

namespace KuiklyOhos {

// 常量定义

const std::string KRTurboDisplayNode::SCROLL_INDEX = "scrollIndex";
const std::string KRTurboDisplayNode::SHADOW_KEY = "shadow";
const std::string KRTurboDisplayNode::FRAME_KEY = "frame";
const std::string KRTurboDisplayNode::INSERT_KEY = "insert";

// 构造函数 & 析构函数

KRTurboDisplayNode::KRTurboDisplayNode(int tag, const std::string& view_name)
    : tag_(tag), view_name_(view_name) {}

KRTurboDisplayNode::~KRTurboDisplayNode() = default;

// 子节点操作

void KRTurboDisplayNode::InsertSubNode(std::shared_ptr<KRTurboDisplayNode> sub_node, int index) {
    int insert_index = index;
    if (index > static_cast<int>(children_.size()) || index == -1) {
        insert_index = children_.size();
    }
    children_.insert(children_.begin() + insert_index, sub_node);
    sub_node->parent_tag_ = tag_;
    sub_node->SetProp(INSERT_KEY, index, PROP_TYPE_INSERT);
}

void KRTurboDisplayNode::RemoveFromParentNode(std::shared_ptr<KRTurboDisplayNode> parent_node) {
    auto& parent_children = parent_node->children_;
    parent_children.erase(
        std::remove(parent_children.begin(), parent_children.end(), shared_from_this()),
        parent_children.end()
    );
    parent_tag_ = std::nullopt;
}

// 属性操作

void KRTurboDisplayNode::SetProp(const std::string& prop_key, const std::any& prop_value) {
    KRTurboDisplayPropType type = PROP_TYPE_ATTR;
    if (prop_key == "setNeedLayout") {
        return;
    }
    SetProp(prop_key, prop_value, type);
}

void KRTurboDisplayNode::SetProp(const std::string& prop_key, const std::any& prop_value, 
                                 KRTurboDisplayPropType prop_type) {
    auto prop = GetProp(prop_key);
    if (prop == nullptr) {
        props_.push_back(std::make_shared<KRTurboDisplayProp>(prop_type, prop_key, prop_value));
    } else {
        prop->SetPropValue(prop_value);
    }
}

void KRTurboDisplayNode::SetFrame(float x, float y, float width, float height) {
    struct Frame { float x, y, width, height; };
    Frame frame{x, y, width, height};
    SetProp(FRAME_KEY, frame, PROP_TYPE_FRAME);
}

void KRTurboDisplayNode::SetShadow(std::shared_ptr<KRTurboDisplayShadow> shadow) {
    SetProp(SHADOW_KEY, shadow, PROP_TYPE_SHADOW);
}

std::shared_ptr<KRTurboDisplayProp> KRTurboDisplayNode::GetProp(const std::string& key) {
    for (auto& prop : props_) {
        if (prop->GetPropKey() == key) {
            return prop;
        }
    }
    return nullptr;
}

// 计算属性

int KRTurboDisplayNode::GetScrollIndex() const {
    for (const auto& prop : props_) {
        if (prop->GetPropKey() == SCROLL_INDEX) {
            try {
                const auto& value = prop->GetPropValue();
                if (!value.has_value()) {
                    return 0;
                }
                if (value.type() == typeid(int)) {
                    return std::any_cast<int>(value);
                } else if (value.type() == typeid(float)) {
                    return static_cast<int>(std::any_cast<float>(value));
                } else if (value.type() == typeid(double)) {
                    return static_cast<int>(std::any_cast<double>(value));
                }
            } catch (const std::bad_any_cast& e) {
                KR_LOG_ERROR << "[TurboDisplay] Failed to cast scrollIndex: " << e.what();
            }
        }
    }
    return 0;
}

KRTurboDisplayNode::RenderFrame KRTurboDisplayNode::GetRenderFrame() const {
    for (const auto& prop : props_) {
        if (prop->GetPropType() == PROP_TYPE_FRAME) {
            try {
                const auto& value = prop->GetPropValue();
                if (!value.has_value()) {
                    return RenderFrame();
                }
                
                struct Frame { float x, y, width, height; };
                if (value.type() == typeid(Frame)) {
                    auto frame = std::any_cast<Frame>(value);
                    return RenderFrame(frame.x, frame.y, frame.width, frame.height);
                }
            } catch (const std::bad_any_cast& e) {
                KR_LOG_ERROR << "[TurboDisplay] Failed to cast renderFrame: " << e.what();
            }
        }
    }
    return RenderFrame();
}

// 方法操作

void KRTurboDisplayNode::AddViewMethod(const std::string& method, const std::string& params, const KRRenderCallback& callback) {
    AddNodeMethod(METHOD_TYPE_VIEW, "", method, params, callback);
}

void KRTurboDisplayNode::AddModuleMethod(const std::string& module_name, 
                                        const std::string& method, 
                                        const std::string& params,
                                        const KRRenderCallback& callback) {
    AddNodeMethod(METHOD_TYPE_MODULE, module_name, method, params, callback);
}

void KRTurboDisplayNode::AddNodeMethod(KRTurboDisplayMethodType type, 
                                      const std::string& name,
                                      const std::string& method, 
                                      const std::string& params,
                                      const KRRenderCallback& callback) {
    auto node_method = std::make_shared<KRTurboDisplayNodeMethod>();
    node_method->SetType(type);
    node_method->SetName(name);
    node_method->SetMethod(method);
    node_method->SetParams(params);
    node_method->SetCallback(callback);
    call_methods_.push_back(node_method);
}

// 深拷贝

std::shared_ptr<KRTurboDisplayNode> KRTurboDisplayNode::DeepCopy() const {
    auto node = std::make_shared<KRTurboDisplayNode>(tag_, view_name_);
    node->parent_tag_ = parent_tag_;

    for (const auto& child : children_) {
        node->children_.push_back(child->DeepCopy());
    }

    for (const auto& prop : props_) {
        node->props_.push_back(prop->DeepCopy());
    }

    for (const auto& method : call_methods_) {
        node->call_methods_.push_back(method->DeepCopy());
    }

    return node;
}

// 序列化辅助函数 - 写入

void KRTurboDisplayNode::WriteInt(std::vector<uint8_t>& bytes, int value) {
    bytes.push_back((value >> 24) & 0xFF);
    bytes.push_back((value >> 16) & 0xFF);
    bytes.push_back((value >> 8) & 0xFF);
    bytes.push_back(value & 0xFF);
}

void KRTurboDisplayNode::WriteString(std::vector<uint8_t>& bytes, const std::string& str) {
    WriteInt(bytes, static_cast<int>(str.size()));
    bytes.insert(bytes.end(), str.begin(), str.end());
}

void KRTurboDisplayNode::WriteBool(std::vector<uint8_t>& bytes, bool value) {
    bytes.push_back(value ? 1 : 0);
}

void KRTurboDisplayNode::WriteByteArray(std::vector<uint8_t>& bytes, const std::vector<uint8_t>& data) {
    WriteInt(bytes, static_cast<int>(data.size()));
    bytes.insert(bytes.end(), data.begin(), data.end());
}

// 序列化辅助函数 - 读取

int KRTurboDisplayNode::ReadInt(const std::vector<uint8_t>& data, size_t& offset) {
    if (offset + 4 > data.size()) {
        throw std::runtime_error("[TurboDisplay-Serialize] ReadInt: insufficient data");
    }
    int value = (static_cast<int>(data[offset]) << 24) |
                (static_cast<int>(data[offset + 1]) << 16) |
                (static_cast<int>(data[offset + 2]) << 8) |
                static_cast<int>(data[offset + 3]);
    offset += 4;
    return value;
}

std::string KRTurboDisplayNode::ReadString(const std::vector<uint8_t>& data, size_t& offset) {
    int length = ReadInt(data, offset);
    if (length < 0 || offset + length > data.size()) {
        throw std::runtime_error("[TurboDisplay-Serialize] ReadString: invalid length or insufficient data");
    }
    std::string str(data.begin() + offset, data.begin() + offset + length);
    offset += length;
    return str;
}

bool KRTurboDisplayNode::ReadBool(const std::vector<uint8_t>& data, size_t& offset) {
    if (offset >= data.size()) {
        throw std::runtime_error("[TurboDisplay-Serialize] ReadBool: insufficient data");
    }
    return data[offset++] != 0;
}

std::vector<uint8_t> KRTurboDisplayNode::ReadByteArray(const std::vector<uint8_t>& data, size_t& offset) {
    int length = ReadInt(data, offset);
    if (length < 0 || offset + length > data.size()) {
        throw std::runtime_error("[TurboDisplay-Serialize] ReadByteArray: invalid length or insufficient data");
    }
    std::vector<uint8_t> bytes(data.begin() + offset, data.begin() + offset + length);
    offset += length;
    return bytes;
}

// 序列化实现

std::vector<uint8_t> KRTurboDisplayNode::ToByteArray() const {
    std::vector<uint8_t> bytes;
    
    try {
        // 1. tag
        WriteInt(bytes, tag_);
        
        // 2. viewName
        WriteString(bytes, view_name_);
        
        // 3. parentTag
        WriteBool(bytes, parent_tag_.has_value());
        if (parent_tag_.has_value()) {
            WriteInt(bytes, parent_tag_.value());
        }
        
        // 4. props
        if (props_.empty()) {
            WriteInt(bytes, 0);
        } else {
            WriteInt(bytes, 1);
            WriteInt(bytes, static_cast<int>(props_.size()));
            for (const auto& prop : props_) {
                auto prop_bytes = prop->ToByteArray();
                WriteByteArray(bytes, prop_bytes);
            }
        }
        
        // 5. callMethods
        if (call_methods_.empty()) {
            WriteInt(bytes, 0);
        } else {
            WriteInt(bytes, 1);
            WriteInt(bytes, static_cast<int>(call_methods_.size()));
            for (const auto& method : call_methods_) {
                auto method_bytes = method->ToByteArray();
                WriteByteArray(bytes, method_bytes);
            }
        }
        
        // 6. children
        if (children_.empty()) {
            WriteInt(bytes, 0);
        } else {
            WriteInt(bytes, 1);
            WriteInt(bytes, static_cast<int>(children_.size()));
            for (const auto& child : children_) {
                auto child_bytes = child->ToByteArray();
                WriteByteArray(bytes, child_bytes);
            }
        }
        
    } catch (const std::exception& e) {
        KR_LOG_ERROR << "[TurboDisplay-Serialize] Serialization failed: " << e.what();
        return {};
    }
    
    return bytes;
}

// 反序列化实现

std::shared_ptr<KRTurboDisplayNode> KRTurboDisplayNode::CreateFromByteArray(const std::vector<uint8_t>& data) {
    if (data.empty()) {
        return nullptr;
    }
    
    size_t offset = 0;
    
    try {
        // 1. tag
        int tag = ReadInt(data, offset);
        
        // 2. viewName
        std::string view_name = ReadString(data, offset);
        auto node = std::make_shared<KRTurboDisplayNode>(tag, view_name);
        
        // 3. parentTag
        bool has_parent = ReadBool(data, offset);
        if (has_parent) {
            node->parent_tag_ = ReadInt(data, offset);
        }
        
        // 4. props
        int has_props = ReadInt(data, offset);
        if (has_props == 1) {
            int props_count = ReadInt(data, offset);
            for (int i = 0; i < props_count; i++) {
                auto prop_bytes = ReadByteArray(data, offset);
                auto prop = KRTurboDisplayProp::CreateFromByteArray(prop_bytes);
                if (prop) {
                    node->props_.push_back(prop);
                }
            }
        }
        
        // 5. callMethods
        int has_methods = ReadInt(data, offset);
        if (has_methods == 1) {
            int methods_count = ReadInt(data, offset);
            for (int i = 0; i < methods_count; i++) {
                auto method_bytes = ReadByteArray(data, offset);
                auto method = KRTurboDisplayNodeMethod::CreateFromByteArray(method_bytes);
                if (method) {
                    node->call_methods_.push_back(method);
                }
            }
        }
        
        // 6. children
        int has_children = ReadInt(data, offset);
        if (has_children == 1) {
            int children_count = ReadInt(data, offset);
            for (int i = 0; i < children_count; i++) {
                auto child_bytes = ReadByteArray(data, offset);
                auto child = CreateFromByteArray(child_bytes);
                if (child) {
                    node->children_.push_back(child);
                }
            }
        }
        
        node->add_view_method_disable_ = false;
        
        return node;
        
    } catch (const std::exception& e) {
        KR_LOG_ERROR << "[TurboDisplay-Deserialize] Deserialization failed at offset " 
                     << offset << ": " << e.what();
        return nullptr;
    }
}

} // namespace KuiklyOhos

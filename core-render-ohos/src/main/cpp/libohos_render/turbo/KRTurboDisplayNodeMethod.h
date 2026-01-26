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

#ifndef KUIKLY_TURBO_DISPLAY_NODE_METHOD_H
#define KUIKLY_TURBO_DISPLAY_NODE_METHOD_H

#include <string>
#include <memory>
#include <functional>
#include "../foundation/KRCommon.h"  // 引入 KRRenderCallback 定义

namespace KuiklyOhos {

// Method类型
enum KRTurboDisplayMethodType {
    METHOD_TYPE_VIEW = 0,
    METHOD_TYPE_MODULE = 1
};

// TurboDisplay节点方法类
class KRTurboDisplayNodeMethod {
public:
    KRTurboDisplayNodeMethod() = default;
    ~KRTurboDisplayNodeMethod() = default;

    // Getter/Setter
    KRTurboDisplayMethodType GetType() const { return type_; }
    void SetType(KRTurboDisplayMethodType type) { type_ = type; }

    std::string GetName() const { return name_; }
    void SetName(const std::string& name) { name_ = name; }

    std::string GetMethod() const { return method_; }
    void SetMethod(const std::string& method) { method_ = method; }

    std::string GetParams() const { return params_; }
    void SetParams(const std::string& params) { params_ = params; }
    
    // callback属性
    KRRenderCallback GetCallback() const { return callback_; }
    void SetCallback(const KRRenderCallback& callback) { callback_ = callback; }

    // 深拷贝
    std::shared_ptr<KRTurboDisplayNodeMethod> DeepCopy() const;

    // 序列化/反序列化
    std::vector<uint8_t> ToByteArray() const;
    static std::shared_ptr<KRTurboDisplayNodeMethod> CreateFromByteArray(const std::vector<uint8_t>& data);

private:
    KRTurboDisplayMethodType type_ = METHOD_TYPE_VIEW;
    std::string name_;
    std::string method_;
    std::string params_;
    KRRenderCallback callback_;  // callback字段（内存字段，不参与序列化）
};

} // namespace KuiklyOhos

#endif // KUIKLY_TURBO_DISPLAY_NODE_METHOD_H

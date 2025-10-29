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

#ifndef CORE_RENDER_OHOS_KRVIEWCONTEXT_H
#define CORE_RENDER_OHOS_KRVIEWCONTEXT_H
#include <string>

struct KRViewContext final{
private:
    union Boxing{
        constexpr Boxing():ctx(nullptr){}
        struct {
            int instance;
            int tag;
        };
        void* ctx;
    };
public:
    KRViewContext(const std::string& instance_id, int tag){
        box_.instance = std::stoi(instance_id);
        box_.tag = tag;
    }
    constexpr KRViewContext(const void *context){
        box_.ctx = (void *)context;
    }
    
    std::string InstanceString(){
        return std::to_string(box_.instance);
    }
    int Instance(){
        return box_.instance;
    }
    int Tag(){
        return box_.tag;
    }
    void *Context(){
        return box_.ctx;
    }
    bool operator == (const KRViewContext& other){
        return box_.ctx == other.box_.ctx;
    }
private:
    Boxing box_;
};

static_assert(sizeof(KRViewContext) == sizeof(void*), "please leave union boxing alone");
#endif // CORE_RENDER_OHOS_KRVIEWCONTEXT_H

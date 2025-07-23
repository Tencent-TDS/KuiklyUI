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

#ifndef CORE_RENDER_OHOS_KR_WEAK_OBJECT_MANAGER_H
#define CORE_RENDER_OHOS_KR_WEAK_OBJECT_MANAGER_H

#include <memory>
#include <map>
#include "libohos_render/utils/KRScopedSpinLock.h"

template<typename T>
class KRWeakRegistry{
public:
    void Set(void* key, std::weak_ptr<T> value){
        KRScopedSpinLock lock(&spin_);
        entries_[key] = value;
    }
    void Remove(void* key){
        KRScopedSpinLock lock(&spin_);
        entries_.erase(key);
    }

    std::weak_ptr<T> Get(void* key){
        KRScopedSpinLock lock(&spin_);
        if (auto it = entries_.find(key); it != entries_.end()) {
            return it->second;
        }
        return std::weak_ptr<T>();
    }

private:
    KRSpinLock spin_;
    std::map<void*, std::weak_ptr<T>> entries_;
};

template<typename T>
class KRWeakObjectMgr : public KRWeakRegistry<T>{
public:
    static KRWeakObjectMgr<T> &GetInstance(){
        static KRWeakObjectMgr<T> instance_;
        return instance_;;
    }
};

template<typename T>
void *KRWeakObjectManagerRegisterWeakObject(std::shared_ptr<T> ptr){
    if(ptr){
        void* key = ptr.get();
        KRWeakObjectMgr<T>::GetInstance().Set(key, ptr);
        return key;
    }
    return nullptr;
}

template<typename T>
void KRWeakObjectManagerUnregisterWeakObject(std::shared_ptr<T> ptr){
    if(ptr){
        KRWeakObjectMgr<T>::GetInstance().Remove(ptr.get());
    }
}

template<typename T>
std::weak_ptr<T> KRWeakObjectManagerGetWeakObject(void *key){
    if(key){
        return KRWeakObjectMgr<T>::GetInstance().Get(key);
    }
    return std::weak_ptr<T>();
}

#endif //CORE_RENDER_OHOS_KR_WEAK_OBJECT_MANAGER_H

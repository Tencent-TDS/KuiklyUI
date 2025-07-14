/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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


#include <map>
#include <string>
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/expand/events/gesture/KRGestureEventHandler.h"

#include "KRWeakObjectManager.h"

template<typename T>
class KRWeakObjectManager{
public:
    void Set(void* key, std::weak_ptr<T> value){
        entries_[key] = value;
    }
    void Remove(void* key){
        entries_.erase(key);
    }
    
    std::weak_ptr<T> Get(void* key){
        if (auto it = entries_.find(key); it != entries_.end()) {
            return it->second;
        }
        return std::weak_ptr<T>();
    }
    
private:
    std::map<void*, std::weak_ptr<T>> entries_;
};

class KRWeakObjectManagerRegistry {
public:
    static KRWeakObjectManagerRegistry& GetInstance(){
        static KRWeakObjectManagerRegistry instance_;
        return instance_;
    }
    
private:
    KRWeakObjectManagerRegistry() = default;
    ~KRWeakObjectManagerRegistry() = default;
    
public:
    KRWeakObjectManager<KRGestureEventHandler> gesture_event_handler_object_manager_; 
    KRWeakObjectManager<IKRRenderViewExport> render_view_object_manager_; 
};

template<>
void *KRWeakObjectManagerRegisterWeakObject(std::shared_ptr<KRGestureEventHandler> ptr){
    if(ptr){
        void* key = ptr.get();
        KRWeakObjectManagerRegistry::GetInstance().gesture_event_handler_object_manager_.Set(key,ptr);
        return key;
    }
    return nullptr;
}

template<>
std::weak_ptr<KRGestureEventHandler> KRWeakObjectManagerGetWeakObject(void *key){
    if(key){
        return KRWeakObjectManagerRegistry::GetInstance().gesture_event_handler_object_manager_.Get(key);
    }
    return std::weak_ptr<KRGestureEventHandler>();
}

template<>
void *KRWeakObjectManagerRegisterWeakObject(std::shared_ptr<IKRRenderViewExport> ptr){
    if(ptr){
        void* key = ptr.get();
        KRWeakObjectManagerRegistry::GetInstance().render_view_object_manager_.Set(key,ptr);
        return key;
    }
    return nullptr;
}

template<>
std::weak_ptr<IKRRenderViewExport> KRWeakObjectManagerGetWeakObject(void *key){
    if(key){
        return KRWeakObjectManagerRegistry::GetInstance().render_view_object_manager_.Get(key);
    }
    return std::weak_ptr<IKRRenderViewExport>();
}

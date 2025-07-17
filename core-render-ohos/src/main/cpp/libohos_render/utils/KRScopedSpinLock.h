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

#ifndef CORE_RENDER_OHOS_KRSCOPEDSPINLOCK_H
#define CORE_RENDER_OHOS_KRSCOPEDSPINLOCK_H

#include <pthread.h>

class KRScopedSpinLock;
class KRSpinLock{
public:
    KRSpinLock(){
        pthread_spin_init(&spin_, PTHREAD_PROCESS_PRIVATE);
    }
    ~KRSpinLock(){
        pthread_spin_destroy(&spin_);
    }
private:
    friend class KRScopedSpinLock;
    pthread_spinlock_t spin_;
};

class KRScopedSpinLock{
public:
    KRScopedSpinLock(KRSpinLock *spinlock): spinlock_(spinlock), status_(-1){
        if(spinlock){
            status_ = pthread_spin_lock(&spinlock_->spin_);
        }
    }
    KRScopedSpinLock(const KRScopedSpinLock &) = delete;
    KRScopedSpinLock &operator=(const KRScopedSpinLock &) = delete;
    
    ~KRScopedSpinLock(){
        if(spinlock_ && status_ == 0){
            status_ = pthread_spin_unlock(&spinlock_->spin_);
        }
    }
    
private:
    KRSpinLock* spinlock_;
    int status_;
};
#endif //CORE_RENDER_OHOS_KRSCOPEDSPINLOCK_H

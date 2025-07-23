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

#import "KuiklyRenderThreadLock.h"
#import <os/lock.h>
API_AVAILABLE(ios(10.0))
@interface KuiklyRenderThreadLock()

@property (nonatomic, assign) os_unfair_lock unfairLock;


@end

@implementation KuiklyRenderThreadLock {
    dispatch_semaphore_t _semaphore;
}

- (instancetype)init {
    if (self = [super init]) {
        if (@available(iOS 10.0, *)) {
            _unfairLock = OS_UNFAIR_LOCK_INIT;
        } else {
            _semaphore = dispatch_semaphore_create(1);
        }
    }
    return self;
}

- (void)threadSafeInBlock:(dispatch_block_t)block {
#pragma clang diagnostic push
#pragma clang diagnostic ignored"-Wunguarded-availability"
    if (@available(iOS 10.0, *)) {
        os_unfair_lock_lock(&_unfairLock);
    } else {
        dispatch_semaphore_wait(_semaphore, DISPATCH_TIME_FOREVER);
    }
        
    if (block) {
        block();
    }
    
    if (@available(iOS 10.0, *)) {
        os_unfair_lock_unlock(&_unfairLock);
    } else {
        dispatch_semaphore_signal(_semaphore);
    }
#pragma clang diagnostic pop
}


@end

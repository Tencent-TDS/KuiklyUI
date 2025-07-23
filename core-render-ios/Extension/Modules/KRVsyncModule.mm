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

#import "KRVsyncModule.h"
#import "KuiklyRenderThreadManager.h"

@implementation KRVsyncModule
{
    KuiklyRenderCallback _tipCb;
    dispatch_source_t _kotlinTimer;
}

- (void)registerVsync:(NSDictionary *)args {
    _tipCb = args[KR_CALLBACK_KEY];
    
    dispatch_queue_t contextQueue = [KuiklyRenderThreadManager contextQueue];
    
    if (!contextQueue) {
        return ;
    }
    [self invalidateTimer];
    _kotlinTimer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, contextQueue);
    dispatch_source_set_timer(_kotlinTimer, DISPATCH_TIME_NOW, NSEC_PER_SEC / 60.0, NSEC_PER_MSEC);
    __weak typeof(self) wself = self;
    dispatch_source_set_event_handler(_kotlinTimer, ^{
        __strong typeof(self) sself = wself;
        [sself vsyncFire];
    });
    dispatch_resume(_kotlinTimer);

}

- (void)vsyncFire {
    if (_tipCb) {
        _tipCb(@{});
    }
}

- (void)invalidateTimer {
    if (_kotlinTimer) {
        dispatch_source_cancel(_kotlinTimer);
        _kotlinTimer = nil;
    }
}

- (void)unRegisterVsync:(NSDictionary *)args {
    [self invalidateTimer];
}

@end

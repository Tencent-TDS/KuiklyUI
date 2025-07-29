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

#import "KRNotifyModule.h"
#import "NSObject+KR.h"
#define PARAM_KEY @"param"
#define CALLBACK_KEY @"callback"
#define EVENT_NAME @"eventName"
#define CALLBACK_ID @"id"
#define DATA @"data"


@interface KRNotifyCallbackObject : NSObject

@property (nonatomic, strong) KuiklyRenderCallback callback;
@property (nonatomic, strong) NSString * callback_id;

@end

@implementation KRNotifyCallbackObject
@end

@interface KRNotifyModule()

@property (nonatomic, strong) NSMutableDictionary<NSString *, NSMutableArray<KRNotifyCallbackObject *> *> *eventCallbackMap;

@end

@implementation KRNotifyModule



- (void)addNotify:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString * eventName = param[EVENT_NAME];
    if (!eventName) {
        return;
    }
    NSString * callbackId = param[CALLBACK_ID];
    if (!callback) {
        return ;
    }
    
    KRNotifyCallbackObject *callbackObject = [[KRNotifyCallbackObject alloc] init];
    callbackObject.callback = callback;
    callbackObject.callback_id = callbackId;
    
    NSMutableArray<KRNotifyCallbackObject *> * queue = [self callbackQueueWithEventName:eventName];
    BOOL isEmpty = queue.count == 0;
    [queue addObject:callbackObject];
    if (isEmpty) {
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onReceiveNotication:) name:eventName object:nil];
    }
}

#pragma mark - notification

- (void)onReceiveNotication:(NSNotification *)notification {
    dispatch_block_t block = ^{
        NSString *eventName = notification.name;
        NSDictionary *userInfo = notification.userInfo;
        NSMutableArray<KRNotifyCallbackObject *> * queue = [self callbackQueueWithEventName:eventName];
        [queue enumerateObjectsUsingBlock:^(KRNotifyCallbackObject * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            obj.callback(userInfo);
        }];
    };
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_async(dispatch_get_main_queue(), block);
    }
   
}

- (void)removeNotify:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString * eventName = param[EVENT_NAME];
    NSString * callbackId = param[CALLBACK_ID];
    NSMutableArray<KRNotifyCallbackObject *> * queue = [self callbackQueueWithEventName:eventName];
    [queue.copy enumerateObjectsUsingBlock:^(KRNotifyCallbackObject *  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if (obj.callback_id == callbackId) {
            [queue removeObject:obj];
        }
    }];
    
    if (queue.count == 0) {
        [[NSNotificationCenter defaultCenter] removeObserver:self name:eventName object:nil];
    }
}

- (void)postNotify:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString * eventName = param[EVENT_NAME];
    NSDictionary * data = param[DATA];
    [[NSNotificationCenter defaultCenter] postNotificationName:eventName object:nil userInfo:data];
}


#pragma mark - getter

- (NSMutableDictionary<NSString *, NSMutableArray<KRNotifyCallbackObject *> *> *)eventCallbackMap {
    if (!_eventCallbackMap) {
        _eventCallbackMap = [[NSMutableDictionary alloc] init];
    }
    return _eventCallbackMap;
}

- (NSMutableArray<KRNotifyCallbackObject *> *)callbackQueueWithEventName:(NSString *)eventName {
    NSMutableArray<KRNotifyCallbackObject *> * queue = self.eventCallbackMap[eventName];
    if (!queue) {
        queue = [[NSMutableArray alloc] init];
        self.eventCallbackMap[eventName] = queue;
    }
    return queue;
}

#pragma mark - dealloc

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end

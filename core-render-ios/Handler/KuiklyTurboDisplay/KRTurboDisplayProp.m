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

#import "KRTurboDisplayProp.h"
#import "KRComponentDefine.h"
#import "KRTurboDisplayShadow.h"

#define PROP_TYPE @"propType"
#define PROP_KEY @"propKey"
#define PROP_VALUE @"propValue"

@interface KRTurboDisplayProp()
@end

@implementation KRTurboDisplayProp

- (instancetype)initWithType:(KRTurboDisplayPropType)type propKey:(NSString *)propKey propValue:(id)propValue {
    if (self = [super init]) {
        _propType = type;
        _propKey = propKey;
        _propValue = propValue;
    }
    return self;
}


- (void)lazyEventIfNeed {
    if (_propValue) {
        return ;
    }
    KR_WEAK_SELF;
    _propValue = ^( id result ) {
        [weakSelf.lazyEventCallbackResults addObject:result ?: @{}]; // 必须回调一次，添加记录一次
    };
}

- (void)performLazyEventToCallback:(KuiklyRenderCallback)callback {
    if (!callback) {
        return ;
    }
    for (id res in self.lazyEventCallbackResults) {
        callback(res);
    }
}


#pragma mark - NSCoding

- (instancetype)initWithCoder:(NSCoder *)coder {
    KRTurboDisplayPropType type = [coder decodeIntegerForKey:PROP_TYPE];
    NSString *propKey = [coder decodeObjectForKey:PROP_KEY];
    id propValue = [coder decodeObjectForKey:PROP_VALUE];
    self = [self initWithType:type propKey:propKey propValue:propValue];
    return self;
}


- (void)encodeWithCoder:(NSCoder *)coder {
    [coder encodeInteger:self.propType forKey:PROP_TYPE];
    [coder encodeObject:self.propKey forKey:PROP_KEY];
    if ([self.propValue conformsToProtocol:@protocol(NSCoding)]) {
        [coder encodeObject:self.propValue forKey:PROP_VALUE];
    }
    
}

#pragma mark - copy

- (KRTurboDisplayProp *)deepCopy {
    
    id value = self.propValue;
    if (self.propType == KRTurboDisplayPropTypeShadow) {
        value = [((KRTurboDisplayShadow *)self.propValue) deepCopy];
    }
    
    KRTurboDisplayProp* newProp = [[KRTurboDisplayProp alloc] initWithType:self.propType propKey:self.propKey propValue:value];
    return newProp;
}

#pragma mark - getter

- (NSMutableArray<id> *)lazyEventCallbackResults {
    if (!_lazyEventCallbackResults) {
        _lazyEventCallbackResults = [NSMutableArray new];
    }
    return _lazyEventCallbackResults;
}


# pragma mark - event replay

+ (KREventReplayPolicy)replayPolicyForEventKey:(NSString *)eventKey {
    static NSSet *lastReplayEvents = nil;       // 仅回放最后一次的事件
    
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        lastReplayEvents = [NSSet setWithArray:@[
            @"scroll", @"dragBegin", @"dragEnd", @"willDragEnd", @"scrollEnd", @"scrollToTop", @"textDidChange"
        ]];
    });
    
    if ([lastReplayEvents containsObject:eventKey]) {
        return KREventReplayPolicyLast;
    }
    return KREventReplayPolicyAll;
}

- (void)performLazyEventToCallback:(KuiklyRenderCallback)callback withPolicy:(KREventReplayPolicy)policy {
    if (!callback || self.lazyEventCallbackResults.count == 0) {
        return;
    }
    
    switch (policy) {
        case KREventReplayPolicyAll:
            // 全量回放
            for (NSUInteger i = 0; i < self.lazyEventCallbackResults.count; i++) {
                id res = self.lazyEventCallbackResults[i];
                callback(res);
            }
            break;
        case KREventReplayPolicyLast:
            // 仅回放最后一次
            callback(self.lazyEventCallbackResults.lastObject);
            break;
            
        default:
            // 全量回放
            for (NSUInteger i = 0; i < self.lazyEventCallbackResults.count; i++) {
                id res = self.lazyEventCallbackResults[i];
                callback(res);
            }
            break;
    }
}



@end

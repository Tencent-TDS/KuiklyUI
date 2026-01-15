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

#import <Foundation/Foundation.h>
#import "KRComponentDefine.h"

NS_ASSUME_NONNULL_BEGIN

typedef enum : NSUInteger {
    KRTurboDisplayPropTypeAttr,
    KRTurboDisplayPropTypeEvent,
    KRTurboDisplayPropTypeFrame,
    KRTurboDisplayPropTypeShadow,
    KRTurboDisplayPropTypeInsert
} KRTurboDisplayPropType;

/// 事件回放策略
typedef enum : NSUInteger {
    KREventReplayPolicyAll,     // 全量回放（click、touch、pan、longPress、doubleClick）
    KREventReplayPolicyLast,    // 仅回放最后一次（scroll、dragBegin、dragEnd、scrollEnd）
} KREventReplayPolicy;

@interface KRTurboDisplayProp : NSObject<NSCoding>

@property (nonatomic, strong ) NSString *propKey;
@property (nonatomic, strong) id propValue;
@property (nonatomic, assign ) KRTurboDisplayPropType propType;
@property (nonatomic, strong) NSMutableArray<id> *lazyEventCallbackResults;

- (instancetype)initWithType:(KRTurboDisplayPropType)type propKey:(NSString *)propKey propValue:(id)propValue;

- (void)lazyEventIfNeed;

- (void)performLazyEventToCallback:(KuiklyRenderCallback)callback;

- (KRTurboDisplayProp *)deepCopy;

/// 按策略回放事件到 callback
- (void)performLazyEventToCallback:(KuiklyRenderCallback)callback withPolicy:(KREventReplayPolicy)policy;

/// 获取事件回放策略（根据 propKey 判断）
+ (KREventReplayPolicy)replayPolicyForEventKey:(NSString *)eventKey;

@end

NS_ASSUME_NONNULL_END

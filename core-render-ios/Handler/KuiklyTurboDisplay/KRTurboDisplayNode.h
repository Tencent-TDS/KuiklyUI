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
#import "KRTurboDisplayNodeMethod.h"
#import "KRTurboDisplayProp.h"

#define FRAME_KEY  @"frame"
#define SHADOW_KEY  @"shadow"
#define INSERT_KEY  @"insert"

NS_ASSUME_NONNULL_BEGIN

@protocol KuiklyRenderShadowProtocol;
@class KRTurboDisplayShadow;
@interface KRTurboDisplayNode : NSObject<NSCoding>

@property (nonatomic, strong) NSNumber *tag;
@property (nonatomic, strong) NSString *viewName;
@property (nonatomic, strong, nullable) NSNumber *parentTag;
@property (nonatomic, strong) NSMutableArray<KRTurboDisplayNode *> *children;
@property (nonatomic, strong, readonly) NSMutableArray<KRTurboDisplayProp *> *props;
@property (nonatomic, strong, readonly) NSMutableArray<KRTurboDisplayNodeMethod *> *callMethods;
@property (nonatomic, strong) id renderShadow; // 真实shadow，内存字段，不参与序列化
@property (nonatomic, strong) NSNumber* scrollIndex; // computed property
@property (nonatomic, assign) CGRect renderFrame; // computed property
@property (nonatomic, assign) BOOL addViewMethodDisable;


- (instancetype)initWithTag:(NSNumber *)tag viewName:(NSString *)viewName;

- (void)insertSubNode:(KRTurboDisplayNode *)subNode index:(NSInteger)index;

- (void)removeFromParentNode:(KRTurboDisplayNode *)parentNode;

- (void)addViewMethodWithMethod:(NSString *)method
                         params:(NSString * _Nullable)params
                       callback:(KuiklyRenderCallback _Nullable)callback;

- (void)addModuleMethodWithModuleName:(NSString *)moduelName
                               method:(NSString *)method
                               params:(NSString * _Nullable)params
                             callback:(KuiklyRenderCallback _Nullable)callback;

- (void)setPropWithKey:(NSString *)propKey propValue:(id)propValue;

- (void)setFrame:(CGRect)frame;

- (void)setShadow:(KRTurboDisplayShadow *)shadow;

- (void)setPropWithKey:(NSString *)propKey propValue:(id)propValue propType:(KRTurboDisplayPropType)type;

- (KRTurboDisplayProp *)propWithKey:(NSString *)key;

- (BOOL)hasChild;

- (KRTurboDisplayNode *)deepCopy;

@end

NS_ASSUME_NONNULL_END

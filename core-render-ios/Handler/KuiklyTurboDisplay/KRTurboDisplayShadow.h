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

NS_ASSUME_NONNULL_BEGIN
@class KRTurboDisplayProp;
@class KRTurboDisplayNodeMethod;
@interface KRTurboDisplayShadow : NSObject<NSCoding>

@property (nonatomic, strong, readonly) NSNumber *tag;
@property (nonatomic, copy, readonly) NSString *viewName;
@property (nonatomic, strong, readonly) NSMutableArray<KRTurboDisplayProp *> *props;
@property (nonatomic, strong, readonly) NSValue *constraintSize;
@property (nonatomic, strong, readonly) NSMutableArray<KRTurboDisplayNodeMethod *> *callMethods;

- (instancetype)initWithTag:(NSNumber *)tag viewName:(NSString *)viewName;

- (void)setPropWithKey:(NSString *)propKey propValue:(id)propValue;

- (void)calculateWithConstraintSize:(CGSize)constraintSize;

- (void)addMethodWithName:(NSString *)name params:(NSString *)params;

- (KRTurboDisplayShadow *)deepCopy;

@end

NS_ASSUME_NONNULL_END

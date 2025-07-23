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
#import "TDFModuleProtocol.h"

NS_ASSUME_NONNULL_BEGIN

@interface TDFMethodArgument : NSObject

@property (nonatomic, copy, readonly) NSString *type;
@property (nonatomic, readonly) TDFNullability nullability;
@property (nonatomic, readonly) BOOL unused;

- (instancetype)initWithType:(NSString *)type nullability:(TDFNullability)nullability unused:(BOOL)unused;

@end

@interface TDFNativeMethod : NSObject

@property (nonatomic, readonly) Class moduleClass;
@property (nonatomic, readonly) SEL selector;
@property (nonatomic, readonly) const char *wrapMethodName;
@property (nonatomic, readonly, weak) id<TDFBridgeDelegate> _Nullable delegate;

- (instancetype)initWithMethod:(const TDFMethodInfo *)exportMethod
                   moduleClass:(Class)moduleClass
                      delegate:(id<TDFBridgeDelegate>)delegate;

- (id)invokeWithModule:(id)module arguments:(NSArray *)arguments;

@end

NS_ASSUME_NONNULL_END

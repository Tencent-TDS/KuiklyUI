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

@class UIView;
@class TDFNativeMethod;

@interface TDFBaseModule : NSObject <TDFModuleProtocol>

/*
 * @brief bridgeDelegate
 */
@property (nonatomic, strong) id<TDFBridgeDelegate> _Nullable delegate;

/*
 * @brief 方法列表
 */
@property (nonatomic, copy, readonly) NSArray<TDFNativeMethod *> *methods;

/*
 * @brief 方法dict key是导出的方法名，value是method
 */
@property (nonatomic, copy, readonly) NSDictionary<NSString *, TDFNativeMethod *> *methodsByName;

@end

NS_ASSUME_NONNULL_END

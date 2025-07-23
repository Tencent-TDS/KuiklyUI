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
#import "KuiklyRenderLayerProtocol.h"
NS_ASSUME_NONNULL_BEGIN
@class KuiklyRenderUIScheduler;
@class KuiklyRenderCore;

/**
 * @brief TurboDisply直出渲染模式实现器
 */
@interface KuiklyTurboDisplayRenderLayerHandler : NSObject<KuiklyRenderLayerProtocol>
/** ui调度器 */
@property (nonatomic, weak) KuiklyRenderUIScheduler *uiScheduler;

- (instancetype)initWithRootView:(UIView *)rootView contextParam:(KuiklyContextParam *)contextParam turboDisplayKey:(NSString *)turboDisplayKey;


@end

NS_ASSUME_NONNULL_END

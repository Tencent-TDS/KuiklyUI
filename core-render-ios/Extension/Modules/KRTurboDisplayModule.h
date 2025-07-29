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
#import "KRBaseModule.h"
NS_ASSUME_NONNULL_BEGIN
/** 设置当前UI作为TurboDisplay首屏通知 */
extern NSString *const kSetCurrentUIAsFirstScreenForNextLaunchNotificationName;
/** 关闭TurboDisplay模式通知 */
extern NSString *const kCloseTurboDisplayNotificationName;

/*
 * @brief TurboDisplay首屏直出渲染模式（通过直接执行二进制产物渲染生成首屏，避免业务代码执行后再生成的首屏等待耗时）
 */
@interface KRTurboDisplayModule : KRBaseModule
/// 首屏使用了TurboDisplay
@property (nonatomic, assign) BOOL firstScreenTurboDisplay;

@end

NS_ASSUME_NONNULL_END

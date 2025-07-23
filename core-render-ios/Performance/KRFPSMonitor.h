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

typedef NS_ENUM(NSInteger, KRFPSThead) {
    KRFPSThead_Main = 1,
    KRFPSThead_Kotlin = 2,
};


@interface KRFPSMonitor : NSObject

/// 当前fps，每秒更新
@property (nonatomic, assign, readonly) NSUInteger curFPS;
/// 平均fps
@property (nonatomic, assign, readonly) NSUInteger avgFPS;
/// 最大fps
@property (nonatomic, assign, readonly) NSUInteger maxFPS;
/// 最低fps
@property (nonatomic, assign, readonly) NSUInteger minFPS;

- (instancetype)initWithThread:(KRFPSThead)thread pageName:(NSString *)pageName;

- (void)onTick:(NSTimeInterval)timestamp;

- (void)endMonitor;

@end

NS_ASSUME_NONNULL_END

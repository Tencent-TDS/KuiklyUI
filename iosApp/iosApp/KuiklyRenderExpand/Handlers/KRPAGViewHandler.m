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

#import "KRPAGViewHandler.h"

@implementation KRPAGViewHandler

#pragma mark - 注册适配器

+ (void)load {
    [KRPAGView registerPAGViewCreator:^id<KRPagViewProtocol> _Nonnull(CGRect frame) {
        return [[KRPAGViewHandler alloc] initWithFrame:frame];
    }];
}

#pragma mark - KRPagViewProtocol

// 合成 ownerPagView 属性（协议 @required）
@synthesize ownerPagView;

/*
 * 以下协议方法由父类 PAGView (libpag) 已提供实现，无需重写：
 *
 * - (BOOL)setPath:(NSString *)filePath;
 * - (void)addListener:(id<IPAGViewListener>)listener;
 * - (void)removeListener:(id<IPAGViewListener>)listener;
 * - (void)play;
 * - (void)stop;
 * - (void)setProgress:(double)value;
 * - (void)setRepeatCount:(int)repeatCount;
 * - (void)setScaleMode:(int)scaleMode;
 * - (PAGComposition *)getComposition;
 *
 * 如果需要自定义行为，可在此处 override 对应方法。
 */

@end

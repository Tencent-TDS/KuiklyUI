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

#import "KRAPNGViewHandler.h"

@implementation KRAPNGViewHandler

+ (void)load {
    [KRAPNGView registerAPNGViewCreator:^id<APNGImageViewProtocol> _Nonnull(CGRect frame) {
        KRAPNGViewHandler *apngView = [[KRAPNGViewHandler alloc] initWithFrame:frame];
        [apngView addObserver:apngView forKeyPath:@"currentLoopCount" options:NSKeyValueObservingOptionNew context:nil];
        apngView.autoPlayAnimatedImage = NO;
        return apngView;
    }];
}

#pragma mark - APNGImageViewProtocol

/// 播放次数，0表示无限循环，如果不设置，则会使用从apng图解析出来的count
@synthesize playCount = _playCount;
@synthesize delegate = _delegate;

- (void)setPlayCount:(NSInteger)playCount {
    if (playCount >= 0) {
        _playCount = playCount;
    }
    self.shouldCustomLoopCount = YES;
    self.animationRepeatCount = _playCount;
}

/// 设置图片路径（优先实现该setFilePath）
/// @param filePath 图片路径
/// @param completion 图片解析完成后回调
- (void)setFilePath:(NSString *_Nullable)filePath withCompletion:(void (^_Nullable)(UIImage * _Nullable image))completion {
    [self sd_setImageWithURL:[NSURL fileURLWithPath:filePath]
                   completed:^(UIImage *image, NSError *error, SDImageCacheType cacheType, NSURL *imageURL) {
        if (completion) {
            completion(image);
        }
    }];
}
/// 设置图片路径
/// @param filePath 图片路径
- (void)setFilePath:(NSString *_Nullable)filePath {
    [self sd_setImageWithURL:[NSURL fileURLWithPath:filePath]];
}


/// 开始播放动画（第一次播放调用该接口，从第一帧开始）
- (void)startAPNGAnimating {
    [self startAnimating];
}

/// 停止播放动画
- (void)stopAPNGAnimating {
    [self stopAnimating];
    if ([_delegate respondsToSelector:@selector(apngImageView:playEndLoop:)]) {
        [_delegate apngImageView:self playEndLoop:self.currentLoopCount];
    }
}

/// 监听播放次数的变化，播放完后进行回调
- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary<NSKeyValueChangeKey,id> *)change
                       context:(void *)context {
    if ([keyPath isEqualToString:@"currentLoopCount"]) {
        NSUInteger currentLoopCount = [change[NSKeyValueChangeNewKey] integerValue];
        if (self.animationRepeatCount && currentLoopCount >= self.animationRepeatCount) {
            [self stopAPNGAnimating];
        }
    }
}


@end


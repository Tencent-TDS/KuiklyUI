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

#import "KRVideoViewHandler.h"
#import <AVFoundation/AVFoundation.h>

@interface KRVideoViewHandler()<WMPlayerDelegate>

@property (nonatomic, assign) BOOL hasNotifiedFirstFrame;

@end

@implementation KRVideoViewHandler

+ (void)load {
    [KRVideoView registerVideoViewCreator:^id<KRVideoViewProtocol> _Nonnull(NSString * _Nonnull src, CGRect frame) {
        WMPlayerModel *playerModel = [WMPlayerModel new];
        playerModel.videoURL = [NSURL URLWithString:src];
        KRVideoViewHandler *videoView = [[KRVideoViewHandler alloc] initPlayerModel:playerModel];
        videoView.delegate = videoView;
        return videoView;
    }];
}

#pragma mark - KRVideoViewProtocol

@synthesize krv_delegate;

- (instancetype)initPlayerModel:(WMPlayerModel *)playerModel {
    if (self = [super initPlayerModel:playerModel]) {
        _hasNotifiedFirstFrame = NO;
        // 监听 playerLayer 的 readyForDisplay 属性来检测首帧渲染
        [self addObserver:self forKeyPath:@"playerLayer.readyForDisplay" options:NSKeyValueObservingOptionNew context:nil];
    }
    return self;
}

- (void)dealloc {
    @try {
        [self removeObserver:self forKeyPath:@"playerLayer.readyForDisplay"];
    } @catch (NSException *exception) {
        // 忽略重复移除观察者的异常
    }
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary<NSKeyValueChangeKey,id> *)change context:(void *)context {
    if ([keyPath isEqualToString:@"playerLayer.readyForDisplay"]) {
        BOOL isReady = [[change objectForKey:NSKeyValueChangeNewKey] boolValue];
        if (isReady && !self.hasNotifiedFirstFrame) {
            self.hasNotifiedFirstFrame = YES;
            // 首帧已经渲染，通知代理
            if (self.krv_delegate && [self.krv_delegate respondsToSelector:@selector(videoFirstFrameDidDisplay)]) {
                [self.krv_delegate videoFirstFrameDidDisplay];
            }
        }
    }
}

- (void)krv_play {
    [self play];
}

- (void)krv_preplay {
    //
}

- (void)krv_pause {
    [self pause];
}

- (void)krv_stop {
    // 重置首帧标记
    self.hasNotifiedFirstFrame = NO;
    [self resetWMPlayer];
}

- (void)krv_setVideoContentMode:(KRVideoViewContentMode)videoViewContentMode {
    if (videoViewContentMode == KRVideoViewContentModeScaleToFill) {
        [self setPlayerLayerGravity:(WMPlayerLayerGravityResize)];
    }  else if (videoViewContentMode == KRVideoViewContentModeScaleAspectFit) {
        [self setPlayerLayerGravity:(WMPlayerLayerGravityResizeAspect)];
    } else {
        [self setPlayerLayerGravity:(WMPlayerLayerGravityResizeAspectFill)];
    }
}

- (void)krv_setRate:(CGFloat)rate {
    self.rate = rate;
}

- (void)krv_setMuted:(BOOL)muted {
    self.muted = muted;
}
/*
 * kuikly侧设置的属性，一般用于业务扩展使用
 */
- (void)krv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    
}
/*
 * kuikly侧调用方法，一般用于业务扩展使用
 */
- (void)krv_callWithMethod:(NSString *)method params:(NSString *)params {
    
}

#pragma mark - WMPlayerDelegate

//准备播放的代理方法
-(void)wmplayerReadyToPlay:(WMPlayer *)wmplayer WMPlayerStatus:(WMPlayerState)state {
    [self.krv_delegate videoPlayStateDidChangedWithState:(KRVideoPlayStatePlaying) extInfo:@{}];
}
//播放失败的代理方法
-(void)wmplayerFailedPlay:(WMPlayer *)wmplayer WMPlayerStatus:(WMPlayerState)state {
    [self.krv_delegate videoPlayStateDidChangedWithState:(KRVideoPlayStateFaild) extInfo:@{}];
}

//播放器已经拿到视频的尺寸大小
-(void)wmplayerGotVideoSize:(WMPlayer *)wmplayer videoSize:(CGSize )presentationSize {
    
}

//播放完毕的代理方法
-(void)wmplayerFinishedPlay:(WMPlayer *)wmplayer {
    [self.krv_delegate videoPlayStateDidChangedWithState:(KRVideoPlayStatePlayEnd) extInfo:@{}];
}

+(BOOL)IsiPhoneX {
    return CGRectGetHeight([UIApplication sharedApplication].statusBarFrame) > 30;
}


@end

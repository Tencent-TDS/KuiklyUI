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

@interface KRVideoViewHandler()<WMPlayerDelegate>

@property (nonatomic, assign) BOOL isPlaying;
@property (nonatomic, strong) NSTimer *progressTimer;

@end

@implementation KRVideoViewHandler

+ (void)load {
    [KRVideoView registerVideoViewCreator:^id<KRVideoViewProtocol> _Nonnull(NSString * _Nonnull src, CGRect frame) {
        WMPlayerModel *playerModel = [WMPlayerModel new];
        playerModel.videoURL = [NSURL URLWithString:src];
        KRVideoViewHandler *videoView = [[KRVideoViewHandler alloc] initPlayerModel:playerModel];
        videoView.delegate = videoView;
        UIView *topView = [videoView valueForKey:@"topView"];
        UIView *bottomView = [videoView valueForKey:@"bottomView"];
        topView.hidden = YES;
        bottomView.hidden = YES;
        return videoView;
    }];
}

- (void)play {
    [super play];
    self.isPlaying = YES;
    [self startProgressTimer];
}

- (void)pause {
    [super pause];
    self.isPlaying = NO;
    [self stopProgressTimer];
}

- (void)resetWMPlayer {
    [super resetWMPlayer];
    self.isPlaying = NO;
    [self stopProgressTimer];
}

#pragma mark - KRVideoViewProtocol

@synthesize krv_delegate;

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

#pragma mark - ProgressTimer

- (void)startProgressTimer {
    if (self.progressTimer) {
        [self.progressTimer invalidate];
        self.progressTimer = nil;
    }
    self.progressTimer = [NSTimer scheduledTimerWithTimeInterval:1.0
                                                          target:self
                                                        selector:@selector(updatePlayerProgress)
                                                        userInfo:nil
                                                         repeats:YES];
}

- (void)stopProgressTimer {
    if (self.progressTimer) {
        [self.progressTimer invalidate];
        self.progressTimer = nil;
    }
}

- (void)updatePlayerProgress {
    [self.krv_delegate playTimeDidChangedWithCurrentTime:[self currentTime] totalTime:[self duration]];
}

- (void)krv_seekToTime:(NSUInteger)seekTotime { 
    // ...
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
    // 通知首帧已显示，用于隐藏封面图片
    [self.krv_delegate videoFirstFrameDidDisplay];
}

//播放完毕的代理方法
-(void)wmplayerFinishedPlay:(WMPlayer *)wmplayer {
    [self.krv_delegate videoPlayStateDidChangedWithState:(KRVideoPlayStatePlayEnd) extInfo:@{}];
}

-(void)wmplayer:(WMPlayer *)wmplayer singleTaped:(UITapGestureRecognizer *)singleTap {
    if (self.isPlaying) {
        [wmplayer pause];
        self.isPlaying = NO;
    } else {
        [wmplayer play];
        self.isPlaying = YES;
    }
}

+(BOOL)IsiPhoneX {
    return CGRectGetHeight([UIApplication sharedApplication].statusBarFrame) > 30;
}


@end


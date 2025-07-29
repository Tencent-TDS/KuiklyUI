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

#import "KRVideoView.h"
#import "KRComponentDefine.h"
#import "KRHttpRequestTool.h"
#import "KRConvertUtil.h"
/// 播控操作状态化维护
typedef NS_ENUM(NSInteger, KRVideoViewPlayControl) {
    KRVideoViewPlayControlNone = 0,
    KRVideoViewPlayControlPreplay = 1, //操作预播放视频
    KRVideoViewPlayControlPlay = 2, // 操作播放视频
    KRVideoViewPlayControlPause = 3, // 操作暂停视频
    KRVideoViewPlayControlStop = 4   // 操作停止视频
};

static VideoViewCreator gVideoViewCreator;

@interface KRVideoView()<KRVideoViewDelegate>

@property (nonatomic, strong) id<KRVideoViewProtocol> videoView;
/// 播放源属性
@property (nonatomic, strong) NSString *css_src;
/// 播控操作属性
@property (nonatomic, strong) NSNumber *css_playControl;
/// 画面拉伸模式
@property (nonatomic, strong) NSString *css_resizeMode;
/// 静音属性
@property (nonatomic, strong) NSNumber *css_muted;
/// 倍速属性
@property (nonatomic, strong) NSNumber *css_rate;
/// 首帧事件
@property (nonatomic, strong) KuiklyRenderCallback css_firstFrame;
/// 播放状态变化事件
@property (nonatomic, strong) KuiklyRenderCallback css_stateChange;
/// 播放时间变化事件
@property (nonatomic, strong) KuiklyRenderCallback css_playTimeChange;
/// 通用扩展事件
@property (nonatomic, strong) KuiklyRenderCallback css_customEvent;

@end

@implementation KRVideoView
@synthesize hr_rootView;

+ (void)registerVideoViewCreator:(VideoViewCreator)creator {
    gVideoViewCreator = creator;
    NSAssert(gVideoViewCreator, @"creator 不能为空");
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        NSAssert(gVideoViewCreator, @"should registerVideoViewCreato");
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
    if ([_videoView respondsToSelector:@selector(krv_setPropWithKey:propValue:)]) {
        [_videoView krv_setPropWithKey:propKey propValue:propValue];
    }
    
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
    if ([_videoView respondsToSelector:@selector(krv_callWithMethod:params:)]) {
        [_videoView krv_callWithMethod:method params:params];
    }
}

#pragma mark - css 属性

- (void)setCss_src:(NSString *)css_src {
    if (!_css_src && css_src.length) { // 因为播放器不复用，所以就一次绑定src即可
        _css_src = css_src;
        [self p_createVideoViewIfNeed];
    }
}

- (void)setCss_resizeMode:(NSString *)css_resizeMode {
    _css_resizeMode = css_resizeMode;
    if ([css_resizeMode isEqualToString:@"cover"]) {
        [_videoView krv_setVideoContentMode:(KRVideoViewContentModeScaleAspectFill)];
    } else if ([css_resizeMode isEqualToString:@"stretch"]) {
        [_videoView krv_setVideoContentMode:(KRVideoViewContentModeScaleToFill)];
    } else {
        [_videoView krv_setVideoContentMode:(KRVideoViewContentModeScaleAspectFit)];
    }
}

- (void)setCss_playControl:(NSNumber *)css_playControl {
    _css_playControl = css_playControl;
    switch ([css_playControl intValue]) {
        case KRVideoViewPlayControlPreplay:
            [_videoView krv_preplay];
            break;
        case KRVideoViewPlayControlPlay:
            [_videoView krv_play];
            break;
        case KRVideoViewPlayControlPause:
            [_videoView krv_pause];
            break;
        case KRVideoViewPlayControlStop:
            [_videoView krv_stop];
            break;
        default:
            break;
    }
}

- (void)setCss_muted:(NSNumber *)css_muted {
    _css_muted = css_muted;
    [_videoView krv_setMuted:[_css_muted boolValue]];
}

- (void)setCss_rate:(NSNumber *)css_rate {
    _css_rate = css_rate;
    [_videoView krv_setRate:[css_rate floatValue]];
}



- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    [self p_createVideoViewIfNeed];
    ((UIView *)_videoView).frame = self.bounds;
}

#pragma mark - KRVideoViewDelegate

- (void)videoPlayStateDidChangedWithState:(KRVideoPlayState)playState extInfo:(NSDictionary<NSString *, NSString *> *)extInfo {
    if (_css_stateChange) {
        _css_stateChange(@{@"state" : @(playState), @"extInfo": extInfo ?: @{}});
    }
    
}

- (void)playTimeDidChangedWithCurrentTime:(NSUInteger)currentTime totalTime:(NSUInteger)totalTime {
    if (_css_playTimeChange) {
        _css_playTimeChange(@{@"currentTime" : @(currentTime), @"totalTime": @(totalTime)});
    }
}

- (void)videoFirstFrameDidDisplay {
    if (_css_firstFrame) {
        _css_firstFrame(@{});
    }
}

- (void)customEventWithInfo:(NSDictionary<NSString *,NSString *> *)eventInfo {
    if (_css_customEvent) {
        _css_customEvent(eventInfo);
    }
}

#pragma mark - private

- (void)p_createVideoViewIfNeed {
    if (_videoView) {
        return ;
    }
    NSAssert(gVideoViewCreator, @"宿主未注册VideoView实现，却使用了VideoView");
    if (_css_src.length && !CGSizeEqualToSize(self.bounds.size, CGSizeZero) && gVideoViewCreator) {
        _videoView = gVideoViewCreator(_css_src, self.bounds);
        _videoView.krv_delegate = self;
        NSAssert([_videoView isKindOfClass:[UIView class]], @"videoView需要为UIView的子类");
        [self addSubview:(UIView *)_videoView];
        ((UIView *)_videoView).frame = self.bounds;
        [self setCss_resizeMode:_css_resizeMode];
        [self setCss_muted:_css_muted];
        if (_css_rate) {
            [self setCss_rate:_css_rate];
        }
        [self setCss_playControl:_css_playControl];
       
    }
}

@end

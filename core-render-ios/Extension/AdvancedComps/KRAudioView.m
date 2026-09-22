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

#import "KRAudioView.h"
#import "KRComponentDefine.h"
#import "KRConvertUtil.h"

/// 播控操作状态化维护，对齐 com.tencent.kuikly.core.views.AudioPlayControl
typedef NS_ENUM(NSInteger, KRAudioViewPlayControl) {
    KRAudioViewPlayControlNone = 0,
    KRAudioViewPlayControlPlay = 1, // 操作播放音频
    KRAudioViewPlayControlPause = 2, // 操作暂停音频
    KRAudioViewPlayControlStop = 3   // 操作停止音频
};

static AudioViewCreator gAudioViewCreator;

@interface KRAudioView()<KRAudioViewDelegate>

@property (nonatomic, strong) id<KRAudioViewProtocol> audioView;
/// 播放源属性
@property (nonatomic, strong) NSString *css_src;
/// 播控操作属性
@property (nonatomic, strong) NSNumber *css_playControl;
/// 倍速属性
@property (nonatomic, strong) NSNumber *css_rate;
/// seek 属性（毫秒）
@property (nonatomic, strong) NSNumber *css_seek;
/// 播放状态变化事件
@property (nonatomic, strong) KuiklyRenderCallback css_stateChange;
/// 播放时间变化事件
@property (nonatomic, strong) KuiklyRenderCallback css_playTimeChange;

@end

@implementation KRAudioView
@synthesize hr_rootView;

+ (void)registerAudioViewCreator:(AudioViewCreator)creator {
    gAudioViewCreator = creator;
    NSAssert(gAudioViewCreator, @"creator 不能为空");
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        NSAssert(gAudioViewCreator, @"should registerAudioViewCreator");
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
    if ([_audioView respondsToSelector:@selector(kra_setPropWithKey:propValue:)]) {
        [_audioView kra_setPropWithKey:propKey propValue:propValue];
    }
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
    if ([_audioView respondsToSelector:@selector(kra_callWithMethod:params:)]) {
        [_audioView kra_callWithMethod:method params:params];
    }
}

#pragma mark - css 属性

- (void)setCss_src:(NSString *)css_src {
    if (!_css_src && css_src.length) { // 播放器不复用，一次绑定 src 即可
        _css_src = css_src;
        [self p_createAudioViewIfNeed];
    }
}

- (void)setCss_playControl:(NSNumber *)css_playControl {
    _css_playControl = css_playControl;
    switch ([css_playControl intValue]) {
        case KRAudioViewPlayControlPlay:
            [_audioView kra_play];
            break;
        case KRAudioViewPlayControlPause:
            [_audioView kra_pause];
            break;
        case KRAudioViewPlayControlStop:
            [_audioView kra_stop];
            break;
        default:
            break;
    }
}

- (void)setCss_rate:(NSNumber *)css_rate {
    _css_rate = css_rate;
    [_audioView kra_setRate:[css_rate floatValue]];
}

- (void)setCss_seek:(NSNumber *)css_seek {
    _css_seek = css_seek;
    [_audioView kra_seekToTime:(NSUInteger)[css_seek unsignedIntegerValue]];
}

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    [self p_createAudioViewIfNeed];
    ((UIView *)_audioView).frame = self.bounds;
}

#pragma mark - KRAudioViewDelegate

- (void)audioPlayStateDidChangedWithState:(KRAudioPlayState)playState extInfo:(NSDictionary<NSString *, NSString *> *)extInfo {
    if (_css_stateChange) {
        _css_stateChange(@{@"state" : @(playState), @"extInfo": extInfo ?: @{}});
    }
}

- (void)playTimeDidChangedWithCurrentTime:(NSUInteger)currentTime totalTime:(NSUInteger)totalTime {
    if (_css_playTimeChange) {
        _css_playTimeChange(@{@"currentTime" : @(currentTime), @"totalTime": @(totalTime)});
    }
}

#pragma mark - private

- (void)p_createAudioViewIfNeed {
    if (_audioView) {
        return;
    }
    NSAssert(gAudioViewCreator, @"宿主未注册AudioView实现，却使用了AudioView");
    if (_css_src.length && !CGSizeEqualToSize(self.bounds.size, CGSizeZero) && gAudioViewCreator) {
        _audioView = gAudioViewCreator(_css_src, self.bounds);
        _audioView.kra_delegate = self;
        NSAssert([_audioView isKindOfClass:[UIView class]], @"audioView需要为UIView的子类");
        [self addSubview:(UIView *)_audioView];
        ((UIView *)_audioView).frame = self.bounds;
        if (_css_rate) {
            [self setCss_rate:_css_rate];
        }
        [self setCss_playControl:_css_playControl];
    }
}

@end

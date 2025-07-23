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

#import "KRAPNGView.h"
#import "KRComponentDefine.h"
#import "KRHttpRequestTool.h"
#import "KRConvertUtil.h"
#import "KRCacheManager.h"

@interface KRAPNGView()<APNGViewPlayDelegate>

@property (nonatomic, strong) id<APNGImageViewProtocol> apngView;
@property (nonatomic, strong) NSString *css_src;
@property (nonatomic, strong) NSNumber *css_autoPlay;
@property (nonatomic, strong) NSNumber *css_repeatCount;

@property (nonatomic,strong) KuiklyRenderCallback css_loadFailure;
@property (nonatomic,strong) KuiklyRenderCallback css_animationStart;
@property (nonatomic,strong) KuiklyRenderCallback css_animationEnd;

@end

@implementation KRAPNGView {
    BOOL _didSetFilePath;
}

@synthesize hr_rootView;

static APNGViewCreator gAPNGViewCreator;

+ (void)registerAPNGViewCreator:(APNGViewCreator)creator {
    gAPNGViewCreator = creator;
    NSAssert(gAPNGViewCreator, @"creator不能为空");
}

+ (id<APNGImageViewProtocol>)createAPNGVIewWithFrame:(CGRect)frame {
    id<APNGImageViewProtocol> apngView = nil;
    if (gAPNGViewCreator) {
        apngView = gAPNGViewCreator(frame);
    }
    NSAssert(apngView, @"请实现APNGAdapter");
    return apngView;
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _css_autoPlay = @(YES);
        _apngView = [[self class] createAPNGVIewWithFrame:frame];
        _apngView.delegate = self;
        NSAssert([_apngView isKindOfClass:[UIView class]], @"apngview 需要为UIView的子类");
        [self addSubview:(UIView *)_apngView];
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}



- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}

#pragma mark - css property setter

- (void)setCss_src:(NSString *)css_src {
    if (_css_src != css_src) {
        _css_src = css_src;
       if([[self class] isCdnUrl:_css_src]) {
            [self handleCDNTypeWithSrc:css_src];
       } else {
           [self p_setWithFilePath:css_src];
           [self tryToAutoPlayInNextLoop];
       }
    }
}

- (void)setCss_repeatCount:(NSNumber *)css_repeatCount {
    _css_repeatCount = css_repeatCount;
    _apngView.playCount = [KRConvertUtil NSInteger:css_repeatCount];
    if ([_apngView respondsToSelector:@selector(setShowLastImageWhenPause:)]) {
         [_apngView setShowLastImageWhenPause:YES];
    }
}

- (void)setCss_autoPlay:(NSNumber *)css_autoPlay {
    _css_autoPlay = css_autoPlay;
    if ([_css_autoPlay boolValue]) {
        [self p_tryToAutoPlay];
    }
}

#pragma mark - css method

- (void)css_play:(NSDictionary *)args  {
    _css_autoPlay = @(YES);
    [_apngView startAPNGAnimating];
    if (self.css_animationStart) {
        self.css_animationStart(@{});
    }
}

- (void)css_stop:(NSDictionary *)args  {
    _css_autoPlay = @(NO);
    [_apngView stopAPNGAnimating];
}


#pragma mark - ImagePlayDelegate

/// 播放结束接口
/// - Parameter imageView: apngView视图
/// - Parameter loopCount: 已播放次数
- (void)apngImageView:(UIView *_Nonnull)apngImageView playEndLoop:(NSUInteger)loopCount {
    if (self.css_animationEnd) {
        self.css_animationEnd(@{});
    }
}


#pragma mark - override

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    ((UIView *)_apngView).frame = self.bounds;
}



#pragma mark - dealloc

- (void)dealloc {
    [_apngView stopAPNGAnimating];
    _apngView.delegate = nil;
}


#pragma mark - private

- (void)p_tryToAutoPlay {
    if (_didSetFilePath && [_css_autoPlay boolValue]) {
        [self css_play:nil];
    }
}
// 当前帧因css_autoPlay默认为true，需要等当前帧设置完该字段，下一帧再去尝试播放
-  (void)tryToAutoPlayInNextLoop {
    __typeof(self) __weak weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        [weakSelf p_tryToAutoPlay];
    });
}

- (void)handleCDNTypeWithSrc:(NSString *)src {
    // download
    NSString *storePath = [[self class] generateStorePathWithUrl:src];
    if ([[NSFileManager defaultManager] fileExistsAtPath:storePath]) {
        [self p_setWithFilePath:storePath];
        [self tryToAutoPlayInNextLoop];
    } else {
        __typeof(self) __weak weakSelf = self;
        [KRHttpRequestTool downloadWithUrl:src param:nil sotrePath:storePath responseBlock:^(NSString * _Nullable path, NSError * _Nullable error) {
            dispatch_async(dispatch_get_main_queue(), ^{
                if ([[NSFileManager defaultManager] fileExistsAtPath:storePath]) {
                    [weakSelf p_setWithFilePath:storePath];
                    [weakSelf p_tryToAutoPlay];
                } else {
                    // 加载失败
                    if (weakSelf.css_loadFailure) {
                        weakSelf.css_loadFailure(@{});
                    }
                }
            });
        }];

    }
}

- (void)p_setWithFilePath:(NSString *)filePath {
    if ([self.apngView respondsToSelector:@selector(setFilePath:withCompletion:)]) {
        [self.apngView setFilePath:filePath withCompletion:nil];
    } else if ([self.apngView respondsToSelector:@selector(setFilePath:)]) {
        [self.apngView setFilePath:filePath];
    } else {
        NSAssert(0, @"apng未实现对应setFilePath方法");
    }
    _didSetFilePath = YES;
}



#pragma mark class method
+ (NSString *)generateStorePathWithUrl:(NSString *)storePath {
    NSString *cacheBasePath = [[KRCacheManager sharedInstance] cachePathWithFolderName:nil];
    return [cacheBasePath stringByAppendingPathComponent:[NSString stringWithFormat:@"kuikly_render_apng_%@.png", [KRConvertUtil hr_md5StringWithString:storePath]]];
}

+ (void)preDownloadIfNeedWithCDNUrl:(NSString *)cdnUrl {
    NSString *storePath = [self generateStorePathWithUrl:cdnUrl];
    if (![[NSFileManager defaultManager] fileExistsAtPath:storePath]) {
        [KRHttpRequestTool downloadWithUrl:cdnUrl
                                     param:nil
                                 sotrePath:storePath
                             responseBlock:^(NSString * _Nullable path, NSError * _Nullable error) {
        }];
    }
}

+ (BOOL)isCdnUrl:(NSString *)url {
    return [url hasPrefix:@"https://"] || [url hasPrefix:@"http://"];
}


@end

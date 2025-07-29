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

#import "KRPAGView.h"
#import "KRComponentDefine.h"
#import "KRHttpRequestTool.h"
#import "KRConvertUtil.h"
#import "KRAPNGView.h"
#import "KuiklyRenderView.h"
#import "KuiklyRenderBridge.h"
#import "KuiklyContextParam.h"
#import "KRCacheManager.h"


NSString *const KRPagAssetsPrefix = @"assets://";

@interface KRPAGView()<IPAGViewListener>


@property (nonatomic, strong) id<KRPagViewProtocol>pagView;

@property (nonatomic, strong) NSString *css_src;
@property (nonatomic, strong) NSNumber *css_autoPlay;
@property (nonatomic, strong) NSNumber *css_repeatCount;
@property (nonatomic, strong) NSString *css_replaceTextLayerContent;
@property (nonatomic, strong) NSString *css_replaceImageLayerContent;

@property (nonatomic,strong) KuiklyRenderCallback css_loadFailure;
@property (nonatomic,strong) KuiklyRenderCallback css_animationStart;
@property (nonatomic,strong) KuiklyRenderCallback css_animationEnd;
@property (nonatomic,strong) KuiklyRenderCallback css_animationCancel;
@property (nonatomic,strong) KuiklyRenderCallback css_animationRepeat;

@end


@implementation KRPAGView {
    BOOL _didSetFilePath;
}


@synthesize hr_rootView;

static PAGViewCreator gPagViewCreator;

+ (void)registerPAGViewCreator:(PAGViewCreator)creator {
    gPagViewCreator = creator;
    NSAssert(gPagViewCreator, @"creator 不能为空");
}

+ (id<KRPagViewProtocol>)createPagViewWithFrame:(CGRect)frame {
    id<KRPagViewProtocol> pagView = nil;
    if (gPagViewCreator) {
        pagView = gPagViewCreator(frame);
    }
    if (!pagView) {
        pagView = (id<KRPagViewProtocol>)[((UIView *)[NSClassFromString(@"PAGView") alloc]) initWithFrame:frame];
        NSAssert(pagView, @"should pod 'libpag', >= 3.3.0.245-noffmpeg");
    }
    return pagView;
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _css_autoPlay = @(YES); // 默认自动播
        _pagView = [[self class] createPagViewWithFrame:frame];
        if ([_pagView respondsToSelector:@selector(setOwnerPagView:)]) {
            [_pagView setOwnerPagView:self];
        }
        [_pagView addListener:self];
        NSAssert([_pagView isKindOfClass:[UIView class]], @"pageView 需要为UIView的子类");
        [self addSubview:(UIView *)_pagView];
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
    if ([_pagView respondsToSelector:@selector(kr_setKuiklyPropWithKey:propValue:)]) {
        [_pagView kr_setKuiklyPropWithKey:propKey propValue:propValue];
    }
    
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}

#pragma mark - Setters

- (void)setCss_src:(NSString *)css_src {
    
    if (_css_src != css_src) {
        _css_src = css_src;
        if([KRAPNGView isCdnUrl:_css_src]) {
            __typeof(self) __weak weakSelf = self;
            [self fetchPagFileIfNeedWithCdnUrl:_css_src completion:^(NSString * _Nullable path, NSError * _Nullable error) {
                if ([[NSFileManager defaultManager] fileExistsAtPath:path]) {
                    [weakSelf p_setWithFilePath:path];
                }
                [weakSelf tryToAutoPlayInNextLoop];
            }];
        } else if ([css_src hasPrefix:KRPagAssetsPrefix]) {
            NSString *assetPath = [self p_getAssetPathWithCss_src:css_src];
            [self p_setWithFilePath:assetPath];
            [self tryToAutoPlayInNextLoop];
        }
        else {
            [self p_setWithFilePath:css_src];
            [self tryToAutoPlayInNextLoop];
        }
    }
}

- (void)setCss_repeatCount:(NSNumber *)css_repeatCount {
    _css_repeatCount = css_repeatCount;
    [self.pagView setRepeatCount:(int)[KRConvertUtil NSInteger:css_repeatCount]];
}

- (void)setCss_autoPlay:(NSNumber *)css_autoPlay {
    _css_autoPlay = css_autoPlay;
    if ([_css_autoPlay boolValue]) {
        [self p_tryToAutoPlay];
    }
}

- (void)setCss_replaceTextLayerContent:(NSString *)css_replaceTextLayerContent {
    NSArray *contents = [css_replaceTextLayerContent componentsSeparatedByString:@","];
    if (contents.count != 2) {
        return;
    }
    NSString *layerName = contents.firstObject;
    NSString *textName = contents.lastObject;
    
    [self p_tryToReplaceText:textName inLayerWithName:layerName];
}

- (void)setCss_replaceImageLayerContent:(NSString *)css_replaceImageLayerContent {
    NSArray *contents = [css_replaceImageLayerContent componentsSeparatedByString:@","];
    if (contents.count != 2) {
        return;
    }
    NSString *layerName = contents.firstObject;
    NSString *filePath = contents.lastObject;
    if ([filePath hasPrefix:KRPagAssetsPrefix]) {
        filePath = [self p_getAssetPathWithCss_src:filePath];
    }
    
    [self p_tryToReplaceImageWithFilePath:filePath inLayerWithName:layerName];
}

#pragma mark - css method

- (void)css_play:(NSDictionary *)args  {
    _css_autoPlay = @(YES);
    [self.pagView play];
}

- (void)css_stop:(NSDictionary *)args  {
    _css_autoPlay = @(NO);
    [self.pagView stop];
}

#pragma mark - override

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    ((UIView *)_pagView).frame = self.bounds;
}

#pragma mark - IPAGViewListener

/**
 * Notifies the start of the animation.
 */
- (void)onAnimationStart:(PAGView*)pagView {
    if (self.css_animationStart) {
        self.css_animationStart(@{});
    }
}

/**
 * Notifies the end of the animation.
 */
- (void)onAnimationEnd:(PAGView*)pagView {
    if (self.css_animationEnd) {
        self.css_animationEnd(@{});
    }
}

/**
 * Notifies the cancellation of the animation.
 */
- (void)onAnimationCancel:(PAGView *)pagView {
    if (self.css_animationCancel) {
        self.css_animationCancel(@{});
    }
}

/**
 * Notifies the repetition of the animation.
 */
- (void)onAnimationRepeat:(PAGView*)pagView {
    if (self.css_animationRepeat) {
        self.css_animationRepeat(@{});
    }
}

#pragma mark - dealloc

- (void)dealloc {
    [self.pagView removeListener:self];
    [self.pagView stop];
    self.pagView = nil;
}

#pragma mark - private

- (void)p_tryToAutoPlay {
    if ([_css_autoPlay boolValue] && _didSetFilePath) {
        [self.pagView play];
    }
}

- (void)tryToAutoPlayInNextLoop {
    __typeof(self) __weak weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        [weakSelf p_tryToAutoPlay];
    });
}

- (void)fetchPagFileIfNeedWithCdnUrl:(NSString *)url completion:(KRPagFileResponse)completion {
    // download
    NSString *storePath = [[self class] generateStorePathWithCdnUrl:url];
    if ([[NSFileManager defaultManager] fileExistsAtPath:storePath]) {
        if (completion) {
            completion(storePath, nil);
            [self tryToAutoPlayInNextLoop];
        }
    } else {
        __typeof(self) __weak weakSelf = self;
        [KRHttpRequestTool downloadWithUrl:url param:nil sotrePath:storePath responseBlock:^(NSString * _Nullable path, NSError * _Nullable error) {
            dispatch_async(dispatch_get_main_queue(), ^{
                if (completion) {
                    if (weakSelf.css_loadFailure && ![[NSFileManager defaultManager] fileExistsAtPath:path]) {
                        self.css_loadFailure(@{});
                    }
                    completion(path, error);
                    if ([[NSFileManager defaultManager] fileExistsAtPath:path]) {
                        [weakSelf p_tryToAutoPlay];
                    }
                }
            });
        }];
    }
}

- (void)p_setWithFilePath:(NSString *)filePath {
    [_pagView setPath:filePath];
    _didSetFilePath = YES;
}

- (void)p_tryToReplaceText:(NSString *)text inLayerWithName:(NSString *)layerName {
    id<IPAGCompositionProtocol> pagComposition = [self.pagView getComposition];
    
    if (!pagComposition) {
        [KRLogModule logError:[NSString stringWithFormat:@"替换 textLayer= %@ 失败，请检查该路径 %@ 的 PAG 素材", layerName, self.css_src]];
        return;
    }
    
    NSArray<id<IPAGLayerProtocol>> *layers = [pagComposition getLayersByName:layerName];
    for (id<IPAGLayerProtocol> layer in layers) {
        if (![layer isKindOfClass:NSClassFromString(@"PAGTextLayer")])
        {
            continue;
        }
        id<IPAGTextLayerProtocol> textLayer = (id<IPAGTextLayerProtocol>)layer;
        [textLayer setText:text];
    }
}

- (void)p_tryToReplaceImageWithFilePath:(NSString *)filePath inLayerWithName:(NSString *)layerName {
    id<IPAGCompositionProtocol> pagComposition = [self.pagView getComposition];
    
    if (!pagComposition) {
        [KRLogModule logError:[NSString stringWithFormat:@"替换 imageLayer= %@ 失败，请检查该路径 %@ 的 PAG 文件是否存在", layerName, self.css_src]];
        return;
    }
    
    NSArray<id<IPAGLayerProtocol>> *layers = [pagComposition getLayersByName:layerName];
    for (id<IPAGLayerProtocol> layer in layers) {
        if (![layer isKindOfClass:NSClassFromString(@"PAGImageLayer")])
        {
            continue;
        }
        id<PAGImageLayerProtocol> imageLayer = (id<PAGImageLayerProtocol>)layer;
        id<PAGImageProtocol> pagImage = [((id<PAGImageProtocol>)NSClassFromString(@"PAGImage")) FromPath:filePath];
        
        if(!pagImage) {
            [KRLogModule logError:[NSString stringWithFormat:@"替换 imageLayer= %@ 失败，请检查该路径 %@ 的图片素材是否存在", layerName, filePath]];
            break;
        }
        [imageLayer setImage:pagImage];
    }
}

- (NSString *)p_getAssetPathWithCss_src:(NSString *)css_src {
    
    NSURL *url = nil;
    NSString *fileExtension = [css_src pathExtension];
    NSRange subRange = NSMakeRange(KRPagAssetsPrefix.length, css_src.length - KRPagAssetsPrefix.length - fileExtension.length - 1);
    NSString *pathWithoutExtension = [css_src substringWithRange:subRange];
    
    KuiklyContextParam *contextParam = ((KuiklyRenderView *)self.hr_rootView).contextParam;
    url = [contextParam.contextMode urlForFileName:pathWithoutExtension extension:fileExtension];
    return url ? url.path : @"";
}

#pragma mark class method
+ (NSString *)generateStorePathWithCdnUrl:(NSString *)cdnUrl {
    NSString *cacheBasePath = [[KRCacheManager sharedInstance] cachePathWithFolderName:nil];
    
    return [cacheBasePath stringByAppendingPathComponent:[NSString stringWithFormat:@"kuikly_pag_%@.pag",[KRConvertUtil hr_md5StringWithString:cdnUrl]]];
}
@end

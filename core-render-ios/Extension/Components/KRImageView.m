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

#import "KRImageView.h"
#import "KRComponentDefine.h"
#import "KRMemoryCacheModule.h"
#import "KuiklyRenderView.h"
#import "KuiklyRenderBridge.h"
#import "KRConvertUtil.h"
#import "KuiklyContextParam.h"
#import "NSObject+KR.h"
#import "KRBlurView.h"
#import "KuiklyRenderThreadManager.h"

NSString *const KRImageAssetsPrefix = @"assets://";
NSString *const KRImageLocalPathPrefix = @"file://";

NSString *const KRImageBase64Prefix = @"data:image";



/*
 * @brief 图片刷新缓存类
 */
@interface KRImageRefreshCache : NSObject
/** 延迟批量更新标记 */
@property (nonatomic, assign) NSUInteger delayBatchFlag;
+ (instancetype)sharedInstance;
- (void)cacheWithKey:(NSString *)key image:(UIImage *)image;
- (UIImage *_Nullable)imageWithKey:(NSString *)key;
- (void)removeAllCache;


@end
typedef void (^KRSetImageBlock) (UIImage *_Nullable image);

/*
 * @brief 暴露给Kotlin侧调用的Image组件
 */
@interface KRImageView()
/** 图片url */
@property (nonatomic, copy) NSString *KUIKLY_PROP(src);
/** 图片适应模式 */
@property (nonatomic, copy) NSString *KUIKLY_PROP(resize);
/** 点9图 */
@property (nonatomic, copy) NSNumber *KUIKLY_PROP(dotNineImage);
/** 高斯模糊半径 */
@property (nonatomic, copy) NSNumber *KUIKLY_PROP(blurRadius);
/** 将指定颜色应用于图像，生成一个新的已染色的图像 */
@property (nonatomic, copy) NSString *KUIKLY_PROP(tintColor);
/** 图片视图渐变遮罩 */
@property (nonatomic, copy) NSString *KUIKLY_PROP(maskLinearGradient);
/** 图片拉伸区域 */
@property (nonatomic, copy) NSString *KUIKLY_PROP(capInsets);
/** 图片颜色滤镜 */
@property (nonatomic, copy) NSString *KUIKLY_PROP(colorFilter);


/** 图片加载成功回调事件 */
@property (nonatomic, strong, nullable) KuiklyRenderCallback KUIKLY_PROP(loadSuccess);
/** 图片分辨率加载成功回调事件 */
@property (nonatomic, strong, nullable) KuiklyRenderCallback KUIKLY_PROP(loadResolution);
/** 图片加载失败回调事件 */
@property (nonatomic, strong, nullable) KuiklyRenderCallback KUIKLY_PROP(loadFailure);
/** 记录图片加载失败，但 loadFailure 属性还未设置的情况 */
@property (nonatomic, assign) BOOL pendingLoadFailure;
/** loadFailure 回调还未设置时，图片加载失败的错误码 */
@property (nonatomic, assign) NSInteger errorCode;
/** 正在进行中的图片加载请求计数 */
@property (nonatomic, assign) NSUInteger imageLoadingCount;

@end

@implementation KRImageView {
    UIImage *_originImage;
}

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.contentMode = UIViewContentModeScaleAspectFill;
        self.clipsToBounds = YES;
        self.kr_reuseDisable = YES;
#if !TARGET_OS_OSX // [macOS]
        self.semanticContentAttribute = UISemanticContentAttributeForceLeftToRight;
#endif // [macOS]
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol


- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)hrv_prepareForeReuse {
    if (self.image && self.css_src && _originImage) {
        [[KRImageRefreshCache sharedInstance] cacheWithKey:self.css_src image:_originImage];
    }
    KUIKLY_RESET_CSS_COMMON_PROP;
    _originImage = nil;
    self.css_src = nil;
    self.css_tintColor = nil;
    self.css_colorFilter = nil;
    self.css_resize = nil;
    self.css_dotNineImage = nil;
    self.clipsToBounds = YES;
    self.css_loadSuccess = nil;
    self.css_loadResolution = nil;
    self.css_loadFailure = nil;
    self.css_capInsets = nil;
    self.pendingLoadFailure = false;
    self.errorCode = 0;
    self.imageLoadingCount = 0;
}

#pragma mark - setter
- (void)setAssetsImage:(NSString *)css_src {
    NSString *fileExtension = [css_src pathExtension];
    NSRange subRange = NSMakeRange(KRImageAssetsPrefix.length, css_src.length - KRImageAssetsPrefix.length - fileExtension.length - 1);
    NSString *pathWithoutExtension = [css_src substringWithRange:subRange];
    KuiklyContextParam *contextParam = ((KuiklyRenderView *)self.hr_rootView).contextParam;
    NSURL *url = [contextParam urlForFileName:pathWithoutExtension extension:fileExtension];
    if (url) {
        NSString *urlString = url ? url.absoluteString : @"";
        [self setImageWithLocalUrl:urlString];
    } else {
        // 没有在默认位置找到assets资源，将src传递给适配器由业务处理
        [self setImageWithUrl:css_src];
    }
}

// 本地文件格式的("file://")图片，Kuikly支持默认加载
- (void)setImageWithLocalUrl:(NSString *)localUrl {
    bool handled = [self setImageWithUrl:localUrl];
    if (handled) {
        return;
    }
    // Remove "file://" prefix to get the actual file path
    NSString *actualPath = [localUrl substringFromIndex:[KRImageLocalPathPrefix length]];
    UIImage *image = [UIImage imageWithContentsOfFile:actualPath];
    self.image = image;
}

- (void)setCss_src:(NSString *)css_src {
    if (self.css_src != css_src) {
        _css_src = css_src;
        [self bindImageToView:nil]; // clear current image 清除缓存
        if (css_src) {
            UIImage *image = [[KRImageRefreshCache sharedInstance] imageWithKey:css_src];
            if (image) {
                self.image = image;
                return;
            }
            // 缓存中不存在当前src对应的图片，则再执行加载
            [self setImageWithSrc:css_src];
        }
    }
}

/*
 * 根据src加载原始图片 + 原始图片染色
 * @param css_src：图片路径
 */
- (void)setImageWithSrc:(NSString *)css_src {
    if (css_src) {
        [self bindImageToView:nil]; // clear current image
        if ([css_src hasPrefix:KRImageAssetsPrefix]) {
            [self setAssetsImage:css_src];
        } else if ([css_src hasPrefix:KRImageBase64Prefix]) {
            [self setImageWithUrl:nil]; // cancel before url download
            [self p_setBase64Image:css_src];
        } else if([css_src hasPrefix:KRImageLocalPathPrefix]) {
            [self setImageWithLocalUrl:css_src];
        } else {    // @"http://", @"https://"
            [self setImageWithUrl:css_src];
        }
    }
}

- (void)setCss_blurRadius:(NSNumber *)css_blurRadius {
    if (_css_blurRadius != css_blurRadius) {
        _css_blurRadius = css_blurRadius;
        if (_originImage == nil) {
            [self setImageWithSrc:self.css_src];
        } else {
            [self p_updateWithImage:_originImage];
        }
    }
}

- (void)setCss_tintColor:(NSString *)css_tintColor {
    _css_tintColor = css_tintColor;
    self.tintColor = [UIView css_color:css_tintColor];
    if (_originImage == nil) {
        [self setImageWithSrc:self.css_src];
    } else {
        [self p_updateWithImage:_originImage];
    }
}

- (void)setCss_colorFilter:(NSString *)css_colorFilter {
    if (_css_colorFilter != css_colorFilter) {
        _css_colorFilter = css_colorFilter;
        if (_originImage == nil) {
            [self setImageWithSrc:self.css_src];
        } else {
            [self p_updateWithImage:_originImage];
        }
    }
}

- (void)setCss_maskLinearGradient:(NSString *)css_maskLinearGradient {
    if (_css_maskLinearGradient != css_maskLinearGradient) {
        _css_maskLinearGradient = css_maskLinearGradient;
        if (_css_maskLinearGradient) {
            self.layer.mask = nil;
        }
        [self p_syncMaskLinearGradientIfNeed];
    }

}

- (BOOL)setImageWithUrl:(NSString *)url {
    BOOL handled = false;
    
    if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_setImageWithUrl:forImageView:placeholderImage:options:complete:)]) {
        self.kr_reuseDisable = YES;     // 先关闭ImageView的复用能力
        self.imageLoadingCount++;
        __weak typeof(self) wself = self;
        handled = [[KuiklyRenderBridge componentExpandHandler] hr_setImageWithUrl:url
                                                                     forImageView:self
                                                                 placeholderImage:nil
                                                                          options:1 << 10
                                                                         complete:^(UIImage * _Nullable image, NSError * _Nullable error, NSURL * _Nullable imageURL) {
            [KuiklyRenderThreadManager performOnMainQueueWithTask:^{
                __strong typeof(wself) sself = wself;
                if (!sself) {
                    return;
                }
                // 回调处理
                if ([sself p_handleImageLoadCompletion:error url:sself.css_src imageURL:imageURL] && image) {
                    sself.image = image;
                }
                [sself p_decrementImageLoadingCount];       // 图片加载完成，不管是否成功，更新图片加载计数，判断是否开放复用能力
            } sync:NO];
        }];
    }
    else if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_setImageWithUrl:forImageView:complete:)]) {
        __weak typeof(self) wself = self;
        handled = [[KuiklyRenderBridge componentExpandHandler] hr_setImageWithUrl:url
                                                                     forImageView:self
                                                                         complete:^(UIImage * _Nullable image, NSError * _Nullable error, NSURL * _Nullable imageURL) {
            [KuiklyRenderThreadManager performOnMainQueueWithTask:^{
                __strong typeof(wself) sself = wself;
                if (!sself) {
                    return;
                }
                // 回调处理
                [sself p_handleImageLoadCompletion:error url:sself.css_src imageURL:imageURL];
            } sync:NO];
        }];
    } else if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_setImageWithUrl:forImageView:)]) {
        handled = [[KuiklyRenderBridge componentExpandHandler] hr_setImageWithUrl:url forImageView:self];
    } else {
        NSAssert(0, @"should expand hr_setImageWithUrl:forImageView:");
    }
    return handled;
}


- (void)setCss_resize:(NSString *)css_resize {
    if (self.css_resize != css_resize) {
        _css_resize = css_resize;
        self.contentMode = [KRConvertUtil UIViewContentMode:css_resize];
    }
}


- (void)setCss_capInsets:(NSString*)insets{
    if (_css_capInsets != insets) {
        _css_capInsets = insets;
        if (_originImage == nil) {
            [self setImageWithSrc:self.css_src];
        } else {
            [self p_updateWithImage:_originImage];
        }
    }
}

- (void)setCss_loadSuccess:(KuiklyRenderCallback)css_loadSuccess {
    if (_css_loadSuccess != css_loadSuccess) {
        _css_loadSuccess = css_loadSuccess;
        if (css_loadSuccess && self.image) {
            [self p_fireLoadSuccessEventWithImage:self.image];
        }
    }
}

- (void)setCss_loadResolution:(KuiklyRenderCallback)css_loadResolution {
    if (_css_loadResolution != css_loadResolution) {
        _css_loadResolution = css_loadResolution;
        if (css_loadResolution && self.image) {
            [self p_fireLoadResolutionEventWithImage:self.image];
        }
    }
}

- (void)setCss_loadFailure:(KuiklyRenderCallback)css_loadFailure {
    if (_css_loadFailure != css_loadFailure) {
        _css_loadFailure = css_loadFailure;
    }
    if (self.pendingLoadFailure) {
        [self p_fireLoadFailureEventWithErrorCode:self.errorCode];
        self.pendingLoadFailure = false;
    }
}

#pragma mark - override

- (void)setImage:(UIImage *)image {
    if (self.css_src && image == nil) {
        return ;
    }
    [self bindImageToView:image];
}

- (void)bindImageToView:(UIImage *)image {
    _originImage = image;
    [self p_updateWithImage:image];
}

- (void)superSetImage:(UIImage *)image {
    [super setImage:image];
    [self p_syncMaskLinearGradientIfNeed];
    if (image) {
        [self p_fireLoadSuccessEventWithImage:image];
        [self p_fireLoadResolutionEventWithImage:image];
    }
}

- (void)layoutSubviews {
    [super layoutSubviews];
    [self p_syncMaskLinearGradientIfNeed];
}

#pragma mark - private

- (void)p_setBase64Image:(NSString *)base64Str {
    __weak typeof(&*self) weakSelf = self;
    KuiklyRenderView *rootView =  self.hr_rootView;
    KRMemoryCacheModule *module = (KRMemoryCacheModule *)[rootView moduleWithName:NSStringFromClass([KRMemoryCacheModule class])];
    if (!module) {
        return;
    }
    NSString *md5Key = base64Str;
    base64Str = [module memoryObjectForKey:md5Key];
    if ([base64Str isKindOfClass:[UIImage class]]) {
        weakSelf.image = (UIImage *)base64Str;
        return ;
    }
    NSAssert(base64Str, @"base64Str is nil");
    [rootView performWhenViewDidLoadWithTask:^{
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            NSRange range = [base64Str rangeOfString:@";base64,"];
            if (range.length) {
                NSString * base64 = [base64Str substringFromIndex:NSMaxRange(range)];
                NSData * imageData =[[NSData alloc] initWithBase64EncodedString:base64 options:NSDataBase64DecodingIgnoreUnknownCharacters];
                UIImage *image = [UIImage imageWithData:imageData];
                dispatch_async(dispatch_get_main_queue(), ^{
                    [module setMemoryObjectWithKey:md5Key value:image];
                    if (weakSelf.css_src == md5Key) {
                        weakSelf.image = image;
                    }
                });
            }
        });
    }];
    

}

// 同步渐变遮罩
- (void)p_syncMaskLinearGradientIfNeed {
    if (CGSizeEqualToSize(self.frame.size, CGSizeZero)) {
        return ;
    }
    if (self.image && _css_maskLinearGradient.length) {
        if (!self.layer.mask) {
            CSSGradientLayer *maskLayer = [[CSSGradientLayer alloc] initWithLayer:nil cssGradient:_css_maskLinearGradient];
            self.layer.mask = maskLayer;
        }
        self.layer.mask.frame = self.bounds;
        [self.layer.mask layoutSublayers];
    } else {
        if (self.layer.mask) {
            self.layer.mask = nil;
        }
    }
}


-(void)p_fireLoadSuccessEventWithImage:(UIImage *)image {
    if (_css_loadSuccess) {
        _css_loadSuccess(@{ @"src" : self.css_src ?: @"" });
    }
}


-(void)p_fireLoadResolutionEventWithImage:(UIImage *)image {
    if (_css_loadResolution) {
        _css_loadResolution(@{ @"imageWidth" : @(image.size.width * image.scale),
                               @"imageHeight" : @(image.size.height * image.scale)
                            });
    }
}

-(void)p_fireLoadFailureEventWithErrorCode:(NSInteger)errorCode {
    if (_css_loadFailure) {
        NSDictionary *params = @{
            @"src": self.css_src ?: @"",
            @"errorCode": @(errorCode)
        };
        _css_loadFailure(params);
    }
}

- (void)p_updateWithImage:(UIImage *)image {
    if(self.css_capInsets != nil && image){
        NSArray* items = [self.css_capInsets componentsSeparatedByString:@" "];
        if(items.count >= 4){
            CGFloat top = [items[0] floatValue];
            CGFloat left = [items[1] floatValue];
            CGFloat bottom = [items[2] floatValue];
            CGFloat right = [items[3] floatValue];
            
            if(top > 0 || left > 0 || bottom > 0 || right >0){
                UIEdgeInsets insets = UIEdgeInsetsMake(top, left, bottom, right);
#if !TARGET_OS_OSX // [macOS]
                image = [image resizableImageWithCapInsets:insets resizingMode:(UIImageResizingModeStretch)];
#else // [macOS
                image = UIImageResizableImageWithCapInsets(image, insets, UIImageResizingModeStretch);
#endif // macOS]
            }
        }
    }else if ([self.css_dotNineImage boolValue] && image) {
        CGFloat imageWidth = image.size.width;
        CGFloat imageHeight = image.size.height;
        UIEdgeInsets insets = UIEdgeInsetsMake(imageHeight * 0.5, imageWidth * 0.5,
                                               imageHeight * 0.5 - 1, imageWidth * 0.5 - 1);
#if !TARGET_OS_OSX // [macOS]
        image = [image resizableImageWithCapInsets:insets resizingMode:(UIImageResizingModeStretch)];
#else // [macOS
        image = UIImageResizableImageWithCapInsets(image, insets, UIImageResizingModeStretch);
#endif // macOS]
    }
    if (image && [self.css_tintColor length]) {
        UIColor *tintColor = [UIView css_color:self.css_tintColor];
        UIImage *tintedImage = [image kr_tintedImageWithColor:tintColor];
        [self superSetImage:tintedImage];
    } else if (image && [self.css_colorFilter length]) { // 颜色滤镜
        NSString *cssColorFilter = self.css_colorFilter;
        NSString *src = [self.css_src copy];
        KR_WEAK_SELF
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            UIImage *colorFilterImage = [image kr_applyColorFilterWithColorMatrix:cssColorFilter];
            dispatch_async(dispatch_get_main_queue(), ^{
                if ([weakSelf.css_src isEqualToString:src] && weakSelf.css_colorFilter == cssColorFilter) {
                    [weakSelf superSetImage:colorFilterImage];
                }
            });
        });
    } else if (image && [self.css_blurRadius floatValue]) {
        CGFloat blurRadius = [self.css_blurRadius floatValue];
        NSString *src = [self.css_src copy];
        KR_WEAK_SELF
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            UIImage *blurImage = [image kr_blurBlurRadius:blurRadius];;
            dispatch_async(dispatch_get_main_queue(), ^{
                if ([weakSelf.css_src isEqualToString:src] && [weakSelf.css_blurRadius floatValue] == blurRadius) {
                    [weakSelf superSetImage:blurImage];
                }
            });
        });
    }  else {
        [self superSetImage:image];
    }
}

// 图片加载结果回调处理
- (BOOL)p_handleImageLoadCompletion:(NSError *)error url:(NSString *)url imageURL:(NSURL *)imageURL {
    // src一致性判断
    if (![self p_srcMatch:url imageURL:imageURL]) {
        return NO;
    }
    // 错误处理
    if (error) {
        if (self.css_loadFailure) {
            [self p_fireLoadFailureEventWithErrorCode:error.code];
        } else {
            self.pendingLoadFailure = true;
            self.errorCode = error.code;
        }
        return NO;
    }
    // 加载成功，允许设置图片
    return YES;
}

// 图片 src 一致性判断
- (BOOL)p_srcMatch:(NSString *)src imageURL:(NSURL *)imageURL {
    if (!src.length || !imageURL) {
        return NO;
    }
       
    NSString *url = imageURL.absoluteString;
    if (!url.length) {
        return NO;
    }
        
    // 网络URL 走完全匹配
    if ([url isEqualToString:src]) {
        return YES;
    }
        
    // 本地资源 取src和url 最后一个"/"之后的内容
    NSString *srcFileName = [self p_fileNameFromPath:src];
    NSString *urlFileName = [self p_fileNameFromPath:url];
    return srcFileName.length && urlFileName.length && [srcFileName isEqualToString:urlFileName];
}

// 提取src路径中的文件名
- (NSString *)p_fileNameFromPath:(NSString *)path {
    if (!path.length) {
        return @"";
    }
        
    // 使用系统 API 获取文件名
    NSString *fileName = path.lastPathComponent;
    return fileName.length > 0 ? fileName : @"";
}

// 新增方法
- (void)p_decrementImageLoadingCount {
    if (self.imageLoadingCount > 0) {
        self.imageLoadingCount--;
    }
    if (self.imageLoadingCount == 0) {
        self.kr_reuseDisable = NO;
    }
}


- (void)dealloc {
    
}




@end


/** KRWrapperImageView **/

@interface KRWrapperImageView : UIView<KuiklyRenderViewExportProtocol>
/// 占位图属性
@property (nonatomic, strong) NSString *css_placeholder;

@end

@implementation KRWrapperImageView {
    KRImageView *_placeholderView;
    KRImageView *_imageView;
}

#pragma mark - init

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _imageView = [[KRImageView alloc] initWithFrame:frame];
        [self addSubview:_imageView];
    }
    return self;
}

- (void)setHr_rootView:(KuiklyRenderView *)hr_rootView {
    _imageView.hr_rootView = hr_rootView;
    _placeholderView.hr_rootView = hr_rootView;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    KUIKLY_SET_CSS_COMMON_PROP
    [_imageView hrv_setPropWithKey:propKey propValue:propValue];
    _placeholderView.contentMode = _imageView.contentMode;
}


- (void)hrv_prepareForeReuse {
    KUIKLY_RESET_CSS_COMMON_PROP
    [_imageView hrv_prepareForeReuse];
}


/*
 * 调用view方法
 */
- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    [_imageView hrv_callWithMethod:method params:params callback:callback];
}

#pragma mark - override

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    _imageView.frame = self.bounds;
    _placeholderView.frame = self.bounds;
}

#pragma mark - setter

- (void)setCss_placeholder:(NSString *)css_placeholder {
    if (_css_placeholder != css_placeholder) {
        _css_placeholder = css_placeholder;
        [_placeholderView removeFromSuperview];
        if (_css_placeholder.length) {
            _placeholderView = [[KRImageView alloc] initWithFrame:self.bounds];
            _placeholderView.contentMode = _imageView.contentMode;
            _placeholderView.hr_rootView = _imageView.hr_rootView;
            _placeholderView.css_src = css_placeholder;
            [self insertSubview:_placeholderView atIndex:0];
        } else {
            _placeholderView = nil;
        }
    }
}

- (void)dealloc {
    
}

@end

// ***** KRImageRefreshCache ****** /

@implementation KRImageRefreshCache {
    NSMutableDictionary *_imageCache;
}

+ (instancetype)sharedInstance {
    static KRImageRefreshCache *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[self alloc] init];
    });
    return sharedInstance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _imageCache = [NSMutableDictionary new];
    }
    return self;
}

- (void)cacheWithKey:(NSString *)key image:(UIImage *)image {
    if (key && image) {
        [_imageCache setObject:image forKey:key];
        // 等2s后释放
        NSUInteger flag = ++self.delayBatchFlag;
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            if (flag == self.delayBatchFlag) {
                [[[self class] sharedInstance] removeAllCache];
            }
        });
    }
}

- (UIImage *_Nullable)imageWithKey:(NSString *)key {
    if (key) {
        return _imageCache[key];
    }
    return nil;
}

- (void)removeCacheWithKey:(NSString *)key {
    if (key) {
        [_imageCache removeObjectForKey:key];
    }
}

- (void)removeAllCache {
    [_imageCache removeAllObjects];
}




@end

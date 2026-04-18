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

#import "KRView.h"
#import "KRView+LiquidGlass.h"
#import "KRConvertUtil.h"
#import "KRComponentDefine.h"
#import "KuiklyRenderView.h"
#import "KRDisplayLink.h"
#import "KRView+Compose.h"
#import "NSObject+KR.h"
#import "KRMemoryCacheModule.h"

/// 层级置顶方法
#define CSS_METHOD_BRING_TO_FRONT @"bringToFront"
/// 无障碍聚焦
#define CSS_METHOD_ACCESSIBILITY_FOCUS @"accessibilityFocus"
/// 无障碍朗读语音
#define CSS_METHOD_ACCESSIBILITY_ANNOUNCE @"accessibilityAnnounce"
/// view 截图
#define CSS_METHOD_TOIMAGE @"toImage"


#pragma mark - KRView Private Interface

@interface KRView()
/**禁止屏幕刷新帧事件**/
@property (nonatomic, strong) NSNumber *KUIKLY_PROP(screenFramePause);
/**屏幕刷新帧事件(VSYNC信号)**/
@property (nonatomic, strong) KuiklyRenderCallback KUIKLY_PROP(screenFrame);

@end


#pragma mark - KRView Implementation

/*
 * @brief 暴露给Kotlin侧调用的View容器组件
 */
@implementation KRView {
    /// 正在调用HitTest方法
    BOOL _hitTesting;
    /// 屏幕刷新定时器
    KRDisplayLink *_displaylink;
}

@synthesize hr_rootView;

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)hrv_prepareForeReuse {
    KUIKLY_RESET_CSS_COMMON_PROP;
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    if ([method isEqualToString:CSS_METHOD_BRING_TO_FRONT]) {
        [self.superview bringSubviewToFront:self];
    } else if ([method isEqualToString:CSS_METHOD_ACCESSIBILITY_FOCUS]) {
        // 设置无障碍焦点到当前视图
        UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, self);
    } else if ([method isEqualToString:CSS_METHOD_ACCESSIBILITY_ANNOUNCE]) {
        // 朗读指定的文本内容
        if (params && params.length > 0) {
            UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, params);
        }
    } else if ([method isEqualToString:CSS_METHOD_TOIMAGE]) {
        [self kr_toImageWithParams:params callback:callback];
    }
}

#pragma mark - CSS Property

- (void)setCss_screenFramePause:(NSNumber *)css_screenFramePause {
    if (_css_screenFramePause != css_screenFramePause) {
        _css_screenFramePause = css_screenFramePause;
        [_displaylink pause:[css_screenFramePause boolValue]];
    }
}

- (void)setCss_screenFrame:(KuiklyRenderCallback)css_screenFrame {
    if (_css_screenFrame != css_screenFrame) {
        _css_screenFrame = css_screenFrame;
        [_displaylink stop];
        _displaylink = nil;
        if (_css_screenFrame) {
            _displaylink = [[KRDisplayLink alloc] init];
            [_displaylink startWithCallback:^(CFTimeInterval timestamp) {
                if (css_screenFrame) {
                    css_screenFrame(nil);
                }
            }];
        }
    }
}

#pragma mark - Override - Base Touch

#if !TARGET_OS_OSX // [macOS]

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesBegan:touches withEvent:event];
    // 如果走compose(superTouch)，由手势驱动，不由touch驱动事件
    if (_css_touchDown && ![self.css_superTouch boolValue]) {
        _css_touchDown([self p_generateBaseParamsWithEvent:event eventName:@"touchDown"]);
    }
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesEnded:touches withEvent:event];
    if (_css_touchUp && ![self.css_superTouch boolValue]) {
        _css_touchUp([self p_generateBaseParamsWithEvent:event eventName:@"touchUp"]);
    }
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesMoved:touches withEvent:event];
    if (_css_touchMove && ![self.css_superTouch boolValue]) {
        _css_touchMove([self p_generateBaseParamsWithEvent:event eventName:@"touchMove"]);
    }
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesCancelled:touches withEvent:event];
    if (_css_touchUp && ![self.css_superTouch boolValue]) {
        _css_touchUp([self p_generateBaseParamsWithEvent:event eventName:@"touchCancel"]);
    }
}

#else

- (void)touchesBeganWithEvent:(NSEvent *)event {
    [super touchesBeganWithEvent:event];
    // 如果走compose(superTouch)，由手势驱动，不由touch驱动事件
    if (_css_touchDown && ![self.css_superTouch boolValue]) {
        _css_touchDown([self p_generateBaseParamsWithEvent:event eventName:@"touchDown"]);
    }
}

- (void)touchesEndedWithEvent:(UIEvent *)event {
    [super touchesEndedWithEvent:event];
    if (_css_touchUp && ![self.css_superTouch boolValue]) {
        _css_touchUp([self p_generateBaseParamsWithEvent:event eventName:@"touchUp"]);
    }
}

- (void)touchesMovedWithEvent:(UIEvent *)event {
    [super touchesMovedWithEvent:event];
    if (_css_touchMove && ![self.css_superTouch boolValue]) {
        _css_touchMove([self p_generateBaseParamsWithEvent:event eventName:@"touchMove"]);
    }
}

- (void)touchesCancelledWithEvent:(UIEvent *)event {
    [super touchesCancelledWithEvent:event];
    if (_css_touchUp && ![self.css_superTouch boolValue]) {
        _css_touchUp([self p_generateBaseParamsWithEvent:event eventName:@"touchCancel"]);
    }
}

#endif // [macOS]

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    if ([self p_hasZIndexInSubviews]) {
        _hitTesting = YES;
    }
    
    // 检查触摸点是否命中了正在执行动画的视图（presentationLayer 位置）
    BOOL hitAnimating = NO;
    if (self.superview) {
        CALayer *presentationLayer = self.layer.presentationLayer;
        CALayer *modelLayer = self.layer.modelLayer;
        if (presentationLayer && !CGRectEqualToRect(presentationLayer.frame, modelLayer.frame)) {
            // 将触摸点从 modelLayer 坐标系转换到 presentationLayer 坐标系，再进行命中测试
            CGPoint pointInPresentation = [presentationLayer convertPoint:point fromLayer:self.layer];
            hitAnimating = [presentationLayer containsPoint:pointInPresentation];
        }
    }
    
    UIView *hitView = [super hitTest:point withEvent:event];
    _hitTesting = NO;
    
    // 子视图命中，优先返回
    if (hitView && hitView != self) {
        return hitView;
    }
    // 自身命中（正常 hitTest 或动画位置补偿），统一走穿透判断
    if (hitView == self || hitAnimating) {
        // 对齐安卓事件机制，无手势事件监听则将手势穿透
        if (!(self.gestureRecognizers.count > 0 || _css_touchUp || _css_touchMove || _css_touchDown)) {
            return nil;
        }
        return self;
    }
    return hitView;
}

- (NSArray<__kindof UIView *> *)subviews {
    NSArray<__kindof UIView *> *views = [super subviews];
    if (views.count && _hitTesting) { // 根据zIndex排序，解决zIndex手势响应问题
        views = [[views copy] sortedArrayUsingComparator:^NSComparisonResult(UIView *  _Nonnull obj1, UIView *  _Nonnull obj2) {
            if (obj1.css_zIndex.intValue < obj2.css_zIndex.intValue) {
                return NSOrderedAscending;
            } else if (obj1.css_zIndex.intValue > obj2.css_zIndex.intValue) {
                return NSOrderedDescending;
            } else {
                NSUInteger index1 = [views indexOfObject:obj1];
                NSUInteger index2 = [views indexOfObject:obj2];
                if (index1 < index2) {
                    return NSOrderedAscending;
                } else if (index1 > index2) {
                    return NSOrderedDescending;
                } else {
                    return NSOrderedSame;
                }
            }
        }];
    }
    return views;
}

#pragma mark - CSS Property Override for Liquid Glass Support

- (void)setCss_borderRadius:(NSString *)css_borderRadius {
    [super setCss_borderRadius:css_borderRadius];
    
    // Sync corner radius to liquid glass effect view
    [self updateEffectViewCornerRadius];
}

- (void)setCss_frame:(NSValue *)css_frame {
    [super setCss_frame:css_frame];
    
    // Sync frame to liquid glass effect view
    [self updateEffectViewFrame];
}

#pragma mark - Private Methods

- (NSDictionary *)p_generateBaseParamsWithEvent:(UIEvent *)event eventName:(NSString *)eventName {
#if !TARGET_OS_OSX // [macOS]
    NSSet<UITouch *> *touches = [event allTouches];
    NSMutableArray *touchesParam = [NSMutableArray new];
    [touches enumerateObjectsUsingBlock:^(UITouch * _Nonnull touchObj, BOOL * _Nonnull stop) {
        [touchesParam addObject:[self p_generateTouchParamWithTouch:touchObj]];
    }];
    __block NSMutableDictionary *result = [([touchesParam firstObject] ?: @{}) mutableCopy];
#else // [macOS
    // macOS uses NSEvent (single mouse event) instead of multi-touch UITouch
    NSDictionary *touchParam = [self p_generateTouchParamWithTouch:event];
    NSArray *touchesParam = @[touchParam];
    NSMutableDictionary *result = [touchParam mutableCopy];
#endif // macOS]
    result[@"touches"] = touchesParam;
    result[@"action"] = eventName;
    result[@"timestamp"] = @(event.timestamp);
    return result;
}

- (NSDictionary *)p_generateTouchParamWithTouch:(UITouch *)touch {
#if !TARGET_OS_OSX // [macOS]
    CGPoint locationInSelf = [touch locationInView:self];
    CGPoint locationInRootView = [touch locationInView:self.hr_rootView];
#else // [macOS
    // Convert window coordinates to view-local coordinates
    CGPoint locationInWindow = [touch locationInWindow];
    CGPoint locationInSelf = [self convertPoint:locationInWindow fromView:nil];
    CGPoint locationInRootView = self.hr_rootView ? [self.hr_rootView convertPoint:locationInWindow fromView:nil] : locationInWindow;
#endif // macOS]
#if TARGET_OS_OSX // [macOS]
    // macOS 上鼠标永远是单点操作，使用固定 pointerId 确保
    // mouseDown/mouseDragged/mouseUp 序列中 pointerId 一致。
    static const NSUInteger kMacMousePointerId = 0;
    return @{
        @"x" : @(locationInSelf.x),
        @"y" : @(locationInSelf.y),
        @"pageX" : @(locationInRootView.x),
        @"pageY" : @(locationInRootView.y),
        @"hash"  : @(kMacMousePointerId),
        @"pointerId" : @(kMacMousePointerId),
    };
#else
    return @{
        @"x" : @(locationInSelf.x),
        @"y" : @(locationInSelf.y),
        @"pageX" : @(locationInRootView.x),
        @"pageY" : @(locationInRootView.y),
        @"hash"  : @(touch.hash),
        @"pointerId" : [NSNumber numberWithUnsignedLong:touch.hash],
    };
#endif // macOS]
}


- (BOOL)p_hasZIndexInSubviews {
    for (UIView *subView in self.subviews) {
        if (subView.css_zIndex) {
            return YES;
        }
    }
    return NO;
}

#pragma mark - ToImage

- (void)kr_toImageWithParams:(NSString *)params callback:(KuiklyRenderCallback)callback {
    if (!callback) {
        return;
    }
    // 主线程保护
    if (![NSThread isMainThread]) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self kr_toImageWithParams:params callback:callback];
        });
        return;
    }
    
    NSDictionary *paramsDict = [params hr_stringToDictionary];
    NSString *type = paramsDict[@"type"];
    if (!type || type.length == 0) {
        callback(@{
            @"code": @(-1),
            @"message": @"type is required"
        });
        return;
    }
    NSInteger sampleSize = MAX(1, [paramsDict[@"sampleSize"] integerValue]);
    
    CGRect bounds = self.bounds;
    if (CGRectIsEmpty(bounds) || bounds.size.width <= 0 || bounds.size.height <= 0) {
        callback(@{
            @"code": @(-1),
            @"message": @"snapshot failed: view bounds is empty"
        });
        return;
    }
    
    // 截图（主线程同步完成）
#if TARGET_OS_OSX // [macOS]
    CGFloat screenScale = [NSScreen mainScreen].backingScaleFactor ?: 1.0;
#else
    CGFloat screenScale = [UIScreen mainScreen].scale;
#endif // [macOS]
    CGFloat scale = MAX(screenScale / (CGFloat)sampleSize, 0.01);
    UIImage *image = [self kr_safeSnapshotWithLayer:self.layer bounds:bounds scale:scale];
    
    if (!image || image.size.width == 0) {
        callback(@{
            @"code": @(-1),
            @"message": @"snapshot failed: image is nil or empty"
        });
        return;
    }
    
    if ([type isEqualToString:@"dataUri"]) {
        // dataUri 模式：base64 编码，采用异步写入
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            NSData *pngData = UIImagePNGRepresentation(image);
            if (!pngData) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    callback(@{
                        @"code": @(-1),
                        @"message": @"snapshot failed: PNG encoding failed"
                    });
                });
                return;
            }
            NSString *base64 = [pngData base64EncodedStringWithOptions:0];
            NSString *dataUri = [NSString stringWithFormat:@"data:image/png;base64,%@", base64 ?: @""];
            dispatch_async(dispatch_get_main_queue(), ^{
                callback(@{
                    @"code": @(0),
                    @"data": dataUri
                });
            });
        });
    } else if ([type isEqualToString:@"file"]) {
        // file 模式：磁盘存储，采用异步写入
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            NSString *filePath = [KRView kr_saveSnapshotToTempFile:image];
            dispatch_async(dispatch_get_main_queue(), ^{
                if (filePath) {
                    callback(@{
                        @"code": @(0),
                        @"data": [NSString stringWithFormat:@"file://%@", filePath]
                    });
                } else {
                    callback(@{
                        @"code": @(-1),
                        @"message": @"failed to save snapshot to file"
                    });
                }
            });
        });
    } else if ([type isEqualToString:@"cacheKey"]) {
        // cacheKey 模式：内存缓存，主线程同步执行，存入 KRMemoryCacheModule
        KuiklyRenderView *rootView = self.hr_rootView;
        if (!rootView) {
            callback(@{
                @"code": @(-1),
                @"message": @"snapshot failed: rootView is nil"
            });
            return;
        }
        KRMemoryCacheModule *module = (KRMemoryCacheModule *)[rootView moduleWithName:NSStringFromClass([KRMemoryCacheModule class])];
        if (!module) {
            callback(@{
                @"code": @(-1),
                @"message": @"snapshot failed: KRMemoryCacheModule not found"
            });
            return;
        }
        
        NSString *cacheKey = [NSString stringWithFormat:@"data:image_Md5_snapshot_%llu_%u",
            (unsigned long long)(CFAbsoluteTimeGetCurrent() * 1000),
            arc4random_uniform(0xFFFFFF)];
        
        [module setMemoryObjectWithKey:cacheKey value:image];
        
        callback(@{
            @"code": @(0),
            @"data": cacheKey
        });
        
    } else {
        callback(@{
            @"code": @(-1),
            @"message": [NSString stringWithFormat:@"unsupported type: %@", type]
        });
    }
}

- (UIImage *)kr_safeSnapshotWithLayer:(CALayer *)layer bounds:(CGRect)bounds scale:(CGFloat)scale {
    @autoreleasepool {
#if TARGET_OS_OSX // [macOS]
        // macOS: 使用 CGBitmapContext 同步渲染，避免 NSImage drawingHandler 延迟绘制导致子线程 crash
        size_t width = (size_t)ceil(bounds.size.width * scale);
        size_t height = (size_t)ceil(bounds.size.height * scale);
        if (width == 0 || height == 0) {
            return nil;
        }
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        CGContextRef ctx = CGBitmapContextCreate(NULL, width, height, 8, width * 4, colorSpace,
                                                  kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host);
        CGColorSpaceRelease(colorSpace);
        if (!ctx) {
            return nil;
        }
        // CGBitmapContext 坐标系原点在左下角，CALayer 绘制基于左上角，需翻转 y 轴
        CGContextTranslateCTM(ctx, 0, (CGFloat)height);
        CGContextScaleCTM(ctx, scale, -scale);
        [layer renderInContext:ctx];
        CGImageRef cgImage = CGBitmapContextCreateImage(ctx);
        CGContextRelease(ctx);
        if (!cgImage) {
            return nil;
        }
        NSImage *image = [[NSImage alloc] initWithCGImage:cgImage size:bounds.size];
        CGImageRelease(cgImage);
        return image;
#else
        UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat];
        format.scale = scale;
        format.opaque = layer.isOpaque;
        UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:bounds.size format:format];
        return [renderer imageWithActions:^(UIGraphicsImageRendererContext *rendererContext) {
            [layer renderInContext:rendererContext.CGContext];
        }];
#endif // [macOS]
    }
}

+ (NSString *)kr_saveSnapshotToTempFile:(UIImage *)image {
    if (!image) {
        return nil;
    }
    @try {
        NSString *fileName = [NSString stringWithFormat:@"kr_snapshot_%llu_%u.png",
            (unsigned long long)(CFAbsoluteTimeGetCurrent() * 1000),
            arc4random_uniform(0xFFFFFF)];
        NSString *filePath = [NSTemporaryDirectory() stringByAppendingPathComponent:fileName];
        NSData *pngData = UIImagePNGRepresentation(image);
        if (!pngData || pngData.length == 0) {
            return nil;
        }
        BOOL success = [pngData writeToFile:filePath atomically:YES];
        return success ? filePath : nil;
    } @catch (NSException *exception) {
        return nil;
    }
}

#pragma mark - Dealloc

- (void)dealloc {
    if (self.css_screenFrame) {
        self.css_screenFrame = nil;
    }
}

@end



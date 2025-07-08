/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#import <CoreText/CoreText.h>

#import "KRCanvasView.h"
#import "KRComponentDefine.h"
#import "KRConvertUtil.h"
#import "NSObject+KR.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"
#import "KRRichTextView.h"
#import "KRMemoryCacheModule.h"

#define HOT_HEAP_GRIDENT_WIDTH 100

typedef void (^KRPathRenderAction)(CGContextRef context, CGMutablePathRef path);



@interface KRCanvasView()


@property (nonatomic, strong) NSMutableArray<KRPathRenderAction> *renderActions;
@property (nonatomic, assign) CGMutablePathRef path;
@property (nonatomic, strong) NSString *fillStyle;
@property (nonatomic, strong) NSString *strokeStyle;
@property (nonatomic, assign) CGFloat lineWidth;


@property (nonatomic, strong) NSString *fontStyle;
@property (nonatomic, strong) NSString *fontWeight;
@property (nonatomic, strong) NSNumber *fontSize;
@property (nonatomic, strong) NSNumber *fontFamily;
@property (nonatomic, strong) NSString *textAlign;
@property (nonatomic, strong) NSMutableArray<NSString *> *saveStack;
@end

@implementation KRCanvasView

@synthesize hr_rootView;
- (instancetype)initWithFrame:(CGRect)frame {
    if ([super initWithFrame:frame]) {
        self.backgroundColor = [UIColor clearColor];
        _path = CGPathCreateMutable();
        _saveStack = [NSMutableArray array];
    }
    return self;
}


- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}


- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)addRenderAction:(KRPathRenderAction)action {
    [self.renderActions addObject:action];
    [self setNeedsDisplay];
}

#pragma mark - KRCanvasLayerDelegate

- (void)layerDidDisplay {
  // nothing to do
}

#pragma mark - css method


- (void)css_reset:(NSDictionary *)args {
    self.renderActions = nil;
    if (_path) {
        CGPathRelease(_path);
    }
    _path = CGPathCreateMutable();
    [self.saveStack removeAllObjects];
    self.fillStyle = nil;
    self.strokeStyle = nil;
    self.lineWidth = 0;
    [self setNeedsDisplay];
}

- (void)css_beginPath:(NSDictionary *)args {
    KR_WEAK_SELF;
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        CGPathRelease(path);
        weakSelf.path = CGPathCreateMutable();
        CGContextBeginPath(context);
    }];
}

- (void)css_moveTo:(NSDictionary *)args {
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
        CGFloat x = [params[@"x"] doubleValue];
        CGFloat y = [params[@"y"] doubleValue];
        CGPathMoveToPoint(path, NULL, x, y);
    }];
}

- (void)css_lineTo:(NSDictionary *)args {
    KR_WEAK_SELF
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        [weakSelf initDefaultInitPointIfNeed:context path:path];
        NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
        CGFloat x = [params[@"x"] doubleValue];
        CGFloat y = [params[@"y"] doubleValue];
        CGPathAddLineToPoint(path, NULL, x, y);
    }];
}

- (void)css_arc:(NSDictionary *)args {
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
        CGFloat centerX = [params[@"x"] doubleValue];
        CGFloat centerY = [params[@"y"] doubleValue];
        CGFloat radius = [params[@"r"] doubleValue];
        CGFloat startAngle = [params[@"sAngle"] doubleValue];
        CGFloat endAngle = [params[@"eAngle"] doubleValue];
        BOOL counterclockwise = [params[@"counterclockwise"] boolValue];
        CGPathAddArc(path, NULL, centerX, centerY, radius, startAngle, endAngle, counterclockwise);
    }];
}

- (void)css_closePath:(NSDictionary *)args {
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        CGPathCloseSubpath(path);
    }];
   
}

- (void)css_stroke:(NSDictionary *)args {
    KR_WEAK_SELF
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
           CGContextAddPath(context, path);
           CGContextSetLineWidth(context, weakSelf.lineWidth);
           [weakSelf applyStrokeStyle:context path:path];
           CGContextDrawPath(context, kCGPathStroke);
           CGContextAddPath(context, path);
    }];
}

- (void)css_fill:(NSDictionary *)args {
    KR_WEAK_SELF
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
           CGContextAddPath(context, path);
           [weakSelf applyFillStyle:context path:path];
           CGContextDrawPath(context, kCGPathFill);
           CGContextAddPath(context, path);
    }];
}

- (void)css_textAlign:(NSDictionary*)args{
    NSString *params = args[KRC_PARAM_KEY];
    KR_WEAK_SELF
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        KR_STRONG_SELF_RETURN_IF_NIL
        strongSelf.textAlign = params;
    }];
}

- (void)css_font:(NSDictionary*)args{
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    KR_WEAK_SELF
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        KR_STRONG_SELF_RETURN_IF_NIL
        strongSelf.fontFamily = params[@"family"];
        strongSelf.fontSize = params[@"size"];
        strongSelf.fontStyle = params[@"style"];
        strongSelf.fontWeight = params[@"weight"];
    }];
}

- (void)drawText:(NSDictionary*)params
         context:(CGContextRef)context
            path:(CGMutablePathRef)path isStroke:(bool)isStroke{
    float x = ((NSNumber*)params[@"x"]).floatValue;
    float y = ((NSNumber*)params[@"y"]).floatValue;

    KRRichTextShadow* shadow = [[KRRichTextShadow alloc] init];
    [shadow hrv_setPropWithKey:@"text" propValue:params[@"text"]];
    [shadow hrv_setPropWithKey:@"fontSize" propValue:self.fontSize];
    [shadow hrv_setPropWithKey:@"fontStyle" propValue:self.fontStyle];
    [shadow hrv_setPropWithKey:@"fontWeight" propValue:self.fontWeight];
    [shadow hrv_setPropWithKey:@"fontFamily" propValue:self.fontFamily];
    [shadow hrv_setPropWithKey:@"textAlign" propValue:self.textAlign];
    shadow.strokeAndFill = false;
    
    if(isStroke){
        [shadow hrv_setPropWithKey:@"strokeColor" propValue:self.strokeStyle];
        [shadow hrv_setPropWithKey:@"strokeWidth" propValue:@(self.lineWidth)];
    }else{
        [shadow hrv_setPropWithKey:@"color" propValue:self.fillStyle];
    }
    
    float left;
    float right;
    
    CGSize sz = [shadow hrv_calculateRenderViewSizeWithConstraintSize:CGSizeMake(CGFLOAT_MAX, CGFLOAT_MAX)];
    if([self.textAlign isEqualToString:@"center"]){
        left = sz.width / 2;
        right = sz.width - left;
    }else if([self.textAlign isEqualToString:@"right"]){
        left = sz.width;
        right = 0;
    }else{
        left = 0;
        right = sz.width;
    }
    float descent = (sz.height - self.fontSize.floatValue) / 2;
    //float ascent = sz.height - descent;
    
    x -= left;
    y += descent;
    
    const int extraSpace = 1;
    sz.width += extraSpace;
    sz.height += extraSpace;
    NSAttributedString* attributedString = [shadow buildAttributedString];
    CGContextSaveGState(context);
    CGContextTranslateCTM(context, x, y + extraSpace);
    CGContextScaleCTM(context, 1.0, -1.0);
    CTFramesetterRef frameSetter = CTFramesetterCreateWithAttributedString((__bridge CFAttributedStringRef)attributedString);
    CGMutablePathRef framePath = CGPathCreateMutable();
    CGPathAddRect(framePath, NULL, CGRectMake(0, 0, sz.width, sz.height));
    CTFrameRef frame = CTFramesetterCreateFrame(frameSetter, CFRangeMake(0, attributedString.length), framePath, NULL);
    CTFrameDraw(frame, context);
    CFRelease(frame);
    CFRelease(framePath);
    CFRelease(frameSetter);
    CGContextRestoreGState(context);
}

- (void)css_strokeText:(NSDictionary*)args{
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    KR_WEAK_SELF
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        KR_STRONG_SELF_RETURN_IF_NIL
        [strongSelf drawText:params context:context path:path isStroke:true];
    }];
}

- (void)css_fillText:(NSDictionary*)args{
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    KR_WEAK_SELF
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        KR_STRONG_SELF_RETURN_IF_NIL
        
        [strongSelf drawText:params context:context path:path isStroke:false];
    }];

}

- (void)css_drawImage:(NSDictionary*)args{
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    NSString *imageCacheKey = params[@"cacheKey"];
    float dx = [params[@"dx"] floatValue];
    float dy = [params[@"dy"] floatValue];
    NSNumber* sx = params[@"sx"];
    NSNumber* sy = params[@"sy"];
    NSNumber* sWidth = params[@"sWidth"];
    NSNumber* sHeight = params[@"sHeight"];
    NSNumber* dWidth =  params[@"dWidth"];
    NSNumber* dHeight = params[@"dHeight"];
    
    KuiklyRenderView *rootView =  self.hr_rootView;
    KRMemoryCacheModule *module = [rootView moduleWithName:NSStringFromClass([KRMemoryCacheModule class])];
    UIImage* image = [module imageWithKey:imageCacheKey];
    
    KR_WEAK_SELF
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        KR_STRONG_SELF_RETURN_IF_NIL
        
        UIImage* sourceImage = image;
        if(sx != nil && sy != nil && sWidth != nil && sHeight != nil){
            CGRect sourceRect =  CGRectMake([sx floatValue], [sy floatValue],
                                            [sWidth floatValue], [sHeight floatValue]);
            CGImageRef cgImage = CGImageCreateWithImageInRect(image.CGImage, sourceRect);
            sourceImage = [UIImage imageWithCGImage:cgImage];
        }
        
        CGRect destRect;
        if(dWidth != nil && dHeight != nil){
            destRect = CGRectMake(dx, dy, [dWidth floatValue], [dHeight floatValue]);
        }else{
            CGSize imageSize = image.size;
            destRect = CGRectMake(dx, dy, imageSize.width, imageSize.height);
        }
        
        [sourceImage drawInRect:destRect];
    }];
}

- (void)css_strokeStyle:(NSDictionary *)args {
    KR_WEAK_SELF
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        weakSelf.strokeStyle = params[@"style"];
    }];
}

- (void)css_fillStyle:(NSDictionary *)args {
    KR_WEAK_SELF
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        weakSelf.fillStyle = params[@"style"];
    }];
}

- (void)css_lineWidth:(NSDictionary *)args {
    KR_WEAK_SELF
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    CGFloat width = [params[@"width"] doubleValue];
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        weakSelf.lineWidth = width;
    }];
}

- (void)css_lineCap:(NSDictionary *)args {
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
       NSString *style = params[@"style"];
       CGLineCap lineCap = kCGLineCapButt;
       if ([style isEqualToString:@"round"]) {
           lineCap = kCGLineCapRound;
       } else if ([style isEqualToString:@"butt"]) {
           lineCap = kCGLineCapButt;
       } else if ([style isEqualToString:@"square"]) {
           lineCap = kCGLineCapSquare;
       }
       [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
           CGContextSetLineCap(context, lineCap);
       }];
}

- (void)css_quadraticCurveTo:(NSDictionary *)args {
    KR_WEAK_SELF;
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
       CGFloat cpx = [params[@"cpx"] doubleValue];
       CGFloat cpy = [params[@"cpy"] doubleValue];
       CGFloat x = [params[@"x"] doubleValue];
       CGFloat y = [params[@"y"] doubleValue];
       [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
           [weakSelf initDefaultInitPointIfNeed:context path:path];
           CGPathAddQuadCurveToPoint(path, NULL, cpx, cpy, x, y);
       }];
}

- (void)css_bezierCurveTo:(NSDictionary *)args {
    KR_WEAK_SELF;
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
       CGFloat cp1x = [params[@"cp1x"] doubleValue];
       CGFloat cp1y = [params[@"cp1y"] doubleValue];
       CGFloat cp2x = [params[@"cp2x"] doubleValue];
       CGFloat cp2y = [params[@"cp2y"] doubleValue];
       CGFloat x = [params[@"x"] doubleValue];
       CGFloat y = [params[@"y"] doubleValue];
       [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
           [weakSelf initDefaultInitPointIfNeed:context path:path];
           CGPathAddCurveToPoint(path, NULL, cp1x, cp1y, cp2x, cp2y, x, y);
       }];
}

- (void)css_clip:(NSDictionary *)args {
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    BOOL intersect = [params[@"intersect"] boolValue];
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        CGContextAddPath(context, path);
        if (intersect) {
            CGContextClip(context);
        } else {
            CGContextEOClip(context);
        }
    }];
}

- (void)css_createLinearGradient:(NSDictionary *)args {
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    CGFloat x0 = [params[@"x0"] doubleValue];
    CGFloat y0 = [params[@"y0"] doubleValue];
    CGFloat x1 = [params[@"x1"] doubleValue];
    CGFloat y1 = [params[@"y1"] doubleValue];
    NSString *colorStopStr = params[@"colorStops"] ?: @"";
    NSArray<NSString *>* splits = [colorStopStr componentsSeparatedByString:@","];
    NSMutableArray *colors = [NSMutableArray new];
    NSMutableArray<NSNumber *> *locations = [NSMutableArray new];
    for (int i = 0; i < splits.count; i++) {
        NSString *colorStopStr = splits[i];
        if (!colorStopStr.length) {
            continue;
        }
        NSArray<NSString *> *colorAndStop = [colorStopStr componentsSeparatedByString:@" "];
        UIColor *color = [UIView css_color:(NSString *)colorAndStop.firstObject];
        [colors addObject:(__bridge id)color.CGColor];
        [locations addObject:@([colorAndStop.lastObject floatValue])];
    }
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        CGFloat locationsC[locations.count];
        for (NSInteger i = 0; i < locations.count; i++) {
            locationsC[i] = [locations[i] floatValue];
        }
        CGGradientRef gradient = CGGradientCreateWithColors(colorSpace, (__bridge CFArrayRef)colors, locations.count > 0 ? locationsC : NULL);
        CGContextDrawLinearGradient(context, gradient, CGPointMake(x0, y0), CGPointMake(x1, y1), 0);
        CGGradientRelease(gradient);
        CGColorSpaceRelease(colorSpace);
    }];
}

- (void)css_createRadialGradient:(NSDictionary *)args {
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
        CGFloat x0 = [params[@"x0"] doubleValue];
        CGFloat y0 = [params[@"y0"] doubleValue];
        CGFloat r0 = [params[@"r0"] doubleValue];
        CGFloat x1 = [params[@"x1"] doubleValue];
        CGFloat y1 = [params[@"y1"] doubleValue];
        CGFloat r1 = [params[@"r1"] doubleValue];
        CGFloat globalAlpha = [params[@"alpha"] doubleValue];
        NSString *colorStopStr = params[@"colors"] ?: @"";
        NSArray<NSString *>* splits = [colorStopStr componentsSeparatedByString:@","];
        NSMutableArray *colors = [NSMutableArray new];
        for (int i = 0; i < splits.count; i++) {
            NSString *colorStr = splits[i];
            UIColor *color = [UIView css_color:(NSString *)colorStr];
            [colors addObject:(__bridge id)color.CGColor];
        }
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        CGGradientRef gradient = CGGradientCreateWithColors(colorSpace, (__bridge CFArrayRef)colors, NULL);
        CGContextSetAlpha(context, globalAlpha);
        CGContextDrawRadialGradient(context, gradient, CGPointMake(x0, y0), r0, CGPointMake(x1, y1), r1, 0);
      
        CGGradientRelease(gradient);
        CGColorSpaceRelease(colorSpace);
    }];
    [self.layer setNeedsDisplay];
}

- (void)css_save:(NSDictionary *)args {
    __weak typeof(self) weakSelf = self;
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        __strong typeof(weakSelf) strongSelf = weakSelf;
        if (!strongSelf) return;
        CGContextSaveGState(context);
        [strongSelf.saveStack addObject:@"gstate"];
    }];
}

- (void)css_restore:(NSDictionary *)args {
    __weak typeof(self) weakSelf = self;
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        __strong typeof(weakSelf) strongSelf = weakSelf;
        if (!strongSelf) return;
        NSString *type = strongSelf.saveStack.lastObject;
        if (type) {
            [strongSelf.saveStack removeLastObject];
            if ([type isEqualToString:@"layer"]) {
                CGContextEndTransparencyLayer(context);
                CGContextRestoreGState(context);
            } else {
                CGContextRestoreGState(context);
            }
        }
    }];
}

- (void)css_saveLayer:(NSDictionary *)args {
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    CGFloat x = [params[@"x"] floatValue];
    CGFloat y = [params[@"y"] floatValue];
    CGFloat width = [params[@"width"] floatValue];
    CGFloat height = [params[@"height"] floatValue];
    __weak typeof(self) weakSelf = self;
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        __strong typeof(weakSelf) strongSelf = weakSelf;
        if (!strongSelf) return;
        CGContextSaveGState(context);
        CGContextBeginTransparencyLayer(context, NULL);
        CGContextClipToRect(context, CGRectMake(x, y, width, height));
        [strongSelf.saveStack addObject:@"layer"];
    }];
}

- (void)css_translate:(NSDictionary *)args {
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    CGFloat dx = [params[@"x"] floatValue];
    CGFloat dy = [params[@"y"] floatValue];
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        CGContextTranslateCTM(context, dx, dy);
    }];
}

- (void)css_scale:(NSDictionary *)args {
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    CGFloat sx = [params[@"x"] floatValue];
    CGFloat sy = [params[@"y"] floatValue];
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        CGContextScaleCTM(context, sx, sy);
    }];
}

- (void)css_rotate:(NSDictionary *)args {
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    CGFloat angle = [params[@"angle"] floatValue];
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        CGContextRotateCTM(context, angle);
    }];
}

- (void)css_skew:(NSDictionary *)args {
    NSDictionary *params = [args[KRC_PARAM_KEY] hr_stringToDictionary];
    CGFloat sx = [params[@"x"] floatValue];
    CGFloat sy = [params[@"y"] floatValue];
    [self addRenderAction:^(CGContextRef context, CGMutablePathRef path) {
        CGAffineTransform transform = CGAffineTransformMake(1, sy, sx, 1, 0, 0);
        CGContextConcatCTM(context, transform);
    }];
}

#pragma mark - override

- (void)drawRect:(CGRect)rect {
    [super drawRect:rect];
    CGContextRef context = UIGraphicsGetCurrentContext();
    if (!context) {
        return;
    }
    for (KRPathRenderAction action in self.renderActions) {
        action(context, self.path);
    }
}



- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    [self setNeedsDisplay];
}

#pragma mark - getter


- (NSMutableArray<KRPathRenderAction> *)renderActions {
    if (!_renderActions) {
        _renderActions = [[NSMutableArray alloc] init];
    }
    return _renderActions;
}

// 在ios中，首次绘制图案，默认原点(0, 0)未生效，需要手动设置一次move到该原点，对齐安卓
- (void)initDefaultInitPointIfNeed:(CGContextRef)context path:(CGMutablePathRef) path {
    CGPoint point = CGPathGetCurrentPoint(path);
    if (CGPointEqualToPoint(point, CGPointZero)) {
        CGPathMoveToPoint(path, NULL, 0, 0);
    }
}

- (CGGradientRef)gradientWithColorStopsStr:(NSString *)colorStopStr {
    NSArray<NSString *>* splits = [colorStopStr componentsSeparatedByString:@","];
    NSMutableArray *colors = [NSMutableArray new];
    NSMutableArray<NSNumber *> *locations = [NSMutableArray new];
    for (int i = 0; i < splits.count; i++) {
        NSString *colorStopStr = splits[i];
        if (!colorStopStr.length) {
            continue;
        }
        NSArray<NSString *> *colorAndStop = [colorStopStr componentsSeparatedByString:@" "];
        UIColor *color = [UIView css_color:(NSString *)colorAndStop.firstObject];
        [colors addObject:(__bridge id)color.CGColor];
        [locations addObject:@([colorAndStop.lastObject floatValue])];
    }
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGFloat locationsC[locations.count];
    for (NSInteger i = 0; i < locations.count; i++) {
        locationsC[i] = [locations[i] floatValue];
    }
    CGGradientRef gradient = CGGradientCreateWithColors(colorSpace, (__bridge CFArrayRef)colors, locations.count > 0 ? locationsC : NULL);
    dispatch_async(dispatch_get_main_queue(), ^{
        // 释放资源
        CGGradientRelease(gradient);
        CGColorSpaceRelease(colorSpace);
    });
    return gradient;
}

- (void)applyStrokeStyle:(CGContextRef)context path:(CGMutablePathRef) path {
    if (self.strokeStyle.length) {
        NSString *linearGradientPrefix = @"linear-gradient";
        if ([self.strokeStyle hasPrefix:linearGradientPrefix]) {
            
           NSDictionary *params = [[self.strokeStyle substringFromIndex:linearGradientPrefix.length] kr_stringToDictionary];
            CGFloat x0 = [params[@"x0"] doubleValue];
            CGFloat y0 = [params[@"y0"] doubleValue];
            CGFloat x1 = [params[@"x1"] doubleValue];
            CGFloat y1 = [params[@"y1"] doubleValue];
            NSString *colorStopStr = params[@"colorStops"] ?: @"";
            CGGradientRef gradient = [self gradientWithColorStopsStr:colorStopStr];
            // 保存当前图形状态
            CGContextSaveGState(context);
            CGContextReplacePathWithStrokedPath(context);
            // 将上下文裁剪为路径
            CGContextClip(context);
            // 绘制渐变
            CGContextDrawLinearGradient(context, gradient, CGPointMake(x0, y0), CGPointMake(x1, y1), 0);
            // 恢复先前的图形状态（包括裁剪状态）
            CGContextRestoreGState(context);
            
            CGContextSetStrokeColorWithColor(context, [UIColor clearColor].CGColor);
        } else {
            UIColor *color = [UIView css_color:self.strokeStyle];
            CGContextSetStrokeColorWithColor(context, color.CGColor);
        }
    } else {
        CGContextSetStrokeColorWithColor(context, [UIColor clearColor].CGColor);
    }
}
- (void)applyFillStyle:(CGContextRef)context path:(CGMutablePathRef) path {
    if (self.fillStyle.length) {
        //渐变
        NSString *linearGradientPrefix = @"linear-gradient";
        if ([self.fillStyle hasPrefix:linearGradientPrefix]) {
            
           NSDictionary *params = [[self.fillStyle substringFromIndex:linearGradientPrefix.length] kr_stringToDictionary];
            CGFloat x0 = [params[@"x0"] doubleValue];
            CGFloat y0 = [params[@"y0"] doubleValue];
            CGFloat x1 = [params[@"x1"] doubleValue];
            CGFloat y1 = [params[@"y1"] doubleValue];
            NSString *colorStopStr = params[@"colorStops"] ?: @"";
            CGGradientRef gradient = [self gradientWithColorStopsStr:colorStopStr];
            // 保存当前图形状态
            CGContextSaveGState(context);
            // 将上下文裁剪为路径
            CGContextClip(context);
            // 绘制渐变
            CGContextDrawLinearGradient(context, gradient, CGPointMake(x0, y0), CGPointMake(x1, y1), 0);
            // 恢复先前的图形状态（包括裁剪状态）
            CGContextRestoreGState(context);
            CGContextSetFillColorWithColor(context, [UIColor clearColor].CGColor);
        } else {
            UIColor *color = [UIView css_color:self.fillStyle];
            CGContextSetFillColorWithColor(context, color.CGColor);
        }
    } else {
        CGContextSetFillColorWithColor(context, [UIColor clearColor].CGColor);
    }
}


#pragma mark - dealloc

- (void)dealloc {

    if (_path) {
        CGPathRelease(_path);
    }
}


@end

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

#import "KRRichTextView.h"
#import "KRComponentDefine.h"
#import "KRConvertUtil.h"
#import "KuiklyRenderBridge.h"
#import "NSObject+KR.h"

NSString *const KuiklyIndexAttributeName = @"KuiklyIndexAttributeName";
@interface KRRichTextView()

@property (nonatomic, strong) NSNumber *css_numberOfLines;
@property (nonatomic, strong) NSString *css_lineBreakMode;


@end

@implementation KRRichTextView {
    
}
@synthesize hr_rootView;

#pragma mark - KuiklyRenderViewExportProtocol

- (instancetype)init {
    if (self = [super init]) {
        self.displaysAsynchronously = NO;
    }
    return self;
}

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)hrv_prepareForeReuse {
    KUIKLY_RESET_CSS_COMMON_PROP;
    self.attributedText = nil;
    self.css_numberOfLines = nil;
    self.css_lineBreakMode = nil;
}

+ (id<KuiklyRenderShadowProtocol>)hrv_createShadow {
    return [[KRRichTextShadow alloc] init];
}

- (void)hrv_setShadow:(id<KuiklyRenderShadowProtocol>)shadow {
    KRRichTextShadow * textShadow = (KRRichTextShadow *)shadow;
    self.attributedText = textShadow.attributedString;
}


#pragma mark - set prop

- (void)setCss_numberOfLines:(NSNumber *)css_numberOfLines {
    if (self.css_numberOfLines != css_numberOfLines) {
        _css_numberOfLines = css_numberOfLines;
        self.numberOfLines = [css_numberOfLines unsignedIntValue];
    }
}

- (void)setCss_lineBreakMode:(NSString *)css_lineBreakMode {
    if (self.css_lineBreakMode != css_lineBreakMode) {
        _css_lineBreakMode = css_lineBreakMode;
        self.lineBreakMode = [KRConvertUtil NSLineBreakMode:css_lineBreakMode];
    }
}

#pragma mark - override

- (void)css_onClickTapWithSender:(UIGestureRecognizer *)sender {
    CGPoint location = [sender locationInView:self];
    CGPoint pageLocation = [sender locationInView:self.window];
    KRTextRender * textRender = self.attributedText.hr_textRender;
    NSInteger index = [textRender characterIndexForPoint:location];
    NSNumber *spanIndex = nil;
    if (index >= 0 && index < self.attributedText.length) {
        spanIndex = [self.attributedText attribute:KuiklyIndexAttributeName atIndex:index effectiveRange:nil];
    }
    self.css_click(@{
        @"x": @(location.x),
        @"y": @(location.y),
        @"pageX": @(pageLocation.x),
        @"pageY": @(pageLocation.y),
        @"index": spanIndex?: @(-1),
    });
    
}

- (void)setBackgroundColor:(UIColor *)backgroundColor
{
    [super setBackgroundColor:backgroundColor];
    // 背景颜色会影响shodow，这里更新下shadow
    [self setCss_boxShadow:self.css_boxShadow];
}

- (void)setCss_boxShadow:(NSString *)css_boxShadow
{
    // 背景色为clear时，会变成textShadow，这里和安卓对齐，统一由textShadow属性来控制
    if (self.backgroundColor != UIColor.clearColor) {
        [super setCss_boxShadow:css_boxShadow];
    }
}

@end

/// KRRichTextShadow
@interface KRRichTextShadow()

@end

@implementation KRRichTextShadow {
    NSMutableDictionary<NSString *, id> *_props; // context thread used
    NSArray<NSDictionary *> * _spans; // context thread used
    NSMutableAttributedString *_mAttributedString; // context thread used
}

#pragma mark - KuiklyRenderShadowProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    if (!_props) {
        _props = [[NSMutableDictionary alloc] init];
    }
    _props[propKey] = propValue;
}


- (CGSize)hrv_calculateRenderViewSizeWithConstraintSize:(CGSize)constraintSize {
    _mAttributedString = [self p_buildAttributedString];
   
    CGFloat height = constraintSize.height > 0 ? constraintSize.height : MAXFLOAT;
    NSInteger numberOfLines = [KRConvertUtil NSInteger:_props[@"numberOfLines"]];
    NSLineBreakMode lineBreakMode = [KRConvertUtil NSLineBreakMode:_props[@"lineBreakMode"]];
    CGFloat lineBreakMargin = [KRConvertUtil CGFloat:_props[@"lineBreakMargin"]];
    CGFloat lineHeight = [KRConvertUtil CGFloat:_props[@"lineHeight"]];
    CGSize fitSize = [KRLabel sizeThatFits:CGSizeMake(constraintSize.width, height) attributedString:_mAttributedString numberOfLines:numberOfLines lineBreakMode:lineBreakMode lineBreakMarin:lineBreakMargin lineHeight:lineHeight];
    return fitSize;
}

- (NSString *)hrv_callWithMethod:(NSString *)method params:(NSString *)params {
    if ([method isEqualToString:@"spanRect"]) { // span所在的排版位置坐标
        return [self css_spanRectWithParams:params];
    } else if ([method isEqualToString:@"isLineBreakMargin"]) {
        return [self isLineBreakMargin];
    }
    return @"";
}

- (dispatch_block_t)hrv_taskToMainQueueWhenWillSetShadowToView {
    __weak typeof(self) weakSelf = self;
    NSMutableAttributedString *attrString = _mAttributedString;
    return ^{
        weakSelf.attributedString = attrString;
    };
}

#pragma mark - public

- (NSAttributedString *)buildAttributedString {
    return [self p_buildAttributedString];
}

#pragma mark - private

- (NSMutableAttributedString *)p_buildAttributedString {
    NSArray *spans = [KRConvertUtil hr_arrayWithJSONString:_props[@"values"]];
    if (!spans.count) {
        spans = @[_props ? : @{}];
    }
    _spans = spans;
    NSString *textPostProcessor = nil;
    NSMutableArray * richAttrArray = [NSMutableArray new];
    UIFont *mainFont = nil;
    for (NSMutableDictionary * span in spans) {
        if (span[@"placeholderWidth"]) { // 属于占位span
            NSAttributedString *placeholderSpanAttributedString = [self p_createPlaceholderSpanAttributedStringWithSpan:span];
            [richAttrArray addObject:placeholderSpanAttributedString];
            continue;
        }
        
        NSString *text = span[@"value"] ?: span[@"text"];
        if (!text.length) {
            continue;
        }
        NSMutableDictionary *propStyle = [(_props ? : @{}) mutableCopy];
        [propStyle addEntriesFromDictionary:span];
        
        UIFont *font = [KRConvertUtil UIFont:propStyle];
        UIColor * color = [UIView css_color:propStyle[@"color"]] ?: [UIColor blackColor];
        CGFloat letterSpacing = [KRConvertUtil CGFloat:propStyle[@"letterSpacing"]];
        KRTextDecorationLineType textDecoration = [KRConvertUtil KRTextDecorationLineType:propStyle[@"textDecoration"]];
        NSTextAlignment textAlign = [KRConvertUtil NSTextAlignment:propStyle[@"textAlign"]];
        NSNumber *lineHeight = nil;
        NSNumber *lineSpacing = nil;
        NSNumber *paragraphSpacing = propStyle[@"paragraphSpacing"] ? @([KRConvertUtil CGFloat:propStyle[@"paragraphSpacing"]]) : nil;
        if (propStyle[@"lineHeight"]) {
            lineHeight = @([KRConvertUtil CGFloat:propStyle[@"lineHeight"]]);
        } else {
            lineSpacing = @([KRConvertUtil CGFloat:propStyle[@"lineSpacing"]]);
        }
        CGFloat headIndent = [KRConvertUtil CGFloat:propStyle[@"headIndent"]];
        UIColor *strokeColor = [UIView css_color:propStyle[@"strokeColor"]];
        CGFloat strokeWidth = [KRConvertUtil CGFloat:propStyle[@"strokeWidth"]];
        NSInteger spanIndex = [spans indexOfObject:span];
        
        NSShadow *textShadow = nil;
        NSString *cssTextShadow = propStyle[@"textShadow"];
        if ([cssTextShadow isKindOfClass:[NSString class]] && cssTextShadow.length > 0) {
            CSSBoxShadow *shadow = [[CSSBoxShadow alloc] initWithCSSBoxShadow:cssTextShadow];
            
            textShadow = [NSShadow new];
            textShadow.shadowColor = shadow.shadowColor;
            textShadow.shadowOffset = CGSizeMake(shadow.offsetX, shadow.offsetY);
            textShadow.shadowBlurRadius = shadow.shadowRadius;
        }
        if (propStyle[@"textPostProcessor"]) {
            textPostProcessor = propStyle[@"textPostProcessor"];
        }

        if (!mainFont) {
            mainFont = font;
        }
        if ([textPostProcessor isKindOfClass:[NSString class]] && textPostProcessor.length) {
            // 代理
            if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(kr_customTextWithText:textPostProcessor:)]) {
                text = [[KuiklyRenderBridge componentExpandHandler] kr_customTextWithText:text textPostProcessor:textPostProcessor];
            }
        }
        NSMutableAttributedString *spanAttrString = [self p_createSpanAttributedStringWithText:text
                                                                                     spanIndex:spanIndex
                                                                                          font:font
                                                                                         color:color
                                                                                 letterSpacing:letterSpacing
                                                                                textDecoration:textDecoration
                                                                                     textAlign:textAlign
                                                                                   lineSpacing:lineSpacing
                                                                                     lineHeight:lineHeight
                                                                              paragraphSpacing:paragraphSpacing
                                                                                     headIndent:headIndent strokeColor:strokeColor
                                                                                   strokeWidth:strokeWidth
                                                                                        shadow:textShadow];
        if (spanAttrString) {
            [richAttrArray addObject:spanAttrString];
        }
    }
    NSMutableAttributedString *resAttr = [[NSMutableAttributedString alloc] init];
    for (NSAttributedString *attr in richAttrArray) {
        [resAttr appendAttributedString:attr];
    }
    if ([textPostProcessor isKindOfClass:[NSString class]] && textPostProcessor.length) {
        // 代理
        if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(kr_customTextWithAttributedString:font:textPostProcessor:)]) {
            resAttr = [[KuiklyRenderBridge componentExpandHandler] kr_customTextWithAttributedString:resAttr font:mainFont textPostProcessor:textPostProcessor];
        }
    }
    if ([textPostProcessor isKindOfClass:[NSString class]] && textPostProcessor.length) {
        // 代理
        if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_customTextWithAttributedString:textPostProcessor:)]) {
            resAttr = [[KuiklyRenderBridge componentExpandHandler] hr_customTextWithAttributedString:resAttr textPostProcessor:textPostProcessor];
        }
    }
    return resAttr;
}


- (nullable NSMutableAttributedString *)p_createSpanAttributedStringWithText:(NSString *)text
                                                                   spanIndex:(NSUInteger)spanIndex
                                                                        font:(UIFont *)font
                                                                       color:(UIColor *)color letterSpacing:(CGFloat)letterSpacing textDecoration:(KRTextDecorationLineType)textDecoration textAlign:(NSTextAlignment)textAliment  lineSpacing:(NSNumber *)lineSpacing
                                                                           lineHeight:(NSNumber *)lineHeight
                                                            paragraphSpacing:(NSNumber *)paragraphSpacing
                                                                  headIndent:(CGFloat)headIndent
                                                             strokeColor:(UIColor *)strokeColor
                                                                 strokeWidth:(CGFloat)strokeWidth
                                                                      shadow:(NSShadow *)shadow
{
    NSMutableAttributedString * attributedString = [[NSMutableAttributedString alloc] initWithString:text attributes:@{}];
    NSRange range = NSMakeRange(0, attributedString.length);
    
    [attributedString addAttribute:NSFontAttributeName value:font ?: [NSNull null] range:range];
    
    [attributedString addAttribute:NSForegroundColorAttributeName value:color range:range];

//    // 强制使用LTR文本方向
    [attributedString addAttribute:NSWritingDirectionAttributeName value:@[@(NSWritingDirectionLeftToRight | NSTextWritingDirectionOverride)] range:range];
    
    if(letterSpacing){
        [attributedString addAttribute:NSKernAttributeName value:@(letterSpacing) range:range];
    }
    
    if (textDecoration == KRTextDecorationLineTypeUnderline) {
        [attributedString addAttribute:NSUnderlineStyleAttributeName value:@(NSUnderlineStyleSingle) range:range];
    }
    if (textDecoration == KRTextDecorationLineTypeStrikethrough ) {
        [attributedString addAttribute:NSStrikethroughStyleAttributeName value:@(NSUnderlineStyleSingle) range:range];
    }
    
    [self p_applyTextAttributeWithAttr:attributedString textAliment:textAliment lineSpacing:lineSpacing paragraphSpacing:paragraphSpacing lineHeight:lineHeight range:range fontSize:font.pointSize headIndent:headIndent font:font];
    if (strokeColor) {
        [attributedString addAttribute:NSStrokeColorAttributeName value:strokeColor range:range];
        NSNumber *width = _strokeAndFill ? @(-strokeWidth) : @(strokeWidth);
        [attributedString addAttribute:NSStrokeWidthAttributeName value:width  range:range];
    }
    
    [attributedString addAttribute:KuiklyIndexAttributeName value:@(spanIndex) range:range];
    if (shadow) {
        [attributedString addAttribute:NSShadowAttributeName value:shadow range:range];
    }
    
    return attributedString;
 
    
}

- (NSAttributedString *)p_createPlaceholderSpanAttributedStringWithSpan:(NSMutableDictionary *)span {
    KRRichTextAttachment *attachment = [[KRRichTextAttachment alloc] init];
    CGFloat height = [span[@"placeholderHeight"] doubleValue];
    CGFloat width = [span[@"placeholderWidth"] doubleValue];
    NSMutableDictionary *propStyle = [(_props ? : @{}) mutableCopy];
    [propStyle addEntriesFromDictionary:span];
    if (!propStyle[@"fontSize"]) {
        for (NSDictionary * inSpan in _spans) {
            if (inSpan[@"fontSize"]) {
                [propStyle addEntriesFromDictionary:inSpan];
                break;
            }
        }
    }
    UIFont *font = [KRConvertUtil UIFont:propStyle];

    CGFloat lineHeight = [KRConvertUtil CGFloat:propStyle[@"lineHeight"]];
    if (lineHeight > 0) {
        attachment.offsetY = - font.descender;
    } else {
        attachment.offsetY = ( height - font.capHeight ) / 2.0;
    }

    attachment.bounds = CGRectMake(0, -attachment.offsetY, width, height);
    if ([span isKindOfClass:[NSMutableDictionary class]]) {
        ((NSMutableDictionary *)span)[@"attachment"] = attachment;
    }

    NSAttributedString *attrString = [NSAttributedString attributedStringWithAttachment:attachment];
    NSMutableAttributedString *mutableAttrString = [[NSMutableAttributedString alloc] initWithAttributedString:attrString];
    [mutableAttrString kr_addAttribute:NSWritingDirectionAttributeName value:@[@(NSWritingDirectionLeftToRight | NSTextWritingDirectionOverride)] range:NSMakeRange(0, mutableAttrString.length)];
    return mutableAttrString;
}


- (void)p_applyTextAttributeWithAttr:(NSMutableAttributedString *)attributedString textAliment:(NSTextAlignment)textAliment
                         lineSpacing:(NSNumber *)lineSpacing
                    paragraphSpacing: (NSNumber *)paragraphSpacing
                          lineHeight:(NSNumber *)lineHeight
                               range:(NSRange)range
                            fontSize:(CGFloat)fontSize
                          headIndent:(CGFloat)headIndent
                                font:(UIFont *)font {
    NSMutableParagraphStyle *style  = [[NSMutableParagraphStyle alloc] init];
    style.alignment = textAliment;
    // 强制使用LTR文本方向，确保文本始终从左到右显示
    style.baseWritingDirection = NSWritingDirectionLeftToRight;
    if (lineSpacing) {
         style.lineSpacing = ceil([lineSpacing floatValue]) ;
    }
    if (lineHeight) {
        style.maximumLineHeight = [lineHeight floatValue];
        style.minimumLineHeight = [lineHeight floatValue];
        CGFloat baselineOffset = ([lineHeight floatValue]  - font.pointSize) / 2;
        [attributedString addAttribute:NSBaselineOffsetAttributeName value:@(baselineOffset) range:range];
    }
    if (paragraphSpacing) {
        style.paragraphSpacing = ceil([paragraphSpacing floatValue]) ;
    }
    if (headIndent) {
        style.firstLineHeadIndent = headIndent;
    }
    [attributedString addAttribute:NSParagraphStyleAttributeName value:style range:range];
}

#pragma mark css - method
/*
 * 返回span所在的文本排版坐标
 */
- (NSString *)css_spanRectWithParams:(NSString *)params {
    if (!_mAttributedString) { // 文本还未排版，调用无效
        return @"";
    }
    NSInteger spanIndex = [params intValue];
    if (spanIndex < _spans.count ) {
        KRRichTextAttachment *attachment = _spans[spanIndex][@"attachment"];

        // 检查attachment是否在可见范围内
        NSInteger numberOfLines = [KRConvertUtil NSInteger:_props[@"numberOfLines"]];
        NSLayoutManager *layoutManager = _mAttributedString.hr_textRender.layoutManager;
        NSTextContainer *textContainer = _mAttributedString.hr_textRender.textContainer;

        if (numberOfLines > 0 && layoutManager && textContainer) {
            // 获取attachment对应的字形索引
            NSUInteger glyphIndex = [layoutManager glyphIndexForCharacterAtIndex:attachment.charIndex];
            // 获取截断的字形范围
            NSRange truncatedGlyphRange = [layoutManager truncatedGlyphRangeInLineFragmentForGlyphAtIndex:glyphIndex];

            // 如果有截断
            if (truncatedGlyphRange.location != NSNotFound && truncatedGlyphRange.length > 0) {
                // 判断attachment是否在截断范围内
                if (glyphIndex >= truncatedGlyphRange.location) {
                    return @"";
                }
            }
        }

        CGRect frame = [_mAttributedString.hr_textRender boundingRectForCharacterRange:NSMakeRange(attachment.charIndex, 1)];
        CGFloat offsetY = (CGRectGetHeight(frame) - attachment.bounds.size.height) / 2.0;
        return [NSString stringWithFormat:@"%.2lf %.2lf %.2lf %.2lf", CGRectGetMinX(frame), CGRectGetMinY(frame) + offsetY, attachment.bounds.size.width , attachment.bounds.size.height];
    }
    return @"";
    
}

- (NSString *)isLineBreakMargin {
    return _mAttributedString.hr_textRender.isBreakLine ? @"1" : @"0";
}


- (void)dealloc {
    
}

@end




@implementation KRRichTextAttachment


- (UIImage *)imageForBounds:(CGRect)imageBounds textContainer:(NSTextContainer *)textContainer characterIndex:(NSUInteger)charIndex {
    return nil;
}



- (CGRect)attachmentBoundsForTextContainer:(NSTextContainer *)textContainer proposedLineFragment:(CGRect)lineFrag glyphPosition:(CGPoint)position characterIndex:(NSUInteger)charIndex {
    _charIndex = charIndex;
    return CGRectMake(0, -self.offsetY, self.bounds.size.width, self.bounds.size.height);
}

@end

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

#import "KRLabel.h"
#import <pthread.h>
#import <libkern/OSAtomic.h>
#import "KRAsyncDeallocManager.h"
#import <objc/runtime.h>
#import "NSObject+KR.h"
#import <CoreText/CoreText.h>

#define KRAssertMainThread() NSAssert(0 != pthread_main_np(), @"This method must be called on the main thread!")
NSString *const KRHighlightAttributeKey = @"KRHighlightAttributeKey";
NSString *const KRBGAttributeKey = @"KRBGAttributeKey";


@interface KRLabel()

@end

@implementation KRLabel


- (void)setSelectedRange:(NSRange)selectedRange {
    if (NSEqualRanges(_selectedRange, selectedRange)) {
        return;
    }
    _selectedRange = selectedRange;
    [self setNeedsDisplay];
}

- (void)setSelectionColor:(UIColor *)selectionColor {
    if (_selectionColor == selectionColor) {
        return;
    }
    _selectionColor = selectionColor;
    [self setNeedsDisplay];
}

#pragma mark - override

- (NSString *)accessibilityLabel{
    NSString * res = [super accessibilityLabel];
    if (res.length <= 0) {
        return self.attributedText.string;
    }
    return res;
}


- (void)setAttributedText:(NSAttributedString *)attributedText {
    [super setAttributedText:attributedText];
    self.textRender = attributedText.hr_textRender;
    self.attributedText.hr_textRender = self.textRender;
    // CoreText 测量修复：UILabel.attributedText 为 copy 语义，测量元数据（associated object）
    // 不随 copy 走。CoreText 路径不再预建渲染树（hr_textRender 为 nil），渲染时惰性构建依赖
    // 元数据还原 lineBreakMargin/isBreakLine/container 宽度，此处从测量原对象补挂，
    // 否则 lineBreakMargin 的排版避让（exclusion path）与 span 定位会失效。
    if (attributedText.hr_measureMeta && !self.attributedText.hr_measureMeta) {
        self.attributedText.hr_measureMeta = attributedText.hr_measureMeta;
    }
    [self setNeedsDisplay];
}


- (void)drawTextInRect:(CGRect)rect {
    // 使用TextKit绘制文本
    // CoreText 测量修复：测量不再预建渲染树，渲染时（主线程）惰性构建，TextKit 对象生命周期收敛在主线程
    if (!self.textRender && self.attributedText.length > 0) {
        self.textRender = [NSAttributedString kr_ensureTextRenderForAttributedString:self.attributedText];
        self.attributedText.hr_textRender = self.textRender;
    }
    if (!self.textRender) {
        return;
    }
    self.textRender.size = rect.size;
    if (self.textRender.lineBreakMargin > 0 && self.textRender.isBreakLine) {
        CGSize size = self.textRender.size;
        UIBezierPath * bezierPath = [UIBezierPath bezierPathWithRect:CGRectMake(size.width - self.textRender.lineBreakMargin, size.height - 10, self.textRender.lineBreakMargin, 10)];
        self.textRender.textContainer.exclusionPaths = @[bezierPath];
    }
	
	// Draw selection background.
    if (self.selectedRange.length > 0 && self.selectedRange.location != NSNotFound && self.selectedRange.location + self.selectedRange.length <= self.textRender.textStorage.length) {
        if (!self.selectionColor) {
            self.selectionColor = [[UIColor colorWithRed:0x00/255.0 green:0x99/255.0 blue:0xff/255.0 alpha:1.0] colorWithAlphaComponent:0.3];
        }
        [self.selectionColor setFill];
        
        NSRange glyphRange = [self.textRender.layoutManager glyphRangeForCharacterRange:self.selectedRange actualCharacterRange:nil];
        [self.textRender.layoutManager enumerateEnclosingRectsForGlyphRange:glyphRange withinSelectedGlyphRange:glyphRange inTextContainer:self.textRender.textContainer usingBlock:^(CGRect r, BOOL * _Nonnull stop) {
             CGRect drawRect = CGRectOffset(r, rect.origin.x, rect.origin.y);
#if TARGET_OS_OSX
             CGContextRef context = UIGraphicsGetCurrentContext();
             if (context) {
                 CGContextFillRect(context, drawRect);
             }
#else
             UIRectFill(drawRect);
#endif
        }];
    }
    
    [self.textRender drawTextAtPoint:rect.origin isCanceled:nil];

}

- (void)setBackgroundColor:(UIColor *)backgroundColor {
    if (backgroundColor == nil) {
        backgroundColor = [UIColor clearColor];
    }
    [super setBackgroundColor:backgroundColor];
}


#pragma mark - public

+ (CGSize)sizeThatFits:(CGSize)size attributedString:(NSAttributedString *)attString numberOfLines:(NSUInteger)lines lineBreakMode:(NSLineBreakMode)mode{
    return [self sizeThatFits:size attributedString:attString numberOfLines:lines lineBreakMode:mode lineBreakMarin:0];
}

+ (CGSize)sizeThatFits:(CGSize)size attributedString:(NSAttributedString *)attString numberOfLines:(NSUInteger)lines lineBreakMode:(NSLineBreakMode)mode lineBreakMarin:(CGFloat)marin {
    return [self sizeThatFits:size attributedString:attString numberOfLines:lines lineBreakMode:mode lineBreakMarin:0 lineHeight:0];
}

+ (CGSize)sizeThatFits:(CGSize)size attributedString:(NSAttributedString *)attString numberOfLines:(NSUInteger)lines lineBreakMode:(NSLineBreakMode)mode lineBreakMarin:(CGFloat)marin lineHeight:(CGFloat)lineHeight {
    // context 线程文本测量死锁修复。
    // 方案：测量改用纯 CoreText（不创建 NSTextStorage、不发 TextKit 编辑通知），从源头免疫
    // 通知中心中任何 block+queue 观察者（如 _UISearchBarTextFieldTokenCounter 悬垂观察者
    // 叠加 malloc 地址复用触发的死锁）。渲染树（KRTextRender）延迟到主线程渲染时惰性构建。
    attString = [attString isKindOfClass:[NSAttributedString class]] ? attString : [[NSAttributedString alloc] initWithString:@""];
    CGSize fitSize = CGSizeZero;
    BOOL isBreakLine = NO;
    // 含硬换行的文本回退 TextKit 路径——fixEllipsis（硬换行省略号修复，
    // 对齐 Android/HarmonyOS）仅在 TextKit 路径实现，CoreText 逐行测量未覆盖该场景，
    // 回退保真；后续版本可将 fixEllipsis 移植到 CoreText 后放开。
    BOOL useCoreText = ![self kr_containsAttachment:attString] && ![self kr_containsHardLineBreak:attString];

    if (useCoreText) {
        fitSize = [self kr_coreTextSize:attString width:size.width lines:lines mode:mode lineHeight:lineHeight];
        if (marin > 0 && lines) {
            CGSize fullSize = [self kr_coreTextSize:attString width:size.width lines:0 mode:mode lineHeight:lineHeight];
            isBreakLine = !CGSizeEqualToSize(fitSize, fullSize);
        }
    } else {
        // 含 NSTextAttachment（富文本图片混排）或硬换行时走 TextKit 老路径保真
        //（CoreText 对齐 attachment 宽度需 CTRunDelegate，后续版本补齐）。
        KRTextRender *textRender = [self kr_textKitMeasure:attString width:size.width lines:lines mode:mode marin:marin lineHeight:lineHeight];
        fitSize = attString.hr_size;
        isBreakLine = textRender.isBreakLine;
    }

    // 测量元数据存入 attString：渲染树惰性构建（主线程）时还原 lineHeight/lines/mode/margin/isBreakLine/width
    attString.hr_measureMeta = @{
        KRMasureMetaLineHeight: @(lineHeight),
        KRMasureMetaLines: @(lines),
        KRMasureMetaMode: @(mode),
        KRMasureMetaMargin: @(marin),
        KRMasureMetaIsBreakLine: @(isBreakLine),
        KRMasureMetaWidth: @(size.width),
    };
    attString.hr_size = fitSize;

    return fitSize;
}

#pragma mark - CoreText measure (deadlock fix)

static NSString *const KRMasureMetaLineHeight = @"lineHeight";
static NSString *const KRMasureMetaLines = @"lines";
static NSString *const KRMasureMetaMode = @"mode";
static NSString *const KRMasureMetaMargin = @"margin";
static NSString *const KRMasureMetaIsBreakLine = @"isBreakLine";
static NSString *const KRMasureMetaWidth = @"width";

/// 是否含 NSTextAttachment（富文本图片混排）。含 attachment 时走 TextKit fallback 保真。
+ (BOOL)kr_containsAttachment:(NSAttributedString *)attString {
    if (attString.length == 0) {
        return NO;
    }
    __block BOOL hasAttachment = NO;
    [attString enumerateAttribute:NSAttachmentAttributeName
                          inRange:NSMakeRange(0, attString.length)
                          options:0
                       usingBlock:^(id value, NSRange range, BOOL *stop) {
        if (value) {
            hasAttachment = YES;
            *stop = YES;
        }
    }];
    return hasAttachment;
}

/// 是否含硬换行符（\n/\r 等）。含硬换行的文本走 TextKit fallback——
/// fixEllipsis（硬换行省略号修复）仅在 TextKit 路径实现。
+ (BOOL)kr_containsHardLineBreak:(NSAttributedString *)attString {
    if (attString.length == 0) {
        return NO;
    }
    return [attString.string rangeOfCharacterFromSet:[NSCharacterSet newlineCharacterSet]].location != NSNotFound;
}

/// 纯 CoreText 测量：逐行断行。行高语义对齐 KRTextRender 的 layoutManager delegate
/// （lineHeight>0 时 usedRect 高度 = MAX(强制行高, 自然行高)；否则按 font+paragraphStyle 行高）。
+ (CGSize)kr_coreTextSize:(NSAttributedString *)attString width:(CGFloat)width lines:(NSUInteger)lines mode:(NSLineBreakMode)mode lineHeight:(CGFloat)lineHeight {
    if (attString.length == 0 || width <= 0) {
        return CGSizeZero;
    }
    CTTypesetterRef typesetter = CTTypesetterCreateWithAttributedString((CFAttributedStringRef)attString);
    if (!typesetter) {
        return CGSizeZero;
    }
    CFIndex length = (CFIndex)attString.length;
    CGFloat maxWidth = 0;
    CGFloat lineExtent = 0;
    // 单行且非换行模式（clipping/truncating）：TextKit 语义返回整段自然宽度，不按 width 断行
    if (lines == 1 && mode != NSLineBreakByWordWrapping && mode != NSLineBreakByCharWrapping) {
        CTLineRef fullLine = CTTypesetterCreateLine(typesetter, CFRangeMake(0, length));
        CGFloat ascent = 0, descent = 0, leading = 0;
        maxWidth = CTLineGetTypographicBounds(fullLine, &ascent, &descent, &leading);
        CFRelease(fullLine);
        // TextKit clip/truncate 单行语义：usedRect 宽度不超过 container 宽（文本延伸被裁剪）
        maxWidth = MIN(maxWidth, width);
        NSDictionary *attrs = [attString attributesAtIndex:0 effectiveRange:nil];
        lineExtent = [self kr_lineExtentOfLineHeight:lineHeight
                                     paragraphStyle:attrs[NSParagraphStyleAttributeName]
                                       naturalHeight:ascent + descent + leading];
    } else {
        CFIndex index = 0;
        CGFloat totalHeight = 0;
        NSUInteger lineCount = 0;
        CGFloat lastLineSpacing = 0;
        while (index < length) {
            NSDictionary *attrs = [attString attributesAtIndex:(NSUInteger)index effectiveRange:nil];
            NSParagraphStyle *ps = attrs[NSParagraphStyleAttributeName];
            // 首行缩进（TextKit firstLineHeadIndent 语义）：首行断行宽度扣减，后续行全宽。
            // 加下限防御：缩进大于等于容器宽时避免断行宽度非正导致测量归零。
            CGFloat lineBreakWidth = (index == 0) ? MAX(width - ps.firstLineHeadIndent, 0.1) : width;
            CFIndex next = CTTypesetterSuggestLineBreak(typesetter, index, lineBreakWidth);
            if (next <= 0) {
                break;
            }
            CTLineRef line = CTTypesetterCreateLine(typesetter, CFRangeMake(index, next));
            CGFloat ascent = 0, descent = 0, leading = 0;
            double lineWidth = CTLineGetTypographicBounds(line, &ascent, &descent, &leading);
            CFRelease(line);
            lineExtent = [self kr_lineExtentOfLineHeight:lineHeight
                                        paragraphStyle:ps
                                          naturalHeight:ascent + descent + leading];
            // TextKit 语义：lineSpacing 加在行与行之间——自然末行的 fragment 不含间距，
            // 但 numberOfLines 截断行不是自然末行、其 fragment 仍含间距（实测对齐）。
            // 先每行累加，仅文本自然排完（index>=length）时减去末行的 spacing。
            // paragraphSpacing 不处理：仅多段(\n)文本计入段末行（该场景已回退 TextKit 路径）；
            // 单段文本 usedRect 不含段后间距（macOS/iOS 模拟器双平台实测：自然换行与截断均不含，
            // spacing 落在末个 glyph fragment 之下、usedRect 仅统计 fragment 并集），补上反而偏高。
            lastLineSpacing = ps.lineSpacing;
            totalHeight += lineExtent + lastLineSpacing;
            maxWidth = MAX(maxWidth, (CGFloat)lineWidth);
            lineCount++;
            index += next;
            if (lines > 0 && lineCount >= lines) {
                break;
            }
        }
        if (index >= length) {
            // 自然末行不加行间距（对齐 NSLayoutManager；截断/断行失败的行保留间距）
            totalHeight -= lastLineSpacing;
        }
        lineExtent = totalHeight;
    }
    CFRelease(typesetter);
    return CGSizeMake(ceil(maxWidth), ceil(lineExtent));
}

/// 行高对齐 TextKit usedRect 语义，三层约束：
/// ① 行内实际渲染字体的 metrics：CTLineGetTypographicBounds 的 ascent+descent+leading
///    （中文等会 fallback，如 SFUI 声明实际按 PingFang 排版，与 NSLayoutManager usedRect 同源）；
/// ② 段落样式 min/maxLineHeight 约束（Kuikly lineHeight attr 会写入 paragraphStyle，
///    TextKit 排版强制应用，CT 逐行 metrics 不含此约束，需手动模拟）；
/// ③ KRTextRender delegate 语义（_lineHeight>0 时 usedRect=MAX(_lineHeight, usedRect)）。
+ (CGFloat)kr_lineExtentOfLineHeight:(CGFloat)lineHeight paragraphStyle:(NSParagraphStyle *)ps naturalHeight:(CGFloat)naturalHeight {
    CGFloat extent = naturalHeight;
    if (ps.minimumLineHeight > 0) {
        extent = MAX(extent, ps.minimumLineHeight);
    }
    if (ps.maximumLineHeight > 0) {
        extent = MIN(extent, ps.maximumLineHeight);
    }
    if (lineHeight > 0) {
        extent = MAX(lineHeight, extent);
    }
    return extent;
}

/// 原 TextKit 测量路径（attachment fallback / 双引擎对比共用）。会创建 NSTextStorage 并发编辑通知，
/// 主文本测量不再走此路径。
+ (KRTextRender *)kr_textKitMeasure:(NSAttributedString *)attString width:(CGFloat)width lines:(NSUInteger)lines mode:(NSLineBreakMode)mode marin:(CGFloat)marin lineHeight:(CGFloat)lineHeight {
    NSTextStorage *textStorage = [[NSTextStorage alloc] initWithAttributedString:[attString copy]];
    textStorage.hr_hasAttachmentViews = attString.hr_hasAttachmentViews;
    KRTextRender *textRender = [[KRTextRender alloc] initWithTextStorage:textStorage lineHeight:lineHeight];
    textRender.lineBreakMargin = marin;
    textRender.maximumNumberOfLines = lines;
    textRender.lineBreakMode = mode;
    CGSize fitSize = [textRender textSizeWithRenderWidth:width];
    if (marin > 0 && lines) {
        textRender.maximumNumberOfLines = 0;
        CGSize newSize = [textRender textSizeWithRenderWidth:width];
        textRender.isBreakLine = !CGSizeEqualToSize(fitSize, newSize);
        textRender.maximumNumberOfLines = lines;//复原
    }
    // Fix the issue of missing ellipsis caused by hard line breaks (aligned with Android/HarmonyOS)
    // Remove this logic if Apple fixes this issue in the future.
    BOOL didModify = [self kr_fixEllipsisIfNeededForTextStorage:textStorage textRender:textRender lines:lines mode:mode fitSize:fitSize];
    if (didModify) {
        // Recalculate fitSize after modifying textStorage
        fitSize = [textRender textSizeWithRenderWidth:width];
    }
    attString.hr_textRender = textRender;
    attString.hr_size = fitSize;
    return textRender;
}

/// Fix the issue where iOS TextKit does not display ellipsis after hard line breaks
/// When text is truncated by line count limit,
/// but the last line ends with \n instead of width overflow, the system does not add ellipsis.
/// This method detects this situation and manually adds ellipsis to align with Android/HarmonyOS behavior
/// @return YES if textStorage was modified, NO otherwise
+ (BOOL)kr_fixEllipsisIfNeededForTextStorage:(NSTextStorage *)textStorage
                                  textRender:(KRTextRender *)textRender
                                       lines:(NSUInteger)lines
                                        mode:(NSLineBreakMode)mode
                                     fitSize:(CGSize)fitSize {
    // Only handle the mode that requires truncation with ellipsis
    if (mode != NSLineBreakByTruncatingTail || lines == 0) {
        return NO;
    }
    
    NSLayoutManager *layoutManager = textRender.layoutManager;
    NSUInteger numberOfGlyphs = [layoutManager numberOfGlyphs];
    if (numberOfGlyphs == 0) {
        return NO;
    }
    
    // Enumerate lines and use rect to determine which lines are actually visible
    __block NSUInteger visibleLineCount = 0;
    __block NSRange lastVisibleLineGlyphRange = NSMakeRange(0, 0);
    __block BOOL hasMoreFragments = NO;
    [layoutManager enumerateLineFragmentsForGlyphRange:NSMakeRange(0, numberOfGlyphs)
                                            usingBlock:^(CGRect rect, CGRect usedRect,
                                                         NSTextContainer *container, NSRange glyphRange, BOOL *stop) {
        // Check if this line fragment is within the visible area
        if (rect.origin.y + rect.size.height <= fitSize.height + 0.5) { // Add small tolerance for floating point
            visibleLineCount++;
            lastVisibleLineGlyphRange = glyphRange;
            if (visibleLineCount >= lines) {
                *stop = YES;
            }
        } else {
            // This fragment is outside visible area
            hasMoreFragments = YES;
            *stop = YES;
        }
    }];
    
    // If no lines reached the limit and no more fragments, text is fully visible
    if (!hasMoreFragments && (lastVisibleLineGlyphRange.location + lastVisibleLineGlyphRange.length >= numberOfGlyphs)) {
        return NO;
    }
    
    // Check if system has already added ellipsis
    NSRange lastLineCharRange = [layoutManager characterRangeForGlyphRange:lastVisibleLineGlyphRange actualGlyphRange:nil];
    NSUInteger lastVisibleCharIndex = lastLineCharRange.location + lastLineCharRange.length;
    NSRange truncatedRange = [layoutManager truncatedGlyphRangeInLineFragmentForGlyphAtIndex:lastVisibleLineGlyphRange.location];
    if (truncatedRange.location != NSNotFound) {
        return NO;
    }
    
    // Ellipsis need to be added manually
    // Only remove the last newline character (not all trailing newlines)
    // to preserve line structure and show ellipsis on the correct line
    NSUInteger endIndex = lastVisibleCharIndex;
    NSString *text = textStorage.string;
    if (endIndex > 0 && ([text characterAtIndex:endIndex - 1] == '\n' || [text characterAtIndex:endIndex - 1] == '\r')) {
        endIndex--;
    }
    
    if (endIndex == 0) {
        return NO;
    }
    
    // Add ellipsis at the end of visible text
    NSDictionary *attrs = [textStorage attributesAtIndex:endIndex - 1 effectiveRange:nil];
    NSMutableAttributedString *newText = [[textStorage attributedSubstringFromRange:NSMakeRange(0, endIndex)] mutableCopy];
    [newText appendAttributedString:[[NSAttributedString alloc] initWithString:@"…" attributes:attrs]];
    [textStorage replaceCharactersInRange:NSMakeRange(0, textStorage.length) withAttributedString:newText];
    return YES;
}



#pragma mark - private

@end
//---------KRTextRender类分割线------------
@interface KRTextRender() <NSLayoutManagerDelegate> {
    CGRect _textBound;
}
@property (nonatomic, strong) KRLayoutManager * layoutManager;
@property (nonatomic, strong) NSTextContainer * textContainer;
@property (nonatomic, strong) NSTextStorage * textStorageOnRender;


@end
@implementation KRTextRender
@synthesize maximumNumberOfLines = _maximumNumberOfLines;

- (instancetype)init{
    if (self = [super init]) {
        _textContainer = [NSTextContainer new];
        _layoutManager = [KRLayoutManager new];
        _layoutManager.delegate = self;
        [_layoutManager addTextContainer:_textContainer];
        _textContainer.lineFragmentPadding = 0;
    }
    return self;
}

- (instancetype)initWithAttributedText:(NSAttributedString *)attributedText{
    if (self = [self initWithTextStorage:[[NSTextStorage alloc] initWithAttributedString:attributedText]]) {
        self.textStorage.hr_hasAttachmentViews = attributedText.hr_hasAttachmentViews;
    }
    return self;
}

- (instancetype)initWithTextStorage:(NSTextStorage *)textStorage lineHeight:(CGFloat)lineHeight {
    if (self = [self init]) {
        self.lineHeight = lineHeight;
        self.textStorage = textStorage;
    }
    return self;
}

- (instancetype)initWithTextStorage:(NSTextStorage *)textStorage{
    return [self initWithTextStorage:textStorage lineHeight:0];
}
#pragma mark - Getter && Setter

- (void)setTextStorage:(NSTextStorage *)textStorage{
    _textStorage = textStorage;
    self.textStorageOnRender = textStorage;
}

- (void)setTextStorageOnRender:(NSTextStorage *)textStorageOnRender{
    if (_textStorageOnRender != textStorageOnRender) {
        if (_textStorageOnRender) {
            [_textStorageOnRender removeLayoutManager:_layoutManager];
        }
        [textStorageOnRender addLayoutManager:_layoutManager];
        _textStorageOnRender = textStorageOnRender;
    }
}

- (void)setSize:(CGSize)size{
    if (isnan(size.width)) {
        size.width = 0;
    }
    if (isnan(size.height)) {
        size.height = 0;
    }
    _size = size;
    if (!CGSizeEqualToSize(_textContainer.size, size)) {
        _textContainer.size = size;
    }
}

- (void)setLineBreakMode:(NSLineBreakMode)lineBreakMode{
    if (_textContainer.lineBreakMode != lineBreakMode) {
        _textContainer.lineBreakMode = lineBreakMode;
    }
}

- (NSUInteger)maximumNumberOfLines{
    return _textContainer.maximumNumberOfLines;
}

- (void)setMaximumNumberOfLines:(NSUInteger)maximumNumberOfLines{
    if (_textContainer.maximumNumberOfLines != maximumNumberOfLines) {
        _textContainer.maximumNumberOfLines = maximumNumberOfLines;
    }
}

#pragma mark - Public

- (NSRange)visibleGlyphRange {
    return [_layoutManager glyphRangeForTextContainer:_textContainer];
}

- (NSRange)visibleCharacterRange {
    return [_layoutManager characterRangeForGlyphRange:[self visibleGlyphRange] actualGlyphRange:nil];
}

- (CGRect)boundingRectForCharacterRange:(NSRange)characterRange {
    NSRange glyphRange = [_layoutManager glyphRangeForCharacterRange:characterRange actualCharacterRange:nil];
    return [self boundingRectForGlyphRange:glyphRange];
}

- (CGRect)boundingRectForGlyphRange:(NSRange)glyphRange {
    return [_layoutManager boundingRectForGlyphRange:glyphRange inTextContainer:_textContainer];
}

- (CGRect)textBound {
    return [_layoutManager usedRectForTextContainer:_textContainer];
}

- (NSInteger)characterIndexForPoint:(CGPoint)point{
    CGFloat distanceToPoint = 1.0;
    NSUInteger index = [_layoutManager characterIndexForPoint:point inTextContainer:_textContainer fractionOfDistanceBetweenInsertionPoints:&distanceToPoint];
    return distanceToPoint < 1 ? index : -1;
}


- (CGSize)textSizeWithRenderWidth:(CGFloat)renderWidth{
    if (!_textStorageOnRender)  return CGSizeZero;
    _textContainer.size = CGSizeMake(renderWidth, MAXFLOAT);
#if TARGET_OS_OSX // [macOS NSLayoutManager needs explicit layout trigger
    // Force layout to ensure usedRectForTextContainer returns correct size
    [_layoutManager ensureLayoutForTextContainer:_textContainer];
#endif // macOS]
    CGSize textSize = [self textBound].size;
    CGSize res = CGSizeMake(ceil(textSize.width), ceil(textSize.height));
    return  res;
}
#pragma mark -  draw text


- (void)drawTextAtPoint:(CGPoint)point isCanceled:(BOOL (^)(void))isCanceled{
    NSRange glyphRange = [_layoutManager glyphRangeForTextContainer:_textContainer];
    // drawing text
    [_layoutManager enumerateLineFragmentsForGlyphRange:glyphRange usingBlock:^(CGRect rect, CGRect usedRect, NSTextContainer * _Nonnull textContainer, NSRange glyphRange, BOOL * _Nonnull stop) {
        [self->_layoutManager drawBackgroundForGlyphRange:glyphRange atPoint:point];
        if (isCanceled && isCanceled()) {*stop = YES; return ;};
        [self->_layoutManager drawGlyphsForGlyphRange:glyphRange atPoint:point];
        if (isCanceled && isCanceled()) {*stop = YES; return ;};
    }];
}

- (void)dealloc{
    [[KRAsyncDeallocManager shareManager] asyncDeallocWithObject:_textStorageOnRender];
    if (_textStorage != _textStorageOnRender) {
        [[KRAsyncDeallocManager shareManager] asyncDeallocWithObject:_textStorage];
    }
    [[KRAsyncDeallocManager shareManager] asyncDeallocWithObject:_layoutManager];
    [[KRAsyncDeallocManager shareManager] asyncDeallocWithObject:_textContainer];

}

#pragma mark - layout manager delegate
- (BOOL)layoutManager:(NSLayoutManager *)layoutManager shouldSetLineFragmentRect:(inout CGRect *)lineFragmentRect lineFragmentUsedRect:(inout CGRect *)lineFragmentUsedRect baselineOffset:(inout CGFloat *)baselineOffset inTextContainer:(NSTextContainer *)textContainer forGlyphRange:(NSRange)glyphRange {
    
    if (_lineHeight > 0) {
        UIFont *font;
        NSParagraphStyle *style;
        NSArray *attrsList = [self attributesListForGlyphRange:glyphRange layoutManager:layoutManager];
        [self getFont:&font paragraphStyle:&style fromAttibutesList:attrsList];

        if (![font isKindOfClass:[UIFont class]]) {
            return NO;
        }

        UIFont *defaultFont = [self systemDefaultFontForFont:font];

        CGRect rect = *lineFragmentRect;
        CGRect usedRect = *lineFragmentUsedRect;
        
        CGFloat textLineHeight = _lineHeight;
        CGFloat fixedBaseLineOffset = [self.class baseLineOffsetForLineHeight:textLineHeight font:defaultFont];
        
        rect.size.height = textLineHeight;
        usedRect.size.height = MAX(textLineHeight, usedRect.size.height);
        
        *lineFragmentRect = rect;
        *lineFragmentUsedRect = usedRect;
        *baselineOffset = fixedBaseLineOffset;
    }
    
    return YES;
}

+ (CGFloat)lineHeightForFont:(UIFont *)font paragraphStyle:(NSParagraphStyle *)style  {
    CGFloat lineHeight = font.lineHeight;
    if (!style) {
        return lineHeight;
    }
    if (style.lineHeightMultiple > 0) {
        lineHeight *= style.lineHeightMultiple;
    }
    if (style.minimumLineHeight > 0) {
        lineHeight = MAX(style.minimumLineHeight, lineHeight);
    }
    if (style.maximumLineHeight > 0) {
        lineHeight = MIN(style.maximumLineHeight, lineHeight);
    }
    return lineHeight;
}


+ (CGFloat)baseLineOffsetForLineHeight:(CGFloat)lineHeight font:(UIFont *)font {
    CGFloat baseLine = lineHeight + font.descender / 2;
    return baseLine;
}

/// get system default font of size
- (UIFont *)systemDefaultFontForFont:(UIFont *)font {
    return [UIFont systemFontOfSize:font.pointSize];
}


- (NSArray<NSDictionary *> *)attributesListForGlyphRange:(NSRange)glyphRange layoutManager:(NSLayoutManager *)layoutManager {

    // exclude the line break. System doesn't calucate the line rect with it.
    if (glyphRange.length > 1) {
        NSGlyphProperty property = [layoutManager propertyForGlyphAtIndex:glyphRange.location + glyphRange.length - 1];
        if (property & NSGlyphPropertyControlCharacter) {
            glyphRange = NSMakeRange(glyphRange.location, glyphRange.length - 1);
        }
    }

    
    NSTextStorage *textStorage = layoutManager.textStorage;
    NSRange targetRange = [layoutManager characterRangeForGlyphRange:glyphRange actualGlyphRange:nil];
    NSMutableArray *dicts = [NSMutableArray arrayWithCapacity:2];

    NSInteger last = -1;
    NSRange effectRange = NSMakeRange(targetRange.location, 0);

    while (effectRange.location + effectRange.length < targetRange.location + targetRange.length) {
        NSInteger current = effectRange.location + effectRange.length;
        // if effectRange didn't advanced, we manuly add 1 to avoid infinate loop.
        if (current <= last) {
            current += 1;
        }
        NSDictionary *attributes = [textStorage attributesAtIndex:current effectiveRange:&effectRange];
        if (attributes) {
            [dicts addObject:attributes];
        }
        last = current;
    }

    return dicts;
}

- (void)getFont:(UIFont **)returnFont paragraphStyle:(NSParagraphStyle **)returnStyle fromAttibutesList:(NSArray<NSDictionary *> *)attributesList {

    if (attributesList.count == 0) {
        return;
    }

    UIFont *findedFont = nil;
    NSParagraphStyle *findedStyle = nil;
    CGFloat lastHeight = -CGFLOAT_MAX;

    // find the attributes with max line height
    for (NSInteger i = 0; i < attributesList.count; i++) {
        NSDictionary *attrs = attributesList[i];

        NSParagraphStyle *style = attrs[NSParagraphStyleAttributeName];
        UIFont *font = attrs[NSFontAttributeName];

        if ([font isKindOfClass:[UIFont class]] &&
            (!style || [style isKindOfClass:[NSParagraphStyle class]]) ) {

            CGFloat height = [self.class lineHeightForFont:font paragraphStyle:style];
            if (height > lastHeight) {
                lastHeight = height;
                findedFont = font;
                findedStyle = style;
            }
        }
    }

    *returnFont = findedFont;
    *returnStyle = findedStyle;
}


@end

//------KRLayoutManager类分割线-----

@implementation KRLayoutManager{
    CGPoint _drawAtPoint;
}

- (void)drawBackgroundForGlyphRange:(NSRange)glyphsToShow atPoint:(CGPoint)origin {
    _drawAtPoint = origin;
    [super drawBackgroundForGlyphRange:glyphsToShow atPoint:origin];
    _drawAtPoint = CGPointZero;
}
- (void)dealloc{
#if DEBUG
    
    

#endif
}

@end
     

@interface KRTextAttachment ()
@property (nonatomic, assign) NSRange range;
@property (nonatomic, assign) CGPoint position;
@end

@implementation KRTextAttachment
- (void)setSize:(CGSize)size {
    _size = size;
    self.bounds = CGRectMake(0, _baseline, _size.width, _size.height);
}

- (void)setBounds:(CGRect)bounds {
    [super setBounds:bounds];
    _size = bounds.size;
}

- (void)setBaseline:(CGFloat)baseline {
    _baseline = baseline;
    self.bounds = CGRectMake(0, _baseline, _size.width, _size.height);
}

- (void)setImage:(UIImage *)image {
    [super setImage:image];
    if (_size.width == 0 && _size.height == 0 ) {
        self.size = image.size;
    }
}

- (void)setView:(UIView *)view {
    _view = view;
    if (_size.width == 0 && _size.height == 0 ) {
        self.size = view.frame.size;
    }
}


- (nullable UIImage *)imageForBounds:(CGRect)imageBounds textContainer:(nullable NSTextContainer *)textContainer characterIndex:(NSUInteger)charIndex {
    _position = CGPointMake(imageBounds.origin.x, imageBounds.origin.y - _size.height);
    return self.image;
}

- (CGRect)attachmentBoundsForTextContainer:(NSTextContainer *)textContainer proposedLineFragment:(CGRect)lineFrag glyphPosition:(CGPoint)position characterIndex:(NSUInteger)charIndex {
    if (_verticalAlignment == KRAttachmentAlignmentBaseline || self.bounds.origin.y > 0) {
        
        return CGRectMake(self.bounds.origin.x, self.bounds.origin.y - 2, self.bounds.size.width, self.bounds.size.height);
    }
    CGFloat offset = 0;
    UIFont *font = [textContainer.layoutManager.textStorage attribute:NSFontAttributeName atIndex:charIndex effectiveRange:nil];
    if (!font) {
        return self.bounds;
    }
    //    CGFloat pointSize = font.pointSize;
    //    CGFloat mid = font.descender + font.capHeight;
    //    CGFloat dd = font.descender ;
    //    CGFloat dd2 = font.ascender ;
    switch (_verticalAlignment) {
            //        case KRAttachmentAlignmentBaseline:
            //            offset = (font.capHeight - _size.height)/2;
            //            break;
        case KRAttachmentAlignmentCenter:
        {
            offset = (_size.height - font.capHeight)/2;
        }
            break;
        case KRAttachmentAlignmentBottom:
            offset = _size.height-font.pointSize + 2;
        default:
            break;
    }
    return CGRectMake(0, -offset, _size.width, _size.height);
}


@end

@implementation KRTextAttachment (Display)

- (void)setFrame:(CGRect)frame {
    _view.frame = frame;
}

- (void)addToSuperView:(UIView *)superView {
    if (_view) {
        [superView addSubview:_view];
    }
}
- (void)removeFromSuperView:(UIView *)superView {
    if (_view.superview == superView) {
        [_view removeFromSuperview];
    }
}

@end

@implementation NSAttributedString (KRTextAttachment)
- (BOOL)hr_hasAttachmentViews{
    NSNumber * value = objc_getAssociatedObject(self, @selector(hr_hasAttachmentViews));
    return [value boolValue];
}


- (void)setHr_hasAttachmentViews:(BOOL)hr_hasAttachmentViews{
    objc_setAssociatedObject(self, @selector(hr_hasAttachmentViews), @(hr_hasAttachmentViews), OBJC_ASSOCIATION_RETAIN);
}

- (NSArray<KRTextAttachment *> *)hr_viewAttachments{
    if (self.hr_hasAttachmentViews) {
        NSMutableArray *res = [NSMutableArray array];
        [self enumerateAttribute:NSAttachmentAttributeName inRange:NSMakeRange(0, self.length) options:kNilOptions usingBlock:^(KRTextAttachment *attribute, NSRange range, BOOL *stop) {
            if (attribute && [attribute isKindOfClass:[KRTextAttachment class]] && (attribute.view)) {
                attribute.range = range;
                [res addObject:attribute];
            }
        }];
        return res.count ? res : nil;
    }
    return nil;
}

@end

@implementation NSAttributedString(MIJAsync)
-(KRTextRender *)hr_textRender{
    return objc_getAssociatedObject(self, @selector(hr_textRender));
}

- (void)setHr_textRender:(KRTextRender *)hr_textRender{
    objc_setAssociatedObject(self, @selector(hr_textRender), hr_textRender, OBJC_ASSOCIATION_RETAIN);
}

/// 渲染树惰性构建：CoreText 测量后不再预建 TextKit 渲染树，渲染时按测量元数据还原
/// KRTextRender 并缓存到 attString。调用方线程说明：
/// - 主线程：渲染（drawTextInRect）、点击/长按定位（kr_findSpanIndexWithLocation）；
/// - context 线程：spanRect（attachment 定位）——该场景文本必含 attachment、
///   已由测量阶段的 TextKit fallback 预建渲染树（hr_textRender 非 nil），此处构建
///   分支仅为防御异常路径，正常不会在子线程创建 NSTextStorage。
+ (KRTextRender *)kr_ensureTextRenderForAttributedString:(NSAttributedString *)attString {
    if (![attString isKindOfClass:[NSAttributedString class]]) {
        return nil;
    }
    KRTextRender *render = attString.hr_textRender;
    if (render) {
        return render;
    }
    NSDictionary *meta = attString.hr_measureMeta;
    render = [[KRTextRender alloc] initWithTextStorage:[[NSTextStorage alloc] initWithAttributedString:attString]
                                              lineHeight:[meta[KRMasureMetaLineHeight] doubleValue]];
    render.maximumNumberOfLines = [meta[KRMasureMetaLines] unsignedIntegerValue];
    render.lineBreakMode = [meta[KRMasureMetaMode] unsignedIntegerValue];
    render.lineBreakMargin = [meta[KRMasureMetaMargin] doubleValue];
    render.isBreakLine = [meta[KRMasureMetaIsBreakLine] boolValue];
    // 还原测量时约束宽度到 textContainer（旧路径由 textSizeWithRenderWidth 设置）。
    // 缺失则 span 定位（boundingRectForCharacterRange）/点击定位（characterIndexForPoint）
    // 在 0 宽容器上排版，坐标全错。MAXFLOAT 高度与旧路径一致。
    CGFloat metaWidth = [meta[KRMasureMetaWidth] doubleValue];
    if (metaWidth > 0) {
        render.size = CGSizeMake(metaWidth, MAXFLOAT);
    }
    attString.hr_textRender = render;
    return render;
}
- (CGSize)hr_size{
    return [objc_getAssociatedObject(self, @selector(hr_size)) CGSizeValue];
}

- (void)setHr_size:(CGSize)hr_size{
    objc_setAssociatedObject(self, @selector(hr_size), [NSValue valueWithCGSize:hr_size], OBJC_ASSOCIATION_RETAIN);
}

/// 测量元数据（CoreText 测量后存储，渲染树惰性构建时还原 lineHeight/lines/mode/margin/isBreakLine）
- (NSDictionary *)hr_measureMeta{
    return objc_getAssociatedObject(self, @selector(hr_measureMeta));
}

- (void)setHr_measureMeta:(NSDictionary *)hr_measureMeta{
    objc_setAssociatedObject(self, @selector(hr_measureMeta), hr_measureMeta, OBJC_ASSOCIATION_RETAIN);
}
@end



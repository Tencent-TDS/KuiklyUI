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
    // TextKit 排版的行生成以 textContainer 高度为准：CoreText 测量与 TextKit 排版
    // 存在像素级高度偏差（含附件行最明显），容器高度用测量值会把最后一行挤出
    // glyph 生成范围，表现为换行后的文字不绘制。绘制时放开容器高度让全部行生成，
    // 超出 view 的部分由绘制裁剪兜底；lineBreakMargin 避让区仍按测量高度放末行右下角。
    if (self.textRender.lineBreakMargin > 0 && self.textRender.isBreakLine) {
        UIBezierPath * bezierPath = [UIBezierPath bezierPathWithRect:CGRectMake(rect.size.width - self.textRender.lineBreakMargin, rect.size.height - 10, self.textRender.lineBreakMargin, 10)];
        self.textRender.textContainer.exclusionPaths = @[bezierPath];
    }
    self.textRender.size = CGSizeMake(rect.size.width, CGFLOAT_MAX);
	
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
    // TextKit 不能跑在 Context 子线程：iOS 26 起抛异常，其编辑通知也会引发死锁。
    // 测量统一走 CoreText：图片以 CTRunDelegate 占位，硬换行分段计算。
    // 渲染仍在主线程由 KRTextRender 构建，不受影响。
    attString = [attString isKindOfClass:[NSAttributedString class]] ? attString : [[NSAttributedString alloc] initWithString:@""];
    // 测量副本用完即弃，元数据写回原串供渲染使用
    NSAttributedString *originalString = attString;
    CGSize fitSize = CGSizeZero;
    BOOL isBreakLine = NO;
    BOOL hasAttachment = [self kr_containsAttachment:attString];
    BOOL needPrepare = hasAttachment || [self kr_containsHardLineBreak:attString];

    if (needPrepare) {
        attString = [self kr_prepareCoreTextMeasureString:attString] ?: attString;
    }
    {
        fitSize = [self kr_coreTextSize:attString width:size.width lines:lines mode:mode lineHeight:lineHeight];
        if (marin > 0 && lines) {
            CGSize fullSize = [self kr_coreTextSize:attString width:size.width lines:0 mode:mode lineHeight:lineHeight];
            isBreakLine = !CGSizeEqualToSize(fitSize, fullSize);
        }
    }

    // 渲染树惰性构建时据此还原测量上下文
    originalString.hr_measureMeta = @{
        KRMasureMetaLineHeight: @(lineHeight),
        KRMasureMetaLines: @(lines),
        KRMasureMetaMode: @(mode),
        KRMasureMetaMargin: @(marin),
        KRMasureMetaIsBreakLine: @(isBreakLine),
        KRMasureMetaWidth: @(size.width),
    };
    originalString.hr_size = fitSize;

    // spanRect（Context 线程）依赖 TextKit 渲染树定位附件坐标，坐标必须与渲染同源。
    // 渲染树统一在主线程构建（幂等缓存），子线程不创建 NSTextStorage，规避编辑通知死锁。
    if (hasAttachment && !originalString.hr_textRender) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [NSAttributedString kr_ensureTextRenderForAttributedString:originalString];
        });
    }

    return fitSize;
}

#pragma mark - CoreText measure (deadlock fix)

static NSString *const KRMasureMetaLineHeight = @"lineHeight";
static NSString *const KRMasureMetaLines = @"lines";
static NSString *const KRMasureMetaMode = @"mode";
static NSString *const KRMasureMetaMargin = @"margin";
static NSString *const KRMasureMetaIsBreakLine = @"isBreakLine";
static NSString *const KRMasureMetaWidth = @"width";

/// 是否含 NSTextAttachment（富文本图片混排）
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

/// 是否含硬换行符（\n/\r 等）
+ (BOOL)kr_containsHardLineBreak:(NSAttributedString *)attString {
    if (attString.length == 0) {
        return NO;
    }
    return [attString.string rangeOfCharacterFromSet:[NSCharacterSet newlineCharacterSet]].location != NSNotFound;
}

#pragma mark - CoreText 测量：图片占位

typedef struct {
    CGFloat width;
    CGFloat height;
} KRCTAttachmentMetrics;

static CGFloat kr_ctAttachmentGetWidth(void *refCon) {
    return ((KRCTAttachmentMetrics *)refCon)->width;
}
static CGFloat kr_ctAttachmentGetAscent(void *refCon) {
    return ((KRCTAttachmentMetrics *)refCon)->height;
}
static CGFloat kr_ctAttachmentGetDescent(void *refCon) {
    return 0;
}
static void kr_ctAttachmentDealloc(void *refCon) {
    free(refCon);
}

/// 在测量副本中给图片位置贴宽度占位，原串不变
+ (NSAttributedString *)kr_prepareCoreTextMeasureString:(NSAttributedString *)attString {
    if (attString.length == 0) {
        return attString;
    }
    NSMutableAttributedString *measureCopy = [attString mutableCopy];
    [attString enumerateAttribute:NSAttachmentAttributeName
                          inRange:NSMakeRange(0, attString.length)
                          options:0
                       usingBlock:^(id value, NSRange range, BOOL *stop) {
        if (![value isKindOfClass:[NSTextAttachment class]]) {
            return;
        }
        NSTextAttachment *attachment = (NSTextAttachment *)value;
        CGFloat w = CGRectGetWidth(attachment.bounds);
        CGFloat h = CGRectGetHeight(attachment.bounds);
        if (w <= 0) {
            w = 1;
        }
        if (h <= 0) {
            h = 1;
        }
        CTRunDelegateCallbacks callbacks;
        memset(&callbacks, 0, sizeof(callbacks));
        callbacks.version = kCTRunDelegateVersion1;
        callbacks.getWidth = kr_ctAttachmentGetWidth;
        callbacks.getAscent = kr_ctAttachmentGetAscent;
        callbacks.getDescent = kr_ctAttachmentGetDescent;
        callbacks.dealloc = kr_ctAttachmentDealloc;
        KRCTAttachmentMetrics *metrics = (KRCTAttachmentMetrics *)malloc(sizeof(KRCTAttachmentMetrics));
        metrics->width = w;
        metrics->height = h;
        CTRunDelegateRef runDelegate = CTRunDelegateCreate(&callbacks, metrics);
        if (runDelegate) {
            [measureCopy addAttribute:(__bridge NSString *)kCTRunDelegateAttributeName
                                 value:(__bridge id)runDelegate
                                 range:range];
            CFRelease(runDelegate);
        }
    }];
    return measureCopy;
}

/// 对齐测量宽度与绘制宽度。
///
/// 背景：2.31.0 起 iOS 文本测量改用 CoreText，绘制仍是 TextKit。两套排版引擎对
/// 个别字形的宽度计算不同，导致按测量结果分配的视图宽度比绘制实际需要的窄，
/// 文本尾部被截断。本函数在逐行测量时把宽度修正为 TextKit 的语义。
///
/// 修正一：行尾字形。CoreText 对行尾的全角闭合标点会选用塑形后的替代字形，
/// 其宽度比字体表里的原始宽度小，例如 14pt 系统字体下的右括号是 6.460 对 7.082；
/// 而绘制端 NSLayoutManager 计算 usedRect（已用矩形）时按字体表宽度计。
/// 此处把行尾字形的宽度替换为字体表值，与旧版 TextKit 测量逐值一致，普通字形
/// 两者恒等、无行为变化；负差字符如 』同样替换，保持与旧版一致。
///
/// 修正二：中文弯引号 “ ” ‘ ’。CoreText 在中文上下文里会把弯引号替换成回退
/// 字体的引号字形，TextKit 则保留基础字体的字形，两引擎渲染的字形本身不同，
/// 宽度差无法用替换法对齐，实测 15pt 下差值在 0.662 至 1.584 之间浮动。
/// 此处按每个引号 0.08em 做保守补偿，该值恒大于真实差值，方向安全：最坏情况
/// 文本比需要的宽 1 至 2px，但不会截断。
static CGFloat kr_lineWidthForTextKit(CTLineRef line, CGFloat lineWidth, NSAttributedString *lineString, NSRange charRange) {
    // 中文弯引号保守补偿
    if (charRange.length > 0 && charRange.location + charRange.length <= lineString.length) {
        __block CGFloat quoteCompensation = 0;
        [lineString enumerateAttributesInRange:charRange
                                      options:0
                                   usingBlock:^(NSDictionary<NSAttributedStringKey, id> *attrs, NSRange sub, BOOL *stop) {
            UIFont *font = attrs[NSFontAttributeName];
            if (![font isKindOfClass:[UIFont class]] || sub.length == 0) {
                return;
            }
            NSString *subStr = [lineString.string substringWithRange:sub];
            NSUInteger quoteCount = 0;
            for (NSUInteger i = 0; i < subStr.length; i++) {
                unichar c = [subStr characterAtIndex:i];
                if (c == 0x201C || c == 0x201D || c == 0x2018 || c == 0x2019) {
                    quoteCount++;
                }
            }
            if (quoteCount > 0) {
                quoteCompensation += quoteCount * 0.08 * font.pointSize;
            }
        }];
        lineWidth += quoteCompensation;
    }

    CFArrayRef runs = CTLineGetGlyphRuns(line);
    if (!runs || CFArrayGetCount(runs) == 0) {
        return lineWidth;
    }
    CTRunRef lastRun = (CTRunRef)CFArrayGetValueAtIndex(runs, CFArrayGetCount(runs) - 1);
    CFIndex glyphCount = CTRunGetGlyphCount(lastRun);
    if (glyphCount <= 0) {
        return lineWidth;
    }
    CGGlyph glyph = 0;
    CGSize shapedAdvance = CGSizeZero;
    CTRunGetGlyphs(lastRun, CFRangeMake(glyphCount - 1, 1), &glyph);
    CTRunGetAdvances(lastRun, CFRangeMake(glyphCount - 1, 1), &shapedAdvance);
    NSDictionary *attrs = (__bridge NSDictionary *)CTRunGetAttributes(lastRun);
    CTFontRef font = (__bridge CTFontRef)attrs[(NSString *)kCTFontAttributeName];
    if (font) {
        CGSize fontAdvance = CGSizeZero;
        CTFontGetAdvancesForGlyphs(font, kCTFontOrientationHorizontal, &glyph, &fontAdvance, 1);
        if (fontAdvance.width != shapedAdvance.width) {
            return lineWidth - shapedAdvance.width + fontAdvance.width;
        }
    }
    return lineWidth;
}

/// 纯 CoreText 逐行测量。行高语义对齐 KRTextRender：
/// lineHeight>0 时取强制行高，否则按字体与段落样式。
/// 多段间距与截断省略号宽度按 TextKit 近似。
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
        // 宽度对齐 TextKit 绘制语义，详见 kr_lineWidthForTextKit 注释
        maxWidth = kr_lineWidthForTextKit(fullLine, maxWidth, attString, NSMakeRange(0, (NSUInteger)length));
        CFRelease(fullLine);
        // TextKit clip/truncate 单行语义：usedRect 宽度不超过 container 宽（文本延伸被裁剪）
        maxWidth = MIN(maxWidth, width);
        NSDictionary *attrs = [attString attributesAtIndex:0 effectiveRange:nil];
        lineExtent = [self kr_lineExtentOfLineHeight:lineHeight
                                     paragraphStyle:attrs[NSParagraphStyleAttributeName]
                                       naturalHeight:ascent + descent + leading];
    } else {
        // 多段测量：按 \n 切段逐段断行，跨段累计；段间 paragraphSpacing 对齐 TextKit 语义
        NSArray<NSAttributedString *> *segments = [self kr_splitParagraphs:attString];
        CFIndex index = 0;
        CGFloat totalHeight = 0;
        NSUInteger lineCount = 0;
        CGFloat lastLineSpacing = 0;
        BOOL truncated = NO;
        UIFont *lastLineFont = nil;
        for (NSUInteger segIdx = 0; segIdx < segments.count; segIdx++) {
            NSAttributedString *segment = segments[segIdx];
            if (segment.length == 0) {
                // 空段（连续 \n）：占一行高度，取相邻段字体 metrics
                NSDictionary *attrs = [self kr_emptyParagraphAttributesInSegments:segments atIndex:segIdx];
                NSParagraphStyle *ps = attrs[NSParagraphStyleAttributeName];
                UIFont *font = attrs[NSFontAttributeName] ?: [UIFont systemFontOfSize:[UIFont systemFontSize]];
                CGFloat extent = [self kr_lineExtentOfLineHeight:lineHeight
                                                   paragraphStyle:ps
                                                     naturalHeight:font.lineHeight];
                lastLineSpacing = ps.lineSpacing;
                totalHeight += extent + lastLineSpacing;
                lineCount++;
                lastLineFont = font;
                if (lines > 0 && lineCount >= lines && segIdx + 1 < segments.count) {
                    truncated = YES;
                    break;
                }
                continue;
            }
            CTTypesetterRef segTypesetter = (segments.count == 1)
                ? typesetter
                : CTTypesetterCreateWithAttributedString((CFAttributedStringRef)segment);
            CFIndex segLength = (CFIndex)segment.length;
            CFIndex segIndex = 0;
            while (segIndex < segLength) {
                NSDictionary *attrs = [segment attributesAtIndex:(NSUInteger)segIndex effectiveRange:nil];
                NSParagraphStyle *ps = attrs[NSParagraphStyleAttributeName];
                // 首行缩进（TextKit firstLineHeadIndent 语义）：首行断行宽度扣减，后续行全宽。
                CGFloat lineBreakWidth = (index == 0) ? MAX(width - ps.firstLineHeadIndent, 0.1) : width;
                CFIndex next = CTTypesetterSuggestLineBreak(segTypesetter, segIndex, lineBreakWidth);
                if (next <= 0) {
                    break;
                }
                CTLineRef line = CTTypesetterCreateLine(segTypesetter, CFRangeMake(segIndex, next));
                CGFloat ascent = 0, descent = 0, leading = 0;
                double lineWidth = CTLineGetTypographicBounds(line, &ascent, &descent, &leading);
                // 宽度对齐 TextKit 绘制语义，详见 kr_lineWidthForTextKit 注释。
                // charRange 为段内相对区间，行字形与段属性一一对应
                lineWidth = kr_lineWidthForTextKit(line, (CGFloat)lineWidth, segment, NSMakeRange((NSUInteger)segIndex, (NSUInteger)next));
                CFRelease(line);
                lineExtent = [self kr_lineExtentOfLineHeight:lineHeight
                                            paragraphStyle:ps
                                              naturalHeight:ascent + descent + leading];
                lastLineSpacing = ps.lineSpacing;
                totalHeight += lineExtent + lastLineSpacing;
                maxWidth = MAX(maxWidth, (CGFloat)lineWidth);
                lineCount++;
                index += next;
                segIndex += next;
                lastLineFont = attrs[NSFontAttributeName];
                if (lines > 0 && lineCount >= lines) {
                    break;
                }
            }
            if (segTypesetter != typesetter) {
                CFRelease(segTypesetter);
            }
            // 段末：非最后段补 paragraphSpacing；行数截断发生在段边界也视为截断
            if (segIdx + 1 < segments.count) {
                if (segment.length > 0) {
                    NSDictionary *endAttrs = [segment attributesAtIndex:segment.length - 1 effectiveRange:nil];
                    NSParagraphStyle *endPs = endAttrs[NSParagraphStyleAttributeName];
                    totalHeight += endPs.paragraphSpacing;
                }
                if (lines > 0 && lineCount >= lines) {
                    truncated = YES;
                    break;
                }
            }
        }
        BOOL naturalEnd = !truncated;
        if (naturalEnd) {
            // 自然末行不加行间距（对齐 NSLayoutManager；截断/断行失败的行保留间距）
            totalHeight -= lastLineSpacing;
        }
        // 行数截断且带省略号模式：末行宽度补省略号宽度（对齐 TextKit/fixEllipsis）
        if (truncated && mode == NSLineBreakByTruncatingTail && lastLineFont) {
            CGFloat ellipsisWidth = [@"…" sizeWithAttributes:@{NSFontAttributeName: lastLineFont}].width;
            maxWidth = MAX(maxWidth, MIN(width, (maxWidth > 0 ? maxWidth : 0) + ellipsisWidth));
            maxWidth = MIN(maxWidth, width + ellipsisWidth);
        }
        lineExtent = totalHeight;
    }
    CFRelease(typesetter);
    return CGSizeMake(ceil(maxWidth), ceil(lineExtent));
}

/// 按 \n 切段，保留各段属性
+ (NSArray<NSAttributedString *> *)kr_splitParagraphs:(NSAttributedString *)attString {
    if (attString.length == 0) {
        return @[];
    }
    NSMutableArray<NSAttributedString *> *segments = [NSMutableArray array];
    NSUInteger start = 0;
    NSUInteger length = attString.length;
    while (start < length) {
        NSRange searchRange = NSMakeRange(start, length - start);
        NSRange nlRange = [attString.string rangeOfCharacterFromSet:[NSCharacterSet newlineCharacterSet]
                                                            options:0
                                                              range:searchRange];
        if (nlRange.location == NSNotFound) {
            [segments addObject:[attString attributedSubstringFromRange:NSMakeRange(start, length - start)]];
            break;
        }
        [segments addObject:[attString attributedSubstringFromRange:NSMakeRange(start, nlRange.location - start)]];
        start = nlRange.location + 1;
        if (start == length) {
            // 末尾 \n：补一个空段，保证段数与 TextKit 一致（末空段一般无视觉影响）
            [segments addObject:[[NSAttributedString alloc] initWithString:@""]];
            break;
        }
    }
    return segments;
}

/// 空段行高取相邻首个非空段的属性；全空时用默认字体，避免 attributesAtIndex 越界
+ (NSDictionary *)kr_emptyParagraphAttributesInSegments:(NSArray<NSAttributedString *> *)segments atIndex:(NSUInteger)idx {
    for (NSInteger i = (NSInteger)idx + 1; i < (NSInteger)segments.count; i++) {
        NSAttributedString *s = segments[i];
        if (s.length > 0) {
            return [s attributesAtIndex:0 effectiveRange:nil];
        }
    }
    for (NSInteger i = (NSInteger)idx - 1; i >= 0; i--) {
        NSAttributedString *s = segments[i];
        if (s.length > 0) {
            return [s attributesAtIndex:s.length - 1 effectiveRange:nil];
        }
    }
    return @{NSFontAttributeName: [UIFont systemFontOfSize:[UIFont systemFontSize]]};
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

/// 渲染树惰性构建：CoreText 测量不预建 TextKit 渲染树，按测量元数据还原并缓存。
/// 仅主线程调用：测量后的异步预建、渲染（drawTextInRect）、点击/长按定位。
/// spanRect 在 Context 线程执行，不调用本方法；渲染树未就绪时跳过本次定位。
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



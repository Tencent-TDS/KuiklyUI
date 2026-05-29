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

#import "KRTextAreaView.h"
#import "KRComponentDefine.h"
#import "KRConvertUtil.h"
#import "KRRichTextView.h"
#import "KuiklyRenderBridge.h"
#import "NSObject+KR.h"

// 字典key常量
NSString *const KRFontSizeKey = @"fontSize";
NSString *const KRFontWeightKey = @"fontWeight";

/*
 * @brief 暴露给Kotlin侧调用的多行输入框组件
 */
@interface KRTextAreaView()<UITextViewDelegate>
/** attr is text */
@property (nonatomic, copy, readwrite) NSString *KUIKLY_PROP(text);
/** attr is lineHeight */
@property (nonatomic, copy, readwrite) NSNumber *KUIKLY_PROP(lineHeight);
/** attr is values */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(values);
/** attr is fontSize */
@property (nonatomic, strong)  NSNumber *KUIKLY_PROP(fontSize);
/** attr is fontWeight */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(fontWeight);
#if TARGET_OS_OSX
/** clipPath for macOS - 使用 KUIKLY_PROP 命名规范，仅在 macOS 声明避免覆盖 iOS 上 UIView+CSS category */
@property (nonatomic, copy) NSString *KUIKLY_PROP(clipPath);
#endif

@property (nonatomic, strong)  NSString *KUIKLY_PROP(placeholder);
/** attr is placeholderColor */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(placeholderColor);
/** attr is textAlign */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(textAlign);
/** attr is maxTextLength */
@property (nonatomic, strong)  NSNumber *KUIKLY_PROP(maxTextLength);
/** attr is lengthLimitType */
@property (nonatomic, strong)  NSNumber *KUIKLY_PROP(lengthLimitType);
/** attr is tint color */
@property (nonatomic, strong, readwrite) NSString *KUIKLY_PROP(tintColor);
/** attr is selection color */
@property (nonatomic, strong, readwrite) NSString *KUIKLY_PROP(selectionColor);
/** attr is color */
@property (nonatomic, strong, readwrite) NSString *KUIKLY_PROP(color);
/** attr is editable */
@property (nonatomic, strong, readwrite) NSNumber *KUIKLY_PROP(editable);
/** attr is keyboardType */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(keyboardType);
/** attr is returnKeyType */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(returnKeyType);
/** 是否在点击 IME 动作按钮（如 Send/Go/Search）时自动收起键盘，默认值为 YES，即自动收起，可由业务设置autoHideKeyboardOnImeAction来关闭 */
@property (nonatomic, strong)  NSNumber *KUIKLY_PROP(autoHideKeyboardOnImeAction);
/** event is textDidChange 文本变化 */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(textDidChange);
/** event is inputFocus 获焦 触发 */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(inputFocus);
/** event is inputBlur 失焦 触发 */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(inputBlur);
/** event is keyboardHeightChange 键盘高度变化 */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(keyboardHeightChange);
/** event is textLengthBeyondLimit 输入长度超过限制 */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(textLengthBeyondLimit);
/** event is 用户按下键盘IME动作按键时回调，例如 Send / Go / Search 等 */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(imeAction);
/** event is 用户按下键盘IME动作按键时回调，例如 Send / Go / Search 等 */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(inputReturn);
/** attr is enablePinyinCallback 是否启用拼音输入回调 */
@property (nonatomic, strong)  NSNumber *KUIKLY_PROP(enablePinyinCallback);
/** event is textInputStateChange raw text/selection/composition state change */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(textInputStateChange);
/** event is selectionChange cursor/selection-only change */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(selectionChange);
/** attr is textInputState */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(textInputState);

/** placeholderTextView property */
@property (nullable, nonatomic, strong) UITextView *placeholderTextView;

- (BOOL)p_shouldReapplyTextPostProcessorForIncomingRawText:(NSString *)rawText;
- (BOOL)p_containsShortcodeToken:(NSString *)rawText;
- (BOOL)p_shouldRejectProgrammaticShortcodeInput:(NSString *)rawText;

@end

@implementation KRTextAreaView {
    NSString *_text;
    BOOL _didAddKeyboardNotification;
    NSMutableDictionary *_props;
    BOOL _ignoreTextDidChanged;
    /** 显式设置的光标颜色 */
    UIColor *_cursorColor;
    /** 显式设置的选中高亮颜色 */
    UIColor *_selectionColor;
}

@synthesize hr_rootView;
#if TARGET_OS_OSX
@synthesize css_clipPath = _css_clipPath;
#endif

#pragma mark - init

- (instancetype)init {
    if (self = [super init]) {
        self.delegate = self;
        self.css_autoHideKeyboardOnImeAction = [NSNumber numberWithInt: 1];     // 保持原有能力，默认是关闭关闭软键盘
#if TARGET_OS_OSX // [macOS]
        self.textContainerInset = NSZeroSize;
        // macOS: 启用 layer-backed 支持 clipPath
        self.wantsLayer = YES;
        // 禁用自动调整
        [self setAutoresizingMask:NSViewNotSizable];
        [self setTranslatesAutoresizingMaskIntoConstraints:NO];
        [self setMinSize:NSZeroSize];
        [self setMaxSize:NSMakeSize(CGFLOAT_MAX, CGFLOAT_MAX)];
        // 禁用默认背景和 focus ring
        [self setDrawsBackground:NO];
        [self setFocusRingType:NSFocusRingTypeNone];
#else // [macOS]
        self.textContainerInset = UIEdgeInsetsZero;
#endif // [macOS]
        self.textContainer.lineFragmentPadding = 0;
        self.backgroundColor = [UIColor clearColor];
        _props = [NSMutableDictionary new];
    }
    return self;
}

#pragma mark - dealloc

- (void)dealloc {
    if (_didAddKeyboardNotification) {
        [[NSNotificationCenter defaultCenter] removeObserver:self];
    }
}


#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    if (propKey && propValue) {
        _props[propKey] = propValue;
    }
    KUIKLY_SET_CSS_COMMON_PROP;
    // textInputState may arrive before textPostProcessor; re-run when attachments were lost but raw is unchanged.
    if ([propKey isEqualToString:@"textPostProcessor"] && self.attributedText.length > 0) {
        NSString *rawText = [self p_outputText];
        if ([self p_shouldReapplyTextPostProcessorForIncomingRawText:rawText]) {
            [self p_applyTextPostProcessorIfNeed];
        }
    }
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}

#pragma mark - setter (css property)

- (void)setCss_text:(NSString *)css_text {
    NSString *lastText = self.text ?: @"";
    NSString *newText = css_text ?: @"";
    if (![lastText isEqualToString:newText]) {
        self.text = css_text;
        [self textViewDidChange:self];
        [self updateLineHeightIfApplicable];
        [self p_updatePlaceholder];
    }
}

- (void)setCss_lineHeight:(NSNumber *)css_lineHeight {
    if (_css_lineHeight != css_lineHeight) {
        _css_lineHeight = css_lineHeight;
        [self updateLineHeightIfApplicable];
    }
}

- (void)updateLineHeightIfApplicable {
    if (_css_lineHeight.floatValue <= FLT_EPSILON) {
        return;
    }
    UIFont* font = self.font ?: [UIFont systemFontOfSize:16];
    NSMutableAttributedString *attrStr = [[NSMutableAttributedString alloc] initWithAttributedString:self.attributedText ?:
                                         [[NSAttributedString alloc] initWithString:self.text ?: @""]];
    NSMutableParagraphStyle *paragraphStyle = [[NSMutableParagraphStyle alloc] init];
    paragraphStyle.minimumLineHeight = [_css_lineHeight floatValue];
    paragraphStyle.maximumLineHeight = [_css_lineHeight floatValue];
    paragraphStyle.lineSpacing = ceil(0.2 * _css_fontSize.floatValue);

    NSRange range = NSMakeRange(0, attrStr.length);
    [attrStr addAttribute:NSParagraphStyleAttributeName value:paragraphStyle range:range];
    CGFloat baselineOffset = ([_css_lineHeight floatValue]  - font.pointSize) / 2;
    [attrStr addAttribute:NSBaselineOffsetAttributeName value:@(baselineOffset) range:range];

    self.attributedText = attrStr;
    NSMutableDictionary *typingAttrs = [self.typingAttributes mutableCopy] ?: [NSMutableDictionary dictionary];
    typingAttrs[NSParagraphStyleAttributeName] = paragraphStyle;
    typingAttrs[NSFontAttributeName] = font;
    typingAttrs[NSBaselineOffsetAttributeName] = @(baselineOffset);
    self.typingAttributes = typingAttrs;
}

- (void)setCss_enablesReturnKeyAutomatically:(NSNumber *)flag{
    self.enablesReturnKeyAutomatically = [flag boolValue];
}

- (void)setCss_values:(NSString *)css_values {
    if (_css_values != css_values) {
        _css_values = css_values;
        if (_css_values.length) {
            KRRichTextShadow *textShadow = [KRRichTextShadow new];
            [textShadow hrv_setPropWithKey:@"textPostProcessor" propValue:NSStringFromClass([self class])];
            for (NSString *key in _props.allKeys) {
                [textShadow hrv_setPropWithKey:key propValue:_props[key]];
            }
            // 调用buildAttributedString之前，都需要设置contextParam，预防字体测量的需求
            [textShadow hrv_setPropWithKey:@"contextParam" propValue:self.hr_rootView.contextParam];
            UITextPosition *newPosition = [self positionFromPosition:self.beginningOfDocument offset:self.selectedRange.location];

            self.attributedText =  [textShadow buildAttributedString];;
            self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];

        } else {
            self.attributedText = nil;
        }
        [self p_updatePlaceholder];
        [self textViewDidChange:self];
    }
}

- (void)setCss_tintColor:(NSNumber *)css_tintColor {
    _cursorColor = [UIView css_color:css_tintColor];
#if !TARGET_OS_OSX
    if (!_selectionColor) {
        self.tintColor = _cursorColor;
    } else {
        [self p_applyNativeCursorColorIfNeeded];
    }
#else
    self.tintColor = _cursorColor;
#endif
}

- (void)setCss_selectionColor:(NSNumber *)css_selectionColor {
    _selectionColor = [KRConvertUtil clampSelectionColorAlpha:[UIView css_color:css_selectionColor]];
#if !TARGET_OS_OSX
    if (!_cursorColor) {
        _cursorColor = self.tintColor; // 保存当前光标颜色（可能是默认值）
    }
    self.tintColor = _selectionColor;
    [self tintColorDidChange]; // 确保子视图响应 tintColor 变化
    [self p_applyNativeCursorColorIfNeeded];
#endif
}

- (void)setCss_color:(NSNumber *)css_color {
    self.textColor = [UIView css_color:css_color];
}

- (void)setCss_editable:(NSNumber *)css_editable {
    self.editable = [UIView css_bool:css_editable];
}

- (void)setCss_textAlign:(NSString *)css_textAlign {
    self.textAlignment = [KRConvertUtil NSTextAlignment:css_textAlign];
}

- (void)setCss_fontSize:(NSNumber *)css_fontSize {
    _css_fontSize = css_fontSize;
    self.font = [KRConvertUtil UIFont:@{KRFontSizeKey: css_fontSize ?: @(16),
                                        KRFontWeightKey: _css_fontWeight ?: @"400"}];
    [self setNeedsLayout];
}

- (void)setCss_fontWeight:(NSString *)css_fontWeight {
    _css_fontWeight = css_fontWeight;
    [self setCss_fontSize:_css_fontSize];
}

- (void)setCss_placeholder:(NSString *)css_placeholder {
    _css_placeholder = css_placeholder;
    self.placeholderTextView.text = css_placeholder;
    [self p_updatePlaceholder];
}

- (void)setCss_placeholderColor:(NSString *)css_placeholderColor {
    self.placeholderTextView.textColor = [UIView css_color:css_placeholderColor];
}

- (void)setCss_maxTextLength:(NSNumber *)css_maxTextLength {
    _css_maxTextLength = css_maxTextLength;
}

- (void)setCss_keyboardType:(NSString *)css_keyboardType {
    self.keyboardType = [KRConvertUtil hr_keyBoardType:css_keyboardType];
}

- (void)setCss_returnKeyType:(NSString *)css_returnKeyType {
    _css_returnKeyType = css_returnKeyType;
    self.returnKeyType = [KRConvertUtil hr_toReturnKeyType:css_returnKeyType];
}

- (void)setCss_keyboardHeightChange:(KuiklyRenderCallback)css_keyboardHeightChange {
    _css_keyboardHeightChange = css_keyboardHeightChange;
    [self p_addKeyboardNotificationIfNeed];
}

- (void)setCss_enablePinyinCallback:(NSNumber *)css_enablePinyinCallback {
    _css_enablePinyinCallback = css_enablePinyinCallback;
}

- (void)setCss_autoHideKeyboardOnImeAction:(NSNumber *)css_autoHideKeyboardOnImeAction {
    _css_autoHideKeyboardOnImeAction = css_autoHideKeyboardOnImeAction;
}

#pragma mark - css method

- (void)css_focus:(NSDictionary *)args  {
    dispatch_async(dispatch_get_main_queue(), ^{
        [self becomeFirstResponder];
    });
}

- (void)css_blur:(NSDictionary *)args  {
    [self resignFirstResponder];
}

- (void)css_getCursorIndex:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KRC_CALLBACK_KEY];
    if (callback) {
        NSUInteger cursorIndex = [self p_getOutputCursorIndex];
        callback(@{@"cursorIndex": @(cursorIndex)});
    }


}

- (void)css_setCursorIndex:(NSDictionary *)args {
    NSUInteger index = [args[KRC_PARAM_KEY] intValue];
    [self updateCursorIndex:index];
}

- (void)setCss_textInputState:(NSString *)css_textInputState {
    if (!css_textInputState.length) return;
    [self css_setTextInputState:@{KRC_PARAM_KEY: css_textInputState}];
}

- (void)css_setTextInputState:(NSDictionary *)args {
    NSString *params = args[KRC_PARAM_KEY];
    if (!params.length) return;
    NSData *jsonData = [params dataUsingEncoding:NSUTF8StringEncoding];
    if (!jsonData) return;
    NSError *error = nil;
    NSDictionary *json = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:&error];
    if (!json) return;

    NSString *requestedRawText = json[@"text"] ?: @"";
    NSInteger requestedSelectionStart = json[@"selectionStart"] ? [json[@"selectionStart"] integerValue] : requestedRawText.length;
    NSInteger requestedSelectionEnd = json[@"selectionEnd"] ? [json[@"selectionEnd"] integerValue] : requestedSelectionStart;
    if ([self p_shouldRejectProgrammaticShortcodeInput:requestedRawText]) {
        if (self.css_textLengthBeyondLimit) {
            self.css_textLengthBeyondLimit(@{});
        }
        if (self.css_textInputStateChange) {
            NSString *outputText = [self p_outputText];
            NSRange outputSelectionRange = [self p_getOutputSelectionRange];
            self.css_textInputStateChange(@{
                @"text": outputText ?: @"",
                @"selectionStart": @(outputSelectionRange.location),
                @"selectionEnd": @(NSMaxRange(outputSelectionRange)),
                @"compositionStart": @(-1),
                @"compositionEnd": @(-1),
                @"length": @([self p_calculateLengthForText:outputText])
            });
        }
        return;
    }
    NSString *rawText = [self p_truncateRawTextForProgrammaticInput:requestedRawText];
    // 仅在非 legacy 模式下触发超限回调，legacy 模式（css_lengthLimitType == nil 或 < 0）保持旧行为。
    if (![rawText isEqualToString:requestedRawText] &&
        self.css_textLengthBeyondLimit &&
        self.css_lengthLimitType != nil &&
        [self.css_lengthLimitType integerValue] >= 0) {
        self.css_textLengthBeyondLimit(@{});
    }
    NSInteger selectionStart = MAX(0, MIN(requestedSelectionStart, (NSInteger)rawText.length));
    NSInteger selectionEnd = MAX(0, MIN(requestedSelectionEnd, (NSInteger)rawText.length));

    if (![self isFirstResponder] && rawText.length > 0) {
        [self becomeFirstResponder];
    }
    _ignoreTextDidChanged = YES;
    NSString *currentRawText = [self p_outputText];
    BOOL textChanged = ![currentRawText isEqualToString:rawText];
    BOOL needsPostProcessor = textChanged || [self p_shouldReapplyTextPostProcessorForIncomingRawText:rawText];
    if (textChanged) {
        NSMutableAttributedString *rawAttr = [[NSMutableAttributedString alloc] initWithString:rawText];
        UIFont *font = self.font ?: self.typingAttributes[NSFontAttributeName];
        if (font) {
            [rawAttr addAttribute:NSFontAttributeName value:font range:NSMakeRange(0, rawAttr.length)];
        }
        UIColor *textColor = self.textColor ?: self.typingAttributes[NSForegroundColorAttributeName];
        if (textColor) {
            [rawAttr addAttribute:NSForegroundColorAttributeName value:textColor range:NSMakeRange(0, rawAttr.length)];
        }
        self.attributedText = rawAttr;
        [self p_updatePlaceholder];
    }
    if (needsPostProcessor) {
        [self p_applyTextPostProcessorIfNeed];
    }
    NSUInteger inputCursorStart = [self p_getInputCursorIndexWithIndex:selectionStart];
    NSUInteger inputCursorEnd = [self p_getInputCursorIndexWithIndex:selectionEnd];
    NSRange selectedRange = NSMakeRange(inputCursorStart, inputCursorEnd - inputCursorStart);
    UITextPosition *startPos = [self positionFromPosition:self.beginningOfDocument offset:selectedRange.location];
    UITextPosition *endPos = [self positionFromPosition:self.beginningOfDocument offset:selectedRange.location + selectedRange.length];
    if (startPos && endPos) {
        self.selectedTextRange = [self textRangeFromPosition:startPos toPosition:endPos];
    }
    _ignoreTextDidChanged = NO;

    // 触发 textInputStateChange 回调，通知长度变化
    if (self.css_textInputStateChange) {
        NSString *outputText = [self p_outputText];
        NSRange outputSelectionRange = [self p_getOutputSelectionRange];
        self.css_textInputStateChange(@{
            @"text": outputText ?: @"",
            @"selectionStart": @(outputSelectionRange.location),
            @"selectionEnd": @(NSMaxRange(outputSelectionRange)),
            @"compositionStart": @(-1),
            @"compositionEnd": @(-1),
            @"length": @([self p_calculateLengthForText:outputText])
        });
    }
}

- (void)css_getTextInputState:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KRC_CALLBACK_KEY];
    if (callback) {
        NSString *rawText = [self p_outputText];
        NSRange outputSelectionRange = [self p_getOutputSelectionRange];
        callback(@{
            @"text": rawText ?: @"",
            @"selectionStart": @(outputSelectionRange.location),
            @"selectionEnd": @(NSMaxRange(outputSelectionRange)),
            @"compositionStart": @(-1),
            @"compositionEnd": @(-1),
            @"length": @([self p_calculateLengthForText:rawText])
        });
    }
}

- (void)updateCursorIndex:(NSUInteger)index {
    index = [self p_getInputCursorIndexWithIndex:index];
    UITextPosition *newPosition = [self positionFromPosition:self.beginningOfDocument offset:index];
    _ignoreTextDidChanged = YES;
    self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
    _ignoreTextDidChanged = NO;
}

- (void)css_setText:(NSDictionary *)args {
    NSString *text = args[KRC_PARAM_KEY];
    self.text = text;
    [self textViewDidChange:self];
}

- (void)css_getInnerContentHeight:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KRC_CALLBACK_KEY];
    if (callback) {
#if !TARGET_OS_OSX // [macOS]
        CGFloat contentHeight = self.contentSize.height;
#else // [macOS
        // On macOS, calculate content height from layout manager
        NSLayoutManager *layoutManager = self.layoutManager;
        NSTextContainer *textContainer = self.textContainer;
        [layoutManager ensureLayoutForTextContainer:textContainer];
        CGFloat contentHeight = [layoutManager usedRectForTextContainer:textContainer].size.height;
#endif // macOS]
        callback(@{@"height": @(contentHeight)});
    }
}

- (BOOL)becomeFirstResponder {
    BOOL result = [super becomeFirstResponder];
#if !TARGET_OS_OSX
    if (result && _cursorColor && _selectionColor) {
        if (@available(iOS 17.0, *)) {
            // iOS 17+ 已通过 insertionPointColor 设置光标颜色，无需手动修复
        } else {
            // 延迟到下一个 runloop，确保光标视图已创建后再修复颜色
            dispatch_async(dispatch_get_main_queue(), ^{
                [self p_restoreCursorColorInView:self];
            });
        }
    }
#endif
    return result;
}

- (void)layoutSubviews {
    [super layoutSubviews];
    if (_placeholderTextView.font != self.font) {
        _placeholderTextView.font = self.font;
    }
#if !TARGET_OS_OSX
    if (_cursorColor && _selectionColor) {
        if (@available(iOS 17.0, *)) {
            // iOS 17+ 已通过 insertionPointColor 设置光标颜色，无需手动遍历修复
        } else {
            [self p_restoreCursorColorInView:self];
        }
    }
#endif
}

/// 遍历子视图，找到光标视图并恢复其颜色
#if !TARGET_OS_OSX
- (void)p_restoreCursorColorInView:(UIView *)view {
    NSString *className = NSStringFromClass([view class]);
    if ([className containsString:@"TextCursor"] || [className containsString:@"CursorView"] || [className containsString:@"Caret"]) {
        view.backgroundColor = _cursorColor;
        view.tintColor = _cursorColor;
        return;
    }
    for (UIView *subview in view.subviews) {
        [self p_restoreCursorColorInView:subview];
    }
}
#endif

#if TARGET_OS_OSX
- (void)layout {
    CGRect savedFrame = self.frame;
    [super layout];
    if (!CGRectEqualToRect(self.frame, savedFrame)) {
        [super setFrame:savedFrame];
    }
}

- (void)setCss_clipPath:(NSString *)css_clipPath {
    _css_clipPath = [css_clipPath copy];
    // 禁用默认 mask，使用 drawRect 裁剪
    self.layer.mask = nil;
    [self setNeedsDisplay:YES];
}

- (NSString *)css_clipPath {
    return _css_clipPath;
}

- (void)drawRect:(NSRect)dirtyRect {
    // 隐藏 placeholder 避免干扰
    if (_placeholderTextView) {
        _placeholderTextView.hidden = YES;
    }

    // 隐藏 CSSBorderLayer，我们将在下面自己绘制边框
    for (CALayer *layer in self.layer.sublayers) {
        if ([NSStringFromClass([layer class]) isEqualToString:@"CSSBorderLayer"]) {
            layer.hidden = YES;
        }
    }

    if (_css_clipPath.length > 0) {
        // 先保存图形状态
        [NSGraphicsContext saveGraphicsState];

        // 解析 clipPath
        NSBezierPath *clipPath = [self kr_bezierPathFromClipPathString:_css_clipPath];
        if (clipPath) {
            // 应用 clipPath 裁剪内容
            [clipPath addClip];
            // 绘制边框（在 clipPath 之前，这样不会被裁剪）
            [self drawBorderWithClipPath:clipPath];
        }

        // 调用父类绘制（在裁剪区域内）
        [super drawRect:dirtyRect];

        // 恢复图形状态
        [NSGraphicsContext restoreGraphicsState];
    } else {
        [super drawRect:dirtyRect];
    }
}

- (void)drawBorderWithClipPath:(NSBezierPath *)clipPath {
    // 解析 border 属性
    if (self.css_border.length > 0) {
        NSArray *borderParts = [self.css_border componentsSeparatedByString:@" "];
        if (borderParts.count >= 3) {
            CGFloat borderWidth = [borderParts[0] floatValue];
            NSString *borderColorStr = borderParts[2];
            UIColor *borderColor = [UIView css_color:borderColorStr];

            // 绘制边框（stroke）
            [borderColor setStroke];
            // 对于 NSBezierPath stroke，使用实际的 borderWidth
            [clipPath setLineWidth:2 * borderWidth];
            [clipPath stroke];
        }
    }
}

- (NSBezierPath *)kr_bezierPathFromClipPathString:(NSString *)pathString {
    NSBezierPath *path = [NSBezierPath bezierPath];
    NSArray *tokens = [pathString componentsSeparatedByCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    NSMutableArray *cleanTokens = [NSMutableArray array];
    for (NSString *token in tokens) {
        if (token.length > 0) {
            [cleanTokens addObject:token];
        }
    }

    NSInteger i = 0;
    while (i < cleanTokens.count) {
        NSString *token = cleanTokens[i];

        if ([token isEqualToString:@"M"]) {
            if (i + 2 < cleanTokens.count) {
                CGFloat x = [cleanTokens[i + 1] floatValue];
                CGFloat y = [cleanTokens[i + 2] floatValue];
                [path moveToPoint:NSMakePoint(x, y)];
                i += 3;
            } else {
                i++;
            }
        } else if ([token isEqualToString:@"L"]) {
            if (i + 2 < cleanTokens.count) {
                CGFloat x = [cleanTokens[i + 1] floatValue];
                CGFloat y = [cleanTokens[i + 2] floatValue];
                [path lineToPoint:NSMakePoint(x, y)];
                i += 3;
            } else {
                i++;
            }
        } else if ([token isEqualToString:@"Z"] || [token isEqualToString:@"z"]) {
            [path closePath];
            i++;
        } else {
            if (i + 1 < cleanTokens.count) {
                unichar firstChar = [token characterAtIndex:0];
                if ((firstChar >= '0' && firstChar <= '9') || firstChar == '-' || firstChar == '+') {
                    CGFloat x = [token floatValue];
                    CGFloat y = [cleanTokens[i + 1] floatValue];
                    [path lineToPoint:NSMakePoint(x, y)];
                    i += 2;
                    continue;
                }
            }
            i++;
        }
    }
    return path;
}
#endif


#pragma mark - UITextViewDelegate

- (void)textViewDidChange:(UITextView *)textView { // 文本值变化
    if (_ignoreTextDidChanged) {
        return ;
    }
    [self p_updatePlaceholder];
    // 如果有拼音输入，根据配置决定是否触发回调
    if (textView.markedTextRange) {
        BOOL enablePinyinCallback = [self.css_enablePinyinCallback boolValue];
        if (enablePinyinCallback) {
            if (self.css_textDidChange) {
                NSString *text = [self p_outputText].copy ?: @"";
                self.css_textDidChange(@{@"text": text, @"length": @([self p_calculateLengthForText:text])});
            }
        }
        return;
    }
    [self p_limitTextInput];
    // 实时应用 textPostProcessor（emoji attachment）
    [self p_applyTextPostProcessorIfNeed];

    if (self.css_textDidChange) {
        NSString *text = [self p_outputText].copy ?: @"";
        self.css_textDidChange(@{@"text": text, @"length": @([self p_calculateLengthForText:text])});
    }

    if (self.css_textInputStateChange) {
        NSString *rawText = [self p_outputText];
        NSRange outputSelectionRange = [self p_getOutputSelectionRange];
        self.css_textInputStateChange(@{
            @"text": rawText ?: @"",
            @"selectionStart": @(outputSelectionRange.location),
            @"selectionEnd": @(NSMaxRange(outputSelectionRange)),
            @"compositionStart": @(-1),
            @"compositionEnd": @(-1),
            @"length": @([self p_calculateLengthForText:rawText])
        });
    }
}

- (void)textViewDidChangeSelection:(UITextView *)textView {
    if (_ignoreTextDidChanged) {
        return;
    }
    if (!self.css_selectionChange) {
        return;
    }
    NSString *rawText = [self p_outputText];
    NSRange outputSelectionRange = [self p_getOutputSelectionRange];
    self.css_selectionChange(@{
        @"text": rawText ?: @"",
        @"selectionStart": @(outputSelectionRange.location),
        @"selectionEnd": @(NSMaxRange(outputSelectionRange)),
        @"compositionStart": @(-1),
        @"compositionEnd": @(-1)
    });
}

- (void)copy:(id)sender {
    NSRange selectedRange = self.selectedRange;
    if (selectedRange.length == 0) {
        [super copy:sender];
        return;
    }
    NSAttributedString *selectedAttr = [self.attributedText attributedSubstringFromRange:selectedRange];
    if (!selectedAttr) {
        [super copy:sender];
        return;
    }
    NSMutableArray *replacements = [NSMutableArray array];
    [selectedAttr enumerateAttribute:NSAttachmentAttributeName
                             inRange:NSMakeRange(0, selectedAttr.length)
                             options:0
                          usingBlock:^(NSObject *value, NSRange range, BOOL *stop) {
        if ([value respondsToSelector:@selector(kr_originlTextBeforeTextAttachment)]) {
            id<KRTextAttachmentStringProtocol> attachment = (id<KRTextAttachmentStringProtocol>)value;
            NSString *shortcode = [attachment kr_originlTextBeforeTextAttachment];
            if (shortcode) {
                [replacements addObject:@{@"range": [NSValue valueWithRange:range], @"shortcode": shortcode}];
            }
        }
    }];
    NSMutableAttributedString *exportAttr = [selectedAttr mutableCopy];
    for (NSDictionary *item in [replacements reverseObjectEnumerator]) {
        NSRange range = [item[@"range"] rangeValue];
        NSString *shortcode = item[@"shortcode"];
        NSDictionary *attrs = [exportAttr attributesAtIndex:range.location effectiveRange:NULL];
        NSAttributedString *replacement = [[NSAttributedString alloc] initWithString:shortcode attributes:attrs];
        [exportAttr replaceCharactersInRange:range withAttributedString:replacement];
    }
    [UIPasteboard generalPasteboard].string = exportAttr.string;
}

- (void)cut:(id)sender {
    [self copy:sender];
    NSRange selectedRange = self.selectedRange;
    if (selectedRange.length > 0) {
        _ignoreTextDidChanged = YES;
        [self.textStorage deleteCharactersInRange:selectedRange];
        _ignoreTextDidChanged = NO;
        [self textViewDidChange:self];
    }
}

- (void)paste:(id)sender {
    NSString *pasteText = UIPasteboard.generalPasteboard.string;
    if (pasteText.length == 0) {
        [super paste:sender];
        return;
    }
    NSRange rawSelectedRange = [self p_getOutputRangeWithInputRange:self.selectedRange];
    NSString *rawText = [self p_outputText] ?: @"";
    NSString *candidateRawText = [rawText stringByReplacingCharactersInRange:rawSelectedRange
                                                                  withString:pasteText];
    NSString *newRawText = [self p_truncateRawTextForProgrammaticInput:candidateRawText];
    BOOL truncated = ![newRawText isEqualToString:candidateRawText];
    NSUInteger outputCursor = MIN(rawSelectedRange.location + pasteText.length, newRawText.length);
    _ignoreTextDidChanged = YES;
    NSMutableAttributedString *rawAttr = [[NSMutableAttributedString alloc] initWithString:newRawText];
    UIFont *font = self.font ?: self.typingAttributes[NSFontAttributeName];
    if (font) {
        [rawAttr addAttribute:NSFontAttributeName value:font range:NSMakeRange(0, rawAttr.length)];
    }
    UIColor *textColor = self.textColor ?: self.typingAttributes[NSForegroundColorAttributeName];
    if (textColor) {
        [rawAttr addAttribute:NSForegroundColorAttributeName value:textColor range:NSMakeRange(0, rawAttr.length)];
    }
    self.attributedText = rawAttr;
    [self p_applyTextPostProcessorIfNeed];
    NSUInteger inputCursor = [self p_getInputCursorIndexWithIndex:outputCursor];
    self.selectedRange = NSMakeRange(inputCursor, 0);
    _ignoreTextDidChanged = NO;
    [self p_updatePlaceholder];
    if (truncated && self.css_textLengthBeyondLimit) {
        self.css_textLengthBeyondLimit(@{});
    }
    if (self.css_textDidChange) {
        self.css_textDidChange(@{@"text": newRawText, @"length": @([self p_calculateLengthForText:newRawText])});
    }
    if (self.css_textInputStateChange) {
        NSRange outputSelectionRange = [self p_getOutputSelectionRange];
        self.css_textInputStateChange(@{
            @"text": newRawText,
            @"selectionStart": @(outputSelectionRange.location),
            @"selectionEnd": @(NSMaxRange(outputSelectionRange)),
            @"compositionStart": @(-1),
            @"compositionEnd": @(-1),
            @"length": @([self p_calculateLengthForText:newRawText])
        });
    }
    [self scrollRangeToVisible:self.selectedRange];
}

- (BOOL)textView:(UITextView *)textView shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text {
    if (_ignoreTextDidChanged) {
        return  NO;
    }
    if (text == nil || [text isEqualToString:@""]) { // 删除操作
        return YES;
            // It's a delete operation
            // Perform your desired action for delete operation here
    }
    if ([text isEqualToString:@"\n"]) {
#if TARGET_OS_OSX
        // macOS: Enter（无 Shift）触发 inputReturn 回调（发送），Shift+Enter 插入换行。
        // 不要求 css_returnKeyType 必须存在——macOS 上多行 TextField 的 imeAction
        // 通常是 Unspecified，不会设置 returnKeyType，但 Enter 键仍应触发 send。
        NSEventModifierFlags modifiers = [NSEvent modifierFlags];
        BOOL isShiftPressed = (modifiers & NSEventModifierFlagShift) != 0;
        if (!isShiftPressed && self.css_inputReturn) {
            NSString *imeAction = self.css_returnKeyType ?: @"send";
            self.css_inputReturn(@{@"text": textView.text.copy ?: @"", @"ime_action": imeAction});
            if ([self.css_autoHideKeyboardOnImeAction boolValue]) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [textView resignFirstResponder];
                });
            }
            return NO;
        }
        if (!isShiftPressed && self.css_imeAction) {
            self.css_imeAction(@{@"ime_action": self.css_returnKeyType ?: @"send"});
            if ([self.css_autoHideKeyboardOnImeAction boolValue]) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [textView resignFirstResponder];
                });
            }
            return NO;
        }
        // Shift+Enter 或无回调注册时：允许插入换行（fall through to return YES）
#else
        // iOS: 保持原有逻辑——需要 returnKeyType 显式设置才拦截回车
        if (self.css_inputReturn && self.css_returnKeyType) {
            self.css_inputReturn(@{@"text": textView.text.copy ?: @"", @"ime_action": self.css_returnKeyType ?: @""});
            if ([self.css_autoHideKeyboardOnImeAction boolValue]) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [textView resignFirstResponder];
                });
            }
            return NO;
        }
        if (self.css_imeAction) {
            self.css_imeAction(@{@"ime_action": self.css_returnKeyType ?: @""});
            if ([self.css_autoHideKeyboardOnImeAction boolValue]) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [textView resignFirstResponder];
                });
            }
            return NO;
        }
#endif
    }

    // legacy 模式（css_lengthLimitType == nil 或 < 0）走 p_limitTextInput 后置截断，不在此拦截
    if (self.css_lengthLimitType == nil || [self.css_lengthLimitType integerValue] < 0) {
        return YES;
    }

    // 检查长度限制
    NSInteger maxLength = [self.css_maxTextLength integerValue];
    if (maxLength > 0) {
        NSString *currentRawText = [self p_outputText] ?: @"";
        NSUInteger rawStart = [self p_getOutputCursorIndexWithInputIndex:range.location];
        NSUInteger rawEnd = [self p_getOutputCursorIndexWithInputIndex:range.location + range.length];
        rawStart = MIN(rawStart, currentRawText.length);
        rawEnd = MIN(rawEnd, currentRawText.length);
        if (rawStart > rawEnd) {
            NSUInteger temp = rawStart;
            rawStart = rawEnd;
            rawEnd = temp;
        }
        NSString *newRawText = [currentRawText stringByReplacingCharactersInRange:NSMakeRange(rawStart, rawEnd - rawStart)
                                                                        withString:text];
        NSUInteger newLength = [self p_calculateLengthForText:newRawText];
        if (newLength > maxLength) {
            if (self.css_textLengthBeyondLimit) {
                self.css_textLengthBeyondLimit(@{});
            }
            return NO; // 超出长度限制，阻止输入
        }
    }

    return YES;
}

- (void)textViewDidBeginEditing:(UITextView *)textView { // 获焦
    if (self.css_inputFocus) {
        self.css_inputFocus(@{@"text": textView.text.copy ?: @""});
    }
}


- (void)textViewDidEndEditing:(UITextView *)textView{ // 失焦
    if (self.css_inputBlur) {
        self.css_inputBlur(@{@"text": textView.text.copy ?: @""});
    }
}

#pragma mark - notication

#if !TARGET_OS_OSX // [macOS]
- (void)onReceivekeyboardWillShowNotification:(NSNotification *)notify {
    NSDictionary *info = notify.userInfo;
    CGFloat keyboardHeight = [[info objectForKey:UIKeyboardFrameEndUserInfoKey] CGRectValue].size.height;
    CGFloat duration = [[info objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];
    NSInteger curve = [[info objectForKey:UIKeyboardAnimationCurveUserInfoKey] integerValue];
    if (self.css_keyboardHeightChange) {
        self.css_keyboardHeightChange(@{@"height": @(keyboardHeight), @"duration": @(duration), @"curve": @(curve)});
    }
}

- (void)onReceivekeyboardWillHideNotification:(NSNotification *)notify {
    NSDictionary *info = notify.userInfo;
    CGFloat duration = [[info objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];
    NSInteger curve = [[info objectForKey:UIKeyboardAnimationCurveUserInfoKey] integerValue];
    if (self.css_keyboardHeightChange) {
        self.css_keyboardHeightChange(@{@"height": @(0), @"duration": @(duration), @"curve": @(curve)});
    }
}
#endif // [macOS]

#pragma mark - override

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    [self setNeedsLayout];
    _placeholderTextView.frame = self.bounds;
#if TARGET_OS_OSX
    // 设置 textContainer 防止自动调整
    if (self.textContainer) {
        self.textContainer.containerSize = NSSizeFromCGSize(frame.size);
        self.textContainer.widthTracksTextView = NO;
        self.textContainer.heightTracksTextView = NO;
    }
    [self setNeedsDisplay:YES];
#endif
}

- (void)setFont:(UIFont *)font {
    [super setFont:font];
    _placeholderTextView.font = font;
}

- (void)setTextAlignment:(NSTextAlignment)textAlignment {
    [super setTextAlignment:textAlignment];
    _placeholderTextView.textAlignment = textAlignment;
}


#pragma mark - private

/// iOS 17+ 使用公开属性 insertionPointColor 独立设置光标颜色，避免与 tintColor（选中高亮色）冲突
#if !TARGET_OS_OSX
- (void)p_applyNativeCursorColorIfNeeded {
    if (!_cursorColor) return;
    if (@available(iOS 17.0, *)) {
        SEL sel = NSSelectorFromString(@"setInsertionPointColor:");
        if ([self respondsToSelector:sel]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [self performSelector:sel withObject:_cursorColor];
#pragma clang diagnostic pop
        }
    }
}
#endif

- (void)p_addKeyboardNotificationIfNeed {
    if (_didAddKeyboardNotification) {
        return ;
    }
#if !TARGET_OS_OSX // [macOS]
    _didAddKeyboardNotification = YES;
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onReceivekeyboardWillShowNotification:)
                                                 name:UIKeyboardWillShowNotification
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onReceivekeyboardWillHideNotification:)
                                                 name:UIKeyboardWillHideNotification
                                               object:nil];
#else // [macOS
    // macOS doesn't have software keyboard, so no notification needed
    _didAddKeyboardNotification = YES;
#endif // macOS]
}

- (void)p_updatePlaceholder {
    _placeholderTextView.hidden = self.text.length > 0 || self.attributedText.length > 0;
    if (self.markedTextRange) { // 输入中
        _placeholderTextView.hidden = YES;
    }
}

- (void)p_limitTextInput {
    UITextView *textView = self;
    // 判断是否存在高亮字符，不进行字数统计和字符串截断
    UITextRange *selectedRange = textView.markedTextRange;
    UITextPosition *position = [textView positionFromPosition:selectedRange.start offset:0];
    if (position) {
        return;
    }
    NSInteger maxLength;
    if (self.css_lengthLimitType == nil || [self.css_lengthLimitType integerValue] < 0) { // 兼容旧版本行为
        maxLength = [self p_legacyMaxInputLengthWithString:textView.attributedText.string];
    } else {
        maxLength = [self.css_maxTextLength integerValue];
    }

    if (maxLength <= 0) {
        return;
    }

    BOOL shouldTruncate = [self p_shouldTruncate:textView.attributedText maxLength:maxLength];
    if (shouldTruncate) {
        if (textView.attributedText) {

            NSUInteger location = self.selectedRange.location;

            NSMutableAttributedString *truncatedAttributedString = [textView.attributedText mutableCopy];
            NSUInteger atIndex = MAX(location - 1, 0);
            NSUInteger deleteLength = 0;

            while ([self p_shouldTruncate:truncatedAttributedString maxLength:maxLength] && (atIndex < truncatedAttributedString.length && atIndex >= 0)) {
                NSRange composedRange = [truncatedAttributedString.string rangeOfComposedCharacterSequenceAtIndex:atIndex]; // 避免切割emoji
                if (composedRange.length == 0) {
                    break;
                }
                [truncatedAttributedString deleteCharactersInRange:composedRange];

                atIndex = composedRange.location -1;
                deleteLength += composedRange.length;
            }
            BOOL truncatedTail = NO;
            while (truncatedAttributedString.length > 0 && [self p_shouldTruncate:truncatedAttributedString maxLength:maxLength]) {
                NSRange range = [truncatedAttributedString.string rangeOfComposedCharacterSequenceAtIndex:truncatedAttributedString.length - 1];
                if (range.length == 0) {
                    break;
                }
                [truncatedAttributedString deleteCharactersInRange:range];
                truncatedTail = YES;
            }
            if (truncatedTail) {
                location = truncatedAttributedString.length;
                deleteLength = 0;
            }

            textView.attributedText = truncatedAttributedString;
            NSUInteger newOffset = MIN(MAX(location - deleteLength, 0), truncatedAttributedString.length);
            UITextPosition *newPosition = [self positionFromPosition:self.beginningOfDocument offset:newOffset];

            if (newPosition) {
                _ignoreTextDidChanged = YES;
                self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
                _ignoreTextDidChanged = NO;

                dispatch_async(dispatch_get_main_queue(), ^{
                    self->_ignoreTextDidChanged = YES;
                    self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
                    self->_ignoreTextDidChanged = NO;
                });
            }
        }

        if (self.css_textLengthBeyondLimit) {
            self.css_textLengthBeyondLimit(@{});
        }
    }
}

- (NSUInteger)p_legacyMaxInputLengthWithString:(NSString *)string {
    NSInteger maxLength = [self.css_maxTextLength intValue];
    if (maxLength <= 0) {
        return 0;
    }
    NSUInteger count = 0;
    NSUInteger length = string.length;
    NSUInteger i = 0;
    for (; i < length; ) {
        NSRange range = [string rangeOfComposedCharacterSequenceAtIndex:i];
        count++;
        i += range.length;
        if (count >= maxLength)  {
            break;
        }
    }

    return MAX(i, maxLength);
}

- (BOOL)p_containsShortcodeToken:(NSString *)rawText {
    if (rawText.length == 0) {
        return NO;
    }
    static NSRegularExpression *tokenRegex = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        tokenRegex = [NSRegularExpression regularExpressionWithPattern:@"\\[[a-zA-Z0-9_\\-]+\\]" options:0 error:nil];
    });
    NSRange fullRange = NSMakeRange(0, rawText.length);
    return [tokenRegex firstMatchInString:rawText options:0 range:fullRange] != nil;
}

- (BOOL)p_shouldRejectProgrammaticShortcodeInput:(NSString *)rawText {
    if (self.css_lengthLimitType == nil || [self.css_lengthLimitType integerValue] < 0) {
        return NO;
    }
    NSInteger maxLength = [self.css_maxTextLength integerValue];
    if (maxLength <= 0 || ![self p_containsShortcodeToken:rawText]) {
        return NO;
    }
    return [self p_calculateLengthForText:rawText] > maxLength;
}

- (NSString *)p_truncateRawTextForProgrammaticInput:(NSString *)rawText {
    if (rawText.length == 0) {
        return @"";
    }
    NSInteger maxLength = [self.css_maxTextLength integerValue];
    if (maxLength <= 0) {
        return rawText;
    }
    if ([self p_calculateLengthForText:rawText] <= maxLength) {
        return rawText;
    }

    NSError *regexError = nil;
    NSRegularExpression *tokenRegex = [NSRegularExpression regularExpressionWithPattern:@"\\[[a-zA-Z0-9_\\-]+\\]" options:0 error:&regexError];
    NSMutableString *truncatedText = [NSMutableString string];
    NSUInteger index = 0;
    while (index < rawText.length) {
        NSRange remainingRange = NSMakeRange(index, rawText.length - index);
        NSTextCheckingResult *tokenMatch = regexError ? nil : [tokenRegex firstMatchInString:rawText options:NSMatchingAnchored range:remainingRange];
        NSRange unitRange;
        if (tokenMatch && tokenMatch.range.location == index) {
            unitRange = tokenMatch.range;
        } else {
            unitRange = [rawText rangeOfComposedCharacterSequenceAtIndex:index];
        }
        NSString *unit = [rawText substringWithRange:unitRange];
        NSString *candidate = [truncatedText stringByAppendingString:unit];
        if ([self p_calculateLengthForText:candidate] > maxLength) {
            break;
        }
        [truncatedText appendString:unit];
        index = NSMaxRange(unitRange);
    }
    return truncatedText;
}

- (NSUInteger)p_calculateLengthForText:(NSString *)text {
    if (self.css_lengthLimitType == nil || [self.css_lengthLimitType integerValue] < 0) {
        // 兼容旧版本行为
        return [text kr_length];
    }
    switch ([self.css_lengthLimitType integerValue]) {
        case 0: // BYTE
            return [text kr_byteLength];
        case 2: { // VIRSUAL_WIDTH
            NSAttributedString *processedAttributedText = [self p_processedAttributedTextForLengthCalculationWithRawText:text];
            return [processedAttributedText.string kr_visualWidth];
        }
        case 1: { // CHARACTER
            NSAttributedString *processedAttributedText = [self p_processedAttributedTextForLengthCalculationWithRawText:text];
            return [self p_calculateCharacterLengthForAttributedText:processedAttributedText];
        }
        default:
            return [text kr_length];
    }
}

// CHARACTER 模式：将 NSTextAttachment 算作 1 个字符，与 Android ReplacementSpan 行为一致
- (NSUInteger)p_calculateCharacterLength:(NSString *)text {
    if (text.length == 0) {
        return 0;
    }

    NSAttributedString *attributedText = self.attributedText;

    // 如果没有 attributedText 或者传入的 text 与当前不同，
    // 则需要基于传入的 text 计算，同时估算 emoji placeholder 的数量
    if (attributedText.length == 0 || ![text isEqualToString:attributedText.string]) {
        // 计算 emoji placeholder 的数量（[xxx] 格式）
        // 每个 placeholder 在显示时会变成 NSTextAttachment，算 1 个字符
        NSUInteger emojiCount = 0;
        NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"\\[[a-zA-Z0-9_\\-]+\\]" options:0 error:nil];
        emojiCount = [regex numberOfMatchesInString:text options:0 range:NSMakeRange(0, text.length)];

        // 移除 emoji placeholder 后的纯文本长度
        NSString *plainText = [regex stringByReplacingMatchesInString:text options:0 range:NSMakeRange(0, text.length) withTemplate:@""];

        NSUInteger length = emojiCount + [plainText kr_length];
        return length;
    }

    // attributedText 与传入的 text 一致，直接计算 attachment
    NSUInteger length = 0;
    NSUInteger index = 0;
    NSMutableArray<NSValue *> *attachmentRanges = [NSMutableArray array];

    // 收集所有 attachment 的范围
    [attributedText enumerateAttribute:NSAttachmentAttributeName
                               inRange:NSMakeRange(0, attributedText.length)
                               options:0
                            usingBlock:^(id value, NSRange range, BOOL *stop) {
        if (value != nil) {
            [attachmentRanges addObject:[NSValue valueWithRange:range]];
        }
    }];

    // 按位置排序
    [attachmentRanges sortUsingComparator:^NSComparisonResult(NSValue *obj1, NSValue *obj2) {
        NSRange r1 = [obj1 rangeValue];
        NSRange r2 = [obj2 rangeValue];
        if (r1.location < r2.location) return NSOrderedAscending;
        if (r1.location > r2.location) return NSOrderedDescending;
        return NSOrderedSame;
    }];

    // 计算长度：普通文本用 kr_length，每个 attachment 算 1
    for (NSValue *rangeValue in attachmentRanges) {
        NSRange range = [rangeValue rangeValue];
        if (index < range.location) {
            NSString *substring = [attributedText.string substringWithRange:NSMakeRange(index, range.location - index)];
            length += [substring kr_length];
        }
        length += 1; // attachment 算 1 个字符
        index = range.location + range.length;
    }

    // 处理剩余文本
    if (index < attributedText.length) {
        NSString *substring = [attributedText.string substringWithRange:NSMakeRange(index, attributedText.length - index)];
        length += [substring kr_length];
    }

    return length;
}

- (BOOL)p_shouldTruncate:(NSAttributedString *)attributedText maxLength:(NSInteger)maxLength {
    NSString *rawText = [self p_rawTextFromAttributedText:attributedText] ?: @"";
    if (self.css_lengthLimitType == nil || [self.css_lengthLimitType integerValue] < 0) {
        // 兼容旧版本行为
        return rawText.length > maxLength;
    }
    switch ([self.css_lengthLimitType integerValue]) {
        case 0: // BYTE
            return [rawText kr_byteLength] > maxLength;
        case 2: // VIRSUAL_WIDTH
        case 1: // CHARACTER
            return [self p_calculateLengthForText:rawText] > maxLength;
        default:
            return [rawText kr_length] > maxLength;
    }
}

// 基于 attributedText 计算 CHARACTER 模式长度
- (NSUInteger)p_calculateCharacterLengthForAttributedText:(NSAttributedString *)attributedText {
    if (attributedText.length == 0) {
        return 0;
    }

    NSUInteger length = 0;
    NSUInteger index = 0;
    NSMutableArray<NSValue *> *attachmentRanges = [NSMutableArray array];

    // 收集所有 attachment 的范围
    [attributedText enumerateAttribute:NSAttachmentAttributeName
                               inRange:NSMakeRange(0, attributedText.length)
                               options:0
                            usingBlock:^(id value, NSRange range, BOOL *stop) {
        if (value != nil) {
            [attachmentRanges addObject:[NSValue valueWithRange:range]];
        }
    }];


    // 按位置排序
    [attachmentRanges sortUsingComparator:^NSComparisonResult(NSValue *obj1, NSValue *obj2) {
        NSRange r1 = [obj1 rangeValue];
        NSRange r2 = [obj2 rangeValue];
        if (r1.location < r2.location) return NSOrderedAscending;
        if (r1.location > r2.location) return NSOrderedDescending;
        return NSOrderedSame;
    }];

    // 计算长度：普通文本用 kr_length，每个 attachment 算 1
    for (NSValue *rangeValue in attachmentRanges) {
        NSRange range = [rangeValue rangeValue];
        if (index < range.location) {
            NSString *substring = [attributedText.string substringWithRange:NSMakeRange(index, range.location - index)];
            length += [substring kr_length];
        }
        length += 1; // attachment 算 1 个字符
        index = range.location + range.length;
    }

    // 处理剩余文本
    if (index < attributedText.length) {
        NSString *substring = [attributedText.string substringWithRange:NSMakeRange(index, attributedText.length - index)];
        length += [substring kr_length];
    }

    return length;
}

- (NSString *)p_outputText {
    return [self p_rawTextFromAttributedText:self.attributedText];
}

- (NSString *)p_rawTextFromAttributedText:(NSAttributedString *)attributedText {
    if (!attributedText) {
        return self.text;
    }

    NSMutableString *outputText = [NSMutableString stringWithString:attributedText.string ?: @""];
    __block NSInteger offset = 0;

    [attributedText enumerateAttribute:NSAttachmentAttributeName
                               inRange:NSMakeRange(0, attributedText.length)
                               options:0
                            usingBlock:^(NSObject *value, NSRange range, BOOL *stop) {
        if (![value respondsToSelector:@selector(kr_originlTextBeforeTextAttachment)]) {
            return;
        }
        id<KRTextAttachmentStringProtocol> attachment = (id<KRTextAttachmentStringProtocol>)value;
        NSString *replaceText = [attachment kr_originlTextBeforeTextAttachment];
        if (replaceText.length == 0) {
            return;
        }
        NSRange replaceRange = NSMakeRange(range.location + offset, range.length);
        [outputText replaceCharactersInRange:replaceRange withString:replaceText];
        offset += (NSInteger)replaceText.length - (NSInteger)range.length;
    }];
    return outputText;
}

- (NSAttributedString *)p_processedAttributedTextForLengthCalculationWithRawText:(NSString *)rawText {
    NSMutableAttributedString *rawAttributedText = [[NSMutableAttributedString alloc] initWithString:rawText ?: @""];
    if (rawAttributedText.length > 0) {
        UIFont *font = self.font ?: self.typingAttributes[NSFontAttributeName] ?: [UIFont systemFontOfSize:16];
        if (font) {
            [rawAttributedText addAttribute:NSFontAttributeName value:font range:NSMakeRange(0, rawAttributedText.length)];
        }
        UIColor *textColor = self.textColor ?: self.typingAttributes[NSForegroundColorAttributeName];
        if (textColor) {
            [rawAttributedText addAttribute:NSForegroundColorAttributeName value:textColor range:NSMakeRange(0, rawAttributedText.length)];
        }
    }

    NSString *processor = _props[@"textPostProcessor"];
    if (![processor isKindOfClass:[NSString class]] || processor.length == 0) {
        return rawAttributedText;
    }
    if (![[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_customTextWithAttributedString:textPostProcessor:)]) {
        return rawAttributedText;
    }

    NSAttributedString *processedAttributedText = [[KuiklyRenderBridge componentExpandHandler] hr_customTextWithAttributedString:rawAttributedText textPostProcessor:processor];
    return processedAttributedText ?: rawAttributedText;
}

- (NSUInteger)p_getOutputCursorIndex {
    return [self p_getOutputCursorIndexWithInputIndex:self.selectedRange.location];
}

- (NSRange)p_getOutputSelectionRange {
    return [self p_getOutputRangeWithInputRange:self.selectedRange];
}

- (NSRange)p_getOutputRangeWithInputRange:(NSRange)inputRange {
    if (inputRange.location == NSNotFound) {
        return NSMakeRange(0, 0);
    }
    NSUInteger start = [self p_getOutputCursorIndexWithInputIndex:inputRange.location];
    NSUInteger end = [self p_getOutputCursorIndexWithInputIndex:NSMaxRange(inputRange)];
    if (start > end) {
        NSUInteger temp = start;
        start = end;
        end = temp;
    }
    return NSMakeRange(start, end - start);
}

- (NSUInteger)p_getOutputCursorIndexWithInputIndex:(NSUInteger)inputIndex {
    NSUInteger location = inputIndex;

    __block int offset = 0;
    NSAttributedString *attributedString = self.attributedText;
    if (!attributedString) {
        return location;
    }
    [attributedString enumerateAttribute:NSAttachmentAttributeName
                                   inRange:NSMakeRange(0, attributedString.length)
                                   options:0
                                usingBlock:^(NSObject *value, NSRange range, BOOL *stop) {
        if ([value respondsToSelector:@selector(kr_originlTextBeforeTextAttachment)]) {
            id<KRTextAttachmentStringProtocol> attachment = (id<KRTextAttachmentStringProtocol> )value;
            NSString *replaceText = [attachment kr_originlTextBeforeTextAttachment] ?: @" ";
            if (range.location < location) {
                offset += (replaceText.length - range.length);
            } else {
                *stop = YES;
            }
        }
    }];
    return location + offset;
}

- (NSUInteger)p_getInputCursorIndexWithIndex:(NSUInteger)cursorIndex {
    NSUInteger location = cursorIndex;

    __block int offset = 0;

    NSAttributedString *attributedString = self.attributedText;
    if (!attributedString) {
        return location;
    }
    [attributedString enumerateAttribute:NSAttachmentAttributeName
                                   inRange:NSMakeRange(0, attributedString.length)
                                   options:0
                                usingBlock:^(NSObject *value, NSRange range, BOOL *stop) {
        if ([value respondsToSelector:@selector(kr_originlTextBeforeTextAttachment)]) {
            id<KRTextAttachmentStringProtocol> attachment = (id<KRTextAttachmentStringProtocol> )value;
            NSString *replaceText = [attachment kr_originlTextBeforeTextAttachment];
            if (replaceText) {
                if (range.location + offset >= cursorIndex) {
                    *stop = YES;
                    return ;
                }
                offset += (replaceText.length - range.length);
            }
        }
    }];
    return location - offset;
}

/// Re-apply when textPostProcessor is set but the view still shows literal raw (attachments not applied).
- (BOOL)p_shouldReapplyTextPostProcessorForIncomingRawText:(NSString *)rawText {
    if (rawText.length == 0) {
        return NO;
    }
    NSString *processor = _props[@"textPostProcessor"];
    if (![processor isKindOfClass:[NSString class]] || processor.length == 0) {
        return NO;
    }
    NSString *displayedText = self.attributedText.string ?: @"";
    return [displayedText isEqualToString:rawText];
}

- (void)p_applyTextPostProcessorIfNeed {
    if (![[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_customTextWithAttributedString:textPostProcessor:)]) {
        return;
    }
    NSString *processor = _props[@"textPostProcessor"];
    if (![processor isKindOfClass:[NSString class]] || processor.length == 0) {
        return;
    }
    NSAttributedString *currentAttr = self.attributedText;
    if (!currentAttr) {
        return;
    }
    if (currentAttr.length > 0) {
        NSRange fontRange;
        [currentAttr attribute:NSFontAttributeName atIndex:0 effectiveRange:&fontRange];
    }

    NSAttributedString *processedAttr = [[KuiklyRenderBridge componentExpandHandler] hr_customTextWithAttributedString:currentAttr textPostProcessor:processor];
    if (!processedAttr) {
        return;
    }

    if (processedAttr.length > 0) {
        NSRange fontRange2;
        [processedAttr attribute:NSFontAttributeName atIndex:0 effectiveRange:&fontRange2];
    }

    // 保存当前光标的原始文本位置
    NSUInteger outputCursor = [self p_getOutputCursorIndex];
    BOOL savedIgnore = _ignoreTextDidChanged;
    _ignoreTextDidChanged = YES;
    self.attributedText = processedAttr;
    NSUInteger inputCursor = [self p_getInputCursorIndexWithIndex:outputCursor];
    UITextPosition *newPosition = [self positionFromPosition:self.beginningOfDocument offset:inputCursor];
    if (newPosition) {
        self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
    }
    _ignoreTextDidChanged = savedIgnore;
}

#pragma mark - getter

- (UITextView *)placeholderTextView {
     if (!_placeholderTextView) {
        _placeholderTextView = [[UITextView alloc] initWithFrame:self.bounds];
        _placeholderTextView.editable = NO;
        _placeholderTextView.userInteractionEnabled = NO;
#if TARGET_OS_OSX // [macOS
        _placeholderTextView.selectable = NO; // NSTextView specific: disable text selection
#endif // macOS]
        _placeholderTextView.textContainerInset = self.textContainerInset;
        _placeholderTextView.textContainer.lineFragmentPadding = self.textContainer.lineFragmentPadding;
        _placeholderTextView.backgroundColor = [UIColor clearColor];
        if (@available(iOS 13.0, macOS 10.10, *)) { // [macOS]
            _placeholderTextView.textColor = UIColor.placeholderTextColor;
        } else {
            _placeholderTextView.textColor = UIColor.lightGrayColor;
        }
        [self insertSubview:_placeholderTextView atIndex:0];
     }
     return _placeholderTextView;
}

@end

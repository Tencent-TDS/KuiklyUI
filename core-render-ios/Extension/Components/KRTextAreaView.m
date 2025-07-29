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
/** attr is placeholder */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(placeholder);
/** attr is placeholderColor */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(placeholderColor);
/** attr is textAlign */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(textAlign);
/** attr is maxTextLength */
@property (nonatomic, strong)  NSNumber *KUIKLY_PROP(maxTextLength);
/** attr is tint color */
@property (nonatomic, strong, readwrite) NSString *KUIKLY_PROP(tintColor);
/** attr is color */
@property (nonatomic, strong, readwrite) NSString *KUIKLY_PROP(color);
/** attr is editable */
@property (nonatomic, strong, readwrite) NSNumber *KUIKLY_PROP(editable);
/** attr is keyboardType */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(keyboardType);
/** attr is returnKeyType */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(returnKeyType);
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

/** placeholderTextView property */
@property (nullable, nonatomic, strong) UITextView *placeholderTextView;

@end

@implementation KRTextAreaView {
    NSString *_text;
    BOOL _didAddKeyboardNotification;
    NSMutableDictionary *_props;
    BOOL _ignoreTextDidChanged;
}

@synthesize hr_rootView;

#pragma mark - init

- (instancetype)init {
    if (self = [super init]) {
        self.delegate = self;
        self.textContainerInset = UIEdgeInsetsZero;
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

- (void)setCss_values:(NSString *)css_values {
    if (_css_values != css_values) {
        _css_values = css_values;
        if (_css_values.length) {
            KRRichTextShadow *textShadow = [KRRichTextShadow new];
            [textShadow hrv_setPropWithKey:@"textPostProcessor" propValue:NSStringFromClass([self class])];
            for (NSString *key in _props.allKeys) {
                [textShadow hrv_setPropWithKey:key propValue:_props[key]];
            }
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
    self.tintColor = [UIView css_color:css_tintColor];
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
        CGFloat contentHeight = self.contentSize.height;
        callback(@{@"height": @(contentHeight)});
    }
}

- (void)layoutSubviews {
    [super layoutSubviews];
    if (_placeholderTextView.font != self.font) {
        _placeholderTextView.font = self.font;
    }
}


#pragma mark - UITextViewDelegate

- (void)textViewDidChange:(UITextView *)textView { // 文本值变化
    if (_ignoreTextDidChanged) {
        return ;
    }
    [self p_updatePlaceholder];
    if (textView.markedTextRange) {
        return ;
    }
    [self p_limitTextInput];
   
    if (self.css_textDidChange) {
        NSString *text = [self p_outputText].copy ?: @"";
        self.css_textDidChange(@{@"text": text, @"length": @([text kr_length])});
    }
}

- (void)paste:(id)sender {
    [super paste:sender];
    // 粘贴后，滚动到当前光标位置（延迟执行确保光标处于正确位置）
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.1 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [self scrollRangeToVisible:self.selectedRange];
    });
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
    if(self.css_imeAction && [text isEqualToString:@"\n"]) {
        self.css_imeAction(@{@"ime_action": self.css_returnKeyType ?: @""});
        dispatch_async(dispatch_get_main_queue(), ^{
            [textView resignFirstResponder];
        });
        return NO;
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

- (void)onReceivekeyboardWillShowNotification:(NSNotification *)notify {
    // 键盘将要弹出
    NSDictionary *info = notify.userInfo;
    CGFloat keyboardHeight = [[info objectForKey:UIKeyboardFrameEndUserInfoKey] CGRectValue].size.height;
    CGFloat duration = [[info objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];
    if (self.css_keyboardHeightChange) {
        self.css_keyboardHeightChange(@{@"height": @(keyboardHeight), @"duration": @(duration)});
    }
}

- (void)onReceivekeyboardWillHideNotification:(NSNotification *)notify {
    // 键盘将要隐藏
    NSDictionary *info = notify.userInfo;
    CGFloat duration = [[info objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];
    if (self.css_keyboardHeightChange) {
        self.css_keyboardHeightChange(@{@"height": @(0), @"duration": @(duration)});
    }
}

#pragma mark - override

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    [self setNeedsLayout];
    _placeholderTextView.frame = self.bounds;
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

- (void)p_addKeyboardNotificationIfNeed {
    if (_didAddKeyboardNotification) {
        return ;
    }
    _didAddKeyboardNotification = YES;
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onReceivekeyboardWillShowNotification:)
                                                 name:UIKeyboardWillShowNotification
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onReceivekeyboardWillHideNotification:)
                                                 name:UIKeyboardWillHideNotification
                                               object:nil];
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
    NSInteger maxLength = [self maxInputLengthWithString:textView.attributedText.string];
    if (maxLength == 0) {
        return;
    }
    
    if (textView.attributedText.length > maxLength) {
        if (textView.attributedText) {

            NSUInteger location = self.selectedRange.location;

            NSMutableAttributedString *truncatedAttributedString = [textView.attributedText mutableCopy];
            NSUInteger atIndex = MAX(location - 1, 0);
            NSUInteger deleteLength = 0;

            while (truncatedAttributedString.length > maxLength && (atIndex < truncatedAttributedString.length && atIndex >= 0)) {
                NSRange composedRange = [truncatedAttributedString.string rangeOfComposedCharacterSequenceAtIndex:atIndex]; // 避免切割emoji
                if (composedRange.length == 0) {
                    break;
                }
                [truncatedAttributedString deleteCharactersInRange:composedRange];

                atIndex = composedRange.location -1;
                deleteLength += composedRange.length;
            }
            if (truncatedAttributedString.length > maxLength) {
                NSRange range = [truncatedAttributedString.string rangeOfComposedCharacterSequenceAtIndex:maxLength];
                truncatedAttributedString = [[NSMutableAttributedString alloc] initWithAttributedString:[truncatedAttributedString attributedSubstringFromRange:NSMakeRange(0, range.location)]];
                location = maxLength;
                deleteLength = 0;
            }

            textView.attributedText = truncatedAttributedString;
            UITextPosition *newPosition = [self positionFromPosition:self.beginningOfDocument offset:MAX(location - deleteLength, 0)];

            _ignoreTextDidChanged = YES;
            self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
            _ignoreTextDidChanged = NO;

            dispatch_async(dispatch_get_main_queue(), ^{
                _ignoreTextDidChanged = YES;
                self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
                _ignoreTextDidChanged = NO;
            });
        }
       
        if (self.css_textLengthBeyondLimit) {
            self.css_textLengthBeyondLimit(@{});
        }
    }
}

- (NSUInteger)maxInputLengthWithString:(NSString *)string {
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

- (NSString *)p_outputText {
    NSAttributedString *attributedString = self.attributedText;
    if (!attributedString) {
        return self.text;
    }
    
    __block NSString *outputText = [attributedString.string mutableCopy];
    __block int offset = 0;
    
    [attributedString enumerateAttribute:NSAttachmentAttributeName
                                   inRange:NSMakeRange(0, attributedString.length)
                                   options:0
                                usingBlock:^(NSObject *value, NSRange range, BOOL *stop) {
        if ([value respondsToSelector:@selector(kr_originlTextBeforeTextAttachment)]) {
            id<KRTextAttachmentStringProtocol> attachment = (id<KRTextAttachmentStringProtocol> )value;
            NSString *replaceText = [attachment kr_originlTextBeforeTextAttachment];
            if (replaceText) {
                outputText = [outputText stringByReplacingCharactersInRange:NSMakeRange(range.location + offset, range.length)
                                                                withString:replaceText];
                offset += (replaceText.length - range.length);
            }
        }
    }];
    return outputText;
}

- (NSUInteger)p_getOutputCursorIndex {
    NSUInteger location = self.selectedRange.location;
    
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

#pragma mark - getter

- (UITextView *)placeholderTextView {
     if (!_placeholderTextView) {
        _placeholderTextView = [[UITextView alloc] initWithFrame:self.bounds];
        _placeholderTextView.editable = NO;
        _placeholderTextView.userInteractionEnabled = NO;
        _placeholderTextView.textContainerInset = self.textContainerInset;
        _placeholderTextView.backgroundColor = [UIColor clearColor];
        _placeholderTextView.textContainer.lineFragmentPadding = self.textContainer.lineFragmentPadding;
        _placeholderTextView.backgroundColor = [UIColor clearColor];
        if (@available(iOS 13.0, *)) {
            _placeholderTextView.textColor = UIColor.placeholderTextColor;
        } else {
            _placeholderTextView.textColor = UIColor.lightGrayColor;
        }
        [self insertSubview:_placeholderTextView atIndex:0];
     }
     return _placeholderTextView;
}

@end

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

#import "KRTextAreaView.h"
#import "KRComponentDefine.h"
#import "KRConvertUtil.h"
#import "KRRichTextView.h"
#import "KuiklyRenderBridge.h"
// 字典key常量
NSString *const KRFontSizeKey = @"fontSize";
NSString *const KRFontWeightKey = @"fontWeight";

/*
 * @brief 暴露给Kotlin侧调用的多行输入框组件
 */
@interface KRTextAreaView()<UITextViewDelegate>
/** attr is text */
@property (nonatomic, copy, readwrite) NSString *KUIKLY_PROP(text);
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
/** placeholderTextView property */
@property (nullable, nonatomic, strong) UITextView *placeholderTextView;

@end

@implementation KRTextAreaView {
    NSString *_text;
    BOOL _didAddKeyboardNotification;
    NSMutableDictionary *_props;
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
    self.text = css_text;
    [self p_updatePlaceholder];
}

- (void)setCss_values:(NSString *)css_values {
    if (_css_values != css_values) {
        _css_values = css_values;
        if (_css_values.length) {
            KRRichTextShadow *textShadow = [KRRichTextShadow new];
            for (NSString *key in _props.allKeys) {
                [textShadow hrv_setPropWithKey:key propValue:_props[key]];
            }
            UITextPosition *newPosition = [self positionFromPosition:self.beginningOfDocument offset:self.selectedRange.location];
            NSAttributedString *resAttr = [textShadow buildAttributedString];
            // 代理
            if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_customTextWithAttributedString:textPostProcessor:)]) {
                resAttr = [[KuiklyRenderBridge componentExpandHandler] hr_customTextWithAttributedString:resAttr textPostProcessor:NSStringFromClass([self class])];
            }
            self.attributedText = resAttr;
           
            self.attributedText = resAttr;
            self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
            
        } else {
            self.attributedText = nil;
        }
        [self p_updatePlaceholder];
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
        NSUInteger cursorIndex = self.selectedRange.location;
        callback(@{@"cursorIndex": @(cursorIndex)});
    }
    
    
}

- (void)css_setCursorIndex:(NSDictionary *)args {
    NSUInteger index = [args[KRC_PARAM_KEY] intValue];
    UITextPosition *newPosition = [self positionFromPosition:self.beginningOfDocument offset:index];
    self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
}

- (void)css_setText:(NSDictionary *)args {
    NSString *text = args[KRC_PARAM_KEY];
    self.text = text;
    [self textViewDidChange:self];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    if (_placeholderTextView.font != self.font) {
        _placeholderTextView.font = self.font;
    }
}


#pragma mark - UITextViewDelegate

- (void)textViewDidChange:(UITextView *)textView { // 文本值变化
    [self p_updatePlaceholder];
    if (textView.markedTextRange) {
        return ;
    }
    [self p_limitTextInput];
   
    if (self.css_textDidChange) {
        self.css_textDidChange(@{@"text": textView.text.copy ?: @""});
    }
    
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
    NSInteger maxLength = [self.css_maxTextLength intValue];
    if (maxLength == 0) {
        return;
    }
    if (textView.text.length > maxLength) {
        NSRange range = [textView.text rangeOfComposedCharacterSequenceAtIndex:maxLength];
        textView.text = [textView.text substringToIndex:range.location];
        if (self.css_textLengthBeyondLimit) {
            self.css_textLengthBeyondLimit(@{});
        }
    }
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
        [self insertSubview:_placeholderTextView atIndex:0];
     }
     return _placeholderTextView;
}
    

@end



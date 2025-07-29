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

#import "KRTextFieldView.h"
#import "KRConvertUtil.h"
#import "KRRichTextView.h"
#import "KuiklyRenderBridge.h"
// 字典key常量
NSString *const KRVFontSizeKey = @"fontSize";
NSString *const KRVFontWeightKey = @"fontWeight";

/*
 * @brief 暴露给Kotlin侧调用的多行输入框组件
 */
@interface KRTextFieldView()<UITextFieldDelegate>
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
/** attr is textAign */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(textAlign);
/** attr is placeholderColor */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(placeholderColor);
/** attr is maxTextLength */
@property (nonatomic, strong)  NSString *KUIKLY_PROP(maxTextLength);
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
/** event is inputReturn 点击return触发 */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(inputReturn);
/** event is keyboardHeightChange 键盘高度变化 */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(keyboardHeightChange);
/** event is textLengthBeyondLimit 输入长度超过限制 */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(textLengthBeyondLimit);

@end

@implementation KRTextFieldView {
    /** text */
    NSString *_text;
    /** didAddKeyboardNotification */
    BOOL _didAddKeyboardNotification;
    /** setNeedUpdatePlaceholder */
    BOOL _setNeedUpdatePlaceholder;
    /** collect props */
    NSMutableDictionary *_props;
}
@synthesize hr_rootView;
#pragma mark - init

- (instancetype)init {
    if (self = [super init]) {
        self.delegate = self;
        _props = [NSMutableDictionary new];
        [self addTarget:self action:@selector(onTextFeildTextChanged:) forControlEvents:UIControlEventEditingChanged];
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
    NSString *lastText = self.text ?: @"";
    NSString *newText = css_text ?: @"";
    if (![lastText isEqualToString:newText]) {
        self.text = css_text;
        [self onTextFeildTextChanged:self];
    }
}

- (void)setCss_values:(NSString *)css_values {
    if (_css_values != css_values) {
        _css_values = css_values;
        if (_css_values.length) {
            KRRichTextShadow *textShadow = [KRRichTextShadow new];
            for (NSString *key in _props.allKeys) {
                [textShadow hrv_setPropWithKey:key propValue:_props[key]];
            }
            // 保存原光标位置
            UITextRange *originalSelectedTextRange = self.selectedTextRange;
            // 设置新的 attributedText
            NSAttributedString *resAttr = [textShadow buildAttributedString];
            // 代理
            if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_customTextWithAttributedString:textPostProcessor:)]) {
                resAttr = [[KuiklyRenderBridge componentExpandHandler] hr_customTextWithAttributedString:resAttr textPostProcessor:NSStringFromClass([self class])];
            }
            self.attributedText = resAttr;
            // 恢复原光标位置
            self.selectedTextRange = originalSelectedTextRange;
        } else {
            self.attributedText = nil;
        }
        [self onTextFeildTextChanged:self];
    }
}

- (void)setCss_color:(NSNumber *)css_color {
    self.textColor = [UIView css_color:css_color];
}

- (void)setCss_tintColor:(NSNumber *)css_tintColor {
    self.tintColor = [UIView css_color:css_tintColor];
}

- (void)setCss_editable:(NSNumber *)css_editable {
    self.enabled = [UIView css_bool:css_editable];
}

- (void)setCss_textAlign:(NSString *)css_textAlign {
    self.textAlignment = [KRConvertUtil NSTextAlignment:css_textAlign];
}

- (void)setCss_fontSize:(NSNumber *)css_fontSize {
    _css_fontSize = css_fontSize;
    self.font = [KRConvertUtil UIFont:@{KRVFontSizeKey: css_fontSize ?: @(16),
                                        KRVFontWeightKey: _css_fontWeight ?: @"400"}];
}

- (void)setCss_fontWeight:(NSString *)css_fontWeight {
    _css_fontWeight = css_fontWeight;
    [self setCss_fontSize:_css_fontSize];
}

- (void)setCss_placeholder:(NSString *)css_placeholder {
    self.placeholder = css_placeholder;
    [self p_setNeedUpdatePlaceholder];
}

- (void)setCss_placeholderColor:(NSString *)css_placeholderColor {
    _css_placeholderColor = css_placeholderColor;
    [self p_setNeedUpdatePlaceholder];
}

- (void)setCss_keyboardType:(NSString *)css_keyboardType {
    self.keyboardType = [KRConvertUtil hr_keyBoardType:css_keyboardType];
    [self setSecureTextEntry:[css_keyboardType isEqualToString:@"password"]];
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

- (void)css_setText:(NSDictionary *)args {
    NSString *text = args[KRC_PARAM_KEY];
    self.text = text;
    [self onTextFeildTextChanged:self];
}

// 获取光标位置
- (void)css_getCursorIndex:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KRC_CALLBACK_KEY];
    if (callback) {
        UITextRange *selectedRange = self.selectedTextRange;
        NSUInteger cursorIndex = [self offsetFromPosition:self.beginningOfDocument toPosition:selectedRange.start];
        callback(@{@"cursorIndex": @(cursorIndex)});
    }
}

// 设置光标位置
- (void)css_setCursorIndex:(NSDictionary *)args {
    NSUInteger index = [args[KRC_PARAM_KEY] intValue];
    UITextPosition *newPosition = [self positionFromPosition:self.beginningOfDocument offset:index];
    self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
}



#pragma mark - override

- (void)layoutSubviews {
    [super layoutSubviews];
    if (_setNeedUpdatePlaceholder) {
        _setNeedUpdatePlaceholder = NO;
        UIColor *color = [UIView css_color:self.css_placeholderColor] ?: [UIColor grayColor];
        UIFont *font = self.font ?: [UIFont systemFontOfSize:16];
        self.attributedPlaceholder = [[NSMutableAttributedString alloc] initWithString:self.placeholder ?: @""
                                                                            attributes:@{NSForegroundColorAttributeName:color?: [UIColor clearColor],
                                                                                         NSFontAttributeName:font}];
    }
}


#pragma mark - UITextViewDelegate

- (void)onTextFeildTextChanged:(UITextField *)textField {  // 文本值变化
    if (textField.markedTextRange) {
        return ;
    }
    [self p_limitTextInput];
    if (self.css_textDidChange) {
        NSString *text = textField.text.copy ?: @"";
        self.css_textDidChange(@{@"text": text, @"length": @([text kr_length])});
    }
}


- (void)textFieldDidBeginEditing:(UITextField *)textField {  // 聚焦
    if (self.css_inputFocus) {
        self.css_inputFocus(@{@"text": textField.text.copy ?: @""});
    }
}

- (void)textFieldDidEndEditing:(UITextField *)textField {  // 失焦
    if (self.css_inputBlur) {
        self.css_inputBlur(@{@"text": textField.text.copy ?: @""});
    }
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    if (self.css_inputReturn) {
        self.css_inputReturn(@{@"text": textField.text.copy ?: @""});
    }
    return YES;
}

- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    return YES;
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

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    [self p_setNeedUpdatePlaceholder];
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

- (void)p_setNeedUpdatePlaceholder {
    _setNeedUpdatePlaceholder = YES;
    [self setNeedsLayout];
}



- (void)p_limitTextInput {
    UITextField *textView = self;
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
            
           // NSUInteger location = self.selectedTextRange.start.location;
            NSUInteger location = [self offsetFromPosition:self.beginningOfDocument toPosition:self.selectedTextRange.start];
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
          
            self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];

            dispatch_async(dispatch_get_main_queue(), ^{
                self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
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


@end



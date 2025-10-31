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
#import <TargetConditionals.h>

// 字典key常量
NSString *const KRVFontSizeKey = @"fontSize";
NSString *const KRVFontWeightKey = @"fontWeight";

/*
 * @brief 暴露给Kotlin侧调用的多行输入框组件
 */
#if TARGET_OS_IOS
@interface KRTextFieldView()<UITextFieldDelegate>
#elif TARGET_OS_OSX
@interface KRTextFieldView()<NSTextFieldDelegate>   // [macOS] for TextFieldView
#endif

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
#if TARGET_OS_IOS
        [self addTarget:self action:@selector(onTextFeildTextChanged:) forControlEvents:UIControlEventEditingChanged];
#elif TARGET_OS_OSX
        self.delegate = self;
        self.continuous = YES;  // 每一次键入，都会触发回调，这点待确认，看上去是可以取消的
#endif
    }
    return self;
}

#pragma mark - NSTextFieldDelegate

/// 增加文字变化事件的监听函数
#if TARGET_OS_OSX   // [macos]
- (void)controlTextDidChange:(NSNotification *)obj {
    [self onTextFeildTextChanged:self];
}
#endif

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
#if TARGET_OS_IOS
    self.text = css_text;
    NSString *lastText = self.text ?: @"";
    NSString *newText = css_text ?: @"";
    if (![lastText isEqualToString:newText]) {
        self.text = css_text;
        [self onTextFeildTextChanged:self];
    }
#elif TARGET_OS_OSX
    self.stringValue = css_text;
    NSString *lastText = self.stringValue ?: @"";
    NSString *newText = css_text ?: @"";
    if (![lastText isEqualToString:newText]) {
        self.stringValue = css_text;
        [self onTextFeildTextChanged:self];
    }
#endif
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
#if TARGET_OS_IOS
            UITextRange *originalSelectedTextRange = self.selectedTextRange;
#elif TARGET_OS_OSX
            NSRange originalSelectedTextRange = NSMakeRange(NSNotFound, 0);
            NSTextView *editor = (NSTextView *)[self currentEditor];
            if (editor) {
                originalSelectedTextRange = editor.selectedRange;
            }
#endif
            // 设置新的 attributedText
            NSAttributedString *resAttr = [textShadow buildAttributedString];
            // 代理
            if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_customTextWithAttributedString:textPostProcessor:)]) {
                resAttr = [[KuiklyRenderBridge componentExpandHandler] hr_customTextWithAttributedString:resAttr textPostProcessor:NSStringFromClass([self class])];
            }
            /// 恢复原光标位置 valo 待评估其正确性
#if TARGET_OS_IOS
            self.attributedText = resAttr;
            self.selectedTextRange = originalSelectedTextRange;
#elif TARGET_OS_OSX
            self.attributedStringValue = resAttr;
            if (originalSelectedTextRange.location != NSNotFound) {
                // 让控件取得焦点（如果已经是第一响应者，这一步不会有副作用）
                [[self window] makeFirstResponder:self];
                
                // 再次获取编辑器（此时一定存在）
                NSTextView *editor2 = (NSTextView *)[self currentEditor];
                if (editor2) {
                    // 防止越界（当我们刚刚把 attributedStringValue 改短时）
                    NSUInteger maxLen = self.stringValue.length;
                    NSRange safeRange = originalSelectedTextRange;
                    if (safeRange.location > maxLen) safeRange.location = maxLen;
                    if (NSMaxRange(safeRange) > maxLen) safeRange.length = maxLen - safeRange.location;
                    editor2.selectedRange = safeRange;
                }
            }
#endif
        } else {
#if TARGET_OS_IOS
            self.attributedText = nil;
#elif TARGET_OS_OSX
            self.attributedStringValue = nil;
#endif
        }
        [self onTextFeildTextChanged:self];
    }
}

- (void)setCss_color:(NSNumber *)css_color {
    self.textColor = [UIView css_color:css_color];
}

/// valo 待评估正确性 tintColor 替换为 textColor
- (void)setCss_tintColor:(NSNumber *)css_tintColor {
#if TARGET_OS_IOS
    self.tintColor = [UIView css_color:css_tintColor];
#elif TARGET_OS_OSX
    self.textColor = [UIView css_color:css_tintColor];
#endif
}

- (void)setCss_editable:(NSNumber *)css_editable {
    self.enabled = [UIView css_bool:css_editable];
}

- (void)setCss_textAlign:(NSString *)css_textAlign {
#if TARGET_OS_IOS
    self.textAlignment = [KRConvertUtil NSTextAlignment:css_textAlign];
#elif TARGET_OS_OSX
    self.alignment = [KRConvertUtil NSTextAlignment:css_textAlign];
#endif
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
#if TARGET_OS_IOS
    self.placeholder = css_placeholder;
#elif TARGET_OS_OSX
    self.placeholderString = css_placeholder;
#endif
    [self p_setNeedUpdatePlaceholder];
}

- (void)setCss_placeholderColor:(NSString *)css_placeholderColor {
    _css_placeholderColor = css_placeholderColor;
    [self p_setNeedUpdatePlaceholder];
}

/// valo 暂未能支持 属于 UITextInputTraits  替代方案思考中
- (void)setCss_keyboardType:(NSString *)css_keyboardType {
    self.keyboardType = [KRConvertUtil hr_keyBoardType:css_keyboardType];
    [self setSecureTextEntry:[css_keyboardType isEqualToString:@"password"]];
}
/// valo 暂未能支持 属于 UITextInputTraits  替代方案思考中
- (void)setCss_returnKeyType:(NSString *)css_returnKeyType {
    _css_returnKeyType = css_returnKeyType;
    self.returnKeyType = [KRConvertUtil hr_toReturnKeyType:css_returnKeyType];
}
/// valo 暂未能支持 属于 UITextInputTraits  替代方案思考中
- (void)setCss_enablesReturnKeyAutomatically:(NSNumber *)flag{
    self.enablesReturnKeyAutomatically = [flag boolValue];
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
#if TARGET_OS_IOS
    self.text = text;
#elif TARGET_OS_OSX
    self.stringValue = text;
#endif
    [self onTextFeildTextChanged:self];
}

// 获取光标位置
- (void)css_getCursorIndex:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KRC_CALLBACK_KEY];
    if (callback) {
#if TARGET_OS_IOS
        UITextRange *selectedRange = self.selectedTextRange;
        NSUInteger cursorIndex = [self offsetFromPosition:self.beginningOfDocument toPosition:selectedRange.start];
#elif TARGET_OS_OSX
        NSRange selectedRange = NSMakeRange(NSNotFound, 0);
        NSTextView *editor = (NSTextView *)[self currentEditor];
        if (editor) {
            selectedRange = editor.selectedRange;
        }
        NSUInteger cursorIndex = selectedRange.location;
#endif
        callback(@{@"cursorIndex": @(cursorIndex)});
    }
}

// 设置光标位置 valo 暂未能支持 属于 UITextInputTraits  替代方案思考中
- (void)css_setCursorIndex:(NSDictionary *)args {
    NSUInteger index = [args[KRC_PARAM_KEY] intValue];
#if TARGET_OS_IOS
    UITextPosition *newPosition = [self positionFromPosition:self.beginningOfDocument offset:index];
    self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
#elif TARGET_OS_OSX
}



#pragma mark - override

- (void)layoutSubviews {
    [super layoutSubviews];
    if (_setNeedUpdatePlaceholder) {
        _setNeedUpdatePlaceholder = NO;
        UIColor *color = [UIView css_color:self.css_placeholderColor] ?: [UIColor grayColor];
        UIFont *font = self.font ?: [UIFont systemFontOfSize:16];
#if TARGET_OS_IOS
        self.attributedPlaceholder = [[NSMutableAttributedString alloc] initWithString:self.placeholder ?: @""
                                                                            attributes:@{NSForegroundColorAttributeName:color?: [UIColor clearColor],
                                                                                         NSFontAttributeName:font}];
#elif TARGET_OS_OSX
        self.placeholderAttributedString = [[NSMutableAttributedString alloc] initWithString:self.placeholderString ?: @""
                                                                            attributes:@{NSForegroundColorAttributeName:color?: [UIColor clearColor],
                                                                                         NSFontAttributeName:font}];
#endif
    }
}


#pragma mark - UITextViewDelegate

#if TARGET_OS_IOS
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
#elif TARGET_OS_OSX
- (void)onTextFeildTextChanged:(NSTextField *)textField {  // 文本值变化
    // [macos]
    NSTextView *editor = (NSTextView *)[textField currentEditor];
    if (editor && editor.hasMarkedText) {
        return;
    }
   
    [self p_limitTextInput];
    if (self.css_textDidChange) {
        NSString *text = textField.stringValue.copy ?: @"";
        self.css_textDidChange(@{@"text": text, @"length": @([text kr_length])});
    }
}
#endif


#if TARGET_OS_IOS
- (void)textFieldDidBeginEditing:(UITextField *)textField {  // 聚焦
#elif TARGET_OS_OSX
- (void)textFieldDidBeginEditing:(NSTextField *)textField {  // 聚焦
#endif
    if (self.css_inputFocus) {
        self.css_inputFocus(@{@"text": textField.text.copy ?: @""});
    }
}

#if TARGET_OS_IOS
- (void)textFieldDidEndEditing:(UITextField *)textField {  // 失焦
#elif TARGET_OS_OSX
- (void)textFieldDidEndEditing:(NSTextField *)textField {  // 失焦
#endif
    if (self.css_inputBlur) {
        self.css_inputBlur(@{@"text": textField.text.copy ?: @""});
    }
}

#if TARGET_OS_IOS
- (BOOL)textFieldShouldReturn:(UITextField *)textField {
#elif TARGET_OS_OSX
- (BOOL)textFieldShouldReturn:(NSTextField *)textField {
#endif
    if (self.css_inputReturn) {
        self.css_inputReturn(@{@"text": textField.text.copy ?: @"", @"ime_action": self.css_returnKeyType ?: @""});
    }
    return YES;
}


#if TARGET_OS_IOS
- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
#elif TARGET_OS_OSX
- (BOOL)textField:(NSTextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
#endif
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



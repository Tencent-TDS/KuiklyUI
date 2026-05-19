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
#import "NSObject+KR.h"
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
@property (nonatomic, strong)  NSNumber *KUIKLY_PROP(maxTextLength);
/** attr is lengthLimitType */
@property (nonatomic, strong)  NSNumber *KUIKLY_PROP(lengthLimitType);
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
/** 是否在点击 IME 动作按钮（如 Send/Go/Search）时自动收起键盘，默认值为 YES，即自动收起，可由业务设置autoHideKeyboardOnImeAction来关闭*/
@property (nonatomic, strong)  NSNumber *KUIKLY_PROP(autoHideKeyboardOnImeAction);
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
/** event is textInputStateChange raw text/selection/composition state change */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(textInputStateChange);
/** event is selectionChange cursor/selection-only change */
@property (nonatomic, strong)  KuiklyRenderCallback KUIKLY_PROP(selectionChange);
/** attr is enablePinyinCallback 是否启用拼音输入回调 */
@property (nonatomic, strong)  NSNumber *KUIKLY_PROP(enablePinyinCallback);

- (BOOL)p_containsShortcodeToken:(NSString *)rawText;
- (BOOL)p_shouldRejectProgrammaticShortcodeInput:(NSString *)rawText;

@end

@implementation KRTextFieldView {
    /** text */
    NSString *_text;
    /** didAddKeyboardNotification */
    BOOL _didAddKeyboardNotification;
    /** setNeedUpdatePlaceholder */
    BOOL _setNeedUpdatePlaceholder;
    /** maxTextLength backing store */
    NSNumber *_css_maxTextLength;
    /** suppress native selection callback during programmatic selection updates */
    BOOL _ignoreSelectionChange;
    /** suppress intermediate textInputStateChange during programmatic state sync */
    BOOL _suppressTextInputStateChange;
    /** collect props */
    NSMutableDictionary *_props;
}
@synthesize hr_rootView;
#pragma mark - init

- (instancetype)init {
    if (self = [super init]) {
        self.delegate = self;
        _props = [NSMutableDictionary new];
        self.css_autoHideKeyboardOnImeAction = [NSNumber numberWithInt: 1];     // 保持原有能力，默认是关闭关闭软键盘
        [self addTarget:self action:@selector(onTextFeildTextChanged:) forControlEvents:UIControlEventEditingChanged];
    }
    return self;
}

- (void)setCss_maxTextLength:(NSNumber *)css_maxTextLength {
    _css_maxTextLength = css_maxTextLength;
}

- (NSNumber *)css_maxTextLength {
    return _css_maxTextLength;
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
            [textShadow hrv_setPropWithKey:@"contextParam" propValue:self.hr_rootView.contextParam];
            // 保存原光标位置
            UITextRange *originalSelectedTextRange = self.selectedTextRange;
            // 设置新的 attributedText
            NSAttributedString *resAttr = [textShadow buildAttributedString];
            // NOTE: UITextField does not render NSTextAttachment images (only UITextView does).
            // Therefore textPostProcessor with emoji/image attachment rendering is NOT supported
            // in single-line mode (KRTextFieldView). Use KRTextAreaView for custom emoji support.
            // The textPostProcessor call here is kept for any non-image text transformations
            // (e.g., text masking, formatting) that do not rely on NSTextAttachment rendering.
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
    _css_returnKeyType = css_returnKeyType;
    self.returnKeyType = [KRConvertUtil hr_toReturnKeyType:css_returnKeyType];
}

- (void)setCss_autoHideKeyboardOnImeAction:(NSNumber *)css_autoHideKeyboardOnImeAction {
    _css_autoHideKeyboardOnImeAction = css_autoHideKeyboardOnImeAction;
}

- (void)setCss_enablesReturnKeyAutomatically:(NSNumber *)flag{
    self.enablesReturnKeyAutomatically = [flag boolValue];
}

- (void)setCss_keyboardHeightChange:(KuiklyRenderCallback)css_keyboardHeightChange {
    _css_keyboardHeightChange = css_keyboardHeightChange;
    [self p_addKeyboardNotificationIfNeed];
}

- (void)setCss_enablePinyinCallback:(NSNumber *)css_enablePinyinCallback {
    _css_enablePinyinCallback = css_enablePinyinCallback;
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
    if (!newPosition) {
        return;
    }
    _ignoreSelectionChange = YES;
    self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
    _ignoreSelectionChange = NO;
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
        [self p_notifyTextInputStateChangeIfNeeded];
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

    // 设置文本。程序化同步时先压住中间态 textInputStateChange，等 selection 调整完再统一回传最终 state
    _suppressTextInputStateChange = YES;
    self.text = rawText;
    [self onTextFeildTextChanged:self];
    _suppressTextInputStateChange = NO;

    // 文本设置后可能因长度限制被截断，需要基于实际文本长度调整 selection
    NSUInteger actualLength = self.text.length;
    NSUInteger actualStart = MAX(0, MIN((NSUInteger)selectionStart, actualLength));
    NSUInteger actualEnd = MAX(0, MIN((NSUInteger)selectionEnd, actualLength));

    UITextPosition *startPos = [self positionFromPosition:self.beginningOfDocument offset:actualStart];
    UITextPosition *endPos = [self positionFromPosition:self.beginningOfDocument offset:actualEnd];
    if (startPos && endPos) {
        _ignoreSelectionChange = YES;
        self.selectedTextRange = [self textRangeFromPosition:startPos toPosition:endPos];
        _ignoreSelectionChange = NO;
    }

    [self p_notifyTextInputStateChangeIfNeeded];
}

- (void)css_getTextInputState:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KRC_CALLBACK_KEY];
    if (callback) {
        NSUInteger cursorIndex = [self offsetFromPosition:self.beginningOfDocument toPosition:self.selectedTextRange.start];
        NSUInteger cursorEnd = [self offsetFromPosition:self.beginningOfDocument toPosition:self.selectedTextRange.end];
        callback(@{
            @"text": self.text ?: @"",
            @"selectionStart": @(cursorIndex),
            @"selectionEnd": @(cursorEnd),
            @"compositionStart": @(-1),
            @"compositionEnd": @(-1),
            @"length": @([self p_calculateLengthForText:self.text])
        });
    }
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
    // 如果有拼音输入，根据配置决定是否触发回调
    if (textField.markedTextRange) {
        BOOL enablePinyinCallback = [self.css_enablePinyinCallback boolValue];
        if (enablePinyinCallback) {
            if (self.css_textDidChange) {
                NSString *text = textField.text.copy ?: @"";
                self.css_textDidChange(@{@"text": text, @"length": @([self p_calculateLengthForText:text])});
            }
        }
        return;
    }
    [self p_limitTextInput];
    if (self.css_textDidChange) {
        NSString *text = textField.text.copy ?: @"";
        self.css_textDidChange(@{@"text": text, @"length": @([self p_calculateLengthForText:text])});
    }
    [self p_notifyTextInputStateChangeIfNeeded];
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

- (void)textFieldDidChangeSelection:(UITextField *)textField {
    if (_ignoreSelectionChange || !self.css_selectionChange) {
        return;
    }
    self.css_selectionChange([self p_currentTextInputStatePayload]);
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    if (self.css_inputReturn) {
        self.css_inputReturn(@{@"text": textField.text.copy ?: @"", @"ime_action": self.css_returnKeyType ?: @""});
    }
    // 根据 autoHideKeyboardOnImeAction 属性决定是否收起键盘
    // 默认值为 NO（不自动收起），如果设置为 YES 则自动收起键盘
    if ([self.css_autoHideKeyboardOnImeAction boolValue]) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [textField resignFirstResponder];
        });
    }
    return YES;
}

- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    // 删除操作允许
    if (string == nil || [string isEqualToString:@""]) {
        return YES;
    }

    // legacy 模式（css_lengthLimitType == nil 或 < 0）走 p_limitTextInput 后置截断，不在此拦截
    if (self.css_lengthLimitType == nil || [self.css_lengthLimitType integerValue] < 0) {
        return YES;
    }

    // 检查长度限制
    NSInteger maxLength = [self.css_maxTextLength integerValue];
    if (maxLength > 0) {
        // 构造替换后的新文本
        NSString *newRawText = [textField.text stringByReplacingCharactersInRange:range withString:string];
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

#pragma mark - notication

- (void)onReceivekeyboardWillShowNotification:(NSNotification *)notify {
    // 键盘将要弹出
    NSDictionary *info = notify.userInfo;
    CGFloat keyboardHeight = [[info objectForKey:UIKeyboardFrameEndUserInfoKey] CGRectValue].size.height;
    CGFloat duration = [[info objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];
    NSInteger curve = [[info objectForKey:UIKeyboardAnimationCurveUserInfoKey] integerValue];
    if (self.css_keyboardHeightChange) {
        self.css_keyboardHeightChange(@{@"height": @(keyboardHeight), @"duration": @(duration), @"curve": @(curve)});
    }
}

- (void)onReceivekeyboardWillHideNotification:(NSNotification *)notify {
    // 键盘将要隐藏
    NSDictionary *info = notify.userInfo;
    CGFloat duration = [[info objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];
    NSInteger curve = [[info objectForKey:UIKeyboardAnimationCurveUserInfoKey] integerValue];
    if (self.css_keyboardHeightChange) {
        self.css_keyboardHeightChange(@{@"height": @(0), @"duration": @(duration), @"curve": @(curve)});
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

- (NSDictionary *)p_currentTextInputStatePayload {
    UITextRange *selectedRange = self.selectedTextRange;
    NSUInteger selectionStart = 0;
    NSUInteger selectionEnd = 0;
    if (selectedRange) {
        selectionStart = [self offsetFromPosition:self.beginningOfDocument toPosition:selectedRange.start];
        selectionEnd = [self offsetFromPosition:self.beginningOfDocument toPosition:selectedRange.end];
    }
    NSString *text = self.text ?: @"";
    return @{
        @"text": text,
        @"selectionStart": @(selectionStart),
        @"selectionEnd": @(selectionEnd),
        @"compositionStart": @(-1),
        @"compositionEnd": @(-1),
        @"length": @([self p_calculateLengthForText:text])
    };
}

- (void)p_notifyTextInputStateChangeIfNeeded {
    if (_suppressTextInputStateChange || !self.css_textInputStateChange) {
        return;
    }
    self.css_textInputStateChange([self p_currentTextInputStatePayload]);
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

- (void)p_limitTextInput {
    UITextField *textView = self;
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

           // NSUInteger location = self.selectedTextRange.start.location;
            NSUInteger location = [self offsetFromPosition:self.beginningOfDocument toPosition:self.selectedTextRange.start];
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
                _ignoreSelectionChange = YES;
                self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
                _ignoreSelectionChange = NO;

                dispatch_async(dispatch_get_main_queue(), ^{
                    self->_ignoreSelectionChange = YES;
                    self.selectedTextRange = [self textRangeFromPosition:newPosition toPosition:newPosition];
                    self->_ignoreSelectionChange = NO;
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

- (NSString *)p_rawTextFromAttributedText:(NSAttributedString *)attributedText {
    if (!attributedText) {
        return self.text;
    }
    NSMutableString *rawText = [NSMutableString stringWithString:attributedText.string ?: @""];
    __block NSInteger offset = 0;
    [attributedText enumerateAttribute:NSAttachmentAttributeName
                               inRange:NSMakeRange(0, attributedText.length)
                               options:0
                            usingBlock:^(id value, NSRange range, BOOL *stop) {
        if (![value respondsToSelector:@selector(kr_originlTextBeforeTextAttachment)]) {
            return;
        }
        id<KRTextAttachmentStringProtocol> attachment = (id<KRTextAttachmentStringProtocol>)value;
        NSString *replaceText = [attachment kr_originlTextBeforeTextAttachment];
        if (replaceText.length == 0) {
            return;
        }
        NSRange replaceRange = NSMakeRange(range.location + offset, range.length);
        [rawText replaceCharactersInRange:replaceRange withString:replaceText];
        offset += (NSInteger)replaceText.length - (NSInteger)range.length;
    }];
    return rawText;
}

- (NSAttributedString *)p_processedAttributedTextForLengthCalculationWithRawText:(NSString *)rawText {
    NSMutableAttributedString *rawAttributedText = [[NSMutableAttributedString alloc] initWithString:rawText ?: @""];
    if (rawAttributedText.length > 0) {
        UIFont *font = self.font ?: [UIFont systemFontOfSize:16];
        if (font) {
            [rawAttributedText addAttribute:NSFontAttributeName value:font range:NSMakeRange(0, rawAttributedText.length)];
        }
        UIColor *textColor = self.textColor;
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

@end



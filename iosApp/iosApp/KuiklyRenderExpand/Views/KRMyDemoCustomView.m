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

#import "KRMyDemoCustomView.h"
#import "KRComponentDefine.h"

@interface KRMyDemoCustomView ()
/** message 文字标签 */
@property (nonatomic, strong) UILabel *messageLabel;
/** Tap Me 按钮 */
@property (nonatomic, strong) UIButton *tapButton;
/** kuikly 属性：显示文字 */
@property (nonatomic, copy, nullable) NSString *KUIKLY_PROP(message);
/** kuikly 事件：Tap Me 点击回调 */
@property (nonatomic, strong, nullable) KuiklyRenderCallback KUIKLY_PROP(onMyViewTapped);
@end

@implementation KRMyDemoCustomView

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.backgroundColor = [UIColor yellowColor];
        self.layer.borderColor = [UIColor blackColor].CGColor;
        self.layer.borderWidth = 2.0;

        _messageLabel = [[UILabel alloc] init];
        _messageLabel.font = [UIFont systemFontOfSize:24];
        _messageLabel.textColor = [UIColor blackColor];
        _messageLabel.textAlignment = NSTextAlignmentCenter;
        [self addSubview:_messageLabel];

        _tapButton = [UIButton buttonWithType:UIButtonTypeSystem];
        [_tapButton setTitle:@"Tap Me" forState:UIControlStateNormal];
        [_tapButton.titleLabel setFont:[UIFont systemFontOfSize:18]];
        [_tapButton addTarget:self action:@selector(onTapButtonClicked)
             forControlEvents:UIControlEventTouchUpInside];
        [self addSubview:_tapButton];
    }
    return self;
}

- (void)layoutSubviews {
    [super layoutSubviews];
    CGFloat halfH = self.bounds.size.height / 2.0;
    self.messageLabel.frame = CGRectMake(0, 0, self.bounds.size.width, halfH);
    self.tapButton.frame = CGRectMake(0, halfH, self.bounds.size.width, halfH);
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    // 让通用属性（含 accessibility/accessibilityRole/accessibilityInfo）与父类 css_touch* 等先生效。
    KUIKLY_SET_CSS_COMMON_PROP;
    // 自定义 css_message / css_onMyViewTapped 通过 KVC 派发到本类 setter。
}

#pragma mark - CSS Property Setters

- (void)setCss_message:(NSString *)css_message {
    _css_message = [css_message copy];
    self.messageLabel.text = _css_message ?: @"";
}

- (void)setCss_onMyViewTapped:(KuiklyRenderCallback)css_onMyViewTapped {
    _css_onMyViewTapped = css_onMyViewTapped;
}

#pragma mark - Actions

- (void)onTapButtonClicked {
    if (self.css_onMyViewTapped) {
        self.css_onMyViewTapped(@{});
    }
}

#pragma mark - Subviews Ordering

/**
 * kuikly 侧通过 addSubview: 添加的子节点默认追加在最上层；
 * 装饰视图（messageLabel / tapButton）应位于最底层，避免遮挡 kuikly 子节点。
 * 这里在每次插入 kuikly 子节点后，主动把装饰视图沉到最底部。
 */
- (void)didAddSubview:(UIView *)subview {
    [super didAddSubview:subview];
    if (subview != self.messageLabel && subview != self.tapButton) {
        [self sendSubviewToBack:self.tapButton];
        [self sendSubviewToBack:self.messageLabel];
    }
}

@end

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


#import "KRSegmentedControl.h"
#import "KRComponentDefine.h"
#import "NSObject+KR.h"


@interface KRSegmentedControl ()

/// 分段控件
@property (nonatomic, strong) UISegmentedControl *segmentedControl;
/// 选中索引变化回调
@property (nonatomic, strong, nullable) KuiklyRenderCallback css_onValueChanged;

@end

@implementation KRSegmentedControl

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _segmentedControl = [[UISegmentedControl alloc] initWithItems:@[]];
        _segmentedControl.frame = self.bounds;
        _segmentedControl.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [_segmentedControl addTarget:self action:@selector(segmentedValueChanged:) forControlEvents:UIControlEventValueChanged];
        [self addSubview:_segmentedControl];
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)setCss_titles:(NSArray *)css_titles {
    [self.segmentedControl removeAllSegments];
    // Parse the JSON string parameter into an array
    NSArray *titlesArray = [css_titles hr_stringToArray];
    [titlesArray enumerateObjectsUsingBlock:^(NSString *title, NSUInteger idx, BOOL *stop) {
        [self.segmentedControl insertSegmentWithTitle:title atIndex:idx animated:NO];
    }];
}

- (void)setCss_selectedIndex:(NSNumber *)css_selectedIndex {
    NSInteger idx = [css_selectedIndex integerValue];
    if (idx >= 0 && idx < self.segmentedControl.numberOfSegments) {
        self.segmentedControl.selectedSegmentIndex = idx;
    }
}

- (void)setCss_onValueChanged:(KuiklyRenderCallback)css_onValueChanged {
    _css_onValueChanged = css_onValueChanged;
}

#pragma mark - 事件

- (void)segmentedValueChanged:(UISegmentedControl *)sender {
    if (self.css_onValueChanged) {
        self.css_onValueChanged(@{@"index": @(sender.selectedSegmentIndex)});
    }
}

#pragma mark - Kuikly 方法调用

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}


@end

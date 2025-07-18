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


#import "KRSegmentedControl.h"
#import "KRComponentDefine.h"

@interface KRSegmentedControl ()
@property (nonatomic, strong) UISegmentedControl *segmentedControl;
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
    [css_titles enumerateObjectsUsingBlock:^(NSString *title, NSUInteger idx, BOOL *stop) {
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

// 可选：设置 badge（如需支持）
- (void)css_setBadge:(NSString *)params {
    // params: {"index": 1, "badge": "99+"}
    NSData *data = [params dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    NSInteger idx = [dict[@"index"] integerValue];
    NSString *badge = dict[@"badge"];
    // UISegmentedControl 原生不支持 badge，可自定义实现
    // 这里只是示例，实际可用自定义视图覆盖 segment
}

@end

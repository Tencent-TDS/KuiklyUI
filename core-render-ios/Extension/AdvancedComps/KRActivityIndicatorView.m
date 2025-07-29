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

#import "KRActivityIndicatorView.h"
#import "KRComponentDefine.h"


@interface KRActivityIndicatorView()

@property (nonatomic, strong) NSString * css_style; // "white" or "gray"

@end

@implementation KRActivityIndicatorView
@synthesize hr_rootView;
- (instancetype)initWithFrame:(CGRect)frame {
    if ([super initWithFrame:frame]) {
        self.backgroundColor = [UIColor clearColor];
        self.activityIndicatorViewStyle = UIActivityIndicatorViewStyleWhite;
        [self startAnimating];
    }
    return self;
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}


- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}


- (void)setCss_style:(NSString *)css_style {
    _css_style = css_style;
    if ([css_style isEqualToString:@"white"]) {
        self.activityIndicatorViewStyle = UIActivityIndicatorViewStyleWhite;
    } else {
        self.activityIndicatorViewStyle = UIActivityIndicatorViewStyleGray;
    }
}

@end

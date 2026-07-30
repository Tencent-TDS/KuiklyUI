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

#import "KRWrapperView.h"

@interface KRWrapperView()


@end

@implementation KRWrapperView {
    __weak UIView *_hostView;
}

- (instancetype)initWithHostView:(UIView *)hostView {
    if (self = [super init]) {
        self.userInteractionEnabled = hostView.userInteractionEnabled;
        _hostView = hostView;
    }
    return self;
}

- (void)moveToSuperview:(UIView *)superView {
    if (!_hostView || self.superview == superView) {
        return ;
    }
    NSUInteger index = [superView.subviews indexOfObject:_hostView];
    NSAssert(index >= 0, @"没找到hostView");
    [_hostView removeFromSuperview];
    [self addSubview:_hostView];
    [superView insertSubview:self atIndex:index];
}


#pragma mark - override

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    _hostView.frame = self.bounds;
    [self.layer.mask setFrame:self.bounds];
}


- (void)dealloc {
    
}

@end

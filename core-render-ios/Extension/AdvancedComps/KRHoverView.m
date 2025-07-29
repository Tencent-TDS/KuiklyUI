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

#import "KRHoverView.h"
#import "KRComponentDefine.h"
#import "KRScrollView.h"
#import "UIView+CSS.h"

@interface KRHoverView()<UIScrollViewDelegate, KRScrollContentViewDelegate>

@property (nonatomic, weak) KRScrollView *scrollView;
// css attr 置顶层级（同一列表多个hoverView时kotlin侧设置该属性，值越大层级越高）
@property (nonatomic, strong) NSNumber *css_bringIndex;
// css attr 悬停距离list顶部距离
@property (nonatomic, strong) NSNumber *css_hoverMarginTop;

@end


@implementation KRHoverView

@synthesize hr_rootView;
- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}

#pragma mark - override

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    KRScrollView *scrollView = (KRScrollView *)self.superview.superview;
    if ([scrollView isKindOfClass:[KRScrollView class]]) {
        self.scrollView = scrollView;
        [self updateFrameToHoverIfNeed];
    }
    KRScrollContentView *scrollContentView = (KRScrollContentView *)self.superview;
    if ([scrollContentView isKindOfClass:[KRScrollContentView class]]) {
        [scrollContentView addScrollContentViewDelegate:self];
    }
}

- (void)removeFromSuperview {
    [super removeFromSuperview];
    self.scrollView = nil;
}

#pragma mark - KRScrollViewDelegate

- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView {
    
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    [self updateFrameToHoverIfNeed];
}

- (void)setCss_frame:(NSValue *)css_frame {
    [super setCss_frame:css_frame];
    if (css_frame) {
        [self updateFrameToHoverIfNeed];
    }
}

- (void)setCss_hoverMarginTop:(NSNumber *)css_hoverMarginTop {
    _css_hoverMarginTop = css_hoverMarginTop;
    [self updateFrameToHoverIfNeed];
}

#pragma mark - KRScrollContentViewDelegate

- (void)contentViewDidInsertSubview {
    [self adjustHoverViewLayerIfNeed];
}

#pragma mark - setter

- (void)setScrollView:(KRScrollView *)scrollView {
    if (_scrollView != scrollView) {
        [_scrollView removeScrollViewDelegate:self];
        _scrollView = scrollView;
        [scrollView addScrollViewDelegate:self];
    }
}

#pragma mark - private

- (void)updateFrameToHoverIfNeed {
    NSValue *frameValue = self.css_frame;
    if (!frameValue || !self.scrollView) {
        return ;
    }
    CGPoint offset = self.scrollView.contentOffset;
    CGRect frame = [frameValue CGRectValue];
    // 仅支持垂直方向置顶
    if (offset.y  > CGRectGetMinY(frame) - [self.css_hoverMarginTop floatValue]) {
        frame.origin.y = offset.y + [self.css_hoverMarginTop floatValue];
        self.frame = frame;
    } else {
        self.frame = frame;
    }
    [self adjustHoverViewLayerIfNeed];
}
/// 调整自身层级
- (void)adjustHoverViewLayerIfNeed {
    NSMutableArray *hoverViews = [[NSMutableArray alloc] init];
    for (UIView *subview in self.superview.subviews) {
        if ([subview isKindOfClass:[KRHoverView class]]) {
            [hoverViews addObject:subview];
        }
    }
    [hoverViews sortUsingComparator:^NSComparisonResult(KRHoverView * _Nonnull obj1, KRHoverView *  _Nonnull obj2) {
        int obj1Index = MAX(obj1.css_bringIndex.intValue, obj1.css_zIndex.intValue);
        int obj2Index = MAX(obj2.css_bringIndex.intValue, obj2.css_zIndex.intValue);
        if (obj1Index == obj2Index) {
            return NSOrderedSame;
        }
        return obj1Index < obj2Index ? NSOrderedAscending : NSOrderedDescending;
    }];
    
    [hoverViews enumerateObjectsUsingBlock:^(KRHoverView *  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        [self.superview bringSubviewToFront:obj];
    }];
    
}

#pragma mark - dealloc

- (void)dealloc {
    
}

@end

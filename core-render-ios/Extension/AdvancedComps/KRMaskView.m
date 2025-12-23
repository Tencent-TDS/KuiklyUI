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

#import "KRMaskView.h"
#import "KRComponentDefine.h"


@implementation KRMaskView

@synthesize hr_rootView;

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}


- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)insertSubview:(UIView *)view atIndex:(NSInteger)index {
    [super insertSubview:view atIndex:index];
    // 总共两个孩子，第一个孩子为生成遮罩的View ，第二个为被遮罩应用的View
    if (index == 1) {
#if TARGET_OS_OSX // [macOS
        UIView *maskSourceView = self.subviews.firstObject;
        [maskSourceView setNeedsDisplay:YES];
        [maskSourceView displayIfNeeded];
        dispatch_async(dispatch_get_main_queue(), ^{
            [self p_updateMaskIfNeeded];
        });
#else // macOS]
        view.layer.mask = self.subviews[0].layer;
#endif // [macOS]
    }
}

#if TARGET_OS_OSX // [macOS
- (void)p_updateMaskIfNeeded {
    UIView *maskSourceView = self.subviews.firstObject;
    [maskSourceView removeFromSuperview];
    UIView *targetView = self.subviews.firstObject;
    targetView.wantsLayer = YES;

    // Flip Y-axis for macOS coordinate system (bottom-left origin)
    CALayer *maskLayer = maskSourceView.layer;
    maskLayer.transform = CATransform3DMakeScale(1.0, -1.0, 1.0);
    maskLayer.anchorPoint = CGPointMake(0.5, 0.5);
    maskLayer.position = CGPointMake(CGRectGetMidX(maskLayer.bounds), CGRectGetMidY(maskLayer.bounds));
    targetView.layer.mask = maskLayer;
}
#endif // macOS]

@end

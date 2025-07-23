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

#import "KRModalView.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"

@implementation KRModalView

@synthesize hr_rootView;

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
    if([UIApplication isAppExtension]){
        return;
    }
    if (self.superview && ![self.superview isKindOfClass:[UIWindow class]]) {
       
        UIWindow *keyWindow = [UIApplication sharedApplication].keyWindow;
        if (keyWindow) {
            [self removeFromSuperview];
            
            [[[[self.hr_rootView kr_viewController] childViewControllers] copy]
             enumerateObjectsUsingBlock:^(__kindof UIViewController * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                if ([self isSubViewInModalWithViewController:obj]) {
                    // 当前Modal添加到keyWindow需移除对应的子vc，避免层级结构不一致带来的异常crash
                    [obj removeFromParentViewController];
                }
            }];
            [keyWindow addSubview:self];
            __weak typeof(self) weakSelf = self;
            // 兜底RenderView销毁时移除该Window上的modalView
            [self.hr_rootView performWhenRenderViewDeallocWithTask:^{
                [weakSelf removeFromSuperview];
            }];
        }
        
      
    }
}


- (BOOL)isSubViewInModalWithViewController:(UIViewController *)viewController {
    if (!viewController.isViewLoaded) {
        return NO;
    }
    UIView *view = viewController.view;
    while (view != nil) {
        if (view == self) {
            return YES;
        }
        if (view == self.hr_rootView) {
            break;
        }
        view = view.superview;
    }
    return NO;
}


@end

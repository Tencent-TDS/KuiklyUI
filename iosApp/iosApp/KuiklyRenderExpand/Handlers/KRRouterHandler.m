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


#import "KRRouterHandler.h"
#import "KuiklyRenderViewController.h"
#import "NativeMixKuiklyViewDemoViewController.h"

#define KR_MODAL_PRESENT @"kr_modal_present"
@implementation KRRouterHandler

+ (void)load {
    [KRRouterModule registerRouterHandler:[self new]];
}


- (void)openPageWithName:(NSString *)pageName pageData:(NSDictionary *)pageData controller:(UIViewController *)controller {
    UIViewController *vc = nil;
    if ([pageName isEqualToString:@"NativeMixKuikly"]) {
        vc = [[NativeMixKuiklyViewDemoViewController alloc] init];
    } else {
        vc = [[KuiklyRenderViewController alloc] initWithPageName:pageName pageData:pageData];
    }
    [controller.navigationController pushViewController:vc animated:YES];
}


- (void)closePage:(UIViewController *)controller {
    if (controller.navigationController.viewControllers.count == 1) { // count == 1 说明controller已是根vc，无法正常pop退出，可认定为present出来的vc（模态弹出方式），故退出以dismiss方式
        [controller.navigationController dismissViewControllerAnimated:NO completion:nil];
    } else {
        [controller.navigationController popViewControllerAnimated:YES];
    }

}

@end

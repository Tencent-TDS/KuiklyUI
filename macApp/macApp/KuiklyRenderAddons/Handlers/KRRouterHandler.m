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
#import "KRNavigationController.h"
#import <objc/runtime.h>

#define KR_MODAL_PRESENT @"kr_modal_present"

// Associated object key for window self-retention
static char kWindowSelfRetentionKey;

@implementation KRRouterHandler

+ (void)load {
    [KRRouterModule registerRouterHandler:[self new]];
}

#pragma mark - KRRouterProtocol

- (void)openPageWithName:(NSString *)pageName 
                pageData:(NSDictionary *)pageData 
              controller:(NSViewController *)controller {
    
    if (!pageName || pageName.length == 0) {
        NSLog(@"[KRRouterHandler] Error: pageName is empty");
        return;
    }
    
    // 创建目标视图控制器
    NSViewController *targetVC = [self createViewControllerWithPageName:pageName pageData:pageData];
    if (!targetVC) {
        NSLog(@"[KRRouterHandler] Error: Failed to create view controller for page: %@", pageName);
        return;
    }
    
    // 设置标题
    targetVC.title = pageName;
    
    // 打开页面（使用多种策略）
    [self presentViewController:targetVC fromController:controller pageData:pageData];
}

- (void)closePage:(NSViewController *)controller {
    if (!controller) {
        NSLog(@"[KRRouterHandler] Error: controller is nil");
        return;
    }
    
    // 策略1：如果在 KRNavigationController 中，使用堆栈管理
    KRNavigationController *navController = controller.kr_navigationController;
    if (navController) {
        // 判断是否为根控制器
        if (navController.viewControllers.count == 1) {
            // 是根控制器，尝试关闭整个导航控制器（如果它是被 present 的）
            if (navController.presentingViewController) {
                [navController.presentingViewController dismissViewController:navController];
                return;
            }
            // 否则关闭窗口
            [controller.view.window close];
        } else {
            // 不是根控制器，pop 返回
            [navController popViewControllerAnimated:YES];
        }
        return;
    }
    
    // 策略2：如果是 Sheet/Modal，关闭它
    if (controller.presentingViewController) {
        [controller.presentingViewController dismissViewController:controller];
        return;
    }
    
    // 策略3：关闭窗口（后备方案）
    NSWindow *window = controller.view.window;
    if (window) {
        [window close];
    } else {
        NSLog(@"[KRRouterHandler] Warning: Unable to close page, no window found");
    }
}

#pragma mark - Private Methods

/**
 * @brief 根据页面名创建视图控制器
 */
- (NSViewController *)createViewControllerWithPageName:(NSString *)pageName 
                                              pageData:(NSDictionary *)pageData {
    NSViewController *vc = nil;
    
    // 支持特定页面的自定义创建逻辑
    // 示例：处理原生混合页面
    if ([pageName isEqualToString:@"NativeMixKuikly"]) {
        // 可以在这里创建自定义的原生 ViewController
        // vc = [[CustomNativeViewController alloc] init];
        NSLog(@"[KRRouterHandler] NativeMixKuikly page is not implemented yet");
    }
    
    // 默认：创建 Kuikly 渲染页面
    if (!vc) {
        vc = [[KuiklyRenderViewController alloc] initWithPageName:pageName pageData:pageData];
    }
    
    return vc;
}

/**
 * @brief 智能展示视图控制器（根据上下文选择最佳方式）
 */
- (void)presentViewController:(NSViewController *)targetVC 
               fromController:(NSViewController *)fromController
                     pageData:(NSDictionary *)pageData {
    
    // 策略1：如果当前在 KRNavigationController 中，使用堆栈导航
    KRNavigationController *navController = fromController.kr_navigationController;
    if (navController) {
        [navController pushViewController:targetVC animated:YES];
        NSLog(@"[KRRouterHandler] Opened page via NavigationController push");
        return;
    }
    
    // 策略2：检查是否应该使用 Sheet（从 pageData 中读取标记）
    BOOL shouldPresentAsSheet = [pageData[KR_MODAL_PRESENT] boolValue] || 
                                [pageData[@"presentAsSheet"] boolValue];
    if (shouldPresentAsSheet && fromController.presentedViewControllers.count == 0) {
        [fromController presentViewControllerAsSheet:targetVC];
        NSLog(@"[KRRouterHandler] Opened page as Sheet (modal)");
        return;
    }
    
    // 策略3：使用新窗口打开（macOS 标准方式）
    [self openInNewWindow:targetVC];
    NSLog(@"[KRRouterHandler] Opened page in new window");
}

/**
 * @brief 在新窗口中打开视图控制器
 */
- (void)openInNewWindow:(NSViewController *)viewController {
    // 获取视图的建议尺寸，如果过小则使用默认值
    CGSize viewSize = viewController.view.frame.size;
    CGFloat windowWidth = MAX(viewSize.width, 900.0);
    CGFloat windowHeight = MAX(viewSize.height, 650.0);
    
    // 创建新窗口
    NSRect windowFrame = NSMakeRect(0, 0, windowWidth, windowHeight);
    NSWindow *newWindow = [[NSWindow alloc] initWithContentRect:windowFrame
                                                      styleMask:NSWindowStyleMaskTitled | 
                                                               NSWindowStyleMaskClosable | 
                                                               NSWindowStyleMaskResizable | 
                                                               NSWindowStyleMaskMiniaturizable
                                                        backing:NSBackingStoreBuffered
                                                          defer:NO];
    
    // 防止窗口在关闭动画期间被释放（会导致crash）
    newWindow.releasedWhenClosed = NO;
    
    // 设置窗口属性
    newWindow.contentViewController = viewController;
    newWindow.title = viewController.title ?: @"Kuikly Page";
    newWindow.minSize = NSMakeSize(400, 300);
    
    // 设置 contentView 的自动布局
    viewController.view.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    
    // 让窗口持有自己的强引用，防止被提前释放
    objc_setAssociatedObject(newWindow, &kWindowSelfRetentionKey, newWindow, OBJC_ASSOCIATION_RETAIN);
    
    // 监听窗口关闭通知，关闭后移除强引用让窗口可以被释放
    // 重要：不要在block中捕获newWindow，而是从notification.object获取，避免循环引用
    [[NSNotificationCenter defaultCenter] addObserverForName:NSWindowWillCloseNotification
                                                       object:newWindow
                                                        queue:[NSOperationQueue mainQueue]
                                                   usingBlock:^(NSNotification *note) {
        NSWindow *closingWindow = note.object; // 从通知获取窗口，不捕获外部引用
        NSLog(@"[KRRouterHandler] Window closing: %@", closingWindow.title);
        // 立即移除强引用，让窗口在关闭动画完成后可以被释放
        objc_setAssociatedObject(closingWindow, &kWindowSelfRetentionKey, nil, OBJC_ASSOCIATION_RETAIN);
    }];
    
    // 居中并显示
    [newWindow center];
    [newWindow makeKeyAndOrderFront:nil];
    
    NSLog(@"[KRRouterHandler] Created window with size: %.0fx%.0f", windowWidth, windowHeight);
}

@end

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

#import "KRBridgeModule.h"
#import "KuiklyRenderViewController.h"
#import "KuiklyContextParam.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"
#import "KRNavigationController.h"
#import <SDWebImageManager.h>
#import <SDWebImageDownloader.h>
#import <SDImageCache.h>
#import <objc/runtime.h>

#define REQ_PARAM_KEY @"reqParam"
#define CMD_KEY @"cmd"

// Associated object key for window self-retention
static char kBridgeWindowSelfRetentionKey;

// macOS Helper: 解析URL参数
static NSDictionary *ParseURLParameters(NSString *urlString) {
    if (urlString.length == 0) {
        return nil;
    }
    
    NSURLComponents *components = [NSURLComponents componentsWithString:urlString];
    if (!components || !components.queryItems || components.queryItems.count == 0) {
        return nil;
    }
    
    NSMutableDictionary *params = [NSMutableDictionary dictionary];
    for (NSURLQueryItem *item in components.queryItems) {
        if (item.name && item.value) {
            params[item.name] = item.value;
        }
    }
    
    return params.count > 0 ? [params copy] : nil;
}

// macOS Helper: 从view获取window
static NSWindow *GetWindowFromView(NSView *view) {
    return view.window;
}

// macOS Helper: 从view获取viewController
static NSViewController *GetViewControllerFromView(NSView *view) {
    return [view kr_viewController];
}


/*
 * @brief 扩展桥接接口，Native暴露接口到kotlin侧，提供kotlin侧调用native能力
 */
@implementation KRBridgeModule

@synthesize hr_rootView;


// 页面退出
- (void)closePage:(NSDictionary *)args {
    NSViewController *viewController = GetViewControllerFromView((NSView *)self.hr_rootView);
    
    // 方案1：NavigationController 堆栈管理（自定义，与iOS一致）
    KRNavigationController *navController = viewController.kr_navigationController;
    if (navController) {
        [navController popViewControllerAnimated:YES];
        return;
    }
    
    // 方案2：关闭 Sheet（macOS 原生方式）
    if (viewController.presentingViewController) {
        [viewController.presentingViewController dismissViewController:viewController];
        return;
    }
    
    // 方案3：关闭窗口（后备方案）
    NSWindow *window = GetWindowFromView((NSView *)self.hr_rootView);
    if (window && window.sheetParent) {
        [window.sheetParent endSheet:window];
    } else if (window) {
        [window close];
    }
}

// 打开页面
- (void)openPage:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *pageName = params[@"pageName"] ?: params[@"url"];
    NSMutableDictionary *pageData = [params[@"pageData"] mutableCopy] ?: [NSMutableDictionary new];
    
    // 解析URL参数
    NSDictionary *urlParams = ParseURLParameters(pageName);
    if (urlParams.count) {
        [pageData addEntriesFromDictionary:urlParams];
    }
    
    KuiklyRenderViewController *renderViewController = [[KuiklyRenderViewController alloc] 
        initWithPageName:pageName pageData:pageData];
    renderViewController.title = pageName;
    
    NSViewController *currentViewController = GetViewControllerFromView((NSView *)self.hr_rootView);
    
    // 方案1：NavigationController 堆栈管理（自定义，与iOS一致）
    KRNavigationController *navController = currentViewController.kr_navigationController;
    if (navController) {
        [navController pushViewController:renderViewController animated:YES];
        return;
    }
    
    // 方案2：Sheet 方式（macOS 原生推荐）
    // 适用于设置、详情等临时页面
    if (currentViewController && currentViewController.presentedViewControllers.count == 0) {
        // 检查是否应该使用 Sheet（可以根据 params 中的标记判断）
        BOOL useSheet = [params[@"presentAsSheet"] boolValue];
        if (useSheet) {
            [currentViewController presentViewControllerAsSheet:renderViewController];
            return;
        }
    }
    
    // 方案3：新窗口（适用于独立功能模块）
    NSWindow *window = GetWindowFromView((NSView *)self.hr_rootView);
    if (window) {
        
        // 获取视图尺寸，使用较大值确保窗口不会太小
        CGSize viewSize = renderViewController.view.frame.size;
        CGFloat windowWidth = MAX(viewSize.width, 900.0);
        CGFloat windowHeight = MAX(viewSize.height, 650.0);
        
        NSWindow *newWindow = [[NSWindow alloc] initWithContentRect:NSMakeRect(0, 0, windowWidth, windowHeight)
                                                          styleMask:NSWindowStyleMaskTitled | 
                                                                   NSWindowStyleMaskClosable | 
                                                                   NSWindowStyleMaskResizable | 
                                                                   NSWindowStyleMaskMiniaturizable
                                                            backing:NSBackingStoreBuffered
                                                              defer:NO];
        
        // 防止窗口在关闭动画期间被释放（会导致crash）
        newWindow.releasedWhenClosed = NO;
        
        newWindow.contentViewController = renderViewController;
        newWindow.title = pageName ?: @"Kuikly Page";
        newWindow.minSize = NSMakeSize(400, 300);
        
        // 设置自动调整大小
        renderViewController.view.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
        
        // 让窗口持有自己的强引用，防止被提前释放
        objc_setAssociatedObject(newWindow, &kBridgeWindowSelfRetentionKey, newWindow, OBJC_ASSOCIATION_RETAIN);
        
        // 监听窗口关闭通知，关闭后移除强引用让窗口可以被释放
        // 重要：不要在block中捕获newWindow，而是从notification.object获取，避免循环引用
        [[NSNotificationCenter defaultCenter] addObserverForName:NSWindowWillCloseNotification
                                                           object:newWindow
                                                            queue:[NSOperationQueue mainQueue]
                                                       usingBlock:^(NSNotification *note) {
            NSWindow *closingWindow = note.object; // 从通知获取窗口，不捕获外部引用
            NSLog(@"[KRBridgeModule] Window closing: %@", closingWindow.title);
            // 立即移除强引用，让窗口在关闭动画完成后可以被释放
            objc_setAssociatedObject(closingWindow, &kBridgeWindowSelfRetentionKey, nil, OBJC_ASSOCIATION_RETAIN);
        }];
        
        [newWindow center];
        [newWindow makeKeyAndOrderFront:nil];
        
        NSLog(@"[KRBridgeModule] Created window with size: %.0fx%.0f", windowWidth, windowHeight);
    }
}

- (void)copyToPasteboard:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *content = params[@"content"];
    // macOS: 使用NSPasteboard替代UIPasteboard
    NSPasteboard *pasteboard = [NSPasteboard generalPasteboard];
    [pasteboard clearContents];
    [pasteboard setString:content ?: @"" forType:NSPasteboardTypeString];
}


- (void)log:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *content = params[@"content"];
    NSLog(@"KuiklyRender:%@", content);
}

- (void)toast:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *content = params[@"content"];
    NSLog(@"KuiklyRender toast:%@", content);
    // 实现toast弹窗
    if (content.length > 0) {
        dispatch_async(dispatch_get_main_queue(), ^{
            NSAlert *alert = [[NSAlert alloc] init];
            alert.messageText = content;
            // 不显示按钮，仅仅1.5秒后自动消失
            [alert.window setLevel:NSFloatingWindowLevel];
            [alert.window setStyleMask:[alert.window styleMask] & ~NSWindowStyleMaskResizable];
            [alert beginSheetModalForWindow:[NSApplication sharedApplication].keyWindow completionHandler:nil];
            
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1.5 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
                [alert.window orderOut:nil];
                [NSApp endSheet:alert.window];
            });
        });
    }
}

- (id)testArray:(NSDictionary *)args {
    
    NSMutableArray *array =  args[KR_PARAM_KEY];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    if ([array isKindOfClass:[NSArray class]]) {
        id dd = array[1];
        if ([dd isKindOfClass:[NSData class]]) {
            callback(@[@"343434", dd, @"33434"]);
            return [NSMutableArray arrayWithObjects:@"224343",dd, nil];
        }
    }
    
    return nil;
}

- (void)getLocalImagePath:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *urlStr = params[@"imageUrl"];
    NSURL *url = [NSURL URLWithString:urlStr];

    [[SDWebImageDownloader sharedDownloader] downloadImageWithURL:url
                                                          options:0 
                                                         progress:nil
                                                        completed:^(UIImage * _Nullable image, NSData * _Nullable data, NSError * _Nullable error, BOOL finished) {
        if (image) {
            NSString *key = [[SDWebImageManager sharedManager] cacheKeyForURL:url];
            [[SDImageCache sharedImageCache] storeImage:image 
                                              imageData:data 
                                                 forKey:key 
                                                 toDisk:YES 
                                             completion:^{
                NSString *path = [[SDImageCache sharedImageCache] cachePathForKey:key];
                KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
                callback(@{@"localPath": path ?: @""});
            }];
        }
    }];
}

- (void)readAssetFile:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *path = params[@"assetPath"];
    KuiklyContextParam *contextParam = ((KuiklyRenderView *)self.hr_rootView).contextParam;
    NSURL *pathUrl = nil;
    pathUrl = [contextParam urlForFileName:[path stringByDeletingPathExtension] extension:[path pathExtension]];
    dispatch_async(dispatch_get_global_queue(0, 0), ^{
        NSError *error;
        NSString *jsonStr = [NSString stringWithContentsOfURL:pathUrl encoding:NSUTF8StringEncoding error:&error];
        NSDictionary *result = @{
            @"result": jsonStr ?: @"",
            @"error": error.description ?: @""
        };
        callback(result);
    });
}

@end

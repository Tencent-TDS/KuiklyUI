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
#import "NSObject+RIJCategory.h"

#import "KuiklyRenderViewController.h"
#import "KuiklyContextParam.h"
#import "KuiklyRenderView.h"
#import <SDWebImageManager.h>
#import <SDWebImageDownloader.h>
#import <SDImageCache.h>

#define REQ_PARAM_KEY @"reqParam"
#define CMD_KEY @"cmd"
// 扩展桥接接口
/*
 * @brief Native暴露接口到kotlin侧，提供kotlin侧调用native能力
 */

@implementation KRBridgeModule

@synthesize hr_rootView;


// 页面退出
- (void)closePage:(NSDictionary *)args {
    [self.hr_rootView.rij_viewController.navigationController popViewControllerAnimated:YES];
}

// 打开页面
- (void)openPage:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] rij_stringToDictionary];
   // KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];//
    NSString *pageName = params[@"pageName"] ?: params[@"url"]; // == pageName
    NSMutableDictionary *pageData = [params[@"pageData"] mutableCopy] ?: [NSMutableDictionary new];
    NSDictionary *urlParams = [pageName rij_getURLParameters];
    if (urlParams.count) {
        [pageData addEntriesFromDictionary:urlParams];
    }
    KuiklyRenderViewController *renderViewController = [[KuiklyRenderViewController alloc] initWithPageName:pageName pageData:pageData];
    [[self.hr_rootView.rij_viewController navigationController] pushViewController:renderViewController animated:YES];
}

- (void)copyToPasteboard:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] rij_stringToDictionary];
    NSString *content = params[@"content"];
    UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
    pasteboard.string = content;
}


- (void)log:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] rij_stringToDictionary];
    NSString *content = params[@"content"];
    NSLog(@"KuiklyRender:%@", content);
}

- (id)testArray:(NSDictionary *)args {
    
    NSMutableArray *aaray =  args[KR_PARAM_KEY];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    if ([aaray isKindOfClass:[NSArray class]]) {
        int coud = aaray.count;
        id dds = aaray[0];
        id dd = aaray[1];
        if ([dd isKindOfClass:[NSData class]]) {
            callback(@[@"343434", dd, @"33434"]);
            return [NSMutableArray arrayWithObjects:@"224343",dd, nil];
        }
    }
    
    return nil;
    
    
}

- (void)getLocalImagePath:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] rij_stringToDictionary];
    NSString *urlStr = params[@"imageUrl"];
    NSURL *url = [NSURL URLWithString:urlStr];

    [[SDWebImageDownloader sharedDownloader] downloadImageWithURL:url
                                                          options:0 progress:nil
                                                        completed:^(UIImage * _Nullable image, NSData * _Nullable data, NSError * _Nullable error, BOOL finished) {
        if (image) {
            NSString *key = [[SDWebImageManager sharedManager] cacheKeyForURL:url];
            [[SDImageCache sharedImageCache] storeImage:image imageData:data forKey:key toDisk:YES completion:^{
                NSString *path = [[SDImageCache sharedImageCache] cachePathForKey:key];
                KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
                callback(@{@"localPath": path ?: @""});

            }];
        }
    }];
}

- (void)readAssetFile:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] rij_stringToDictionary];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *path = params[@"assetPath"];
    KuiklyContextParam *contextParam = ((KuiklyRenderView *)self.hr_rootView).contextParam;
    NSURL *pathUrl = nil;
    pathUrl = [contextParam.contextMode urlForFileName:[path stringByDeletingPathExtension] extension:[path pathExtension]];
    dispatch_async(dispatch_get_global_queue(0, 0), ^{
        NSError *error;
        NSString *jsonStr = [NSString stringWithContentsOfURL:pathUrl encoding:NSUTF8StringEncoding error:&error];
        NSDictionary *result = @{
            @"result": jsonStr?:@"",
            @"error": error.description?:@""
        };
        callback(result);
    });
}

@end

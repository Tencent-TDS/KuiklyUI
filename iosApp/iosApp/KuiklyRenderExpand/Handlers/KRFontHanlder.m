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

#import "KRFontHanlder.h"
#import "KRFontModule.h"
#import "KuiklyContextParam.h"
#import <CoreText/CoreText.h>




@implementation KRFontHanlder

+ (void)load {
    [KuiklyRenderBridge registerFontHandler:[KRFontHanlder new]];
}

- (CGFloat)scaleFitWithFontSize:(CGFloat)fontSize {
    return (fontSize *2);
}

- (BOOL)hr_loadCustomFont:(NSString *)fontFamily
            contextParams:(KuiklyContextParam *)contextParam {
    // 1 判断字体是否已经注册，已注册直接返回
    if ([[UIFont familyNames] containsObject:fontFamily]) {
        return YES;
    }

    // 2 构建动态化模式下，三方字体资源的路径
    NSString *fontPath = [NSString stringWithFormat:@"%@/%@", contextParam.resourceFolderUrl, fontFamily];
    NSURL *fontPathURL = [NSURL fileURLWithPath:fontPath];


    // 3 执行HotReload动态化模式下，字体资源已被加载到本地，因此字体加载实际是加载本地URL（走else部分）
    // 若指定通过网络URL的方式加载字体资源，将由业务方自行设定加载逻辑，且注意加载时的异步 与 在主线程返回加载完成的UIFont 如何实现相互配合
    if ([fontPathURL.scheme hasPrefix:@"http"]) {
        // 待实现
        return NO;
    } else {
        // 加载本地URL加载字体资源
        return [self registerFontAtLocalURL:fontPathURL];
    }
}

/*
 * 字体动态加载函数
 */
- (BOOL)registerFontAtLocalURL:(NSURL *)fontURL {

    CGDataProviderRef fontDataProvider = CGDataProviderCreateWithURL((__bridge CFURLRef)fontURL);
    if (!fontDataProvider) {
        return NO;
    }

    // 创建目标字体Provider
    CGFontRef newFont = CGFontCreateWithDataProvider(fontDataProvider);
    CGDataProviderRelease(fontDataProvider);

    if (!newFont) {
        return NO;
    }

    // 字体加载成功，进行字体注册
    CFErrorRef error = NULL;
    BOOL success = CTFontManagerRegisterFontsForURL((__bridge  CFURLRef)fontURL,
                                                    kCTFontManagerScopeProcess,
                                                    &error);

    // 获取字体的名称
    CGFontRelease(newFont);
    if (!success) {
        if (error) {
            CFRelease(error);
        }
        return NO;
    }
    return YES;
}

@end

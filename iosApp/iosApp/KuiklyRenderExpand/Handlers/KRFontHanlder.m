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
    NSURL *fontPathURL = [self p_localFontURLWithFontFamily:fontFamily contextParams:contextParam];
    return fontPathURL && [self registerFontAtLocalURL:fontPathURL] != nil;
}

- (UIFont *)hr_fontWithFontFamily:(NSString *)fontFamily
                         fontSize:(CGFloat)fontSize
                       fontWeight:(UIFontWeight)fontWeight
                    contextParams:(KuiklyContextParam *)contextParam {
    // fontFamily 可为别名，注册后用字体文件内真实的 PostScript Name 创建 UIFont
    // 多字重字体族可按 fontWeight 映射到对应字体文件（如 CustomSans-Bold）
    // Demo 示例：别名 SatisfyAlias 映射到 bundle 内 Satisfy-Regular.ttf（见 BugReproIosGenericFontWeightPage）
    NSURL *fontPathURL = [fontFamily isEqualToString:@"SatisfyAlias"]
        ? [[NSBundle mainBundle] URLForResource:@"Satisfy-Regular" withExtension:@"ttf"]
        : [self p_localFontURLWithFontFamily:fontFamily contextParams:contextParam];
    NSString *postScriptName = fontPathURL ? [self registerFontAtLocalURL:fontPathURL] : nil;
    return postScriptName.length ? [UIFont fontWithName:postScriptName size:fontSize] : nil;
}

#pragma mark - Private

/// 构建三方字体资源的本地路径
- (nullable NSURL *)p_localFontURLWithFontFamily:(NSString *)fontFamily
                                   contextParams:(KuiklyContextParam *)contextParam {
    NSURL *baseURL = contextParam.resourceFolderUrl;
    if (baseURL == nil) {
        return nil;
    }
    NSURL *fontPathURL = [baseURL URLByAppendingPathComponent:fontFamily];
    // 字体资源已在本地；若为网络 URL，需业务方自行处理异步加载与主线程返回 UIFont 的配合
    if ([fontPathURL.scheme hasPrefix:@"http"]) {
        return nil;
    }
    return fontPathURL;
}

/// 注册本地字体文件，返回字体真实的 PostScript Name；失败返回 nil
- (nullable NSString *)registerFontAtLocalURL:(NSURL *)fontURL {
    CGDataProviderRef fontDataProvider = CGDataProviderCreateWithURL((__bridge CFURLRef)fontURL);
    if (!fontDataProvider) {
        return nil;
    }
    CGFontRef newFont = CGFontCreateWithDataProvider(fontDataProvider);
    CGDataProviderRelease(fontDataProvider);
    if (!newFont) {
        return nil;
    }
    NSString *postScriptName = (__bridge_transfer NSString *)CGFontCopyPostScriptName(newFont);
    CGFontRelease(newFont);
    if (!postScriptName.length) {
        return nil;
    }
    // 已注册过则不重复注册
    if ([UIFont fontWithName:postScriptName size:12]) {
        return postScriptName;
    }
    CFErrorRef error = NULL;
    BOOL success = CTFontManagerRegisterFontsForURL((__bridge CFURLRef)fontURL,
                                                    kCTFontManagerScopeProcess,
                                                    &error);
    if (!success) {
        if (error) {
            CFRelease(error);
        }
        return nil;
    }
    return postScriptName;
}

@end

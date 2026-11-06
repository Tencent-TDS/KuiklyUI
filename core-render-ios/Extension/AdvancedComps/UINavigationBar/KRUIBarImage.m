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

#import "KRUIBarImage.h"
#import "KuiklyRenderView.h"
#import "KuiklyContextParam.h"
#import "KuiklyRenderBridge.h"

#if !TARGET_OS_OSX // [macOS]
static NSString * const kKRBarImageAssetsPrefix = @"assets://";
static NSString * const kKRBarImageFilePrefix = @"file://";
static NSString * const kKRBarImageBase64Prefix = @"data:image";

// 与 UIFontWeightRegular / UIImageSymbolWeightRegular 相同（0.0）；不能用宏做全局 const 初值。
const CGFloat KRUIBarImageSymbolFontWeightRegular = 0.0;

@implementation KRUIBarImage

+ (UIImage *)p_symbolNamed:(NSString *)symbol fontWeight:(CGFloat)fontWeight {
    if (symbol.length == 0) {
        return nil;
    }
    if (@available(iOS 13.0, *)) {
        UIImage *image = [UIImage systemImageNamed:symbol];
        UIImageSymbolWeight weight = (UIImageSymbolWeight)fontWeight;
        UIImageSymbolConfiguration *config =
            [UIImageSymbolConfiguration configurationWithPointSize:17 weight:weight];
        return [image imageWithConfiguration:config] ?: image;
    }
    return nil;
}

+ (UIImage *)p_fitBarImage:(UIImage *)image {
    if (!image) {
        return nil;
    }
    CGFloat maxSide = MAX(image.size.width, image.size.height);
    if (maxSide <= 0 || maxSide <= 24.0) {
        return image;
    }
    CGFloat scale = 22.0 / maxSide;
    CGSize size = CGSizeMake(image.size.width * scale, image.size.height * scale);
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:size];
    return [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull rendererContext) {
        (void)rendererContext;
        [image drawInRect:CGRectMake(0, 0, size.width, size.height)];
    }];
}

+ (UIImage *)p_finishImage:(UIImage *)image useTemplate:(BOOL)useTemplate isSymbol:(BOOL)isSymbol {
    if (!image) {
        return nil;
    }
    UIImage *sized = isSymbol ? image : [self p_fitBarImage:image];
    if (isSymbol) {
        return sized;
    }
    return [sized imageWithRenderingMode:useTemplate ? UIImageRenderingModeAlwaysTemplate
                                                     : UIImageRenderingModeAlwaysOriginal];
}

+ (UIImage *)p_imageFromBase64:(NSString *)src {
    NSRange range = [src rangeOfString:@";base64,"];
    if (range.location == NSNotFound) {
        return nil;
    }
    NSString *payload = [src substringFromIndex:range.location + range.length];
    NSData *data = [[NSData alloc] initWithBase64EncodedString:payload
                                                       options:NSDataBase64DecodingIgnoreUnknownCharacters];
    return data ? [UIImage imageWithData:data] : nil;
}

+ (UIImage *)p_namedImage:(NSString *)name {
    if (name.length == 0) {
        return nil;
    }
    return [UIImage imageNamed:name.lastPathComponent] ?: [UIImage imageNamed:name];
}

+ (void)p_loadURLString:(NSString *)urlString
            useTemplate:(BOOL)useTemplate
                 symbol:(NSString *)symbol
         symbolFontWeight:(CGFloat)symbolFontWeight
             completion:(void (^)(UIImage *image))completion {
    if ([urlString hasPrefix:kKRBarImageFilePrefix] || [urlString hasPrefix:@"/"]) {
        NSString *path = [urlString hasPrefix:kKRBarImageFilePrefix]
            ? [urlString substringFromIndex:kKRBarImageFilePrefix.length]
            : urlString;
        UIImage *local = [UIImage imageWithContentsOfFile:path];
        if (local) {
            completion([self p_finishImage:local useTemplate:useTemplate isSymbol:NO]);
            return;
        }
    }
    id<KuiklyRenderComponentExpandProtocol> handler = [KuiklyRenderBridge componentExpandHandler];
    if ([handler respondsToSelector:@selector(hr_setImageWithUrl:imageParams:complete:)]) {
        [handler hr_setImageWithUrl:urlString imageParams:nil complete:^(UIImage *image, NSError *error, NSURL *imageURL) {
            (void)error;
            (void)imageURL;
            dispatch_async(dispatch_get_main_queue(), ^{
                if (image) {
                    completion([self p_finishImage:image useTemplate:useTemplate isSymbol:NO]);
                } else {
                    completion([self p_finishImage:[self p_symbolNamed:symbol fontWeight:symbolFontWeight]
                                       useTemplate:useTemplate
                                          isSymbol:YES]);
                }
            });
        }];
        return;
    }
    completion([self p_finishImage:[self p_symbolNamed:symbol fontWeight:symbolFontWeight]
                       useTemplate:useTemplate
                          isSymbol:YES]);
}

+ (void)resolveSrc:(NSString *)src
            symbol:(NSString *)symbol
    symbolFontWeight:(CGFloat)symbolFontWeight
       useTemplate:(BOOL)useTemplate
          rootView:(UIView *)rootView
        completion:(void (^)(UIImage *image))completion {
    if (!completion) {
        return;
    }
    if (src.length == 0) {
        completion([self p_finishImage:[self p_symbolNamed:symbol fontWeight:symbolFontWeight]
                           useTemplate:useTemplate
                              isSymbol:YES]);
        return;
    }
    if ([src hasPrefix:kKRBarImageBase64Prefix]) {
        completion([self p_finishImage:[self p_imageFromBase64:src] useTemplate:useTemplate isSymbol:NO]);
        return;
    }
    if ([src hasPrefix:@"http://"] || [src hasPrefix:@"https://"]) {
        [self p_loadURLString:src
                  useTemplate:useTemplate
                       symbol:symbol
               symbolFontWeight:symbolFontWeight
                   completion:completion];
        return;
    }
    if ([src hasPrefix:kKRBarImageAssetsPrefix]) {
        NSString *path = [src substringFromIndex:kKRBarImageAssetsPrefix.length];
        NSString *extension = path.pathExtension;
        NSString *name = extension.length ? [path stringByDeletingPathExtension] : path;
        UIImage *named = [self p_namedImage:name];
        if (named) {
            completion([self p_finishImage:named useTemplate:useTemplate isSymbol:NO]);
            return;
        }
        if ([rootView isKindOfClass:[KuiklyRenderView class]]) {
            KuiklyContextParam *param = ((KuiklyRenderView *)rootView).contextParam;
            NSURL *url = [param urlForFileName:name extension:extension];
            if (url) {
                [self p_loadURLString:url.absoluteString
                          useTemplate:useTemplate
                               symbol:symbol
                       symbolFontWeight:symbolFontWeight
                           completion:completion];
                return;
            }
        }
        [self p_loadURLString:src
                  useTemplate:useTemplate
                       symbol:symbol
               symbolFontWeight:symbolFontWeight
                   completion:completion];
        return;
    }
    if ([src hasPrefix:kKRBarImageFilePrefix]) {
        [self p_loadURLString:src
                  useTemplate:useTemplate
                       symbol:symbol
               symbolFontWeight:symbolFontWeight
                   completion:completion];
        return;
    }
    UIImage *named = [self p_namedImage:src];
    if (named) {
        completion([self p_finishImage:named useTemplate:useTemplate isSymbol:NO]);
        return;
    }
    if ([[NSFileManager defaultManager] fileExistsAtPath:src]) {
        completion([self p_finishImage:[UIImage imageWithContentsOfFile:src] useTemplate:useTemplate isSymbol:NO]);
        return;
    }
    completion([self p_finishImage:[self p_symbolNamed:symbol fontWeight:symbolFontWeight]
                       useTemplate:useTemplate
                          isSymbol:YES]);
}

@end
#endif // [macOS]

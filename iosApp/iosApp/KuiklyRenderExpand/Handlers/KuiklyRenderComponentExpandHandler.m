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

#import "KuiklyRenderComponentExpandHandler.h"
#import <SDWebImage/UIImageView+WebCache.h>

@implementation KuiklyRenderComponentExpandHandler

+ (void)load {
    // 注册自定义实现
    [KuiklyRenderBridge registerComponentExpandHandler:[self new]];
}

/*
 * 自定义实现设置颜值
 * @param value 设置的颜色值
 * @return 完成自定义处理的颜色对象
 */
- (UIColor *)hr_colorWithValue:(NSString *)value {
    return nil;
}


/*
 * 自定义图片src一致性判断
 * @param src 设置的src属性
 * @param imageURL 资源的路径
 * @return 是否满足一致性判断的条件
 */
- (BOOL)hr_srcMatch:(NSString *)src imageURL:(NSURL *)imageURL {
    if (!src.length || !imageURL)
        return NO;
    
    NSString *url = imageURL.absoluteString;
    if (!url.length)
        return NO;
    
    // 网络URL 走完全匹配
    if ([url isEqualToString:src])
        return YES;
    
    // 本地资源 取src和url 最后一个“/"之后的内容
    NSString *srcFileName = [self p_fileNameFromPath:src];
    NSString *urlFileName = [self p_fileNameFromPath:url];
    return srcFileName.length && urlFileName.length && [srcFileName isEqualToString:urlFileName];
}

// 提取文件名
- (NSString *)p_fileNameFromPath:(NSString *)path {
    if (!path.length)
        return @"";
    
    // 去除 URL 参数和锚点
    NSRange range = [path rangeOfCharacterFromSet:[NSCharacterSet characterSetWithCharactersInString:@"?#"]];
    if (range.location != NSNotFound) {
        path = [path substringToIndex:range.location];
    }
    
    // 提取最后一个 / 之后的内容
    NSRange slashRange = [path rangeOfString:@"/" options:NSBackwardsSearch];
    if (slashRange.location != NSNotFound) {
        // 如果 / 是最后一个字符，返回 ""
        if (slashRange.location == path.length - 1) {
            return @"";
        }
        return [path substringFromIndex:slashRange.location + 1];
    }
    return path;
}


/*
 * 自定义实现设置图片
 * @param url 设置的图片url，如果url为nil，则是取消图片设置，需要view.image = nil
 * @return 是否处理该图片设置，返回值为YES，则交给该代理实现，否则sdk内部自己处理
 *
 * 注意：如果同时实现了带完成回调的方法
 *      - (BOOL)hr_setImageWithUrl:(NSString *)url forImageView:(UIImageView *)imageView
 *                        complete:(ImageCompletionBlock)completeBlock;
 * 则优先调用带回调的方法。
 */
- (BOOL)hr_setImageWithUrl:(NSString *)url forImageView:(UIImageView *)imageView {
    [imageView sd_setImageWithURL:[NSURL URLWithString:url]];
    return YES;
}

/*
 * 自定义实现设置图片（带完成回调，优先调用该方法）
 * @param url 设置的图片url，如果url为nil，则是取消图片设置，需要view.image = nil
 * @param completeBlock 图片加载完成的回调，如有error，会触发loadFailure事件
 * @return 是否处理该图片设置，返回值为YES，则交给该代理实现，否则sdk内部自己处理
 */
- (BOOL)hr_setImageWithUrl:(NSString *)url forImageView:(UIImageView *)imageView complete:(ImageCompletionBlock)completeBlock {
    [imageView sd_setImageWithURL:[NSURL URLWithString:url]
                        completed:^(UIImage * _Nullable image, NSError * _Nullable error, SDImageCacheType cacheType, NSURL * _Nullable imageURL) {
        if (completeBlock) {
            completeBlock(image, error, imageURL);
        }
    }];
    return YES;
}


/*
 * 自定义实现设置图片（带完成回调和src一致性验证，优先调用该方法）
 * @param url 设置的图片url，如果url为nil，则是取消图片设置，需要view.image = nil
 * @param placeholder 设置的占位图，默认设置为nil
 * @param options 图片加载参数（对应SDWebImageOptions），默认传KRImageOptionAvoidAutoSetImage(1<<10，对应SDWebImageAvoidAutoSetImage)，阻断SDWebImage无感更新ImageView的image
 * @param complete 图片处理完成后的回调，内置src一致性验证
 * @return 是否处理该图片设置，返回值为YES，则交给该代理实现，否则sdk内部自己处理
 */
- (BOOL)hr_setImageWithUrl:(nonnull NSString *)url forImageView:(nonnull UIImageView *)imageView placeholderImage:(nullable UIImage *)placeholder options:(NSUInteger)options complete:(ImageCompletionBlock)completeBlock {
    [imageView sd_setImageWithURL:[NSURL URLWithString:url]
                 placeholderImage:placeholder
                          options:(SDWebImageOptions)options
                        completed:^(UIImage * _Nullable image, NSError * _Nullable error, SDImageCacheType cacheType, NSURL * _Nullable imageURL) {
        if (completeBlock) {
            completeBlock(image, error, [NSURL URLWithString:url]);
        }
    }];
    return YES;
}


    
@end

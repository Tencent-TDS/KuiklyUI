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
#import <SDWebImage/SDWebImageManager.h>
//#import <YYWebImage/YYWebImageManager.h>


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
            completeBlock(image, error, [NSURL URLWithString:url]);
        }
    }];
    return YES;
}

/*
 * 自定义实现设置图片（带完成回调和src一致性验证，优先调用该方法）
 * @param loadURL 待加载图片的 url
 * @param imageParams 自定义的图片参数
 * @param complete 图片处理完成后的回调，内置src一致性验证
 * @return 是否处理该图片设置，返回值为YES，则交给该代理实现，否则sdk内部自己处理
 */
- (BOOL)hr_setImageWithUrl:(nonnull NSString *)loadURL imageParams:(NSDictionary* _Nullable)imageParams complete:(ImageCompletionBlock)completeBlock;{
    // @warning 若使用SDWebImage加载图片，options 需设置为 SDWebImageAvoidAutoSetImage = 1 << 10 ，避免自动设置图片导致图片错乱问题
    
    // 使用SDWebImage 实现图片加载使用示例
    [[SDWebImageManager sharedManager] loadImageWithURL:[NSURL URLWithString:loadURL]
                                                options:SDWebImageAvoidAutoSetImage
                                               progress:nil
                                              completed:^(UIImage * _Nullable image, NSData * _Nullable data, NSError * _Nullable error, SDImageCacheType cacheType, BOOL finished, NSURL * _Nullable imageURL) {
        // @warning 实现此方法时，必须在图片加载完成后调用completeBlock，SDK通过此回调完成ImageView.image的最终设置，若不调用将导致图片无法显示
        if (completeBlock) {
            // * @warning 注意回调时所传入的imageURL是 hr_setImageWithUrl的参数url，而非SDWebImage Block中返回的imageURL参数
            completeBlock(image, error, [NSURL URLWithString:loadURL]);
        }
    }];
    // 使用YYImage 实现图片加载使用示例
//    [[YYWebImageManager sharedManager] requestImageWithURL:[NSURL URLWithString:loadURL]
//                                                   options:YYWebImageOptionAvoidSetImage
//                                                  progress:nil
//                                                 transform:nil
//                                                completion:^(UIImage * _Nullable image, NSURL * _Nonnull url, YYWebImageFromType from, YYWebImageStage stage, NSError * _Nullable error) {
//        // @warning 实现此方法时，必须在图片加载完成后调用completeBlock，SDK通过此回调完成ImageView.image的最终设置，若不调用将导致图片无法显示
//        if (completeBlock) {
//            // * @warning 注意回调时所传入的imageURL是 hr_setImageWithUrl的参数url，而非SDWebImage Block中返回的imageURL参数
//            completeBlock(image, error, [NSURL URLWithString:loadURL]);
//        }
//    }];
    return YES;
}


@end

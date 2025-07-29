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

#import "KRSnapshotModule.h"
#import "KuiklyRenderView.h"
#import "KRComponentDefine.h"
#define _KRWeakSelf __weak typeof(self) weakSelf = self;

@interface KRSnapshotModule()


@end

@implementation KRSnapshotModule

- (void)snapshotPager:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString * snapshotKey = param[@"snapshotKey"];
    _KRWeakSelf;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.2 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [weakSelf snapshotRootViewIfNeedWithSnapshotKey:snapshotKey];
    });
}



- (void)snapshotRootViewIfNeedWithSnapshotKey:(NSString *)snapshotKey {
    CGFloat beginTime = CFAbsoluteTimeGetCurrent();
    UIView *snapshotView = self.hr_rootView;
    CALayer *snapshotLayer = snapshotView.layer;
    CGRect bounds = snapshotView.bounds;
    UIImage *snapshotImage = [UIView kr_safeAsImageWithLayer:snapshotLayer bounds:bounds];
    if (!snapshotImage || snapshotImage.size.width == 0) {
        return ;
    }
    CGFloat endTime = CFAbsoluteTimeGetCurrent();
    NSLog(@"snapshot cost time:%.2lfms", (endTime - beginTime) * 1000.0);
    dispatch_async(dispatch_get_global_queue(0, 0), ^{
        [[self class] saveImageToDiskWithCacheKey:snapshotKey image:snapshotImage];
    });
}

+ (void)saveImageToDiskWithCacheKey:(NSString *)cacheKey image:(UIImage *)image {
    NSData *imageData = UIImagePNGRepresentation(image);
    NSString *cacheFilePath = [self generateSnapshotFilePathWithCacheKey:cacheKey];
    NSError *error = nil;
    [[NSFileManager defaultManager] removeItemAtPath:cacheFilePath error:&error];
    BOOL success = [imageData writeToFile:cacheFilePath atomically:YES];
#if DEBUG
    assert(success);
#endif
}

// 创建缓存目录
+ (NSString *)generateSnapshotFilePathWithCacheKey:(NSString *)cacheKey {
    NSString *cachesDir = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES).firstObject;
    cachesDir = [NSString stringWithFormat:@"%@/kuikly_snapshot", cachesDir];
    if (![[NSFileManager defaultManager] fileExistsAtPath:cachesDir]) {
        NSError * error;
        [[NSFileManager defaultManager] createDirectoryAtPath:cachesDir
                                       withIntermediateDirectories:YES
                                                        attributes:nil
                                                        error:&error];
    }
    return [NSString stringWithFormat:@"%@/%@", cachesDir, cacheKey];
}

+ (UIImage *)snapshotPagerWithSnapshotKey:(NSString *)snapshotKey {
    NSString *snapshotCacheFilePath = [self generateSnapshotFilePathWithCacheKey:snapshotKey];
    if ([[NSFileManager defaultManager] fileExistsAtPath:snapshotCacheFilePath]) {
        NSData *imageData = [[NSData alloc] initWithContentsOfFile:snapshotCacheFilePath];
        if (!imageData) {
            return nil;
        }
        return [[UIImage alloc] initWithData:imageData];
    }
   
    return nil;
  
}

@end

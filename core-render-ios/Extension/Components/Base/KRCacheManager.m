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

#import "KRCacheManager.h"
#import "KRLogModule.h"

static id<KRCacheProtocol> gCacheHandler;


@implementation KRCacheManager

+ (void)registerCacheHandler:(id<KRCacheProtocol>)cacheHandler {
    gCacheHandler = cacheHandler;
}


+ (instancetype)sharedInstance {
    static dispatch_once_t onceToken;
    static KRCacheManager *instance;
    dispatch_once(&onceToken, ^{
        instance = [[KRCacheManager alloc] init];
    });
    return instance;
}

/*
 * 根据文件夹名返回对应缓存全路径
 */
- (NSString *)cachePathWithFolderName:(NSString *)folderName {
    NSString *rootPath = [self rootCachePath];
    NSString *cachePath = rootPath;
    if ([folderName isKindOfClass:[NSString class]] && folderName.length) {
        cachePath = [rootPath stringByAppendingPathComponent:folderName];
    }
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSError *error;
    if (![fileManager fileExistsAtPath:cachePath]) {
        BOOL success = [fileManager createDirectoryAtPath:cachePath withIntermediateDirectories:YES attributes:nil error:&error];
        if (!success) {
            [KRLogModule logError:[NSString stringWithFormat:@"Error create kuikly root cache directory:%@ error: %@", cachePath, error.localizedDescription]];
        }
    }
    return cachePath;
}



- (NSString *)rootCachePath {
    NSString *rootPath = nil;
    if (gCacheHandler && [gCacheHandler respondsToSelector:@selector(kr_rootCachePath)]) {
        rootPath = [gCacheHandler kr_rootCachePath];
    }
    if (!([rootPath isKindOfClass:[NSString class]] && rootPath.length)) {
        rootPath = [self defaultRootCahcePath];
    }
    return rootPath;
}

- (NSString *)defaultRootCahcePath {
    return [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
}


@end

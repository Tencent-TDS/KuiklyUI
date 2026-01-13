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

#import "KRTurboDisplayCacheManager.h"
#import "KRTurboDisplayNode.h"
#import "KRLogModule.h"
#import "KuiklyRenderLayerHandler.h"
#import "KuiklyRenderThreadManager.h"

@implementation KRTurboDisplayCacheData



@end

@interface KRTurboDisplayCacheManager()

@property (nonatomic, strong) NSLock *fileLock;

@end

@implementation KRTurboDisplayCacheManager

- (instancetype)init {
    if (self = [super init]) {
        _fileLock = [NSLock new];
    }
    return self;
}

+ (instancetype)sharedInstance {
    static KRTurboDisplayCacheManager *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[self alloc] init];
    });
    return sharedInstance;
}

// 确保不创建多个实例
+ (instancetype)allocWithZone:(struct _NSZone *)zone {
    static KRTurboDisplayCacheManager *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [super allocWithZone:zone];
    });
    return sharedInstance;
}

// 确保不创建多个实例
- (id)copyWithZone:(NSZone *)zone {
    return self;
}

// 存储到磁盘的Tag格式化，避免渲染生成和真实节点冲突
- (void)formatTagWithCacheTree:(KRTurboDisplayNode *)node {
   
    if (![node.tag isEqual:KRV_ROOT_VIEW_TAG] && [node.tag intValue] >= 0) {
        node.tag = @(-([node.tag intValue] + 2));
    }
    
    if (node.parentTag && ![node.parentTag isEqual:KRV_ROOT_VIEW_TAG] && [node.parentTag intValue] >= 0) {
        node.parentTag = @(-([node.parentTag intValue] + 2));
    }
    
  
   
    if ([node hasChild]) {
        for (KRTurboDisplayNode *subNode in node.children) {
            [self formatTagWithCacheTree:subNode];
        }
    }
   
}

- (NSString *)cacheKeyWithTurboDisplayKey:(NSString *)turboDisplayKey pageName:(NSString *)pageName {
    NSString *key = [[NSString stringWithFormat:@"%@_%@",pageName, turboDisplayKey] kr_md5String];
    return [NSString stringWithFormat:@"kuikly_turbo_display_9%@.data", key];
}

- (void)removeAllTurboDisplayCacheFiles {
    [self.fileLock lock];
    NSString *folderPath = [self cacheRootPath];
    // 检查文件夹是否存在
    @try {
        BOOL isDirectory;
        BOOL folderExists = [[NSFileManager defaultManager] fileExistsAtPath:folderPath isDirectory:&isDirectory];
        if (folderExists && isDirectory) {
             NSError *error;
             // 删除文件夹及其所有子文件
             BOOL success = [[NSFileManager defaultManager]  removeItemAtPath:folderPath error:&error];
               
             if (!success) {
                [KRLogModule logError:[NSString stringWithFormat:@"%s failed:%@", __FUNCTION__, error.localizedDescription]];
             }
         }
       
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"%s exception:%@", __FUNCTION__, exception]];
    
    } @finally {
        [self.fileLock unlock];
    }

}

- (void)removeCacheWithKey:(NSString *)cacheKey {
    @try {
        [self.fileLock lock];
        NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
        [[NSFileManager defaultManager] removeItemAtPath:filePath error:nil];
       
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when removeCacheWithKey:%@ key:%@", exception, cacheKey]];
    } @finally {
        [self.fileLock unlock];
    }
}

- (void)cacheWithViewNode:(KRTurboDisplayNode *)viewNode cacheKey:(NSString *)cacheKey {
    
    [KuiklyRenderThreadManager performOnLogQueueWithBlock:^{
        @try {
            [self.fileLock lock];
            [self formatTagWithCacheTree:viewNode];
            NSData *nodeData = [NSKeyedArchiver archivedDataWithRootObject:viewNode requiringSecureCoding:NO error:nil];
            // 将 NSData 存储到磁盘
           
            NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
            [nodeData writeToFile:filePath atomically:YES];
           
        } @catch (NSException *exception) {
            [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when archived Node Data:%@ key:%@", exception, cacheKey]];
        } @finally {
            [self.fileLock unlock];
        }
    }];
   
}

- (void)cacheWithViewNodeData:(NSData *)nodeData cacheKey:(NSString *)cacheKey {
    if (!nodeData) {
        return ;
    }
    // 丢入异步串行队列
    [KuiklyRenderThreadManager performOnLogQueueWithBlock:^{
        @try {
            // 将 NSData 存储到磁盘
            [self.fileLock lock];
            NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
            [nodeData writeToFile:filePath atomically:YES];
           
        } @catch (NSException *exception) {
            [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when archived Node NSData:%@ key:%@", exception, cacheKey]];
        } @finally {
            [self.fileLock unlock];
        }
    }];
}


- (BOOL)hasNodeWithCacheKey:(NSString *)cacheKey {
    BOOL res = NO;
    
    @try {
        [self.fileLock lock];
    
        NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
        if ([[NSFileManager defaultManager] fileExistsAtPath:filePath]) {
            res = YES;
        }
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when hasNodeWithCacheKey:%@ key:%@", exception, cacheKey]];
    } @finally {
        [self.fileLock unlock];
    }
    return res;
}

- (KRTurboDisplayCacheData *)nodeWithCachKey:(NSString *)cacheKey {
   
    KRTurboDisplayCacheData *cacheData = nil;
    @try {
        [self.fileLock lock];
        NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
        if ([[NSFileManager defaultManager] fileExistsAtPath:filePath]) {
            NSData *nodeData = [NSData dataWithContentsOfFile:filePath];
            
            if (nodeData) {
                cacheData = [KRTurboDisplayCacheData new];
//                cacheData.turboDisplayNode = [NSKeyedUnarchiver unarchivedObjectOfClass:[KRTurboDisplayNode class] fromData:nodeData error:nil];
                NSKeyedUnarchiver *unarchiver = [[NSKeyedUnarchiver alloc] initForReadingFromData:nodeData error:nil];
                if (unarchiver) {
                    unarchiver.requiresSecureCoding = NO;
                    cacheData.turboDisplayNode = [unarchiver decodeObjectForKey:NSKeyedArchiveRootObjectKey];
                    [unarchiver finishDecoding];
                }
                cacheData.turboDisplayNodeData = nodeData;
            }
           
            // 删除原来文件
            [[NSFileManager defaultManager] removeItemAtPath:filePath error:nil];
        }
        
        // 同时读取并删除额外缓存内容文件
        NSString *extraCacheKey = [self extraCacheKeyFromMainCacheKey:cacheKey];
        NSString *extraFilePath = [[self cacheRootPath] stringByAppendingPathComponent:extraCacheKey];
        if ([[NSFileManager defaultManager] fileExistsAtPath:extraFilePath]) {
            NSData *extraData = [NSData dataWithContentsOfFile:extraFilePath];
            if (extraData) {
                cacheData.extraCacheContent = [[NSString alloc] initWithData:extraData encoding:NSUTF8StringEncoding];
            }
            // 删除额外缓存文件
            [[NSFileManager defaultManager] removeItemAtPath:extraFilePath error:nil];
        }
       
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when unarchived Node Data:%@ key:%@", exception, cacheKey]];
        cacheData = nil;
    } @finally {
        [self.fileLock unlock];
    }
    return cacheData;
}


- (NSString *)cacheRootPath {
    NSString *cachesDirectory = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
    NSString *turboDisplayDirectory = [cachesDirectory stringByAppendingPathComponent:@"TurboDisplay"];

    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSError *error;

    if (![fileManager fileExistsAtPath:turboDisplayDirectory]) {
        BOOL success = [fileManager createDirectoryAtPath:turboDisplayDirectory withIntermediateDirectories:YES attributes:nil error:&error];
        if (!success) {
            [KRLogModule logError:[NSString stringWithFormat:@"Error creating TurboDisplay directory: %@", error.localizedDescription]];
        }
    }
    return turboDisplayDirectory;
}

#pragma mark - Extra Cache Content

- (NSString *)extraCacheKeyFromMainCacheKey:(NSString *)mainCacheKey {
    // kuikly_turbo_display_9xxx.data -> kuikly_turbo_display_extra_xxx.json
    NSString *hash = [mainCacheKey stringByReplacingOccurrencesOfString:@"kuikly_turbo_display_9" withString:@""];
    hash = [hash stringByReplacingOccurrencesOfString:@".data" withString:@""];
    return [NSString stringWithFormat:@"kuikly_turbo_display_extra_%@.json", hash];
}

- (void)cacheWithViewNode:(KRTurboDisplayNode *)viewNode cacheKey:(NSString *)cacheKey extraCacheContent:(NSString *)extraCacheContent {
    // 1. 缓存节点树（复用现有逻辑）
    [self cacheWithViewNode:viewNode cacheKey:cacheKey];
    
    // 2. 缓存额外内容到独立文件
    if (extraCacheContent.length > 0) {
        [KuiklyRenderThreadManager performOnLogQueueWithBlock:^{
            @try {
                [self.fileLock lock];
                NSString *extraKey = [self extraCacheKeyFromMainCacheKey:cacheKey];
                NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:extraKey];
                NSData *data = [extraCacheContent dataUsingEncoding:NSUTF8StringEncoding];
                BOOL success = [data writeToFile:filePath atomically:YES];
            } @catch (NSException *exception) {
                [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when caching extra content:%@ key:%@", exception, cacheKey]];
            } @finally {
                [self.fileLock unlock];
            }
        }];
    }
}

- (NSString *)extraCacheContentWithCacheKey:(NSString *)cacheKey {
    NSString *extraContent = nil;
    @try {
        [self.fileLock lock];
        NSString *extraCacheKey = [self extraCacheKeyFromMainCacheKey:cacheKey];
        NSString *extraFilePath = [[self cacheRootPath] stringByAppendingPathComponent:extraCacheKey];
        
        if ([[NSFileManager defaultManager] fileExistsAtPath:extraFilePath]) {
            NSData *extraData = [NSData dataWithContentsOfFile:extraFilePath];
            if (extraData) {
                extraContent = [[NSString alloc] initWithData:extraData encoding:NSUTF8StringEncoding];
            }
        }
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when reading extra cache content:%@ key:%@", exception, cacheKey]];
    } @finally {
        [self.fileLock unlock];
    }
    return extraContent;
}

@end
 

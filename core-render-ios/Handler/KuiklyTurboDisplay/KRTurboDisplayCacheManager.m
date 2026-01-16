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

static NSString * const kTurboDisplayCacheKeyPrefix = @"kuikly_turbo_display_9";

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
    return [NSString stringWithFormat:@"%@%@.data", kTurboDisplayCacheKeyPrefix, key];
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
    [self.fileLock lock];
    @try {
        // 删除 TB 缓存文件
        NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
        [[NSFileManager defaultManager] removeItemAtPath:filePath error:nil];
        
        // 删除额外缓存文件
        NSString *extraCacheKey = [self extraCacheKeyFromMainCacheKey:cacheKey];
        NSString *extraFilePath = [[self cacheRootPath] stringByAppendingPathComponent:extraCacheKey];
        [[NSFileManager defaultManager] removeItemAtPath:extraFilePath error:nil];
       
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when removeCacheWithKey:%@ key:%@", exception, cacheKey]];
    } @finally {
        [self.fileLock unlock];
    }
}

// TB缓存写入 - TurboDisplayNode格式写入【独立写入】
- (void)cacheWithViewNode:(KRTurboDisplayNode *)viewNode cacheKey:(NSString *)cacheKey {
    
    [KuiklyRenderThreadManager performOnLogQueueWithBlock:^{
        @try {
            [self.fileLock lock];
            [self formatTagWithCacheTree:viewNode];
            
            NSError *archiveError = nil;
            NSData *nodeData = [NSKeyedArchiver archivedDataWithRootObject:viewNode requiringSecureCoding:NO error:&archiveError];
            if (archiveError) {
                [KRLogModule logError:[NSString stringWithFormat:@"Archive node error:%@ key:%@", archiveError.localizedDescription, cacheKey]];
                return;
            }
            if (!nodeData) {
                [KRLogModule logError:[NSString stringWithFormat:@"Archive node returned nil data, key:%@", cacheKey]];
                return;
            }
            
            NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
            [nodeData writeToFile:filePath atomically:YES];
           
        } @catch (NSException *exception) {
            [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when archived Node Data:%@ key:%@", exception, cacheKey]];
        } @finally {
            [self.fileLock unlock];
        }
    }];
   
}

// TB缓存写入 - 原子性写入TB缓存+额外缓存【推荐使用】
- (void)cacheWithViewNode:(KRTurboDisplayNode *)viewNode cacheKey:(NSString *)cacheKey extraCacheContent:(NSString *)extraCacheContent {
    
    [KuiklyRenderThreadManager performOnLogQueueWithBlock:^{
        [self.fileLock lock];
        @try {
            // 1. TB 首屏缓存
            // 树tag 格式化
            [self formatTagWithCacheTree:viewNode];
            NSError *archiveError = nil;
            NSData *nodeData = [NSKeyedArchiver archivedDataWithRootObject:viewNode requiringSecureCoding:NO error:&archiveError];
            if (archiveError) {
                [KRLogModule logError:[NSString stringWithFormat:@"Archive node error:%@ key:%@", archiveError.localizedDescription, cacheKey]];
                return; // TB 序列化失败，不写 extra
            }
            if (!nodeData) {
                [KRLogModule logError:[NSString stringWithFormat:@"Archive node returned nil data, key:%@", cacheKey]];
                return; // TB 序列化失败，不写 extra
            }
            
            NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
            BOOL tbWriteSuccess = [nodeData writeToFile:filePath atomically:YES];
            if (!tbWriteSuccess) {
                [KRLogModule logError:[NSString stringWithFormat:@"Write TB cache file failed, key:%@", cacheKey]];
                return; // TB 写入失败，不写 extra
            }
            
            // 2. 写入额外缓存（在同一个异步任务中，确保原子性）
            if (extraCacheContent.length > 0) {
                NSString *extraKey = [self extraCacheKeyFromMainCacheKey:cacheKey];
                NSString *extraFilePath = [[self cacheRootPath] stringByAppendingPathComponent:extraKey];
                NSData *extraData = [extraCacheContent dataUsingEncoding:NSUTF8StringEncoding];
                BOOL extraWriteSuccess = [extraData writeToFile:extraFilePath atomically:YES];
                if (!extraWriteSuccess) {
                    [KRLogModule logError:[NSString stringWithFormat:@"Write extra cache file failed, key:%@", cacheKey]];
                }
            }
           
        } @catch (NSException *exception) {
            [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when caching Node Data:%@ key:%@", exception, cacheKey]];
        } @finally {
            [self.fileLock unlock];
        }
    }];
}
// TB缓存写入 - NSData格式写入【独立写入】
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
            BOOL success = [nodeData writeToFile:filePath atomically:YES];
            if (!success) {
                [KRLogModule logError:[NSString stringWithFormat:@"Write TB cache NSData file failed, key:%@", cacheKey]];
            }
            
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
// TB缓存读取【读取后删除TB缓存和额外缓存】
- (KRTurboDisplayCacheData *)nodeWithCachKey:(NSString *)cacheKey {
    KRTurboDisplayCacheData *cacheData = nil;
    @try {
        [self.fileLock lock];
        NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
        
        // 使用 alloc init 而非 autorelease 方式读取数据，避免 mmap 后立即删除文件的潜在问题
        NSData *nodeData = [[NSData alloc] initWithContentsOfFile:filePath];
        
        // 1 读取并删除TB首屏缓存
        if (nodeData && nodeData.length > 0) {
            cacheData = [KRTurboDisplayCacheData new];
            
            NSError *unarchiverError = nil;
            NSKeyedUnarchiver *unarchiver = [[NSKeyedUnarchiver alloc] initForReadingFromData:nodeData error:&unarchiverError];
            
            if (unarchiverError) {
                [KRLogModule logError:[NSString stringWithFormat:@"NSKeyedUnarchiver init error:%@ key:%@", unarchiverError.localizedDescription, cacheKey]];
                cacheData = nil;
            }
            
            if (unarchiver) {
                unarchiver.requiresSecureCoding = NO;    // 关闭 Secure Coding，允许使用 decodeObjectForKey:
                cacheData.turboDisplayNode = [unarchiver decodeObjectForKey:NSKeyedArchiveRootObjectKey];
                [unarchiver finishDecoding];
            }
            cacheData.turboDisplayNodeData = nodeData;
        }
        
        // 显式置空 nodeData，确保在删除文件前释放对文件的引用
        nodeData = nil;
        
        // 2 仅删除 TB 缓存文件，读取的操作在 extraCacheContentWithCachKey 方法中调用
        [[NSFileManager defaultManager] removeItemAtPath:filePath error:nil];
        
        // 删除额外存储业务自定义缓存
        NSString *extraCacheKey = [self extraCacheKeyFromMainCacheKey:cacheKey];
        NSString *extraFilePath = [[self cacheRootPath] stringByAppendingPathComponent:extraCacheKey];
        [[NSFileManager defaultManager] removeItemAtPath:extraFilePath error:nil];
        
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
    NSString *hash = [mainCacheKey stringByReplacingOccurrencesOfString:kTurboDisplayCacheKeyPrefix withString:@""];
    hash = [hash stringByReplacingOccurrencesOfString:@".data" withString:@""];
    return [NSString stringWithFormat:@"kuikly_turbo_display_extra_%@.json", hash];
}

// 缓存「业务自定义内容」到独立文件【独立写入】
- (void)cacheWithExtraCacheContent:(NSString *)extraCacheContent cacheKey:(NSString *)cacheKey {
    if (extraCacheContent.length > 0) {
        [KuiklyRenderThreadManager performOnLogQueueWithBlock:^{
            @try {
                [self.fileLock lock];
                NSString *extraKey = [self extraCacheKeyFromMainCacheKey:cacheKey];
                NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:extraKey];
                NSData *data = [extraCacheContent dataUsingEncoding:NSUTF8StringEncoding];
                [data writeToFile:filePath atomically:YES];
            } @catch (NSException *exception) {
                [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when caching extra content:%@ key:%@", exception, cacheKey]];
            } @finally {
                [self.fileLock unlock];
            }
        }];
    }
}
// 缓存「业务自定义内容」到独立文件【只读取，不删除，由 nodeWithCachKey: 统一删除】
- (NSString *)extraCacheContentWithCacheKey:(NSString *)cacheKey {
    NSString *extraContent = nil;
    @try {
        [self.fileLock lock];
        NSString *extraCacheKey = [self extraCacheKeyFromMainCacheKey:cacheKey];
        NSString *extraFilePath = [[self cacheRootPath] stringByAppendingPathComponent:extraCacheKey];
        
        // 直接读取，无需先判断文件是否存在
        // 使用 alloc init 而非 autorelease 方式，避免 mmap + 提前删除的潜在问题
        NSData *extraData = [[NSData alloc] initWithContentsOfFile:extraFilePath];
        if (extraData && extraData.length > 0) {
            extraContent = [[NSString alloc] initWithData:extraData encoding:NSUTF8StringEncoding];
        }
        // 显式置空
        extraData = nil;
        
        // 注意：这里只读取，不删除文件，删除操作在 nodeWithCachKey: 中统一进行
        // 原因：如果在此处删除，而业务首屏还未到达就异常退出，下次启动将无法恢复额外缓存
        
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"An exception occurred when reading extra cache content:%@ key:%@", exception, cacheKey]];
    } @finally {
        [self.fileLock unlock];
    }
    return extraContent;
}

@end
 

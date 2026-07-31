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
             } else {
                [KRLogModule logInfo:[NSString stringWithFormat:@"[TurDisplay] %@ 目录下所有缓存文件全部被清除成功", folderPath]];
             }
         }
       
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"[TurboDisplay] Error: %s exception:%@", __FUNCTION__, exception]];
    
    } @finally {
        [self.fileLock unlock];
    }

}

- (void)removeCacheWithKey:(NSString *)cacheKey {
    
    @try {
        [self.fileLock lock];
        
        // 【修改】只需删除合并文件
        NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
        NSError *error;
        BOOL success = [[NSFileManager defaultManager] removeItemAtPath:filePath error:&error];
        if (!success) {
           [KRLogModule logError:[NSString stringWithFormat:@"%s failed:%@", __FUNCTION__, error.localizedDescription]];
        } else {
           [KRLogModule logInfo:[NSString stringWithFormat:@"[TurDisplay] %@ 缓存文件被清除成功", filePath]];
        }
        
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"[TurboDisplay] Error: An exception occurred when removeCacheWithKey:%@ key:%@", exception, cacheKey]];
    } @finally {
        [self.fileLock unlock];
    }
}

// 【缓存写入】 写入 TB缓存 + extraCacheContent 自定义缓存内容
- (void)cacheWithViewNode:(KRTurboDisplayNode *)viewNode
                 cacheKey:(NSString *)cacheKey
        extraCacheContent:(NSString *)extraCacheContent {
    
    [KuiklyRenderThreadManager performOnLogQueueWithBlock:^{
        [self.fileLock lock];
        @try {
            // 1. TB 首屏缓存序列化
            [self formatTagWithCacheTree:viewNode];
            
            NSError *archiveError = nil;
            NSData *nodeData = [NSKeyedArchiver archivedDataWithRootObject:viewNode requiringSecureCoding:NO error:&archiveError];
            if (archiveError) {
                [KRLogModule logError:[NSString stringWithFormat:@"TB node serizalition error:%@ key:%@", archiveError.localizedDescription, cacheKey]];
                return;
            }
            if (!nodeData) {
                [KRLogModule logError:[NSString stringWithFormat:@"[TurboDisplay] Error: Archive node returned nil data, key:%@", cacheKey]];
                return;
            }
            // 2. 【修改】构建合并数据：[4字节extra长度][extra内容][TB数据]
            uint32_t extraLength = 0;
            NSData *extraData = nil;
            if (extraCacheContent.length > 0) {
                extraData = [extraCacheContent dataUsingEncoding:NSUTF8StringEncoding];
                extraLength = (uint32_t)extraData.length;
                NSLog(@"[缓存写入] ExtraCacheContent的长度 %u", extraLength);
            }

            // 开始执行写入
            NSMutableData *combinedData = [NSMutableData data];
            
            
            // 写入1：extra长度（4字节，小端序）
            uint8_t lengthBytes[4] = {
                (extraLength >> 0) & 0xFF,
                (extraLength >> 8) & 0xFF,
                (extraLength >> 16) & 0xFF,
                (extraLength >> 24) & 0xFF
            };
            [combinedData appendBytes:lengthBytes length:4];
            
            // 写入2：extra内容（如果有）
            if (extraCacheContent.length > 0) {
                [combinedData appendData:extraData];
            }
            
            // 写入3：TB 首屏缓存
            [combinedData appendData:nodeData];
            
            // 3. 写入合并文件
            NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
            BOOL writeSuccess = [combinedData writeToFile:filePath atomically:YES];
            if (!writeSuccess) {
                [KRLogModule logError:[NSString stringWithFormat:@"[TurboDisplay] Error: Write TB cache file failed, key:%@", cacheKey]];
                return;
            }
        
        } @catch (NSException *exception) {
            [KRLogModule logError:[NSString stringWithFormat:@"[TurboDisplay] Error: An exception occurred when caching Node Data:%@ key:%@", exception, cacheKey]];
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
            [KRLogModule logInfo:[NSString stringWithFormat:@"缓存文件 key:%@ 存在", cacheKey]];
        }
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"[TurboDisplay] Error: An exception occurred when hasNodeWithCacheKey:%@ key:%@", exception, cacheKey]];
    } @finally {
        [self.fileLock unlock];
    }
    return res;
}

// 【读取】读取TB缓存 + extraCacheContent 额外自定义缓存 并且删除原始文件
- (KRTurboDisplayCacheData *)nodeWithCachKey:(NSString *)cacheKey {
    KRTurboDisplayCacheData *cacheData = nil;
    @try {
        [self.fileLock lock];
        NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
        
        
        // 读取整个合并文件
        NSData *combinedData = [[NSData alloc] initWithContentsOfFile:filePath];
        // 长度检验
        if (!combinedData || combinedData.length <= 4) {
            [self.fileLock unlock];
            return nil;
        }
        
        // 解析前4字节（extra长度）
        uint8_t *bytes = (uint8_t *)combinedData.bytes;
        uint32_t extraLength = (bytes[0]) | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24);
        NSLog(@"[缓存读取] ExtraCacheContent的长度 %u", extraLength);
        
        // 验证数据完整性
        if (combinedData.length <= 4 + extraLength) {
            [KRLogModule logError:[NSString stringWithFormat:@"Cache file corrupted, extraLength=%u, fileSize=%lu",
                                   extraLength, (unsigned long)combinedData.length]];
            [self.fileLock unlock];
            return nil;
        }
        
        // 提取 extra 内容（如果有）
        NSString *extraContent = nil;
        if (extraLength > 0) {
            NSData *extraData = [combinedData subdataWithRange:NSMakeRange(4, extraLength)];
            extraContent = [[NSString alloc] initWithData:extraData encoding:NSUTF8StringEncoding];
        }
        
        // 提取 TB 数据
        NSUInteger tbOffset = 4 + extraLength;
        NSUInteger tbLength = combinedData.length - tbOffset;
        NSData *nodeData = [combinedData subdataWithRange:NSMakeRange(tbOffset, tbLength)];
        
        // 反序列化 TB
        if (nodeData && nodeData.length > 0) {
            cacheData = [KRTurboDisplayCacheData new];
            
            NSError *unarchiverError = nil;
            NSKeyedUnarchiver *unarchiver = [[NSKeyedUnarchiver alloc] initForReadingFromData:nodeData error:&unarchiverError];
            
            if (unarchiverError) {
                [KRLogModule logError:[NSString stringWithFormat:@"[TurboDisplay] Error: NSKeyedUnarchiver init error:%@ key:%@",
                                       unarchiverError.localizedDescription, cacheKey]];
                cacheData = nil;
            }
            
            if (unarchiver) {
                unarchiver.requiresSecureCoding = NO;
                cacheData.turboDisplayNode = [unarchiver decodeObjectForKey:NSKeyedArchiveRootObjectKey];
                [unarchiver finishDecoding];
            }
            cacheData.turboDisplayNodeData = nodeData;
            cacheData.extraCacheContent = extraContent;  // 【新增】设置 extra 内容
        }
        
        // 【修改】统一删除合并文件
        [[NSFileManager defaultManager] removeItemAtPath:filePath error:nil];
        
        
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"[TurboDisplay] Error: An exception occurred when unarchived Node Data:%@ key:%@", exception, cacheKey]];
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
            [KRLogModule logError:[NSString stringWithFormat:@"[TurboDisplay] Error: fail to create TurboDisplay directory: %@", error.localizedDescription]];
        }
    }
    return turboDisplayDirectory;
}

#pragma mark - Extra Cache Content

// 缓存「业务自定义内容」到独立文件【只读取，不删除，由 nodeWithCachKey: 统一删除】
- (NSString *)extraCacheContentWithCacheKey:(NSString *)cacheKey {
    NSString *extraContent = nil;
    @try {
        [self.fileLock lock];
        
        // 【修改】从合并文件中读取 extra
        NSString *filePath = [[self cacheRootPath] stringByAppendingPathComponent:cacheKey];
        
        // 读取前4字节（extra长度）
        NSFileHandle *fileHandle = [NSFileHandle fileHandleForReadingAtPath:filePath];
        if (!fileHandle) {
            [self.fileLock unlock];
            return nil;
        }
        
        NSData *lengthData = [fileHandle readDataOfLength:4];
        if (lengthData.length != 4) {
            [fileHandle closeFile];
            [self.fileLock unlock];
            return nil;
        }
        
        uint8_t *lengthBytes = (uint8_t *)lengthData.bytes;
        uint32_t extraLength = (lengthBytes[0]) | (lengthBytes[1] << 8) | (lengthBytes[2] << 16) | (lengthBytes[3] << 24);
        
        // 读取 extra 内容
        if (extraLength > 0) {
            NSData *extraData = [fileHandle readDataOfLength:extraLength];
            if (extraData && extraData.length == extraLength) {
                extraContent = [[NSString alloc] initWithData:extraData encoding:NSUTF8StringEncoding];
            }
        }
        
        [fileHandle closeFile];
        // 【重要】不删除文件，等待 nodeWithCachKey 中读取并统一删除
        
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"[TurboDisplay] Error: An exception occurred when reading extra cache content:%@ key:%@", exception, cacheKey]];
    } @finally {
        [self.fileLock unlock];
    }
    return extraContent;
}

@end
 

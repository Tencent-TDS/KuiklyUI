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

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@class KRTurboDisplayNode;
@class KRTurboDisplayCacheData;
/*
 * @brief TurboDisplay首屏渲染指令二进制文件缓存管理
 */

@interface KRTurboDisplayCacheManager : NSObject

+ (instancetype)sharedInstance;
/*
 * @brief 缓存node
 */
- (void)cacheWithViewNode:(KRTurboDisplayNode *)viewNode cacheKey:(NSString *)key;
/*
 * @brief 获取缓存node（注：获取之后内部自动删除，避免缓存文件有问题时一直处于问题）
 */
- (KRTurboDisplayCacheData *)nodeWithCachKey:(NSString *)cacheKey;

- (void)removeCacheWithKey:(NSString *)cacheKey;

/*
 * @brief 返回对应缓存Key
 */
- (NSString *)cacheKeyWithTurboDisplayKey:(NSString *)turboDisplayKey pageName:(NSString *)pageName;

/*
 * @brief 删除所有TurboDisplay缓存文件
 */
- (void)removeAllTurboDisplayCacheFiles;
/*
 * @brief 是否存在该缓存key的节点
 */
- (BOOL)hasNodeWithCacheKey:(NSString *)cacheKey;
/*
 * @brief 缓存Node节点的NSData二进制数据，用于回写
 */
- (void)cacheWithViewNodeData:(NSData *)nodeData cacheKey:(NSString *)cacheKey;

@end

@interface KRTurboDisplayCacheData : NSObject

@property (nonatomic, strong, nullable) KRTurboDisplayNode *turboDisplayNode;
@property (nonatomic, strong, nullable) NSData *turboDisplayNodeData;

@end

NS_ASSUME_NONNULL_END

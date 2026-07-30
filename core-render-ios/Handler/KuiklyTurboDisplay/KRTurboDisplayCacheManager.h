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
 * @brief 原子性缓存：同时写入 TB 缓存和额外缓存（强烈推荐使用此方法）
 * 场景：用户在 ScrollEnd 后立即杀死 App，如果分开写入可能导致只写入了 extra 而 TB 未写入
 */
- (void)cacheWithViewNode:(KRTurboDisplayNode *)viewNode cacheKey:(NSString *)cacheKey extraCacheContent:(NSString * _Nullable)extraCacheContent;

/*
 * @brief 获取缓存node（注：获取之后内部自动删除，避免缓存文件有问题时一直处于问题）
 */
- (KRTurboDisplayCacheData *)nodeWithCachKey:(NSString *)cacheKey;

/*
 * @brief 删除 TB缓存 + 额外的自定义缓存 extraCacheContent
 */
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

/**
 * @brief 仅读取额外缓存内容（轻量级，用于initView时机）
 */
- (nullable NSString *)extraCacheContentWithCacheKey:(NSString *)cacheKey;

@end

@interface KRTurboDisplayCacheData : NSObject

@property (nonatomic, strong, nullable) KRTurboDisplayNode *turboDisplayNode;
@property (nonatomic, strong, nullable) NSData *turboDisplayNodeData;
@property (nonatomic, strong, nullable) NSString *extraCacheContent;    // 额外缓存内容（JSON字符串），用于存储业务自定义的View属性（如ListView的offset

@end

NS_ASSUME_NONNULL_END


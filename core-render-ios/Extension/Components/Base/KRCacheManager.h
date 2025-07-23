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

@protocol KRCacheProtocol <NSObject>

@optional

/*
 * 指定Kuikly sdk内所有缓存的根缓存路径，默认为系统Cache/kuikly作为目录
 */
- (NSString * _Nonnull)kr_rootCachePath;



@end

NS_ASSUME_NONNULL_BEGIN

@interface KRCacheManager : NSObject

/*
 * @brief 注册Cache的自定义实现
 */
+ (void)registerCacheHandler:(id<KRCacheProtocol>)cacheHandler;

+ (instancetype)sharedInstance;
/*
 * 根据文件夹名返回对应缓存全路径
 * @param folderName 文件夹名词，即返回rootCachePath/folderName, 如果该参数为空，则返回rootCachePath
 */
- (NSString *)cachePathWithFolderName:(NSString * _Nullable)folderName;


@end

NS_ASSUME_NONNULL_END

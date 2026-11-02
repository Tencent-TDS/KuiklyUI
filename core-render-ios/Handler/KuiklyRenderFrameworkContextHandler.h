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
#import "KuiklyRenderContextProtocol.h"
NS_ASSUME_NONNULL_BEGIN
/**
 * @brief framework执行环境的实现者
 */
@interface KuiklyRenderFrameworkContextHandler : NSObject<KuiklyRenderContextProtocol>

/*
 * @brief 对应contextCode是否framework模式
 */
+ (BOOL)isFrameworkWithContextCode:(NSString *)contextCode;
/*
 * @brief 判断PageName是否存在于当前Framework中
 * @param pageName 对应Kotin测@Page注解名字
 * @param frameworkName 编译出来的framework名字
 * @return 是否存在该页面
 */
+ (BOOL)isPageExistWithPageName:(NSString *)pageName frameworkName:(NSString *)frameworkName;
/*
 * @brief 根据framework名获取Kotlin/Native入口类
 */
+ (Class _Nullable)entryClassWithFrameworkName:(NSString *)frameworkName;
/*
 * @brief framework模式预热：在主线程提前执行一次Kotlin入口调用，
 * 完成Kotlin/Native runtime的主线程注册（规避finalizer 0xf8崩溃）
 * @discussion 本预热为 Kotlin/Native 2.1.21 专属 workaround
 * （KT-84331，上游 2.4.0 已修复），升级 Kotlin >= 2.4.0 后可移除
 * @param frameworkName 编译出来的framework名字
 */
+ (void)warmupMainThreadRuntimeWithFrameworkName:(nullable NSString *)frameworkName;

@end

NS_ASSUME_NONNULL_END

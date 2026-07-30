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

#import "KRBaseModule.h"

NS_ASSUME_NONNULL_BEGIN
@protocol KRRouterProtocol;
@interface KRRouterModule : KRBaseModule

+ (void)registerRouterHandler:(id<KRRouterProtocol>)routerHandler;

@end

@protocol KRRouterProtocol <NSObject>

@required
/*
 * @brief 打开kuikly页面
 * @param pageName kuikly页面名，为@Page注解名
 * @param pageData 页面的传参，数据类型为jsonObject，key&value为字符串/Number等基础数据类型
 * @param controller 当前页面所在controller
 */
- (void)openPageWithName:(NSString *)pageName
                pageData:(NSDictionary * _Nullable)pageData
              controller:(UIViewController *)controller;
/*
 * @brief 关闭当前页
 * @param controller 需要关闭的controller
 */
- (void)closePage:(UIViewController *)controller;

@end

NS_ASSUME_NONNULL_END

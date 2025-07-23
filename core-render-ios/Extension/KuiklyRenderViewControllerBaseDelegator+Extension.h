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

#import "KuiklyRenderViewControllerBaseDelegator.h"

NS_ASSUME_NONNULL_BEGIN

@interface KuiklyRenderViewControllerBaseDelegator (Extension)
/// kuikly页面名称
@property (nonatomic, strong) NSString *pageName;
/// kuikly视图
@property (nullable, nonatomic, weak) UIView * view;

/*
 * @brief 获取kmm工程打包的framework名字，并将获取到的名字传入callback处理
 * @param callback 处理获取到的framework名字的回调函数
 */
- (void)fetchContextCodeWithResultCallback:(KuiklyContextCodeCallback)callback;
/*
 * @brief 创建Kuikly接入模式实例
 * @param contextCode kmm工程打包的framework名字
 */
- (KuiklyBaseContextMode *)createContextMode:(NSString * _Nullable) contextCode;
/*
 * @brief 初始化renderView
 * @param contextCode kmm工程打包的framework名字
 */
- (void)initRenderViewWithContextCode:(NSString *)contextCode;

@end

NS_ASSUME_NONNULL_END


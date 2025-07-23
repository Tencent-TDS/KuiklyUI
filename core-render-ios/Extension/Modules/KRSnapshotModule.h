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
/*
 * @brief 页面快照模块，用于下次打开pager的首屏未出现时以快照作为首屏过渡，实现首屏无任何白屏体验
 */
@interface KRSnapshotModule : KRBaseModule
/*
 * @brief 获取页面快照
 * @param snapshotKey 同kotlin侧设置snapshotPager方法传入的key
 * @return 返回对应的快照图片
 */
+ (UIImage *)snapshotPagerWithSnapshotKey:(NSString *)snapshotKey;

@end

NS_ASSUME_NONNULL_END

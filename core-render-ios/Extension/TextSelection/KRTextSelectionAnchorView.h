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

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/// 游标视图
@interface KRTextSelectionAnchorView : UIView

/// 是否是起始游标（头在上）
@property (nonatomic, assign, readonly) BOOL isTop;

/// 圆形头部的直径
+ (CGFloat)anchorCapDiameter;

/// 圆形头部超出文字行高的偏移量（用于定位游标）
+ (CGFloat)anchorCapOffset;

/// 默认的游标宽度（通常等于圆形头部直径）
+ (CGFloat)defaultAnchorWidth;

/// 默认的游标高度（不含文字行高）
+ (CGFloat)defaultAnchorHeight;

/// 设置是否是顶部游标
- (void)setIsTop:(BOOL)isTop;

/// 设置游标行高
- (void)setAnchorLineHeight:(CGFloat)height;

/// 设置游标颜色
- (void)setColor:(UIColor *)color;

@end

NS_ASSUME_NONNULL_END


//
//  UIScrollView+KuiklyCoordinator.h
//  QQNews
//
//  Created by 李庭栋 on 2025/9/9.
//  Copyright © 2025 Tencent. All rights reserved.
//

#import "KRUIKit.h" // [macOS]

NS_ASSUME_NONNULL_BEGIN
@class NestedScrollCoordinator;
// UIScrollView拓展类，用于处理与宿主的嵌套联动
@interface UIScrollView (KuiklyCoordinator)
/// parentScroll是否处于锁定状态
@property(nonatomic, assign) BOOL isParentLocked;
/// scrollview所在联动控制器
@property(nonatomic, weak) NestedScrollCoordinator *coordinator;

@end

NS_ASSUME_NONNULL_END

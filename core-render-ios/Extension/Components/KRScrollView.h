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
#import "KuiklyRenderViewExportProtocol.h"
#import "NestedScrollCoordinator.h"
#import "NestedScrollProtocol.h"

#import "KRView.h"
NS_ASSUME_NONNULL_BEGIN

/*
 * @brief 暴露给Kotlin侧调用的Scoller组件
 */
@interface KRScrollView : UIScrollView<KuiklyRenderViewExportProtocol, UIScrollViewDelegate, NestedScrollProtocol>

@property (nonatomic, assign) BOOL autoAdjustContentOffsetDisable ;
@property (nonatomic, assign) BOOL setContentSizeing ;

/// Record the last content offset for scroll lock.
@property (nonatomic, assign) CGPoint lastContentOffset;

/// Nested scroll coordinator
@property (nonatomic, strong) NestedScrollCoordinator *nestedScrollCoordinator;

/*
 * 添加滚动监听
 */
- (void)addScrollViewDelegate:(id<UIScrollViewDelegate>)scrollViewDelegate;
/*
 * 删除滚动监听
 */
- (void)removeScrollViewDelegate:(id<UIScrollViewDelegate>)scrollViewDelegate;

@end

@protocol KRScrollContentViewDelegate <NSObject>
@optional
- (void)contentViewDidInsertSubview;

@end

@interface KRScrollContentView : KRView<KuiklyRenderViewExportProtocol>
/*
 * 添加滚动监听
 */
- (void)addScrollContentViewDelegate:(id<KRScrollContentViewDelegate>)scrollContentViewDelegate;
/*
 * 删除滚动监听
 */
- (void)removeScrollContentViewDelegate:(id<KRScrollContentViewDelegate>)scrollContentViewDelegate;
@end



NS_ASSUME_NONNULL_END

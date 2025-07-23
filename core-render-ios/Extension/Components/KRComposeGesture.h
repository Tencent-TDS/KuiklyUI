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
#import "KRView.h"

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSUInteger, TouchesEventKind) {
    TouchesEventKindBegin = 1,
    TouchesEventKindMoved,
    TouchesEventKindEnd,
    TouchesEventKindCancel,
    TouchesEventKindRedirected
};

/**
 * 自定义手势识别器，可以捕获所有触摸事件
 * 用于解决 KRView 与 ScrollView 子节点的触摸事件冲突问题
 */
@interface KRComposeGestureRecognizer : UIGestureRecognizer

/**
 * 触摸回调
 */
@property (nonatomic, strong, nullable) void (^onTouchCallback)(NSSet<UITouch *> *touches, UIEvent *event, TouchesEventKind phase);

@end

/**
 * 手势处理器，负责处理手势与 ScrollView 的交互
 */
@interface KRComposeGestureHandler : NSObject <UIGestureRecognizerDelegate>

/**
 * 持有该手势处理器的视图
 */
@property (nonatomic, weak) KRView *containerView;

/**
 * 原生滚动手势集合
 */
@property (nonatomic, strong) NSMutableSet *nativeScrollGestures;

/**
 * 是否开启原生手势
 */
@property (nonatomic, assign) BOOL enableNativeGesture;

/**
 * 初始化方法
 */
- (instancetype)initWithContainerView:(UIView *)containerView;

/**
 * 从触摸事件生成参数
 */
- (NSDictionary *)generateParamsWithTouches:(NSSet<UITouch *> *)touches event:(UIEvent *)event eventName:(NSString *)eventName;

/**
 * 原生的pan手势是否正在进行中
 */
- (BOOL)nativeScrollGestureOnGoing;

@end

NS_ASSUME_NONNULL_END 

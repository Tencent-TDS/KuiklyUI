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

#import "KRComposeGesture.h"
#import "KRView.h"

#pragma mark - KRTouchGestureRecognizer


@interface KRComposeGestureRecognizer ()

// 跟踪所有活跃的触摸点
@property (nonatomic, strong) NSMutableSet<UITouch *> *trackedTouches;

@end

@implementation KRComposeGestureRecognizer

- (instancetype)init {
    self = [super init];
    if (self) {
#if !TARGET_OS_OSX // [macOS]
        // 设置手势识别器属性，确保不会干扰其他触摸事件
        self.cancelsTouchesInView = YES;
        self.delaysTouchesBegan = YES;
#endif // [macOS]
        
        // 初始化跟踪的触摸点集合
        self.trackedTouches = [NSMutableSet new];
    }
    return self;
}

- (BOOL)isOngoing {
    return self.state == UIGestureRecognizerStateBegan || self.state == UIGestureRecognizerStateChanged;
}

- (BOOL)startTrackingTouches:(NSSet<UITouch *> *)touches {
    for (UITouch *touch in touches) {
        [self.trackedTouches addObject:touch];
    }
    return self.trackedTouches.count == 0;
}

- (void)onTouchesEvent:(NSSet<UITouch *> *)touches event:(UIEvent *)event phase:(TouchesEventKind)phase {
    if (self.onTouchCallback) {
        self.onTouchCallback(touches, event, phase);
    }
}

- (void)checkPanIntent {
}

- (void)stopTrackingTouches:(NSSet<UITouch *> *)touches {
#if !TARGET_OS_OSX // [macOS]
    for (UITouch *touch in touches) {
        [self.trackedTouches removeObject:touch];
    }
#else // [macOS]
    NSMutableSet<NSEvent *> *touchesToRemove = [NSMutableSet setWithCapacity:touches.count];
    for (UITouch *touch in touches) {
        for (NSEvent *trackedEvent in self.trackedTouches) {
            if (touch.eventNumber == trackedEvent.eventNumber) {
                [touchesToRemove addObject:trackedEvent];
            }
        }
    }
    [self.trackedTouches minusSet:touchesToRemove];
#endif // [macOS]
}

#if !TARGET_OS_OSX // [macOS]

// 重写触摸事件方法，直接传递所有触摸事件
- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {

    BOOL areTouchesInitial = [self startTrackingTouches:touches];

    [self onTouchesEvent:self.trackedTouches event:event phase:TouchesEventKindBegin];
    
    if ([self isOngoing]) {
        switch (self.state) {
            case UIGestureRecognizerStatePossible:
                self.state = UIGestureRecognizerStateBegan;
                break;
            case UIGestureRecognizerStateBegan:
            case UIGestureRecognizerStateChanged:
                self.state = UIGestureRecognizerStateChanged;
                break;
            default:
                break;
        }
    } else {
        if (!areTouchesInitial) {
            [self checkPanIntent];
        }
    }
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self onTouchesEvent:_trackedTouches event:event phase:TouchesEventKindMoved];
    
    if ([self isOngoing]) {
        self.state = UIGestureRecognizerStateChanged;
    } else {
        [self checkPanIntent];
    }
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self onTouchesEvent:_trackedTouches event:event phase:TouchesEventKindEnd];
    [self stopTrackingTouches:touches];
    
    if ([self isOngoing]) {
        self.state = self.trackedTouches.count == 0 ? UIGestureRecognizerStateEnded : UIGestureRecognizerStateChanged;
    } else {
        if (self.trackedTouches.count == 0) {
            self.state = UIGestureRecognizerStateFailed;
        }
    }
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self onTouchesEvent:_trackedTouches event:event phase:TouchesEventKindCancel];
    [self stopTrackingTouches:touches];
    
    if ([self isOngoing]) {
        self.state = self.trackedTouches.count == 0 ? UIGestureRecognizerStateCancelled : UIGestureRecognizerStateEnded;
    } else {
        if (self.trackedTouches.count == 0) {
            self.state = UIGestureRecognizerStateFailed;
        }
    }
}

#else // [macOS

// macOS: 使用鼠标事件替代触摸事件（NSGestureRecognizer 不支持 touches* 方法）
// 注意：macOS 上每个 mouseDown/mouseDragged/mouseUp 都是独立的 NSEvent 对象，
// 其 hash 值不同。为了让 Compose 层手势检测器能正确追踪指针（依赖 pointerId 一致性），
// 需要在整个鼠标操作序列中使用当前最新的 NSEvent，并保持 trackedTouches 的更新。

- (void)mouseDown:(NSEvent *)event {
    // 清空旧的追踪集合，开始新的鼠标操作序列
    [self.trackedTouches removeAllObjects];
    NSSet<UITouch *> *touches = [NSSet setWithObject:(UITouch *)event];
    [self startTrackingTouches:touches];
    
    [self onTouchesEvent:self.trackedTouches event:(UIEvent *)event phase:TouchesEventKindBegin];
    
    // macOS 的 NSGestureRecognizer 必须在 mouseDown 中将 state 设为 Began，
    // 否则 AppKit 不会将后续的 mouseDragged/mouseUp 事件分发给该手势识别器。
    // 这与 iOS 的 UIGestureRecognizer 行为不同——iOS 在 Possible 状态下也会分发所有 touch 事件。
    self.state = UIGestureRecognizerStateBegan;
}

- (void)mouseDragged:(NSEvent *)event {
    // 替换 trackedTouches 中的旧事件为当前事件，确保坐标是最新的
    [self.trackedTouches removeAllObjects];
    [self.trackedTouches addObject:(UITouch *)event];
    
    [self onTouchesEvent:_trackedTouches event:(UIEvent *)event phase:TouchesEventKindMoved];
    
    self.state = UIGestureRecognizerStateChanged;
}

- (void)mouseUp:(NSEvent *)event {
    // 使用当前 mouseUp 事件替换，确保 up 坐标正确
    [self.trackedTouches removeAllObjects];
    [self.trackedTouches addObject:(UITouch *)event];
    
    [self onTouchesEvent:_trackedTouches event:(UIEvent *)event phase:TouchesEventKindEnd];
    [self.trackedTouches removeAllObjects];
    
    self.state = UIGestureRecognizerStateEnded;
}

- (void)mouseCancelled:(NSEvent *)event {
    [self.trackedTouches removeAllObjects];
    [self.trackedTouches addObject:(UITouch *)event];
    
    [self onTouchesEvent:_trackedTouches event:event phase:TouchesEventKindCancel];
    [self.trackedTouches removeAllObjects];
    
    self.state = UIGestureRecognizerStateCancelled;
}

#endif // macOS]

// 重写 reset 方法，在手势识别结束后重置状态
- (void)reset {
#if TARGET_OS_OSX
    // macOS: 如果 gesture 在有未配对 Press 的情况下被 AppKit 重置（例如 NSTextView 
    // 抢走 first responder 导致 mouseUp 丢失），需要主动合成一次 Cancel 事件发给 
    // Compose，让 Compose 的 pointer 状态机（HitPathTracker）清理被卡住的 hit path。
    // 否则下一次点击外部组件时，Compose 会将新的 Press 错误地归为"旧事件的延续"，
    // 分发到旧的 hit path（如 TextField），新按钮收不到 Press → onClick 不触发，
    // 必须点击两次才能生效。
    // 注意：只检查 state != Ended（正常结束），不排除 Cancelled——因为 AppKit 可能
    // 在 reset 前将 state 设为 Cancelled 但不调用 mouseCancelled:，此时 Compose
    // 仍未收到配对的 Cancel/Release 事件。
    if (self.trackedTouches.count > 0 && self.state != UIGestureRecognizerStateEnded) {
        NSSet *pending = [self.trackedTouches copy];
        [self onTouchesEvent:pending event:(UIEvent *)pending.anyObject phase:TouchesEventKindCancel];
    }
#endif
    [super reset];
    [self.trackedTouches removeAllObjects];
}

@end

#pragma mark - ComposeGestureHandler

@implementation KRComposeGestureHandler

- (instancetype)initWithContainerView:(KRView *)containerView {
    self = [super init];
    if (self) {
        _containerView = containerView;
        _nativeScrollGestures = [NSMutableSet new];
        _enableNativeGesture = YES;
    }
    return self;
}

#pragma mark - UIGestureRecognizerDelegate

// 允许手势识别器与其他手势同时工作
- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    // 允许 Pan 手势与 ScrollView 的手势同时工作
    if ([otherGestureRecognizer isKindOfClass:[UIPanGestureRecognizer class]]) {
        [self.nativeScrollGestures addObject:otherGestureRecognizer];
        
        if (!self.enableNativeGesture) {
            otherGestureRecognizer.enabled = NO;
        }
        
//        NSLog(@"xxxxx touch shouldRecognizeSimultaneouslyWithGestureRecognizer YES");
        return YES;
    }
    return NO;
}

// 确保我们的手势不会阻止其他手势
- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRequireFailureOfGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    return NO;
}

// 确保其他手势不会阻止我们的手势
- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldBeRequiredToFailByGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    return NO;
}

// 始终允许我们的手势开始
- (BOOL)gestureRecognizerShouldBegin:(UIGestureRecognizer *)gestureRecognizer {
    return YES;
}

#pragma mark - 参数生成

// 从触摸事件生成参数
- (NSDictionary *)generateParamsWithTouches:(NSSet<UITouch *> *)touches event:(UIEvent *)event eventName:(NSString *)eventName {
    NSMutableArray *touchesParam = [NSMutableArray new];
    
#if !TARGET_OS_OSX // [macOS]
    // 处理所有触摸点
    for (UITouch *touch in touches) {
        CGPoint locationInSelf = [touch locationInView:self.containerView];
        CGPoint locationInRootView = [touch locationInView:(UIView *)[self.containerView hr_rootView]];
                        
        [touchesParam addObject:@{
            @"x" : @(locationInSelf.x),
            @"y" : @(locationInSelf.y),
            @"pageX" : @(locationInRootView.x),
            @"pageY" : @(locationInRootView.y),
            @"hash" : @(touch.hash),
            @"pointerId" : @(touch.hash),  // 使用 touch.hash 作为唯一的 pointerId
        }];
    }
#else // [macOS
    // macOS: 从 NSEvent 提取鼠标位置信息
    // macOS 上鼠标永远是单点操作，使用固定的 pointerId 确保
    // mouseDown/mouseDragged/mouseUp 序列中 pointerId 一致，
    // 否则 Compose 手势检测器无法正确追踪指针。
    static const NSUInteger kMacMousePointerId = 0;
    for (UITouch *touch in touches) {
        NSEvent *mouseEvent = (NSEvent *)touch;
        CGPoint locationInSelf = [self.containerView convertPoint:mouseEvent.locationInWindow fromView:nil];
        UIView *rootView = (UIView *)[self.containerView hr_rootView];
        CGPoint locationInRootView = [rootView convertPoint:mouseEvent.locationInWindow fromView:nil];
        if (![rootView isFlipped]) {
            // manually flipped the y-coordinate if rootView does not flip coordinate system.
            locationInRootView.y = rootView.bounds.size.height - locationInRootView.y;
        }
        
        [touchesParam addObject:@{
            @"x" : @(locationInSelf.x),
            @"y" : @(locationInSelf.y),
            @"pageX" : @(locationInRootView.x),
            @"pageY" : @(locationInRootView.y),
            @"hash" : @(kMacMousePointerId),
            @"pointerId" : @(kMacMousePointerId),
        }];
    }
#endif // macOS]
    
    // 创建包含触摸点数组的完整参数
    NSMutableDictionary *result = touchesParam.count > 0 ? [touchesParam.firstObject mutableCopy] : [@{} mutableCopy];
    result[@"touches"] = touchesParam;
    result[@"action"] = eventName;
    result[@"consumed"] =  @(self.nativeScrollGestureOnGoing ? 1 : 0);
    result[@"timestamp"] = @(event.timestamp * 1000);  // 将时间戳转换为毫秒
    
    return result;
}

// 启用或禁用原生手势
- (void)setEnableNativeGesture:(BOOL)enableNativeGesture {
    _enableNativeGesture = enableNativeGesture;
    
//    NSLog(@"xxxxx touch ComposeGestureHandler: enableNativeGesture: %i, size: %lu", enableNativeGesture, (unsigned long)self.nativeScrollGestures.count);
    for (UIGestureRecognizer *gesture in self.nativeScrollGestures) {
        gesture.enabled = enableNativeGesture;
    }
}

- (BOOL)nativeScrollGestureOnGoing {
    for (UIPanGestureRecognizer *ges in self.nativeScrollGestures) {
        if (ges.state == UIGestureRecognizerStateBegan || ges.state == UIGestureRecognizerStateChanged) {
            return YES;
        }
    }
    return NO;
}

@end

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
- (void)mouseDown:(NSEvent *)event {
    // 在 macOS 上创建模拟的触摸对象
    NSSet<UITouch *> *touches = [NSSet setWithObject:(UITouch *)event];
    BOOL areTouchesInitial = [self startTrackingTouches:touches];
    
    [self onTouchesEvent:self.trackedTouches event:(UIEvent *)event phase:TouchesEventKindBegin];
    
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

- (void)mouseDragged:(NSEvent *)event {
    [self onTouchesEvent:_trackedTouches event:(UIEvent *)event phase:TouchesEventKindMoved];
    
    if ([self isOngoing]) {
        self.state = UIGestureRecognizerStateChanged;
    } else {
        [self checkPanIntent];
    }
}

- (void)mouseUp:(NSEvent *)event {
    NSSet<UITouch *> *touches = [NSSet setWithObject:(UITouch *)event];
    [self onTouchesEvent:_trackedTouches event:(UIEvent *)event phase:TouchesEventKindEnd];
    [self stopTrackingTouches:touches];
    
    if ([self isOngoing]) {
        self.state = self.trackedTouches.count == 0 ? UIGestureRecognizerStateEnded : UIGestureRecognizerStateChanged;
    } else {
        if (self.trackedTouches.count == 0) {
            self.state = UIGestureRecognizerStateFailed;
        }
    }
}

- (void)mouseCancelled:(NSEvent *)event {
    NSSet<UITouch *> *touches = [NSSet setWithObject:(UITouch *)event];
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

#endif // macOS]

// 重写 reset 方法，在手势识别结束后重置状态
- (void)reset {
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
            @"hash" : @(mouseEvent.hash),
            @"pointerId" : @(mouseEvent.hash),  // 使用 event.hash 作为唯一的 pointerId
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

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

#import "KRView+Compose.h"
#import <objc/runtime.h>
#import "KRScrollView.h"
#import "KRComposeGesture.h"

@interface KRView ()

@property (nonatomic, strong) KRComposeGestureRecognizer *composeGesture;
@property (nonatomic, strong) KRComposeGestureHandler *composeGesHandler;

@end

@implementation KRView (Compose)

- (KRComposeGestureRecognizer *)composeGesture {
    return objc_getAssociatedObject(self, @selector(composeGesture));
}

- (void)setComposeGesture:(KRComposeGestureRecognizer *)composeGesture {
    objc_setAssociatedObject(self, @selector(composeGesture), composeGesture, OBJC_ASSOCIATION_RETAIN);
}

- (KRComposeGestureHandler *)composeGesHandler {
    return objc_getAssociatedObject(self, @selector(composeGesHandler));
}

- (void)setComposeGesHandler:(KRComposeGestureHandler *)composeGesHandler {
    objc_setAssociatedObject(self, @selector(composeGesHandler), composeGesHandler, OBJC_ASSOCIATION_RETAIN);
}

- (NSNumber *)css_superTouch {
    return objc_getAssociatedObject(self, @selector(css_superTouch));
}

- (void)setCss_superTouch:(NSNumber *)css_superTouch {
    if (self.css_superTouch != css_superTouch) {
        objc_setAssociatedObject(self, @selector(css_superTouch), css_superTouch, OBJC_ASSOCIATION_RETAIN);
        
        // 移除现有的手势识别器
        if (self.composeGesture) {
            [self removeGestureRecognizer:self.composeGesture];
            self.composeGesture = nil;
        }
                
        if ([css_superTouch boolValue]) {
            // 创建手势处理器
            self.composeGesHandler = [[KRComposeGestureHandler alloc] initWithContainerView:self];
            
            // 创建并配置自定义触摸手势识别器
            self.composeGesture = [[KRComposeGestureRecognizer alloc] init];
            __weak typeof(self) weakSelf = self;

            self.composeGesture.onTouchCallback = ^(NSSet<UITouch *> * _Nonnull touches, UIEvent * _Nonnull event, TouchesEventKind phase) {
                                
                switch (phase) {
                    case TouchesEventKindBegin:
                        if ([weakSelf.composeGesHandler nativeScrollGestureOnGoing]) {
//                            NSLog(@"xxxxx touch 原生滑动中，不接受新事件");
                            return;
                        }
                        
                        if (weakSelf.css_touchDown) {
                            NSDictionary *params = [weakSelf.composeGesHandler generateParamsWithTouches:touches event:event eventName:@"touchDown"];
                            weakSelf.css_touchDown(params);
                        }
                        break;
                    case TouchesEventKindMoved:
                        if (weakSelf.css_touchMove) {
                            NSDictionary *params = [weakSelf.composeGesHandler generateParamsWithTouches:touches event:event eventName:@"touchMove"];
                            weakSelf.css_touchMove(params);
                        }
                        break;
                    case TouchesEventKindEnd:
                        if (weakSelf.css_touchUp) {
                            NSDictionary *params = [weakSelf.composeGesHandler generateParamsWithTouches:touches event:event eventName:@"touchUp"];
                            weakSelf.css_touchUp(params);
                        }
                        weakSelf.composeGesHandler.enableNativeGesture = YES;
                        break;
                    case TouchesEventKindCancel:
                        if (weakSelf.css_touchUp) {
                            NSDictionary *params = [weakSelf.composeGesHandler generateParamsWithTouches:touches event:event eventName:@"touchCancel"];
                            weakSelf.css_touchUp(params);
                        }
                        weakSelf.composeGesHandler.enableNativeGesture = YES;
                        break;
                    default:
                        break;
                }
            };
                        
            // 设置手势识别器的代理
            self.composeGesture.delegate = self.composeGesHandler;
            
            // 添加手势识别器
            [self addGestureRecognizer:self.composeGesture];
        }
    }
}

- (NSNumber *)css_preventTouch {
    return objc_getAssociatedObject(self, @selector(css_preventTouch));
}

- (void)setCss_preventTouch:(NSNumber *)css_preventTouch {
    if (self.css_preventTouch != css_preventTouch) {
        objc_setAssociatedObject(self, @selector(css_preventTouch), css_preventTouch, OBJC_ASSOCIATION_RETAIN);
        
//        NSLog(@"xxxxx touch prevent touch %i", [css_preventTouch boolValue]);
        self.composeGesHandler.enableNativeGesture = ![css_preventTouch boolValue];
    }
}

@end

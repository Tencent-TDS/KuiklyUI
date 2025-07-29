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

#import "KRMultiDelegateProxy.h"
#import "KRWeakObject.h"
@interface KRMultiDelegateProxy()
/* 代理集合 */
@property (nonatomic, strong) NSMutableArray *delegates;

@end

@implementation KRMultiDelegateProxy

- (void)addDelegate:(NSObject *)delegate {
    if (delegate) {
        [self.delegates addObject:[[KRWeakObject alloc] initWithObject:delegate]];
    }
}

- (void)removeDelegate:(NSObject *)delegate {
    for (KRWeakObject *weakObject in [self.delegates copy]) {
        if (weakObject.weakObject == delegate) {
            [self.delegates removeObject:weakObject];
            break;
        }
    }
}

#pragma mark - override

- (BOOL)respondsToSelector:(SEL)aSelector {
    for (KRWeakObject *weakObject in [self.delegates copy]) {
        if ([weakObject.weakObject respondsToSelector:aSelector]) {
            return YES;
        }
    }
    return NO;
}

- (NSMethodSignature *)methodSignatureForSelector:(SEL)aSelector{
    NSMethodSignature *sig = nil;
    if (!sig) {
        for (KRWeakObject *weakObject in [self.delegates copy]) {
            if ((sig = [weakObject.weakObject methodSignatureForSelector:aSelector])) {
                break;
            }
        }
    }
    return sig;
}

// 转发方法调用给所有delegate
- (void)forwardInvocation:(NSInvocation *)anInvocation{
    for (KRWeakObject *weakObject in [self.delegates copy]) {
        if ([weakObject.weakObject respondsToSelector:anInvocation.selector]) {
            [anInvocation invokeWithTarget:weakObject.weakObject];
        }
    }
}

#pragma mark - getter

- (NSMutableArray *)delegates {
    if (!_delegates) {
        _delegates = [[NSMutableArray alloc] init];
    }
    return _delegates;
}

@end

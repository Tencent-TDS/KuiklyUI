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

#import "KRReflectionModule.h"
#import "KRConvertUtil.h"
#import "NSObject+KR.h"
#import "KuiklyRenderThreadManager.h"
#import "KuiklyRenderView.h"
#define SPLIT_TAG @"\n$\t&@\n"


@interface KROCObject : NSObject
/// 引用计数，默认为0
@property (nonatomic, assign) NSUInteger krRetainCount;
@property (nonatomic, strong) NSObject *ocObject;

@end

@implementation KROCObject

- (void)dealloc {
    
}

@end

/*
 * kuikly反射模块，提供动态调用Native Api能力
 */

@interface KRReflectionModule()

@property (nonatomic, strong) NSMutableDictionary<NSString *, KROCObject *> *objectRegistry;
@property (nonatomic, assign) long long autoObjectID;
@property (nonatomic, assign) BOOL needAutoReleaseNextLoop;
@property (nonatomic, assign) BOOL disable;

@end

@implementation KRReflectionModule

- (instancetype)init {
    if (self = [super init]) {
        _objectRegistry = [[NSMutableDictionary alloc] init];
    }
    return self;
}

- (id)hrv_callWithMethod:(NSString *)method params:(id)params callback:(KuiklyRenderCallback)callback {
    if (_disable) {
        return nil;
    }
    return [super hrv_callWithMethod:method params:params callback:callback];
}
// call by kotlin
- (NSString *)retain:(NSDictionary *)args {
    NSString *objectID = args[KR_PARAM_KEY];
    id object = [self p_objectWithID:objectID];
    if ([object isKindOfClass:[KROCObject class]]) {
        ((KROCObject *)object).krRetainCount++;
    }
    return nil;
}
// call by kotlin
- (NSString *)release:(NSDictionary *)args {
    NSString *objectID = args[KR_PARAM_KEY];
    id object = [self p_objectWithID:objectID];
    if ([object isKindOfClass:[KROCObject class]]) {
        ((KROCObject *)object).krRetainCount--;
        [self p_setNeedAutoRelease];
    }
    return nil;
}

// call by kotlin
- (NSString *)toString:(NSDictionary *)args {
    NSString *objectID = args[KR_PARAM_KEY];
    id object = [self p_objectWithID:objectID];
    if (!object) {
        return nil;
    }
    if ([object isKindOfClass:[KROCObject class]]) {
        NSObject *ocObject =  ((KROCObject *)object).ocObject;
        if ([ocObject respondsToSelector:@selector(stringValue)]) {
            return [((NSNumber *)ocObject) stringValue];
        }
        if ([ocObject isKindOfClass:[NSString class]]) {
            return (NSString *)ocObject;
        }
        return [ocObject description];
    } else {
        return NSStringFromClass(object); // class to string
    }
}

// call by kotlin
- (NSString *)invoke:(NSDictionary *)args {
    NSString *param = args[KR_PARAM_KEY];
    
    NSArray<NSString *> *paramsSplits = [param componentsSeparatedByString:SPLIT_TAG];
    NSArray<NSString *> *objectMethodSplits = [((NSString *)paramsSplits.firstObject) componentsSeparatedByString:@"|"];
    NSString *objectID = objectMethodSplits.firstObject ?: @"";
    id object = [self p_objectWithID:objectID];
    if (!object) {
        return nil;
    }
    NSString *methodName = objectMethodSplits.lastObject;
    NSArray *argObjects = [self p_argsWithSplits:paramsSplits method:methodName];
    SEL selector = NSSelectorFromString(methodName);
    NSObject *returnObject = nil;
    if ([object isKindOfClass:[KROCObject class]]) { // 调用实例方法
        NSObject *ocObject = ((KROCObject *)object).ocObject;
        if (![ocObject respondsToSelector:selector]) {
            [self p_alertWithTitle:@"反射对象方法失败" message:[NSString stringWithFormat:@"方法名:%@ 不存在", methodName]];
            return nil;
        }
        returnObject = [NSObject kr_performWithTarget:ocObject selector:selector withObjects:argObjects];
    } else { // Class 调用类方法
        if (![object respondsToSelector:selector]) {
            [self p_alertWithTitle:@"反射类方法失败" message:[NSString stringWithFormat:@"类:%@, 类方法名:%@ 不存在", object, methodName]];
            return nil;
        }
        returnObject = [NSObject kr_performWithClass:(Class)object selector:selector withObjects:argObjects];
    }
    
    if (returnObject) {
        return [self p_setWithObject:returnObject];
    }
    
    return nil;
}

- (void)p_alertWithTitle:(NSString *)title message:(NSString *)message {
    self.disable = YES;
    NSLog(@"%@|%@", title, message);
#if DEBUG
    [KRConvertUtil hr_alertWithTitle:title message:message];
#else
    NSAssert(false, [NSString stringWithFormat:@"%@|%@", title, message]);
#endif
}

// get / set
- (id)p_objectWithID:(NSString *)objectID {
    id ocObject = self.objectRegistry[objectID];
    if (!ocObject) {
        ocObject = NSClassFromString(objectID);
    }
    if (!ocObject) {
        if ([objectID longLongValue] > 0) {
            NSString *message = [NSString stringWithFormat:@"获取对象ID %@不存在，对象可能被自动清理，可尝试retain()/release()手动管理内存", objectID];
            [self p_alertWithTitle:@"反射获取对象失败" message:message];
        } else {
            NSString *message  = [NSString stringWithFormat:@"类名：%@ 不存在，可能已经改名或者贴错，建议iOS上复制类名", objectID];
            [self p_alertWithTitle:@"反射类名失败" message:message];
        }
    }
    return ocObject;
}

- (NSString *)p_setWithObject:(NSObject *)object {
    KROCObject *ocObject = [KROCObject new];
    ocObject.ocObject = object;
    self.autoObjectID++;
    NSString *objecgID = [@(self.autoObjectID) stringValue];
    self.objectRegistry[objecgID] = ocObject;
    [self p_setNeedAutoRelease];
    return objecgID;
}


- (void)p_setNeedAutoRelease {
    if (!self.needAutoReleaseNextLoop) {
        self.needAutoReleaseNextLoop = YES;
        __weak typeof(self) weakSelf = self;
        [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
            [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
                [[weakSelf.objectRegistry copy] enumerateKeysAndObjectsUsingBlock:^(NSString * _Nonnull key,
                                                                                    KROCObject * _Nonnull obj,
                                                                                    BOOL * _Nonnull stop) {
                    KROCObject *ocObject = obj;
                    if (ocObject.krRetainCount <= 0) {
                        [weakSelf.objectRegistry removeObjectForKey:key];
                    }
                }];
                weakSelf.needAutoReleaseNextLoop = NO;
            } sync:NO];
            
        } sync:NO];
    }
}

- (NSArray *)p_argsWithSplits:(NSArray *)splits method:(NSString *)method {
    __weak typeof(self) weakSelf = self;
    NSMutableArray *args = [[NSMutableArray alloc] init];
    for (int i = 1; i < splits.count; i++) {
        NSString *split = splits[i];
        int typeLength = [[split substringToIndex:1] intValue];
        NSString *type = [split substringWithRange:NSMakeRange(1, typeLength)];
        NSString *value = typeLength + 1 < split.length ? [split substringFromIndex:typeLength + 1] : @"";
        id arg = nil;
        if ([type isEqualToString:@"object"]) {
            arg = (KROCObject *)[self p_objectWithID:value];
            if (!arg) {
                [KRConvertUtil hr_alertWithTitle:@"反射参数失败" message:[NSString stringWithFormat:@"object id:%@ 不存在 method: %@", value, method]];
                return nil;
            } else if ([arg isKindOfClass:[KROCObject class]]) {
                arg = ((KROCObject *)arg).ocObject;
            }
        } else if ([type isEqualToString:@"boolean"]) {
            arg = @([value boolValue]);
        } else if ([type isEqualToString:@"int"]) {
            arg = @([value longLongValue]);
        } else if ([type isEqualToString:@"uint"]) {
            arg = @([value longLongValue]);
        }  else if ([type isEqualToString:@"short"]) {
            arg = @([value longLongValue]);
        }  else if ([type isEqualToString:@"float"]) {
            arg = @([value floatValue]);
        }  else if ([type isEqualToString:@"double"]) {
            arg = @([value doubleValue]);
        }  else if ([type isEqualToString:@"jsonObject"]) {// {"key":"value"}
            arg = [value hr_stringToDictionary];
        }  else if ([type isEqualToString:@"jsonArray"]) { // [{"key":"value"}, 1, ""] to NSArray
            arg = [value hr_stringToArray];
        } else if ([type isEqualToString:@"block0"]) { // 0个参数
            arg = ^() {
                [weakSelf p_responseCallbackWithArg1:nil arg2:nil arg3:nil arg4:nil arg5:nil callbackID:value];
            };
        } else if ([type isEqualToString:@"block1"]) { // 1个参数
            arg = ^(id arg1) {
                [weakSelf p_responseCallbackWithArg1:arg1 arg2:nil arg3:nil arg4:nil arg5:nil callbackID:value];
            };
        }  else if ([type isEqualToString:@"block2"]) { // 2个参数
            arg = ^(id arg1, id arg2) {
                [weakSelf p_responseCallbackWithArg1:arg1 arg2:arg2 arg3:nil arg4:nil arg5:nil callbackID:value];
            };
        }  else if ([type isEqualToString:@"block3"]) { // 3个参数
            arg = ^(id arg1, id arg2, id arg3) {
                [weakSelf p_responseCallbackWithArg1:arg1 arg2:arg2 arg3:arg3 arg4:nil arg5:nil callbackID:value];
            };
        }  else if ([type isEqualToString:@"block4"]) { // 4个参数
            arg = ^(id arg1, id arg2, id arg3, id arg4) {
                [weakSelf p_responseCallbackWithArg1:arg1 arg2:arg2 arg3:arg3 arg4:arg4 arg5:nil callbackID:value];
            };
        }  else if ([type isEqualToString:@"block5"]) { // 5个参数
            arg = ^(id arg1, id arg2, id arg3, id arg4, id arg5) {
                [weakSelf p_responseCallbackWithArg1:arg1 arg2:arg2 arg3:arg3 arg4:arg4 arg5:arg5 callbackID:value];
            };
        }
        else {
            arg = value;
        }
        [args addObject:arg];
    }
    return args;
}

- (void)p_responseCallbackWithArg1:(id)arg1 arg2:(id)arg2 arg3:(id)arg3 arg4:(id)arg4 arg5:(id)arg5 callbackID:(NSString *)callbackID {
    // __weak typeof(self) weakSelf = self;
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
        NSMutableDictionary *resData = [[NSMutableDictionary alloc] init];
        if(arg1) {
            [resData setObject:[self p_setWithObject:arg1] forKey:@"arg1"];
        }
        if(arg2) {
            [resData setObject:[self p_setWithObject:arg2] forKey:@"arg2"];
        }
        if(arg3) {
            [resData setObject:[self p_setWithObject:arg3] forKey:@"arg3"];
        }
        if(arg4) {
            [resData setObject:[self p_setWithObject:arg4] forKey:@"arg4"];
        }
        if(arg5) {
            [resData setObject:[self p_setWithObject:arg5] forKey:@"arg5"];
        }
        [((KuiklyRenderView *)self.hr_rootView) fireCallbackWithID:callbackID data:resData];
    }];
}

- (void)dealloc {
    
}

@end



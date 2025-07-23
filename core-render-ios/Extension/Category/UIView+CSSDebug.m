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

#import "UIView+CSSDebug.h"
#import <objc/runtime.h>

/**
 FIXME: [FIXME]
 
 TODO By Chenkaijie
 */

@implementation UIView (CSSDebug)

- (NSString *)css_debugName {
    return objc_getAssociatedObject(self, @selector(css_debugName));
}
// kotiln侧统一驱动该debug能力(重写Pager.debugUIInspector方法返回true or false打开该能力)
- (void)setCss_debugName:(NSString *)css_debugName {
    objc_setAssociatedObject(self, @selector(css_debugName), css_debugName, OBJC_ASSOCIATION_RETAIN);

#if DEBUG
    // 只在DEBUG环境下动态替换子类
    [UIView ktv_replaceSubclass:self debugName:css_debugName];
#endif
}

#if DEBUG

#define KTVObjectHookSubClassPrefix     @"KT"

+ (void)ktv_replaceSubclass:(id)object debugName:(NSString *)debugName {
    if (debugName.length == 0) {
        return;
    }
    Class subClass = [UIView ktv_createSubClass:object debugName:debugName];
    if (!subClass) {
        return;
    }
    NSLog(@"Replace subClass success, class = %@", [object class]);
}

+ (Class)ktv_createSubClass:(id)object debugName:(NSString *)debugName {
    if (!object) {
        return nil;
    }
    
    Class statedClass = [object class];
    Class baseClass = object_getClass(object);
    
    // 非NSObject的子类，不做subclasshook处理
    if (![baseClass isKindOfClass:[NSObject class]]) {
        return statedClass;
    }
    
    // 元类，不做处理
    if (class_isMetaClass(baseClass)) {
        return baseClass;
    }
    
    // 对象存在KVO，忽略这类case
    if (statedClass != baseClass) {
        return baseClass;
    }

    // 由于KTV的重用机制，已替换过类名的情况下要特殊处理，重新替换其实现类
    NSString *name = NSStringFromClass(baseClass);
    if ([name hasPrefix:KTVObjectHookSubClassPrefix]) {
        NSArray *array = [name componentsSeparatedByString:@"_"];
        if (array.count >= 3) {
            name = [array objectAtIndex:1];
        }
    }
    
    name = [NSString stringWithFormat:@"%@_%@_%@", KTVObjectHookSubClassPrefix, name, debugName];
    Class subClass = NSClassFromString(name);
    if (!subClass) {
        subClass = objc_allocateClassPair(baseClass, name.UTF8String, 0);
        // [UIView ktv_registClass:subClass stated:statedClass];
        // [UIView ktv_registClass:object_getClass(subClass) stated:statedClass];
        objc_registerClassPair(subClass);
    }
    object_setClass(object, subClass);
    return subClass;
}

+ (void)ktv_registClass:(Class)klass stated:(Class)statedClass {
    IMP classImp = imp_implementationWithBlock((Class)^(id s){
        return statedClass;
    });
    Method classMethod = class_getInstanceMethod(klass, @selector(class));
    class_replaceMethod(klass, @selector(class), classImp, method_getTypeEncoding(classMethod));
}

#endif

@end

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

#import "TDFBaseModule.h"
#import "TDFNativeMethod.h"
#import <objc/runtime.h>

@interface TDFBaseModule ()

@property (nonatomic, copy) NSArray<TDFNativeMethod *> *methods;
@property (nonatomic, copy) NSDictionary<NSString *, TDFNativeMethod *> *methodsByName;

@end

@implementation TDFBaseModule

- (NSArray<TDFNativeMethod *> *)methods
{
    [self calculateMethods];
    return _methods;
}

- (NSDictionary<NSString *,TDFNativeMethod *> *)methodsByName
{
    [self calculateMethods];
    return _methodsByName;
}

- (void)calculateMethods
{
    if (_methods && _methodsByName) {
        return;
    }

    NSMutableArray *moduleMethods = [NSMutableArray new];
    NSMutableDictionary *moduleMethodsByName = [NSMutableDictionary new];

    unsigned int methodCount;
    Class cls = self.class;
    while (cls && cls != [NSObject class] && cls != [NSProxy class]) {
        Method *methods = class_copyMethodList(object_getClass(cls), &methodCount);
        for (unsigned int i = 0; i < methodCount; i++) {
            Method method = methods[i];
            SEL selector = method_getName(method);
            if ([NSStringFromSelector(selector) hasPrefix:@"__tdf_export__"]) {
                IMP imp = method_getImplementation(method);
                const TDFMethodInfo *exportedMethod = ((const TDFMethodInfo *(*)(id, SEL))imp)(self.class, selector);
                TDFNativeMethod *moduleMethod = [[TDFNativeMethod alloc] initWithMethod:exportedMethod moduleClass:self.class delegate:_delegate];

                NSString *str = [NSString stringWithUTF8String:moduleMethod.wrapMethodName];
                [moduleMethodsByName setValue:moduleMethod forKey:str];
                [moduleMethods addObject:moduleMethod];
            }
        }
        free(methods);
        cls = class_getSuperclass(cls);
    }

    _methods = [moduleMethods copy];
    _methodsByName = [moduleMethodsByName copy];
}

#pragma mark - protocol

+ (dispatch_queue_t _Nullable)methodQueue {
    return nil;
}

+ (NSString * _Nonnull)moduleName {
    return @"";
}

+ (BOOL)isSync_Hippy {
    return NO;
}

- (void)invalidate {
    // override
}

- (void)dealloc {
    if (_delegate.bridgeType == TDF_BRIDGE_TYPE_KUIKLY) {
        [self invalidate];
    }
}

@end


static NSMutableDictionary<NSString*, Class> *TDFNameToModules;
Class TDGGetModuleClass(NSString *name) {
    if (name.length == 0) {
        return nil;
    }
    Class moduleClass = TDFNameToModules[name];;
    if (!moduleClass) {
        moduleClass = NSClassFromString(name);
    }
    return moduleClass;
}

/**
 * Register the given class as a bridge module. All modules must be registered
 * prior to the first bridge initialization.
 */

void TDFRegisterModule(Class moduleClass) {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        TDFNameToModules = [NSMutableDictionary new];
    });
    
#if DEBUG
    // TDFModule must inherit from TDFBaseModule
    assert([moduleClass isSubclassOfClass:[TDFBaseModule class]]);
#endif
    NSString *name = [moduleClass moduleName];
    if (name.length == 0) {
        name = NSStringFromClass(moduleClass);
    }
    
    // Register module
    TDFNameToModules[name] = moduleClass;
}

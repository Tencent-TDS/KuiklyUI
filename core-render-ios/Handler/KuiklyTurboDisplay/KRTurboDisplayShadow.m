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

#import "KRTurboDisplayShadow.h"
#import "KRTurboDisplayProp.h"
#import "KRTurboDisplayNodeMethod.h"

#define TAG @"tag"
#define VIEW_NAME @"viewName"
#define PROPS @"props"
#define CONSTRAINT_SIZE @"constraintSize"
#define CALL_METHODS @"callMethods"

@interface KRTurboDisplayShadow()

@property (nonatomic, strong, readwrite) NSNumber *tag;
@property (nonatomic, copy, readwrite) NSString *viewName;
@property (nonatomic, strong, readwrite) NSMutableArray<KRTurboDisplayProp *> *props;
@property (nonatomic, strong, readwrite) NSValue *constraintSize;
@property (nonatomic, strong, readwrite) NSMutableArray<KRTurboDisplayNodeMethod *> *callMethods;

@end

@implementation KRTurboDisplayShadow

- (instancetype)initWithTag:(NSNumber *)tag viewName:(NSString *)viewName {
    if (self = [super init]) {
        _tag = tag;
        _viewName = viewName;
        _props = [NSMutableArray new];
        _callMethods = [NSMutableArray new];
    }
    return self;
}


- (void)calculateWithConstraintSize:(CGSize)constraintSize {
    _constraintSize = [NSValue valueWithCGSize:constraintSize];
}

- (void)setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KRTurboDisplayProp *prop = [self p_propWithKey:propKey];
    if (!prop) { // add prop
        prop = [[KRTurboDisplayProp alloc] initWithType:KRTurboDisplayPropTypeAttr propKey:propKey propValue:propValue];
        [self.props addObject:prop];
    } else { // only update
        prop.propValue = propValue;
    }
}

- (void)addMethodWithName:(NSString *)name params:(NSString *)params {
    KRTurboDisplayNodeMethod *method = [KRTurboDisplayNodeMethod new];
    method.method = name;
    method.params = params;
    [self.callMethods addObject:method];
}

- (KRTurboDisplayProp *)p_propWithKey:(NSString *)key {
    for (KRTurboDisplayProp *prop in self.props) {
        if ([prop.propKey isEqualToString:key]) {
            return prop;
        }
    }
    return nil;
}


#pragma mark coding

- (instancetype)initWithCoder:(NSCoder *)coder {
    NSNumber *tag = [coder decodeObjectForKey:TAG];
    NSString *viewName = [coder decodeObjectForKey:VIEW_NAME];
    self = [self initWithTag:tag viewName:viewName];
    if (self) {
        _props = [coder decodeObjectForKey:PROPS];
        _constraintSize = [coder decodeObjectForKey:CONSTRAINT_SIZE];
        _callMethods = [coder decodeObjectForKey:CALL_METHODS];
    }
    return self;
}


- (void)encodeWithCoder:(NSCoder *)coder {
    [coder encodeObject:self.tag forKey:TAG];
    [coder encodeObject:self.viewName forKey:VIEW_NAME];
    [coder encodeObject:self.props forKey:PROPS];
    [coder encodeObject:self.constraintSize forKey:CONSTRAINT_SIZE];
    [coder encodeObject:self.callMethods forKey:CALL_METHODS];
    
}

#pragma mark - copy

- (KRTurboDisplayShadow *)deepCopy {
    KRTurboDisplayShadow *shadow = [[KRTurboDisplayShadow alloc] initWithTag:[self.tag copy] viewName:[self.viewName copy]];
    if (_props.count) {
        NSMutableArray *props = [NSMutableArray new];
        for (KRTurboDisplayProp *prop in _props) {
            [props addObject:[prop deepCopy]];
        }
        shadow.props = props;
    }
    if (_callMethods.count) {
        NSMutableArray *callMethods = [NSMutableArray new];
        for (KRTurboDisplayNodeMethod *method in _callMethods) {
            [callMethods addObject:[method deepCopy]];
        }
        shadow.callMethods = callMethods;
    }
    return shadow;
}




@end

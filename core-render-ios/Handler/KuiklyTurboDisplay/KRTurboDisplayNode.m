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

#import "KRTurboDisplayNode.h"
#import "KuiklyRenderViewExportProtocol.h"
#import "KRTurboDisplayShadow.h"
#import "KRTurboDisplayNodeMethod.h"

#define TAG @"tag"
#define VIEW_NAME @"viewName"
#define PARENT_TAG @"parentTag"
#define CHILDREN @"children"
#define PROPS @"props"
#define CALL_METHODS @"callMethods"
#define SCROLL_INDEX @"scrollIndex"

@interface KRTurboDisplayNode()


@property (nonatomic, strong, readwrite) NSMutableArray<KRTurboDisplayProp *> *props;
@property (nonatomic, strong, readwrite) NSMutableArray<KRTurboDisplayNodeMethod *> *callMethods;

@end

@implementation KRTurboDisplayNode

#pragma mark - public

- (instancetype)initWithTag:(NSNumber *)tag viewName:(NSString *)viewName {
    if (self = [super init]) {
        _tag = tag;
        _viewName = viewName;
    }
    return self;
}

- (void)insertSubNode:(KRTurboDisplayNode *)subNode index:(NSInteger)index {
    NSInteger originIndex = index;
    assert([NSThread isMainThread]);
    if (index > _children.count || index == -1) {
        index = _children.count;
    }
    [self.children insertObject:subNode atIndex:index];
    subNode.parentTag = self.tag;
    [subNode setPropWithKey:INSERT_KEY propValue:@(originIndex) propType:(KRTurboDisplayPropTypeInsert)];
}

- (void)removeFromParentNode:(KRTurboDisplayNode *)parentNode {
    [parentNode.children removeObject:self];
    self.parentTag = nil;
}


- (void)addMethodWithName:(NSString *)name params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    assert([NSThread isMainThread]);
    KRTurboDisplayNodeMethod *method = [KRTurboDisplayNodeMethod new];
    method.method = name;
    method.params = params;
    method.callback = callback;
    [self.callMethods addObject:method];
}


- (void)addViewMethodWithMethod:(NSString *)method
                         params:(NSString * _Nullable)params
                       callback:(KuiklyRenderCallback _Nullable)callback {
    [self addNodeMethodWithType:(KRTurboDisplayNodeMethodTypeView) name:nil method:method params:params callback:callback];
    
}
- (void)addModuleMethodWithModuleName:(NSString *)moduelName
                         method:(NSString *)method
                         params:(NSString * _Nullable)params
                       callback:(KuiklyRenderCallback _Nullable)callback {
    [self addNodeMethodWithType:(KRTurboDisplayNodeMethodTypeModule) name:moduelName method:method params:params callback:callback];
}

- (void)addNodeMethodWithType:(KRTurboDisplayNodeMethodType)type
                         name:(NSString *)name
                         method:(NSString *)method
                         params:(NSString * _Nullable)params
                       callback:(KuiklyRenderCallback _Nullable)callback {
    KRTurboDisplayNodeMethod *nodeMethod = [KRTurboDisplayNodeMethod new];
    nodeMethod.type = type;
    nodeMethod.name = name;
    nodeMethod.method = method;
    nodeMethod.params = params;
    nodeMethod.callback = callback;
    [self.callMethods addObject:nodeMethod];
}

- (void)setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    assert([NSThread isMainThread]);
    KRTurboDisplayPropType type = KRTurboDisplayPropTypeAttr;
    if ([propValue isKindOfClass:NSClassFromString(@"NSBlock")]) {
        type = KRTurboDisplayPropTypeEvent;
        if ([propKey isEqualToString:@"setNeedLayout"]) { // 该事件为虚拟事件，仅for驱动loop
            return ;
        }
    }
    [self setPropWithKey:propKey propValue:propValue propType:type];
}

- (void)setFrame:(CGRect)frame {
    assert([NSThread isMainThread]);
    [self setPropWithKey:FRAME_KEY propValue:[NSValue valueWithCGRect:frame] propType:KRTurboDisplayPropTypeFrame];
}

- (void)setShadow:(KRTurboDisplayShadow *)shadow {
    [self setPropWithKey:SHADOW_KEY propValue:shadow propType:(KRTurboDisplayPropTypeShadow)];
}


- (void)setPropWithKey:(NSString *)propKey propValue:(id)propValue propType:(KRTurboDisplayPropType)type {
    KRTurboDisplayProp *prop = [self propWithKey:propKey];
    if (!prop) { // add prop
        prop = [[KRTurboDisplayProp alloc] initWithType:type propKey:propKey propValue:propValue];
        [self.props addObject:prop];
    } else { // only update
        if (type == KRTurboDisplayPropTypeFrame) {
            prop.propValue = propValue;
        }
        prop.propValue = propValue;
       
    }
}

- (KRTurboDisplayProp *)propWithKey:(NSString *)key {
    if (_props == nil) {
        return nil;
    }
    for (KRTurboDisplayProp *prop in self.props) {
        if ([prop.propKey isEqualToString:key]) {
            return prop;
        }
    }
    return nil;
}

// override scrollIndex getter method
- (NSNumber *)scrollIndex {
    for (KRTurboDisplayProp *prop in self.props) {
        if ([prop.propKey isEqualToString:KR_SCROLL_INDEX]) {
            return (NSNumber *)prop.propValue;
        }
    }
    return @(0);
}

- (CGRect)renderFrame {
    for (KRTurboDisplayProp *prop in self.props) { //
        if (prop.propType == KRTurboDisplayPropTypeFrame) {
            return  [((NSValue *)prop.propValue) CGRectValue];
        }
    }
    return CGRectZero;
}

#pragma mark - NSCoding

- (instancetype)initWithCoder:(NSCoder *)coder {
    NSNumber *tag = [coder decodeObjectForKey:TAG];
    NSString *viewName = [coder decodeObjectForKey:VIEW_NAME];
    self = [self initWithTag:tag viewName:viewName];
    if (self) {
        _parentTag = [coder decodeObjectForKey:PARENT_TAG];
        _children = [coder decodeObjectForKey:CHILDREN];
        _props = [coder decodeObjectForKey:PROPS];
        _callMethods = [coder decodeObjectForKey:CALL_METHODS];
    }
    return self;
}



- (void)encodeWithCoder:(NSCoder *)coder {
    [coder encodeObject:_tag forKey:TAG];
    [coder encodeObject:_viewName forKey:VIEW_NAME];
    [coder encodeObject:_parentTag forKey:PARENT_TAG];
    [coder encodeObject:_children forKey:CHILDREN];
    [coder encodeObject:_props forKey:PROPS];
    [coder encodeObject:_callMethods forKey:CALL_METHODS];
}

#pragma mark - copying

- (KRTurboDisplayNode *)deepCopy {
    KRTurboDisplayNode *node = [[KRTurboDisplayNode alloc] initWithTag:[self.tag copy] viewName:[self.viewName copy]];
    node.parentTag = [_parentTag copy];
    if (_children.count) {
        NSMutableArray *children = [NSMutableArray new];
        for (KRTurboDisplayNode *subNode in _children) {
            [children addObject:[subNode deepCopy]];
        }
        node.children = children;
    }
    if (_props.count) {
        NSMutableArray *props = [NSMutableArray new];
        for (KRTurboDisplayProp *prop in _props) {
            [props addObject:[prop deepCopy]];
        }
        node.props = props;
    }
    if (_callMethods.count) {
        NSMutableArray *callMethods = [NSMutableArray new];
        for (KRTurboDisplayNodeMethod *method in _callMethods) {
            [callMethods addObject:[method deepCopy]];
        }
        node.callMethods = callMethods;
    }
    return node;
}


#pragma mark - getter

- (NSMutableArray<KRTurboDisplayNode *> *)children {
    if (!_children) {
        _children = [NSMutableArray new];
    }
    return _children;
}

- (NSMutableArray<KRTurboDisplayProp *> *)props {
    if (!_props) {
        _props = [NSMutableArray new];
    }
    return _props;
}

- (NSMutableArray<KRTurboDisplayNodeMethod *> *)callMethods {
    if (!_callMethods) {
        _callMethods = [NSMutableArray new];
    }
    return _callMethods;
}

- (BOOL)hasChild {
    return _children.count > 0;
}

@end

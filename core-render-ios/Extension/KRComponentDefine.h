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
#ifndef KRComponentDefine_h
#define KRComponentDefine_h

#import "UIView+CSS.h"

// Kuikly属性声明宏
#define KUIKLY_PROP(name) css_##name

#define KUIKLY_SET_PROP(key, method) \
if ([propKey isEqualToString:@#key]) { \
    self.key = [KuiklyConvert method:propValue]; \
    return; \
}

#define KUIKLY_SET_CSS_PROP(key, method) \
if ([propKey isEqualToString:@#key]) { \
    self.key = [UIView method:propValue]; \
}

// 设置通用样式
#define KUIKLY_SET_CSS_COMMON_PROP  \
if ([self css_setPropWithKey:propKey value:propValue]) { \
    return ; \
}

// 重置通用样式
#define KUIKLY_RESET_CSS_COMMON_PROP  [self css_reset];

#define KRC_PARAM_KEY @"param"
#define KRC_CALLBACK_KEY @"callback"
// 设置CSS_Method
#define KUIKLY_CALL_CSS_METHOD  \
SEL selector = NSSelectorFromString( [NSString stringWithFormat:@"css_%@:", method] ); \
if ([self respondsToSelector:selector]) { \
    IMP imp = [self methodForSelector:selector]; \
    void (*func)(id, SEL, id) = (void *)imp; \
    NSMutableDictionary *args = [@{} mutableCopy]; \
    if (params) { args[KRC_PARAM_KEY] = params; } \
    if (callback) { args[KRC_CALLBACK_KEY] = callback; } \
    func(self, selector, args); \
}

#define KR_STRONG_SELF_RETURN_IF_NIL \
if (!weakSelf) { \
    return; \
} \
__strong typeof(&*weakSelf) strongSelf = weakSelf; \

#define KR_WEAK_SELF __weak typeof(&*self) weakSelf = self;


#define KR_STRONG_SELF_RETURN_NIL \
if (!weakSelf) { \
    return nil; \
} \
__strong typeof(&*weakSelf) strongSelf = weakSelf; \

#endif /* KRComponentDefine_h */

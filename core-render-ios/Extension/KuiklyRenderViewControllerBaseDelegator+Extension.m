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

#import "KuiklyRenderViewControllerBaseDelegator+Extension.h"
#import <objc/runtime.h>

@implementation KuiklyRenderViewControllerBaseDelegator (Extension)

- (void)setPageName:(NSString *)pageName {
    objc_setAssociatedObject(self, @selector(pageName), pageName, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSString *)pageName {
    return objc_getAssociatedObject(self, @selector(pageName));
}

- (void)setView:(NSString *)view {
    objc_setAssociatedObject(self, @selector(view), view, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSString *)view {
    return objc_getAssociatedObject(self, @selector(view));
}

@end



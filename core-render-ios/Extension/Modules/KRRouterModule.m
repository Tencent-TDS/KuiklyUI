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

#import "KRRouterModule.h"
#import "NSObject+KR.h"

static id<KRRouterProtocol> gRouterHanlder;

@implementation KRRouterModule

+ (void)registerRouterHandler:(id<KRRouterProtocol>)routerHandler {
    gRouterHanlder = routerHandler;
#if DEBUG
    assert([routerHandler respondsToSelector:@selector(closePage:)]);
#endif
}


// call by kotlin
- (void)openPage:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *pageName = params[@"pageName"];
    NSDictionary *pageData = params[@"pageData"];
#if DEBUG
    assert(gRouterHanlder); // 异常：未注册，需要通过registerRouterHandler方法注册实现
#endif
    [gRouterHanlder openPageWithName:pageName pageData:pageData controller:self.hr_rootView.kr_viewController];
}
// call by kotlin
- (void)closePage:(NSDictionary *)args {
    [gRouterHanlder closePage:self.hr_rootView.kr_viewController];
}

@end

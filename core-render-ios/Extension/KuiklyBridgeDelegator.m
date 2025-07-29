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

#import "KuiklyBridgeDelegator.h"
#import "KuiklyRenderView.h"

@implementation KuiklyBridgeDelegator

@synthesize pageName = _pageName;
@synthesize rootView = _rootView;
@synthesize bridgeType = _bridgeType;
@synthesize hippyBridge = _hippyBridge;

- (nonnull instancetype)initWithRootView:(nonnull KuiklyRenderView *)rootView {
    if (self = [super init]) {
        _rootView = (UIView *)rootView;
        _pageName = rootView.contextParam.pageName;
        _bridgeType = TDF_BRIDGE_TYPE_KUIKLY;
    }
    return self;
}


- (id _Nullable)moduleWithName:(NSString * _Nonnull)moduleName {
    return [(KuiklyRenderView *)_rootView moduleWithName:moduleName];
}

- (void)sendWithEvent:(NSString * _Nonnull)event data:(NSDictionary * _Nullable)data {
    [(KuiklyRenderView *)_rootView sendWithEvent:event data:data];
}

- (UIView * _Nullable)viewWithTag:(NSInteger)tag {
    return [(KuiklyRenderView *)_rootView viewWithTag:tag];
}

- (void)performCallback:(NSNumber *_Nonnull)callbackId params:(id _Nonnull)params {
    if ([callbackId isKindOfClass:[NSNumber class]]) {
        NSDictionary *paramDic = @{
            @"result": params,
        };
        [(KuiklyRenderView *)_rootView fireCallbackWithID:[callbackId stringValue] data:paramDic];
    }
}

@end

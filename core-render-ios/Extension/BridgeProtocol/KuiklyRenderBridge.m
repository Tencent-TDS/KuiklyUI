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

#import "KuiklyRenderBridge.h"

static id<KuiklyRenderComponentExpandProtocol> gComponentExpandHandler;

@implementation KuiklyRenderBridge


+ (void)registerComponentExpandHandler:(id<KuiklyRenderComponentExpandProtocol>)componentExpandHandler {
    gComponentExpandHandler = componentExpandHandler;
}

+ (void)registerLogHandler:(id<KuiklyLogProtocol>)logHandler {
    [KRLogModule registerLogHandler:logHandler];
}

+ (void)registerAPNGViewCreator:(APNGViewCreator)creator {
    [KRAPNGView registerAPNGViewCreator:creator];
}

+ (void)registerPAGViewCreator:(PAGViewCreator)creator {
    [KRPAGView registerPAGViewCreator:creator];
}

+ (void)registerFontHandler:(id<KuiklyFontProtocol>)fontHandler {
    [KRFontModule registerFontHandler:fontHandler];
}

+ (void)registerCacheHandler:(id<KRCacheProtocol>)cacheHandler {
    [KRCacheManager registerCacheHandler:cacheHandler];
}

+ (id<KuiklyRenderComponentExpandProtocol>)componentExpandHandler {
    if (!gComponentExpandHandler) {
        gComponentExpandHandler = [[NSClassFromString(@"KuiklyRenderComponentExpandHandler") alloc] init];
    }
    return gComponentExpandHandler;
}

@end

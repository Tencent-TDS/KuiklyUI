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

#import "KRFontModule.h"
static id<KuiklyFontProtocol> gFontHandler;
@implementation KRFontModule

+ (void)registerFontHandler:(id<KuiklyFontProtocol>)fontHandler {
    gFontHandler = fontHandler;
}

/*
 * 返回经过缩放的字体大小
 */
- (NSString *)scaleFontSize:(NSDictionary *)args {
    NSArray *params = args[KR_PARAM_KEY];
    if ([params isKindOfClass:[NSArray class]] && params.count) { // 参数合法
        CGFloat fontSize = [params[0] intValue];
        if (gFontHandler && [gFontHandler respondsToSelector:@selector(scaleFitWithFontSize:)]) {
            return [NSString stringWithFormat:@"%.2lf", [gFontHandler scaleFitWithFontSize:fontSize] ];
        } else {
            return [NSString stringWithFormat:@"%.2lf",fontSize];
        }
    }
    return nil;
}


@end

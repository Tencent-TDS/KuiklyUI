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

#import "KRBaseModule.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"
#import "KRLogModule.h"
NSString *const KR_PARAM_KEY = @"param";
NSString *const KR_CALLBACK_KEY = @"callback";


@implementation KRBaseModule

@synthesize hr_rootView;
@synthesize hr_contextParam;
#pragma mark - KuiklyRenderModuleExportProtocol

- (id _Nullable)hrv_callWithMethod:(NSString *)method params:(id _Nullable)params callback:(KuiklyRenderCallback)callback {
    SEL selector = NSSelectorFromString( [NSString stringWithFormat:@"%@:", method] );
    if ([self respondsToSelector:selector]) {
        NSMutableDictionary *args = [@{
             KR_PARAM_KEY: params ?: @"",
        } mutableCopy];
        if (callback){
            args[KR_CALLBACK_KEY] = callback;
        }
        id result = [self kr_invokeWithSelector:selector args:args];
        return result;
    } else {
        NSString *reason = [NSString stringWithFormat:@"module方法不存在: %@:(NSDictionary *)args）在Module中未实现，请补充该方法", method];
        [KRLogModule logError:reason];
        NSAssert(false, reason);
        callback( @{
            @"code":@(-1),
            @"message": @"method does not exist",
        } );
    }
    return nil;
}


/*
 * @brief 获取tag对应的View实例（仅支持在主线程调用）.
 * @param tag view对应的索引
 * @return view实例
 */
- (UIView * _Nullable)viewWithTag:(NSNumber *)tag {
    if ( [self.hr_rootView isKindOfClass:[KuiklyRenderView class]]) {
        id<KuiklyRenderViewExportProtocol> viewHandler = [((KuiklyRenderView *)self.hr_rootView) viewWithRefTag:tag];
        if ([viewHandler isKindOfClass:[UIView class]]) {
            return (UIView *)viewHandler;
        }
    }
    return nil;
}

@end

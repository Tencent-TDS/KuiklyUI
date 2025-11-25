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

#import <Foundation/Foundation.h>
#import "KRBackPressModule.h"
#import "KuiklyRenderThreadManager.h"


@implementation KRBackPressModule {
    KuiklyBackPressCompletion _backPressCompletion;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        
    }
    return self;
}


// Koltin侧接收Event后调用Native传递是否已经消费本次返回手势
- (id)hrv_callWithMethod:(NSString *)method params:(id)params callback:(KuiklyRenderCallback)callback {
    if ([method isEqualToString:@"backHandle"]) {
        [self backHandleWithParams:params];
        return nil;
    }
    return [super hrv_callWithMethod:method params:params callback:callback];
}

// 基于Koltin参数执行回调
- (void)backHandleWithParams:(nullable NSString *)params {
    BOOL isConsumed = NO;
    // 解析Kotlin侧调用所传输过来的参数
    if (params) {
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[params dataUsingEncoding:NSUTF8StringEncoding] options:0 error:nil];
        isConsumed =  [dict[@"consumed"] intValue] == 1;
    }

    if (_backPressCompletion) {
        KuiklyBackPressCompletion completion = _backPressCompletion;
        _backPressCompletion = nil;
        
        // 主线程执行Completion
        if ([NSThread isMainThread]) {
            completion(isConsumed);
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                completion(isConsumed);
            });
        }
    }
}

// 设置Completion
- (void)setBackPressCompletion:(KuiklyBackPressCompletion)completion {
    _backPressCompletion = completion;
}

@end

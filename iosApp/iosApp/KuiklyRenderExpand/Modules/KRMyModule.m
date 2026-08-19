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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "KRMyModule.h"

@implementation KRMyModule

// KRBaseModule 通过 `hrv_callWithMethod:params:callback:` 反射调用形如 `- (id)methodName:(NSDictionary *)args` 的方法。
// args[KR_PARAM_KEY] 为 Kotlin 传入的 param，args[KR_CALLBACK_KEY] 为回调（存在时）。
// 返回值 = 同步返回值；如需异步回调，用 args[KR_CALLBACK_KEY] 触发。

// 同步调用(JSON传输)：接收 JSON 字符串，同步返回 data 字段值
- (id)syncJsonCall:(NSDictionary *)args {
    NSString *jsonStr = args[KR_PARAM_KEY] ?: @"";
    NSData *jsonData = [jsonStr dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *json = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
    return json[@"data"] ?: @"";
}

// 同步调用(ByteArray传输)：接收含 NSData 的数组，交换字节后返回
- (id)syncByteArrayCall:(NSDictionary *)args {
    NSArray *params = args[KR_PARAM_KEY];
    if (![params isKindOfClass:[NSArray class]] || params.count == 0) {
        return nil;
    }
    NSData *bytes = params.firstObject;
    if (![bytes isKindOfClass:[NSData class]] || bytes.length < 4) {
        return bytes;
    }
    NSMutableData *result = [bytes mutableCopy];
    uint8_t *p = (uint8_t *)result.mutableBytes;
    uint8_t tmp1 = p[0];
    uint8_t tmp2 = p[1];
    p[0] = p[3];
    p[1] = p[2];
    p[2] = tmp2;
    p[3] = tmp1;
    return result;
}

// 异步回调(JSON传输)：通过 callback 回传 JSON 字符串（Kotlin CallbackFn 会用框架 JSONObject 解析）
- (id)asyncJsonCallback:(NSDictionary *)args {
    NSString *jsonStr = args[KR_PARAM_KEY] ?: @"";
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSData *jsonData = [jsonStr dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *json = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
    NSString *content = json[@"data"] ?: @"";
    if (callback) {
        callback([NSString stringWithFormat:@"{\"content\":\"%@\"}", content]);
    }
    return nil;
}

// 异步回调(数组传输)：通过 callback 回传首元素
- (id)asyncArrayCallback:(NSDictionary *)args {
    NSArray *params = args[KR_PARAM_KEY];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    if (callback) {
        callback([params isKindOfClass:[NSArray class]] ? params.firstObject : @"");
    }
    return nil;
}

// 异步回调(混合数组)：通过 callback 回传 [NSData, NSString, NSString]
- (id)asyncMixedArrayCallback:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    if (callback) {
        NSData *x = [NSData dataWithBytes:(uint8_t[]){33, 34, 35} length:3];
        callback(@[x, @"s1", @"hello"]);
    }
    return nil;
}

// 同步调用(返回 Double 3.14)
- (id)retDouble:(NSDictionary *)args {
    return @(3.14);
}

// 同步调用(返回 Int 314)
- (id)retInt:(NSDictionary *)args {
    return @(314);
}

@end
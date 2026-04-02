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

#import "KRNetworkModule.h"
#import "KRHttpRequestTool.h"

@implementation KRNetworkModule

/**
 * Extracts the raw JSON substring for a given key from a JSON string,
 * preserving the original key ordering.
 *
 * When the Kotlin side serializes the network params via JSONObject.toString(),
 * it produces a JSON string with keys in insertion order. If we parse this
 * string into an NSDictionary and then re-serialize the nested "param" object,
 * the key order may change because NSDictionary is a hash table that does not
 * preserve insertion order. Using the raw substring avoids this problem.
 */
- (NSString *)kr_extractJsonObjectForKey:(NSString *)key fromString:(NSString *)jsonStr {
    if (!jsonStr.length || !key.length) return nil;

    NSString *keyPattern = [NSString stringWithFormat:@"\"%@\":", key];
    NSRange keyRange = [jsonStr rangeOfString:keyPattern];
    if (keyRange.location == NSNotFound) return nil;

    NSInteger pos = NSMaxRange(keyRange);
    NSInteger len = (NSInteger)jsonStr.length;

    // Skip whitespace
    while (pos < len) {
        unichar c = [jsonStr characterAtIndex:pos];
        if (c != ' ' && c != '\t' && c != '\n' && c != '\r') break;
        pos++;
    }
    if (pos >= len) return nil;

    unichar firstChar = [jsonStr characterAtIndex:pos];
    if (firstChar != '{' && firstChar != '[') return nil;

    // Match brackets, handling nested objects/arrays and escape sequences in strings
    NSInteger depth = 0;
    NSInteger startPos = pos;
    BOOL inString = NO;
    BOOL escaped = NO;

    while (pos < len) {
        unichar c = [jsonStr characterAtIndex:pos];
        if (escaped) {
            escaped = NO;
        } else if (c == '\\') {
            escaped = YES;
        } else if (c == '"') {
            inString = !inString;
        } else if (!inString) {
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') {
                depth--;
                if (depth == 0) {
                    return [jsonStr substringWithRange:NSMakeRange(startPos, pos - startPos + 1)];
                }
            }
        }
        pos++;
    }
    return nil;
}

/*
 * 通用Http请求接口， call by kotlin
 */
- (void)httpRequest:(NSDictionary *)args {
    NSString *argsJsonStr = args[KR_PARAM_KEY];
    NSDictionary *param = [argsJsonStr hr_stringToDictionary];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *url = param[@"url"];
    NSString *method = param[@"method"];
    NSDictionary *requestParam = param[@"param"];
    NSDictionary *headers = param[@"headers"];
    NSString *cookie = param[@"cookie"];
    NSInteger timeout = [param[@"timeout"] intValue];

    // Extract the raw "param" JSON substring from the original bridge string to
    // preserve key ordering. The Kotlin side computes the request signature using
    // JSONObject.toString() which maintains insertion order. Parsing argsJsonStr
    // into an NSDictionary loses that order (NSDictionary is a hash map), so
    // re-serializing requestParam via hr_dictionaryToString would produce a body
    // with different key ordering, causing server-side signature verification to fail.
    NSString *rawParamStr = [self kr_extractJsonObjectForKey:@"param" fromString:argsJsonStr];

    [KRHttpRequestTool requestWithMethod:method
                                     url:url
                                   param:requestParam
                           rawBodyString:rawParamStr
                              binaryData:nil
                                 headers:headers
                                 timeout:timeout
                                  cookie:cookie
                           responseBlock:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        int success = data && error == nil ? 1 : 0;
        NSString * errorMsg = (error ? [error localizedDescription] : @"") ?: @"";
        if (callback) {
            NSString *headers = nil;
            NSInteger statusCode = success ? 200 : error.code;
            if ([response isKindOfClass:[NSHTTPURLResponse class]]) {
                statusCode = ((NSHTTPURLResponse *)response).statusCode;
                headers = [((NSHTTPURLResponse *)response).allHeaderFields hr_dictionaryToString];// 获取回包的headers
            }
            NSString * result = nil;
            if (!error && data.length) {
                result = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            }
            callback(@{@"data": result ?: @"",
                       @"success": @(success),
                       @"headers": headers?: @"",
                       @"statusCode": @(statusCode),
                       @"errorMsg": errorMsg});
        }
    }];
}

/*
 * 通用Http请求接口（二进制方式），call by kotlin
 */
- (void)httpRequestBinary:(NSDictionary *)args {
    NSArray *paramArgs = args[KR_PARAM_KEY];
    if (paramArgs.count < 2) {
        return;
    }
    NSDictionary *param = [paramArgs[0] hr_stringToDictionary];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *url = param[@"url"];
    NSString *method = param[@"method"];
    NSDictionary *requestParam = param[@"param"];
    NSDictionary *headers = param[@"headers"];
    NSString *cookie = param[@"cookie"];
    NSInteger timeout = [param[@"timeout"] intValue];
    id binaryData = paramArgs[1]; // 获取二进制数据

    [KRHttpRequestTool requestWithMethod:method
                                     url:url
                                   param:requestParam
                              binaryData:(NSData *)binaryData
                                 headers:headers
                                 timeout:timeout
                                  cookie:cookie
                           responseBlock:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        int success = data && error == nil ? 1 : 0;
        NSString * errorMsg = (error ? [error localizedDescription] : @"") ?: @"";
        if (callback) {
            NSString *headers = nil;
            NSInteger statusCode = success ? 200 : error.code;
            if ([response isKindOfClass:[NSHTTPURLResponse class]]) {
                statusCode = ((NSHTTPURLResponse *)response).statusCode;
                headers = [((NSHTTPURLResponse *)response).allHeaderFields hr_dictionaryToString];
            }
            NSDictionary *resInfo = @{
                    @"success": @(success),
                    @"headers": headers ?: @"",
                    @"statusCode": @(statusCode),
                    @"errorMsg": errorMsg
            };
            callback(@[[resInfo hr_dictionaryToString], data ?: [NSData data]]);
        }
    }];
}

@end

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
 * Extracts the raw JSON object/array substring for a top-level key from a JSON
 * object string, preserving the original key ordering.
 *
 * Only matches the key at the outermost object level (depth 1) to avoid false
 * positives from nested objects that happen to contain the same key name.
 *
 * Background: NSDictionary is a hash map that does not preserve insertion order.
 * Parsing a JSON string into NSDictionary and re-serializing it produces a
 * different key ordering than the original, which breaks request signature
 * verification when the signature is computed from the original ordered JSON.
 */
- (NSString *)kr_extractJsonObjectForKey:(NSString *)key fromString:(NSString *)jsonStr {
    if (![jsonStr isKindOfClass:[NSString class]] || !jsonStr.length || !key.length) return nil;

    NSString *keyToken = [NSString stringWithFormat:@"\"%@\"", key];
    NSInteger keyTokenLen = (NSInteger)keyToken.length;
    NSInteger len = (NSInteger)jsonStr.length;
    NSInteger pos = 0;
    NSInteger depth = 0;
    BOOL inString = NO;
    BOOL escaped = NO;

    while (pos < len) {
        unichar c = [jsonStr characterAtIndex:pos];

        if (escaped) {
            escaped = NO;
            pos++;
            continue;
        }

        if (c == '\\' && inString) {
            escaped = YES;
            pos++;
            continue;
        }

        if (c == '"') {
            if (!inString && depth == 1 && pos + keyTokenLen <= len) {
                NSString *candidate = [jsonStr substringWithRange:NSMakeRange(pos, keyTokenLen)];
                if ([candidate isEqualToString:keyToken]) {
                    NSInteger colonPos = pos + keyTokenLen;
                    while (colonPos < len) {
                        unichar wc = [jsonStr characterAtIndex:colonPos];
                        if (wc != ' ' && wc != '\t' && wc != '\n' && wc != '\r') break;
                        colonPos++;
                    }
                    if (colonPos < len && [jsonStr characterAtIndex:colonPos] == ':') {
                        NSInteger valueStart = colonPos + 1;
                        while (valueStart < len) {
                            unichar wc = [jsonStr characterAtIndex:valueStart];
                            if (wc != ' ' && wc != '\t' && wc != '\n' && wc != '\r') break;
                            valueStart++;
                        }
                        if (valueStart >= len) return nil;

                        unichar firstChar = [jsonStr characterAtIndex:valueStart];
                        if (firstChar != '{' && firstChar != '[') return nil;

                        NSInteger vDepth = 0;
                        BOOL vInString = NO;
                        BOOL vEscaped = NO;
                        NSInteger vPos = valueStart;

                        while (vPos < len) {
                            unichar vc = [jsonStr characterAtIndex:vPos];
                            if (vEscaped) {
                                vEscaped = NO;
                            } else if (vc == '\\' && vInString) {
                                vEscaped = YES;
                            } else if (vc == '"') {
                                vInString = !vInString;
                            } else if (!vInString) {
                                if (vc == '{' || vc == '[') {
                                    vDepth++;
                                } else if (vc == '}' || vc == ']') {
                                    vDepth--;
                                    if (vDepth == 0) {
                                        return [jsonStr substringWithRange:NSMakeRange(valueStart, vPos - valueStart + 1)];
                                    }
                                }
                            }
                            vPos++;
                        }
                        return nil;
                    }
                }
            }
            inString = !inString;
            pos++;
            continue;
        }

        if (!inString) {
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
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
    NSString *argsJsonStr = paramArgs[0];
    NSDictionary *param = [argsJsonStr hr_stringToDictionary];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *url = param[@"url"];
    NSString *method = param[@"method"];
    NSDictionary *requestParam = param[@"param"];
    NSDictionary *headers = param[@"headers"];
    NSString *cookie = param[@"cookie"];
    NSInteger timeout = [param[@"timeout"] intValue];
    id binaryData = paramArgs[1];

    NSString *rawParamStr = [self kr_extractJsonObjectForKey:@"param" fromString:argsJsonStr];

    [KRHttpRequestTool requestWithMethod:method
                                     url:url
                                   param:requestParam
                           rawBodyString:rawParamStr
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

#pragma mark - Debug Self-Tests for kr_extractJsonObjectForKey:fromString:

#if DEBUG

@implementation KRNetworkModule (ExtractJsonTests)

+ (BOOL)kr_shouldRunExtractJsonTests {
    NSString *flag = [[[NSProcessInfo processInfo] environment] objectForKey:@"KR_RUN_EXTRACT_JSON_SELF_TESTS"];
    if (![flag isKindOfClass:[NSString class]]) {
        return NO;
    }
    NSString *normalizedFlag = [flag lowercaseString];
    return [normalizedFlag isEqualToString:@"1"] ||
           [normalizedFlag isEqualToString:@"yes"] ||
           [normalizedFlag isEqualToString:@"true"];
}

+ (void)kr_runExtractJsonTestsIfEnabled {
    if ([self kr_shouldRunExtractJsonTests]) {
        [self kr_runExtractJsonTests];
    }
}
+ (void)kr_runExtractJsonTests {
    KRNetworkModule *m = [[KRNetworkModule alloc] init];
    int passed = 0, failed = 0;

    #define _KR_ASSERT(name, expr) do { \
        if (expr) { passed++; } \
        else { failed++; NSLog(@"[KRNetworkModule] FAIL: %@", name); } \
    } while(0)

    // Basic extraction
    _KR_ASSERT(@"basic",
        [[m kr_extractJsonObjectForKey:@"param" fromString:
          @"{\"url\":\"http://a.com\",\"param\":{\"a\":1,\"b\":2}}"]
         isEqualToString:@"{\"a\":1,\"b\":2}"]);

    // Must match top-level key only, not nested
    _KR_ASSERT(@"depth-1 only",
        [[m kr_extractJsonObjectForKey:@"param" fromString:
          @"{\"headers\":{\"param\":\"val\"},\"param\":{\"x\":1}}"]
         isEqualToString:@"{\"x\":1}"]);

    // Escaped quotes and brackets inside string values
    _KR_ASSERT(@"escaped quotes",
        [[m kr_extractJsonObjectForKey:@"param" fromString:
          @"{\"param\":{\"k\":\"v\\\"}{\"}}"]
         isEqualToString:@"{\"k\":\"v\\\"}{\"}"]);

    // Array value
    _KR_ASSERT(@"array value",
        [[m kr_extractJsonObjectForKey:@"items" fromString:
          @"{\"items\":[1,2,{\"a\":3}]}"]
         isEqualToString:@"[1,2,{\"a\":3}]"]);

    // Key not present
    _KR_ASSERT(@"missing key",
        [m kr_extractJsonObjectForKey:@"missing" fromString:@"{\"a\":1}"] == nil);

    // Value is string, not object/array
    _KR_ASSERT(@"string value",
        [m kr_extractJsonObjectForKey:@"param" fromString:@"{\"param\":\"hello\"}"] == nil);

    // Escaped backslash before closing quote
    _KR_ASSERT(@"escaped backslash",
        [[m kr_extractJsonObjectForKey:@"param" fromString:
          @"{\"k\":\"val\\\\\",\"param\":{\"z\":0}}"]
         isEqualToString:@"{\"z\":0}"]);

    // Edge cases: empty / nil
    _KR_ASSERT(@"empty string", [m kr_extractJsonObjectForKey:@"k" fromString:@""] == nil);
    _KR_ASSERT(@"empty key",    [m kr_extractJsonObjectForKey:@"" fromString:@"{\"a\":1}"] == nil);

    // Deeply nested same key name
    _KR_ASSERT(@"deep nested same key",
        [[m kr_extractJsonObjectForKey:@"param" fromString:
          @"{\"a\":{\"b\":{\"param\":{\"wrong\":true}}},\"param\":{\"right\":true}}"]
         isEqualToString:@"{\"right\":true}"]);

    // Realistic bridge payload with "param" in headers (should not match)
    _KR_ASSERT(@"realistic payload",
        [[m kr_extractJsonObjectForKey:@"param" fromString:
          @"{\"url\":\"https://api.example.com/v1/order\",\"method\":\"POST\","
           "\"param\":{\"order_id\":\"12345\",\"items\":[{\"sku\":\"A\",\"qty\":2},"
           "{\"sku\":\"B\",\"qty\":1}],\"sign\":\"abc123\"},\"headers\":"
           "{\"Content-Type\":\"application/json\",\"param\":\"should-not-match\"},"
           "\"cookie\":\"sid=xyz\",\"timeout\":30}"]
         isEqualToString:
          @"{\"order_id\":\"12345\",\"items\":[{\"sku\":\"A\",\"qty\":2},"
           "{\"sku\":\"B\",\"qty\":1}],\"sign\":\"abc123\"}"]);

    // Key order preserved (the whole point of this fix)
    _KR_ASSERT(@"key order preserved",
        [[m kr_extractJsonObjectForKey:@"param" fromString:@"{\"param\":{\"z\":1,\"a\":2,\"m\":3}}"]
         isEqualToString:@"{\"z\":1,\"a\":2,\"m\":3}"]);

    #undef _KR_ASSERT

    if (failed == 0) {
        NSLog(@"[KRNetworkModule] All %d extract-json tests passed.", passed);
    } else {
        NSLog(@"[KRNetworkModule] extract-json tests: %d passed, %d FAILED", passed, failed);
    }
}

@end

#endif

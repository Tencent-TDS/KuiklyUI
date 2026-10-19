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

#pragma mark - Incremental UTF-8 decoding

/// Validates UTF-8 following the WHATWG decoder: every invalid sequence becomes U+FFFD, matching
/// util.TextDecoder (HarmonyOS) and InputStreamReader (Android). Appends the sanitized bytes to
/// `output` and returns how many input bytes were consumed; when `flush` is NO, a trailing
/// incomplete sequence (at most 3 bytes) is left unconsumed for the next chunk.
static NSUInteger KRAppendSanitizedUTF8(const uint8_t *bytes, NSUInteger length, BOOL flush, NSMutableData *output) {
    static const uint8_t kReplacement[] = {0xEF, 0xBF, 0xBD};
    NSUInteger i = 0;
    while (i < length) {
        NSUInteger start = i;
        uint8_t lead = bytes[i];
        if (lead <= 0x7F) {
            [output appendBytes:&bytes[i] length:1];
            i++;
            continue;
        }
        NSUInteger needed = 0;
        uint8_t lower = 0x80;
        uint8_t upper = 0xBF;
        if (lead >= 0xC2 && lead <= 0xDF) {
            needed = 1;
        } else if (lead >= 0xE0 && lead <= 0xEF) {
            if (lead == 0xE0) lower = 0xA0;
            if (lead == 0xED) upper = 0x9F;
            needed = 2;
        } else if (lead >= 0xF0 && lead <= 0xF4) {
            if (lead == 0xF0) lower = 0x90;
            if (lead == 0xF4) upper = 0x8F;
            needed = 3;
        } else {
            [output appendBytes:kReplacement length:sizeof(kReplacement)];
            i++;
            continue;
        }
        NSUInteger j = i + 1;
        BOOL valid = YES;
        for (NSUInteger seen = 0; seen < needed; seen++, j++) {
            if (j >= length) {
                if (!flush) {
                    return start;
                }
                [output appendBytes:kReplacement length:sizeof(kReplacement)];
                return length;
            }
            uint8_t continuation = bytes[j];
            if (continuation < lower || continuation > upper) {
                valid = NO;
                break;
            }
            lower = 0x80;
            upper = 0xBF;
        }
        if (valid) {
            [output appendBytes:&bytes[start] length:j - start];
        } else {
            // The offending byte is not consumed; it may start the next sequence.
            [output appendBytes:kReplacement length:sizeof(kReplacement)];
        }
        i = j;
    }
    return length;
}

#pragma mark - KRStreamSessionDelegate

@interface KRStreamSessionDelegate : NSObject <NSURLSessionDataDelegate>

@property (nonatomic, copy) KuiklyRenderCallback callback;
@property (nonatomic, copy) NSString *requestId;
@property (nonatomic, assign) BOOL isFirstCallback;
@property (nonatomic, strong) NSHTTPURLResponse *httpResponse;
@property (nonatomic, strong) NSMutableData *pendingUTF8Data;
/// 请求结束（成功/失败/被取消）后由 delegate 主动回调，用于统一清理
/// module 侧持有的 session/task/delegate 三个字典条目，并 invalidate session。
@property (nonatomic, copy) void (^onFinish)(NSString *requestId);

@end

@implementation KRStreamSessionDelegate

- (instancetype)initWithCallback:(KuiklyRenderCallback)callback requestId:(NSString *)requestId {
    self = [super init];
    if (self) {
        _callback = callback;
        _requestId = requestId;
        _isFirstCallback = YES;
        _pendingUTF8Data = [NSMutableData data];
    }
    return self;
}

- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask didReceiveResponse:(NSURLResponse *)response completionHandler:(void (^)(NSURLSessionResponseDisposition))completionHandler {
    if ([response isKindOfClass:[NSHTTPURLResponse class]]) {
        self.httpResponse = (NSHTTPURLResponse *)response;
        NSInteger statusCode = self.httpResponse.statusCode;
        if (statusCode < 200 || statusCode > 299) {
            completionHandler(NSURLSessionResponseCancel);
            if (self.callback) {
                NSString *headers = [self.httpResponse.allHeaderFields hr_dictionaryToString] ?: @"";
                self.callback(@{
                    @"event": @"error",
                    @"data": [NSString stringWithFormat:@"HTTP error %ld", (long)statusCode],
                    @"headers": headers,
                    @"statusCode": @(statusCode)
                });
            }
            return;
        }
    }
    completionHandler(NSURLSessionResponseAllow);
}

- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask didReceiveData:(NSData *)data {
    if (!self.callback) return;
    NSString *text = [self decodeUTF8Data:data flush:NO];
    if (text.length == 0) return;
    [self emitDataText:text];
}

- (void)emitDataText:(NSString *)text {
    NSMutableDictionary *eventData = [@{
        @"event": @"data",
        @"data": text
    } mutableCopy];
    if (self.isFirstCallback && self.httpResponse) {
        self.isFirstCallback = NO;
        NSString *headers = [self.httpResponse.allHeaderFields hr_dictionaryToString] ?: @"";
        eventData[@"headers"] = headers;
        eventData[@"statusCode"] = @(self.httpResponse.statusCode);
    }
    self.callback([eventData copy]);
}

/// Only the incomplete trailing sequence (at most 3 bytes) is kept across callbacks, so invalid
/// bytes can neither block later output nor make the buffer grow for the lifetime of the stream.
- (NSString *)decodeUTF8Data:(NSData *)data flush:(BOOL)flush {
    if (data.length > 0) {
        [self.pendingUTF8Data appendData:data];
    }
    NSUInteger length = self.pendingUTF8Data.length;
    if (length == 0) return @"";
    NSMutableData *sanitized = [NSMutableData dataWithCapacity:length];
    NSUInteger consumed = KRAppendSanitizedUTF8(self.pendingUTF8Data.bytes, length, flush, sanitized);
    [self.pendingUTF8Data replaceBytesInRange:NSMakeRange(0, consumed) withBytes:NULL length:0];
    if (sanitized.length == 0) return @"";
    return [[NSString alloc] initWithData:sanitized encoding:NSUTF8StringEncoding] ?: @"";
}

- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task didCompleteWithError:(NSError *)error {
    if (self.callback) {
        if (error) {
            if (error.code != NSURLErrorCancelled) {
                BOOL isTimeout = error.code == NSURLErrorTimedOut;
                self.callback(@{
                    @"event": @"error",
                    @"data": isTimeout ? @"request timeout" : (error.localizedDescription ?: @"unknown error"),
                    @"statusCode": @(isTimeout ? -1001 : -1000)
                });
            }
        } else {
            NSString *tail = [self decodeUTF8Data:nil flush:YES];
            if (tail.length > 0) {
                [self emitDataText:tail];
            }
            self.callback(@{
                @"event": @"complete",
                @"data": @""
            });
        }
    }
    // 无论成功/失败/取消，都需要通知 module 清理资源，避免 session/delegate 内存滞留
    if (self.onFinish) {
        self.onFinish(self.requestId);
        self.onFinish = nil;
    }
}

@end

#pragma mark - KRNetworkModule

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

- (NSMutableDictionary<NSString *, NSURLSessionDataTask *> *)activeStreamTasks {
    if (!_activeStreamTasks) {
        _activeStreamTasks = [NSMutableDictionary dictionary];
    }
    return _activeStreamTasks;
}

- (NSMutableDictionary<NSString *, KRStreamSessionDelegate *> *)activeStreamDelegates {
    if (!_activeStreamDelegates) {
        _activeStreamDelegates = [NSMutableDictionary dictionary];
    }
    return _activeStreamDelegates;
}

- (NSMutableDictionary<NSString *, NSURLSession *> *)activeStreamSessions {
    if (!_activeStreamSessions) {
        _activeStreamSessions = [NSMutableDictionary dictionary];
    }
    return _activeStreamSessions;
}

/// Percent-encodes everything except ASCII alphanumerics and "*-._", so a literal "+" always becomes
/// "%2B". Spaces become "+" for form bodies and "%20" for URL queries, matching Android.
- (NSString *)kr_percentEncode:(NSString *)string spaceAsPlus:(BOOL)spaceAsPlus {
    static NSCharacterSet *allowed = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        allowed = [NSCharacterSet characterSetWithCharactersInString:
                   @"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789*-._"];
    });
    NSString *encoded = [string stringByAddingPercentEncodingWithAllowedCharacters:allowed] ?: @"";
    return spaceAsPlus ? [encoded stringByReplacingOccurrencesOfString:@"%20" withString:@"+"] : encoded;
}

/// Validates the URL (http/https with a host) and appends GET params to its query. Query values
/// are percent-encoded by hand because NSURLQueryItem leaves a literal "+", which form-style
/// query decoders on the server turn into a space. Returns nil if the URL is invalid.
- (NSURL *)kr_streamRequestURLWithString:(id)urlString method:(NSString *)method params:(id)params {
    if (![urlString isKindOfClass:[NSString class]] || [(NSString *)urlString length] == 0) {
        return nil;
    }
    NSURLComponents *components = [NSURLComponents componentsWithString:urlString];
    NSString *scheme = components.scheme.lowercaseString;
    if (!components || !([scheme isEqualToString:@"http"] || [scheme isEqualToString:@"https"]) ||
        components.host.length == 0) {
        return nil;
    }
    if ([[method uppercaseString] isEqualToString:@"GET"] && [params isKindOfClass:[NSDictionary class]] &&
        [(NSDictionary *)params count] > 0) {
        NSMutableArray<NSString *> *parts = [NSMutableArray array];
        NSString *existingQuery = components.percentEncodedQuery;
        if (existingQuery.length > 0) {
            [parts addObject:existingQuery];
        }
        for (id key in (NSDictionary *)params) {
            NSString *name = [self kr_percentEncode:[key description] spaceAsPlus:NO];
            NSString *value = [self kr_percentEncode:[self kr_formValueString:params[key]] spaceAsPlus:NO];
            [parts addObject:[NSString stringWithFormat:@"%@=%@", name, value]];
        }
        components.percentEncodedQuery = [parts componentsJoinedByString:@"&"];
    }
    return components.URL;
}

- (NSString *)kr_formValueString:(id)value {
    if (!value || value == [NSNull null]) {
        return @"";
    }
    if ([value isKindOfClass:[NSString class]]) {
        return value;
    }
    if ([value isKindOfClass:[NSNumber class]] && CFGetTypeID((__bridge CFTypeRef)value) == CFBooleanGetTypeID()) {
        return [value boolValue] ? @"true" : @"false";
    }
    if ([value isKindOfClass:[NSDictionary class]] || [value isKindOfClass:[NSArray class]]) {
        NSData *json = [NSJSONSerialization dataWithJSONObject:value options:0 error:nil];
        return json ? [[NSString alloc] initWithData:json encoding:NSUTF8StringEncoding] : @"";
    }
    return [value description];
}

- (NSString *)kr_formURLEncodedStringFromParams:(NSDictionary *)params {
    NSMutableArray<NSString *> *parts = [NSMutableArray arrayWithCapacity:params.count];
    for (id key in params) {
        NSString *name = [self kr_percentEncode:[key description] spaceAsPlus:YES];
        NSString *value = [self kr_percentEncode:[self kr_formValueString:params[key]] spaceAsPlus:YES];
        [parts addObject:[NSString stringWithFormat:@"%@=%@", name, value]];
    }
    return [parts componentsJoinedByString:@"&"];
}

/// 统一清理指定 requestId 关联的 session/task/delegate。
/// 主动关闭（closeStreamRequest:）与请求结束回调（didCompleteWithError:）均走此方法，
/// 避免 NSURLSession 因未 invalidate 而长期持有 delegate 造成内存滞留。
- (void)kr_finalizeStreamRequestWithId:(NSString *)requestId {
    if (requestId.length == 0) return;
    NSURLSession *session = self.activeStreamSessions[requestId];
    // invalidateAndCancel 会取消未完成的 task 并释放 session 对 delegate 的强引用
    [session invalidateAndCancel];
    [self.activeStreamSessions removeObjectForKey:requestId];
    [self.activeStreamTasks removeObjectForKey:requestId];
    [self.activeStreamDelegates removeObjectForKey:requestId];
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

/*
 * 流式 HTTP 文本请求接口：按网络分片回调 UTF-8 文本，不解析 SSE 等上层协议
 */
- (void)httpStreamRequest:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *url = param[@"url"];
    NSString *method = param[@"method"];
    NSDictionary *requestParam = param[@"param"];
    NSDictionary *headers = param[@"headers"];
    NSString *cookie = param[@"cookie"];
    NSInteger timeout = [param[@"timeout"] intValue];
    NSString *requestId = param[@"requestId"];

    if (!requestId || requestId.length == 0) {
        if (callback) {
            callback(@{
                @"event": @"error",
                @"data": @"requestId is empty",
                @"statusCode": @(-1000)
            });
        }
        return;
    }

    // 防御性判断：requestId 必须唯一，避免覆盖已有的流式请求导致句柄丢失
    if (self.activeStreamTasks[requestId] != nil) {
        if (callback) {
            callback(@{
                @"event": @"error",
                @"data": @"duplicate requestId",
                @"statusCode": @(-1000)
            });
        }
        return;
    }

    NSURL *requestURL = [self kr_streamRequestURLWithString:url method:method params:requestParam];
    if (!requestURL) {
        if (callback) {
            callback(@{
                @"event": @"error",
                @"data": [NSString stringWithFormat:@"invalid url: %@", url],
                @"statusCode": @(-1000)
            });
        }
        return;
    }

    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:requestURL];
    request.HTTPMethod = method ?: @"GET";
    // NSURLSession treats timeoutInterval as an idle timeout (reset whenever data arrives), which
    // is the cross-platform stream contract; the total duration is bounded only by
    // timeoutIntervalForResource (7 days by default).
    request.timeoutInterval = timeout > 0 ? timeout : 30;

    if (cookie.length > 0) {
        [request setValue:cookie forHTTPHeaderField:@"Cookie"];
    }
    if (headers) {
        for (NSString *key in headers) {
            [request setValue:[headers[key] description] forHTTPHeaderField:key];
        }
    }

    if ([method isEqualToString:@"POST"] && [requestParam isKindOfClass:[NSDictionary class]]) {
        // Same body contract as Android/HarmonyOS: JSON when Content-Type contains "json",
        // otherwise UTF-8 application/x-www-form-urlencoded.
        NSString *contentType = [request valueForHTTPHeaderField:@"Content-Type"];
        // Messaging nil returns a zeroed NSRange (location 0), so the length check is required.
        BOOL isJSON = contentType.length > 0 &&
            [contentType rangeOfString:@"json" options:NSCaseInsensitiveSearch].location != NSNotFound;
        if (isJSON) {
            // Keep the original key order, as httpRequest: does, so request signatures still match.
            NSString *rawParamStr = [self kr_extractJsonObjectForKey:@"param" fromString:args[KR_PARAM_KEY]];
            request.HTTPBody = rawParamStr ? [rawParamStr dataUsingEncoding:NSUTF8StringEncoding]
                                           : [NSJSONSerialization dataWithJSONObject:requestParam options:0 error:nil];
        } else {
            if (contentType.length == 0) {
                [request setValue:@"application/x-www-form-urlencoded; charset=UTF-8" forHTTPHeaderField:@"Content-Type"];
            }
            request.HTTPBody = [[self kr_formURLEncodedStringFromParams:requestParam] dataUsingEncoding:NSUTF8StringEncoding];
        }
    }

    KRStreamSessionDelegate *delegate = [[KRStreamSessionDelegate alloc] initWithCallback:callback requestId:requestId];
    NSURLSessionConfiguration *config = [NSURLSessionConfiguration defaultSessionConfiguration];
    // delegateQueue 使用主队列：
    // 1. 所有 delegate 回调（didReceiveData / didCompleteWithError 等）都在主线程执行，
    //    onFinish 里对 activeStreamXXX 三个字典的读写天然串行，无需额外加锁。
    // 2. Kuikly Module 的 httpStreamRequest: / closeStreamRequest: 本身也在主线程调用，
    //    统一线程后不存在多线程并发读写 NSMutableDictionary 的隐患。
    // 3. 流式响应每个分片数据量通常较小、解码轻量（仅 initWithData:encoding:），对主线程负担可忽略；
    //    业务侧收到 data 事件后通常要更新 UI，本就需要主线程，避免了一次额外的线程切换。
    NSURLSession *session = [NSURLSession sessionWithConfiguration:config delegate:delegate delegateQueue:[NSOperationQueue mainQueue]];
    NSURLSessionDataTask *task = [session dataTaskWithRequest:request];

    __weak typeof(self) weakSelf = self;
    delegate.onFinish = ^(NSString *finishedRequestId) {
        __strong typeof(weakSelf) strongSelf = weakSelf;
        if (!strongSelf) return;
        // 请求正常结束/失败后，统一清理 session/task/delegate，避免内存滞留
        [strongSelf kr_finalizeStreamRequestWithId:finishedRequestId];
    };

    self.activeStreamTasks[requestId] = task;
    self.activeStreamDelegates[requestId] = delegate;
    self.activeStreamSessions[requestId] = session;

    [task resume];
}

/*
 * 关闭流式请求
 */
- (void)closeStreamRequest:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *requestId = param[@"requestId"];
    // 使用统一清理入口：invalidate session + 移除三个字典条目
    // NSURLSession 未 invalidate 时会 strong 持有 delegate，仅 cancel task 不足以释放
    [self kr_finalizeStreamRequestWithId:requestId];
}

- (void)dealloc {
    // 拷贝一份 key 快照，避免在遍历过程中修改字典
    NSArray<NSString *> *requestIds = [self.activeStreamSessions.allKeys copy];
    for (NSString *requestId in requestIds) {
        [self kr_finalizeStreamRequestWithId:requestId];
    }
    // 兜底清理：即使某些请求仅存在于 tasks/delegates 字典中（异常路径），也确保全部释放
    for (NSURLSessionDataTask *task in self.activeStreamTasks.allValues) {
        [task cancel];
    }
    [self.activeStreamTasks removeAllObjects];
    [self.activeStreamDelegates removeAllObjects];
    [self.activeStreamSessions removeAllObjects];
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

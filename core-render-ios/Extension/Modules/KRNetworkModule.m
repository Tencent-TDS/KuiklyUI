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

#pragma mark - KRStreamSessionDelegate

@interface KRStreamSessionDelegate : NSObject <NSURLSessionDataDelegate>

@property (nonatomic, copy) KuiklyRenderCallback callback;
@property (nonatomic, copy) NSString *requestId;
@property (nonatomic, assign) BOOL isFirstCallback;
@property (nonatomic, strong) NSHTTPURLResponse *httpResponse;

@end

@implementation KRStreamSessionDelegate

- (instancetype)initWithCallback:(KuiklyRenderCallback)callback requestId:(NSString *)requestId {
    self = [super init];
    if (self) {
        _callback = callback;
        _requestId = requestId;
        _isFirstCallback = YES;
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
    NSString *text = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] ?: @"";
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

- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task didCompleteWithError:(NSError *)error {
    if (!self.callback) return;
    if (error) {
        if (error.code == NSURLErrorCancelled) {
            return;
        }
        self.callback(@{
            @"event": @"error",
            @"data": error.localizedDescription ?: @"unknown error",
            @"statusCode": @(-1000)
        });
    } else {
        self.callback(@{
            @"event": @"complete",
            @"data": @""
        });
    }
}

@end

#pragma mark - KRNetworkModule

@implementation KRNetworkModule

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

/*
 * 通用Http请求接口， call by kotlin
 */
- (void)httpRequest:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *url = param[@"url"];
    NSString *method = param[@"method"];
    NSDictionary *requestParam = param[@"param"];
    NSDictionary *headers = param[@"headers"];
    NSString *cookie = param[@"cookie"];
    NSInteger timeout = [param[@"timeout"] intValue];
    
    [KRHttpRequestTool requestWithMethod:method
                                     url:url
                                   param:requestParam
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

/*
 * SSE 流式Http请求接口
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

    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:url]];
    request.HTTPMethod = method ?: @"GET";
    request.timeoutInterval = timeout > 0 ? timeout : 30;

    if (cookie.length > 0) {
        [request setValue:cookie forHTTPHeaderField:@"Cookie"];
    }
    if (headers) {
        for (NSString *key in headers) {
            [request setValue:[headers[key] description] forHTTPHeaderField:key];
        }
    }

    if ([method isEqualToString:@"POST"] && requestParam) {
        NSString *contentType = headers[@"Content-Type"] ?: headers[@"content-type"] ?: @"";
        if ([contentType containsString:@"application/json"]) {
            NSData *body = [NSJSONSerialization dataWithJSONObject:requestParam options:0 error:nil];
            request.HTTPBody = body;
        } else {
            NSMutableArray *parts = [NSMutableArray array];
            for (NSString *key in requestParam) {
                [parts addObject:[NSString stringWithFormat:@"%@=%@", key, requestParam[key]]];
            }
            request.HTTPBody = [[parts componentsJoinedByString:@"&"] dataUsingEncoding:NSUTF8StringEncoding];
        }
    }

    KRStreamSessionDelegate *delegate = [[KRStreamSessionDelegate alloc] initWithCallback:callback requestId:requestId];
    NSURLSessionConfiguration *config = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:config delegate:delegate delegateQueue:nil];
    NSURLSessionDataTask *task = [session dataTaskWithRequest:request];

    if (requestId) {
        self.activeStreamTasks[requestId] = task;
        self.activeStreamDelegates[requestId] = delegate;
    }

    [task resume];
}

/*
 * 关闭 SSE 流式请求
 */
- (void)closeStreamRequest:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *requestId = param[@"requestId"];
    if (requestId) {
        NSURLSessionDataTask *task = self.activeStreamTasks[requestId];
        [task cancel];
        [self.activeStreamTasks removeObjectForKey:requestId];
        [self.activeStreamDelegates removeObjectForKey:requestId];
    }
}

- (void)dealloc {
    for (NSURLSessionDataTask *task in self.activeStreamTasks.allValues) {
        [task cancel];
    }
    [self.activeStreamTasks removeAllObjects];
    [self.activeStreamDelegates removeAllObjects];
}

@end

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

#import "KRHttpRequestTool.h"
#import "NSObject+KR.h"
#import "KRLogModule.h"
@implementation KRHttpRequestTool


+ (void)requestWithMethod:(NSString *)method url:(NSString *)url param:(NSDictionary *)param binaryData:(NSData * _Nullable)binaryData headers:(NSDictionary *)headerDics timeout:(float)timeout cookie:(NSString * _Nullable)p_cookie responseBlock:(KRKotlinHttpResponse)response {
    if(!([url isKindOfClass:[NSString class]] && url.length)) return;
    BOOL binaryMode = binaryData ? YES : NO;
    param = [param isKindOfClass:[NSDictionary class]] ? param : @{};
    
    method = [method uppercaseString];
    
    NSMutableDictionary *headers = [[NSMutableDictionary alloc] init];
    if ([headerDics isKindOfClass:[NSDictionary class]] && headerDics.count) {
        [headers addEntriesFromDictionary:headerDics];
    }
    
    NSData *postBody = nil;
    if ([method isEqualToString:@"POST"]) {
        if (binaryMode) {
            postBody = binaryData;
        } else {
            NSString *contentType = headers[@"content-type"] ? : headers[@"Content-Type"];
            if (!contentType) {
                contentType = headers[@"content-Type"];
            }
            
            if ([contentType isKindOfClass:[NSString class]] && [contentType rangeOfString:@"/json"].length) {
                if (param.count) {
                    postBody = [[param hr_dictionaryToString] dataUsingEncoding:NSUTF8StringEncoding];
                }
            } else {
                postBody = [self toPostBodyFromParam:param];
            }
        }
        param = nil;
    } else if([method isEqualToString:@"GET"]) {
        if ([param isKindOfClass:[NSDictionary class]] && param.count) {
            url = [url kr_appendUrlEncodeWithParam:param];
            param = nil;
        }
    }

    NSString *cookie = p_cookie.length ? p_cookie : headers[@"Cookie"];
    if ([cookie isKindOfClass:[NSString class]] && cookie.length) {
        [headers removeObjectForKey:@"Cookie"];
    }
    
    NSMutableURLRequest *request = [self _requestWithMethod:method URLString:url parameters:nil error:nil];
    
    NSDictionary *copyHeaders = [headers copy];
    for (NSString *key in copyHeaders.allKeys) {
        id value = copyHeaders[key];
        if ([value isKindOfClass:[NSNumber class]]) {
            [headers setObject:[((NSNumber *)value) stringValue] forKey:key];
        }
    }
    
    if (headers.count) {
        [request setAllHTTPHeaderFields:headers];
    }
    
    if (postBody) {
        [request setHTTPBody:postBody];
    }
    
    [request setTimeoutInterval:timeout ? : 30];
    
    // set cookie
    NSString *value = [self _getDefaultCookie];
    if (cookie.length) {
        value = [NSString stringWithFormat:@"%@%@", value,cookie];
    }
    [request setValue:value forHTTPHeaderField:@"Cookie"];

    dispatch_async(dispatch_get_global_queue(0, 0), ^{
        [KRHttpRequestUtil kotlinRequestWithURLRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response2, NSError * _Nullable error) {
            if (response) {
                response(data, response2, error);
            }
        }];
    });
}

+(NSData *) toPostBodyFromParam:(NSDictionary *)dictionary {
    if ([dictionary isKindOfClass:[NSDictionary class]]) {
        NSMutableArray *mArray = [[NSMutableArray alloc] initWithCapacity:0];
        for (NSString *key in dictionary) {
            NSString *string = [NSString stringWithFormat:@"%@=%@",key,dictionary[key]];
            [mArray addObject:string];
        }
        
        NSString *newString = [mArray componentsJoinedByString:@"&"];
        return [newString dataUsingEncoding:NSUTF8StringEncoding];
    }
    
    return nil;
}

+ (NSString *)_getDefaultCookie{
    return @"";
}



+ (void)downloadWithUrl:(NSString * )url param:(NSDictionary * _Nullable)params sotrePath:(NSString * )path responseBlock:(KRHttpFileResponse)completion{
    [KRHttpRequestUtil downloadWithUrl:url  responseBlock:^(NSString * _Nullable tempPath, NSError * _Nullable error) {
        NSString * sotrePath = tempPath;
        if (path && tempPath && [[NSFileManager defaultManager] fileExistsAtPath:tempPath]){
            // 创建文件夹
            [[NSFileManager defaultManager] removeItemAtPath:path error:nil];
            [[NSFileManager defaultManager] moveItemAtPath:tempPath toPath:path error:nil];
            
            if ([[NSFileManager defaultManager] fileExistsAtPath:path]) {
                sotrePath = path;
            }else {
#if DEBUG
                assert(0);
#endif
            }
        }
        
        
        if (completion) {
            completion(sotrePath,error);
        }
    }];
}



+ (NSMutableURLRequest *)_requestWithMethod:(NSString *)method
                                 URLString:(NSString *)URLString
                                parameters:(id)parameters
                                     error:(NSError *__autoreleasing *)error
{
    NSParameterAssert(method);
    NSParameterAssert(URLString);
    
    NSURL *url = [NSURL URLWithString:URLString];
    
    NSParameterAssert(url);
    
    NSMutableURLRequest *mutableRequest = [[NSMutableURLRequest alloc] initWithURL:url];
    mutableRequest.HTTPMethod = method;
    if (![mutableRequest valueForHTTPHeaderField:@"Content-Type"]) {
        [mutableRequest setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    }
    return mutableRequest;
}

@end

//类分割线

@implementation KRHttpRequestUtil

+ (void)requestContentLengthWithUrl:(NSString *)url completionHandler:(void (^)(long long contentLength, NSError * _Nullable error))completionHandler{
    NSMutableURLRequest * urlRequest = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:url]];
    NSString *range = @"bytes=0-2";
    urlRequest.cachePolicy = NSURLRequestReloadIgnoringLocalCacheData;
    [urlRequest setValue:range forHTTPHeaderField:@"Range"];
    [KRHttpRequestUtil requestWithURLRequest:urlRequest completionHandler:^(NSDictionary * _Nullable json, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        
        NSDictionary *headers = ((NSHTTPURLResponse*)response).allHeaderFields;
        NSString *contentRange = [headers objectForKey:@"Content-Range"];
        if (contentRange) {
            long long contentLength = 0;
            NSArray<NSString *> *ranges = [contentRange componentsSeparatedByString:@"/"];
            if (ranges.count > 1) {
                NSString *contentLengthString = [ranges.lastObject stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
                contentLength = [contentLengthString longLongValue];
            }
            long long newFileLength  = contentLength ?: response.expectedContentLength;
            if (completionHandler) {
                completionHandler(newFileLength,error);
            }
        }else if(error){
            completionHandler(0,error);
        }
        
    }];
}

+ (NSURLSessionDataTask *)requestWithURLRequest:(NSURLRequest *)request completionHandler:(void (^)(NSDictionary * _Nullable json, NSURLResponse * _Nullable response, NSError * _Nullable error))completionHandler{
    NSURLSession * session = [NSURLSession sharedSession];
    
    
    __block NSURLSessionDataTask * task = nil;
    url_session_manager_create_task_safely(^{
       task = [session dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
            
            dispatch_async(dispatch_get_global_queue(0, 0), ^{
                if([data isKindOfClass:[NSData class]]){
                
                    NSDictionary * json = nil;
                    @try{
                        json = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:nil];
                    }
                    @catch(NSException * e){
                        NSLog(@"KRHttpRequestUtil_error_%@",e);
    
                    }
                    if (!error && data.length && !json) {
                        NSString *dataString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
                        if (dataString) {
                            json = @{ @"data": dataString }; // 可能后台回包是非json，需要透传
                        }
                    }
                    
                    if(completionHandler){
                        completionHandler(json,response,error);
                    }
                }else {
                    if(completionHandler){
                        completionHandler(nil,response,error);
                    }
                }
            });
        }];
    });
    

    [task resume];
    return task;
}

+ (NSURLSessionDataTask *)kotlinRequestWithURLRequest:(NSURLRequest *)request completionHandler:(void (^)(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error))completionHandler{
    NSURLSession * session = [NSURLSession sharedSession];
    __block NSURLSessionDataTask * task = nil;
    url_session_manager_create_task_safely(^{
       task = [session dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable e) {
           __block NSError *error = e;
            dispatch_async(dispatch_get_global_queue(0, 0), ^{
                if([data isKindOfClass:[NSData class]]){

                    // Check if the response has a non-success status code
                    if ([response isKindOfClass:[NSHTTPURLResponse class]]) {
                        NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *)response;
                        NSInteger statusCode = httpResponse.statusCode;
                        if (statusCode < 200 || statusCode >= 300) {
                            [KRLogModule logError:[NSString stringWithFormat:@"Received a non-success status code: %ld", (long)statusCode]];
                            // Create a custom error for the non-success status code
                            error = [NSError errorWithDomain:@"ServeErrorDomain" code:statusCode userInfo:@{NSLocalizedDescriptionKey:[NSString stringWithFormat:@"Server returned a non-success status code: %ld", (long)statusCode]}];
                        }
                    }
                    if(completionHandler){
                        completionHandler(data, response, error);
                    }
                }else {
                    if(completionHandler){
                        completionHandler(nil,response,error);
                    }
                }
            });
        }];
    });
    

    [task resume];
    return task;
}


+ (void)downloadWithUrl:(NSString * )url  responseBlock:(KRHttpFileResponse)response{
    NSURLSession * session = [NSURLSession sharedSession];
    NSMutableURLRequest * urlRequest = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:url]];
    urlRequest.cachePolicy = NSURLRequestReloadIgnoringLocalCacheData;
 
    [urlRequest setValue:@"bytes=0-" forHTTPHeaderField:@"Range"];
    NSURLSessionDownloadTask * task = [session downloadTaskWithRequest:urlRequest completionHandler:^(NSURL * _Nullable location, NSURLResponse * _Nullable response2, NSError * _Nullable error) {
        NSString * sotrePath =  [location path];
        if (response) {
            response(sotrePath,error);
        }
    }];
    
    [task resume];
}

+ (void)uploadWithUrl:(NSString *)url fileAtPath:(NSString *)filePath responseBlock:(KRHttpResponse)responseBlock{
    NSMutableURLRequest * urlRequest = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:url]];
    [self uploadWithUrlRequest:urlRequest fileAtPath:filePath responseBlock:responseBlock];
}



+ (void)uploadWithUrlRequest:(NSURLRequest *)urlRequest fileAtPath:(NSString *)filePath responseBlock:(KRHttpResponse)responseBlock{
    
    NSURLSession * session = [NSURLSession sharedSession];
    
    NSURL * fileUrl = [NSURL fileURLWithPath:filePath];
    NSURLSessionUploadTask * task =  [session uploadTaskWithRequest:urlRequest fromFile:fileUrl completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
                if([data isKindOfClass:[NSData class]]){
                     NSDictionary * json = nil;
                     @try{
                         json = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:nil];
                     }
                     @catch(NSException * e){
                         NSLog(@"KRHttpRequestUtil_error_%@",e);
                     }
                     if(responseBlock){
                          responseBlock(json,error);
                     }
                }
                else {
                    if(responseBlock){
                          responseBlock(nil,error);
                    }
                }
    }];
    [task resume];
}



#define NSFoundationVersionNumber_With_Fixed_5871104061079552_bug 1140.11
static void url_session_manager_create_task_safely(dispatch_block_t block) {
    if (NSFoundationVersionNumber < NSFoundationVersionNumber_With_Fixed_5871104061079552_bug) {
        // Fix of bug
        // Open Radar:http://openradar.appspot.com/radar?id=5871104061079552 (status: Fixed in iOS8)
        // Issue about:https://github.com/AFNetworking/AFNetworking/issues/2093
        dispatch_sync(url_session_manager_creation_queue(), block);
    } else {
        block();
    }
}

static dispatch_queue_t url_session_manager_creation_queue(void) {
    static dispatch_queue_t url_session_manager_creation_queue;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        url_session_manager_creation_queue = dispatch_queue_create("com.tencent.networking.session.manager.creation", DISPATCH_QUEUE_SERIAL);
    });
    
    return url_session_manager_creation_queue;
}
@end


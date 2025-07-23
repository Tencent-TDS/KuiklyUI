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

NS_ASSUME_NONNULL_BEGIN
typedef void (^KRHttpResponse)(NSDictionary * _Nullable result , NSError * _Nullable error);
typedef void (^KRKotlinHttpResponse)(NSData * _Nullable result , NSURLResponse * _Nullable response, NSError * _Nullable error);
typedef void (^KRHttpFileResponse)(NSString * _Nullable path , NSError * _Nullable error);
@interface KRHttpRequestTool : NSObject

+ (void)downloadWithUrl:(NSString * )url param:(NSDictionary * _Nullable)param sotrePath:(NSString * )path responseBlock:(KRHttpFileResponse)response;
+ (void)requestWithMethod:(NSString *)method url:(NSString *)url param:(NSDictionary *)param binaryData:(NSData * _Nullable)binaryData headers:(NSDictionary *)headerDics timeout:(float)timeout cookie:(NSString * _Nullable)cookie responseBlock:(KRKotlinHttpResponse)response;



@end


@interface KRHttpRequestUtil : NSObject


+ (NSURLSessionDataTask *)requestWithURLRequest:(NSURLRequest *)request completionHandler:(void (^)(NSDictionary * _Nullable json, NSURLResponse * _Nullable response, NSError * _Nullable error))completionHandler;
+ (void)downloadWithUrl:(NSString * )url responseBlock:(KRHttpFileResponse)response;
+ (void)requestContentLengthWithUrl:(NSString *)url completionHandler:(void (^)(long long contentLength, NSError * _Nullable error))completionHandler;
+ (NSURLSessionDataTask *)kotlinRequestWithURLRequest:(NSURLRequest *)request completionHandler:(void (^)(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error))completionHandler;
@end

NS_ASSUME_NONNULL_END

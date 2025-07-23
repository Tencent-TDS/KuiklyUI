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

#import "KRCodecModule.h"
#import "NSObject+KR.h"
#import "KRConvertUtil.h"

@implementation KRCodecModule

- (NSString *)urlEncode:(NSDictionary *)args {
    NSString *string = args[KR_PARAM_KEY];
    return [string kr_urlEncode];
}

- (NSString *)urlDecode:(NSDictionary *)args {
    NSString *string = args[KR_PARAM_KEY];
    return [string stringByRemovingPercentEncoding];
}

- (NSString *)base64Encode:(NSDictionary *)args {
    NSString *string = args[KR_PARAM_KEY];
    return [string kr_base64Encode];
}

- (NSString *)base64Decode:(NSDictionary *)args {
    NSString *string = args[KR_PARAM_KEY];
    return [string kr_base64Decode];
}

- (NSString *)md5:(NSDictionary *)args {
    NSString *string = args[KR_PARAM_KEY];
    return [string kr_md5String];
}

- (NSString *)sha256:(NSDictionary *)args {
    NSString *string = args[KR_PARAM_KEY];
    return [string kr_sha256String];
}

@end

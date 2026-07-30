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

#import "KRSharedPreferencesModule.h"
#import "NSObject+KR.h"
@implementation KRSharedPreferencesModule

// call by kotlin
// sync getItem
- (NSString *)getItem:(NSDictionary *)args {
    NSString *cacheKey = args[KR_PARAM_KEY];
    NSString * value = [[NSUserDefaults standardUserDefaults] objectForKey:cacheKey ?: @""];
    return [value isKindOfClass:[NSString class]] ? value : @"";
}
// call by kotlin
// sync setItem
- (NSString *)setItem:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *cachKey = param[@"key"] ?: @"";
    NSString *cachValue = param[@"value"] ?: @"";
    [[NSUserDefaults standardUserDefaults] setObject:cachValue ?: @"" forKey:cachKey];
    [[NSUserDefaults standardUserDefaults] synchronize];
    return @"";
    
}

@end

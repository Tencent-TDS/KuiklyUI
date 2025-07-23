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

#import "TDFConvert.h"

@implementation TDFConvert

#define TDF_JSON_ARRAY_CONVERTER(type) TDF_JSON_ARRAY_CONVERTER_NAMED(type, type)
#define TDF_JSON_ARRAY_CONVERTER_NAMED(type, name) \
  +(NSArray *)name##Array : (id)json               \
  {                                                \
    return json;                                   \
  }

TDF_CONVERTER(id, id, self)

TDF_CONVERTER(BOOL, BOOL, boolValue)
TDF_NUMBER_CONVERTER(double, doubleValue)
TDF_NUMBER_CONVERTER(float, floatValue)
TDF_NUMBER_CONVERTER(int, intValue)

TDF_NUMBER_CONVERTER(int64_t, longLongValue);
TDF_NUMBER_CONVERTER(uint64_t, unsignedLongLongValue);

TDF_NUMBER_CONVERTER(NSInteger, integerValue)
TDF_NUMBER_CONVERTER(NSUInteger, unsignedIntegerValue)

TDF_CUSTOM_CONVERTER(CGFloat, CGFloat, [self double:json])

TDF_JSON_CONVERTER(NSArray)
TDF_JSON_CONVERTER(NSDictionary)
TDF_JSON_CONVERTER(NSString)
TDF_JSON_CONVERTER(NSNumber)

TDF_CUSTOM_CONVERTER(NSData *, NSData, [json dataUsingEncoding:NSUTF8StringEncoding])
TDF_CUSTOM_CONVERTER(NSSet *, NSSet, [NSSet setWithArray:json])
TDF_CUSTOM_CONVERTER(NSTimeInterval, NSTimeInterval, [self double:json] / 1000.0)

TDF_JSON_ARRAY_CONVERTER(NSArray)
TDF_JSON_ARRAY_CONVERTER(NSString)
TDF_JSON_ARRAY_CONVERTER(NSDictionary)
TDF_JSON_ARRAY_CONVERTER(NSNumber)

@end

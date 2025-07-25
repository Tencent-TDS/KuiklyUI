/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
#import "TDFModuleProtocol.h"

#define TDFNilIfNull(value)                           \
  ({                                                  \
    __typeof__(value) t = (value);                    \
    (id) t == (id)kCFNull ? (__typeof(value))nil : t; \
  })

@interface TDFParseUtils : NSObject

TDF_EXTERN BOOL TDFReadChar(const char **input, char c);
TDF_EXTERN BOOL TDFReadString(const char **input, const char *string);
TDF_EXTERN void TDFSkipWhitespace(const char **input);
TDF_EXTERN BOOL TDFParseSelectorIdentifier(const char **input, NSString **string);
TDF_EXTERN BOOL TDFParseArgumentIdentifier(const char **input, NSString **string);
TDF_EXTERN NSString *TDFParseType(const char **input);

@end


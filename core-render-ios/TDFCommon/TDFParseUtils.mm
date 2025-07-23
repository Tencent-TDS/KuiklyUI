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

#import "TDFParseUtils.h"

@implementation TDFParseUtils

BOOL TDFReadChar(const char **input, char c) {
  if (**input == c) {
    (*input)++;
    return YES;
  }
  return NO;
}

BOOL TDFReadString(const char **input, const char *string) {
  int i;
  for (i = 0; string[i] != 0; i++) {
    if (string[i] != (*input)[i]) {
      return NO;
    }
  }
  *input += i;
  return YES;
}

void TDFSkipWhitespace(const char **input) {
  while (isspace(**input)) {
    (*input)++;
  }
}

static BOOL TDFIsIdentifierHead(const char c) {
  return isalpha(c) || c == '_';
}

static BOOL TDFIsIdentifierTail(const char c) {
  return isalnum(c) || c == '_';
}

BOOL TDFParseArgumentIdentifier(const char **input, NSString **string) {
  const char *start = *input;

  do {
    if (!TDFIsIdentifierHead(**input)) {
      return NO;
    }
    (*input)++;

    while (TDFIsIdentifierTail(**input)) {
      (*input)++;
    }

    // allow namespace resolution operator
  } while (TDFReadString(input, "::"));

  if (string) {
    *string = [[NSString alloc] initWithBytes:start length:(NSInteger)(*input - start) encoding:NSASCIIStringEncoding];
  }
  return YES;
}

BOOL TDFParseSelectorIdentifier(const char **input, NSString **string) {
  const char *start = *input;
  if (!TDFIsIdentifierHead(**input)) {
    return NO;
  }
  (*input)++;
  while (TDFIsIdentifierTail(**input)) {
    (*input)++;
  }
  if (string) {
    *string = [[NSString alloc] initWithBytes:start length:(NSInteger)(*input - start) encoding:NSASCIIStringEncoding];
  }
  return YES;
}

static BOOL TDFIsCollectionType(NSString *type) {
  static NSSet *collectionTypes;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    collectionTypes = [[NSSet alloc] initWithObjects:@"NSArray", @"NSSet", @"NSDictionary", nil];
  });
  return [collectionTypes containsObject:type];
}

NSString *TDFParseType(const char **input) {
  NSString *type;
  TDFParseArgumentIdentifier(input, &type);
  TDFSkipWhitespace(input);
  if (TDFReadChar(input, '<')) {
    TDFSkipWhitespace(input);
    NSString *subtype = TDFParseType(input);
    if (TDFIsCollectionType(type)) {
      if ([type isEqualToString:@"NSDictionary"]) {
        // Dictionaries have both a key *and* value type, but the key type has
        // to be a string for JSON, so we only care about the value type
        TDFSkipWhitespace(input);
        TDFReadChar(input, ',');
        TDFSkipWhitespace(input);
        subtype = TDFParseType(input);
      }
      if (![subtype isEqualToString:@"id"]) {
        type = [type stringByReplacingCharactersInRange:(NSRange){0, 2 /* "NS" */} withString:subtype];
      }
    } else {
      // It's a protocol rather than a generic collection - ignore it
    }
    TDFSkipWhitespace(input);
    TDFReadChar(input, '>');
  }
  TDFSkipWhitespace(input);
  if (!TDFReadChar(input, '*')) {
    TDFReadChar(input, '&');
  }
  return type;
}

@end

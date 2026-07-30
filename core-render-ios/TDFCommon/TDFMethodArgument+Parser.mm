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

#import "TDFMethodArgument+Parser.h"
#import "TDFParseUtils.h"

@implementation TDFMethodArgument (Parser)

static BOOL TDFParseSelectorPart(const char **input, NSMutableString *selector) {
  NSString *selectorPart;
  if (TDFParseSelectorIdentifier(input, &selectorPart)) {
    [selector appendString:selectorPart];
  }
  TDFSkipWhitespace(input);
  if (TDFReadChar(input, ':')) {
    [selector appendString:@":"];
    TDFSkipWhitespace(input);
    return YES;
  }
  return NO;
}

static BOOL TDFParseUnused(const char **input) {
  return TDFReadString(input, "__attribute__((unused))") || TDFReadString(input, "__attribute__((__unused__))") ||
      TDFReadString(input, "__unused") || TDFReadString(input, "[[maybe_unused]]");
}

static TDFNullability TDFParseNullability(const char **input) {
  if (TDFReadString(input, "nullable")) {
    return TDFNullable;
  } else if (TDFReadString(input, "nonnull")) {
    return TDFNonnullable;
  }
  return TDFNullabilityUnspecified;
}

static TDFNullability TDFParseNullabilityPostfix(const char **input) {
  if (TDFReadString(input, "_Nullable") || TDFReadString(input, "__nullable")) {
    return TDFNullable;
  } else if (TDFReadString(input, "_Nonnull") || TDFReadString(input, "__nonnull")) {
    return TDFNonnullable;
  }
  return TDFNullabilityUnspecified;
}

NSString *TDFParseMethodSignature(const char *input, NSArray<TDFMethodArgument *> **arguments) {
  TDFSkipWhitespace(&input);

  NSMutableArray *args;
  NSMutableString *selector = [NSMutableString new];
  while (TDFParseSelectorPart(&input, selector)) {
    if (!args) {
      args = [NSMutableArray new];
    }

    // Parse type
    if (TDFReadChar(&input, '(')) {
      TDFSkipWhitespace(&input);

      // 5 cases that both nullable and __unused exist
      // 1: foo:(nullable __unused id)foo 2: foo:(nullable id __unused)foo
      // 3: foo:(__unused id _Nullable)foo 4: foo:(id __unused _Nullable)foo
      // 5: foo:(id _Nullable __unused)foo
      TDFNullability nullability = TDFParseNullability(&input);
      TDFSkipWhitespace(&input);

      BOOL unused = TDFParseUnused(&input);
      TDFSkipWhitespace(&input);

      NSString *type = TDFParseType(&input);
      TDFSkipWhitespace(&input);

      if (nullability == TDFNullabilityUnspecified) {
        nullability = TDFParseNullabilityPostfix(&input);
        TDFSkipWhitespace(&input);
        if (!unused) {
          unused = TDFParseUnused(&input);
          TDFSkipWhitespace(&input);
          if (unused && nullability == TDFNullabilityUnspecified) {
            nullability = TDFParseNullabilityPostfix(&input);
            TDFSkipWhitespace(&input);
          }
        }
      } else if (!unused) {
        unused = TDFParseUnused(&input);
        TDFSkipWhitespace(&input);
      }
      [args addObject:[[TDFMethodArgument alloc] initWithType:type nullability:nullability unused:unused]];
      TDFSkipWhitespace(&input);
      TDFReadChar(&input, ')');
      TDFSkipWhitespace(&input);
    } else {
      // Type defaults to id if unspecified
      [args addObject:[[TDFMethodArgument alloc] initWithType:@"id" nullability:TDFNullable unused:NO]];
    }

    // Argument name
    TDFParseArgumentIdentifier(&input, NULL);
    TDFSkipWhitespace(&input);
  }

  *arguments = [args copy];
  return selector;
}

@end

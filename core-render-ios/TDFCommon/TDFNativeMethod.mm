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

#import "TDFNativeMethod.h"
#import "TDFModuleProtocol.h"
#import "TDFMethodArgument+Parser.h"
#import "TDFConvert.h"
#import <objc/message.h>
#import "TDFParseUtils.h"


typedef BOOL (^TDFArgumentBlock)(NSUInteger, id);   // NSUInteger index, id json

@implementation TDFMethodArgument

- (instancetype)initWithType:(NSString *)type nullability:(TDFNullability)nullability unused:(BOOL)unused
{
  if (self = [super init]) {
    _type = [type copy];
    _nullability = nullability;
    _unused = unused;
  }
  return self;
}
@end

@implementation TDFNativeMethod
{
    const TDFMethodInfo *_methodInfo;
    NSString *_wrapMethodName;
    
    SEL _selector;
    NSInvocation *_invocation;
    NSArray<TDFArgumentBlock> *_argumentBlocks;
    NSMutableArray *_retainedObjects;
}

- (instancetype)initWithMethod:(const TDFMethodInfo *)exportMethod
                   moduleClass:(Class)moduleClass
                      delegate:(id<TDFBridgeDelegate>)delegate
{
    if (self = [super init]) {
        _moduleClass = moduleClass;
        _methodInfo = exportMethod;
        _delegate = delegate;
    }
    return self;
}

- (nonnull id)invokeWithModule:(nonnull id)module arguments:(nonnull NSArray *)arguments
{
    if (_argumentBlocks == nil) {
        [self processMethodSignature];
    }
    NSAssert([module class] == _moduleClass, @"Attempted to invoke method \
              %@ on a module of class %@", [self methodName], [module class]);

    // Set arguments
    NSUInteger index = 0;
    for (id json in arguments) {
        TDFArgumentBlock block = _argumentBlocks[index];
        if (!block(index, TDFNilIfNull(json))) {
            NSAssert(YES, @"could not be processed. Aborting method call. json: %@, method: %@", json, [self methodName]);
            return nil;
        }
        index++;
    }

    [_invocation invokeWithTarget:module];
    [_retainedObjects removeAllObjects];

    void *returnValue;
    [_invocation getReturnValue:&returnValue];
    return (__bridge id)returnValue;
}

static SEL selectorForType(NSString *type)
{
  const char *input = type.UTF8String;
  return NSSelectorFromString([TDFParseType(&input) stringByAppendingString:@":"]);
}

- (void)processMethodSignature {
    __weak typeof(self) wself = self;
    NSArray<TDFMethodArgument *> *arguments;
    _selector = NSSelectorFromString(TDFParseMethodSignature(_methodInfo->objcName, &arguments));
    NSAssert(_selector, @"%s is not a valid selector", _methodInfo->objcName);

    // Create method invocation
    NSMethodSignature *methodSignature = [_moduleClass instanceMethodSignatureForSelector:_selector];
    NSAssert(methodSignature, @"%s is not a recognized Objective-C method.", sel_getName(_selector));
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:methodSignature];
    invocation.selector = _selector;
    _invocation = invocation;
    NSMutableArray *retainedObjects = [NSMutableArray array];
    _retainedObjects = retainedObjects;

    // Process arguments
    NSUInteger numberOfArguments = methodSignature.numberOfArguments;
    NSMutableArray<TDFArgumentBlock> *argumentBlocks = [[NSMutableArray alloc] initWithCapacity:numberOfArguments - 2];

  #define TDF_RETAINED_ARG_BLOCK(_logic)                                                         \
    [argumentBlocks addObject:^(NSUInteger index, id json) { \
      _logic [invocation setArgument:&value atIndex:(index) + 2];                                \
      if (value) {                                                                               \
        [retainedObjects addObject:value];                                                       \
      }                                                                                          \
      return YES;                                                                                \
    }]

  #define __PRIMITIVE_CASE(_type, _nullable)                                                \
    {                                                                                       \
      isNullableType = _nullable;                                                           \
      _type (*convert)(id, SEL, id) = (__typeof__(convert))objc_msgSend;                    \
      [argumentBlocks addObject:^(NSUInteger index, id json) { \
        _type value = convert([TDFConvert class], selector, json);                          \
        [invocation setArgument:&value atIndex:(index) + 2];                                \
        return YES;                                                                         \
      }];                                                                                   \
      break;                                                                                \
    }

  #define PRIMITIVE_CASE(_type) __PRIMITIVE_CASE(_type, NO)
  #define NULLABLE_PRIMITIVE_CASE(_type) __PRIMITIVE_CASE(_type, YES)

  // Explicitly copy the block
  #define __COPY_BLOCK(block...)         \
    id value = [block copy];             \
    if (value) {                         \
      [retainedObjects addObject:value]; \
    }

  #define BLOCK_CASE(_block_args, _block)             \
    TDF_RETAINED_ARG_BLOCK(__COPY_BLOCK(^_block_args{ _block });)

    for (NSUInteger i = 2; i < numberOfArguments; i++) {
      const char *objcType = [methodSignature getArgumentTypeAtIndex:i];
      BOOL isNullableType = NO;
      TDFMethodArgument *argument = arguments[i - 2];
      NSString *typeName = argument.type;
      SEL selector = selectorForType(typeName);
      if ([TDFConvert respondsToSelector:selector]) {
        switch (objcType[0]) {
          // Primitives
          case _C_CHR:
            PRIMITIVE_CASE(char)
          case _C_UCHR:
            PRIMITIVE_CASE(unsigned char)
          case _C_SHT:
            PRIMITIVE_CASE(short)
          case _C_USHT:
            PRIMITIVE_CASE(unsigned short)
          case _C_INT:
            PRIMITIVE_CASE(int)
          case _C_UINT:
            PRIMITIVE_CASE(unsigned int)
          case _C_LNG:
            PRIMITIVE_CASE(long)
          case _C_ULNG:
            PRIMITIVE_CASE(unsigned long)
          case _C_LNG_LNG:
            PRIMITIVE_CASE(long long)
          case _C_ULNG_LNG:
            PRIMITIVE_CASE(unsigned long long)
          case _C_FLT:
            PRIMITIVE_CASE(float)
          case _C_DBL:
            PRIMITIVE_CASE(double)
          case _C_BOOL:
            PRIMITIVE_CASE(BOOL)
          case _C_SEL:
            NULLABLE_PRIMITIVE_CASE(SEL)
          case _C_CHARPTR:
            NULLABLE_PRIMITIVE_CASE(const char *)
          case _C_PTR:
            NULLABLE_PRIMITIVE_CASE(void *)
          case _C_ID: {
            isNullableType = YES;
            id (*convert)(id, SEL, id) = (__typeof__(convert))objc_msgSend;
            TDF_RETAINED_ARG_BLOCK(id value = convert([TDFConvert class], selector, json););
            break;
          }
          case _C_STRUCT_B: {
            NSMethodSignature *typeSignature = [TDFConvert methodSignatureForSelector:selector];
            NSInvocation *typeInvocation = [NSInvocation invocationWithMethodSignature:typeSignature];
            typeInvocation.selector = selector;
            typeInvocation.target = [TDFConvert class];

            [argumentBlocks addObject:^(NSUInteger index, id json) {
              void *returnValue = malloc(typeSignature.methodReturnLength);
              if (!returnValue) {
                NSLog(@"Error: Memory allocation failed in argument block for selector %@", NSStringFromSelector(selector));
                return NO;
              }
              [typeInvocation setArgument:&json atIndex:2];
              [typeInvocation invoke];
              [typeInvocation getReturnValue:returnValue];
              [invocation setArgument:returnValue atIndex:index + 2];
              free(returnValue);
              return YES;
            }];
            break;
          }
          default: {
            static const char *blockType = @encode(__typeof__(^{
            }));
            if (!strcmp(objcType, blockType)) {
            } else {
                NSAssert(YES, @"Unsupported argument type '%@' in method %@.", typeName, [self methodName]);
            }
          }
        }
      } else if ([typeName isEqualToString:@"TDFModuleSuccessCallback"]) {
          BLOCK_CASE((id args), { [wself.delegate performCallback:json params:args]; });
      } else if ([typeName isEqualToString:@"TDFModuleErrorCallback"]) {
          BLOCK_CASE((NSString *code, NSString *message, NSError *error), {
              NSDictionary *args = [wself paramsFromErrorCode:code message:message error:error];
              [wself.delegate performCallback:json params:args];
          });
      } else {
        // Unknown argument type
          NSAssert(YES, @"Unknown argument type '%@' in method %@. Extend TDFConvert to support this type.",
            typeName, [self methodName]);
      }
    }
    _argumentBlocks = argumentBlocks;

}

- (NSDictionary<NSString *, id> *)paramsFromErrorCode:(NSString *)code
                                            message:(NSString *)message
                                              error:(NSError *__nullable) error {
    NSString *const errorDomain = @"KuiklyError";
    NSString *errorMessage;
    NSArray<NSString *> *stackTrace = [NSThread callStackSymbols];
    NSMutableDictionary<NSString *, id> *errorInfo = [NSMutableDictionary dictionaryWithObject:stackTrace forKey:@"nativeStackIOS"];

    if (error) {
        errorMessage = error.localizedDescription ?: @"Unknown error from a native module";
        errorInfo[@"domain"] = error.domain ?: errorDomain;
    } else {
        errorMessage = @"Unknown error from a native module";
        errorInfo[@"domain"] = errorDomain;
    }
    errorMessage = message ?: errorMessage;
    errorInfo[@"code"] = code ?: @"EUNSPECIFIED";
    errorInfo[@"message"] = errorMessage ?: @"";
    errorInfo[@"userInfo"] = error.userInfo ? : @{};

    return errorInfo;
}

- (SEL)selector
{
  if (_selector == NULL) {
    [self processMethodSignature];
  }
  return _selector;
}

- (const char *)wrapMethodName
{
  NSString *methodName = _wrapMethodName;
  if (!methodName) {
    const char *outName = _methodInfo->outName;
    if (outName && strlen(outName) > 0) {
      methodName = @(outName);
    } else {
      methodName = @(_methodInfo->objcName);
      NSRange colonRange = [methodName rangeOfString:@":"];
      if (colonRange.location != NSNotFound) {
        methodName = [methodName substringToIndex:colonRange.location];
      }
      methodName = [methodName stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
      NSAssert(methodName.length,@"%s Illegal method name, module: %@", _methodInfo->objcName, NSStringFromClass(_moduleClass));
    }
      _wrapMethodName = methodName;
  }
  return methodName.UTF8String;
}

- (NSString *)methodName
{
  if (!_selector) {
    [self processMethodSignature];
  }
  return [NSString stringWithFormat:@"-[%@ %s]", _moduleClass, sel_getName(_selector)];
}

@end

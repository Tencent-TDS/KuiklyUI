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

#import "NSObject+KR.h"
#import <CommonCrypto/CommonDigest.h>
#import "KRConvertUtil.h"
#import "KRLogModule.h"
#import <UIKit/UIKit.h>
#import <objc/runtime.h>
#import <Accelerate/Accelerate.h>
#import <CoreImage/CoreImage.h>

@implementation NSObject (KR)

- (NSDictionary *)hr_stringToDictionary {
    if ([self  isKindOfClass:[NSString class]]) {
        NSString * string = (NSString *)self;
        NSData *data = [string dataUsingEncoding:NSUTF8StringEncoding];
        NSError *error = nil;
        
        NSDictionary* res = nil;
        @try{
            res = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
        }
        @catch(NSException * e){
            NSString *errorMessage = [NSString stringWithFormat:@"%s exception:%@", __FUNCTION__, e];
            [KRLogModule logError:errorMessage];
            NSAssert(false, errorMessage);
        }
        if ([res isKindOfClass:[NSDictionary class]]) {
            return res;
        }
    }
    return nil;
}

- (NSArray *)hr_stringToArray {
    if ([self  isKindOfClass:[NSString class]]) {
        NSString * string = (NSString *)self;
        NSData *data = [string dataUsingEncoding:NSUTF8StringEncoding];
        NSError *error = nil;
        NSArray* res = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
        if ([res isKindOfClass:[NSArray class]]) {
            return res;
        }
    }
    return nil;
}

- (NSArray *)kr_stringToArray {
    return [self hr_stringToArray];
}

- (NSDictionary *)kr_stringToDictionary {
    return [self hr_stringToDictionary];
}


- (NSString *)kr_urlEncode {
    NSString * string = nil;
    if ([self isKindOfClass:[NSString class]]) {
        string = (NSString *)self;
    }else if([self isKindOfClass:[NSNumber class]]){
        string =  [((NSNumber *)self) stringValue];
    }
    if (string) {
        return (NSString*)CFBridgingRelease(CFURLCreateStringByAddingPercentEscapes(nil,(CFStringRef)string,
                                                                                    nil,(CFStringRef)@"!*'();:@&=+$,/?%#[]", kCFStringEncodingUTF8));
    }
    return @"";
}

- (id)kr_invokeWithSelector:(SEL)selector args:(id)args {
    NSMethodSignature *methodSignature = [self methodSignatureForSelector:selector];
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:methodSignature];
    if (args) {
        [invocation setArgument:&args atIndex:2];
    }
    [invocation setSelector:selector];
    [invocation invokeWithTarget:self];
    if (strcmp(methodSignature.methodReturnType, @encode(void)) != 0) {
        void *returnValue;
        [invocation getReturnValue:&returnValue];
        return (__bridge  id)returnValue;
    }
    return nil;
}

+ (id)kr_performWithTarget:(id)target selector:(SEL)aSelector  withObjects:(NSArray *)objects {
    return [self kr_performWithTarget:target selector:aSelector signature:[target methodSignatureForSelector:aSelector] withObjects:objects];
}

+ (id)kr_performWithClass:(Class)classTarget selector:(SEL)aSelector  withObjects:(NSArray *)objects {
    if ([NSStringFromSelector(aSelector) isEqualToString:@"new"]) {
        return [classTarget new];
    } else if ([NSStringFromSelector(aSelector) isEqualToString:@"alloc"]) {
        return [classTarget alloc];
    }
    return [self kr_performWithTarget:classTarget selector:aSelector
                            signature:[classTarget methodSignatureForSelector:aSelector] withObjects:objects];
}

+ (id)kr_performWithTarget:(id)target selector:(SEL)aSelector  signature:(NSMethodSignature *)signature withObjects:(NSArray *)objects {
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:signature];
    for (NSInteger i = 0; i < objects.count; i ++) {
       [invocation kr_setArgumentObject:objects[i] atIndex:i+2];
    }
    invocation.selector = aSelector;
    invocation.target = target;
    [invocation retainArguments];
    [invocation invoke];
    if (!signature.methodReturnLength) {
      return nil;
    } else {
      void *valueLoc = alloca(signature.methodReturnLength);
      [invocation getReturnValue:valueLoc];
      return [NSObject kr_objectWithBuffer:valueLoc type:signature.methodReturnType];
    }
}


+ (id)kr_objectWithBuffer:(void *)valueLoc type:(const char *)argType {
  #define RETURN_WRAPPERED_OBJECT(type) \
    do { \
      type val = 0; \
      val = *((type *) valueLoc); \
      return @(val); \
  } while(0);
  
  if (strcmp(argType, @encode(id)) == 0 || strcmp(argType, @encode(Class)) == 0 || strcmp(argType, @encode(void(^)(void))) == 0) {
    return *((__autoreleasing id *)valueLoc);
  } else if (strcmp(argType, @encode(char)) == 0) {
    RETURN_WRAPPERED_OBJECT(char);
  } else if (strcmp(argType, @encode(int)) == 0) {
    RETURN_WRAPPERED_OBJECT(int);
  } else if (strcmp(argType, @encode(short)) == 0) {
    RETURN_WRAPPERED_OBJECT(short);
  } else if (strcmp(argType, @encode(long)) == 0) {
    RETURN_WRAPPERED_OBJECT(long);
  } else if (strcmp(argType, @encode(long long)) == 0) {
    RETURN_WRAPPERED_OBJECT(long long);
  } else if (strcmp(argType, @encode(unsigned char)) == 0) {
    RETURN_WRAPPERED_OBJECT(unsigned char);
  } else if (strcmp(argType, @encode(unsigned int)) == 0) {
    RETURN_WRAPPERED_OBJECT(unsigned int);
  } else if (strcmp(argType, @encode(unsigned short)) == 0) {
    RETURN_WRAPPERED_OBJECT(unsigned short);
  } else if (strcmp(argType, @encode(unsigned long)) == 0) {
    RETURN_WRAPPERED_OBJECT(unsigned long);
  } else if (strcmp(argType, @encode(unsigned long long)) == 0) {
    RETURN_WRAPPERED_OBJECT(unsigned long long);
  } else if (strcmp(argType, @encode(float)) == 0) {
    RETURN_WRAPPERED_OBJECT(float);
  } else if (strcmp(argType, @encode(double)) == 0) {
    RETURN_WRAPPERED_OBJECT(double);
  } else if (strcmp(argType, @encode(BOOL)) == 0) {
    RETURN_WRAPPERED_OBJECT(BOOL);
  } else if (strcmp(argType, @encode(char *)) == 0) {
    RETURN_WRAPPERED_OBJECT(const char *);
  } else {
    return [NSValue valueWithBytes:valueLoc objCType:argType];
  }
}

+ (BOOL)kr_swizzleInstanceMethod:(SEL)origSel withMethod:(SEL)altSel {
    Method originMethod = class_getInstanceMethod(self, origSel);
    Method newMethod = class_getInstanceMethod(self, altSel);

    if (originMethod && newMethod) {
        if (class_addMethod(self, origSel, method_getImplementation(newMethod), method_getTypeEncoding(newMethod))) {
            class_replaceMethod(self, altSel, method_getImplementation(originMethod), method_getTypeEncoding(originMethod));
        } else {
            method_exchangeImplementations(originMethod, newMethod);
        }
        return YES;
    }
    return NO;
}



@end

/** NSDicationry (KR) */

@implementation NSDictionary(KR)

- (NSString *)hr_dictionaryToString {
    return [KRConvertUtil hr_dictionaryToJSON:self];
}

- (NSString *)kr_dictionaryToString {
   return  [self hr_dictionaryToString];
}

- (NSString * )kr_urlEncodeString {
    NSMutableArray * pairs = [[NSMutableArray alloc] initWithCapacity:self.count];
    if ([self isKindOfClass:[NSDictionary class]]) {
        [self enumerateKeysAndObjectsUsingBlock:^(id  _Nonnull key, id  _Nonnull obj, BOOL * _Nonnull stop) {
            NSString * encodeKey = [((NSObject *)key) kr_urlEncode];
            NSString * encodeValue = [((NSObject *)obj) kr_urlEncode];
            if (encodeKey && encodeValue) {
                NSString * pair = [NSString stringWithFormat:@"%@=%@",encodeKey,encodeValue];
                if (pair) {
                    [pairs addObject:pair];
                }
            }
        }];
        if (pairs.count) {
            return [pairs componentsJoinedByString:@"&"];
        }
    }
    return @"";
}

@end

/** NSString (KR) */

@implementation NSString (KR)

- (NSString *)kr_appendUrlEncodeWithParam:(NSDictionary *)param {
    NSString * preStr = @"?";
    if ([self rangeOfString:@"?"].length) {
        if (![self hasSuffix:@"&"]) {
            preStr = @"&";
        }
    }
    return [self stringByAppendingString:[NSString stringWithFormat:@"%@%@",preStr,[param kr_urlEncodeString]]];
}

- (NSString *)kr_md5String {
    const char *cstr = [self UTF8String];
    unsigned char result[16];
    CC_MD5(cstr, (CC_LONG)strlen(cstr), result);
    
    return [NSString stringWithFormat:@"%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X",
            result[0], result[1], result[2], result[3],
            result[4], result[5], result[6], result[7],
            result[8], result[9], result[10], result[11],
            result[12], result[13], result[14], result[15]
            ];
}


- (NSString *)kr_base64Encode {
    NSData *data = [self dataUsingEncoding: NSUTF8StringEncoding];
    return [data base64EncodedStringWithOptions:0];
}

- (NSString *)kr_base64Decode {
    NSData *data = [[NSData alloc] initWithBase64EncodedString:self options:0];
    return [[NSString alloc] initWithData:data encoding: NSUTF8StringEncoding];
}

- (NSString *)kr_sha256String {
    NSData *data = [self dataUsingEncoding:NSUTF8StringEncoding];
    unsigned char hash[CC_SHA256_DIGEST_LENGTH];
    CC_SHA256([data bytes], (CC_LONG)[data length], hash);
    NSMutableString *hashString = [NSMutableString stringWithCapacity:CC_SHA256_DIGEST_LENGTH * 2];
    for (int i = 0; i < CC_SHA256_DIGEST_LENGTH; i++) {
        [hashString appendFormat:@"%02x", hash[i]];
    }
    return hashString;
}

- (NSString *)kr_subStringWithIndex:(NSUInteger)index {
    NSString *result = self;
    if (result.length > index) {
        NSRange range = [result rangeOfComposedCharacterSequenceAtIndex:index];
        if (range.location>= 0) {
            result = [result substringToIndex:range.location];
        }
    }
    return result;
}

- (NSUInteger )kr_length {
    NSUInteger count = 0;
    NSUInteger length = self.length;
    for (NSUInteger i = 0; i < length; ) {
        NSRange range = [self rangeOfComposedCharacterSequenceAtIndex:i];
        count++;
        i += range.length;
    }
    return count;
}

@end

/** ***** UIView (KR) ***** **/

@implementation UIView (KR)

+ (UIImage *)kr_safeAsImageWithLayer:(CALayer *)layer bounds:(CGRect)bounds {
    @autoreleasepool {
        if (@available(iOS 10.0, *)) {
            UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithBounds:bounds];
            return [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull rendererContext) {
                          [layer renderInContext:rendererContext.CGContext];
                    }];
        } else {
            UIGraphicsBeginImageContext(bounds.size);
            [layer renderInContext:UIGraphicsGetCurrentContext()];
            UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();
            return image;
        }
    }
    
}

- (UIViewController *)kr_viewController {
    for (UIView* next = self; next; next = next.superview) {
        UIResponder *nextResponder = [next nextResponder];
        if ([nextResponder isKindOfClass:[UIViewController class]]) {
            return (UIViewController*)nextResponder;
        }
    }
    return nil;
}

@end



@implementation NSInvocation (KR)


- (void)kr_setArgumentObject:(id)arguementObject atIndex:(NSInteger)idx {
    

    
  #define KR_SET_ARG(type, selector) \
    do { \
        type val = [arguementObject selector]; \
        [self setArgument:&val atIndex:idx]; \
  } while (0)
  if ([arguementObject isKindOfClass:NSNull.class]) {
    arguementObject = nil;
  }
  const char *argType = [self.methodSignature getArgumentTypeAtIndex:idx];
  if (strcmp(argType, @encode(id)) == 0 || strcmp(argType, @encode(Class)) == 0) {
    [self setArgument:&arguementObject atIndex:idx];
  } else if (strcmp(argType, @encode(char)) == 0) {
    KR_SET_ARG(char, charValue);
  } else if (strcmp(argType, @encode(int)) == 0) {
    KR_SET_ARG(int, intValue);
  } else if (strcmp(argType, @encode(short)) == 0) {
    KR_SET_ARG(short, shortValue);
  } else if (strcmp(argType, @encode(long)) == 0) {
    KR_SET_ARG(long, longValue);
  } else if (strcmp(argType, @encode(long long)) == 0) {
    KR_SET_ARG(long long, longLongValue);
  } else if (strcmp(argType, @encode(unsigned char)) == 0) {
    KR_SET_ARG(unsigned char, unsignedCharValue);
  } else if (strcmp(argType, @encode(unsigned int)) == 0) {
    KR_SET_ARG(unsigned int, unsignedIntValue);
  } else if (strcmp(argType, @encode(unsigned short)) == 0) {
    KR_SET_ARG(unsigned short, unsignedShortValue);
  } else if (strcmp(argType, @encode(unsigned long)) == 0) {
    KR_SET_ARG(unsigned long, unsignedLongValue);
  } else if (strcmp(argType, @encode(unsigned long long)) == 0) {
    KR_SET_ARG(unsigned long long, unsignedLongLongValue);
  } else if (strcmp(argType, @encode(float)) == 0) {
    KR_SET_ARG(float, floatValue);
  } else if (strcmp(argType, @encode(double)) == 0) {
    KR_SET_ARG(double, doubleValue);
  } else if (strcmp(argType, @encode(BOOL)) == 0) {
    KR_SET_ARG(BOOL, boolValue);
  } else if (strcmp(argType, @encode(char *)) == 0) {
    const char *cString = [arguementObject UTF8String];
    [self setArgument:&cString atIndex:idx];
    [self retainArguments];
  } else if (strcmp(argType, @encode(void (^)(void))) == 0) {
    [self setArgument:&arguementObject atIndex:idx];
  } else {
    NSCParameterAssert([arguementObject isKindOfClass:NSValue.class]);
    NSUInteger valueSize = 0;
    NSGetSizeAndAlignment([arguementObject objCType], &valueSize, NULL);
    unsigned char valueBytes[valueSize];
    [arguementObject getValue:valueBytes];
    [self setArgument:valueBytes atIndex:idx];
  }
}

@end


@implementation UIImage (KR)

/**
  使用vImage进行高斯模糊
 @param radius 模糊的范围 取值0~12.5
 @return 返回已经模糊过的图片
 */
- (UIImage *)kr_blurBlurRadius:(CGFloat)radius {
    UIImage *image = self;
    CGFloat maxWidth = 150; // 分辨率保留1x
    if (image.size.width == 0) {
        return nil;
    }
    image = [self kr_resizeImageWithImage:image resolution:CGSizeMake(maxWidth, maxWidth * (image.size.height / image.size.width))];
    
    CGImageRef cgImage = image.CGImage;
    size_t width = CGImageGetWidth(cgImage);
    size_t height = CGImageGetHeight(cgImage);
    size_t bytesPerRow = CGImageGetBytesPerRow(cgImage);
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    UInt8 *srcData = (UInt8 *)calloc(height * bytesPerRow, sizeof(UInt8));
    CGContextRef srcContext = CGBitmapContextCreate(srcData, width, height, 8, bytesPerRow, colorSpace,
                                                    kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGContextDrawImage(srcContext, CGRectMake(0, 0, width, height), cgImage);
    UInt8 *destData = (UInt8 *)calloc(height * bytesPerRow, sizeof(UInt8));
    CGContextRef destContext = CGBitmapContextCreate(destData, width, height, 8, bytesPerRow, colorSpace,
                                                     kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    vImage_Buffer src = {
        .data = srcData,
        .height = height,
        .width = width,
        .rowBytes = bytesPerRow
    };
    vImage_Buffer dest = {
        .data = destData,
        .height = height,
        .width = width,
        .rowBytes = bytesPerRow
    };
    int reblurCount = radius == 12.5f ? 6 : 4;
    radius = (radius / 12.5f) * 43.0 + 2.0;
    uint32_t kernelSize = floor(radius * 3 * sqrt(2 * M_PI) / 4 + 0.5);
    if (kernelSize % 2 == 0) {
        kernelSize += 1;
    }
    for (NSUInteger i = 0; i < reblurCount; i++) {
        vImage_Error error = vImageBoxConvolve_ARGB8888(&src, &dest, NULL, 0, 0, kernelSize, kernelSize, NULL, kvImageEdgeExtend);
        if (error) {
            [KRLogModule logError:[NSString stringWithFormat:@"vImageBoxConvolve_ARGB8888 error: %ld", error]];
        }
        vImage_Buffer temp = src;
        src = dest;
        dest = temp;
    }
   
    CGImageRef blurredCGImage = CGBitmapContextCreateImage(destContext);
    UIImage *blurredImage = [UIImage imageWithCGImage:blurredCGImage];
    CGImageRelease(blurredCGImage);
    CGContextRelease(srcContext);
    CGContextRelease(destContext);
    CGColorSpaceRelease(colorSpace);
    free(srcData);
    free(destData);
    return blurredImage;
}


- (UIImage *)kr_resizeImageWithImage:(UIImage *)image resolution:(CGSize)targetResolution {
    CGFloat horizontalRatio = targetResolution.width / image.size.width;
    CGFloat verticalRatio = targetResolution.height / image.size.height;
    CGFloat ratio = MAX(horizontalRatio, verticalRatio);
    
    CGSize newSize = CGSizeMake(image.size.width * ratio, image.size.height * ratio);
    
    UIGraphicsBeginImageContextWithOptions(newSize, YES, 0.0);
    [image drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    UIImage *resizedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return resizedImage;
}


- (UIImage *)kr_tintedImageWithColor:(UIColor *)color {
    if (@available(iOS 13.0, *)) {
        return [self imageWithTintColor:color];
    }
   
    return [self imageWithRenderingMode:(UIImageRenderingModeAlwaysTemplate)];
}

- (UIImage *)kr_applyColorFilterWithColorMatrix:(NSString *)colorFilterMatrix {
    NSArray<NSString *> *colorMatrix = [colorFilterMatrix componentsSeparatedByString:@"|"];
    if ([colorMatrix count] < 20) {
        return self;
    }
    UIImage *image = self;
    CIImage *ciImage = [[CIImage alloc] initWithImage:image];
    CIFilter *filter = [CIFilter filterWithName:@"CIColorMatrix"];
    
    CIVector *vectorR = [CIVector vectorWithX:[colorMatrix[0] floatValue]
                                            Y:[colorMatrix[1] floatValue]
                                            Z:[colorMatrix[2] floatValue]
                                            W:[colorMatrix[3] floatValue]];
    CIVector *vectorG = [CIVector vectorWithX:[colorMatrix[5] floatValue]
                                            Y:[colorMatrix[6] floatValue]
                                            Z:[colorMatrix[7] floatValue]
                                            W:[colorMatrix[8] floatValue]];
    CIVector *vectorB = [CIVector vectorWithX:[colorMatrix[10] floatValue]
                                            Y:[colorMatrix[11] floatValue]
                                            Z:[colorMatrix[12] floatValue]
                                            W:[colorMatrix[13] floatValue]];
    CIVector *vectorA = [CIVector vectorWithX:[colorMatrix[15] floatValue]
                                            Y:[colorMatrix[16] floatValue]
                                            Z:[colorMatrix[17] floatValue]
                                            W:[colorMatrix[18] floatValue]];
    CIVector *vectorBias = [CIVector vectorWithX:[colorMatrix[4] floatValue]
                                               Y:[colorMatrix[9] floatValue]
                                               Z:[colorMatrix[14] floatValue]
                                               W:[colorMatrix[19] floatValue]];
    
    [filter setValue:ciImage forKey:kCIInputImageKey];
    [filter setValue:vectorR forKey:@"inputRVector"];
    [filter setValue:vectorG forKey:@"inputGVector"];
    [filter setValue:vectorB forKey:@"inputBVector"];
    [filter setValue:vectorA forKey:@"inputAVector"];
    [filter setValue:vectorBias forKey:@"inputBiasVector"];
    
    CIContext *context = [CIContext contextWithOptions:nil];
    CIImage *outputCIImage = filter.outputImage;
    CGImageRef filteredImageRef = [context createCGImage:outputCIImage fromRect:outputCIImage.extent];
    UIImage *filteredImage = [UIImage imageWithCGImage:filteredImageRef scale:image.scale orientation:image.imageOrientation];
    CGImageRelease(filteredImageRef);
    
    return filteredImage;
}


@end



@implementation NSMutableAttributedString (KR)

- (void)kr_addAttribute:(NSAttributedStringKey)name value:(id)value range:(NSRange)range {
    if (!value || [value isKindOfClass:[NSNull class]]) {
        return ;
    }
    [self addAttribute:name value:value range:range];
}

@end


@implementation UIApplication (KR)
+ (BOOL)isAppExtension{
    NSDictionary *extensionInfo = [NSBundle.mainBundle objectForInfoDictionaryKey:@"NSExtension"];
    NSString *extensionPointIdentifier = extensionInfo[@"NSExtensionPointIdentifier"];
    return extensionPointIdentifier != nil;
}
@end

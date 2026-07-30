#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wbuiltin-macro-redefined"
#define __FILE__ "NSObject+RIJCategory.m"
#pragma clang diagnostic pop


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

#import "NSObject+RIJCategory.h"
#import "objc/runtime.h"
#import "UIKit/UIKit.h"
#import "sys/utsname.h"
@implementation UIView(RIJCategory)

- (CGFloat)width {
    return self.rij_width;
}

- (void)setWidth:(CGFloat)width {
    [self setRij_width:width];
}

- (CGFloat)height {
    return self.rij_height;
}

- (void)setHeight:(CGFloat)height {
    [self setRij_height:height];
}

- (CGPoint)leftTop {
    return [self rij_leftTop];
}

- (void)setLeftTop:(CGPoint)leftTop {
    [self setRij_leftTop:leftTop];
}

- (CGPoint)rightBottom {
    return [self rij_rightBottom];
}

- (void)setRightBottom:(CGPoint)rightBottom {
    [self setRij_rightBottom:rightBottom];
}

- (void)setLeft:(CGFloat)left {
    [self setRij_left:left];
}

- (CGFloat)left {
    return [self rij_left];
}

- (void)setRight:(CGFloat)right {
    [self setRij_right:right];
}

- (CGFloat)right {
    return [self rij_right];
}

- (void)setX:(CGFloat)x {
    [self setRij_x:x];
}

- (CGFloat)x {
    return [self rij_x];
}

- (void)setY:(CGFloat)y {
    [self setRij_y:y];
}

- (CGFloat)y {
    return [self rij_y];
}

- (void)setCenterX:(CGFloat)centerX {
    return [self setRij_centerX:centerX];
}

- (CGFloat)centerX {
    return [self rij_centerX];
}

- (void)setCenterY:(CGFloat)centerY {
    [self setRij_centerY:centerY];
}

- (CGFloat)centerY {
    return [self rij_centerY];
}

- (void)setSize:(CGSize)size {
     [self setRij_size:size];
}

- (CGSize)size {
    return [self rij_size];
}

-(CGFloat) top {
    return [self rij_top];
}

- (void)setTop:(CGFloat)top {
    [self setRij_top:top];
}

-(CGFloat) bottom {
    return [self rij_bottom];
}
- (void)setBottom:(CGFloat)bottom {
    [self setRij_bottom:bottom];
}

- (CGFloat)rij_width
{
    return self.frame.size.width;
}

-(void)setRij_width:(CGFloat)width
{
    CGRect frame = self.frame;
    self.frame = CGRectMake(frame.origin.x, frame.origin.y, width, frame.size.height);
}

- (CGFloat)rij_height
{
    return self.frame.size.height;
}

- (void)setRij_height:(CGFloat)height
{
    CGRect frame = self.frame;
    self.frame = CGRectMake(frame.origin.x, frame.origin.y, frame.size.width, height);
}

- (CGPoint)rij_leftTop
{
    CGPoint result = CGPointMake(self.frame.origin.x, self.frame.origin.y);
    return result;
}

- (void)setRij_leftTop:(CGPoint)leftTop
{
    CGRect frame = self.frame;
    self.frame = CGRectMake(leftTop.x, leftTop.y, frame.size.width, frame.size.height);
}

- (CGPoint)rij_rightBottom
{
    CGPoint result = CGPointMake(self.frame.origin.x + self.frame.size.width, self.frame.origin.y + self.frame.size.height);
    return result;
}



- (void)setRij_rightBottom:(CGPoint)rightBottom
{
    CGRect frame = self.frame;
    self.frame = CGRectMake(rightBottom.x - frame.size.width, rightBottom.y - frame.size.height, frame.size.width, frame.size.height);
}

-(CGFloat) rij_top{
    return self.frame.origin.y;
}

- (void)setRij_top:(CGFloat)top {
    CGRect frame = self.frame;
    frame.origin.y = top;
    self.frame = frame;
}

-(CGFloat) rij_bottom{
    return self.frame.origin.y + self.frame.size.height;
}

- (void)setRij_left:(CGFloat)left
{
    CGRect frame = self.frame;
    self.frame = CGRectMake(left, frame.origin.y, frame.size.width, frame.size.height);
}

- (CGFloat)rij_left;
{
    return self.frame.origin.x;
}

- (void)setRij_right:(CGFloat)right
{
    CGRect frame = self.frame;
    self.frame = CGRectMake(right - frame.size.width, frame.origin.y, frame.size.width, frame.size.height);
}

- (void)setRij_bottom:(CGFloat)bottom
{
    CGRect frame = self.frame;
    self.frame = CGRectMake(frame.origin.x, bottom - frame.size.height, frame.size.width, frame.size.height);
}


- (CGFloat)rij_right
{
    return self.frame.origin.x + self.frame.size.width;
}


- (void)setRij_x:(CGFloat)x
{
    CGRect frame = self.frame;
    frame.origin.x = x;
    self.frame = frame;
}

- (CGFloat)rij_x
{
    return self.frame.origin.x;
}

- (void)setRij_y:(CGFloat)y
{
    CGRect frame = self.frame;
    frame.origin.y = y;
    self.frame = frame;
}

- (CGFloat)rij_y
{
    return self.frame.origin.y;
}

- (void)setRij_centerX:(CGFloat)centerX
{
    CGPoint center = self.center;
    center.x = centerX;
    self.center = center;
}

- (CGFloat)rij_centerX
{
    return self.center.x;
}

- (void)setRij_centerY:(CGFloat)centerY
{
    CGPoint center = self.center;
    center.y = centerY;
    self.center = center;
}

- (CGFloat)rij_centerY
{
    return self.center.y;
}

- (void)setRij_size:(CGSize)size{
    CGRect frame = self.frame;
    frame.size = size;
    self.frame = frame;
}






- (CGSize)rij_size
{
    return self.frame.size;
}



-(UITapGestureRecognizer *) rij_addTapGestureRecognizerWithTarget:(id)target action:(SEL) action{
    UITapGestureRecognizer * tap = [[UITapGestureRecognizer alloc] initWithTarget:target action:action];
    self.userInteractionEnabled = true;
    [self addGestureRecognizer:tap];
    return tap;
}


-(UIImage*)rij_convertToImage{
    CGSize s = self.bounds.size;
    // 下面方法，第一个参数表示区域大小。第二个参数表示是否是非透明的。如果需要显示半透明效果，需要传NO，否则传YES。第三个参数就是屏幕密度了
    UIGraphicsBeginImageContextWithOptions(s, NO, [UIScreen mainScreen].scale);
    [self.layer renderInContext:UIGraphicsGetCurrentContext()];
    UIImage*image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
}

// 判断View是否显示在屏幕上
- (BOOL)rij_isDisplayedInScreen{
    if(CGSizeEqualToSize(self.bounds.size, CGSizeZero)){
        return NO;
    }
    UIView * superView = self.superview;
    UIWindow * rootView = nil;//根视图相当于屏幕
    while (superView) {
        if([superView isKindOfClass:[UIWindow class]]){
            rootView = (UIWindow *)superView;
            break;
        }
        if (superView.hidden && superView != rootView) {
            return NO;
        }
        superView = superView.superview;
    }
    if(rootView){
        CGRect location = [self convertRect:self.bounds toView:rootView]; //在屏幕中的位置 //如果
        CGRect windowBound = rootView.bounds;
        if(CGRectIntersectsRect(location, windowBound) || CGRectContainsRect(location, windowBound)){
            return true;
        }
    }
    return NO;
}

- (UIViewController *)rij_viewController{
    for (UIView* next = self; next; next = next.superview) {
        UIResponder *nextResponder = [next nextResponder];
        if ([nextResponder isKindOfClass:[UIViewController class]]) {
            return (UIViewController*)nextResponder;
        }
    }
    return nil;
}

- (UIColor *)rij_colorOfPoint:(CGPoint)point {
    unsigned char pixel[4] = {0};
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(pixel, 1, 1, 8, 4, colorSpace, (CGBitmapInfo)kCGImageAlphaPremultipliedLast);
    
    CGContextTranslateCTM(context, -point.x, -point.y);
    
    [self.layer renderInContext:context];
    
    CGContextRelease(context);
    CGColorSpaceRelease(colorSpace);
    
    UIColor *color = [UIColor colorWithRed:pixel[0]/255.0 green:pixel[1]/255.0 blue:pixel[2]/255.0 alpha:pixel[3]/255.0];
    
    return color;
}




@end




//--------------RIJDeallocObject类分割线-----------


@implementation RIJDeallocObject


- (void)addBlockWithBlock:(IDBlock)block{
    if (block) {
        RIJWeakBlock * weakBlock = [RIJWeakBlock new];
        weakBlock.block = block;
        [self.blocks addObject:weakBlock];
    }
}

- (void)dealloc{
    if (_blocks) {
        for (RIJWeakBlock * block in self.blocks) {
            if (block.block) {
                block.block(self.obj);
            }
        }
    }
    
}

- (NSMutableArray *)blocks{
    if (!_blocks) {
        _blocks = [NSMutableArray new];
    }
    return _blocks;
}

@end


//--------------RIJWeakBlock类分割线-----------

@implementation RIJWeakBlock


- (void)setTarget:(id)target{
    _target = target;
    if ([target isKindOfClass:[NSObject class]]) {
        self.targetKey = [(NSObject *)target rij_address];
    }
}

- (void)dealloc{
    self.block = nil;
    self.targetKey = nil;
}

@end

@implementation NSObject (RIJCategory)

+ (BOOL)rij_isIphoneX{
    CGFloat height = 0;
    if (@available(iOS 11.0, *)) {
        height += [UIApplication sharedApplication].delegate.window.safeAreaInsets.bottom;
    }
    return (height > 0);
}

- (NSString *)rij_urlEncode{
    NSString * string = nil;
    if ([self isKindOfClass:[NSString class]]) {
        string = (NSString *)self;
    }else if([self isKindOfClass:[NSNumber class]]){
        string =  [((NSNumber *)self) stringValue];
    }
    if (string) {
        return (NSString*)CFBridgingRelease(CFURLCreateStringByAddingPercentEscapes(nil,
                                                                                    (CFStringRef)string, nil,
                                                                                    (CFStringRef)@"!*'();:@&=+$,/?%#[]", kCFStringEncodingUTF8));
    }
    return @"";
}


- (id)rij_extraData{
    return  objc_getAssociatedObject(self, @selector(rij_extraData));
}


- (void)setRij_extraData:(id)rij_extraData{
    objc_setAssociatedObject(self, @selector(rij_extraData), rij_extraData, OBJC_ASSOCIATION_RETAIN);
}

- (NSString *)rij_address{
    return [NSString stringWithFormat:@"%ld",(long)self];
}

- (RIJDeallocObject *)rij_deallocObject{
    RIJDeallocObject * object = objc_getAssociatedObject(self, @selector(rij_deallocObject));
    if (!object) {
        object = [RIJDeallocObject new];
        object.obj = [self rij_address];
        objc_setAssociatedObject(self, @selector(rij_deallocObject), object, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    return object;
}

-(void)rij_performBlockOnDeallocWithBlock:(IDBlock) block{
    [[self rij_deallocObject] addBlockWithBlock:block];
}


- (NSDictionary *)rij_stringToDictionary{
    if ([self  isKindOfClass:[NSString class]]) {
        NSString * string = (NSString *)self;
        NSData *data = [string dataUsingEncoding:NSUTF8StringEncoding];
        NSError *error = nil;
        NSDictionary* res = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
        if ([res isKindOfClass:[NSDictionary class]]) {
            return res;
        }
    }
    return nil;
}



- (NSArray *) rij_arrayValueForKey:(NSString *)key{
    if(![key isKindOfClass:[NSString class]]) return nil;
    if ([self isKindOfClass:[NSDictionary class]]) {
        NSObject * object =  [((NSDictionary *)self) objectForKey:key];
        if ([object isKindOfClass:[NSArray class]]) {
            return (NSArray *)object;
        }else if(object){
            return @[object];
        }
    }
    return nil;
}

- (NSString *) rij_stringValueForKey:(NSString *)key{
    if(![key isKindOfClass:[NSString class]]) return nil;
    if ([self isKindOfClass:[NSDictionary class]]) {
        NSObject * object =  [((NSDictionary *)self) objectForKey:key];
        if ([object isKindOfClass:[NSString class]]) {
            return (NSString *)object;
        }else if([object isKindOfClass:[NSNumber class]]){
            return [((NSNumber *)object) stringValue];
        }else if([object isKindOfClass:[NSDictionary class]]){
            return [((NSDictionary *)object) rij_stringValueForKey:@"rij_pb_origin_str"];
        }
    }
    return nil;
}

- (long long) rij_longlongValueForKey:(NSString *)key{
    if(![key isKindOfClass:[NSString class]]) return 0;
    if ([self isKindOfClass:[NSDictionary class]]) {
        NSObject * object =  [((NSDictionary *)self) objectForKey:key];
        if ([object isKindOfClass:[NSString class]]) {
            return [((NSString *)object) longLongValue];
        }else if([object isKindOfClass:[NSNumber class]]){
            NSNumber * number = (NSNumber *)object;
            return [number longLongValue];
        }
    }
    return 0;
}

- (unsigned long long) rij_unsignedLonglongValueForKey:(NSString *)key{
    return (unsigned long long)[self rij_longlongValueForKey:key];
}
- (int) rij_intValueForKey:(NSString *)key{
    return (int)[self rij_longlongValueForKey:key];
}

- (NSObject *) rij_objectForKey:(NSString *)key{
    if ([self isKindOfClass:[NSDictionary class]]) {
        NSObject * object = [((NSDictionary *)self) objectForKey:key];
        if ([object isKindOfClass:[NSNull class]]) {
            return nil;
        }
        return object;
    }
    return nil;
}

- (NSArray *) rij_arrayValueForMultiKeys:(NSArray *)keys{
    if ([keys isKindOfClass:[NSArray class]] && keys.count) {
        NSObject * value = [self __getLastValueWithData:self keys:keys];
        return [value rij_arrayValueForKey:(NSString *)keys.lastObject];
    }
    return nil;
}
- (NSString *) rij_stringValueForMultiKeys:(NSArray *)keys{
    if ([keys isKindOfClass:[NSArray class]] && keys.count) {
        NSObject * value = [self __getLastValueWithData:self keys:keys];
        return [value rij_stringValueForKey:(NSString *)keys.lastObject];
    }
    return nil;
}
- (long long) rij_longlongValueForMultiKeys:(NSArray *)keys{
    if ([keys isKindOfClass:[NSArray class]] && keys.count) {
        NSObject * value = [self __getLastValueWithData:self keys:keys];
        return [value rij_longlongValueForKey:(NSString *)keys.lastObject];
    }
    return 0;
}

- (id) __getValueWithData:(id)data key:(NSString *)key{
    if ([data isKindOfClass:[NSDictionary class]]) {
        return [((NSDictionary *)data) valueForKey:key];
    }
    return nil;
}

- (id) __getLastValueWithData:(id)data keys:(NSArray *)keys{
    id value = data;
    for (int i = 0; i < keys.count - 1; i++) {
        id key = keys[i];
        if ([key isKindOfClass:[NSNumber class]]) {
            if ( value && ![value isKindOfClass:[NSArray class]]) {
                value = @[value];
            }
            value = [((NSArray *)value) rij_objectAtIndex:[((NSNumber *)key) unsignedIntegerValue]];
        }else if([key isKindOfClass:[NSString class]]){
            value = [self __getValueWithData:value key:(NSString *)key];
        }
    }
    return value;
}


-(id) rij_valueForMultiKey:(id) key , ...{
    id value = self;
    va_list arguments;
    id eachObject;
    if (key) {
        va_start(arguments, key);
        if ([key isKindOfClass:[NSString class]]) {
            value = [self _rij_getValueWithData:value key:key];
        }else if([key isKindOfClass:[NSNumber class]]){
            value = [self _rij_getValueWithData:value index:[key unsignedIntegerValue]];
        }
        if (!value) {
            return value;
        }
        while ((eachObject = va_arg(arguments, id))) {
            if ([eachObject isKindOfClass:[NSString class]]) {
                value = [self _rij_getValueWithData:value key:eachObject];
            }else if([eachObject isKindOfClass:[NSNumber class]]){
                value = [self _rij_getValueWithData:value index:[eachObject unsignedIntegerValue]];
            }
            if (!value) {
                return value;
            }
        }
        va_end(arguments);
    }
    return value;
}

//- (void)rij_setObject:(id)anObject forKey:(NSString *)key{
//
//    if ([key isKindOfClass:[NSString class]] && anObject) {
//
//    }
//}

-(id) _rij_getValueWithData:(id)data key:(NSString *)key{
    if ([data isKindOfClass:[NSDictionary class]]) {
        return [data objectForKey:key];
    }
    return nil;
}

-(id) _rij_getValueWithData:(id)data index:(NSUInteger)index{
    if ([data isKindOfClass:[NSArray class]]) {
        NSArray * aData = (NSArray *)data;
        if (aData.count > index) {
            return [aData objectAtIndex:index];
        }
        
    }
    return nil;
}


+ (NSString *)rij_deviceModel{
    struct utsname systemInfo;
    uname(&systemInfo);
    return [NSString stringWithCString:systemInfo.machine encoding:NSUTF8StringEncoding];
}


@end


#import <CommonCrypto/CommonDigest.h>
//UIImage
@implementation NSString (RIJCategory)

- (NSString *)rij_appendUrlEncodeWithParam:(NSDictionary *)param{
    
    NSString * preStr = @"?";
    if ([self rangeOfString:@"?"].length) {
        if (![self hasSuffix:@"&"]) {
            preStr = @"&";
        }
    }
    return [self stringByAppendingString:[NSString stringWithFormat:@"%@%@",preStr,[param rij_urlEncodeString]]];
}
- (NSString *)rij_md5String {
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


//- (id)objectForKeyedSubscript:(id)key{
//    [self _safe_error_objectForKeyedSubscript:key];
//    return nil;
//}
//
//- (id)_safe_error_objectForKeyedSubscript:(id)key{
//#if !GRAY_OR_APPSTORE
//    assert(0);//非法访问 断言
//#endif
//    return nil;
//}
- (NSMutableDictionary *)rij_getURLParameters {
    return [self viola_getURLParameters];
}

/**
 *  截取URL中的参数
 *
 *  @return NSMutableDictionary parameters
 */
- (NSMutableDictionary *)viola_getURLParameters{
    
    // 查找参数
    NSRange range = [self rangeOfString:@"?"];
    if (range.location == NSNotFound) {
        return nil;
    }
    
    NSMutableDictionary *params = [NSMutableDictionary dictionary];
    
    // 截取参数
    NSString *parametersString = [self substringFromIndex:range.location + 1];
    
    // 判断参数是单个参数还是多个参数
    if ([parametersString containsString:@"&"]) {
        
        // 多个参数，分割参数
        NSArray *urlComponents = [parametersString componentsSeparatedByString:@"&"];
        
        for (NSString *keyValuePair in urlComponents) {
            // 生成Key/Value
            NSArray *pairComponents = [keyValuePair componentsSeparatedByString:@"="];
            NSString *key = [pairComponents.firstObject stringByRemovingPercentEncoding];
            NSString *value = [pairComponents.lastObject stringByRemovingPercentEncoding];
            
            // Key不能为nil
            if (key == nil || value == nil) {
                continue;
            }
            
            id existValue = [params valueForKey:key];
            
            if (existValue != nil) {
                
                //                // 已存在的值，生成数组
                //                if ([existValue isKindOfClass:[NSArray class]]) {
                //                    // 已存在的值生成数组
                //                    NSMutableArray *items = [NSMutableArray arrayWithArray:existValue];
                //                    [items addObject:value];
                //
                //                    [params setValue:items forKey:key];
                //                } else {
                //
                //                    // 非数组
                //                    [params setValue:@[existValue, value] forKey:key];
                //                }
                
            } else {
                
                // 设置值
                [params setValue:value forKey:key];
            }
        }
    } else {
        // 单个参数
        
        // 生成Key/Value
        NSArray *pairComponents = [parametersString componentsSeparatedByString:@"="];
        
        // 只有一个参数，没有值
        if (pairComponents.count == 1) {
            return nil;
        }
        
        // 分隔值
        NSString *key = [pairComponents.firstObject stringByRemovingPercentEncoding];
        NSString *value = [pairComponents.lastObject stringByRemovingPercentEncoding];
        
        // Key不能为nil
        if (key == nil || value == nil) {
            return nil;
        }
        
        // 设置值
        [params setValue:value forKey:key];
    }
    
    return params;
}


- (NSString *)rij_trimingWhitespaceAndNewLine{
    //1. 去掉首尾空格和换行符
    NSString * str = [self copy];
    str = [str stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    //2. 去掉所有空格和换行符
    str = [str stringByReplacingOccurrencesOfString:@"\r" withString:@""];
    str = [str stringByReplacingOccurrencesOfString:@"\n" withString:@""];
    return str;
}

- (void)rij_trimingHeadWhiteSpaceAndNewLine{
    
}

- (NSString *)rij_getRealURLWithMap:(NSDictionary *)map{
    if (map) {
       NSDictionary * urlMap = map[@"web_to_viola_map"];
        if ([urlMap isKindOfClass:[NSDictionary class]]) {
            NSString * key = [self componentsSeparatedByString:@"?"].firstObject;
            if (key && urlMap[key] && [self respondsToSelector:@selector(rij_getURLParameters)]) {
                NSString * violaUrl = urlMap[key];
                if ([violaUrl isKindOfClass:[NSString class]] && violaUrl.length) {
                    NSDictionary * urlParam = [violaUrl  rij_getURLParameters];
                    NSString * minVersion = (NSString *)urlParam[@"v_minVersion"];
                    if (minVersion.length) {
                        NSString * curVersion = [[self class] rij_getAppVersion];
                        if ([NSString  compareVersionA:curVersion versionB:minVersion] == -1) {
                            return nil;
                        }
                    }
//                    violaUrl = [NSClassFromString(@"ViolaRouterManager") _replaceWebUrl:self violaUrl:violaUrl urlParam:@""];
                    return violaUrl;
                    
                }
            }
        }
    }
    return nil;
}


+ (int)compareVersionA:(NSString *)version versionB:(NSString *)versionB
{
    NSArray *versionNumbers1 = [version componentsSeparatedByString:@"."];
    NSArray *versionNumbers2 = [versionB componentsSeparatedByString:@"."];
    
    int maxNumbersCount = MAX((int)versionNumbers1.count,(int)versionNumbers2.count);
    for (int i=0; i<maxNumbersCount; i++) {
        NSInteger subVer1 ;
        NSInteger subVer2;
        if (versionNumbers1.count > i) {
            subVer1 = [versionNumbers1[i]   integerValue];
        }else{
            subVer1 = 0;
        }
        if (versionNumbers2.count > i) {
            subVer2 = [versionNumbers2[i]  integerValue];
        }else{
            subVer2 = 0;
        }
        
        if (subVer1 < subVer2) { //verion1的子版本号小于version2的子版本号，直接返回YES
            return -1;
        }else if (subVer1 > subVer2){//verion1的子版本号大于version2的子版本号，直接返回NO
            return 1;
        }else{  //两者相等，继续比较下一子版本号
            continue;
        }
    }
    //若所有子版本号都相等，则认为两个版本号相等
    return 0;
}

+ (NSString *)rij_getAppVersion{
    
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    NSString *appVersion = infoDictionary[@"CFBundleShortVersionString"];
    //    NSString *appBuild = CZ_DicGetValueForKey_c(infoDictionary,"CFBundleVersion");
    //    appBuild = [appBuild stringByReplacingOccurrencesOfString:@"." withString:@""];
    
    return  appVersion;
}



@end


@implementation  UIApplication (RIJCategory)

- (void)rij_setStatusBarStyle:(UIStatusBarStyle)style animated:(BOOL)animated{
    SEL sel = NSSelectorFromString(@"qq_setStatusBarStyle:animated:");
    if ([self respondsToSelector:sel]) {
        [self qq_setStatusBarStyle:style animated:animated];
    }else {
        [self setStatusBarStyle:style animated:animated];
    }
}

@end



//UIImage
@implementation UIImage (RIJCategory)

- (UIImage *)rij_makeRoundedWithRadius:(CGFloat) radius{
    return [self rij_imageByRoundCornerRadius:radius corners:(UIRectCornerAllCorners) borderWidth:0 borderColor:nil borderLineJoin:(kCGLineJoinMiter)];
}
- (UIImage *)rij_imageByRoundCornerRadius:(CGFloat)radius
                              borderWidth:(CGFloat)borderWidth
                              borderColor:(UIColor *)borderColor{
    return [self rij_imageByRoundCornerRadius:radius corners:(UIRectCornerAllCorners) borderWidth:borderWidth borderColor:borderColor borderLineJoin:(kCGLineJoinMiter)];
}
- (UIImage *)rij_imageByRoundCornerRadius:(CGFloat)radius
                                 corners:(UIRectCorner)corners
                             borderWidth:(CGFloat)borderWidth
                             borderColor:(UIColor *)borderColor
                          borderLineJoin:(CGLineJoin)borderLineJoin {
    
    if (corners != UIRectCornerAllCorners) {
        UIRectCorner tmp = 0;
        if (corners & UIRectCornerTopLeft) tmp |= UIRectCornerBottomLeft;
        if (corners & UIRectCornerTopRight) tmp |= UIRectCornerBottomRight;
        if (corners & UIRectCornerBottomLeft) tmp |= UIRectCornerTopLeft;
        if (corners & UIRectCornerBottomRight) tmp |= UIRectCornerTopRight;
        corners = tmp;
    }
    
    UIGraphicsBeginImageContextWithOptions(self.size, NO, self.scale);
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGRect rect = CGRectMake(0, 0, self.size.width, self.size.height);
    CGContextScaleCTM(context, 1, -1);
    CGContextTranslateCTM(context, 0, -rect.size.height);
    
    CGFloat minSize = MIN(self.size.width, self.size.height);
    if (borderWidth < minSize / 2) {
        UIBezierPath *path = [UIBezierPath bezierPathWithRoundedRect:CGRectInset(rect, borderWidth, borderWidth) byRoundingCorners:corners cornerRadii:CGSizeMake(radius, borderWidth)];
        [path closePath];
        
        CGContextSaveGState(context);
        [path addClip];
        CGContextDrawImage(context, rect, self.CGImage);
        CGContextRestoreGState(context);
    }
    
    if (borderColor && borderWidth < minSize / 2 && borderWidth > 0) {
        CGFloat strokeInset = (floor(borderWidth * self.scale) + 0.5) / self.scale;
        CGRect strokeRect = CGRectInset(rect, strokeInset, strokeInset);
        CGFloat strokeRadius = radius > self.scale / 2 ? radius - self.scale / 2 : 0;
        UIBezierPath *path = [UIBezierPath bezierPathWithRoundedRect:strokeRect byRoundingCorners:corners cornerRadii:CGSizeMake(strokeRadius, borderWidth)];
        [path closePath];
        
        path.lineWidth = borderWidth;
        path.lineJoinStyle = borderLineJoin;
        [borderColor setStroke];
        [path stroke];
    }
    
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
}

+ (UIImage *)rij_imageWithColor:(UIColor*)color size:(CGSize)size{
    CGFloat scale = [UIScreen mainScreen].scale;
    UIGraphicsBeginImageContextWithOptions(size, NO, scale);
    [color setFill];
    UIRectFill(CGRectMake(0, 0, size.width, size.height));
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
}

- (UIImage *)rij_imageByResizeToSize:(CGSize)size {
    if (size.width <= 0 || size.height <= 0) return nil;
    UIGraphicsBeginImageContextWithOptions(size, NO, self.scale);
    [self drawInRect:CGRectMake(0, 0, size.width, size.height)];
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
}

- (UIImage *)rij_imageByResizeToSize:(CGSize)size contentMode:(UIViewContentMode)contentMode {
    return [self rij_imageByResizeToSize:size contentMode:contentMode scale:self.scale];
}

- (UIImage *)rij_imageByResizeToSize:(CGSize)size contentMode:(UIViewContentMode)contentMode scale:(CGFloat)scale {
    if (size.width <= 0 || size.height <= 0) return nil;
    UIGraphicsBeginImageContextWithOptions(size, NO, scale);
    [self rij_drawInRect:CGRectMake(0, 0, size.width, size.height) withContentMode:contentMode clipsToBounds:NO];
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
}

- (void)rij_drawInRect:(CGRect)rect withContentMode:(UIViewContentMode)contentMode clipsToBounds:(BOOL)clips{
    CGRect drawRect = [[self class] rij_rectFitWithContentMode:contentMode rect:rect size:self.size];
    if (drawRect.size.width == 0 || drawRect.size.height == 0) return;
    if (clips) {
        CGContextRef context = UIGraphicsGetCurrentContext();
        if (context) {
            CGContextSaveGState(context);
            CGContextAddRect(context, rect);
            CGContextClip(context);
            [self drawInRect:drawRect];
            CGContextRestoreGState(context);
        }
    } else {
        [self drawInRect:drawRect];
    }
}


+ (CGRect)rij_rectFitWithContentMode:(UIViewContentMode)mode rect:(CGRect) rect size:(CGSize) size {
    rect = CGRectStandardize(rect);
    size.width = size.width < 0 ? -size.width : size.width;
    size.height = size.height < 0 ? -size.height : size.height;
    CGPoint center = CGPointMake(CGRectGetMidX(rect), CGRectGetMidY(rect));
    switch (mode) {
        case UIViewContentModeScaleAspectFit:
        case UIViewContentModeScaleAspectFill: {
            if (rect.size.width < 0.01 || rect.size.height < 0.01 ||
                size.width < 0.01 || size.height < 0.01) {
                rect.origin = center;
                rect.size = CGSizeZero;
            } else {
                CGFloat scale;
                if (mode == UIViewContentModeScaleAspectFit) {
                    if (size.width / size.height < rect.size.width / rect.size.height) {
                        scale = rect.size.height / size.height;
                    } else {
                        scale = rect.size.width / size.width;
                    }
                } else {
                    if (size.width / size.height < rect.size.width / rect.size.height) {
                        scale = rect.size.width / size.width;
                    } else {
                        scale = rect.size.height / size.height;
                    }
                }
                size.width *= scale;
                size.height *= scale;
                rect.size = size;
                rect.origin = CGPointMake(center.x - size.width * 0.5, center.y - size.height * 0.5);
            }
        } break;
        case UIViewContentModeCenter: {
            rect.size = size;
            rect.origin = CGPointMake(center.x - size.width * 0.5, center.y - size.height * 0.5);
        } break;
        case UIViewContentModeTop: {
            rect.origin.x = center.x - size.width * 0.5;
            rect.size = size;
        } break;
        case UIViewContentModeBottom: {
            rect.origin.x = center.x - size.width * 0.5;
            rect.origin.y += rect.size.height - size.height;
            rect.size = size;
        } break;
        case UIViewContentModeLeft: {
            rect.origin.y = center.y - size.height * 0.5;
            rect.size = size;
        } break;
        case UIViewContentModeRight: {
            rect.origin.y = center.y - size.height * 0.5;
            rect.origin.x += rect.size.width - size.width;
            rect.size = size;
        } break;
        case UIViewContentModeTopLeft: {
            rect.size = size;
        } break;
        case UIViewContentModeTopRight: {
            rect.origin.x += rect.size.width - size.width;
            rect.size = size;
        } break;
        case UIViewContentModeBottomLeft: {
            rect.origin.y += rect.size.height - size.height;
            rect.size = size;
        } break;
        case UIViewContentModeBottomRight: {
            rect.origin.x += rect.size.width - size.width;
            rect.origin.y += rect.size.height - size.height;
            rect.size = size;
        } break;
        case UIViewContentModeScaleToFill:
        case UIViewContentModeRedraw:
        default: {
            rect = rect;
        }
    }
    return rect;
}

@end

//color

@implementation UIColor (RIJCategory)

#pragma mark - 颜色转换 十六进制的颜色转换为UIColor

+ (UIColor *) rij_colorWithHexString: (NSString *)color{
    return [self rij_colorWithHexString:color alpha:1];
}

+ (UIColor *) rij_colorWithHexString: (NSString *)color alpha:(CGFloat) alpha{
    NSString *cString = [[color stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]] uppercaseString];
    
    // String should be 6 or 8 characters
    if ([cString length] < 6) {
        return [UIColor clearColor];
    }
    
    // strip 0X if it appears
    if ([cString hasPrefix:@"0X"])
        cString = [cString substringFromIndex:2];
        if ([cString hasPrefix:@"#"])
            cString = [cString substringFromIndex:1];
            if ([cString length] != 6)
                return [UIColor clearColor];
    
    // Separate into r, g, b substrings
    NSRange range;
    range.location = 0;
    range.length = 2;
    
    //r
    NSString *rString = [cString substringWithRange:range];
    
    //g
    range.location = 2;
    NSString *gString = [cString substringWithRange:range];
    
    //b
    range.location = 4;
    NSString *bString = [cString substringWithRange:range];
    
    // Scan values
    unsigned int r, g, b;
    [[NSScanner scannerWithString:rString] scanHexInt:&r];
    [[NSScanner scannerWithString:gString] scanHexInt:&g];
    [[NSScanner scannerWithString:bString] scanHexInt:&b];
    
    return [UIColor colorWithRed:((float) r / 255.0f) green:((float) g / 255.0f) blue:((float) b / 255.0f) alpha:alpha];
}


+ (BOOL)rij_isLighterColor:(UIColor *)color{
    return ![color rij_isDarkColor:color];
}

-(BOOL)rij_isDarkColor:(UIColor*)newColor{
    if ([self rij_alphaForColor: newColor]<10e-5) {
        return YES;
    }
    const CGFloat * componentColors = CGColorGetComponents(newColor.CGColor);
    CGFloat colorBrightness = ((componentColors[0] * 299) + (componentColors[1] * 587) + (componentColors[2] * 114)) / 1000;
    if (colorBrightness < 0.5){
        return YES;
    }
    else{
        return NO;
    }
}

- (CGFloat)rij_alphaForColor:(UIColor*)color {
    CGFloat r, g, b, a, w, h, s, l;
    BOOL compatible = [color getWhite:&w alpha:&a];
    if (compatible) {
        return a;
    } else {
        compatible = [color getRed:&r green:&g blue:&b alpha:&a];
        if (compatible) {
            return a;
        } else {
            [color getHue:&h saturation:&s brightness:&l alpha:&a];
            return a;
        }
    }
}

@end


@implementation NSMutableAttributedString (ReadInJoyVideo)

+ (NSMutableAttributedString *)createWithText:(NSString *)text font:(UIFont *)font color:(UIColor *)color{
    if (font && color) {
        NSMutableAttributedString * res = [[NSMutableAttributedString alloc] initWithString:text attributes:@{NSFontAttributeName:font,NSForegroundColorAttributeName:color}];;
        [res rij_sizeThatFitsForDefault];
        return res;
    }
    return nil;
}

+ (NSMutableAttributedString *)createForNoneSizeWithText:(NSString *)text font:(UIFont *)font color:(UIColor *)color{
    if (text && font && color) {
        NSMutableAttributedString * res = [[NSMutableAttributedString alloc] initWithString:text attributes:@{NSFontAttributeName:font,NSForegroundColorAttributeName:color}];;
        return res;
    }
    return nil;
}



@end


@implementation NSArray (RIJCategory)
- (id)rij_objectAtIndex:(NSUInteger)index{
    if (index >= [self count]) {
        return nil;
    }
    id value = [self objectAtIndex:index];
    if (value && [value isKindOfClass:[NSNull class]]) {
        return nil;
    }
    return value;
}

@end

@implementation NSMutableDictionary (RIJCategory)
- (void)rij_setObject:(id)anObject forKey:(id<NSCopying>)aKey{
    if (anObject) {
        [self setObject:anObject forKey:aKey];
    }
}

@end

@implementation NSDictionary (RIJCategory)

- (NSString *)vrij_convertToJsonStringWithOption:(NSJSONWritingOptions)option{
    NSDictionary * dic = self;
    if([self isKindOfClass:[NSString class]]){
        return (NSString *)self;
    }
    if (option == NSJSONWritingPrettyPrinted) {
//        dic = [self rij_convertToOnlyNumberAndStringDictionary];
    }
    
    if (!dic) {
        return @"";
    }
    NSError *error;
    
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dic options:option error:&error];
    
    NSString *jsonString;
    
    if (!jsonData) {
        NSLog(@"rij_convertToJsonString_error:%@",error);
    }else{
        
        jsonString = [[NSString alloc]initWithData:jsonData encoding:NSUTF8StringEncoding];
        
    }
    
    //    NSMutableString *mutStr = [NSMutableString stringWithString:jsonString];
    //
    //    NSRange range = {0,jsonString.length};
    
    //去掉字符串中的空格
    
    //    [mutStr replaceOccurrencesOfString:@" " withString:@"" options:NSLiteralSearch range:range];
    
    //    NSRange range2 = {0,mutStr.length};
    
    //去掉字符串中的换行符
    //    [mutStr replaceOccurrencesOfString:@"\n" withString:@"" options:NSLiteralSearch range:range2];
    return jsonString;
}

- (NSString *)vrij_convertToJsonString{
    return [self vrij_convertToJsonStringWithOption:NSJSONWritingPrettyPrinted];
    
}

- (NSString * )rij_urlEncodeString{
    NSMutableArray * pairs = [[NSMutableArray alloc] initWithCapacity:self.count];
    if ([self isKindOfClass:[NSDictionary class]]) {
        [self enumerateKeysAndObjectsUsingBlock:^(id  _Nonnull key, id  _Nonnull obj, BOOL * _Nonnull stop) {
            NSString * encodeKey = [((NSObject *)key) rij_urlEncode];
            NSString * encodeValue = [((NSObject *)obj) rij_urlEncode];
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


- (NSDictionary *)rij_convertToOnlyNumberAndStringDictionary{
    //
    NSMutableDictionary * resDictionary = [NSMutableDictionary new];
    [[self class] _parseOnlyNumberAndStringWithDictionary:resDictionary fromDictionary:self];
    return resDictionary;
}


+ (void) _parseOnlyNumberAndStringWithDictionary:(NSMutableDictionary *)toDictionary  fromDictionary:(NSDictionary *)fromDictionary{
#if DEBUG
    assert(toDictionary);
#endif
    if ([fromDictionary isKindOfClass:[NSDictionary class]]) {
        [fromDictionary enumerateKeysAndObjectsUsingBlock:^(id  _Nonnull key, id  _Nonnull obj, BOOL * _Nonnull stop) {
            if (key) {
                if ([obj isKindOfClass:[NSArray class]]) {
                    NSMutableArray * arrayValue = [NSMutableArray new];
                    for (NSObject * ele in (NSArray *)obj) {
                        if ([ele isKindOfClass:[NSDictionary class]]) {
                            NSMutableDictionary * dicValue = [NSMutableDictionary new];
                            [[self class] _parseOnlyNumberAndStringWithDictionary:dicValue fromDictionary:(NSDictionary *)ele];
                            [arrayValue addObject:dicValue];
                        }else {
                            
                            if (![ele isKindOfClass:[NSNumber class]] && ![ele isKindOfClass:[NSString class]]) {
                                 NSString * stringValue = [NSString stringWithFormat:@"%@_descriont:%@", NSStringFromClass([ele class]),[ele description]];
                                if(stringValue){
                                    [arrayValue addObject:stringValue];
                                }
                            }else {
                                [arrayValue addObject:ele];
                            }
                            
                        }
                    }
                    [toDictionary setObject:arrayValue forKey:key];
                }else if([obj isKindOfClass:[NSDictionary class]]){
                    NSMutableDictionary * dicValue = [NSMutableDictionary new];
                    [[self class] _parseOnlyNumberAndStringWithDictionary:dicValue fromDictionary:(NSDictionary *)obj];
                    [toDictionary setObject:dicValue forKey:key];
                }else if(obj){
                    
                   
                        
                        
                    if (![obj isKindOfClass:[NSNumber class]] && ![obj isKindOfClass:[NSString class]]) {
                        NSString * stringValue = [NSString stringWithFormat:@"%@_descriont:%@", NSStringFromClass([obj class]),[obj description]];
                        if(stringValue){
                            [toDictionary setObject:stringValue forKey:key];
                        }
                    }else {
                        if ([obj isKindOfClass:[NSNumber class]]) {
                             NSNumber * number = (NSNumber *)obj;
                            
                            double doubleValue = [number doubleValue];
                             if (isnan(doubleValue) || isinf(doubleValue)) {
#if DEBUG
                                 assert(0);//@"vaild number(nan or inf)");
#endif
                                 [toDictionary setObject:@(0) forKey:key];//0 代替NaN或INF值
                             }else {
                                 [toDictionary setObject:obj forKey:key];
                             }
                        }else {
                           [toDictionary setObject:obj forKey:key];
                        }
                       
                    }
                    
                    
                    
                }
            }
        }];
    }else if(fromDictionary){
#if DEBUG
        assert(0);
#endif
    }
    
}

@end






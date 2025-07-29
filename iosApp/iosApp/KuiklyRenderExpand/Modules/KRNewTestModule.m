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

#import "KRNewTestModule.h"

@implementation KRNewTestModule

TDF_EXPORT_MODULE(NewTestModule)

TDF_EXPORT_METHOD(fun1:(NSDictionary *)p1
                  p2:(NSArray *)p2
                  p3:(NSNumber *)p3
                  p4:(NSString *)p4
                  p5:(CGFloat)p5
                  p6:(NSTimeInterval)p6
                  p7:(NSSet *)p7
                  p8:(NSUInteger)p8
                  ) {
    // set类型不支持
    return nil;
}

TDF_EXPORT_METHOD(fun2:(NSString *)arg1 callback1:(TDFModuleSuccessCallback) callback1 callback2:(TDFModuleErrorCallback)callback2) {
 
    callback1(@{@"sgeoi": @123, @"list": @[@(YES)] });
//    NSError *error = [NSError errorWithDomain:NSCocoaErrorDomain code:123 userInfo:@{@"sgeoi": @123, @"list": @[@(YES)] }];
//    callback2(error);
    callback2(@"error", @"seg", nil);
    return nil;
}

TDF_EXPORT_METHOD(fun3:(BOOL)p1
                  p2:(double)p2
                  p3:(float)p3
                  p4:(int)p4
                  p5:(uint64_t)p5
                  ) {

    return nil;
}


// 不支持这些类型 看convert头文件里面有的类型
//TDF_EXPORT_METHOD(fun4:(char)p1
//                  p2:(unsigned char)p2
//                  p3:(short)p3
//                  p4:(long)p4
//                  p5:(unsigned long long)p5
//                  p6:(void *)p6
//                  ) {
//
//}


@end

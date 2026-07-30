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

#import "TDFTestModule.h"

@implementation TDFTestModule

TDF_EXPORT_METHOD(syncCall:(NSString *)p1
                  p2:(int)p2
                  p3:(double)p3
                  p4:(BOOL)p4
                  p5:(float)p5
                  p6:(NSArray *)p6
                  p7:(NSDictionary *)p7
                  ) {
    return nil;
}

TDF_EXPORT_METHOD(syncCallWithReturnValue:(NSString *)p1
                  p2:(int)p2
                  p3:(double)p3
                  p4:(BOOL)p4
                  p5:(float)p5
                  p6:(NSArray *)p6
                  p7:(NSDictionary *)p7
                  ) {
    return @[@1, @4, @"1235"];
}

TDF_EXPORT_METHOD(asyncCall:(BOOL)isSucc
                  succCallback:(TDFModuleSuccessCallback)sCb
                  errorCallback:(nonnull TDFModuleErrorCallback)eCb
                  ) {
    
    if (sCb) {
        sCb(@"xxx succ");
    }
    if (eCb) {
        NSError *error = [NSError errorWithDomain:NSCocoaErrorDomain code:124 userInfo:@{@"qweg": @(213)}];
        eCb(@"weg", @"qweg", error);
    }
    return nil;
}

@end

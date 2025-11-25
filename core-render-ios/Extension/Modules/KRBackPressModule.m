//
//  KRBackPressModule.m
//  OpenKuiklyIOSRender
//
//  Created by xubin on 2025/11/24.
//

#import <Foundation/Foundation.h>
#import "KRBackPressModule.h"

@implementation KRBackPressModule

- (instancetype)init {
    self = [super init];
    if (self) {
        _isBackConsumed = NO;
        _backConsumedTime = 0;
    }
    return self;
}
// Koltin侧接收Event后调用Native传递拦截参数
- (id)hrv_callWithMethod:(NSString *)method params:(id)params callback:(KuiklyRenderCallback)callback {
    if ([method isEqualToString:@"backHandle"]) {
        [self backHandleWithParams:params];
        return nil;
    }
    return [super hrv_callWithMethod:method params:params callback:callback];
}

- (void)backHandleWithParams:(nullable NSString *)params {
    if (params) {
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[params dataUsingEncoding:NSUTF8StringEncoding] options:0 error:nil];
        self.isBackConsumed = [dict[@"consumed"] intValue] == 1;
    } else {
        self.isBackConsumed = NO;
    }
    self.backConsumedTime = [[NSDate date] timeIntervalSince1970];
}





@end

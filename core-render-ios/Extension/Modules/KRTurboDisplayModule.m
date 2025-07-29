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

#import "KRTurboDisplayModule.h"
NSString *const kSetCurrentUIAsFirstScreenForNextLaunchNotificationName = @"kSetCurrentUIAsFirstScreenForNextLaunchNotificationName";
NSString *const kCloseTurboDisplayNotificationName = @"kCloseTurboDisplayNotificationName";

@implementation KRTurboDisplayModule

/**
 * 下次启动设置当前 UI 作为首屏(call by kotlin)
 */
- (void)setCurrentUIAsFirstScreenForNextLaunch:(NSDictionary *)args {
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSNotificationCenter defaultCenter] postNotificationName:kSetCurrentUIAsFirstScreenForNextLaunchNotificationName
                                                            object:self.hr_rootView
                                                          userInfo:nil];
    });
}
/**
 * 关闭TurboDisplay模式
 */
- (void)closeTurboDisplay:(NSDictionary *)args {
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSNotificationCenter defaultCenter] postNotificationName:kCloseTurboDisplayNotificationName object:self.hr_rootView userInfo:nil];
    });
}

/**
 * 首屏是否为TurboDisplay模式
 */
- (NSString *)isTurboDisplay:(NSDictionary *)args {
    return self.firstScreenTurboDisplay ? @"1" : @"0";
}
@end

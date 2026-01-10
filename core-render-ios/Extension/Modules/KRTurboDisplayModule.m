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
#import "KRTurboDisplayCacheManager.h"

NSString *const kSetCurrentUIAsFirstScreenForNextLaunchNotificationName = @"kSetCurrentUIAsFirstScreenForNextLaunchNotificationName";
NSString *const kCloseTurboDisplayNotificationName = @"kCloseTurboDisplayNotificationName";
NSString *const kClearCurrentPageCacheNotificationName = @"kClearCurrentPageCacheNotificationName";

@implementation KRTurboDisplayModule

/**
 * 下次启动设置当前 UI 作为首屏(call by kotlin)
 */
- (void)setCurrentUIAsFirstScreenForNextLaunch:(NSDictionary *)args {
    NSLog(@"【TurboDisplay-setCurrentUIAsFirstScreenForNextLaunch】开始设置当前UI为下次首屏");
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSNotificationCenter defaultCenter] postNotificationName:kSetCurrentUIAsFirstScreenForNextLaunchNotificationName
                                                            object:self.hr_rootView
                                                          userInfo:nil];
        NSLog(@"【TurboDisplay-setCurrentUIAsFirstScreenForNextLaunch】通知已发送");
    });
}

/**
 * 关闭TurboDisplay模式
 */
- (void)closeTurboDisplay:(NSDictionary *)args {
    NSLog(@"【TurboDisplay-closeTurboDisplay】关闭TurboDisplay模式");
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSNotificationCenter defaultCenter] postNotificationName:kCloseTurboDisplayNotificationName object:self.hr_rootView userInfo:nil];
        NSLog(@"【TurboDisplay-closeTurboDisplay】通知已发送");
    });
}

/**
 * 首屏是否为TurboDisplay模式
 */
- (NSString *)isTurboDisplay:(NSDictionary *)args {
    NSString *result = self.firstScreenTurboDisplay ? @"1" : @"0";
    NSLog(@"【TurboDisplay-isTurboDisplay】查询结果: %@", result);
    return result;
}

/**
 * 强制清除所有TurboDisplay缓存文件
 */
- (void)clearAllCache:(NSDictionary *)args {
    NSLog(@"【TurboDisplay-clearAllCache】开始清除所有缓存");
    [[KRTurboDisplayCacheManager sharedInstance] removeAllTurboDisplayCacheFiles];
    NSLog(@"【TurboDisplay-clearAllCache】所有缓存已清除");
}

/**
 * 强制清除当前页面的TurboDisplay缓存
 */
- (void)clearCurrentPageCache:(NSDictionary *)args {
    NSLog(@"【TurboDisplay-clearCurrentPageCache】开始清除当前页面缓存");
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSNotificationCenter defaultCenter] postNotificationName:kClearCurrentPageCacheNotificationName object:self.hr_rootView userInfo:nil];
        NSLog(@"【TurboDisplay-clearCurrentPageCache】通知已发送");
    });
}

@end

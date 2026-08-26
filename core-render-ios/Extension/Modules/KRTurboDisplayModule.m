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
NSString *const kExecuteTurboDisplayDiffNotificationName = @"kExecuteTurboDisplayDiffNotificationName";
NSString *const kSuspendTurboDisplayDiffNotificationName = @"kSuspendTurboDisplayDiffNotificationName";

@implementation KRTurboDisplayModule

/**
 * 下次启动设置当前 UI 作为首屏(call by kotlin)
 * @param args 参数字典，可包含 extraCacheContent 用于传递额外缓存内容（如 ListView 的 offset）
 */
- (void)setCurrentUIAsFirstScreenForNextLaunch:(NSDictionary *)args {
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSNotificationCenter defaultCenter] postNotificationName:kSetCurrentUIAsFirstScreenForNextLaunchNotificationName
                                                            object:self.hr_rootView
                                                          userInfo:args];
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
    NSString *result = self.firstScreenTurboDisplay ? @"1" : @"0";
    return result;
}

/**
 * 强制清除所有TurboDisplay缓存文件
 */
- (void)clearAllCache:(NSDictionary *)args {
    [[KRTurboDisplayCacheManager sharedInstance] removeAllTurboDisplayCacheFiles];
}

/**
 * 强制清除当前页面的TurboDisplay缓存
 */
- (void)clearCurrentPageCache:(NSDictionary *)args {
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSNotificationCenter defaultCenter] postNotificationName:kClearCurrentPageCacheNotificationName object:self.hr_rootView userInfo:nil];
    });
}

/**
 * 挂起diff的页面声明(call by kotlin)，无需参数：
 * 必须同步post（禁止dispatch_async）：声明必须早于handler didInit的挂起判断点，
 * created()的同步调用栈内直达handler；didInit后到达的声明由handler侧忽略（迟到防御）
 */
- (void)suspendTurboDisplayDiff:(NSDictionary *)args {
    [[NSNotificationCenter defaultCenter] postNotificationName:kSuspendTurboDisplayDiffNotificationName
                                                        object:self.hr_rootView
                                                      userInfo:nil];
}

/**
 * 挂起diff的执行触发(call by kotlin)，无需参数：
 * 挂起由页面created中suspendTurboDisplayDiff声明驱动（零Native配置），didInit静态挂起，
 * 免疫sync事件插队；幂等由Handler侧diffSuspended状态保证（非挂起/已执行的通知在Handler被拒）
 */
- (void)executeTurboDisplayDiff:(NSDictionary *)args {
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSNotificationCenter defaultCenter] postNotificationName:kExecuteTurboDisplayDiffNotificationName
                                                            object:self.hr_rootView
                                                          userInfo:nil];
    });
}

@end

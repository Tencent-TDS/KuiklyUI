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

#import "KRPerformanceModule.h"
#import "KuiklyRenderView.h"
#import "KRPerformanceDataProtocol.h"

NSString *const kKuiklyPageLoadTimeFromKotlinNotification = @"KuiklyPageLoadTimeFromKotlinNotification";

@implementation KRPerformanceModule

- (void)onPageCreateFinish:(NSDictionary *)args {
    NSDictionary *params = [args[KR_PARAM_KEY] kr_stringToDictionary];
    [[NSNotificationCenter defaultCenter] postNotificationName:kKuiklyPageLoadTimeFromKotlinNotification object:self.hr_rootView userInfo:params];
}

- (void)getPerformanceData:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    
    KuiklyRenderView *rootView = self.hr_rootView;
    id<KRPerformanceDataProtocol> performanceManager = rootView.delegate.performanceManager;
    
    NSArray *keysArray = @[@"initViewCost", @"fetchContextCodeCost", @"initRenderContextCost", @"pageBuildCost", @"pageLayoutCost", @"createPageCost", @"firstPaintCost", @"createInstanceCost", @"newPageCost", @"renderCost"];
    NSMutableDictionary *timeMap = [NSMutableDictionary new];
    NSAssert(keysArray.count == KRLoadStage_renderFP + 1, @"keys 与 枚举数量不匹配 ");
    for (int i = KRLoadStage_initView; i <= KRLoadStage_renderFP; i++) {
        int duration = [performanceManager durationForStage:i];
        timeMap[keysArray[i]] = @(duration);
    }
    
    NSDictionary *performData = @{
        @"mode": @(rootView.contextParam.contextMode.modeId),
        @"pageExistTime": @(performanceManager.pageExistTime),
        @"isFirstLaunchOfProcess": @([performanceManager isFirstLaunchOfProcess]),
        @"isFirstLaunchOfPage": @([performanceManager isFirstLaunchOfPage]),
        @"pageLoadTime": timeMap,
        @"mainFPS": @(performanceManager.mainFPS.avgFPS),
        @"kotlinFPS": @(performanceManager.kotlinFPS.avgFPS),
        @"memory": @{
            @"avgIncrement": @(performanceManager.memoryMonitor.avgIncrementMemory),
            @"peakIncrement": @(performanceManager.memoryMonitor.peakIncrementMemory),
            @"appPeak": @(performanceManager.memoryMonitor.appPeakMemory),
            @"appAvg": @(performanceManager.memoryMonitor.appAvgMemory),
        },
    };
    callback(performData);
}

@end

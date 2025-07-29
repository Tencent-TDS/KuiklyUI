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

#ifndef KuiklyRenderModuleExportProtocol_h
#define KuiklyRenderModuleExportProtocol_h
#define KR_SYNC_CALLBACK_KEY @"hr_sync_callback"
NS_ASSUME_NONNULL_BEGIN
@class UIView;
@class KuiklyRenderView;
@class KuiklyContextParam;
/*
 * 回调给Kotlin侧的闭包
 * @param result 数据(类型可为NSDictionary, NSArray, NSString, NSNumber, NSData, 注: NSDictionary在Kotlin侧对应为String类型)
 */
typedef void (^KuiklyRenderCallback)(id _Nullable result);
/*
 * @brief 暴露native侧的module组件所需实现的协议
 */
@protocol KuiklyRenderModuleExportProtocol <NSObject>

@optional

/*
 * @brief 当前所在根视图
 */
@property (nonatomic, weak) KuiklyRenderView *hr_rootView;
/*
 * @brief
 */
@property (nonatomic, strong) KuiklyContextParam *hr_contextParam;

/*
 * @brief Kotlin侧调用当前module的实例方法出发该回调(多线程调用)
 * @param method 方法名
 * @param params 方法参数 (透传kotlin侧数据, 类型可为NSString, NSArray, NSData, NSNumber)
 * @param callback 方法中的异步回调闭包参数
 * @return 同步返回给kotlin侧的返回值(类型可为NSDictionary, NSString, NSArray, NSData, NSNumber)
 */
- (id _Nullable)hrv_callWithMethod:(NSString * _Nonnull)method
                    params:(id _Nullable)params
                  callback:(KuiklyRenderCallback _Nullable)callback;


@end


NS_ASSUME_NONNULL_END

#endif /* KuiklyRenderModuleExportProtocol_h */

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

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

#define KR_ASSERT_CONTEXT_HTREAD assert([KuiklyRenderThreadManager isContextQueue])

@interface KuiklyRenderThreadManager : NSObject

/*
 * 指定Context线程执行闭包
 * @param block 任务闭包
 */
+ (void)performOnContextQueueWithBlock:(dispatch_block_t)block;

/*
 * 指定Context线程执行闭包
 * @param block 任务闭包
 * @param sync 是否同步执行
 */
+ (void)performOnContextQueueWithBlock:(dispatch_block_t)block sync:(BOOL)sync;
/*
 * 如果是在context线程的话，立即在context线程执行，否则next runloop执行
 */
+ (void)performOnContextQueueImmediatelyWithBlock:(dispatch_block_t)block;

/*
 * 主线程执行任务
 */
+ (void)performOnMainQueueWithTask:(dispatch_block_t)task sync:(BOOL)sync;
/*
 * 指定Log线程执行闭包
 * @param block 任务闭包
 */
+ (void)performOnLogQueueWithBlock:(dispatch_block_t)block;

/*
 * Context线程
 */
+ (dispatch_queue_t)contextQueue;
/*
 * 当前所处线程是否为context线程
 */
+ (BOOL)isContextQueue;
/*
 * TDFModule线程执行
 * @param moduleName TDFModule名字
 * @param task 执行的闭包
 * @return 是否执行成功
 */
+ (BOOL)performOnModuleQueueWithTDFModuleName:(NSString *)moduleName task:(dispatch_block_t)task;
/*
 * 断言当前线程必须为ContextQueue
 */
+ (void)assertContextQueue;

/*
 * 延时在主线程执行
 * @param task 主线程上执行的闭包任务
 * @param delay 延时时间，单位为ms
 */
+ (void)performOnMainQueueWithTask:(dispatch_block_t)task delay:(CGFloat)delay;

/*
 * 延时在context线程执行
 * @param task context线程延时执行的闭包任务
 * @param delay 延时时间，单位为s
 */
+ (void)performOnContextQueueWithTask:(dispatch_block_t)task delay:(CGFloat)delay;
@end

NS_ASSUME_NONNULL_END

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

#import "KuiklyRenderThreadManager.h"
#import "KRLogModule.h"
#include <TargetConditionals.h>

NSString *const KRRenderContextQueueName = @"com.tencent.kuikly.context";
NSString *const KRRenderLogQueueName = @"com.tencent.kuikly.log";

#if TARGET_OS_OSX
static void *KRContextQueueSpecificKey = &KRContextQueueSpecificKey;

static dispatch_queue_t KRContextQueue(void) {
    static dispatch_queue_t queue = NULL;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        dispatch_queue_attr_t queueAttr =
            dispatch_queue_attr_make_with_qos_class(DISPATCH_QUEUE_SERIAL, QOS_CLASS_USER_INTERACTIVE, 0);
        queue = dispatch_queue_create([KRRenderContextQueueName UTF8String], queueAttr);
        dispatch_queue_set_specific(queue, KRContextQueueSpecificKey, KRContextQueueSpecificKey, NULL);
    });
    return queue;
}
#else
static NSThread *gContextThread = nil;
static CFRunLoopRef gContextRunLoop = NULL;
static dispatch_semaphore_t gContextThreadReadySemaphore = nil;
static NSPort *gContextRunLoopKeepAlivePort = nil;

static void KREnsureContextThreadStarted(void);

static void KRPerformOnContextThread(dispatch_block_t block) {
    if (!block) {
        return;
    }
    KREnsureContextThreadStarted();
    CFRunLoopPerformBlock(gContextRunLoop, kCFRunLoopCommonModes, block);
    CFRunLoopWakeUp(gContextRunLoop);
}
#endif

@implementation KuiklyRenderThreadManager

#if !TARGET_OS_OSX
+ (void)p_contextThreadMain {
    @autoreleasepool {
        NSRunLoop *runLoop = [NSRunLoop currentRunLoop];
        gContextRunLoopKeepAlivePort = [NSPort port];
        [runLoop addPort:gContextRunLoopKeepAlivePort forMode:NSRunLoopCommonModes];
        gContextRunLoop = CFRunLoopGetCurrent();
        CFRetain(gContextRunLoop);
        dispatch_semaphore_signal(gContextThreadReadySemaphore);

        while (!NSThread.currentThread.cancelled) {
            @autoreleasepool {
                [runLoop runMode:NSDefaultRunLoopMode beforeDate:NSDate.distantFuture];
            }
        }
    }
}
#endif

// 指定Context线程执行闭包
+ (void)performOnContextQueueWithBlock:(dispatch_block_t)block {
    [self performOnContextQueueWithBlock:block sync:NO];
}

// 指定Context线程执行闭包
+ (void)performOnContextQueueWithBlock:(dispatch_block_t)block sync:(BOOL)sync {
    if (!block) {
        return;
    }
    if (sync) {
        if ([self isContextQueue]) {
            block();
        } else {
#if TARGET_OS_OSX
            dispatch_sync(KRContextQueue(), block);
#else
            dispatch_semaphore_t completion = dispatch_semaphore_create(0);
            KRPerformOnContextThread(^{
                block();
                dispatch_semaphore_signal(completion);
            });
            dispatch_semaphore_wait(completion, DISPATCH_TIME_FOREVER);
#endif
        }
    } else {
#if TARGET_OS_OSX
        dispatch_async(KRContextQueue(), block);
#else
        KRPerformOnContextThread(block);
#endif
    }
}

+ (void)performOnLogQueueWithBlock:(dispatch_block_t)block {
    dispatch_async([KuiklyRenderThreadManager logQueue], block);
}

+ (void)performOnContextQueueImmediatelyWithBlock:(dispatch_block_t)block {
    if (!block) {
        return;
    }
    if ([self isContextQueue]) {
        block();
    } else {
        [self performOnContextQueueWithBlock:block];
    }
}

// 主线程执行任务
+ (void)performOnMainQueueWithTask:(dispatch_block_t)task sync:(BOOL)sync {
    if (sync) {
        if ([NSThread isMainThread]) {
            task();
        } else {
            dispatch_sync(dispatch_get_main_queue(), task);
        }
    } else {
        dispatch_async(dispatch_get_main_queue(), task);
    }
}
// TDFModule线程执行
+ (BOOL)performOnModuleQueueWithTDFModuleName:(NSString *)moduleName task:(dispatch_block_t)task{
    Class moduleClass = TDGGetModuleClass(moduleName);
    if (!moduleClass) {
        [KRLogModule logError:[NSString stringWithFormat:
                                   @"没找到对应的module "
                                   @"%@，请注意是否有TDF_EXPORT_MODULE或者类名是否与kotlin 的moduleName一致",
                                   moduleName ?: @""]];
        return NO;
    }
    dispatch_queue_t methodQueue = dispatch_get_main_queue();
    if ([moduleClass respondsToSelector:@selector(methodQueue)]) {
        methodQueue = [moduleClass methodQueue]
                          ?: dispatch_get_main_queue();  // 默认异步派发到主线程，对齐kuikly旧module的线程逻辑
    }
    dispatch_async(methodQueue, task);
    return YES;
}


static dispatch_queue_t gLogQueue = NULL;
+ (dispatch_queue_t)logQueue {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        dispatch_queue_attr_t queue_attr = dispatch_queue_attr_make_with_qos_class(DISPATCH_QUEUE_SERIAL,
                                                                                   QOS_CLASS_DEFAULT,0);
        gLogQueue = dispatch_queue_create([KRRenderLogQueueName UTF8String],
                                              queue_attr);
        dispatch_queue_set_specific(gLogQueue,
                                    &gLogQueue,
                                    (void *)[KRRenderLogQueueName UTF8String], (dispatch_function_t)CFRelease);
    });
    return gLogQueue;
}

+ (BOOL)isContextQueue {
#if TARGET_OS_OSX
    return dispatch_get_specific(KRContextQueueSpecificKey) != NULL;
#else
    return gContextThread != nil && NSThread.currentThread == gContextThread;
#endif
}

+ (void)assertContextQueue {
    assert([KuiklyRenderThreadManager isContextQueue]);
}

/*
 * 延时在主线程执行
 * @param task 主线程上执行的闭包任务
 * @param delay 延时时间，单位为s
 */
+ (void)performOnMainQueueWithTask:(dispatch_block_t)task delay:(CGFloat)delay {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)), dispatch_get_main_queue(), task);
}

/*
 * 延时在context线程执行
 * @param task context线程延时执行的闭包任务
 * @param delay 延时时间，单位为s
 */
+ (void)performOnContextQueueWithTask:(dispatch_block_t)task delay:(CGFloat)delay {
    if (!task) {
        return;
    }
#if TARGET_OS_OSX
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)),
                   KRContextQueue(), task);
#else
    [self performOnContextQueueWithBlock:^{
        NSTimer *timer = [NSTimer timerWithTimeInterval:MAX(0, delay)
                                                repeats:NO
                                                  block:^(__unused NSTimer *firedTimer) {
            task();
        }];
        [[NSRunLoop currentRunLoop] addTimer:timer forMode:NSRunLoopCommonModes];
    }];
#endif
}


@end

#if !TARGET_OS_OSX
static void KREnsureContextThreadStarted(void) {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        gContextThreadReadySemaphore = dispatch_semaphore_create(0);
        gContextThread =
            [[NSThread alloc] initWithTarget:KuiklyRenderThreadManager.class
                                   selector:@selector(p_contextThreadMain)
                                     object:nil];
        gContextThread.name = KRRenderContextQueueName;
        gContextThread.qualityOfService = NSQualityOfServiceUserInteractive;
        gContextThread.stackSize = 1024 * 1024;
        [gContextThread start];
        dispatch_semaphore_wait(gContextThreadReadySemaphore, DISPATCH_TIME_FOREVER);
    });
}
#endif

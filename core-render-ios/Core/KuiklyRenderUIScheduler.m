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

#import "KuiklyRenderThreadLock.h"
#import "KuiklyRenderThreadManager.h"
#import "KuiklyRenderUIScheduler.h"
#import "KRComponentDefine.h"

@interface KuiklyRenderUIScheduler ()
/** 需要在主线程执行的闭包 */
@property(nonatomic, strong) dispatch_block_t needSyncMainQueueTasksBlock;
/** Context线程上的主线程任务集合 */
@property(nonatomic, strong) NSMutableArray<dispatch_block_t> *mainThreadTasksOnContextQueue;
/** 主线程上的任务集合 */
@property(nonatomic, strong) NSMutableArray<dispatch_block_t> *mainThreadTasks;
/** 执行主线程任务中 */
@property(nonatomic, assign, readwrite) BOOL performingMainQueueTask;
/** 调度器代理 */
@property(nonatomic, weak) id<KuiklyRenderUISchedulerDelegate> delegate;
/** 主线程任务线程安全锁 */
@property(nonatomic, strong) KuiklyRenderThreadLock *threadLock;
/** 首屏视图是否加载完 */
@property(nonatomic, assign) BOOL viewDidLoad;
/** 标记首屏加载完成*/
@property(nonatomic, assign) BOOL isMarkViewDidLoad;
/** 主线程上的任务集合 */
@property(nonatomic, strong) NSMutableArray<dispatch_block_t> *viewDidLoadMainThreadTasks;


@end

@implementation KuiklyRenderUIScheduler

#pragma mark - init

- (instancetype)initWithDelegate:(id<KuiklyRenderUISchedulerDelegate>)delegate {
    if (self = [super init]) {
        self.delegate = delegate;
        self.mainThreadTasks = [[NSMutableArray alloc] init];
        self.viewDidLoadMainThreadTasks = [[NSMutableArray alloc] init];
        self.threadLock = [[KuiklyRenderThreadLock alloc] init];
  }
  return self;
}

#pragma mark - public

/*
 * @brief 添加任务到主线程批量一次执行
 */
- (void)addTaskToMainQueueWithTask:(dispatch_block_t)taskBlock {
    KR_ASSERT_CONTEXT_HTREAD;
    if (!_mainThreadTasksOnContextQueue) {
        
        _mainThreadTasksOnContextQueue = [[NSMutableArray alloc] init];
    }
    [_mainThreadTasksOnContextQueue addObject:taskBlock];
    [self p_setNeedSyncMainQuequeTasks];
}
/*
 * @brief 立即执行待执行的主线程队列任务
 */
- (void)performSyncMainQueueTasksBlockIfNeed {
    KR_ASSERT_CONTEXT_HTREAD;
    if (self.needSyncMainQueueTasksBlock) {
        self.needSyncMainQueueTasksBlock();
        self.needSyncMainQueueTasksBlock = nil;
    }
}
/*
 * @brief 添加执行首屏完成后再去执行该任务
 */
- (void)performWhenViewDidLoadWithTask:(dispatch_block_t)task {
    NSAssert([NSThread isMainThread], @"请在主线程调用该接口");
    if (!task) {
        return;
    }
    if (self.viewDidLoad || self.isMarkViewDidLoad) {
        task();
    } else {
        [self.viewDidLoadMainThreadTasks addObject:task];
    }
}
/*
 * @brief 标记首屏已经已经加载完成
 */
- (void)markViewDidLoad {
    self.isMarkViewDidLoad = YES;
}

#pragma mark - private

- (void)p_setNeedSyncMainQuequeTasks {
    if (!_needSyncMainQueueTasksBlock) {
        KR_WEAK_SELF
        self.needSyncMainQueueTasksBlock = ^{
            KR_STRONG_SELF_RETURN_IF_NIL
           // 同步主线程任务前，需要告诉kotlin侧 去 layoutIfNeed, 避免viewFrame设置时机和创建view时机不同步
            [strongSelf p_dispatchWillPerformUITasksDelegator];
            NSArray *tasks = weakSelf.mainThreadTasksOnContextQueue;
            weakSelf.mainThreadTasksOnContextQueue = nil;
            [weakSelf.threadLock threadSafeInBlock:^{
            [weakSelf.mainThreadTasks addObjectsFromArray:tasks ?: [NSArray new]];
         }];
        [KuiklyRenderThreadManager performOnMainQueueWithTask:^{
            KR_STRONG_SELF_RETURN_IF_NIL
            __block NSArray* mainThreadTasks = nil;
            [strongSelf.threadLock threadSafeInBlock:^{
                mainThreadTasks = [weakSelf.mainThreadTasks copy];
                [weakSelf.mainThreadTasks removeAllObjects];
            }];
            strongSelf.performingMainQueueTask = YES;
            for (dispatch_block_t task in (mainThreadTasks ?: [NSArray new])) {
                task();
            }
            strongSelf.performingMainQueueTask = NO;
            [strongSelf p_performTaskAfterViewDidLoadOnOnce];
          } sync:[NSThread isMainThread]];
    };
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
        [weakSelf performSyncMainQueueTasksBlockIfNeed];
    }];
  }
}

// 分发代理
- (void)p_dispatchWillPerformUITasksDelegator {
    if ([self.delegate respondsToSelector:@selector(willPerformUITasksWithScheduler:)]) {
        [self.delegate willPerformUITasksWithScheduler:self];
    }
}

// 首屏执行后的待执行任务
- (void)p_performTaskAfterViewDidLoadOnOnce {
    if (!self.viewDidLoad) {
        self.viewDidLoad = YES;
        [self p_performViewDidLoadTasks];
    }
}

// perform all wait to after viewDidLoad tasks
- (void)p_performViewDidLoadTasks {
    dispatch_block_t block = ^{
        for (dispatch_block_t task in [self.viewDidLoadMainThreadTasks copy]) {
            task();
        }
        self.viewDidLoadMainThreadTasks = [[NSMutableArray alloc] init];
    };
    if (_isMarkViewDidLoad) { // 已经标记，当前帧直接处理
        block();
    } else {
        dispatch_async(dispatch_get_main_queue(), block);
    }
    
}

@end

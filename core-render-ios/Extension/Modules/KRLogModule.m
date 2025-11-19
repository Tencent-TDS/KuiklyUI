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

#import "KRLogModule.h"
#import "KuiklyRenderThreadManager.h"
#import "KRConvertUtil.h"

static id<KuiklyLogProtocol> gLogHandler;
static id<KuiklyLogProtocol> gLogUserSuppliedHandler;


@interface KuiklyLogHandler : NSObject<KuiklyLogProtocol>

@end

@implementation KuiklyLogHandler

- (BOOL)asyncLogEnable {
    return NO;
}

- (void)logInfo:(NSString *)message {
    NSLog(@"%@", message);
}

- (void)logDebug:(NSString *)message {
#if DEBUG
    NSLog(@"%@", message);
#endif
}

- (void)logError:(NSString *)message {
    NSLog(@"%@", message);
}

@end


@interface KRLogModule()

@property (nonatomic, assign) BOOL needSyncLogTasks; // 防抖标志位，用于避免重复触发批量日志处理任务。
@property (nonatomic, strong) NSMutableArray<dispatch_block_t> *logTasks;
@property (nonatomic, assign) BOOL asyncLogEnable;
@end

@implementation KRLogModule

- (instancetype)init {
    if (self = [super init]) {
        // 在异步日志模式下，日志任务会先存入这个数组，然后批量处理
        _logTasks = [NSMutableArray new];
        // 设置异步日志开关
        _asyncLogEnable = [[[self class] logHandler] asyncLogEnable];
    }
    return self;
}

/*
 * @brief 注册自定义log实现（只能注册一次，重复调用会被忽略）
 */
+ (void)registerLogHandler:(id<KuiklyLogProtocol>)logHandler {
    static dispatch_once_t registerOnceToken;
    dispatch_once(&registerOnceToken, ^{
        gLogUserSuppliedHandler = logHandler;
    });
}

+ (id<KuiklyLogProtocol>)logHandler {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        gLogHandler = [KuiklyLogHandler new];
    });
    
    // 优先使用用户赋值的 handler
    id<KuiklyLogProtocol> handler = gLogUserSuppliedHandler ?: gLogHandler;
    
    return handler;
}

- (void)logInfo:(NSDictionary *)args {
    NSString *message = args[KR_PARAM_KEY];
    if (_asyncLogEnable) {
        NSString *logTime = [self logTimeDate];
        [self addLogTask:^{
            [[KRLogModule logHandler] logInfo:[NSString stringWithFormat:@"|%@|%@", logTime, message]];
        }];
    } else {
        [[KRLogModule logHandler] logInfo:message];
    }
    
   
}

- (void)logDebug:(NSDictionary *)args {
    NSString *message = args[KR_PARAM_KEY];
    if (_asyncLogEnable) {
        NSString *logTime = [self logTimeDate];
        [self addLogTask:^{
            [[KRLogModule logHandler] logDebug:[NSString stringWithFormat:@"|%@|%@", logTime, message]];
        }];
    } else {
        [[KRLogModule logHandler] logDebug:message];
    }
}

- (void)logError:(NSDictionary *)args {
    NSString *message = args[KR_PARAM_KEY];
    if (_asyncLogEnable) {
        NSString *logTime = [self logTimeDate];
        [self addLogTask:^{
            [[KRLogModule logHandler] logError:[NSString stringWithFormat:@"|%@|%@", logTime, message]];
        }];
    } else {
        [[KRLogModule logHandler] logError:message];
    }
}

#pragma mark - public

+ (void)logError:(NSString *)errorLog {
    NSString *message = [NSString stringWithFormat:@"[kuikly error]%@", errorLog];
    NSLog(@"%@", message);
    [[KRLogModule logHandler] logError:message]; // 日志落入接入层体系中
#if DEBUG
    [KRConvertUtil hr_alertWithTitle:@"kuikly error" message:message]; // 本地开发可视化提醒
#endif
}

+ (void)logInfo:(NSString *)infoLog {
    [[KRLogModule logHandler] logInfo:infoLog]; // 日志落入接入层体系中
}


#pragma mark - private
  
- (void)addLogTask:(dispatch_block_t)task {
    assert([KuiklyRenderThreadManager isContextQueue]);
    if (!task) {
        return ;
    }
    [_logTasks addObject:task];
    [self setNeedSyncLogTasks];
}

- (void)setNeedSyncLogTasks {
    if (!_needSyncLogTasks) {
        _needSyncLogTasks = YES;
        [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
            // 先复制任务并清空数组
            NSArray *tasks = [self.logTasks copy];
            self.logTasks = [NSMutableArray new];
            // 重置标志位，允许新的批次开始
            self.needSyncLogTasks = NO;
            // 异步执行日志任务
            [KuiklyRenderThreadManager performOnLogQueueWithBlock:^{
                for (dispatch_block_t task in tasks) {
                    task();
                }
            }];
        }];
    }
}

- (NSString *)logTimeDate {
    NSDate *date = [NSDate date];
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"HH:mm.ss.SSS"];
    return [formatter stringFromDate:date];
}

@end



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

#import "KRFontWarmupManager.h"
#import <pthread.h>

static NSString * const kKRFontWarmupKeyFormat = @"%@|%.2f";

@interface KRFontWarmupManager ()
@property (nonatomic, strong) NSMutableSet<NSString *> *warmedKeys;
@property (nonatomic, strong) NSMutableSet<UIFont *> *residentFonts;
@property (nonatomic, strong) dispatch_queue_t lockQueue;
@end

@implementation KRFontWarmupManager

+ (instancetype)sharedManager {
    static KRFontWarmupManager *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{    
        instance = [[self alloc] init];
    });
    return instance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _warmedKeys = [NSMutableSet set];
        _residentFonts = [NSMutableSet set];
        _lockQueue = dispatch_queue_create("com.tencent.kuikly.fontwarmup.lock", DISPATCH_QUEUE_SERIAL);
#if !TARGET_OS_OSX
        // 收到内存告警时清空预热缓存，释放对 UIFont 的强引用，避免 residentFonts 只增不减。
        // 内存告警通知为 UIKit 专有，macOS 无此机制，故条件编译隔离。
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(p_onMemoryWarning)
                                                     name:UIApplicationDidReceiveMemoryWarningNotification
                                                   object:nil];
#endif
    }
    return self;
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)p_onMemoryWarning {
    // invalidateAllFonts 限定主线程访问，这里统一派发到主线程执行。
    dispatch_async(dispatch_get_main_queue(), ^{
        [self invalidateAllFonts];
    });
}

+ (NSString *)keyForFont:(UIFont *)font {
    return [NSString stringWithFormat:kKRFontWarmupKeyFormat, font.fontName, font.pointSize];
}

- (BOOL)p_isWarmed:(UIFont *)font {
    __block BOOL warmed = NO;
    dispatch_sync(self.lockQueue, ^{
        warmed = [self.warmedKeys containsObject:[self.class keyForFont:font]];
    });
    return warmed;
}

- (void)p_markWarmed:(UIFont *)font {
    dispatch_barrier_async(self.lockQueue, ^{
        [self.warmedKeys addObject:[self.class keyForFont:font]];
        [self.residentFonts addObject:font];
    });
}

- (void)warmupFonts:(NSArray<UIFont *> *)fonts {
    NSAssert(pthread_main_np() != 0, @"warmupFonts: must be called on main thread");
    if (!fonts.count) {
        return;
    }
    
    static NSString *warmupText = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        warmupText = @"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz中文符号↑↓←→↗↘😀🙂😎📈📉$€£¥%&*()_+-=[]{}|;':\"\\,./<>?";
    });
    
    for (UIFont *font in fonts) {
        if (![font isKindOfClass:[UIFont class]]) {
            continue;
        }
        if ([self p_isWarmed:font]) {
            continue;
        }
        
        NSAttributedString *attr = [[NSAttributedString alloc] initWithString:warmupText
                                                                   attributes:@{NSFontAttributeName: font}];
        @autoreleasepool {
            NSTextStorage *storage = [[NSTextStorage alloc] initWithAttributedString:attr];
            (void)storage;
        }
        [self p_markWarmed:font];
    }
}

- (void)warmupFontsAsync:(NSArray<UIFont *> *)fonts {
    if (!fonts.count) {
        return;
    }
    // UIFont: 是 Context 线程的高频测量路径，这里先用线程安全的 p_isWarmed: 过滤掉已预热字体，
    // 避免即便字体早已预热仍每次都向主队列投递 async block。
    NSMutableArray<UIFont *> *pending = nil;
    for (UIFont *font in fonts) {
        if (![font isKindOfClass:[UIFont class]]) {
            continue;
        }
        if ([self p_isWarmed:font]) {
            continue;
        }
        if (!pending) {
            pending = [NSMutableArray array];
        }
        [pending addObject:font];
    }
    if (!pending.count) {
        return;
    }
    NSArray<UIFont *> *copy = [pending copy];
    dispatch_async(dispatch_get_main_queue(), ^{
        [self warmupFonts:copy];
    });
}

- (void)invalidateFont:(UIFont *)font {
    NSAssert(pthread_main_np() != 0, @"invalidateFont: must be called on main thread");
    if (![font isKindOfClass:[UIFont class]]) {
        return;
    }
    dispatch_barrier_async(self.lockQueue, ^{
        [self.warmedKeys removeObject:[self.class keyForFont:font]];
        [self.residentFonts removeObject:font];
    });
}

- (void)invalidateAllFonts {
    NSAssert(pthread_main_np() != 0, @"invalidateAllFonts must be called on main thread");
    dispatch_barrier_async(self.lockQueue, ^{
        [self.warmedKeys removeAllObjects];
        [self.residentFonts removeAllObjects];
    });
}

@end

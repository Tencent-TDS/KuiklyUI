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

#import "KRAsyncDeallocManager.h"

@interface KRAsyncDeallocManager()

@property (nullable, nonatomic, strong) NSMutableArray * deallocObjects;

@property (nonatomic, assign) BOOL _setNeedDeallocObjects;



@end

@implementation KRAsyncDeallocManager

+ (instancetype)shareManager{
    static KRAsyncDeallocManager * instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[KRAsyncDeallocManager alloc] init];
    });
    return instance;
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        _deallocObjects = [NSMutableArray new];
    }
    return self;
}

- (void)asyncDeallocWithObject:(id)deallocObject{
    [self _asyncDeallocOnDefaultGlobalQueueWithObject:deallocObject];
}


#pragma mark - private

- (void)_asyncDeallocOnDefaultGlobalQueueWithObject:(id)deallocObject{
    if (deallocObject == nil) {
        return ;
    }
    if ([NSThread isMainThread]) {
        [_deallocObjects addObject:deallocObject];
        if (__setNeedDeallocObjects == NO) {
            __setNeedDeallocObjects = YES;
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)),dispatch_get_main_queue(), ^{
                NSMutableArray * objects = self.deallocObjects;
                self.deallocObjects = [NSMutableArray new];
                self._setNeedDeallocObjects = NO;
                dispatch_async(dispatch_get_global_queue(0, 0), ^{
                    [objects removeAllObjects];
                });
            });
        }
    }else {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self _asyncDeallocOnDefaultGlobalQueueWithObject:deallocObject];
        });
    }
}




#pragma mark - getter


@end

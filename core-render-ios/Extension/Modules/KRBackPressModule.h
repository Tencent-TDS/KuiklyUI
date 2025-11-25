//
//  KRBackPressModule.h
//  OpenKuiklyIOSRender
//
//  Created by xubin on 2025/11/24.
//

#import "KRBaseModule.h"

NS_ASSUME_NONNULL_BEGIN

/**
 * 监听 BackPress 消费状态回调
 */
@interface KRBackPressModule : KRBaseModule

/// 返回键是否被消费
@property (nonatomic, assign) BOOL isBackConsumed;
///返回键消费时间戳
@property (nonatomic, assign) NSTimeInterval backConsumedTime;

@end

NS_ASSUME_NONNULL_END

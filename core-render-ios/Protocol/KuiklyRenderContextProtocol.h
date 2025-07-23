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

#ifndef KuiklyRenderContextProtocol_h
#define KuiklyRenderContextProtocol_h
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class KuiklyBaseContextMode;
@class KuiklyContextParam;

typedef NSInteger KuiklyContextMode;
typedef void(^OnUnhandledExceptionBlock)(NSString *exReason, NSString *callstackStr, KuiklyContextMode mode);

/*
 * Native侧调用Kotlin侧方法枚举
 */
typedef NS_ENUM(NSUInteger, KuiklyRenderContextMethod) {
    KuiklyRenderContextMethodUnknown = 0,
    KuiklyRenderContextMethodCreateInstance = 1, /// "createInstance" 方法
    KuiklyRenderContextMethodUpdateInstance = 2, /// "updateInstance" 方法
    KuiklyRenderContextMethodDestroyInstance = 3, /// "updateInstance" 方法
    KuiklyRenderContextMethodFireCallback = 4, /// "fireCallback" 方法
    KuiklyRenderContextMethodFireViewEvent = 5, /// "fireViewEvent" 方法
    KuiklyRenderContextMethodLayoutView = 6, /// "layoutView" 方法
};

/*
 * Kotlin侧调用Native侧方法枚举
 */
typedef NS_ENUM(NSUInteger, KuiklyRenderNativeMethod) {
    KuiklyRenderNativeMethodUnknown = 0,
    KuiklyRenderNativeMethodCreateRenderView = 1, /// "createRenderView" 方法
    KuiklyRenderNativeMethodRemoveRenderView = 2, /// "removeRenderView" 方法
    KuiklyRenderNativeMethodInsertSubRenderView = 3, /// "insertSubRenderView" 方法
    KuiklyRenderNativeMethodSetViewProp = 4, /// "setViewProp" 方法
    KuiklyRenderNativeMethodSetRenderViewFrame = 5, /// "setRenderViewFrame" 方法
    KuiklyRenderNativeMethodCalculateRenderViewSize = 6, /// "calculateRenderViewSize" 方法
    KuiklyRenderNativeMethodCallViewMethod = 7, /// "callViewMethod" 方法
    KuiklyRenderNativeMethodCallModuleMethod = 8, /// "callModuleMethod" 方法
    KuiklyRenderNativeMethodCreateShadow = 9, /// "createShadow" 方法
    KuiklyRenderNativeMethodRemoveShadow = 10, /// "removeShadow" 方法
    KuiklyRenderNativeMethodSetShadowProp = 11, /// "setShadowProp" 方法
    KuiklyRenderNativeMethodSetShadowForView = 12, /// "setShadowForView" 方法
    KuiklyRenderNativeMethodSetTimeout = 13, /// "setTimeout方法"
    KuiklyRenderNativeMethodCallShadowMethod = 14, /// "callShadowMethod" 方法
    KuiklyRenderNativeMethodFireFatalException = 15, /// "fireFatalException" 方法
    KuiklyRenderNativeMethodSyncFlushUI = 16, /// "syncFlushUI" 方法
    KuiklyRenderNativeMethodCallTDFModuleMethod = 17,   /// "callTDFModuleMethod" 方法
};


typedef id _Nullable (^KuiklyRenderNativeMethodCallback)(KuiklyRenderNativeMethod method, NSArray *args);

/*
 * @brief 渲染对应的逻辑代码可执行环境协议
 */
@protocol KuiklyRenderContextProtocol <NSObject>

@optional

/*
 * @brief 设置异常处理函数
 * @param onExceptionBlock 异常处理函数闭包
 */
- (void)setOnExceptionBlock:(OnUnhandledExceptionBlock)onExceptionBlock;

@required
/*
 * @brief 是否等待销毁中（该字段可用于中断context线程队列中的任务）
 */
@property (atomic, assign) BOOL isDestroying;

/*
 * @brief 初始化Kotlin侧代码执行环境
 * @param contextCode 环境代码
 * @param contextParam 初始化相关参数。
 * @return 返回context实例
 */
- (instancetype)initWithContext:(NSString * _Nullable)contextCode
                   contextParam:(KuiklyContextParam * _Nullable)contextParam;
/*
 * @brief Native侧调用Kotlin侧方法接口
 * @param method Kotlin侧的方法
 * @param args 方法参数
 */
- (void)callWithMethod:(KuiklyRenderContextMethod)method args:(NSArray *)args;
/*
 * @brief Kotlin侧用Native侧方法时接口回调
 * @param callback kotlin侧调用native侧方法时回调闭包
 */
- (void)registerCallNativeWtihCallback:(KuiklyRenderNativeMethodCallback)callback;
/*
 * @brief 即将销毁该实例
 */
- (void)willDestroy;

@end


NS_ASSUME_NONNULL_END
#endif /* KuiklyRenderContextProtocol_h */

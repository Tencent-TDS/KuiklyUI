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

#ifndef TDFModuleProtocol_h
#define TDFModuleProtocol_h

@class UIView;

typedef void (^TDFModuleSuccessCallback)(id _Nullable result);
typedef void (^TDFModuleErrorCallback)(NSString * _Nullable code, NSString * _Nullable message, NSError * _Nullable error);

typedef NS_ENUM(NSUInteger, TDF_BRIDGE_TYPE) {
    TDF_BRIDGE_TYPE_KUIKLY = 1,
    TDF_BRIDGE_TYPE_HIPPY = 2,
};

typedef NS_ENUM(NSInteger, TDFNullability) {
  TDFNullabilityUnspecified,
  TDFNullable,
  TDFNonnullable,
};

@protocol TDFModuleProtocol;
@protocol TDFBridgeDelegate <NSObject>

/*
 * @brief 当前所在根视图
 */
@property (nonatomic, weak, readonly) UIView * _Nullable rootView;

/*
 * @brief 当前的PageName
 */
@property (nonatomic, copy, readonly) NSString * _Nullable pageName;

/*
 * @brief 当前module对象，处于哪个容器框架的类型
 */
@property (nonatomic, assign, readonly) TDF_BRIDGE_TYPE bridgeType;

/*
 * @brief hippy的bridge对象，在kuikly框架运行时为空
 */
@property (nonatomic, weak, readonly) id _Nullable hippyBridge;

/*
 * @brief 通过KuiklyRenderView发送事件到KuiklyKotlin侧（支持多线程调用, 非主线程时为同步通信，建议主线程调用）.
 *        注：kotlin侧通过pager的addPagerEventObserver方法监听接收
 * @param event 事件名
 * @param data 事件对应的参数
 */
- (void)sendWithEvent:(NSString *_Nonnull)event data:(NSDictionary *_Nullable)data;

/*
 * @brief 获取模块对应的实例（仅支持在主线程调用）.
 * @param moduleName 模块名
 * @return module实例
 */
- (id _Nullable)moduleWithName:(NSString *_Nonnull)moduleName;

/*
 * @brief 获取kotlin/js侧tag对应的Native View实例（仅支持在主线程调用）.
 * @param tag view对应的索引
 * @return view实例
 */
- (UIView * _Nullable)viewWithTag:(NSInteger)tag;

/*
 * @brief 通过callback ID 回调到框架
 * @param callbackId
 * @param params 参数
 */
- (void)performCallback:(NSNumber *_Nonnull)callbackId params:(id _Nonnull)params;

@end


@protocol TDFModuleProtocol <NSObject>
@optional

/*
 * @brief methodQueue 当前Module的异步方法调用，框架会自动切换到该线程执行;同步方法默认在kotlin/js线程执行；
 * 如果返回空，kuikly的异步调用默认会切到主线程，hippy的异步调用，默认会切到dom线程
 */
+ (dispatch_queue_t _Nullable )methodQueue;

/*
 * @brief isSync 当前方法是否同步调用，仅对hippy框架生效；kuikly需要同步调用，请在kotlin侧控制
 */
+ (BOOL)isSync_Hippy;

/*
 * @brief invalidate module销毁前调用
 */
- (void)invalidate;

// Implemented by HIPPY_EXPORT_MODULE
+ (NSString *_Nonnull)moduleName;

@end


#pragma mark - marco

#if defined(__cplusplus)
#define TDF_EXTERN extern "C" __attribute__((visibility("default")))
#define TDF_EXTERN_C_BEGIN extern "C" {
#define TDF_EXTERN_C_END }
#else
#define TDF_EXTERN extern __attribute__((visibility("default")))
#define TDF_EXTERN_C_BEGIN
#define TDF_EXTERN_C_END
#endif

/*
 * brief 获取tdfModule的Class
 */
TDF_EXTERN Class _Nullable TDGGetModuleClass(NSString * _Nonnull name);

/*
 * brief 导出module名称
 */
#define TDF_EXPORT_MODULE(name)              \
    TDF_EXTERN void TDFRegisterModule(Class); \
    +(NSString *)moduleName {                     \
        return @ #name;                        \
    }                                             \
    +(void)load {                                 \
        TDFRegisterModule(self);                \
    }


// methodInfo
TDF_EXTERN_C_BEGIN
typedef struct TDFMethodInfo {
    const char * _Nullable const outName;
    const char * _Nonnull const objcName;
} TDFMethodInfo;
TDF_EXTERN_C_END

/**
 * brief 导出方法. kotlin/js侧 会取第一个冒号前字符串作为方法名
 * For example, in ModuleName.m:
 *
 * - (void)doSomething:(NSString *)aString withA:(NSInteger)a andB:(NSInteger)b
 * { ... }
 *
 * becomes
 *
 * TDF_EXPORT_METHOD(doSomething:(NSString *)aString
 *                   withA:(NSInteger)a
 *                   andB:(NSInteger)b)
 * { ... }
 *
 * and is exposed  as `NativeModules.ModuleName.doSomething`.
 */
#define TDF_EXPORT_METHOD(method)               \
        TDF_REMAP_METHOD_FOR_HIPPY(, method)    \
        TDF_REMAP_METHOD(, method)              \
        -(id)method;

#define TDF_CONCAT2(A, B) A##B
#define TDF_CONCAT(A, B) TDF_CONCAT2(A, B)

/**
 * Similar to TDF_EXPORT_METHOD but lets you set the JS/Kotlin name of the exported
 * method. Example usage:
 *
 * TDF_REMAP_METHOD(executeQueryWithParameters,
 *   executeQuery:(NSString *)query parameters:(NSDictionary *)parameters)
 * { ... }
 */
#define TDF_REMAP_METHOD(name, method)       \
  +(const TDFMethodInfo *)TDF_CONCAT(__tdf_export__, TDF_CONCAT(name, TDF_CONCAT(__LINE__, __COUNTER__)))    \
  {                                                                                                          \
    static TDFMethodInfo config = {#name, #method};                                                          \
    return &config;                                                                                          \
  }                                                                                                          \


/**
 * Exports method for Hippy, allows setting a custom JavaScript name.
 */
#define TDF_REMAP_METHOD_FOR_HIPPY(js_name, method) \
+(NSArray<NSString *> *)TDF_CONCAT(__hippy_export__, TDF_CONCAT(js_name, TDF_CONCAT(__LINE__, __COUNTER__))) { \
    NSString *sig = @ #method; \
    sig = [sig stringByReplacingOccurrencesOfString:@"TDFModuleSuccessCallback" withString:@"HippyPromiseResolveBlock"]; \
    sig = [sig stringByReplacingOccurrencesOfString:@"TDFModuleErrorCallback" withString:@"HippyPromiseRejectBlock"]; \
    return @[@ #js_name, sig]; \
}


#endif /* TDFModuleProtocol_h */

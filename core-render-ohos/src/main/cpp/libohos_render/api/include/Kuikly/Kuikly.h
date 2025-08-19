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

#ifndef CORE_RENDER_OHOS_KUIKLY_H
#define CORE_RENDER_OHOS_KUIKLY_H

#include <stdint.h>
#include <stddef.h>
#include <arkui/drawable_descriptor.h>
#include <arkui/native_type.h>

#include "KRAnyData.h"

#ifdef __cplusplus
extern "C" {
#endif


struct KRRenderModuleCallbackContextData;
/**
 * 回调上下文对象，KRRenderModuleCallMethod的时候传给业务，
 * 业务在调用KRRenderModuleDoCallback的时候回传给框架，
 * 以进一步回传给kotlin业务逻辑
 */
typedef struct KRRenderModuleCallbackContextData *KRRenderModuleCallbackContext;

/**
 * 回调给Kotlin侧的闭包
 * @param data 数据(类型为String)
 */
void KRRenderModuleDoCallback(KRRenderModuleCallbackContext context, const char *data);

/**
 * 从回调上下文对象根据tag获取capi的handle
 * @param tag
 * @return ArkUI_NodeHandle
 */
ArkUI_NodeHandle KRRenderModuleGetViewWithTag(KRRenderModuleCallbackContext context, int tag);

/**
 * The type of a funtion to free an object
 * @param 
 */
typedef void (*KRFreeFunc)(void*);

/**
 * Module的CallMethod调用的返回值
 * @field res   字符串数据指针
 * @field length 字符串实际长度
 * @field free  资源释放回调函数
 */
struct KRCallMethodCValue {
    const char* res;
    int length;
    KRFreeFunc free;
};

/**
 * Module的CallMethod调用
 * @param moduleInstance 模块实例，这是KRRenderModuleOnConstruct的返回值
 * @param moduleName 模块实例，这是KRRenderModuleOnConstruct的返回值
 * @param sync bool 是否同步
 * @param method 调用的模块方法
 * @param context 回调的上下文，可为nullptr，有值的时候业务可通过KRRenderModuleDoCallback回调数据给kotlin调用方
 * @return KRCallMethodCValue
 */
typedef KRCallMethodCValue (*KRRenderModuleCallMethod)(const void* moduleInstance, const char* moduleName, int sync, const char *method, KRAnyData param,
                                                       KRRenderModuleCallbackContext context);

/**
 * Module的CallMethod调用(V2)
 * @param moduleInstance 模块实例，这是KRRenderModuleOnConstruct的返回值
 * @param moduleName 模块实例，这是KRRenderModuleOnConstruct的返回值
 * @param sync bool 是否同步
 * @param method 调用的模块方法
 * @param context 回调的上下文，可为nullptr，有值的时候业务可通过KRRenderModuleDoCallback回调数据给kotlin调用方
 * @return KRAnyData
 * @note 返回值KRAnyData由框架Destroy
 */
typedef KRAnyData (*KRRenderModuleCallMethodV2)(const void* moduleInstance, const char* moduleName, int sync, const char *method, KRAnyData param,
                                                       KRRenderModuleCallbackContext context);

/**
 * Module的OnConstruct调用，Module构造时，此方法会被调用。返回的指针在后续的调用中会作为moduleInstance回传。
 * @param moduleName 模块名字
 */
typedef void * (*KRRenderModuleOnConstruct)(const char *moduleName);

/**
 * Module的OnDestruct调用，Module析构时，此方法会被调用。
 * @param moduleInstance 模块实例，这是KRRenderModuleOnConstruct的返回值
 * 
 */
typedef void (*KRRenderModuleOnDestruct)(const void* moduleInstance);

/**
 * 注册自定义模块
 * @param moduleName 模块名称
 * @param onConstruct Module构造时调用的方法
 * @param onDestruct Module析构时调用的方法
 * @param onCallMethod 模块的call method实现
 * @param reserved 保留字段
 */
void KRRenderModuleRegister(const char *moduleName,
                            KRRenderModuleOnConstruct onConstruct,
                            KRRenderModuleOnDestruct  onDestruct,
                            KRRenderModuleCallMethod  onCallMethod,
                            void *reserved);

/**
 * 注册自定义模块(V2)
 * @param moduleName 模块名称
 * @param onConstruct Module构造时调用的方法
 * @param onDestruct Module析构时调用的方法
 * @param onCallMethod 模块的call method实现
 * @param reserved 保留字段
 */
void KRRenderModuleRegisterV2(const char *moduleName,
                            KRRenderModuleOnConstruct onConstruct,
                            KRRenderModuleOnDestruct  onDestruct,
                            KRRenderModuleCallMethodV2  onCallMethod,
                            void *reserved);

/**
 * 自定义属性handler
 * @param arkui_handle view对应的ohos capi的handle，类型为ArkUI_NodeHandle
 * @param propKey 属性名称
 * @param propValue 属性值
 */
typedef bool (*KRRenderViewOnSetProp)(void *arkui_handle, const char *propKey, KRAnyData propValue);

/**
 * 自定义属性重置handler
 * @param arkui_handle view对应的ohos capi的handle，类型为ArkUI_NodeHandle
 * @param propKey 属性名称
 */
typedef bool (*KRRenderViewOnResetProp)(void *arkui_handle, const char *propKey);

/**
 * 设置自定义属性handler
 * @param handler
 */
void KRRenderViewSetExternalPropHandler(KRRenderViewOnSetProp set, KRRenderViewOnResetProp reset);

/**
 * @brief 一个用于析构KRFontAdapterFontBufferForFamily返回的fontBuffer的回调函数
 * @param fontBuffer
 * @param len
 */
typedef void (*KRFontDataDeallocator)(char *data);

/**
 * @brief 根据fontFamily返回font family src或 font buffer.
 *        模式1：返回font src，这种情况输入是fontFamily，deallocator, 输出是函数返回值
 *        模式2: 通过参数指针返回font buffer，这种情况输入是fontFamily，输出是fontBuffer，len，deallocator
 * @param fontFamily
 * @param[out] fontBuffer 用于返回font buffer指针
 * @param[out] len 返回的font buffer长度
 * @return font family
 */
typedef char *(*KRFontAdapter)(const char *fontFamily, char **fontBuffer, size_t *len,
                               KRFontDataDeallocator *deallocator);

/**
 * @brief 注册font adapter
 * @param adapter adapter函数指针
 * @param fontFamily adapter对应的font family
 */
void KRRegisterFontAdapter(KRFontAdapter adapter, const char *fontFamily);

/**
 * @brief 一个用于析构KRImageAdapter返回的imageDescriptor或src的回调函数
 * @param imageDescriptor或src
 */
typedef void (*KRImageDataDeallocator)(void *data);

/**
 * @brief 根据imageScr返回image src或image descriptor
 *        模式1：返回image src，这种情况输入是imageSrc, 输出是函数返回值，deallocator
 *        模式2: 通过参数指针返回image descriptor，这种情况输入是imageSrc，输出是imageDescriptor，deallocator
 * @param imageSrc image组件设置的scr属性
 * @param[out] imageDescriptor 用于返回image的drawable descriptor
 * @param[out] deallocator 用于释放imageDescriptor指针或src，如果是非空则kuikly使用完imageDescriptor或image
 * src后会用调用deallocator
 * @return image src
 */
typedef char *(*KRImageAdapter)(const char *imageSrc, ArkUI_DrawableDescriptor **imageDescriptor,
                                KRImageDataDeallocator *deallocator);

/**
 * @brief 注册image adapter
 * @param adapter adapter函数指针
 */
void KRRegisterImageAdapter(KRImageAdapter adapter);

/**
 * Log level定义
 * 判断loglevel的时候，不要假设每个level的值是永远固定的，请通过常量对比进行判断
 */
extern int KRLogLevelInfo;
extern int KRLogLevelDebug;
extern int KRLogLevelError;

/**
 * Log Adapter回调
 * @param logLevel
 * @param tag
 * @param message
 * @return
 */
typedef void (*KRLogAdapter)(int logLevel, const char *tag, const char *message);

/**
 * 注册c实现的log adapter，进程声明周期中，只应调用一次，建议在初始化阶段（如调用initKuikly前）进行调用。
 * example:
 * 1. Implement the adapter
 * void MyLogAdapter(int logLevel, const char* tag, const char* message){
 *     static int MyDomain = 0x1234;
 *     OH_LOG_Print(LOG_APP, LOG_INFO, MyDomain, tag, "%{public}s", message.c_str());
 * }
 *
 * 2. Register before calling initKuikly
 * if(!registerd){// e.g. register could a static variable
 *     KRRegisterLogAdapter(&MyLogAdapter);
 * }
 *
 * @param adapter
 */
void KRRegisterLogAdapter(KRLogAdapter adapter);


/**
 * Color Adapter回调
 */
typedef int64_t (*KRColorAdapterParseColor)(const char* str);

/**
 * 注册c实现的颜色解析adapter，进程声明周期中，只应调用一次，建议在初始化阶段（如调用initKuikly前）进行调用。
 * example:
 * 1. Implement the adapter
 * static uint32_t MyColorParser(const char* str){
 *     uint32_t val = 0;
 *     ... parse from str ...
 *     return val;
 * }
 * 
 * 2. Register before calling initKuikly
 * if(!registerd){// e.g. register could a static variable
 *     KRRegisterColorAdapter(&MyColorParser);
 * }
 *
 */
void KRRegisterColorAdapter(KRColorAdapterParseColor adapter);

/**
 * 禁止view复用。
 * 这是一个临时API，后续会删除，未经沟通，请勿调用。
 */
void KRDisableViewReuse();

#ifdef __cplusplus
}
#endif

#endif  // CORE_RENDER_OHOS_KUIKLY_H

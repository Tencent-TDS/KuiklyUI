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
 * 从回调上下文对象获取当前页面的InstanceID
 * @param context 回调上下文
 * @return 字符串指针，仅当前scope有效，请勿转移指针，如有需要请拷贝字符串内容。
 */
const char* KRRenderModuleGetInstanceID(KRRenderModuleCallbackContext context);

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
 * @brief 业务图片加载完成后，用于回调给kuikly的函数指针
 * @param context 上下文
 * @param src src image组件设置的src属性
 * @param image_descriptor 解码好的图片
 * @param new_src 新的src地址，比如从原src映射到一个新的src路径
 * @discuss 当image_descriptor非空时，kuikly优先用image_descriptor，其次再使用new_src
 */
typedef void (*KRSetImageCallback)(const void* context,
                                   const char *src,
                                   ArkUI_DrawableDescriptor *image_descriptor,
                                   const char *new_src);
/**
 * @brief 自定义image adapter
 * @param context 上下文
 * @param src image组件设置的src属性
 * @param callback 自定义加载图片完成后可通过callback指针回调给kuikly，并把context以及src参数回填
 * @return 已处理则返回1，否则返回0
 */
typedef int32_t (*KRImageAdapterV2)(const void *context,
                                 const char *src,
                                 KRSetImageCallback callback);

/**
 * @brief 注册image adapter
 * @param adapter adapter函数指针
 */
[[deprecated("Use KRRegisterImageAdapterV2(KRImageAdapterV2) instead.")]]
void KRRegisterImageAdapter(KRImageAdapter adapter);

/**
 * @brief 注册image adapter V2
 * @param adapter adapter函数指针
 */
void KRRegisterImageAdapterV2(KRImageAdapterV2 adapter);

/**
 * @brief 自定义image adapter V3，支持传递图片加载参数
 * @param context 上下文
 * @param src image组件设置的src属性
 * @param imageParams 图片加载参数（Map类型），可通过 KRAnyData 系列API获取内容，可能为 nullptr
 * @param callback 自定义加载图片完成后可通过callback指针回调给kuikly，并把context以及src参数回填
 * @return 已处理则返回1，否则返回0
 */
typedef int32_t (*KRImageAdapterV3)(const void *context,
                                    const char *src,
                                    KRAnyData imageParams,
                                    KRSetImageCallback callback);

/**
 * @brief 注册image adapter V3
 * @param adapter adapter函数指针
 * @note V3版本支持传递图片加载参数 imageParams（Map类型）
 */
void KRRegisterImageAdapterV3(KRImageAdapterV3 adapter);


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

/**
 * 启用新的文本渲染能力。
 * 这是一个临时API，后续会删除，未经沟通，请勿调用。
 */
void KREnableTextRenderV2();

/* ============ Text Post Processor Adapter ============
 *
 * 用途：在 SDK 把文本写入 ARKUI_NODE_TEXT_EDITOR / RichText 等组件之前，给业务一次
 *      对原始文本的预处理机会，最常见用法是把诸如 "[smile]" 这类自定义短码替换为
 *      ImageSpan（emoji / 自定义贴纸）。与 Android `IKRTextPostProcessorAdapter` /
 *      iOS `hr_customTextWithAttributedString:textPostProcessor:` 在概念上对齐。
 *
 * 模型：
 *   原始文本(string) -> [Text/Image Span 序列] -> SDK 内部组装 StyledString
 *
 *   业务侧通过 KRTextProcessedResultBuilder 句柄按顺序追加 Text/Image Span，
 *   SDK 会按追加顺序拼接为最终 StyledString。
 *
 * 内存所有权（关键）：
 *   - text 入参与 builder 句柄仅在回调期间有效，回调返回后 SDK 立即回收；
 *   - 业务调用 AppendTextSpan/AppendImageSpan 时传入的字符串会被 SDK 立即拷贝，
 *     调用返回后业务可任意释放/复用自己的缓冲区；
 *   - 业务从不分配也从不释放任何 SDK 内部数据结构。
 */

/**
 * 业务用来累积本次处理结果的"构建器"句柄（不透明）。
 * 仅在 KRTextPostProcessorAdapter 回调期间有效，不可跨线程/异步保存。
 */
struct KRTextProcessedResultBuilder_;
typedef struct KRTextProcessedResultBuilder_ *KRTextProcessedResultBuilder;

/**
 * 追加一段纯文本 span。
 * @param builder builder 句柄
 * @param text    UTF-8 文本，SDK 内部立即拷贝；NULL 视为忽略
 */
void KRTextProcessedResultAppendTextSpan(KRTextProcessedResultBuilder builder,
                                         const char *text);

/**
 * 追加一段图片 span。
 * @param builder builder 句柄
 * @param src     **可寻址 URI**，仅支持以下协议（业务侧需自行解析为下列形态）：
 *                  - "file://..."（推荐：可由 hap rawfile 复制到沙盒后给出）
 *                  - "http://..." / "https://..."
 *                  - "data:image/...;base64,..."
 *                业务在此处不应再使用任何 SDK 私有协议（如 "assets://"）。
 *                SDK 内部立即拷贝；NULL/空字符串视为忽略
 * @param width   图片宽度（vp），<=0 表示按当前字号自适应
 * @param height  图片高度（vp），<=0 表示按当前字号自适应
 *
 * @note 该接口未传 raw_literal（image 在原始文本中对应的字面量），SDK 在
 *       「输入框编辑后差分回写 raw」场景下无法精准还原该 image 段——表现为：
 *       用户键入纯文本时 image 仍存活，但 raw 文本中 image 位置会以单空格代替，
 *       上抛给业务的 textDidChange/state.text 会带来 shortcode 丢失。
 *       *推荐* 在输入框场景下改用 KRTextProcessedResultAppendImageSpanWithRaw。
 */
void KRTextProcessedResultAppendImageSpan(KRTextProcessedResultBuilder builder,
                                          const char *src,
                                          float width,
                                          float height);

/**
 * 追加一段图片 span（携带 raw 字面量；推荐用于输入框场景）。
 *
 * 与 KRTextProcessedResultAppendImageSpan 的差异：本版本要求业务额外传入
 * raw_literal —— 即「image 在原始文本里的字面量」（例如 "[smile]"），让 SDK 在
 * 输入框编辑差分回写阶段能基于权威映射把 ArkUI 内部的占位空格还原回原始字面量，
 * shortcode 完全不丢失。
 *
 * 与 iOS `KRTextAttachmentStringProtocol::kr_originlTextBeforeTextAttachment` 在
 * 职责边界上对齐：image attachment 自身携带「raw 字面量」是最健壮的方案，避免
 * SDK 侧再做正则 / find 反推。
 *
 * @param builder      builder 句柄
 * @param src          可寻址 URI，约束同 KRTextProcessedResultAppendImageSpan
 * @param raw_literal  image 在原始文本中的字面量（UTF-8）。SDK 内部立即拷贝；
 *                     NULL / 空字符串等价调用 KRTextProcessedResultAppendImageSpan
 *                     （即 SDK 在编辑后无法精准还原该 image 段）
 * @param width        图片宽度（vp），<=0 表示按当前字号自适应
 * @param height       图片高度（vp），<=0 表示按当前字号自适应
 */
void KRTextProcessedResultAppendImageSpanWithRaw(KRTextProcessedResultBuilder builder,
                                                 const char *src,
                                                 const char *raw_literal,
                                                 float width,
                                                 float height);

/**
 * 文本预处理 adapter 函数签名。
 *
 * 业务侧约定：
 *   * 不调用任何 Append   -> 视为"未处理"，SDK 退回原始文本路径；
 *   * 至少调用一次 Append -> 视为"已处理"，SDK 完全按 builder 中的 Span 序列渲染。
 *
 * @param name     当前派发的 processor 名称（UTF-8）。
 *                 例如：编辑态 / 输入框通常传 "input"；只读文本可传 DSL 中设置的名称。
 *                 仅本次回调期间有效；当当前路径未显式指定 processor 时可能为 NULL。
 * @param text     原始文本（UTF-8），仅本次回调期间有效
 * @param reserved 保留参数（当前永远为 NULL，未来用于扩展上下文）
 * @param builder  本次处理的结果构建器，仅本次回调期间有效
 */
typedef void (*KRTextPostProcessorAdapter)(const char *name,
                                           const char *text,
                                           void *reserved,
                                           KRTextProcessedResultBuilder builder);

/**
 * 注册文本预处理 adapter（覆盖式）。
 *
 * SDK 内部仍会根据运行时 processor 名称进行分发，但业务侧只需注册一个统一 adapter。
 * 当前 processor 名称（如 "input" / "richtext"）会在回调时通过 `name` 参数回传给业务。
 *
 * 后注册覆盖前者；adapter 传 NULL 表示注销。
 *
 * @param adapter adapter 函数指针；NULL 表示注销
 */
void KRRegisterTextPostProcessorAdapter(KRTextPostProcessorAdapter adapter);


#ifdef __cplusplus
}
#endif

#endif  // CORE_RENDER_OHOS_KUIKLY_H

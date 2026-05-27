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

#include <ark_runtime/jsvm.h>
#include <dlfcn.h>
#include <hilog/log.h>
#include <multimedia/image_framework/image/image_source_native.h>
#include <multimedia/image_framework/image/pixelmap_native.h>
#include <rawfile/raw_file_manager.h>
#include <sys/stat.h>
#include <fstream>
#include <map>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include "libohos_render/api/include/Kuikly/Kuikly.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "napi/native_api.h"
#include "thirdparty/biz_entry/libshared_api.h"

static std::string customFontPath;
static std::string customImagePath;
static std::string g_resfile_dir;
static NativeResourceManager *g_resource_manager = nullptr;

static napi_threadsafe_function g_threadsafe_func = NULL;
struct ImageCallbackTask{
    ImageCallbackTask(const void* ctx,
                      const char *src,
                      ArkUI_DrawableDescriptor *image_descriptor,
                      KRSetImageCallback cb):context(ctx), src(src), imageDescriptor(image_descriptor), callback(cb){
        // blank
    }
    void run(){
        callback(context, src.c_str(), imageDescriptor, nullptr);
        OH_ArkUI_DrawableDescriptor_Dispose(imageDescriptor);
    }
    ArkUI_DrawableDescriptor *imageDescriptor;
    KRSetImageCallback callback;
    const void *context;
    std::string src;
};

static void threadsafe_func(napi_env env, napi_value js_fun, void *context, void *data) {
    struct ImageCallbackTask *task = (struct ImageCallbackTask *)data;
    if (task != nullptr) {
        task->run();
        delete task;
    }
}

static napi_value SetResourceManager(napi_env env, napi_callback_info info) {
    if (g_resource_manager) {
        return nullptr;
    }

    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    g_resource_manager = OH_ResourceManager_InitNativeResourceManager(env, args[0]);
    // Note: One should call OH_ResourceManager_ReleaseNativeResourceManager for the resource manager when it is no longer needed anymore,
    // for the simplicity of the demo, we just keep it around forever. 
    
    if (g_threadsafe_func == NULL) {
        napi_value work_name;
        napi_create_string_utf8(env, "Image callback", NAPI_AUTO_LENGTH, &work_name);
        napi_status status = napi_create_threadsafe_function(env, NULL, NULL, work_name, 0, 1, NULL, NULL, NULL,
                                                             threadsafe_func, &g_threadsafe_func);
        if (status != napi_ok) {
            napi_throw_error(env, "-1", "napi_create_threadsafe_function error");
            return nullptr;
        }
    }
    
    return nullptr;
}

static napi_value SetFontPath(napi_env env, napi_callback_info info) {
    if (customFontPath.size() > 0) {
        return nullptr;
    }

    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);

    size_t length = 0;
    napi_status status;
    status = napi_get_value_string_utf8(env, args[0], nullptr, 0, &length);
    std::string buffer(length, 0);
    status = napi_get_value_string_utf8(env, args[0], reinterpret_cast<char *>(buffer.data()), length + 1, &length);
    customFontPath = buffer;

    return nullptr;
}

// 业务侧 adapter 处理表情资源需要一个可寻址的文件路径。hap 中的 resfile
// （由 ohosApp/entry/hvigorfile.ts 的 kuiklyCopyAssets 从 demo/commonMain/assets 拷过去）
// 可以通过 context.resourceDir 调醒；SDK 仅接受 file:// URI，这里在业务侧拼接。
static napi_value SetResfileDir(napi_env env, napi_callback_info info) {
    if (!g_resfile_dir.empty()) {
        return nullptr;
    }
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    size_t length = 0;
    napi_get_value_string_utf8(env, args[0], nullptr, 0, &length);
    std::string buffer(length, 0);
    napi_get_value_string_utf8(env, args[0], reinterpret_cast<char *>(buffer.data()), length + 1, &length);
    g_resfile_dir = buffer;
    return nullptr;
}

static bool isEqual2(const char *str1, const char *str2) {
    if ((str1 == NULL && str2) || (str1 && str2 == NULL) || (str1 == NULL && str2 == NULL)) {
        return false;
    }
    if (std::strcmp(str1, str2) == 0) {
        return true;
    } else {
        return false;
    }
}

static bool isEqual(const std::string &str1, const char *str2) {
    return isEqual2(str1.c_str(), str2);
}

static char *MyFontAdapter(const char *fontFamily, char **fontBuffer, size_t *len, KRFontDataDeallocator *deallocator) {
    if (isEqual(fontFamily, "Satisfy-Regular")) {
        return "rawfile:Satisfy-Regular.ttf";
    }
    return (char *)customFontPath.c_str();
}

#define MyImageAdapterV2_SYNC_CALLBACK 1
    
int32_t MyImageAdapterV3(const void *context,
                                 const char *src,
                                 KRAnyData imageParams,
                                 KRSetImageCallback callback){
    // 获取imageParams,跨端侧传入的是：{"test":"abc"}
    std::map<std::string, std::string> paramsMap;
    // 方式1：使用 KRAnyDataVisitMap 遍历所有参数（推荐）
    if (imageParams != nullptr && KRAnyDataIsMap(imageParams)) {
        // 定义 lambda 作为访问器
        auto visitor = [](const char* key, KRAnyData value, void* userData) {
            auto* map = static_cast<std::map<std::string, std::string>*>(userData);
            // 根据类型转换成字符串存储
            if (KRAnyDataIsString(value)) {
                const char* str = KRAnyDataGetString(value);
                if (str) {
                    (*map)[key] = str;
                }
            } else if (KRAnyDataIsInt(value)) {
                int32_t intVal;
                KRAnyDataGetInt(value, &intVal);
                (*map)[key] = std::to_string(intVal);
            } else if (KRAnyDataIsLong(value)) {
                int64_t longVal;
                KRAnyDataGetLong(value, &longVal);
                (*map)[key] = std::to_string(longVal);
            } else if (KRAnyDataIsFloat(value)) {
                float floatVal;
                KRAnyDataGetFloat(value, &floatVal);
                (*map)[key] = std::to_string(floatVal);
            } else if (KRAnyDataIsBool(value)) {
                bool boolVal;
                KRAnyDataGetBool(value, &boolVal);
                (*map)[key] = boolVal ? "true" : "false";
            }
        };
        
        // 遍历所有键值对
        KRAnyDataVisitMap(imageParams, visitor, &paramsMap);
    }
    
    // 业务逻辑...
    if (paramsMap.count("test") > 0) {
        auto value = paramsMap["test"];
        KR_LOG_INFO << "imageParams testxxx value: " << value;
    }
    
    // 方式2：获取特定的参数值（如果只需要某个字段）
    if (imageParams != nullptr && KRAnyDataIsMap(imageParams)) {
        KRAnyData testValue = nullptr;
        if (KRAnyDataGetMapValue(imageParams, "test", &testValue) == KRANYDATA_SUCCESS && testValue != nullptr) {
            if (KRAnyDataIsString(testValue)) {
                const char *str = KRAnyDataGetString(testValue);
                KR_LOG_INFO << "imageParams test value: " << str;
            }
        }
    }
    
    static int counter = 0;
    if(counter++ % 2 == 0){
        return 0;
    }
    std::string_view src_view(src);
    if(src_view.find("panda2") != std::string_view::npos){
        if(RawFile *raw_file = OH_ResourceManager_OpenRawFile(g_resource_manager, "panda2.png")){
            RawFileDescriptor descriptor;
            if(OH_ResourceManager_GetRawFileDescriptor(raw_file, descriptor)){
                OH_ImageSourceNative *image_source = nullptr;
                Image_ErrorCode errCode = OH_ImageSourceNative_CreateFromRawFile(&descriptor, &image_source);
                if(image_source){
                    OH_DecodingOptions *ops = nullptr;
                    OH_DecodingOptions_Create(&ops);
                    // 设置为AUTO会根据图片资源格式解码，如果图片资源为HDR资源则会解码为HDR的pixelmap。
                    OH_DecodingOptions_SetDesiredDynamicRange(ops, IMAGE_DYNAMIC_RANGE_AUTO);
                    OH_PixelmapNative *resPixMap = nullptr;
            
                    // ops参数支持传入nullptr, 当不需要设置解码参数时，不用创建
                    errCode = OH_ImageSourceNative_CreatePixelmap(image_source, ops, &resPixMap);
                    OH_DecodingOptions_Release(ops);
                    if (errCode != IMAGE_SUCCESS) {
                        return 0;
                    }
                    OH_ImageSourceNative_Release(image_source);
            
                    // 通过PixelMap创建DrawableDescriptor
                    ArkUI_DrawableDescriptor *imageDescriptor = OH_ArkUI_DrawableDescriptor_CreateFromPixelMap(resPixMap);
#if MyImageAdapterV2_SYNC_CALLBACK
                    // call back immediate ly
                    callback(context, src, imageDescriptor, nullptr);
                    OH_ArkUI_DrawableDescriptor_Dispose(imageDescriptor);
#else
                    // use thread safe function to simulate an async callback
                    ImageCallbackTask *mainTask = new ImageCallbackTask(context, src, imageDescriptor, callback);
                    napi_call_threadsafe_function(g_threadsafe_func, static_cast<void *>(mainTask), napi_tsfn_blocking);
#endif
                    OH_PixelmapNative_Release(resPixMap);
                    return 1;
                }
                OH_ResourceManager_ReleaseRawFileDescriptor(descriptor);
            }
        }
    }
    return 0;
}

int32_t MyImageAdapterV2(const void *context,
                                 const char *src,
                                 KRSetImageCallback callback){
    static int counter = 0;
    if(counter++ % 2 == 0){
        return 0;
    }
    std::string_view src_view(src);
    if(src_view.find("panda2") != std::string_view::npos){
        if(RawFile *raw_file = OH_ResourceManager_OpenRawFile(g_resource_manager, "panda2.png")){
            RawFileDescriptor descriptor;
            if(OH_ResourceManager_GetRawFileDescriptor(raw_file, descriptor)){
                OH_ImageSourceNative *image_source = nullptr;
                Image_ErrorCode errCode = OH_ImageSourceNative_CreateFromRawFile(&descriptor, &image_source);
                if(image_source){
                    OH_DecodingOptions *ops = nullptr;
                    OH_DecodingOptions_Create(&ops);
                    // 设置为AUTO会根据图片资源格式解码，如果图片资源为HDR资源则会解码为HDR的pixelmap。
                    OH_DecodingOptions_SetDesiredDynamicRange(ops, IMAGE_DYNAMIC_RANGE_AUTO);
                    OH_PixelmapNative *resPixMap = nullptr;
                    
                    // ops参数支持传入nullptr, 当不需要设置解码参数时，不用创建
                    errCode = OH_ImageSourceNative_CreatePixelmap(image_source, ops, &resPixMap);
                    OH_DecodingOptions_Release(ops);
                    if (errCode != IMAGE_SUCCESS) {
                        return 0;
                    }
                    OH_ImageSourceNative_Release(image_source);
            
                    // 通过PixelMap创建DrawableDescriptor
                    ArkUI_DrawableDescriptor *imageDescriptor = OH_ArkUI_DrawableDescriptor_CreateFromPixelMap(resPixMap);
#if MyImageAdapterV2_SYNC_CALLBACK
                    // call back immediate ly
                    callback(context, src, imageDescriptor, nullptr);
                    OH_ArkUI_DrawableDescriptor_Dispose(imageDescriptor);
#else
                    // use thread safe function to simulate an async callback
                    ImageCallbackTask *mainTask = new ImageCallbackTask(context, src, imageDescriptor, callback);
                    napi_call_threadsafe_function(g_threadsafe_func, static_cast<void *>(mainTask), napi_tsfn_blocking);
#endif
                    OH_PixelmapNative_Release(resPixMap);
                    return 1;
                }
                OH_ResourceManager_ReleaseRawFileDescriptor(descriptor);
            }
        }
    }
    return 0;
}

static char *MyImageAdapter(const char *imageSrc, ArkUI_DrawableDescriptor **imageDescriptor,
                            KRImageDataDeallocator *deallocator) {
    if (std::strcmp(imageSrc, "customImageSrc") == 0) {
        // 创建ImageSource实例
        OH_ImageSourceNative *source = nullptr;
        Image_ErrorCode errCode =
            OH_ImageSourceNative_CreateFromUri((char *)customImagePath.c_str(), customImagePath.length(), &source);
        if (errCode != IMAGE_SUCCESS) {
            return nullptr;
        }

        // 通过图片解码参数创建PixelMap对象
        OH_DecodingOptions *ops = nullptr;
        OH_DecodingOptions_Create(&ops);
        // 设置为AUTO会根据图片资源格式解码，如果图片资源为HDR资源则会解码为HDR的pixelmap。
        OH_DecodingOptions_SetDesiredDynamicRange(ops, IMAGE_DYNAMIC_RANGE_AUTO);
        OH_PixelmapNative *resPixMap = nullptr;

        // ops参数支持传入nullptr, 当不需要设置解码参数时，不用创建
        errCode = OH_ImageSourceNative_CreatePixelmap(source, ops, &resPixMap);
        OH_DecodingOptions_Release(ops);
        if (errCode != IMAGE_SUCCESS) {
            return nullptr;
        }
        OH_ImageSourceNative_Release(source);

        // 通过PixelMap创建DrawableDescriptor
        *imageDescriptor = OH_ArkUI_DrawableDescriptor_CreateFromPixelMap(resPixMap);

        OH_PixelmapNative_Release(resPixMap);
        *deallocator = [](void *data) {
            OH_ArkUI_DrawableDescriptor_Dispose(static_cast<ArkUI_DrawableDescriptor *>(data));
        };
        return nullptr;
    } else {
        char *newImageSrc = (char *)imageSrc;
        return newImageSrc;
    }
}
static void MyLogAdapter(int logLevel, const char *tag, const char *message) {
    static int MyDomain = 0x1234;
    OH_LOG_Print(LOG_APP, LOG_INFO, MyDomain, tag, "%{public}s", message);
}

static int64_t MyColorAdapter(const char* str){
    // Add custom parsing and return actual color value.
    // Demo only returns -1 to allow kuikly automatically convert the color string
    return -1;
}

static void* ExampleModuleOnConstruct(const char *moduleName){
    return nullptr;
}

void ExampleModuleOnDestruct(const void* moduleInstance){
    // since nullptr was returned in ExampleModuleOnConstruct,
    // we don't need to do anything here
}

static KRCallMethodCValue ExampleModuleOnCallMethod(const void* moduleInstance,
    const char* moduleName,
    int sync,
    const char *method,
    KRAnyData param,
    KRRenderModuleCallbackContext context){
    
    if (context){
        // Do some work and callback later.
        // For the sake of simplicity, a thread is used here to illustrate the async behavior,
        // which might probably not be the best practice.
        std::thread([context] { 
            char* result = "{\"key\":\"value\"}";
            KRRenderModuleDoCallback(context, result);
        }).detach();
    }
    std::string resultString(method ? method: "");
    resultString.append(" handled.");
    KRCallMethodCValue ret;
    ret.res = strdup(resultString.c_str()); // the result string
    ret.free = free; // strdup result need to be freed later
    ret.length = resultString.size(); // the length of the res string
    return ret;
}

// =====================================================================
// 沙盒 cacheDir：从 ets 侧 EntryAbility.onCreate 时通过 setCacheDir(napi) 灌入。
// MyTextPostProcessorAdapter 把 hap rawfile 中的 emoji 资源拷贝到 cacheDir 子目录后
// 给出 file:// URI，作为 KRTextProcessedResultAppendImageSpan 的可寻址参数。
// =====================================================================
static std::string g_cache_dir;

static napi_value SetCacheDir(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        return nullptr;
    }
    if (argc < 1 || args[0] == nullptr) {
        return nullptr;
    }
    size_t length = 0;
    if (napi_get_value_string_utf8(env, args[0], nullptr, 0, &length) != napi_ok) {
        return nullptr;
    }
    std::string buffer(length, 0);
    if (napi_get_value_string_utf8(env, args[0], buffer.data(), length + 1, &length) != napi_ok) {
        return nullptr;
    }
    g_cache_dir = buffer;
    return nullptr;
}

// =====================================================================
// Text Post Processor adapter 示例
//
// 与 Android `KRTextPostProcessorAdapter`、iOS `EMOJI_IMAGE_MAP` 对齐：识别 [smile]
// 等自定义短码，替换为 hap resfile 内的 PNG。技术要点：
//   1) SDK 仅接受 file:// / http(s):// / data:image;base64 形态的可寻址 URI，
//      不接受私有协议（如 assets://）；
//   2) hap 内 resfile 资源（由 hvigorfile.ts 的 kuiklyCopyAssets 从
//      demo/commonMain/assets 同步过来）已经是真实可寻址文件，无需拷贝，
//      直接拼接 "file://${context.resourceDir}/${rel}" 即可；
//   3) ets 侧通过 setResfileDir(getContext().resourceDir) 把根路径下发给 native。
// =====================================================================

static const std::map<std::string, std::string> &EmojiShortcodeToResfile() {
    static const std::map<std::string, std::string> map = {
        {"[smile]",   "common/emoji_smile.png"},
        {"[heart]",   "common/emoji_heart.png"},
        {"[thumbup]", "common/emoji_thumbup.png"},
        {"[star]",    "common/emoji_star.png"},
        {"[fire]",    "common/emoji_fire.png"},
    };
    return map;
}

// resfile 相对路径 -> file:// URI 的进程级缓存（避免每次扫描都拼字符串）。
static std::mutex g_emoji_uri_cache_mutex;
static std::map<std::string, std::string> g_emoji_uri_cache;

// resfile 资源已经是 hap 内可寻址的真实文件，无需拷贝，直接拼出
// "file://${g_resfile_dir}/${rel}" 即可。
// 失败条件：g_resfile_dir 未注入（ets 侧没调 setResfileDir）。
static std::string GetEmojiFileUri(const std::string &resfile_rel) {
    static constexpr int kEmojiDomain = 0x1235;
    {
        std::lock_guard<std::mutex> lk(g_emoji_uri_cache_mutex);
        auto it = g_emoji_uri_cache.find(resfile_rel);
        if (it != g_emoji_uri_cache.end()) {
            return it->second;
        }
    }
    if (g_resfile_dir.empty()) {
        OH_LOG_Print(LOG_APP, LOG_ERROR, kEmojiDomain, "KuiklyEmoji",
                     "GetEmojiFileUri: g_resfile_dir empty, did ets call setResfileDir? rel=%{public}s",
                     resfile_rel.c_str());
        return "";
    }
    std::string abs_path = g_resfile_dir + "/" + resfile_rel;
    // 仅在 first miss 时 stat 校验一次；发现不存在直接返空，让 adapter 走 fallback
    // 把短码以纯文本保留，避免显示成空白。
    struct stat st {};
    if (stat(abs_path.c_str(), &st) != 0 || st.st_size <= 0) {
        OH_LOG_Print(LOG_APP, LOG_ERROR, kEmojiDomain, "KuiklyEmoji",
                     "GetEmojiFileUri: file not found, abs=%{public}s", abs_path.c_str());
        return "";
    }
    std::string uri = "file://" + abs_path;
    OH_LOG_Print(LOG_APP, LOG_INFO, kEmojiDomain, "KuiklyEmoji",
                 "GetEmojiFileUri OK, rel=%{public}s uri=%{public}s",
                 resfile_rel.c_str(), uri.c_str());
    {
        std::lock_guard<std::mutex> lk(g_emoji_uri_cache_mutex);
        g_emoji_uri_cache[resfile_rel] = uri;
    }
    return uri;
}

static void MyTextPostProcessorAdapter(const char *text,
                                       void * /*reserved*/,
                                       KRTextProcessedResultBuilder builder) {
    if (!text || !builder) {
        return;
    }
    // 探针：便于线上一眼确认 adapter 是否被调到、看到输入文本。tag=KuiklyEmoji。
    {
        static constexpr int kEmojiDomain = 0x1235;
        OH_LOG_Print(LOG_APP, LOG_INFO, kEmojiDomain, "KuiklyEmoji",
                     "MyTextPostProcessorAdapter called, text=%{public}s", text);
    }
    const std::string s = text;
    const auto &shortcode_map = EmojiShortcodeToResfile();

    // 单轮扫描：在遇到 '[' 时尝试匹配"[xxx]"；匹配不上则仅推进 1 个字节继续扫描。
    // 使用临时缓冲 pending 累积"未匹配上表情"的纯文本，匹配上后一次性 flush。
    std::string pending;
    pending.reserve(s.size());
    auto flush_pending = [&]() {
        if (!pending.empty()) {
            KRTextProcessedResultAppendTextSpan(builder, pending.c_str());
            pending.clear();
        }
    };

    size_t i = 0;
    while (i < s.size()) {
        if (s[i] == '[') {
            size_t rb = s.find(']', i + 1);
            if (rb != std::string::npos && rb - i + 1 <= 32) {
                std::string code = s.substr(i, rb - i + 1);
                auto it = shortcode_map.find(code);
                if (it != shortcode_map.end()) {
                    std::string uri = GetEmojiFileUri(it->second);
                    static constexpr int kEmojiDomain2 = 0x1235;
                    OH_LOG_Print(LOG_APP, LOG_INFO, kEmojiDomain2, "KuiklyEmoji",
                                 "shortcode hit code=%{public}s resfile=%{public}s uri_empty=%{public}d",
                                 code.c_str(), it->second.c_str(), uri.empty() ? 1 : 0);
                    if (!uri.empty()) {
                        flush_pending();
                        // width/height 均传 0，表示"按当前字号自适应"。
                        // 显式回传 raw_literal=code（即 "[smile]" 等原始字面量），
                        // 让 SDK 在编辑差分回写阶段基于权威映射精准还原 shortcode。
                        // 与 iOS demo 中 KREmojiTextAttachment.originalShortcode 对齐。
                        KRTextProcessedResultAppendImageSpanWithRaw(builder, uri.c_str(),
                                                                    code.c_str(), 0, 0);
                        OH_LOG_Print(LOG_APP, LOG_INFO, kEmojiDomain2, "KuiklyEmoji",
                                     "AppendImageSpanWithRaw called, code=%{public}s uri=%{public}s",
                                     code.c_str(), uri.c_str());
                        i = rb + 1;
                        continue;
                    }
                }
            }
        }
        pending.push_back(s[i]);
        ++i;
    }
    flush_pending();
}

static void registerExampleCModule(){
    KRRenderModuleRegister("MyExampleCModule", &ExampleModuleOnConstruct, &ExampleModuleOnDestruct, &ExampleModuleOnCallMethod, nullptr);
}

// 设置输入控件新实现开关。参数：int32 value（0=走老控件；1=走新 ARKUI_NODE_TEXT_EDITOR 控件，
// 且仅在 API>=24 时生效）。仅影响设置后新创建的 Input/TextArea。
// 开关值的实际存储位于 libkuikly.so（core-render-ohos 产物）内，本函数通过链接
// 期符号解析调用对应的 C API，避免跨 so 使用 inline/static 变量每个模块一份副本导致开关失效。
extern "C" void KRSetUseNewTextInputComponent(int value);
static napi_value SetUseNewTextInputComponent(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_value result;
    napi_create_int32(env, 0, &result);
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "napi_get_cb_info error");
        return result;
    }
    int32_t value = 0;
    if (argc >= 1 && args[0] != nullptr) {
        napi_get_value_int32(env, args[0], &value);
    }
    KRSetUseNewTextInputComponent(value);
    napi_create_int32(env, value, &result);
    return result;
}

static int adapterRegistered = false;
static napi_value InitKuikly(napi_env env, napi_callback_info info) {
    if (!adapterRegistered) {
        registerExampleCModule();
        
        KRRegisterColorAdapter(MyColorAdapter);
        KRRegisterLogAdapter(MyLogAdapter);
        KRRegisterFontAdapter(MyFontAdapter, "Satisfy-Regular");
        KRRegisterImageAdapter(MyImageAdapter);
        KRRegisterImageAdapterV2(MyImageAdapterV2);
        KRRegisterImageAdapterV3(MyImageAdapterV3);
        // 文本预处理（emoji 短码 -> 图片替换）示例：
        //   * "input"    : 输入框（ARKUI_NODE_TEXT_EDITOR 路径）；
        //   * "richtext" : 只读富文本（KRRichTextView OnForegroundDraw 路径，方案 A）。
        // 两条路径共用同一个 adapter 实现：扫描 [xxx] 短码 → 解为 file:// URI → 用
        // KRTextProcessedResultAppendImageSpanWithRaw 回传，SDK 内部按 name 分流到对应
        // 渲染管线（编辑态走 SetStyledText + ImageAttachment；只读富文本走 PlaceholderSpan
        // + DrawPixelMapRect）。
        KRRegisterTextPostProcessorAdapter("input", MyTextPostProcessorAdapter);
        KRRegisterTextPostProcessorAdapter("richtext", MyTextPostProcessorAdapter);
        adapterRegistered = true;
    }

    auto api = libshared_symbols();
    int handler = api->kotlin.root.initKuikly();
    napi_value result;
    napi_create_int32(env, handler, &result);
    return result;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    napi_property_descriptor desc[] = {
        {"initKuikly", nullptr, InitKuikly, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"setFontPath", nullptr, SetFontPath, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"setCacheDir", nullptr, SetCacheDir, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"setResfileDir", nullptr, SetResfileDir, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"setResourceManager", nullptr, SetResourceManager, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"setUseNewTextInputComponent", nullptr, SetUseNewTextInputComponent, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    return exports;
}
EXTERN_C_END

static napi_module entry_module = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "kuikly_entry",
    .nm_priv = static_cast<void *>(0),
    .reserved = {0},
};

extern "C" __attribute__((constructor)) void RegisterKuikly_EntryModule(void) {
    napi_module_register(&entry_module);
}

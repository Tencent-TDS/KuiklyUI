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
#include <thread>
#include <memory>
#include <string>
#include "libohos_render/api/include/Kuikly/Kuikly.h"
#include "napi/native_api.h"
#include "thirdparty/biz_entry/libshared_api.h"

static std::string customFontPath;
static std::string customImagePath;

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

static void registerExampleCModule(){
    KRRenderModuleRegister("MyExampleCModule", &ExampleModuleOnConstruct, &ExampleModuleOnDestruct, &ExampleModuleOnCallMethod, nullptr);
}

static int adapterRegistered = false;
static napi_value InitKuikly(napi_env env, napi_callback_info info) {
    if (!adapterRegistered) {
        registerExampleCModule();
        
        KRRegisterColorAdapter(MyColorAdapter);
        KRRegisterLogAdapter(MyLogAdapter);
        KRRegisterFontAdapter(MyFontAdapter, "Satisfy-Regular");
        KRRegisterImageAdapter(MyImageAdapter);
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

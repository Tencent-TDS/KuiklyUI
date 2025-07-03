/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_KRRENDERADAPTERMANAGER_H
#define CORE_RENDER_OHOS_KRRENDERADAPTERMANAGER_H

#include <hilog/log.h>
#include <string>
#include "libohos_render/expand/components/image/KRImageLoadOption.h"

struct ArkTSFuncData {
    napi_env env;
    napi_value func;
    napi_ref func_ref = nullptr;
};

/**
 * @brief 获取并使用从imageAdapter得到的src或imageDescriptor的回调函数
 * @param char* 图片源
 * @param OH_PixelmapNative* OH_PixelmapNative描述的图片
 * 注：image组件优先使用OH_PixelmapNative*，当OH_PixelmapNative*为空时使用char*
 */
typedef std::function<void(char*, OH_PixelmapNative*)> ImageCallback;

class IKRColorParseAdapter {
 public:
    /**
     * 宿主自定义颜色转换接口
     * @param colorStr 来自Kotlin侧传递的颜色字符串
     * @return 如果不解析该颜色，则默认返回-1，否则返回返回16进制颜色值（如0xffbababa）
     */
    virtual std::int64_t GetHexColor(const std::string &colorStr) = 0;
};

class IKRLogAdapter {
 public:
    virtual ~IKRLogAdapter() {}
    virtual void LogInfo(const std::string &tag, const std::string &msg) = 0;
    virtual void LogDebug(const std::string &tag, const std::string &msg) = 0;
    virtual void LogError(const std::string &tag, const std::string &msg) = 0;
};

class KRRenderAdapterManager {
 public:
    void OnFatalException(const std::string &instance_id, const std::string &stack);
    void Log(const LogLevel &log_level, const std::string &tag, const std::string &msg);

    static KRRenderAdapterManager &GetInstance();
    /**
     * NDK侧注册自定义Color解析Public接口
     */
    void RegisterColorAdapter(IKRColorParseAdapter *color_adapter);
    /**
     * 注册 C++层日志接口
     */
    void RegisterLogAdapter(std::shared_ptr<IKRLogAdapter> log_adapter);

    /**
     * 注册图片加载适配器
     * @param image_adapter
     */
    void RegisterImageAdapter(napi_env env, napi_value func);

    IKRColorParseAdapter *GetColorAdapter();

    /**
     * @brief 调用图片适配器获取图片
     * @param image_src 图片源
     * @param callback 返回结果的回调函数，回调函数接收图片源和OH_PixelmapNative*图片作为参数
     */
    void CallImageAdapter(const char *image_src, ImageCallback callback);
    
    /**
     * @brief 调用Image CallBack设置通过适配器获取到的图片
     * 优先使用pixel_map，若pixel_map为空，则使用image_src内部加载图片
     * @param image_src 图片源
     * @param pixel_map 适配器返回的图片
     * @param callback_id 回调函数的id
     */
    void fireImageCallback(const char *image_src, OH_PixelmapNative *pixel_map, std::string callback_id);
    
    bool HasCustomImageAdapter() {
        return image_adapter_ != nullptr;
    }

    bool HasCustomLogAdapter() {
        return log_adapter_ != nullptr;
    }

 private:
    KRRenderAdapterManager() = default;
    IKRColorParseAdapter *color_adapter_ = nullptr;
    std::shared_ptr<IKRLogAdapter> log_adapter_;
    std::shared_ptr<ArkTSFuncData> image_adapter_ = nullptr;
    // ImageCallback管理索引表
    std::unordered_map<std::string, ImageCallback> image_callback_map_;
    void LogInfo(const std::string &tag, const std::string &msg);
    void LogDebug(const std::string &tag, const std::string &msg);
    void LogError(const std::string &tag, const std::string &msg);
    void CallArkTsExceptionModule(const std::string &instance_id, const std::string &method_name,
                                  const std::string &stack);
    std::string GetIncreaseImageCallbackId(ImageCallback image_callback);
};

#endif  // CORE_RENDER_OHOS_KRRenderAdapterManager_H

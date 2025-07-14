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

class IKRColorParseAdapter {
 public:
    virtual ~IKRColorParseAdapter() = default;
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

class IKRImageAdapter {
 public:
    virtual void ConvertImageLoadOption(const std::shared_ptr<KRImageLoadOption> image_option) = 0;
};

class KRRenderAdapterManager {
 public:
    void OnFatalException(const std::string &instance_id, const std::string &stack);
    void Log(const LogLevel &log_level, const std::string &tag, const std::string &msg);

    static KRRenderAdapterManager &GetInstance();
    /**
     * NDK侧注册自定义Color解析Public接口
     */
    void RegisterColorAdapter(std::shared_ptr<IKRColorParseAdapter> color_adapter);
    /**
     * 注册 C++层日志接口
     */
    void RegisterLogAdapter(std::shared_ptr<IKRLogAdapter> log_adapter);

    /**
     * 注册图片加载适配器
     * @param image_adapter
     */
    void RegisterImageAdapter(IKRImageAdapter *image_adapter);

    std::shared_ptr<IKRColorParseAdapter> GetColorAdapter();

    IKRImageAdapter *GetImageAdapter();

    bool HasCustomLogAdapter() {
        return log_adapter_ != nullptr;
    }

 private:
    KRRenderAdapterManager() = default;
    std::shared_ptr<IKRColorParseAdapter> color_adapter_;
    std::shared_ptr<IKRLogAdapter> log_adapter_;
    IKRImageAdapter *image_adapter_ = nullptr;
    void LogInfo(const std::string &tag, const std::string &msg);
    void LogDebug(const std::string &tag, const std::string &msg);
    void LogError(const std::string &tag, const std::string &msg);
    void CallArkTsExceptionModule(const std::string &instance_id, const std::string &method_name,
                                  const std::string &stack);
};

#endif  // CORE_RENDER_OHOS_KRRenderAdapterManager_H

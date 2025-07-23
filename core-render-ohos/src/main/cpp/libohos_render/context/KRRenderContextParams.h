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

#ifndef CORE_RENDER_OHOS_KRRENDERCONTEXTPARAMS_H
#define CORE_RENDER_OHOS_KRRENDERCONTEXTPARAMS_H

#include <string>
#include "libohos_render/context/KRRenderNativeMode.h"
#include "libohos_render/foundation/KRConfig.h"
#include "libohos_render/foundation/type/KRRenderValue.h"

/***
 * 页面上下文参数（页面名，页面数据，页面执行模式，页面实例id）
 */
class KRRenderContextParams {
 public:
    KRRenderContextParams(const std::string &page_name, const std::shared_ptr<KRRenderValue> &page_data,
                          const std::string &instance_id, const std::string &configJsonStr) {
        this->page_name_ = page_name;
        this->instance_id_ = instance_id;
        this->page_data_ = page_data;
        this->config_ = std::make_shared<KRConfig>(configJsonStr);

        auto page_data_map = this->page_data_->toMap();
        int page_data_mode = page_data_map["executeMode"]->toInt();
        std::unordered_map<int, KRRenderExecuteModeCreator> mode_creator_register =
            KRRenderExecuteMode::GetExecuteModeCreatorRegister();
        if (mode_creator_register.find(page_data_mode) != mode_creator_register.end()) {
            auto creator = mode_creator_register[page_data_mode];
            execute_mode_ = creator();
        } else {
            std::shared_ptr<KRRenderExecuteMode> defaultMode = std::make_shared<KRRenderNativeMode>();
            if (defaultMode->GetMode() == page_data_mode) {
                execute_mode_ = defaultMode;
            }
        }
        context_code_ = page_data_map["contextCode"]->toString();
    }
    const std::string &PageName() const {
        return page_name_;
    }
    const std::shared_ptr<KRRenderExecuteMode> &ExecuteMode() const {
        return execute_mode_;
    }
    const std::string &ContextCode() const {
        return context_code_;
    }
    const std::string &InstanceId() const {
        return instance_id_;
    }
    const std::shared_ptr<KRRenderValue> &PageData() const {
        return page_data_;
    }
    const std::shared_ptr<KRRenderValue> PageParam() const {
        return page_data_->toMap().find("param")->second;
    }
    const std::shared_ptr<KRConfig> &Config() const {
        return config_;
    }

    KRRenderContextParams(const KRRenderContextParams &) = delete;
    KRRenderContextParams &operator=(const KRRenderContextParams &) = delete;

 private:
    /** 页面名，对标kotlin侧 @Page("xxxxx")中的"xxxxx" */
    std::string page_name_;
    /** 唯一页面实例ID，用于与context产物交互时指定所在页面实例(对标kotlin侧pagerId) */
    std::string instance_id_;
    /** 页面执行模式*/
    std::shared_ptr<KRRenderExecuteMode> execute_mode_;
    /**
     * 执行上下文code，驱动渲染对应的代码。KRRenderExecuteMode.Native模式时: 传递 ""
     */
    std::string context_code_;
    /** 页面携带透传的数据，对标kotlin侧的pagerData.params */
    std::shared_ptr<KRRenderValue> page_data_;
    /** 配置数据 */
    std::shared_ptr<KRConfig> config_;
};
#endif  // CORE_RENDER_OHOS_KRRENDERCONTEXTPARAMS_H

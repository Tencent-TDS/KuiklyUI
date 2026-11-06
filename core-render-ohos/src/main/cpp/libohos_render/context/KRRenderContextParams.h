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
#include "libohos_render/context/KRRenderExecuteMode.h"
#include "libohos_render/core/KRRenderFactories.h"
#include "libohos_render/foundation/KRConfig.h"
#include "libohos_render/foundation/type/KRRenderValue.h"

/***
 * 页面上下文参数（页面名，页面数据，页面执行模式，页面实例id）
 */
class KRRenderContextParams {
 public:
    KRRenderContextParams(const KRRenderValue &page_name, const KRRenderValue &page_data,
                          const KRRenderValue &instance_id, const KRRenderValue &configJson,
                          const std::string &context_code, int execute_mode) {
        // Keep the NAPI UTF-16 boxes for CallKotlin. std::string copies are
        // only for C++ lookup / logs / paths — do not Make() them back to Kotlin.
        this->page_name_value_ = page_name;
        this->instance_id_value_ = instance_id;
        this->page_name_ = page_name.toString();
        this->instance_id_ = instance_id.toAsciiString();
        this->page_data_ = page_data;
        this->parsed_page_data_ = this->page_data_.Parsed();
        this->config_ = std::make_shared<KRConfig>(configJson);
        this->context_code_ = context_code;
        this->execute_mode_ = kuikly::ExecuteModeFactory::Create(execute_mode);
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
    const KRRenderValue &PageNameValue() const {
        return page_name_value_;
    }
    const KRRenderValue &InstanceIdValue() const {
        return instance_id_value_;
    }
    /** 取出原始 pageData（多为 JSON 字符串），成员被置空。 */
    KRRenderValue RemovePageData() {
        return std::move(page_data_);
    }
    /** 取出解析后的 JSON object，成员被置空。 */
    KRRenderValue RemoveParsedPageData() {
        return std::move(parsed_page_data_);
    }
    /** 只读 peek：不消费，供 DidInit 之前需要读取 pageData 的逻辑使用。 */
    const KRRenderValue &PageData() const {
        return page_data_;
    }
    const KRRenderValue &ParsedPageData() const {
        return parsed_page_data_;
    }
    const std::shared_ptr<KRConfig> &Config() const {
        return config_;
    }
    const std::string &WindowId() const {
        return config_->GetWindowId();
    }

    KRRenderContextParams(const KRRenderContextParams &) = delete;
    KRRenderContextParams &operator=(const KRRenderContextParams &) = delete;

 private:
    /** 页面名，对标kotlin侧 @Page("xxxxx")中的"xxxxx" */
    std::string page_name_;
    /** 唯一页面实例ID，用于与context产物交互时指定所在页面实例(对标kotlin侧pagerId) */
    std::string instance_id_;
    /** ArkTS 入口留下的 UTF-16 盒，CallKotlin 用这两份，不用上面的 UTF-8 拷贝。 */
    KRRenderValue page_name_value_;
    KRRenderValue instance_id_value_;
    /** 页面执行模式*/
    std::shared_ptr<KRRenderExecuteMode> execute_mode_;
    /**
     * 执行上下文code，驱动渲染对应的代码。KRRenderExecuteMode.Native模式时: 传递 ""
     */
    std::string context_code_;
    /** 页面携带透传的数据，对标kotlin侧的pagerData.params；动态化仍可能是 JSON 文本 */
    KRRenderValue page_data_;
    /** `page_data_.Parsed()` 的缓存：object 原样，string 则 parse 一次 */
    KRRenderValue parsed_page_data_;
    /** 配置数据 */
    std::shared_ptr<KRConfig> config_;
};
#endif  // CORE_RENDER_OHOS_KRRENDERCONTEXTPARAMS_H

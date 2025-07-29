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

#ifndef CORE_RENDER_OHOS_KRRENDERLOGER_H
#define CORE_RENDER_OHOS_KRRENDERLOGER_H

#include <arm-linux-ohos/asm/setup.h>
#include <hilog/log.h>
#include <sstream>
#include <string>
#include "libohos_render/adapter/KRRenderAdapterManager.h"

class KRRenderLog {
 public:
    explicit KRRenderLog(LogLevel log_level) : log_level_(log_level), tag_("KRRender") {}
    KRRenderLog(LogLevel log_level, const std::string &tag) : log_level_(log_level), tag_(tag) {}

    ~KRRenderLog() {
        stream_ << std::endl;
        KRRenderAdapterManager::GetInstance().Log(log_level_, tag_, std::move(stream_.str()));
    }

    template <typename T> KRRenderLog &operator<<(const T &value) {
        stream_ << value;
        return *this;
    }

 private:
    std::string tag_;
    LogLevel log_level_;
    std::stringstream stream_;
};

#define KR_LOG_INFO KRRenderLog(LOG_INFO)
#define KR_LOG_DEBUG KRRenderLog(LOG_DEBUG)
#define KR_LOG_ERROR KRRenderLog(LOG_ERROR)

#define KR_LOG_INFO_WITH_TAG(tag) KRRenderLog(LOG_INFO, tag)
#define KR_LOG_DEBUG_WITH_TAG(tag) KRRenderLog(LOG_DEBUG, tag)
#define KR_LOG_ERROR_WITH_TAG(tag) KRRenderLog(LOG_ERROR, tag)

#endif  // CORE_RENDER_OHOS_KRRENDERLOGER_H

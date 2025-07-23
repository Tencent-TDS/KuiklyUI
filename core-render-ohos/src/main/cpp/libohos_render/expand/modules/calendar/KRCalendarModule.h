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
#pragma once
#include "KRDate.h"
#include "libohos_render/export/IKRRenderModuleExport.h"

namespace kuikly {
namespace module {
// according to ICalendar Field
enum Field {
    YEAR = 1,
    MONTH = 2,
    DAY_OF_MONTH = 5,
    DAY_OF_YEAR = 6,  // 一年中第一天的值为 1
    DAY_OF_WEEK = 7,  // 自星期日起[1-7]
    HOUR_OF_DAY = 11,
    MINUS = 12,
    SECOND = 13,
    MILLISECOND = 14
};

class KRCalendarModule : public IKRRenderModuleExport {
 public:
    static const char MODULE_NAME[];
    bool SyncMode();
    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback) override;
    void OnDestroy() override;

 private:
    static const char METHOD_CURRENT_TIMESTAMP[];
    static const char METHOD_GET_FIELD[];
    static const char METHOD_GET_TIME_IN_MILLIS[];
    static const char METHOD_FORMAT[];
    static const char METHOD_PARSE_FORMAT[];
    static const char PARAM_OPERATIONS[];
    static const char *PARAM_FIELD;
    static const char PARAM_TIME_MILLIS[];
    static const char *PARAM_FORMAT;
    static const char *PARAM_FORMATTED_TIME;
    static const char *OP_SET;
    static const char *OP_ADD;

    util::Date CalDate(util::Date &date, const std::string &op);

    std::string CurrentTimestamp(const KRAnyValue &params);

    std::string GetField(const KRAnyValue &params);

    std::string GetTimeMillis(const KRAnyValue &params);

    std::string ZeroPadded(int num, int digits);

    std::string Format(const KRAnyValue &params);

    std::string Parse(const KRAnyValue &params);

    void Replace(std::string &str, const char *format, std::string substr);
};

}  // namespace module
}  // namespace kuikly

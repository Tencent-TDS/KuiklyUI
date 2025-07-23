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

#include "KRCalendarModule.h"

#include "thirdparty/cJSON/cJSON.h"

static const char *TAG = __FILE_NAME__;
namespace kuikly {
namespace module {

const char KRCalendarModule::MODULE_NAME[] = "KRCalendarModule";
const char KRCalendarModule::METHOD_CURRENT_TIMESTAMP[] = "method_cur_timestamp";
const char KRCalendarModule::METHOD_GET_FIELD[] = "method_get_field";
const char KRCalendarModule::METHOD_GET_TIME_IN_MILLIS[] = "method_get_time_in_millis";
const char KRCalendarModule::METHOD_FORMAT[] = "method_format";
const char KRCalendarModule::METHOD_PARSE_FORMAT[] = "method_parse_format";
const char KRCalendarModule::PARAM_OPERATIONS[] = "operations";
const char *KRCalendarModule::PARAM_FIELD = "field";
const char KRCalendarModule::PARAM_TIME_MILLIS[] = "timeMillis";
const char *KRCalendarModule::PARAM_FORMAT = "format";
const char *KRCalendarModule::PARAM_FORMATTED_TIME = "formattedTime";
const char *KRCalendarModule::OP_SET = "set";
const char *KRCalendarModule::OP_ADD = "add";

bool KRCalendarModule::SyncMode() {
    return true;
}
KRAnyValue KRCalendarModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                        const KRRenderCallback &callback) {
    // KLOG_INFO(TAG) << method;
    if (method == this->METHOD_CURRENT_TIMESTAMP) {
        return std::make_shared<KRRenderValue>(this->CurrentTimestamp(params));
    } else if (method == this->METHOD_GET_FIELD) {
        return std::make_shared<KRRenderValue>(this->GetField(params));
    } else if (method == this->METHOD_GET_TIME_IN_MILLIS) {
        return std::make_shared<KRRenderValue>(this->GetTimeMillis(params));
    } else if (method == this->METHOD_FORMAT) {
        return std::make_shared<KRRenderValue>(this->Format(params));
    } else if (method == this->METHOD_PARSE_FORMAT) {
        return std::make_shared<KRRenderValue>(this->Parse(params));
    }
    return std::make_shared<KRRenderValue>();
}

void KRCalendarModule::OnDestroy() {
    // Intentionally left blank
}

util::Date KRCalendarModule::CalDate(util::Date &date, const std::string &op) {
    // KLOG_INFO(TAG) << "calDate: op = " << op;
    cJSON *opObject = cJSON_Parse(op.data());

    cJSON *optPtr = cJSON_GetObjectItemCaseSensitive(opObject, "opt");
    std::string opt = optPtr != nullptr ? optPtr->valuestring : "";
    if (opt != this->OP_SET && opt != this->OP_ADD) {
        return util::Date();
    }

    cJSON *valuePtr = cJSON_GetObjectItemCaseSensitive(opObject, "value");
    int value = (valuePtr != nullptr) ? static_cast<int>(valuePtr->valuedouble) : 0;
    int originalValue = 0;
    util::Date newDate = util::Date(date);

    cJSON *fieldPtr = cJSON_GetObjectItemCaseSensitive(opObject, this->PARAM_FIELD);
    int field = (fieldPtr != nullptr) ? static_cast<int>(fieldPtr->valuedouble) : 0;
    // KLOG_INFO(TAG) << "field = " << field;
    cJSON_Delete(opObject);
    switch (field) {
    case YEAR:
        originalValue = (opt == this->OP_SET) ? 0 : date.GetFullYear();
        newDate.SetFullYear(originalValue + value);
        // KLOG_INFO(TAG) << "originalValue = " << originalValue << ' ' << value;
        break;
    case MONTH:
        originalValue = (opt == this->OP_SET) ? 0 : date.GetMonth();
        newDate.SetMonth(originalValue + value);
        // KLOG_INFO(TAG) << "originalValue = " << originalValue << ' ' << value;
        break;
    case DAY_OF_MONTH:
        originalValue = (opt == this->OP_SET) ? 0 : date.GetDate();
        newDate.SetDate(originalValue + value);
        break;
    case DAY_OF_YEAR:
        originalValue = (opt == this->OP_SET) ? 0 : date.GetDateOfYear();
        newDate.SetDateOfYear(originalValue + value);
        break;
    case DAY_OF_WEEK:
        originalValue = (opt == this->OP_SET) ? 0 : date.GetDateOfWeek();
        newDate.SetDateOfWeek(originalValue + value);
        break;
    case HOUR_OF_DAY:
        originalValue = (opt == this->OP_SET) ? 0 : date.GetHours();
        newDate.SetHours(originalValue + value);
        break;
    case MINUS:
        originalValue = (opt == this->OP_SET) ? 0 : date.GetMinutes();
        newDate.SetMinutes(originalValue + value);
        break;
    case SECOND:
        originalValue = (opt == this->OP_SET) ? 0 : date.GetSeconds();
        newDate.SetSeconds(originalValue + value);
        break;
    case MILLISECOND:
        originalValue = (opt == this->OP_SET) ? 0 : date.GetMilliseconds();
        newDate.SetMilliseconds(originalValue + value);
        break;
    default:
        break;
    }
    return newDate;
}

std::string KRCalendarModule::CurrentTimestamp(const KRAnyValue &params) {
    return std::to_string(util::Date().Now());
}

std::string KRCalendarModule::GetField(const KRAnyValue &params) {
    // KLOG_INFO(TAG) << "getField: " << params.ToStringChecked();
    cJSON *paramObj = cJSON_Parse(params->toString().data());
    cJSON *dateMillisPtr = cJSON_GetObjectItemCaseSensitive(paramObj, this->PARAM_TIME_MILLIS);
    std::int64_t dateMillis = (dateMillisPtr != nullptr) ? static_cast<std::int64_t>(dateMillisPtr->valuedouble) : 0;
    util::Date date = util::Date(dateMillis);
    cJSON *operationPtr = cJSON_GetObjectItemCaseSensitive(paramObj, this->PARAM_OPERATIONS);
    if (operationPtr != nullptr) {
        cJSON *opsObj = cJSON_Parse(operationPtr->valuestring);
        int opsSize = cJSON_GetArraySize(opsObj);
        for (auto i = 0; i < opsSize; i++) {
            cJSON *valuePtr = cJSON_GetArrayItem(opsObj, i);
            if (valuePtr != nullptr) {
                date = this->CalDate(date, valuePtr->valuestring);
            }
        }
        cJSON_Delete(opsObj);
    }
    date.GetTime();  // set/add操作后，可能日期会溢出。比如month=35，second = -5，需要重新mktime一下
    cJSON *fieldPtr = cJSON_GetObjectItemCaseSensitive(paramObj, this->PARAM_FIELD);
    int field = (fieldPtr != nullptr) ? static_cast<int>(fieldPtr->valuedouble) : 0;
    // KLOG_INFO(TAG) << "getField: fileld = " << field;
    cJSON_Delete(paramObj);
    switch (field) {
    case YEAR:
        return std::to_string(date.GetFullYear());
    case MONTH:
        return std::to_string(date.GetMonth());
    case DAY_OF_MONTH:
        return std::to_string(date.GetDate());
    case DAY_OF_YEAR:
        return std::to_string(date.GetDateOfYear());
    case DAY_OF_WEEK:
        return std::to_string(date.GetDateOfWeek());
    case HOUR_OF_DAY:
        return std::to_string(date.GetHours());
    case MINUS:
        return std::to_string(date.GetMinutes());
    case SECOND:
        return std::to_string(date.GetSeconds());
    case MILLISECOND:
        return std::to_string(date.GetMilliseconds());
    default:
        break;
    }
    return "";
}

std::string KRCalendarModule::GetTimeMillis(const KRAnyValue &params) {
    // KLOG_INFO(TAG) << "getTimeMillis: " << params.ToStringChecked();
    cJSON *paramObj = cJSON_Parse(params->toString().data());
    cJSON *dateMillisPtr = cJSON_GetObjectItemCaseSensitive(paramObj, this->PARAM_TIME_MILLIS);
    std::int64_t dateMillis = (dateMillisPtr != nullptr) ? static_cast<std::int64_t>(dateMillisPtr->valuedouble) : 0;
    util::Date date = util::Date(dateMillis);
    // KLOG_INFO(TAG) << "startTime" << date.GetTime();
    cJSON *operationPtr = cJSON_GetObjectItemCaseSensitive(paramObj, this->PARAM_OPERATIONS);
    // KLOG_INFO(TAG) << "getTimeMillis:  operation = " << operationPtr->valuestring;
    if (operationPtr != nullptr) {
        cJSON *opsObj = cJSON_Parse(operationPtr->valuestring);
        auto opsSize = cJSON_GetArraySize(opsObj);
        // KLOG_INFO(TAG) << "getTimeMillis: opsSize = " << opsSize;
        for (auto i = 0; i < opsSize; i++) {
            cJSON *valuePtr = cJSON_GetArrayItem(opsObj, i);
            if (valuePtr != nullptr) {
                date = this->CalDate(date, valuePtr->valuestring);
            }
        }
        cJSON_Delete(opsObj);
    }
    cJSON_Delete(paramObj);
    // KLOG_INFO(TAG) << date.GetTime();
    return std::to_string(date.GetTime());
}

std::string KRCalendarModule::ZeroPadded(int num, int digits) {
    std::string s = "0000" + std::to_string(num);
    return s.substr(s.length() - digits);
}

std::string KRCalendarModule::Format(const KRAnyValue &params) {
    // KLOG_INFO(TAG) << "format: " << params.ToStringChecked();
    cJSON *paramObj = cJSON_Parse(params->toString().data());
    cJSON *dateMillisPtr = cJSON_GetObjectItemCaseSensitive(paramObj, this->PARAM_TIME_MILLIS);
    std::int64_t dateMillis = (dateMillisPtr != nullptr) ? static_cast<std::int64_t>(dateMillisPtr->valuedouble) : 0;
    util::Date date = util::Date(dateMillis);
    cJSON *formatPtr = cJSON_GetObjectItemCaseSensitive(paramObj, this->PARAM_FORMAT);
    std::string formatStr = (formatPtr != nullptr) ? formatPtr->valuestring : "";
    cJSON_Delete(paramObj);
    std::string result = formatStr;

    this->Replace(result, "yyyy", this->ZeroPadded(date.GetFullYear(), 4));
    this->Replace(result, "YYYY", this->ZeroPadded(date.GetFullYear(), 4));
    this->Replace(result, "MM",
                  this->ZeroPadded(date.GetMonth() + 1, 2));  // 格式化输出时，要把date的月份调整为正常月份计数，从1开始
    this->Replace(result, "dd", this->ZeroPadded(date.GetDate(), 2));
    this->Replace(result, "HH", this->ZeroPadded(date.GetHours(), 2));
    this->Replace(result, "mm", this->ZeroPadded(date.GetMinutes(), 2));
    this->Replace(result, "ss", this->ZeroPadded(date.GetSeconds(), 2));
    this->Replace(result, "SSS", this->ZeroPadded(date.GetMilliseconds(), 3));
    //  输出的已格式化字符串，要剔除格式字符串中的转义字符(')、('')。
    std::string convertResult = "";
    auto length = result.length();
    for (std::string::size_type i = 0; i < length; ++i) {
        if (i + 1 < length && result.at(i) == '\'' && result.at(i + 1) == '\'') {  // 如果遇到"''"，则替换为"'"
            convertResult += '\'';
            ++i;  // 跳过下一个单引号
        } else if (result.at(i) != '\'') {
            convertResult += result.at(i);  //  如果遇到单个的"'"，则删除（即不添加到结果字符串中）
        }
    }
    // KLOG_INFO(TAG) << "format: " << result;
    return convertResult;
}

std::string KRCalendarModule::Parse(const KRAnyValue &params) {
    // KLOG_INFO(TAG) << "parse: " << params.ToStringChecked();
    cJSON *paramObj = cJSON_Parse(params->toString().data());
    cJSON *dateStrPtr = cJSON_GetObjectItemCaseSensitive(paramObj, this->PARAM_FORMATTED_TIME);
    std::string dateStr = (dateStrPtr != nullptr) ? dateStrPtr->valuestring : "";
    // KLOG_INFO(TAG) << "parse: " << dateStr;
    cJSON *formatStrPtr = cJSON_GetObjectItemCaseSensitive(paramObj, this->PARAM_FORMAT);
    std::string formatStr = (formatStrPtr != nullptr) ? formatStrPtr->valuestring : "";
    cJSON_Delete(paramObj);
    // Only supports date and format consistency
    util::Date date = util::Date();
    date.Parse(dateStr, formatStr);

    // KLOG_INFO(TAG) << "parse: " << std::to_string(date.GetTime());
    return std::to_string(date.GetTime());
}

void KRCalendarModule::Replace(std::string &str, const char *format, std::string substr) {
    auto pos = str.find(format);
    if (pos != std::string::npos) {
        str.replace(pos, strlen(format), substr);
    }
}

}  // namespace module
}  // namespace kuikly

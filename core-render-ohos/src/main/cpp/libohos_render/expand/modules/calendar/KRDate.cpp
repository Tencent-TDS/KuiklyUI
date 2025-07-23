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

#include "KRDate.h"

#include <chrono>

namespace kuikly {
namespace util {

Date::Date() {
    std::chrono::milliseconds msTime =
        std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch());
    std::int64_t timestamp = msTime.count();
    time_t timeSec = static_cast<time_t>(timestamp / 1000);
    localtime_r(&timeSec, &time);
    millis = static_cast<int>(timestamp % 1000);
}

Date::Date(std::int64_t timestamp) {
    time_t timeSec = static_cast<time_t>(timestamp / 1000);
    localtime_r(&timeSec, &time);
    millis = static_cast<int>(timestamp % 1000);
}

Date::Date(const Date &other) {
    time = other.time;
    millis = other.millis;
}

Date &Date::operator=(const Date &other) {
    time = other.time;
    millis = other.millis;
    return *this;
}

int Date::GetFullYear() {
    return time.tm_year + 1900;
}
int Date::GetMonth() {
    return time.tm_mon;
}
int Date::GetDate() {
    return time.tm_mday;
}
int Date::GetDateOfYear() {
    return time.tm_yday + 1;
}  // tm_day从0开始，返回值对标跨端层yday从1开始
int Date::GetDateOfWeek() {
    return time.tm_wday + 1;
}  // tm_wday从0开始，返回值对标跨端层wday从1开始
int Date::GetHours() {
    return time.tm_hour;
}
int Date::GetMinutes() {
    return time.tm_min;
}
int Date::GetSeconds() {
    return time.tm_sec;
}
int Date::GetMilliseconds() {
    return millis;
}

void Date::SetYear(int year) {
    time.tm_year = year - 1900;
}
void Date::SetFullYear(int year) {
    time.tm_year = year - 1900;
}
void Date::SetMonth(int month) {
    time.tm_mon = month;
}
void Date::SetDate(int date) {
    time.tm_mday = date;
}
void Date::SetDateOfYear(int date) {
    time.tm_yday = date - 1;
}  // 跨端层输入yday从1开始
void Date::SetDateOfWeek(int date) {
    time.tm_wday = date - 1;
}  // 跨端层输入wday从1开始
void Date::SetHours(int hour) {
    time.tm_hour = hour;
}
void Date::SetMinutes(int min) {
    time.tm_min = min;
}
void Date::SetSeconds(int sec) {
    time.tm_sec = sec;
}
void Date::SetMilliseconds(int num) {
    time.tm_sec += num / 1000;  // 溢出处理。如果设置的毫秒数超过1000，则要进位到秒
    millis = num % 1000;
}

std::int64_t Date::GetTime() {
    auto timeSec = mktime(&time);
    return static_cast<std::int64_t>(timeSec) * 1000 + millis;
}
std::int64_t Date::Now() {
    return this->GetTime();
}

void Date::Parse(std::string &dateStr, std::string &formatStr) {
    auto pos = formatStr.find("yyyy");
    if (pos != std::string::npos) {
        // 已格式化的日期字符串不包含引号，要减去格式字符串中的引号数量，以便找到该字段正确位置。
        pos -= quoteCount(formatStr.substr(0, pos));
        int year = std::stoi(dateStr.substr(pos, 4));
        this->SetYear(year);
    }
    pos = formatStr.find("YYYY");
    if (pos != std::string::npos) {
        pos -= quoteCount(formatStr.substr(0, pos));
        int year = std::stoi(dateStr.substr(pos, 4));
        this->SetYear(year);
    }
    pos = formatStr.find("MM");
    if (pos != std::string::npos) {
        pos -= quoteCount(formatStr.substr(0, pos));
        int month = std::stoi(dateStr.substr(pos, 2)) - 1;  // 解析时，要将正常月份计数调整为从0开始
        this->SetMonth(month);
    }
    pos = formatStr.find("dd");
    if (pos != std::string::npos) {
        pos -= quoteCount(formatStr.substr(0, pos));
        int date = std::stoi(dateStr.substr(pos, 2));
        this->SetDate(date);
    }
    pos = formatStr.find("HH");
    if (pos != std::string::npos) {
        pos -= quoteCount(formatStr.substr(0, pos));
        int hour = std::stoi(dateStr.substr(pos, 2));
        this->SetHours(hour);
    }
    pos = formatStr.find("mm");
    if (pos != std::string::npos) {
        pos -= quoteCount(formatStr.substr(0, pos));
        int minute = std::stoi(dateStr.substr(pos, 2));
        this->SetMinutes(minute);
    }
    pos = formatStr.find("ss");
    if (pos != std::string::npos) {
        pos -= quoteCount(formatStr.substr(0, pos));
        int second = std::stoi(dateStr.substr(pos, 2));
        this->SetSeconds(second);
    }
    pos = formatStr.find("SSS");
    if (pos != std::string::npos) {
        pos -= quoteCount(formatStr.substr(0, pos));
        int num = std::stoi(dateStr.substr(pos, 3));
        this->SetMilliseconds(num);
    }
}

/**
 * 计算当前时间字段位置所对应的格式字符串之前的字符子串所包含的"'"和"''"数量。
 * 对齐安卓的日期和时间格式模式字符串约定。即：在日期和时间模式字符串中，未加引号的字母 'A' 到 'Z' 和 'a' 到 'z'
 * 被解释为模式字母，用来表示日期或时间字符串元素。 文本可以使用单引号 (') 引起来，以免进行解释。"''"
 * 表示单引号。所有其他字符均不解释；只是在格式化时将它们简单复制到输出字符串，或者在解析时与输入字符串进行匹配。
 * @param subString 当前时间字段位置之前的子串
 * @return
 */
std::string::size_type Date::quoteCount(std::string subString) {
    std::string::size_type count = 0;
    auto length = subString.length();
    for (std::string::size_type i = 0; i < length; i++) {
        if (subString.at(i) == '\'') {
            if ((i + 1) < length && subString.at(i + 1) == '\'') {
                count++;  //  双引号计数。'' is treated as a single quote regardless of being in a quoted section.
                i++;
                continue;
            }
            count++;  //  单引号计数
        }
    }
    return count;
}

}  //  namespace util
}  //  namespace kuikly

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

#include <ctime>
#include <string>

namespace kuikly {
namespace util {

// tm does not support milliseconds
class Date {
 public:
    Date();

    explicit Date(std::int64_t timestamp);

    Date(const Date &other);

    Date &operator=(const Date &other);

    int GetFullYear();
    int GetMonth();
    int GetDate();
    int GetDateOfYear();
    int GetDateOfWeek();
    int GetHours();
    int GetMinutes();
    int GetSeconds();
    int GetMilliseconds();
    std::int64_t GetTime();

    void SetYear(int year);
    void SetFullYear(int year);
    void SetMonth(int month);
    void SetDate(int date);
    void SetDateOfYear(int date);
    void SetDateOfWeek(int date);
    void SetHours(int hour);
    void SetMinutes(int min);
    void SetSeconds(int sec);
    void SetMilliseconds(int num);

    std::int64_t Now();

    void Parse(std::string &dateStr, std::string &formatStr);
    std::string::size_type quoteCount(std::string subString);

 private:
    tm time;
    int millis;
};

}  //  namespace util
}  //  namespace kuikly

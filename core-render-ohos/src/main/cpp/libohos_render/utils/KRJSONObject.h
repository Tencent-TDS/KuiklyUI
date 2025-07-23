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

#ifndef CORE_RENDER_OHOS_KRJSONOBJECT_H
#define CORE_RENDER_OHOS_KRJSONOBJECT_H
#include <string>

namespace kuikly {
namespace util {
class JSONObject : public std::enable_shared_from_this<JSONObject> {
 public:
    static std::shared_ptr<JSONObject> Parse(const std::string &str);

    explicit JSONObject(void *cjson, std::shared_ptr<JSONObject> owner = nullptr);
    ~JSONObject();

    std::string GetString(const std::string &key, const std::string &default_value = "");
    std::vector<std::string> GetStringArray(const std::string &key);

    double GetNumber(const std::string &key, const double default_value = 0);
    std::vector<double> GetNumberArray(const std::string &key);

    std::shared_ptr<JSONObject> GetArrayItem(int index);
    int GetArraySize();

    std::shared_ptr<JSONObject> GetObjectItem(const std::string &key);

 private:
    void *cjson_ = nullptr;
    std::shared_ptr<JSONObject> owner_ = nullptr;
};

}  // end namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRJSONOBJECT_H

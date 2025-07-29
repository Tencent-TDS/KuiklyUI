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

#include "libohos_render/expand/modules/back_press/KRBackPressModule.h"

#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/utils/KRJSONObject.h"
#include "libohos_render/utils/KRStringUtil.h"

namespace kuikly {
namespace module {

const char KRBackPressModule::MODULE_NAME[] = "KRBackPressModule";
const char KRBackPressModule::METHOD_BACK_HANDLE[] = "backHandle";

KRAnyValue KRBackPressModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                   const KRRenderCallback &callback) {
    if (kuikly::util::isEqual(method, METHOD_BACK_HANDLE)) {
        BackHandle(params);
    }
    return KREmptyValue();
}

void KRBackPressModule::BackHandle(const KRAnyValue &params) {
    auto jsonObj = kuikly::util::JSONObject::Parse(params->toString());
    is_back_consumed.store(jsonObj->GetNumber("consumed", 0) == 1);
    back_consumed_time.store(std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count());
}

}
}

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

#include "libohos_render/export/IKRRenderModuleExport.h"

namespace kuikly {
namespace module {
class KRCodecModule : public IKRRenderModuleExport {
 public:
    static const char MODULE_NAME[];

    bool SyncMode();
    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback) override;
    void OnDestroy() override;

 private:
    static const char METHOD_URL_DECODE[];
    static const char METHOD_URL_ENCODE[];
    static const char METHOD_BASE64_ENCODE[];
    static const char METHOD_BASE64_DECODE[];
    static const char METHOD_MD5[];
    static const char METHOD_SHA256[];

    KRAnyValue UrlEncode(std::string);
    KRAnyValue UrlDecode(std::string);
    KRAnyValue Base64Encode(std::string);
    KRAnyValue Base64Decode(std::string);
    KRAnyValue Md5(std::string);
    KRAnyValue Sha256(std::string);
};
}  // namespace module
}  // namespace kuikly

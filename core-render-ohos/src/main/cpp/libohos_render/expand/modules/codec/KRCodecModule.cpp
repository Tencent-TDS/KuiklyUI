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

#include "KRCodecModule.h"

#include "libohos_render/expand/modules/codec/KRCodec.h"

namespace kuikly {
namespace module {

const char KRCodecModule::MODULE_NAME[] = "KRCodecModule";
const char KRCodecModule::METHOD_URL_DECODE[] = "urlDecode";
const char KRCodecModule::METHOD_URL_ENCODE[] = "urlEncode";
const char KRCodecModule::METHOD_BASE64_ENCODE[] = "base64Encode";
const char KRCodecModule::METHOD_BASE64_DECODE[] = "base64Decode";
const char KRCodecModule::METHOD_MD5[] = "md5";
const char KRCodecModule::METHOD_SHA256[] = "sha256";

bool KRCodecModule::SyncMode() {
    return true;
}
void KRCodecModule::OnDestroy() {}
KRAnyValue KRCodecModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                     const KRRenderCallback &callback) {
    auto str = params->toString();
    if (method == this->METHOD_URL_ENCODE) {
        return this->UrlEncode(str);
    } else if (method == this->METHOD_URL_DECODE) {
        return this->UrlDecode(str);
    } else if (method == METHOD_BASE64_ENCODE) {
        return this->Base64Encode(str);
    } else if (method == METHOD_BASE64_DECODE) {
        return this->Base64Decode(str);
    } else if (method == METHOD_MD5) {
        return this->Md5(str);
    } else if (method == METHOD_SHA256) {
        return this->Sha256(str);
    }
    return std::make_shared<KRRenderValue>();
}

KRAnyValue KRCodecModule::UrlEncode(const std::string str) {
    return std::make_shared<KRRenderValue>(KREncodeURLComponent(str));
}

KRAnyValue KRCodecModule::UrlDecode(const std::string str) {
    return std::make_shared<KRRenderValue>(KRDecodeURLComponent(str));
}

KRAnyValue KRCodecModule::Base64Encode(const std::string str) {
    return std::make_shared<KRRenderValue>(KRBase64Encode(str));
}

KRAnyValue KRCodecModule::Base64Decode(const std::string str) {
    return std::make_shared<KRRenderValue>(KRBase64Decode(str));
}

KRAnyValue KRCodecModule::Md5(const std::string str) {
    return std::make_shared<KRRenderValue>(KRMd5(str));
}

KRAnyValue KRCodecModule::Sha256(const std::string str) {
    return std::make_shared<KRRenderValue>(KRSha256(str));
}
}  // namespace module
}  // namespace kuikly

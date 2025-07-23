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

#include "KRCodec.h"

#include <iomanip>
#include <sstream>
#include "md5.h"
#include "sha256.h"

#define MD5_DIGEST_LENGTH 16
static const char *TAG = __FILE_NAME__;
namespace kuikly {
inline namespace model_util {
const char HEX_DIGITS[] = "0123456789abcdef";
// Maps integer in the range [0,16) to a hex digit.

const char HEX_DIGITS_URI[] = "0123456789ABCDEF";
// RFC 3986 section 2.1 says "For consistency, URI producers and normalizers should use uppercase
// hexadecimal digits for all percent-encodings.

const char base64_chars[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                            "abcdefghijklmnopqrstuvwxyz"
                            "0123456789+/";
// Maps integer in the range [0,16) to a hex digit.

std::string KREncodeURLComponent(const std::string &in) {
    std::string out;
    for (auto &b : in) {
        if (('A' <= b && b <= 'Z') || ('a' <= b && b <= 'z') || ('0' <= b && b <= '9') || b == '-' || b == '_' ||
            b == '.' || b == '!' || b == '~' || b == '*' || b == '\'' || b == '(' || b == ')') {
            out.push_back(b);
        } else {
            out.push_back('%');
            out.push_back(HEX_DIGITS_URI[b / 16]);
            out.push_back(HEX_DIGITS_URI[b % 16]);
        }
    }
    return out;
}

std::string KRDecodeURLComponent(const std::string &in) {
    std::string res;
    for (size_t i = 0; i < in.size(); ++i) {
        if (in[i] == '%' && i + 2 < in.size()) {
            char d1 = in[i + 1];
            char d2 = in[i + 2];
            if (std::isxdigit(d1) && std::isxdigit(d2)) {
                char b = (std::isdigit(d1) ? (d1 - '0') : (std::toupper(d1) - 'A' + 10)) << 4;
                b |= (std::isdigit(d2) ? (d2 - '0') : (std::toupper(d2) - 'A' + 10));
                res.push_back(b);
                i += 2;
            } else {
                res.push_back(in[i]);
            }
        } else {
            res.push_back(in[i]);
        }
    }
    return res;
}

std::string KRBase64Encode(const std::string &in) {
    std::string out;
    int val = 0, valb = -6;
    for (unsigned char c : in) {
        val = (val << 8) + c;
        valb += 8;
        while (valb >= 0) {
            out.push_back(base64_chars[(val >> valb) & 0x3F]);
            valb -= 6;
        }
    }
    if (valb > -6)
        out.push_back(base64_chars[((val << 8) >> (valb + 8)) & 0x3F]);
    while (out.size() % 4)
        out.push_back('=');
    return out;
}

std::string KRBase64Encode(const std::string_view in) {
    std::string out;
    int val = 0, valb = -6;
    for (unsigned char c : in) {
        val = (val << 8) + c;
        valb += 8;
        while (valb >= 0) {
            out.push_back(base64_chars[(val >> valb) & 0x3F]);
            valb -= 6;
        }
    }
    if (valb > -6)
        out.push_back(base64_chars[((val << 8) >> (valb + 8)) & 0x3F]);
    while (out.size() % 4)
        out.push_back('=');
    return out;
}

std::string KRBase64Decode(const std::string &in) {
    std::string out;
    std::vector<int> T(256, -1);
    for (int i = 0; i < 64; i++)
        T[base64_chars[i]] = i;
    int val = 0, valb = -8;
    for (unsigned char c : in) {
        if (T[c] == -1)
            break;
        val = (val << 6) + T[c];
        valb += 6;
        if (valb >= 0) {
            out.push_back(static_cast<char>((val >> valb) & 0xFF));
            valb -= 8;
        }
    }
    return out;
}

std::string KRMd5(const std::string &in) {
    unsigned char md[MD5_DIGEST_LENGTH];
    MD5_CTX md5c;
    MD5_Init(&md5c);
    MD5_Update(&md5c, in.data(), in.size());
    MD5_Final(md, &md5c);
    std::ostringstream out;
    for (int i = 0; i < MD5_DIGEST_LENGTH; ++i) {
        out << std::hex << std::setw(2) << std::setfill('0') << static_cast<int>(md[i]);
    }
    return out.str().substr(8, 16);
}

std::string KRSha256(const std::string &in) {
    uint8_t digest[SHA256_DIGEST_SIZE];
    SHA256_hash(in.data(), in.size(), digest);
    std::ostringstream out;
    for (int i = 0; i < SHA256_DIGEST_SIZE; ++i) {
        out << std::hex << std::setw(2) << std::setfill('0') << static_cast<int>(digest[i]);
    }
    return out.str();
}
}  //  namespace util
}  //  namespace kuikly

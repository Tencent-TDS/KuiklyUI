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

#include "libohos_render/utils/KRURIHelper.h"

#include <filemanagement/file_uri/oh_file_uri.h>
#include <mutex>

KRURIHelper *KRURIHelper::GetInstance() {
    static KRURIHelper *instance_ = nullptr;
    static std::once_flag flag;
    std::call_once(flag, []() { instance_ = new KRURIHelper(); });
    return instance_;
}

std::string KRURIHelper::URIForResFile(const std::string &str, const std::string &resfile_dir) {
    if (uriPrefix_ == nullptr) {
        FileManagement_ErrCode err = OH_FileUri_GetUriFromPath("", 0, &uriPrefix_);
        if (err != FileManagement_ErrCode::ERR_OK) {
            // anything we can do?
            return str;
        }
        uriPrefixLen_ = strlen(uriPrefix_);
    }
    return std::string(uriPrefix_, uriPrefix_ + uriPrefixLen_) + resfile_dir + "/" + str;
}

KRURIHelper::KRURIHelper() : uriPrefix_(nullptr), uriPrefixLen_(0) {
    // blank
}

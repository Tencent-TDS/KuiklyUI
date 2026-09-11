/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_KRNAPIHANDLESCOPE_H
#define CORE_RENDER_OHOS_KRNAPIHANDLESCOPE_H

#include "napi/native_api.h"

// RAII wrapper so napi_open_handle_scope is always paired with
// napi_close_handle_scope, including early-return paths.
class KRNapiHandleScope {
 public:
    explicit KRNapiHandleScope(napi_env env) : env_(env), scope_(nullptr), opened_(false) {
        if (env_ != nullptr && napi_open_handle_scope(env_, &scope_) == napi_ok && scope_ != nullptr) {
            opened_ = true;
        }
    }

    KRNapiHandleScope(const KRNapiHandleScope &) = delete;
    KRNapiHandleScope &operator=(const KRNapiHandleScope &) = delete;

    ~KRNapiHandleScope() {
        if (opened_ && env_ != nullptr && scope_ != nullptr) {
            napi_close_handle_scope(env_, scope_);
        }
    }

 private:
    napi_env env_;
    napi_handle_scope scope_;
    bool opened_;
};

#endif  // CORE_RENDER_OHOS_KRNAPIHANDLESCOPE_H

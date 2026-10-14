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

#ifndef CORE_RENDER_OHOS_KRRENDERVALUE_NAPI_H
#define CORE_RENDER_OHOS_KRRENDERVALUE_NAPI_H

#include <js_native_api.h>
#include <js_native_api_types.h>

#include "libohos_render/foundation/type/KRRenderValue.h"

namespace kuikly {
namespace util {

struct NapiValue {
    NapiValue() = default;
    NapiValue(napi_env e, napi_value v) : env(e), value(v) {}

    napi_env env = nullptr;
    napi_value value = nullptr;

    /** Walk this ArkTS value into a KRJSON tree. */
    KRRenderValue ToRenderValue() const;
    /** Store {env, value} as a kTagExt sidecar (PixelMap / drawable). */
    KRRenderValue ToOpaqueRenderValue() const;

    /** Encode a render value as an ArkTS value (objects/arrays stay wrapped). */
    static NapiValue FromRenderValue(napi_env env, const KRRenderValue &render_value,
                                     napi_status *status = nullptr);
    /** Read back a kTagExt sidecar. */
    static NapiValue RawHandle(const KRRenderValue &render_value);
};

napi_value WrapKRJSON(napi_env env, KRJSON value);
bool TryUnwrapKRJSON(napi_env env, napi_value value, KRJSON *out);
napi_value TakeKRJSONRoutePayload(napi_env env, napi_value token);

}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRRENDERVALUE_NAPI_H

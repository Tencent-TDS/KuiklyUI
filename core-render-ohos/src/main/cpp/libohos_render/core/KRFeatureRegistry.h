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

#ifndef CORE_RENDER_OHOS_KRFEATUREREGISTRY_H
#define CORE_RENDER_OHOS_KRFEATUREREGISTRY_H

#include <string>
#include <type_traits>
#include <utility>

#include "libohos_render/core/KRRenderFactories.h"
#include "libohos_render/foundation/type/KRRenderValue.h"
#include "thirdparty/cJSON/cJSON.h"

namespace kuikly {
namespace features {

template <typename T, typename = void>
struct HasCreateMode : std::false_type {};

template <typename T>
struct HasCreateMode<T, std::void_t<decltype(T::kMode), decltype(T::CreateMode())>> : std::true_type {};

template <typename T, typename = void>
struct HasCreateLayer : std::false_type {};

template <typename T>
struct HasCreateLayer<T, std::void_t<decltype(T::CreateRenderLayer(
                             std::declval<std::weak_ptr<IKRRenderView>>(),
                             std::declval<std::shared_ptr<KRRenderContextParams>>(),
                             std::declval<std::shared_ptr<KRUIScheduler>>()))>> : std::true_type {};

template <typename T, typename = void>
struct HasCreateContextHandler : std::false_type {};

template <typename T>
struct HasCreateContextHandler<
    T, std::void_t<decltype(T::CreateContextHandler(std::declval<const std::shared_ptr<KRRenderContextParams> &>()))>>
    : std::true_type {};

template <typename T, typename = void>
struct HasRegisterModules : std::false_type {};

template <typename T>
struct HasRegisterModules<T, std::void_t<decltype(T::RegisterModules())>> : std::true_type {};

template <typename T, typename = void>
struct HasPreparePageData : std::false_type {};

template <typename T>
struct HasPreparePageData<
    T, std::void_t<decltype(T::PreparePageDataForKotlin(std::declval<cJSON *>(),
                                                        std::declval<const std::shared_ptr<IKRRenderLayer> &>()))>>
    : std::true_type {};

template <typename T, typename = void>
struct HasShouldPreparePageData : std::false_type {};

template <typename T>
struct HasShouldPreparePageData<
    T, std::void_t<decltype(T::ShouldPreparePageData(std::declval<const std::shared_ptr<KRRenderValue> &>(),
                                                     std::declval<const std::shared_ptr<IKRRenderLayer> &>()))>>
    : std::true_type {};

template <typename F>
bool TryCreateMode(int mode, std::shared_ptr<KRRenderExecuteMode> &out) {
    if constexpr (HasCreateMode<F>::value) {
        if (F::kMode == mode) {
            out = F::CreateMode();
            return true;
        }
    }
    return false;
}

template <typename F>
bool TryCreateLayer(std::weak_ptr<IKRRenderView> render_view, std::shared_ptr<KRRenderContextParams> context,
                    std::shared_ptr<KRUIScheduler> ui_scheduler, std::shared_ptr<IKRRenderLayer> &out) {
    if constexpr (HasCreateLayer<F>::value) {
        if (auto layer = F::CreateRenderLayer(render_view, context, ui_scheduler)) {
            out = std::move(layer);
            return true;
        }
    }
    return false;
}

template <typename F>
bool TryCreateContextHandler(int mode, const std::shared_ptr<KRRenderContextParams> &context,
                             std::shared_ptr<IKRRenderNativeContextHandler> &out) {
    if constexpr (HasCreateContextHandler<F>::value) {
        if constexpr (HasCreateMode<F>::value) {
            if (F::kMode != mode) {
                return false;
            }
        }
        out = F::CreateContextHandler(context);
        return static_cast<bool>(out);
    }
    return false;
}

template <typename F>
void TryRegisterModules() {
    if constexpr (HasRegisterModules<F>::value) {
        F::RegisterModules();
    }
}

template <typename F>
bool TryShouldPreparePageData(const std::shared_ptr<KRRenderValue> &page_data,
                             const std::shared_ptr<IKRRenderLayer> &layer) {
    if constexpr (HasShouldPreparePageData<F>::value) {
        return F::ShouldPreparePageData(page_data, layer);
    }
    if constexpr (HasPreparePageData<F>::value) {
        return true;
    }
    return false;
}

template <typename F>
void TryPreparePageData(cJSON *page_data, const std::shared_ptr<IKRRenderLayer> &layer) {
    if constexpr (HasPreparePageData<F>::value) {
        F::PreparePageDataForKotlin(page_data, layer);
    }
}

template <typename... Features>
struct KRFeatureRegistry {
    static std::shared_ptr<KRRenderExecuteMode> CreateExecuteMode(int mode) {
        std::shared_ptr<KRRenderExecuteMode> result;
        (void)((TryCreateMode<Features>(mode, result) || ...));
        return result;
    }

    static std::shared_ptr<IKRRenderLayer> CreateRenderLayer(std::weak_ptr<IKRRenderView> render_view,
                                                             std::shared_ptr<KRRenderContextParams> context,
                                                             std::shared_ptr<KRUIScheduler> ui_scheduler) {
        std::shared_ptr<IKRRenderLayer> result;
        (void)((TryCreateLayer<Features>(render_view, context, ui_scheduler, result) || ...));
        return result;
    }

    static std::shared_ptr<IKRRenderNativeContextHandler> CreateContextHandler(
        int mode, const std::shared_ptr<KRRenderContextParams> &context) {
        std::shared_ptr<IKRRenderNativeContextHandler> result;
        (void)((TryCreateContextHandler<Features>(mode, context, result) || ...));
        return result;
    }

    static void RegisterModules() {
        (TryRegisterModules<Features>(), ...);
    }

    static std::shared_ptr<KRRenderValue> PreparePageDataForKotlin(const std::shared_ptr<KRRenderValue> &page_data,
                                                                  const std::shared_ptr<IKRRenderLayer> &layer) {
        if (!page_data) {
            return page_data;
        }
        if (!(TryShouldPreparePageData<Features>(page_data, layer) || ...)) {
            return page_data;
        }
        const std::string page_data_json = page_data->toString();
        cJSON *page_data_obj = cJSON_Parse(page_data_json.c_str());
        if (page_data_obj != nullptr && cJSON_IsObject(page_data_obj)) {
            (TryPreparePageData<Features>(page_data_obj, layer), ...);
            if (char *json = cJSON_PrintUnformatted(page_data_obj)) {
                auto result = KRRenderValue::Make(std::string(json));
                cJSON_free(json);
                cJSON_Delete(page_data_obj);
                return result;
            }
            cJSON_Delete(page_data_obj);
            return page_data;
        }
        if (page_data_obj != nullptr) {
            cJSON_Delete(page_data_obj);
        }
        return page_data;
    }
};

}  // namespace features
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRFEATUREREGISTRY_H

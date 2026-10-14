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
#include <ark_runtime/jsvm.h>
#include <arkui/native_node_napi.h>
#include <cstdint>
#include <string_view>
#include "libohos_render/expand/modules/back_press/KRBackPressModule.h"
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/foundation/KRCallbackData.h"
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/manager/KRArkTSManager.h"
#include "libohos_render/manager/KRRenderManager.h"
#include "libohos_render/foundation/type/KRRenderValueNapi.h"
#include "libohos_render/utils/NAPIUtil.h"
#include "napi/native_api.h"

using kuikly::util::NapiValue;
using kuikly::util::TakeKRJSONRoutePayload;
using kuikly::util::TryUnwrapKRJSON;
using kuikly::util::WrapKRJSON;

//  ArkTs层页面加载事件
static napi_value OnLaunchStart(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "napi_get_cb_info error");
        return 0;
    }
    std::string instance_id = kuikly::util::getNApiArgsAsciiStdString(env, args[0]);
    KRRenderManager::GetInstance().OnLaunchStart(instance_id);
    return 0;
}
static napi_value UpdateConfig(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value args[2] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "napi_get_cb_info error");
        return 0;
    }
    std::string instance_id = kuikly::util::getNApiArgsAsciiStdString(env, args[0]);
    auto config_json = NapiValue(env, args[1]).ToRenderValue();
    if (auto renderView = KRRenderManager::GetInstance().GetRenderView(instance_id)) {
        if (auto ctx = renderView->GetContext()) {
            ctx->Config()->Update(config_json);
        } else {
            KR_LOG_ERROR << "Config update failed, context null";
        }
    } else {
        KR_LOG_ERROR << "Config update failed, no render view";
    }
    return 0;
}

// 初始化render view
static napi_value OnInitRenderView(napi_env env, napi_callback_info info) {
    // args: instance_id, page_name, context_code, execute_mode, page_data, width, height, config_json, uiContext, resourceManager
    size_t argc = 10;
    napi_value args[10] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "napi_get_cb_info error");
        return 0;
    }
    auto instance_id = NapiValue(env, args[0]).ToRenderValue();
    auto page_name = NapiValue(env, args[1]).ToRenderValue();
    std::string context_code = kuikly::util::getNApiArgsStdString(env, args[2]);
    int execute_mode = kuikly::util::getNApiArgsInt(env, args[3]);
    auto page_Data = NapiValue(env, args[4]).ToRenderValue();
    double renderViewWidth = kuikly::util::getNApiArgsDouble(env, args[5]);
    double renderViewHeight = kuikly::util::getNApiArgsDouble(env, args[6]);
    auto config_json = NapiValue(env, args[7]).ToRenderValue();
    std::string instance_id_utf8 = instance_id.toAsciiString();
    auto renderView = KRRenderManager::GetInstance().GetRenderView(instance_id_utf8);
    if (renderView != nullptr) {
        size_t page_data_units = 0;
        if (page_Data.isString()) {
            if (KRJSONGetType(page_Data.jsonValue()) == KRJSON_U16STRING) {
                KRJSONGetStringUTF16(page_Data.jsonValue(), &page_data_units);
            } else {
                KRJSONGetString(page_Data.jsonValue(), &page_data_units);
            }
        }
        if (!page_Data.isMap() && !page_Data.isArray() && (!page_Data.isString() || page_data_units == 0)) {
            page_Data = KRRenderValue::Make(KRRenderValue::Map{});
        }
        auto context = std::make_shared<KRRenderContextParams>(page_name, page_Data, instance_id, config_json,
                                                               context_code, execute_mode);
        ArkUI_ContextHandle context_handle;
        OH_ArkUI_GetContextFromNapiValue(env, args[8], &context_handle);
        NativeResourceManager *native_resources_manager = OH_ResourceManager_InitNativeResourceManager(env, args[9]);
        int64_t launch_time = KRRenderManager::GetInstance().GetLaunchStartTime(instance_id_utf8);
        renderView->Init(context, context_handle, native_resources_manager, renderViewWidth, renderViewHeight,
                         launch_time);
    } else {
        napi_throw_error(env, "-1006", "renderView is nil when get render view");
    }

    return 0;
}

// 销毁render view
static napi_value OnDestroyRenderView(napi_env env, napi_callback_info info) {
    // 1、从info中取出TS传递过来的参数放入args
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "napi_get_cb_info error");
        return 0;
    }
    std::string instanceId = kuikly::util::getNApiArgsAsciiStdString(env, args[0]);
    KRRenderManager::GetInstance().DestroyRenderView(instanceId);
    return 0;
}

// render view size 变化
static napi_value OnRenderViewSizeChanged(napi_env env, napi_callback_info info) {
    // 1、从info中取出TS传递过来的参数放入args
    size_t argc = 3;
    napi_value args[3] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "napi_get_cb_info error");
        return 0;
    }

    std::string instanceId = kuikly::util::getNApiArgsAsciiStdString(env, args[0]);
    double width = kuikly::util::getNApiArgsDouble(env, args[1]);
    double height = kuikly::util::getNApiArgsDouble(env, args[2]);
    auto renderView = KRRenderManager::GetInstance().GetRenderView(instanceId);
    if (renderView != nullptr) {
        renderView->OnRenderViewSizeChanged(width, height);
        napi_value result;
        napi_create_int32(env, 1, &result);
        return result;
    } else {
        napi_throw_error(env, "-1006", "renderView is nil when get render view");
    }
    return 0;
}

static napi_value ArkTSCallNative(napi_env env, napi_callback_info info) {
    // 1、从info中取出TS传递过来的参数放入args
    size_t argc = 8;
    napi_value args[8] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "napi_get_cb_info error");
        return 0;
    }
    KRArkTSManager::GetInstance().HandleArkTSCallNative(env, args, argc);
    return 0;
}

static napi_value ArkTSOnSendEvent(napi_env env, napi_callback_info info) {
    // 1、从info中取出TS传递过来的参数放入args
    size_t argc = 3;
    napi_value args[3] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "ArkTSOnSendEvent napi_get_cb_info error");
        return 0;
    }

    std::string instance_id = kuikly::util::getNApiArgsAsciiStdString(env, args[0]);
    auto event = NapiValue(env, args[1]).ToRenderValue();
    // 结构化 napi 值（Record / Array）直接构建 KRJSON，字符串同样兼容
    auto data = NapiValue(env, args[2]).ToRenderValue();
    auto renderView = KRRenderManager::GetInstance().GetRenderView(instance_id);
    if (renderView != nullptr) {
        renderView->SendEvent(event, data);
        napi_value result;
        napi_create_int32(env, 1, &result);
        return result;
    }
    return 0;
}

static napi_value ArkTSOnSendEventSync(napi_env env, napi_callback_info info) {
    size_t argc = 4;
    napi_value args[4] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "ArkTSOnSendEventSync napi_get_cb_info error");
        return 0;
    }

    std::string instance_id = kuikly::util::getNApiArgsAsciiStdString(env, args[0]);
    auto event = NapiValue(env, args[1]).ToRenderValue();
    auto data = NapiValue(env, args[2]).ToRenderValue();
    bool sync = kuikly::util::getNApiArgsBool(env, args[3]);
    auto renderView = KRRenderManager::GetInstance().GetRenderView(instance_id);
    if (renderView != nullptr) {
        renderView->SendEvent(event, data, sync);
        napi_value result;
        napi_create_int32(env, 1, &result);
        return result;
    }
    return 0;
}
static napi_value CreateNativeRoot(napi_env env, napi_callback_info info) {
    // The API OH_ArkUI_NodeContent_RegisterCallback depends on it
    auto nodeApi = kuikly::util::GetNodeApi();
    KRRenderManager::GetInstance().CreateRenderViewIfNeeded(env, info);
    return nullptr;
}

static napi_value isBackPressConsumed(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value args[2] = {nullptr};
    napi_value result;
    napi_create_int32(env, KRBackPressState::Undefined, &result);
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "napi_get_cb_info error");
        return result;
    }
    std::string instance_id = kuikly::util::getNApiArgsAsciiStdString(env, args[0]);

    auto render_view = KRRenderManager::GetInstance().GetRenderView(instance_id);
    if (render_view != nullptr) {
        std::string back_press_module_name = kuikly::module::KRBackPressModule::MODULE_NAME;
        auto back_press_module = std::dynamic_pointer_cast<kuikly::module::KRBackPressModule>(render_view->GetModule(back_press_module_name));
        if (!back_press_module) {
            return result;
        }
        bool is_back_consumed = back_press_module->is_back_consumed.load();
        if (is_back_consumed) {
            napi_create_int32(env, KRBackPressState::Consumed, &result);
        } else {
            napi_create_int32(env, KRBackPressState::NotConsumed, &result);
        }
    }
    return result;
}

static napi_value IsKRJSON(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    if (napi_get_cb_info(env, info, &argc, args, nullptr, nullptr) != napi_ok) {
        return nullptr;
    }
    KRJSON value = KRJSON_INVALID;
    napi_value result = nullptr;
    napi_get_boolean(env, argc == 1 && TryUnwrapKRJSON(env, args[0], &value), &result);
    return result;
}

static napi_value ParseKRJson(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    if (napi_get_cb_info(env, info, &argc, args, nullptr, nullptr) != napi_ok || argc != 1) {
        return nullptr;
    }
    napi_valuetype type = napi_undefined;
    if (napi_typeof(env, args[0], &type) != napi_ok || type != napi_string) {
        return nullptr;
    }
    const std::string text = kuikly::util::getNApiArgsStdString(env, args[0]);
    const KRRenderValue parsed = KRRenderValue::Parse(text);
    if (parsed.isNull()) {
        return nullptr;
    }
    return WrapKRJSON(env, parsed.jsonValue());
}

static napi_value TakeKRJsonRoutePayload(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    if (napi_get_cb_info(env, info, &argc, args, nullptr, nullptr) != napi_ok || argc != 1) {
        return nullptr;
    }
    return TakeKRJSONRoutePayload(env, args[0]);
}

// Returns a new KRJsonValue object: the top-level members of `value` (retained, not deep-copied)
// plus `key` set to the string `entry`. `value` itself is never mutated; it may be shared by the
// source page, the route store and the destination page.
static napi_value KRJsonWithEntry(napi_env env, napi_callback_info info) {
    size_t argc = 3;
    napi_value args[3] = {nullptr};
    if (napi_get_cb_info(env, info, &argc, args, nullptr, nullptr) != napi_ok || argc != 3) {
        return nullptr;
    }
    KRJSON source = KRJSON_INVALID;
    if (!TryUnwrapKRJSON(env, args[0], &source) || KRJSONGetType(source) != KRJSON_OBJECT) {
        return args[0];
    }
    std::u16string key;
    std::u16string entry;
    kuikly::util::GetNApiArgsStdU16String(env, args[1], key);
    kuikly::util::GetNApiArgsStdU16String(env, args[2], entry);
    KRJSON result = KRJSONNewObject();
    const size_t count = KRJSONGetSize(source);
    for (size_t i = 0; i < count; ++i) {
        size_t units = 0;
        const uint16_t *member = KRJSONObjectKeyAtUTF16(source, i, &units);
        if (member == nullptr ||
            std::u16string_view(reinterpret_cast<const char16_t *>(member), units) == key) {
            continue;
        }
        KRJSONObjectAppendUTF16NoDedup(result, member, units, KRJSONObjectValueAt(source, i));
    }
    const KRRenderValue entry_value = KRRenderValue::Make(entry);
    KRJSONObjectAppendUTF16NoDedup(result, reinterpret_cast<const uint16_t *>(key.data()), key.size(),
                                   entry_value.jsonValue());
    napi_value wrapped = WrapKRJSON(env, result);
    KRJSONRelease(result);
    return wrapped;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    napi_property_descriptor desc[] = {
        //  { "add", nullptr, Add, nullptr, nullptr, nullptr, napi_default, nullptr },
        {"onRenderViewSizeChanged", nullptr, OnRenderViewSizeChanged, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"onDestroyRenderView", nullptr, OnDestroyRenderView, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"onInitRenderView", nullptr, OnInitRenderView, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"arkTSCallNative", nullptr, ArkTSCallNative, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"sendEvent", nullptr, ArkTSOnSendEvent, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"sendEventSync", nullptr, ArkTSOnSendEventSync, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"updateConfig", nullptr, UpdateConfig, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"OnLaunchStart", nullptr, OnLaunchStart, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"createNativeRoot", nullptr, CreateNativeRoot, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"isBackPressConsumed", nullptr, isBackPressConsumed, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"isKRJSON", nullptr, IsKRJSON, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"parseKRJson", nullptr, ParseKRJson, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"takeKRJsonRoutePayload", nullptr, TakeKRJsonRoutePayload, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"krJsonWithEntry", nullptr, KRJsonWithEntry, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    KRMainThread::Export(env, exports);                   // 缓存主线程 uv_loop / async 句柄
    KRRenderManager::GetInstance().Export(env, exports);  // 尝试注册RenderView
    return exports;
}
EXTERN_C_END

static napi_module demoModule = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "kuikly",
    .nm_priv = (static_cast<void *>(0)),
    .reserved = {0},
};

extern "C" __attribute__((constructor)) void RegisterHarmony_renderModule(void) {
    napi_module_register(&demoModule);
}

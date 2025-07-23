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
#include "libohos_render/expand/modules/back_press/KRBackPressModule.h"
#include "libohos_render/foundation/KRCallbackData.h"
#include "libohos_render/manager/KRArkTSManager.h"
#include "libohos_render/manager/KRRenderManager.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/NAPIUtil.h"
#include "napi/native_api.h"

//  ArkTs层页面加载事件
static napi_value OnLaunchStart(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "napi_get_cb_info error");
        return 0;
    }
    std::string instance_id = kuikly::util::getNApiArgsStdString(env, args[0]);
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
    std::string instance_id = kuikly::util::getNApiArgsStdString(env, args[0]);
    std::string config_json = kuikly::util::getNApiArgsStdString(env, args[1]);
    if (auto renderView = KRRenderManager::GetInstance().GetRenderView(instance_id)) {
        if (auto ctx = renderView->GetContext()) {
            renderView->GetContext()->Config()->Update(config_json);
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
    // args is page_name, page_data, width , height, config_json
    size_t argc = 8;
    napi_value args[8] = {nullptr};
    if (napi_ok != napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) {
        napi_throw_error(env, "-1000", "napi_get_cb_info error");
        return 0;
    }
    std::string instance_id = kuikly::util::getNApiArgsStdString(env, args[0]);
    std::string page_name = kuikly::util::getNApiArgsStdString(env, args[1]);
    std::string page_data_json_str = kuikly::util::getNApiArgsStdString(env, args[2]);
    double renderViewWidth = kuikly::util::getNApiArgsDouble(env, args[3]);
    double renderViewHeight = kuikly::util::getNApiArgsDouble(env, args[4]);
    std::string config_json = kuikly::util::getNApiArgsStdString(env, args[5]);
    auto renderView = KRRenderManager::GetInstance().GetRenderView(instance_id);
    if (renderView != nullptr) {
        auto page_Data = std::make_shared<KRRenderValue>(page_data_json_str == "" ? "{}" : page_data_json_str);
        auto context = std::make_shared<KRRenderContextParams>(page_name, page_Data, instance_id, config_json);
        ArkUI_ContextHandle context_handle;
        OH_ArkUI_GetContextFromNapiValue(env, args[6], &context_handle);
        NativeResourceManager *native_resources_manager = OH_ResourceManager_InitNativeResourceManager(env, args[7]);
        int64_t launch_time = KRRenderManager::GetInstance().GetLaunchStartTime(instance_id);
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
    std::string instanceId = kuikly::util::getNApiArgsStdString(env, args[0]);
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

    std::string instanceId = kuikly::util::getNApiArgsStdString(env, args[0]);
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

    std::string instance_id = kuikly::util::getNApiArgsStdString(env, args[0]);
    auto event = kuikly::util::getNApiArgsStdString(env, args[1]);
    auto data = kuikly::util::getNApiArgsStdString(env, args[2]);
    auto renderView = KRRenderManager::GetInstance().GetRenderView(instance_id);
    if (renderView != nullptr) {
        renderView->SendEvent(event, data);
        napi_value result;
        napi_create_int32(env, 1, &result);
        return result;
    }
    return 0;
}
static napi_value CreateNativeRoot(napi_env env, napi_callback_info info) {
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
    std::string instance_id = kuikly::util::getNApiArgsStdString(env, args[0]);
    double send_time = kuikly::util::getNApiArgsInt64(env, args[1]);

    auto render_view = KRRenderManager::GetInstance().GetRenderView(instance_id);
    if (render_view != nullptr) {
        std::string back_press_module_name = kuikly::module::KRBackPressModule::MODULE_NAME;
        auto back_press_module = std::dynamic_pointer_cast<kuikly::module::KRBackPressModule>(render_view->GetModule(back_press_module_name));
        if (!back_press_module) {
            return result;
        }
        if (back_press_module->back_consumed_time.load() >= send_time) {
            bool is_back_consumed = back_press_module->is_back_consumed.load();
            if (is_back_consumed) {
                napi_create_int32(env, KRBackPressState::Consumed, &result);
            } else {
                napi_create_int32(env, KRBackPressState::NotConsumed, &result);
            }
        }
    }
    return result;
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
        {"updateConfig", nullptr, UpdateConfig, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"OnLaunchStart", nullptr, OnLaunchStart, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"createNativeRoot", nullptr, CreateNativeRoot, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"isBackPressConsumed", nullptr, isBackPressConsumed, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
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

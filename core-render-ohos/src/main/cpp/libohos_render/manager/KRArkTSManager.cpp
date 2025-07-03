/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#include "libohos_render/manager/KRArkTSManager.h"

#include "libohos_render/foundation/ark_ts.h"
#include "libohos_render/foundation/KRCallbackData.h"
#include "libohos_render/manager/KRKeyboardManager.h"
#include "libohos_render/manager/KRRenderManager.h"
#include "libohos_render/scheduler/KRContextScheduler.h"
#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/view/KRRenderView.h"


napi_value CToNApiValue(napi_env env, const KRAnyValue &value) {
    napi_value arg0Value;
    napi_status status;
    if (value != nullptr) {
        value->ToNapiValue(env, &arg0Value, status);
    } else {
        napi_get_null(env, &arg0Value);
    }
    return arg0Value;
}

KRArkTSManager &KRArkTSManager::GetInstance() {
    static KRArkTSManager instance;  // 静态局部变量
    return instance;
}

KRArkTSManager::KRArkTSManager() {}

/**
 * 处理来自ArkTS对Native侧的统一调用
 */
void KRArkTSManager::HandleArkTSCallNative(napi_env env, napi_value *args, size_t arg_size) {
    auto methodId = std::make_shared<KRRenderValue>(env, args[1])->toInt();
    if (methodId == static_cast<int>(KRArkTSCallNativeMethod::Register)) {  // 注册ArkTS互通信
        RegisterArkTSCallback(env, args, arg_size);
    } else if (methodId == static_cast<int>(KRArkTSCallNativeMethod::FireCallback)) {  // callback参数回调
        FireCallbackFromArkTS(env, args, arg_size);
    } else if (methodId == static_cast<int>(KRArkTSCallNativeMethod::KeyboardHeightChange)) {  // 键盘高度变化回调
        KeyboardHeightChange(env, args, arg_size);
    } else if (methodId == static_cast<int>(KRArkTSCallNativeMethod::FireViewEvent)) {
        // 响应View的事件（From ArkTS侧）
        FireViewEventFromArkTS(env, args, arg_size);
    }else if(methodId == static_cast<int>(KRArkTSCallNativeMethod::General)){
        // ArkTS到C++方向到通用信息传递
        HandleGeneralMessage(env, args, arg_size);
    }
}

/**
 * 注册调用ArkTS的回调闭包，实现Native调用ArkTS通道
 */
void KRArkTSManager::RegisterArkTSCallback(napi_env env, napi_value *args, size_t arg_size) {
    if (arkTSCallbackData_ != nullptr) {
        return;
    }
    arkTSCallbackData_ = new KRCallbackData();
    arkTSCallbackData_->env = env;
    arkTSCallbackData_->callbackFunc = args[arg_size - 1];

    // 将传入的callback转换为napi_ref延长其生命周期，防止被GC掉
    napi_create_reference(env, args[arg_size - 1], 1, &(arkTSCallbackData_->callbackRef));
}

/**
 * 调用ArkTS方法
 * 注：不允许在子线程调用，若要在子线程调用，请用KRContextScheduler::ScheduleTaskOnMainThread
 */
KRAnyValue KRArkTSManager::CallArkTSMethod(const std::string &instanceId, KRNativeCallArkTSMethod methodId,
                                           const KRAnyValue &arg0, const KRAnyValue &arg1, const KRAnyValue &arg2,
                                           const KRAnyValue &arg3, const KRAnyValue &arg4,
                                           const KRRenderCallback &callback, bool callback_keep_alive,
                                           ArkUI_NodeHandle *return_node_handle, bool arg_prefers_raw_napi_value,
                                           ArkUI_NodeContentHandle *pContentHandle) {
    if (arkTSCallbackData_ == nullptr) {
        return nullptr;
    }
    napi_env env = arkTSCallbackData_->env;
    napi_value callbackFun;
    napi_get_reference_value(env, arkTSCallbackData_->callbackRef, &callbackFun);
    napi_value callbackArgs[8] = {nullptr};
    napi_value instanceIdValue;
    napi_status status;
    std::make_shared<KRRenderValue>(instanceId)->ToNapiValue(env, &instanceIdValue, status);
    callbackArgs[0] = instanceIdValue;
    napi_value methodIdValue;
    napi_create_int32(env, (int32_t)methodId, &methodIdValue);
    callbackArgs[1] = methodIdValue;
    callbackArgs[2] = CToNApiValue(env, arg0);
    callbackArgs[3] = CToNApiValue(env, arg1);
    callbackArgs[4] = CToNApiValue(env, arg2);
    callbackArgs[5] = CToNApiValue(env, arg3);
    callbackArgs[6] = CToNApiValue(env, arg4);
    if (callback != nullptr) {
        auto pager_id = instanceId;
        auto renderView = KRRenderManager::GetInstance().GetRenderView(pager_id);
        if (renderView != nullptr) {
            auto callback_id =
                renderView->GenerateArgCallbackId(callback, callback_keep_alive, arg_prefers_raw_napi_value);
            callbackArgs[7] = CToNApiValue(env, NewKRRenderValue(callback_id));
        }
    } else {
        napi_value nullValue;
        napi_get_null(env, &nullValue);
        callbackArgs[7] = nullValue;
    }
    // 执行回调函数
    napi_value result;
    napi_call_function(env, nullptr, callbackFun, 8, callbackArgs, &result);
    if (return_node_handle != nullptr) {  // 返回一个arkui侧的node_handle
        napi_value componentContent = nullptr;
        napi_get_element(env, result, 0, &componentContent);
        //napi_get_named_property(env, result, "componentContent", &componentContent);
        OH_ArkUI_GetNodeHandleFromNapiValue(env, componentContent, return_node_handle);
        
        napi_value nodeContent = nullptr;
        napi_get_element(env, result, 1, &nodeContent);
        //napi_get_named_property(env, result, "slot", &nodeContent);
        ArkUI_NodeContentHandle contentHandle = nullptr;
        OH_ArkUI_GetNodeContentFromNapiValue(env, nodeContent, &contentHandle);
        if(pContentHandle){
            *pContentHandle = contentHandle;
        }
        
        return std::make_shared<KRRenderValue>(nullptr);
    }
    return std::make_shared<KRRenderValue>(env, result);
}

/**
 * 键盘高度变化回调
 */
void KRArkTSManager::KeyboardHeightChange(napi_env env, napi_value *args, size_t arg_size) {
    auto height = std::make_shared<KRRenderValue>(env, args[2])->toFloat();
    auto duration_ms = std::make_shared<KRRenderValue>(env, args[3])->toInt();
    KRKeyboardManager::GetInstance().NotifyKeyboardHeightChanged(height, duration_ms);
}

/**
 * ArkTS侧调用Native Callback
 */
void KRArkTSManager::FireCallbackFromArkTS(napi_env env, napi_value *args, size_t arg_size) {
    auto pager_id = std::make_shared<KRRenderValue>(env, args[0])->toString();
    auto callback_id = std::make_shared<KRRenderValue>(env, args[2])->toString();
    auto renderView = KRRenderManager::GetInstance().GetRenderView(pager_id);
    if (renderView != nullptr) {
        bool arg_prefer_raw_napi_value = false;
        auto callback = renderView->GetArgCallback(callback_id, arg_prefer_raw_napi_value);
        if (callback != nullptr) {
            std::shared_ptr<KRRenderValue> data;
            if (arg_prefer_raw_napi_value) {
                data = std::make_shared<KRRenderValue>(NapiValue(env, args[3]));
            } else {
                data = std::make_shared<KRRenderValue>(env, args[3]);
            }
            callback(data);
        }
    }
}

/**
 * ArkTS侧响应ViewEvent事件
 */
void KRArkTSManager::FireViewEventFromArkTS(napi_env env, napi_value *args, size_t arg_size) {
    auto pager_id = std::make_shared<KRRenderValue>(env, args[0])->toString();
    auto tag = std::make_shared<KRRenderValue>(env, args[2])->toInt();
    auto eventKey = std::make_shared<KRRenderValue>(env, args[3])->toString();
    auto renderView = KRRenderManager::GetInstance().GetRenderView(pager_id);
    if (renderView != nullptr) {
        auto view = renderView->GetView(tag);
        if (view != nullptr) {
            auto data = std::make_shared<KRRenderValue>(env, args[4]);
            view->FireViewEventFromArkTS(eventKey, data);
        }
    }
}

enum KRGenralMessageKind{
  RegisterCreateor = 0
};

void KRArkTSManager::HandleGeneralMessage(napi_env env, napi_value *args, size_t arg_size){
    if(arg_size < 3){
        return;
    }
    ArkTS arkts(env);
    int messageKind = arkts.GetInteger(args[2]);
    switch(messageKind){
    case RegisterCreateor:
        std::string viewType = arkts.GetString(args[3]);
        int version = arkts.GetInteger(args[4]);
        KRArkTSViewNameRegistry::ViewKind viewKind = KRArkTSViewNameRegistry::ViewKindNotFound;
        if(version == 1){
            viewKind = KRArkTSViewNameRegistry::ViewKindV1;
        }else if(version == 2){
            viewKind = KRArkTSViewNameRegistry::ViewKindV2;
        }
        if(viewKind != KRArkTSViewNameRegistry::ViewKindNotFound){
            KRArkTSViewNameRegistry::GetInstance().AddViewName(viewType, viewKind);
        }
        break;
    }
}
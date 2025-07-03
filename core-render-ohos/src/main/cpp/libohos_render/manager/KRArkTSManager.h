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

#ifndef CORE_RENDER_OHOS_KRARKTSMANAGER_H
#define CORE_RENDER_OHOS_KRARKTSMANAGER_H
#include <arkui/native_type.h>
#include <cstddef>
#include <string>
#include <set>
#include "libohos_render/foundation/KRCallbackData.h"
#include "libohos_render/foundation/KRCommon.h"
#include "napi/native_api.h"

/// Native调用ArkTS方法枚举
enum class KRNativeCallArkTSMethod {
    Unknown = 0,
    CallModuleMethod = 1,     // 调用Module方法
    CreateView = 2,           // 创建逻辑View
    CreateArkUINode = 3,      // 创建ArkTS的ArkUI节点
    SetViewProp = 4,          // 设置View属性
    SetViewEvent = 5,         // 设置View事件
    CallViewMethod = 6,       // 调用View方法
    RemoveView = 7,           // 删除View
    SetViewSize = 8,          // 设置View尺寸
    DidMoveToParentView = 9,  // 添加到父节点中
};

/// ArkTS调用Native方法枚举
enum class KRArkTSCallNativeMethod {
    General = 0,
    Register = 1,              // 注册给Native回调ArkTS闭包
    FireCallback = 2,          // 响应native callback
    KeyboardHeightChange = 3,  // 键盘高度变化
    FireViewEvent = 4,         // 响应View事件
};

class KRArkTSManager {
 public:
    static KRArkTSManager &GetInstance();
    // 禁止复制和赋值
    KRArkTSManager(const KRArkTSManager &) = delete;
    KRArkTSManager &operator=(const KRArkTSManager &) = delete;

    /**
     * 处理来自ArkTS对Native侧的统一调用
     */
    void HandleArkTSCallNative(napi_env env, napi_value *args, size_t arg_size);

    /**
     * 调用ArkTS方法(仅能主线程调用)
     * 注：不允许在子线程调用，若要在子线程调用，请用KRContextScheduler::ScheduleTaskOnMainThread
     * @param return_node_handle 返回ArkTS侧的ArkUI node节点句柄（默认为null）
     * @param callback_keep_alive callback 是否 keep alive
     */
    KRAnyValue CallArkTSMethod(const std::string &instanceId, KRNativeCallArkTSMethod methodId, const KRAnyValue &arg0,
                               const KRAnyValue &arg1, const KRAnyValue &arg2, const KRAnyValue &arg3,
                               const KRAnyValue &arg4, const KRRenderCallback &callback,
                               bool callback_keep_alive = false, ArkUI_NodeHandle *return_node_handle = nullptr,
                               bool arg_prefers_raw_napi_value = false, ArkUI_NodeContentHandle *contentHandle = nullptr);

    /**
     * 获取NAPI Env
     * @return napi_env
     */
    napi_env GetEnv() {
        if (arkTSCallbackData_) {
            return arkTSCallbackData_->env;
        }
        return nullptr;
    }

 private:
    KRArkTSManager();  // 构造函数私有化
    KRCallbackData *arkTSCallbackData_ = nullptr;
    /**
     * 注册调用ArkTS的回调闭包，实现Native调用ArkTS通道
     */
    void RegisterArkTSCallback(napi_env env, napi_value *args, size_t arg_size);
    /**
     * 键盘高度变化回调
     */
    void KeyboardHeightChange(napi_env env, napi_value *args, size_t arg_size);
    /**
     * ArkTS侧调用Native Callback
     */
    void FireCallbackFromArkTS(napi_env env, napi_value *args, size_t arg_size);
    /**
     * ArkTS侧响应ViewEvent事件
     */
    void FireViewEventFromArkTS(napi_env env, napi_value *args, size_t arg_size);
    
    void HandleGeneralMessage(napi_env env, napi_value *args, size_t arg_size);
};

class IKRRenderViewExport;
class KRArkTSViewNameRegistry{
    friend class KRArkTSManager;
    friend class IKRRenderViewExport;
    //friend std::shared_ptr<IKRRenderViewExport> IKRRenderViewExport::CreateView(const std::string &view_name);
private:
    enum ViewKind{
        ViewKindNotFound, ViewKindV1, ViewKindV2
    };
    static KRArkTSViewNameRegistry& GetInstance(){
        static KRArkTSViewNameRegistry instance_;
        return instance_;
    }
    
    void AddViewName(const std::string &name, ViewKind kind ){
        if(kind == ViewKindV1){
            v1_names_.insert(name);
        }else if(kind == ViewKindV2){
            v2_names_.insert(name);
        }
    }
    
    ViewKind KindOfView(const std::string& viewName){
        if(v1_names_.find(viewName) != v1_names_.end()){
            return ViewKindV1;
        }
        if(v2_names_.find(viewName) != v2_names_.end()){
            return ViewKindV2;
        }
        return ViewKindNotFound;
    }
    
private:
    std::set<std::string> v1_names_;
    std::set<std::string> v2_names_;
};
#endif  // CORE_RENDER_OHOS_KRARKTSMANAGER_H

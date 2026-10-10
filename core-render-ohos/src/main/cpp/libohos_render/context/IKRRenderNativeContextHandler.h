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

#ifndef CORE_RENDER_OHOS_IKRRENDERNATIVECONTEXTHANDLER_H
#define CORE_RENDER_OHOS_IKRRENDERNATIVECONTEXTHANDLER_H

#include <string>
#include "KRRenderContextParams.h"
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/foundation/type/KRRenderValue.h"

#define CALL_ARGS_COUNT 6

enum class KuiklyRenderContextMethod {
    KuiklyRenderContextMethodUnknown = 0,
    KuiklyRenderContextMethodCreateInstance = 1,   // "createInstance" 方法
    KuiklyRenderContextMethodUpdateInstance = 2,   // "updateInstance" 方法
    KuiklyRenderContextMethodDestroyInstance = 3,  // "destroyInstance" 方法
    KuiklyRenderContextMethodFireCallback = 4,     // "fireCallback" 方法
    KuiklyRenderContextMethodFireViewEvent = 5,    // "fireViewEvent" 方法
    KuiklyRenderContextMethodLayoutView = 6        // "layoutView" 方法
};

enum class KuiklyRenderNativeMethod {
    KuiklyRenderNativeMethodUnknown = 0,
    KuiklyRenderNativeMethodCreateRenderView = 1,         // "createRenderView" 方法
    KuiklyRenderNativeMethodRemoveRenderView = 2,         // "removeRenderView" 方法
    KuiklyRenderNativeMethodInsertSubRenderView = 3,      // "insertSubRenderView" 方法
    KuiklyRenderNativeMethodSetViewProp = 4,              // "setViewProp" 方法
    KuiklyRenderNativeMethodSetRenderViewFrame = 5,       // "setRenderViewFrame" 方法
    KuiklyRenderNativeMethodCalculateRenderViewSize = 6,  // "calculateRenderViewSize" 方法
    KuiklyRenderNativeMethodCallViewMethod = 7,           // "callViewMethod" 方法
    KuiklyRenderNativeMethodCallModuleMethod = 8,         // "callModuleMethod" 方法
    KuiklyRenderNativeMethodCreateShadow = 9,             // "createShadow" 方法
    KuiklyRenderNativeMethodRemoveShadow = 10,            // "removeShadow" 方法
    KuiklyRenderNativeMethodSetShadowProp = 11,           // "setShadowProp" 方法
    KuiklyRenderNativeMethodSetShadowForView = 12,        // "setShadowForView" 方法
    KuiklyRenderNativeMethodSetTimeout = 13,              // "setTimeout方法"
    KuiklyRenderNativeMethodCallShadowMethod = 14,        // "callShadowModule方法"
    KuiklyRenderNativeMethodFireFatalException = 15,      // "fireFatalException"方法
    KuiklyRenderNativeMethodSyncFlushUI = 16,             // "syncFlushUI方法"
    KuiklyRenderNativeMethodCallTDFNativeMethod = 17      // "callTDFModuleMethod"
};

class IKRRenderNativeContextHandler;
class KRRenderContextParams;

class ICallNativeCallback {
 public:
    ICallNativeCallback() {}
    /**
     * 处理来自 Kotlin 侧的 Native 方法调用。
     *
     * 契约说明：
     * - arg0 为 Kotlin 传入的 instanceId（字符串）。
     *   实现方也可通过 handler 自身持有的
     *   `IKRRenderNativeContextHandler::instance_id_` 获取同一值。
     * - arg1..arg5 的语义由 KuiklyRenderNativeMethod 各枚举值决定，
     *   具体参见 KRRenderCore::PerformNativeCallback 的分派实现。
     */
    virtual KRAnyValue OnCallNative(const KuiklyRenderNativeMethod &method, KRAnyValue &arg0, KRAnyValue &arg1,
                                    KRAnyValue &arg2, KRAnyValue &arg3, KRAnyValue &arg4, KRAnyValue &arg5) = 0;
};

class IKRRenderNativeContextHandler : public std::enable_shared_from_this<IKRRenderNativeContextHandler> {
 public:
    IKRRenderNativeContextHandler() : is_destroying_(false) {}

    static KRRenderCValue DispatchCallNative(const std::string &instanceId, int methodId, const KRRenderCValue &arg0,
                                             const KRRenderCValue &arg1, const KRRenderCValue &arg2,
                                             const KRRenderCValue &arg3, const KRRenderCValue &arg4,
                                             const KRRenderCValue &arg5);
    
    static std::shared_ptr<IKRRenderNativeContextHandler>
    CreateContextHandler(const std::shared_ptr<KRRenderContextParams> &context_params);

    void RegisterCallNative(ICallNativeCallback *callback);

    // arg0 为 instanceId，语义参见 ICallNativeCallback::OnCallNative；原样透传。
    KRAnyValue OnCallNative(const KuiklyRenderNativeMethod &method, KRAnyValue &arg0, KRAnyValue &arg1,
                            KRAnyValue &arg2, KRAnyValue &arg3, KRAnyValue &arg4, KRAnyValue &arg5);

    void Init(const std::shared_ptr<KRRenderContextParams> context_params);

    virtual void InitContext();  //  初始化通信上下文

    void Call(const KuiklyRenderContextMethod &method, const KRAnyValue &arg0, const KRAnyValue &arg1,
              const KRAnyValue &arg2, const KRAnyValue &arg3, const KRAnyValue &arg4, const KRAnyValue &arg5);

    virtual void CallKotlinMethod(const KuiklyRenderContextMethod &method, const KRAnyValue &arg0,
                                  const KRAnyValue &arg1, const KRAnyValue &arg2, const KRAnyValue &arg3,
                                  const KRAnyValue &arg4, const KRAnyValue &arg5) = 0;

    virtual ~IKRRenderNativeContextHandler() = default;

    virtual void WillDestroy();
    virtual void OnDestroy();

 protected:
    virtual void OnInit(const std::shared_ptr<KRRenderContextParams> &context_paramse);

 protected:
    std::string instance_id_;
    ICallNativeCallback *call_native_callback_;
    bool is_destroying_ = false;
};

#endif  // CORE_RENDER_OHOS_IKRRENDERNATIVECONTEXTHANDLER_H

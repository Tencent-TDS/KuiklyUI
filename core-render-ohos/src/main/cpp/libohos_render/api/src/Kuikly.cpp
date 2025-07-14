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

#include "libohos_render/api/include/Kuikly/Kuikly.h"

#include <hilog/log.h>
#include <unordered_set>

#include "KRAnyDataInternal.h"
#include "libohos_render/expand/components/image/KRImageAdapterManager.h"
#include "libohos_render/expand/components/richtext/KRFontAdapterManager.h"
#include "libohos_render/export/IKRRenderModuleExport.h"

#ifdef __cplusplus
extern "C" {
#endif

struct KRRenderModuleCallbackContextData {
    std::weak_ptr<IKRRenderModuleExport> module_;
    KRRenderCallback cb_;
    bool keepCallbackAlive_;
    KRRenderModuleCallbackContextData(std::weak_ptr<IKRRenderModuleExport> module,
        KRRenderCallback cb,
        bool keepCallbackAlive): module_(module), cb_(cb), keepCallbackAlive_(keepCallbackAlive){
        // blank
    }
    ~KRRenderModuleCallbackContextData(){
        module_.reset();
        cb_ = nullptr;
    }
};


bool KRAnyDataIsString(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return false;
    }
    return internal->anyValue->isString();
}

bool KRAnyDataIsInt(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return false;
    }
    return internal->anyValue->isInt();
}

const char *KRAnyDataGetString(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return nullptr;
    }
    return internal->anyValue->toCValue().value.stringValue;
}

#ifdef __cplusplus
}
#endif

namespace {
class KRForwardRenderModule : public IKRRenderModuleExport {
 public:
    KRForwardRenderModule(const std::string &moduleName,
                            KRRenderModuleOnConstruct onConstruct,
                            KRRenderModuleOnDestruct  onDestruct,
                            KRRenderModuleCallMethod  onCallMethod,
                            void *userData)
        : moduleName_(moduleName), userData_(userData),
          onConstruct_(onConstruct), onDestruct_(onDestruct), onCallMethod_(onCallMethod) {
        if (onConstruct) {
            moduleInstance_ = onConstruct(moduleName.c_str());
        }else{
            moduleInstance_ = nullptr;
        }
    }
    ~KRForwardRenderModule() {
        if (onDestruct_) {
            onDestruct_(moduleInstance_);
        }
        std::lock_guard<std::mutex> guard(callbacksMutex_);
        std::for_each(callbacks_.begin(), callbacks_.end(), [this /* non-escaping lambda, safe*/](auto item) { FreeCallbackContext(item); });
    }

    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback, bool callback_keep_alive) override {
        if (onCallMethod_) {
            std::string paramStr = params->toString();
            // create a callback context
            struct KRRenderModuleCallbackContextData *cbData = AllocCallbackContext(shared_from_this(), callback, callback_keep_alive);
            struct KRAnyDataInternal anyDataInternal;
            anyDataInternal.anyValue = params;
            auto val = onCallMethod_(moduleInstance_, moduleName_.c_str(), sync, method.c_str(), &anyDataInternal, (KRRenderModuleCallbackContext)cbData);
            
            if(val.res){
                auto value = std::make_shared<KRRenderValue>(val.res);
                if(val.free){
                    val.free((void*)val.res);
                }
                
                return value;
            }
            return std::make_shared<KRRenderValue>();
        }
        return nullptr;
    }

    void DoCallback(KRRenderModuleCallbackContext context, const char *data) {
        // Note: avoid dangling pointer by look up through a set instead of directly casting context to KRRenderModuleCallbackContextData
        KRRenderModuleCallbackContextData *callbackContextData = FindCallbackContext(context);
        if (callbackContextData && callbackContextData->cb_) {
            callbackContextData->cb_(NewKRRenderValue(data));
            if(!callbackContextData->keepCallbackAlive_){
                callbackContextData->cb_ = nullptr;
                RemoveCallbackContext(context);
                FreeCallbackContext(callbackContextData);
            }
        }
    }

 private:
    struct KRRenderModuleCallbackContextData *AllocCallbackContext(std::weak_ptr<IKRRenderModuleExport> module,
                                                                   const KRRenderCallback &callback,
        bool keepCallbackAlive) {
        struct KRRenderModuleCallbackContextData *cbData = new KRRenderModuleCallbackContextData(shared_from_this(), callback, keepCallbackAlive);
        std::lock_guard<std::mutex> guard(callbacksMutex_);
        callbacks_.insert(cbData);
        return cbData;
    }
    struct KRRenderModuleCallbackContextData *FindCallbackContext(KRRenderModuleCallbackContext context) {
        if (context == nullptr) {
            return nullptr;
        }
        KRRenderModuleCallbackContextData *result = nullptr;
        std::lock_guard<std::mutex> guard(callbacksMutex_);
        auto it = callbacks_.find(context);
        if (it != callbacks_.end()) {
            result = *it;
        }
        return result;
    }
    struct KRRenderModuleCallbackContextData *RemoveCallbackContext(KRRenderModuleCallbackContext context) {
        if (context == nullptr) {
            return nullptr;
        }
        KRRenderModuleCallbackContextData *result = nullptr;
        std::lock_guard<std::mutex> guard(callbacksMutex_);
        auto it = callbacks_.find(context);
        if (it != callbacks_.end()) {
            result = *it;
            callbacks_.erase(it);
        }
        return result;
    }
    void FreeCallbackContext(struct KRRenderModuleCallbackContextData *context) {
        if (context == nullptr) {
            return;
        }
        delete context;
    }

    std::string moduleName_;
    KRRenderModuleOnConstruct onConstruct_;
    KRRenderModuleOnDestruct onDestruct_;
    KRRenderModuleCallMethod onCallMethod_;
    void *userData_;
    void *moduleInstance_;
    std::unordered_set<struct KRRenderModuleCallbackContextData *> callbacks_;
    std::mutex callbacksMutex_;
};

class BridgeLogAdapter : public IKRLogAdapter {
 public:
    explicit BridgeLogAdapter(KRLogAdapter adapter) : adapter_(adapter) {
        // blank
    }
    void LogInfo(const std::string &tag, const std::string &msg) {
        if (adapter_) {
            adapter_(KRLogLevelInfo, tag.c_str(), msg.c_str());
        }
    }
    void LogDebug(const std::string &tag, const std::string &msg) {
        if (adapter_) {
            adapter_(KRLogLevelDebug, tag.c_str(), msg.c_str());
        }
    }
    void LogError(const std::string &tag, const std::string &msg) {
        if (adapter_) {
            adapter_(KRLogLevelError, tag.c_str(), msg.c_str());
        }
    }

 private:
    KRLogAdapter adapter_;
};

}  // namespace

#ifdef __cplusplus
extern "C" {
#endif

void KRRenderModuleDoCallback(KRRenderModuleCallbackContext context, const char *data) {
    struct KRRenderModuleCallbackContextData *contextData = (struct KRRenderModuleCallbackContextData *)context;
    if (auto renderModule = contextData->module_.lock()) {
        std::shared_ptr<KRForwardRenderModule> forwardRenderModule =
            std::dynamic_pointer_cast<KRForwardRenderModule>(renderModule);
        forwardRenderModule->DoCallback(context, data);
    }
}

void KRRenderModuleRegister(const char *moduleName,
                            KRRenderModuleOnConstruct onConstruct,
                            KRRenderModuleOnDestruct  onDestruct,
                            KRRenderModuleCallMethod  onCallMethod,
                            void *userData) {
    std::string name = moduleName;
    IKRRenderModuleExport::RegisterModuleCreator(moduleName, [onConstruct, onDestruct, onCallMethod, userData, name]() {
        return std::make_shared<KRForwardRenderModule>(name, onConstruct, onDestruct, onCallMethod, userData);
    });
}

void KRRegisterFontAdapter(KRFontAdapter adapter, const char *fontFamily) {
    KRFontAdapterManager::GetInstance()->RegisterFontAdapter(adapter, fontFamily);
}

void KRRegisterImageAdapter(KRImageAdapter adapter) {
    KRImageAdapterManager::GetInstance()->RegisterImageAdapter(adapter);
}

int KRLogLevelInfo = LogLevel::LOG_INFO;
int KRLogLevelDebug = LogLevel::LOG_DEBUG;
int KRLogLevelError = LogLevel::LOG_ERROR;

void KRRegisterLogAdapter(KRLogAdapter adapter) {
    auto bridge = std::make_shared<BridgeLogAdapter>(adapter);
    KRRenderAdapterManager::GetInstance().RegisterLogAdapter(std::dynamic_pointer_cast<IKRLogAdapter>(bridge));
}
    
void KRRegisterColorAdapter(KRColorAdapterParseColor adapter){
    class BridgedColorParseAdapter : public IKRColorParseAdapter {
     public:
        BridgedColorParseAdapter(KRColorAdapterParseColor parser): parser_(parser){
            // blank
        }
        std::int64_t GetHexColor(const std::string &colorStr) override{
            return parser_ ? parser_(colorStr.c_str()) : 0;
        }
    private:
        KRColorAdapterParseColor parser_;
    };
    if(adapter){
        KRRenderAdapterManager::GetInstance().RegisterColorAdapter(std::make_shared<BridgedColorParseAdapter>(adapter));        
    }else{
        KRRenderAdapterManager::GetInstance().RegisterColorAdapter(nullptr);
    }
}

// API for troubleshooting view reuse issue
int g_kuikly_disable_view_reuse = 0;
void KRDisableViewReuse(){
    g_kuikly_disable_view_reuse = 1;
}
#ifdef __cplusplus
}
#endif

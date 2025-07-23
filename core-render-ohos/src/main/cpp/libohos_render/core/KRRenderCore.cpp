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

#include "libohos_render/core/KRRenderCore.h"

#include <functional>
#include <memory>
#include "libohos_render/foundation/KRRect.h"
#include "libohos_render/layer/KRRenderLayerHandler.h"
#include "libohos_render/manager/KRArkTSManager.h"
#include "libohos_render/scheduler/KRContextScheduler.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/view/KRRenderView.h"
#include "libohos_render/manager/KRRenderManager.h"

EXTERN_C_START
const KRRenderCValue com_tencent_kuikly_CallNative(int methodId, KRRenderCValue arg0, KRRenderCValue arg1,
                                                   KRRenderCValue arg2, KRRenderCValue arg3, KRRenderCValue arg4,
                                                   KRRenderCValue arg5) {
    return IKRRenderNativeContextHandler::DispatchCallNative(std::string(arg0.value.stringValue), methodId, arg0, arg1,
                                                             arg2, arg3, arg4, arg5);
}

CallKotlin callKotlin_;
int com_tencent_kuikly_SetCallKotlin(CallKotlin callKotlin) {
    callKotlin_ = callKotlin;
    return 0;
}

void com_tencent_kuikly_ScheduleContextTask(const char *pagerId, void (*onSchedule)(const char *pagerId)) {
    KRContextScheduler::ScheduleTask(
        false, 0, [instanceId = std::string(pagerId), onSchedule]() { onSchedule(instanceId.c_str()); });
}

bool com_tencent_kuikly_IsCurrentOnContextThread(const char *pagerId) {
    return KRContextScheduler::IsCurrentOnContextThread();
}
EXTERN_C_END

/**
 * 是否为同步调用Mask
 */
static constexpr int kSyncCallbackMask = 1;
/**
 * callback是否为keep alive Mask
 */
static constexpr int kCallbackKeepAliveMask = 2;

/** 任务在context线程中执行 */
static void PerformTaskOnContextQueue(bool isSync, int delayMs, const KRSchedulerTask &task) {
    KRContextScheduler::ScheduleTask(isSync, delayMs, task);
}

KRRenderCore::KRRenderCore(std::weak_ptr<IKRRenderView> renderView, std::shared_ptr<KRRenderContextParams> context)
    : ICallNativeCallback() {
    renderView_ = renderView;
    context_ = context;
    defaultNullValue_ = std::make_shared<KRRenderValue>();
    uiScheduler_ = std::make_shared<KRUIScheduler>(this);
    contextHandler_ = IKRRenderNativeContextHandler::CreateContextHandler(context);
    // 注册kotlin call native回调（走onCallNative接口）
    contextHandler_->RegisterCallNative(this);
    contextHandler_->Init(context_);
    renderLayerHandler_ = std::make_shared<KRRenderLayerHandler>();
    renderLayerHandler_->Init(renderView, context);
}

bool KRRenderCore::IsSyncCallback(const KRAnyValue &params) {
    auto is_sync_call_int = params->toInt();
    // == kSyncCallbackMask: 表示 callback 是同步，不 keep alive
    // == (kSyncCallbackMask + kCallbackKeepAliveMask): 表示 callback 是同步，keep alive
    return (is_sync_call_int == kSyncCallbackMask) ||
           (is_sync_call_int == (kSyncCallbackMask + kCallbackKeepAliveMask));
}

bool KRRenderCore::IsCallbackKeepAlive(const KRAnyValue &params) {
    auto is_sync_call_int = params->toInt();
    // == kCallbackKeepAliveMask: callback 是 keep alive
    // == (kSyncCallbackMask + kCallbackKeepAliveMask): callback 是 keep alive 并且是同步 callback
    return (is_sync_call_int == kCallbackKeepAliveMask) ||
           (is_sync_call_int == (kSyncCallbackMask + kCallbackKeepAliveMask));
}

void KRRenderCore::DidInit() {
    auto strongSelf = shared_from_this();
    // createInstance to kotlin
    auto sync = context_->ExecuteMode()->IsContextSyncInit();
    KRContextScheduler::DirectRunOnMainThread(sync, [strongSelf, sync] {
        auto page_name = std::make_shared<KRRenderValue>(strongSelf->context_->PageName());
        auto page_data = std::make_shared<KRRenderValue>(strongSelf->context_->PageData()->toString());
        auto null_arg = strongSelf->defaultNullValue_;
        strongSelf->notifyInitState(KRInitState::kStateInitContextStart);
        strongSelf->contextHandler_->InitContext();
        strongSelf->notifyInitState(KRInitState::kStateInitContextFinish);
        strongSelf->notifyInitState(KRInitState::kStateCreateInstanceStart);
        strongSelf->CallKotlinMethod(KuiklyRenderContextMethod::KuiklyRenderContextMethodCreateInstance, page_name, page_data,
                                     null_arg, null_arg, null_arg);
        // 获取操作完成后的时间点
        auto end = std::chrono::steady_clock::now();
        strongSelf->uiScheduler_->PerformSyncMainQueueTasksBlockIfNeed(sync);
        strongSelf->notifyInitState(KRInitState::kStateCreateInstanceFinish);
    });
    strongSelf->uiScheduler_->PerformMainThreadTaskWaitToSyncBlockIfNeed();
}

// kt通信
void KRRenderCore::SendEvent(std::string event_name, const std::string &json_data) {
    auto self = shared_from_this();
    PerformTaskOnContextQueue(false, 0, [self, event_name, json_data] {
        auto event = std::make_shared<KRRenderValue>(event_name);
        auto data = std::make_shared<KRRenderValue>(json_data);
        auto nullValue = self->defaultNullValue_;
        self->CallKotlinMethod(KuiklyRenderContextMethod::KuiklyRenderContextMethodUpdateInstance, event, data, nullValue,
                               nullValue, nullValue);
    });
}

std::shared_ptr<IKRRenderViewExport> KRRenderCore::GetView(int tag) {
    return renderLayerHandler_->GetRenderView(tag);
}

std::shared_ptr<IKRRenderModuleExport> KRRenderCore::GetModule(std::string &module_name) {
    return renderLayerHandler_->GetModule(module_name);
}

std::shared_ptr<IKRRenderModuleExport> KRRenderCore::GetModuleOrCreate(std::string &module_name) {
    return renderLayerHandler_->GetModuleOrCreate(module_name);
}

void KRRenderCore::WillDealloc(const std::string &instanceId) {
    contextHandler_->WillDestroy();
    renderLayerHandler_->WillDestroy();
    auto self = shared_from_this();
    std::string id = instanceId;
    PerformTaskOnContextQueue(false, 0, [self, id] {
        auto nullValue = self->defaultNullValue_;
        self->CallKotlinMethod(KuiklyRenderContextMethod::KuiklyRenderContextMethodDestroyInstance, nullValue, nullValue,
                               nullValue, nullValue, nullValue);
        self->contextHandler_->OnDestroy();
        self->uiScheduler_->AddTaskToMainQueueWithTask([self, id] {
            self->OnDestroy();
            KRRenderManager::GetInstance().DestroyRenderViewCallBack(id);
        });
    });
}

void KRRenderCore::OnDestroy() {
    renderLayerHandler_->OnDestroy();
}

void KRRenderCore::AddTaskToMainQueueWithTask(const KRSchedulerTask &task) {
    uiScheduler_->AddTaskToMainQueueWithTask(task);
}

/**
 * 执行任务当该主线程loop结束时
 */
void KRRenderCore::PerformTaskWhenMainThreadEnd(const KRSchedulerTask &task) {
    uiScheduler_->PerformTaskWhenDidEnd(task);
}

/**
 * 是否正在执行主线程任务中
 */
bool KRRenderCore::IsPerformMainTasking() {
    return uiScheduler_->IsPerformMainTasking();
}

std::shared_ptr<KRRenderValue>
KRRenderCore::OnCallNative(const KuiklyRenderNativeMethod &method, std::shared_ptr<KRRenderValue> &arg0,
                           std::shared_ptr<KRRenderValue> &arg1, std::shared_ptr<KRRenderValue> &arg2,
                           std::shared_ptr<KRRenderValue> &arg3, std::shared_ptr<KRRenderValue> &arg4,
                           std::shared_ptr<KRRenderValue> &arg5) {
    if (ShouldSyncCallMethod(method, arg5)) {  // 是否同步调用Native方法，如Module syncCall方法
        return PerformNativeCallback(method, arg1, arg2, arg3, arg4, arg5, true);
    } else {
        if (!uiScheduler_) {
            return defaultNullValue_;
        }
        std::weak_ptr<KRRenderCore> weakSelf = shared_from_this();
        uiScheduler_->AddTaskToMainQueueWithTask([weakSelf, method, arg1, arg2, arg3, arg4, arg5] {
            if (auto locked = weakSelf.lock()) {
                locked->PerformNativeCallback(method, arg1, arg2, arg3, arg4, arg5, false);
            }
        });
    }
    return defaultNullValue_;
}

// 判断事件是否需要同步调用
bool KRRenderCore::ShouldSyncCallMethod(const KuiklyRenderNativeMethod &method, std::shared_ptr<KRRenderValue> &arg5) {
    if (method == KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCallModuleMethod) {
        return IsSyncCallback(arg5);
    }
    return method == KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCalculateRenderViewSize ||
           method == KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCreateShadow ||
           method == KuiklyRenderNativeMethod::KuiklyRenderNativeMethodRemoveShadow ||
           method == KuiklyRenderNativeMethod::KuiklyRenderNativeMethodSetShadowProp ||
           method == KuiklyRenderNativeMethod::KuiklyRenderNativeMethodSetShadowForView ||
           method == KuiklyRenderNativeMethod::KuiklyRenderNativeMethodSetTimeout ||
           method == KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCallShadowMethod ||
           method == KuiklyRenderNativeMethod::KuiklyRenderNativeMethodSyncFlushUI ||
           method == KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCallTDFNativeMethod;
}

KRAnyValue KRRenderCore::PerformNativeCallback(const KuiklyRenderNativeMethod &method, const KRAnyValue &arg1,
                                               const KRAnyValue &arg2, const KRAnyValue &arg3, const KRAnyValue &arg4,
                                               const KRAnyValue &arg5, bool sync) {
    switch (method) {
    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCreateRenderView: {
        renderLayerHandler_->CreateRenderView(arg1->toInt(), arg2->toString());
        break;
    }
    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodRemoveRenderView: {
        renderLayerHandler_->RemoveRenderView(arg1->toInt());
        break;
    }
    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodInsertSubRenderView: {
        renderLayerHandler_->InsertSubRenderView(arg1->toInt(), arg2->toInt(), arg3->toInt());
        break;
    }
    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodSetViewProp: {
        bool isEvent = arg4->toInt() == 1;
        if (isEvent) {
            bool sync = IsSyncCallback(arg5);
            std::weak_ptr<KRRenderCore> weakSelf = shared_from_this();
            KRRenderCallback callback = [weakSelf, arg1, arg2, arg3, arg4, arg5, sync](KRAnyValue res) {
                auto shouldSync = sync;
                PerformTaskOnContextQueue(shouldSync, 0, [weakSelf, shouldSync, res, arg1, arg2, arg3, arg4, arg5] {
                    if (auto locked = weakSelf.lock()) {
                        locked->CallKotlinMethod(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireViewEvent, arg1, arg2,
                                                 res, locked->defaultNullValue_, locked->defaultNullValue_);
                        if (shouldSync) {  // 主线程
                            locked->uiScheduler_->PerformSyncMainQueueTasksBlockIfNeed(true);
                        }
                    }
                });
                if (shouldSync) {
                    if (auto locked = weakSelf.lock()) {
                        locked->uiScheduler_->PerformMainThreadTaskWaitToSyncBlockIfNeed();
                    }
                }
            };
            renderLayerHandler_->SetEvent(arg1->toInt(), arg2->toString(), callback);
        } else {
            renderLayerHandler_->SetProp(arg1->toInt(), arg2->toString(), arg3);
        }
        break;
    }

    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodSetRenderViewFrame: {
        auto rect = KRRect(arg2->toFloat(), arg3->toFloat(), arg4->toFloat(), arg5->toFloat());
        std::string rectData((const char *)&rect, sizeof(KRRect));
        auto value = std::make_shared<KRRenderValue>(rectData);
        renderLayerHandler_->SetProp(arg1->toInt(), "frame", value);
        break;
    }
    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCalculateRenderViewSize: {
        auto sizeStr = renderLayerHandler_->CalculateRenderViewSize(arg1->toInt(), arg2->toDouble(), arg3->toDouble());
        return std::make_shared<KRRenderValue>(sizeStr);
    }
    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCallViewMethod: {
        auto callbackId = arg4->toString();
        KRRenderCallback callback = nullptr;
        if (!callbackId.empty()) {
            std::weak_ptr<KRRenderCore> weakSelf = shared_from_this();
            callback = [weakSelf, arg4](KRAnyValue res) {
                if (auto locked = weakSelf.lock()) {
                    PerformTaskOnContextQueue(false, 0, [weakSelf, arg4, res] {
                        if (auto locked = weakSelf.lock()) {
                            locked->CallKotlinMethod(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireCallback, arg4,
                                                     res, locked->defaultNullValue_, locked->defaultNullValue_,
                                                     locked->defaultNullValue_);
                        }
                    });
                }
            };
        }
        renderLayerHandler_->CallViewMethod(arg1->toInt(), arg2->toString(), arg3, callback);
        break;
    }
    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCallModuleMethod: {
        auto callbackId = arg4->toString();
        KRRenderCallback callback = nullptr;
        auto callback_keep_alive = false;
        if (!callbackId.empty()) {
            callback_keep_alive = IsCallbackKeepAlive(arg5);
            std::weak_ptr<KRRenderCore> weakSelf = shared_from_this();
            callback = [weakSelf, arg4](KRAnyValue res) {
                if (auto locked = weakSelf.lock()) {
                    PerformTaskOnContextQueue(false, 0, [weakSelf, arg4, res] {
                        if (auto locked = weakSelf.lock()) {
                            locked->CallKotlinMethod(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireCallback, arg4,
                                                     res, locked->defaultNullValue_, locked->defaultNullValue_,
                                                     locked->defaultNullValue_);
                        }
                    });
                }
            };
        }
        return renderLayerHandler_->CallModuleMethod(sync, arg1->toString(), arg2->toString(), arg3, callback,
                                                     callback_keep_alive);
    }

    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCreateShadow: {
        renderLayerHandler_->CreateShadow(arg1->toInt(), arg2->toString());
        return defaultNullValue_;
    }

    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodRemoveShadow: {
        renderLayerHandler_->RemoveShadow(arg1->toInt());
        return defaultNullValue_;
    }
    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodSetShadowProp: {
        renderLayerHandler_->SetShadowProp(arg1->toInt(), arg2->toString(), arg3);
        return defaultNullValue_;
    }
    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodSetShadowForView: {
        auto tag = arg1->toInt();
        auto shadow = renderLayerHandler_->Shadow(tag);
        if (!shadow) {
            return defaultNullValue_;
        }
        auto task = shadow->TaskToMainQueueWhenWillSetShadowToView();
        std::weak_ptr<KRRenderCore> weakSelf = shared_from_this();
        uiScheduler_->AddTaskToMainQueueWithTask([task, weakSelf, tag, shadow] {
            if (task) {
                task();
            }
            if (auto lock = weakSelf.lock()) {
                lock->renderLayerHandler_->SetShadow(tag, shadow);
            }
        });
        return defaultNullValue_;
    }
    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodSetTimeout: {
        std::weak_ptr<KRRenderCore> weakSelf = shared_from_this();
        PerformTaskOnContextQueue(false, arg1->toInt() > 0 ? arg1->toInt() : 1, [weakSelf, arg2] {
            if (auto lock = weakSelf.lock()) {
                auto nullValue = lock->defaultNullValue_;
                lock->CallKotlinMethod(KuiklyRenderContextMethod::KuiklyRenderContextMethodFireCallback, arg2, nullValue,
                                       nullValue, nullValue, nullValue);
            }
        });
        break;
    }

    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCallShadowMethod: {
        std::weak_ptr<KRRenderCore> weakSelf = shared_from_this();
        return renderLayerHandler_->CallShadowMethod(arg1->toInt(), arg2->toString(), arg3->toString());
    }

    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodFireFatalException: {
        KRRenderAdapterManager::GetInstance().OnFatalException(context_->InstanceId(), arg1->toString());
        break;
    }

    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodSyncFlushUI: {
        uiScheduler_->PerformSyncMainQueueTasksBlockIfNeed(false);
        return defaultNullValue_;
    }

    case KuiklyRenderNativeMethod::KuiklyRenderNativeMethodCallTDFNativeMethod: {
        // to do
        break;
    }
    }
    return defaultNullValue_;
}

void KRRenderCore::WillPerformUITasksWithScheduler() {  // 运行在 context 线程
    // 去触发layout to kotlin
    // 同步主线程任务前，需要告诉kotlin侧 去 layoutIfNeed, 避免viewFrame设置时机和创建view时机不同步
    CallKotlinMethod(KuiklyRenderContextMethod::KuiklyRenderContextMethodLayoutView, defaultNullValue_, defaultNullValue_,
                     defaultNullValue_, defaultNullValue_, defaultNullValue_);
}

void KRRenderCore::CallKotlinMethod(const KuiklyRenderContextMethod &method, const KRAnyValue &arg1, const KRAnyValue &arg2,
                                    const KRAnyValue &arg3, const KRAnyValue &arg4, const KRAnyValue &arg5) {
    auto arg0 = std::make_shared<KRRenderValue>(context_->InstanceId());
    contextHandler_->Call(method, arg0, arg1, arg2, arg3, arg4, arg5);
}

void KRRenderCore::notifyInitState(KRInitState state) {
    if (auto rootview = renderView_.lock()) {
        std::shared_ptr<KRRenderView> derivedPtr = std::dynamic_pointer_cast<KRRenderView>(rootview);
        if (derivedPtr) {
            derivedPtr->DispatchInitState(state);  //  向根View通知初始化事件
        }
    }
}
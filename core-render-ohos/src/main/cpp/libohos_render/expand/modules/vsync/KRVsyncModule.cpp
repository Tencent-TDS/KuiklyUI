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

#include "KRVsyncModule.h"

#include <cstring>
#include <memory>

#include "libohos_render/scheduler/KRContextScheduler.h"

namespace kuikly {
namespace module {

const char KRVsyncModule::MODULE_NAME[] = "KRVsyncModule";
const char KRVsyncModule::METHOD_REGISTER_VSYNC[] = "registerVsync";
const char KRVsyncModule::METHOD_UNREGISTER_VSYNC[] = "unRegisterVsync";

namespace {

// Passed to OH_NativeVSync_RequestFrame as user data. Holds a weak_ptr so an
// in-flight vsync callback stays safe after the module is destroyed, plus the
// generation at request time so callbacks from a stale registration are dropped.
struct VsyncRequestContext {
    std::weak_ptr<KRVsyncModule> weak_module;
    uint64_t generation;
};

KRAnyValue MakeTimestampParam(long long timestamp_nanos) {
    KRRenderValueMap map;
    map["timestamp"] = KRRenderValue::Make(static_cast<int64_t>(timestamp_nanos));
    return KRRenderValue::Make(map);
}

}  // namespace

KRVsyncModule::~KRVsyncModule() {
    if (native_vsync_) {
        OH_NativeVSync_Destroy(native_vsync_);
        native_vsync_ = nullptr;
    }
}

KRAnyValue KRVsyncModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                     const KRRenderCallback &callback, bool callback_keep_alive) {
    if (std::strcmp(method.c_str(), METHOD_REGISTER_VSYNC) == 0) {
        RegisterVsync(callback);
    } else if (std::strcmp(method.c_str(), METHOD_UNREGISTER_VSYNC) == 0) {
        UnRegisterVsync();
    }
    return KREmptyValue();
}

void KRVsyncModule::OnDestroy() {
    UnRegisterVsync();
}

void KRVsyncModule::RegisterVsync(const KRRenderCallback &callback) {
    if (!callback) {
        return;
    }
    std::lock_guard<std::mutex> guard(mutex_);
    callback_ = callback;
    running_ = true;
    generation_++;
    tick_pending_.store(false);
    if (!native_vsync_) {
        native_vsync_ = OH_NativeVSync_Create(MODULE_NAME, strlen(MODULE_NAME));
    }
    RequestNextVsyncLocked();
}

void KRVsyncModule::UnRegisterVsync() {
    std::lock_guard<std::mutex> guard(mutex_);
    running_ = false;
    callback_ = nullptr;
    generation_++;
}

// Must be called with mutex_ held.
void KRVsyncModule::RequestNextVsyncLocked() {
    if (!native_vsync_) {
        return;
    }
    auto *context = new VsyncRequestContext{
        std::static_pointer_cast<KRVsyncModule>(shared_from_this()), generation_};
    OH_NativeVSync_RequestFrame(native_vsync_, &KRVsyncModule::OnVsync, context);
}

// Invoked on the vsync thread. The callback itself dispatches to the context
// thread inside KRRenderCore, so no extra thread hop is needed here.
void KRVsyncModule::OnVsync(long long timestamp, void *data) {
    auto *context = static_cast<VsyncRequestContext *>(data);
    auto self = context->weak_module.lock();
    const uint64_t generation = context->generation;
    delete context;
    if (!self) {
        return;
    }
    KRRenderCallback cb = nullptr;
    {
        std::lock_guard<std::mutex> guard(self->mutex_);
        if (!self->running_ || generation != self->generation_) {
            return;
        }
        self->RequestNextVsyncLocked();
        // Back-pressure: if the previous tick is still queued on the context
        // thread (e.g. a slow frame is blocking it), drop this tick instead of
        // letting ticks pile up behind it. The next vsync will deliver a fresh
        // timestamp once the context thread catches up.
        if (self->tick_pending_.exchange(true)) {
            return;
        }
        cb = self->callback_;
    }
    if (cb) {
        cb(MakeTimestampParam(timestamp));
        // The callback above enqueues the Kotlin frame task on the context
        // thread; enqueue the flag reset right behind it so tick_pending_
        // clears only after that frame task has actually been consumed.
        std::weak_ptr<KRVsyncModule> weak_module = self;
        KRContextScheduler::ScheduleTask(0, [weak_module] {
            if (auto module = weak_module.lock()) {
                module->tick_pending_.store(false);
            }
        });
    } else {
        self->tick_pending_.store(false);
    }
}

}  // namespace module
}  // namespace kuikly

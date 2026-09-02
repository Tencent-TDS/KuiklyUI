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

#include "libohos_render/expand/modules/vsync/KRVsyncModule.h"

#include <cstring>
#include <memory>

#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/scheduler/KRContextScheduler.h"
#include "libohos_render/utils/KRStringUtil.h"

namespace kuikly {
namespace module {

const char KRVsyncModule::MODULE_NAME[] = "KRVsyncModule";
const char KRVsyncModule::METHOD_REGISTER_VSYNC[] = "registerVsync";
const char KRVsyncModule::METHOD_UNREGISTER_VSYNC[] = "unRegisterVsync";

namespace {

// 与 Kotlin 侧 VsyncModule 的钳制区间保持一致(core/.../VsyncModule.kt)
constexpr int64_t kMinFrameIntervalNanos = 1'000'000;      // 1ms
constexpr int64_t kMaxFrameIntervalNanos = 100'000'000;    // 100ms

}  // namespace
VsyncRequestContext::VsyncRequestContext(const std::weak_ptr<KRVsyncModule> &weak, uint64_t gen)
    : weak_module(weak), generation(gen) {}

// 释放一份引用，归零者负责 delete。原子操作保证 vsync 线程与 context 线程并发安全。
static inline void ReleaseRequestRef(VsyncRequestContext *context) {
    if (context != nullptr && context->refs.fetch_sub(1) == 1) {
        delete context;
    }
}


KRVsyncModule::~KRVsyncModule() {
    UnRegisterVsync();
    std::lock_guard<std::mutex> guard(mutex_);
    if (native_vsync_ != nullptr) {
        OH_NativeVSync_Destroy(native_vsync_);
        native_vsync_ = nullptr;
    }
}

KRAnyValue KRVsyncModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                     const KRRenderCallback &callback) {
    if (kuikly::util::isEqual(method, METHOD_REGISTER_VSYNC)) {
        RegisterVsync(callback);
    } else if (kuikly::util::isEqual(method, METHOD_UNREGISTER_VSYNC)) {
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
    last_timestamp_nanos_ = 0;
    last_interval_nanos_ = 0;
    if (native_vsync_ == nullptr) {
        native_vsync_ = OH_NativeVSync_Create(MODULE_NAME, strlen(MODULE_NAME));
    }
    RequestNextVsyncLocked();
}

void KRVsyncModule::UnRegisterVsync() {
    std::lock_guard<std::mutex> guard(mutex_);
    running_ = false;
    callback_ = nullptr;
    // 代数递增后，在途回调因 generation 不匹配被安全丢弃
    generation_++;
    // 释放槽侧引用；若在途回调尚未到达，其引用减法会将计数归零并完成释放
    ReleaseRequestRef(pending_request_);
    pending_request_ = nullptr;
}

// 调用前须持有 mutex_
void KRVsyncModule::RequestNextVsyncLocked() {
    if (native_vsync_ == nullptr || !running_) {
        return;
    }
    ReleaseRequestRef(pending_request_);  // 防御：清掉未被回调消费的残留(正常路径为 null)
    pending_request_ = new VsyncRequestContext(
        std::static_pointer_cast<KRVsyncModule>(shared_from_this()), generation_);
    OH_NativeVSync_RequestFrame(native_vsync_, &KRVsyncModule::OnVsync, pending_request_);
}

// 系统 vsync 线程回调。KRRenderCallback 内部(KRRenderCore 桥接)会自动把
// 任务投递到 context 线程，此处直接调用回调是安全的。
void KRVsyncModule::OnVsync(long long timestamp, void *data) {
    auto *context = static_cast<VsyncRequestContext *>(data);
    KRRenderCallback callback = nullptr;
    int32_t interval_nanos = 0;
    // 字段读取阶段内存必然存活：槽侧引用此时至少为 1(见 VsyncRequestContext 注释)。
    std::shared_ptr<KRVsyncModule> self = context->weak_module.lock();
    const uint64_t generation = context->generation;
    if (self) {
        std::lock_guard<std::mutex> guard(self->mutex_);
        // 摘链：让槽侧不再持有本 context（槽侧引用的释放由 UnRegister/arm 侧的
        // ReleaseRequestRef 完成，若已摘链则该处为 no-op）
        if (self->pending_request_ == context) {
            self->pending_request_ = nullptr;
        }
        if (!self->running_ || generation != self->generation_) {
            ReleaseRequestRef(context);  // 释放回调侧引用
            return;
        }
        // 帧间隔 = 相邻两次 vsync 时间戳差值，自适应刷新率变化(含 LTPO 变频)
        if (self->last_timestamp_nanos_ != 0) {
            const int64_t diff = timestamp - self->last_timestamp_nanos_;
            if (diff >= kMinFrameIntervalNanos && diff <= kMaxFrameIntervalNanos) {
                self->last_interval_nanos_ = static_cast<int32_t>(diff);
            }
            // 差值越界(首帧后异常/休眠恢复)时沿用上次有效值
            interval_nanos = self->last_interval_nanos_;
        }
        self->last_timestamp_nanos_ = timestamp;
        // RequestFrame 为一次性请求，先重新 arm 下一帧
        self->RequestNextVsyncLocked();
        if (interval_nanos == 0) {
            // 首帧无差值基准，不上报( Kotlin 侧启动时已手动渲染首帧 )
            ReleaseRequestRef(context);
            return;
        }
        // 背压：上一 tick 仍在 context 线程排队/执行时丢弃本 tick，防止堆积。
        // 下一帧 vsync 会带来新的时间戳，慢帧消化后自动恢复。
        if (self->tick_pending_.exchange(true)) {
            ReleaseRequestRef(context);
            return;
        }
        callback = self->callback_;
    }
    if (callback) {
        // 注意：必须传 int32_t。KRRenderValue(int32) -> KRRenderCValue::Type::INT ->
        // Kotlin 侧 Type.INT -> value.intValue 转为 Kotlin Int；若传 int64 会变为
        // Kotlin Long，Kotlin 侧 (data as? Int) 匹配失败而回退默认帧间隔。
        callback(KRRenderValue::Make(interval_nanos));
        // callback 内部已把帧任务投递到 context 队列；把背压复位任务排到同一
        // FIFO 队列紧随其后，保证 tick_pending_ 在帧任务实际消费后才清除。
        std::weak_ptr<KRVsyncModule> weak_self = self;
        KRContextScheduler::ScheduleTask(0, [weak_self] {
            if (auto locked = weak_self.lock()) {
                locked->tick_pending_.store(false);
            }
        });
    } else if (self) {
        self->tick_pending_.store(false);
    }
    ReleaseRequestRef(context);
}

}  // namespace module
}  // namespace kuikly

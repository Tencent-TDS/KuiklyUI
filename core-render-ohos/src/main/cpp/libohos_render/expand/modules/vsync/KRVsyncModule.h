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

#pragma once

#include <native_vsync/native_vsync.h>
#include <atomic>
#include <cstdint>
#include <mutex>
#include "libohos_render/export/IKRRenderModuleExport.h"

namespace kuikly {
namespace module {

/**
 * Vsync module: provides system vsync callbacks (OH_NativeVSync) for the Kotlin-side
 * VsyncModule, aligning the Compose frame dispatcher with the screen refresh rate
 * (replacing the previous 12ms timer loop).
 */
class KRVsyncModule : public IKRRenderModuleExport {
 public:
    static const char MODULE_NAME[];

    KRVsyncModule() = default;
    ~KRVsyncModule();

    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback, bool callback_keep_alive) override;
    void OnDestroy() override;

 private:
    static const char METHOD_REGISTER_VSYNC[];
    static const char METHOD_UNREGISTER_VSYNC[];

    void RegisterVsync(const KRRenderCallback &callback);
    void UnRegisterVsync();
    void RequestNextVsyncLocked();

    static void OnVsync(long long timestamp, void *data);

    std::mutex mutex_;
    KRRenderCallback callback_ = nullptr;
    bool running_ = false;
    // Generation counter: bumped on unregister/destroy so that in-flight vsync
    // callbacks from a previous registration are safely ignored.
    uint64_t generation_ = 0;
    // Back-pressure flag: true while a vsync tick is queued on the context thread
    // but not yet consumed. Further ticks are dropped instead of piling up.
    std::atomic<bool> tick_pending_{false};
    OH_NativeVSync *native_vsync_ = nullptr;
};

}  // namespace module
}  // namespace kuikly

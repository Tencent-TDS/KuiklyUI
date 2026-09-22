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

#include "libohos_render/export/KRShadowExportC.h"
#include "libohos_render/export/IKRRenderShadowExport.h"

#include <memory>
#include <string>

// ============================================================
// C-to-C++ Bridge: wraps C callbacks inside an IKRRenderShadowExport
// ============================================================

class KRShadowCWrapper : public IKRRenderShadowExport {
public:
    KRShadowCWrapper(KRShadowHandle handle, const KRShadowVTable &vtable)
        : handle_(handle), vtable_(vtable) {}

    ~KRShadowCWrapper() {
        if (vtable_.destroy && handle_) {
            vtable_.destroy(handle_);
        }
    }

    void SetProp(const std::string &prop_key,
                 const KRAnyValue &prop_value) override {
        if (vtable_.set_prop && handle_) {
            std::string val = prop_value ? prop_value->toString() : "";
            vtable_.set_prop(handle_, prop_key.c_str(), val.c_str());
        }
    }

    KRSize CalculateRenderViewSize(double constraint_width,
                                   double constraint_height) override {
        if (vtable_.calculate_size && handle_) {
            double w = 0.0, h = 0.0;
            vtable_.calculate_size(handle_, constraint_width, constraint_height,
                                   &w, &h);
            return KRSize(w, h);
        }
        return KRSize(0.0, 0.0);
    }

    KRSchedulerTask TaskToMainQueueWhenWillSetShadowToView() override {
        return nullptr;
    }

private:
    KRShadowHandle handle_;
    KRShadowVTable vtable_;
};

// ============================================================
// Public C API (exported from libkuikly.so)
// ============================================================

extern "C" {

void KRRegisterShadowCreator(const char *view_name,
                             KRShadowCreateFunc create,
                             const KRShadowVTable *vtable) {
    if (!view_name || !create || !vtable) {
        return;
    }
    if (!vtable->calculate_size || !vtable->destroy) {
        return; // required callbacks missing
    }

    std::string name(view_name);
    // Copy the vtable so the lambda below can safely capture it by value.
    KRShadowVTable vt = *vtable;

    IKRRenderShadowExport::RegisterShadowCreator(
        name,
        [create, vt]() -> std::shared_ptr<IKRRenderShadowExport> {
            KRShadowHandle h = create();
            if (!h) {
                return nullptr;
            }
            return std::make_shared<KRShadowCWrapper>(h, vt);
        });
}

} // extern "C"

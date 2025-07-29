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

#ifndef CORE_RENDER_OHOS_KRImageViewWrapper_H
#define CORE_RENDER_OHOS_KRImageViewWrapper_H

#include "libohos_render/expand/components/image/KRImageView.h"
#include "libohos_render/export/IKRRenderViewExport.h"

class KRImageViewWrapper : public IKRRenderViewExport {
 public:
    KRImageViewWrapper() = default;
    KRImageViewWrapper(const KRImageViewWrapper &) = delete;
    KRImageViewWrapper(KRImageViewWrapper &&) = delete;
    KRImageViewWrapper &operator=(const KRImageViewWrapper &) = delete;
    KRImageViewWrapper &operator=(KRImageViewWrapper &&) = delete;
    bool ReuseEnable() override;
    void DidMoveToParentView() override;
    void DidInit() override;
    void SetRenderViewFrame(const KRRect &frame) override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    bool ResetProp(const std::string &prop_key) override;
    void OnDestroy() override;

 private:
    void InitImageView(std::shared_ptr<KRImageView> image_view);

 private:
    std::string place_holder_src_;
    std::string resize_;
    std::shared_ptr<KRImageView> place_holder_image_view_;
    std::shared_ptr<KRImageView> image_view_;
    bool did_insert_image_view_ = false;
};

#endif  // CORE_RENDER_OHOS_KRImageViewWrapper_H
